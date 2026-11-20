package persistence;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import model.ModelosSistema.*;

import java.awt.Desktop;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PersistenciaService {
    private static final String ARQUIVO_JSON = "dados_planilha_certa.json";
    private static final String ARQUIVO_BACKUP = "dados_planilha_certa.bak.json";
    private static final String ARQUIVO_CSV = "relatorio_planilha_certa.csv";
    private final Path diretorio;
    private final Gson gson;
    private final GoogleDriveService driveService;
    private byte[] revisaoPrincipal;

    public PersistenciaService() {
        this(Paths.get("data"));
    }

    public PersistenciaService(Path diretorio) {
        this.diretorio = diretorio;
        // Adaptador para evitar erro de reflexão com LocalDate no Java 17+
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> {
                    if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
                        throw new JsonSyntaxException("Prazo ou data inválida");
                    }
                    return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
                })
                .create();
        this.driveService = new GoogleDriveService();
    }

    public synchronized boolean salvarDados(BancoDados banco) {
        Path principal = diretorio.resolve(ARQUIVO_JSON);
        try {
            Files.createDirectories(diretorio);
            try (FileChannel canal = abrirCanalDeBloqueio(); FileLock bloqueio = canal.lock()) {
                byte[] revisaoEmDisco = revisao(principal);
                if (!Arrays.equals(revisaoPrincipal, revisaoEmDisco)) {
                    System.out.println("[AVISO] A base foi alterada por outra sessão. Reabra o sistema antes de gravar.");
                    return false;
                }
                if (revisaoEmDisco != null) {
                    if (lerArquivoJson(principal) != null) {
                        copiarArquivo(principal, diretorio.resolve(ARQUIVO_BACKUP));
                    } else {
                        preservarArquivoCorrompido(principal);
                    }
                }
                revisaoPrincipal = gravarPrincipal(banco);
            }
        } catch (IOException | JsonIOException | OverlappingFileLockException e) {
            System.out.println("[ERRO DE ARQUIVO] Não foi possível salvar os dados: " + e.getMessage());
            return false;
        }
        driveService.enviarBackup(principal.toFile());
        return true;
    }

    public synchronized BancoDados carregarDados() {
        Path principal = diretorio.resolve(ARQUIVO_JSON);
        try {
            Files.createDirectories(diretorio);
            try (FileChannel canal = abrirCanalDeBloqueio(); FileLock bloqueio = canal.lock()) {
                BancoDados banco = carregarDados(true);
                revisaoPrincipal = revisao(principal);
                return banco;
            }
        } catch (IOException | OverlappingFileLockException e) {
            System.out.println("[AVISO] Não foi possível preparar a gravação em '" + diretorio + "'.");
            revisaoPrincipal = null;
            return carregarDados(false);
        }
    }

    private BancoDados carregarDados(boolean permiteRestaurar) {
        Path principal = diretorio.resolve(ARQUIVO_JSON);
        Path backup = diretorio.resolve(ARQUIVO_BACKUP);
        BancoDados dados = lerArquivoJson(principal);
        if (dados != null) {
            System.out.printf("[PERSISTÊNCIA] Base carregada: %d cliente(s), %d consultor(es), %d projeto(s).%n",
                    dados.clientes.size(), dados.consultores.size(), dados.projetos.size());
            return dados;
        }

        dados = lerArquivoJson(backup);
        if (dados != null) {
            recuperar(dados, principal, backup, permiteRestaurar);
            return dados;
        }

        Path raiz = diretorio.toAbsolutePath().normalize().getParent();
        if (raiz != null && Files.notExists(principal) && Files.notExists(backup)) {
            Path principalAntigo = raiz.resolve(ARQUIVO_JSON);
            Path backupAntigo = raiz.resolve(ARQUIVO_BACKUP);
            BancoDados dadosBackup = lerArquivoJson(backupAntigo);
            dados = lerArquivoJson(principalAntigo);
            if (dados != null || dadosBackup != null) {
                boolean usouBackup = dados == null;
                if (usouBackup) {
                    dados = dadosBackup;
                }
                recuperar(dados, principal, usouBackup ? backupAntigo : principalAntigo, permiteRestaurar);
                if (permiteRestaurar && dadosBackup != null && Files.exists(principal)) {
                    try {
                        copiarArquivo(backupAntigo, backup);
                    } catch (IOException e) {
                        System.out.println("[AVISO] O backup anterior permanece em " + backupAntigo);
                    }
                }
                return dados;
            }
        }
        System.out.println("[INFO] Nenhuma base válida encontrada. Iniciando uma base limpa.");
        return new BancoDados();
    }

    private BancoDados lerArquivoJson(Path caminho) {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(caminho, StandardCharsets.UTF_8))) {
            JsonElement elemento = gson.getAdapter(JsonElement.class).read(reader);
            if (elemento == null || !elemento.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonSyntaxException("Estrutura JSON inválida");
            }
            JsonObject objeto = elemento.getAsJsonObject();
            exigirListas(objeto, "clientes", "consultores", "projetos");
            BancoDados dados = gson.fromJson(objeto, BancoDados.class);
            dados.sanitizar();
            validarDadosParaLeitura(dados);
            return dados;
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException | JsonParseException | DateTimeException | IllegalArgumentException | IllegalStateException e) {
            System.out.println("[AVISO] Base inválida ou inacessível em '" + caminho + "'.");
            return null;
        }
    }

    private static void validarDadosParaLeitura(BancoDados dados) {
        Set<String> clientes = identificarPessoas(dados.clientes);
        Set<String> consultores = identificarPessoas(dados.consultores);
        if (dados.consultores.stream().anyMatch(consultor -> consultor.getEspecializacao() == null)) {
            throw new JsonSyntaxException("Consultor sem especialização");
        }
        for (Projeto projeto : dados.projetos) {
            String consultorId = projeto.getConsultorId();
            if (projeto.getId() == null || projeto.getTitulo() == null || projeto.getArea() == null
                    || !clientes.contains(projeto.getClienteId())
                    || (consultorId != null && !consultores.contains(consultorId))) {
                throw new JsonSyntaxException("Projeto sem dados ou vínculos válidos");
            }
            if (projeto.getEtapas().isEmpty() || projeto.getEtapas().stream().anyMatch(etapa ->
                    etapa.getNome() == null || etapa.getPrazo() == null || !Double.isFinite(etapa.getValor()))) {
                throw new JsonSyntaxException("Projeto sem etapas válidas");
            }
        }
    }

    private static Set<String> identificarPessoas(List<? extends Pessoa> pessoas) {
        Set<String> ids = new HashSet<>();
        for (Pessoa pessoa : pessoas) {
            if (pessoa.getId() == null || pessoa.getId().isBlank() || pessoa.getNome() == null) {
                throw new JsonSyntaxException("Cadastro sem identificação");
            }
            ids.add(pessoa.getId());
        }
        return ids;
    }

    private static void exigirListas(JsonObject objeto, String... nomes) {
        for (String nome : nomes) {
            if (!objeto.has(nome) || !objeto.get(nome).isJsonArray()) {
                throw new JsonSyntaxException("Lista obrigatória inválida: " + nome);
            }
        }
    }

    private void recuperar(BancoDados dados, Path principal, Path origem, boolean permiteRestaurar) {
        System.out.println("[RECUPERAÇÃO] Base carregada de '" + origem + "'.");
        if (!permiteRestaurar) {
            return;
        }
        try {
            Files.createDirectories(diretorio);
            if (Files.exists(principal)) {
                preservarArquivoCorrompido(principal);
            }
            gravarPrincipal(dados);
        } catch (IOException | JsonIOException e) {
            System.out.println("[AVISO] Base recuperada em memória. Falha ao gravar: " + e.getMessage());
        }
    }

    private byte[] gravarPrincipal(BancoDados banco) throws IOException {
        Path principal = diretorio.resolve(ARQUIVO_JSON);
        Path temporario = Files.createTempFile(diretorio, "dados-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                gson.toJson(banco, writer);
            }
            byte[] novaRevisao = revisao(temporario);
            substituirArquivo(temporario, principal);
            System.out.println("[PERSISTÊNCIA] Dados salvos em '" + principal + "'.");
            return novaRevisao;
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private FileChannel abrirCanalDeBloqueio() throws IOException {
        return FileChannel.open(diretorio.resolve("dados.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    private static byte[] revisao(Path arquivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream entrada = new DigestInputStream(Files.newInputStream(arquivo), digest)) {
                entrada.transferTo(OutputStream.nullOutputStream());
            }
            return digest.digest();
        } catch (NoSuchFileException e) {
            return null;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private void copiarArquivo(Path origem, Path destino) throws IOException {
        Path temporario = Files.createTempFile(diretorio, "backup-", ".tmp");
        try {
            Files.copy(origem, temporario, StandardCopyOption.REPLACE_EXISTING);
            substituirArquivo(temporario, destino);
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private void preservarArquivoCorrompido(Path principal) throws IOException {
        Path copia = Files.createTempFile(diretorio, "dados-corrompidos-", ".json");
        try {
            Files.copy(principal, copia, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(copia);
            throw e;
        }
    }

    private static void substituirArquivo(Path temporario, Path destino) throws IOException {
        try {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void exportarCSV(BancoDados banco) {
        if (banco.projetos.isEmpty()) {
            System.out.println("[AVISO] Não há projetos cadastrados para exportar para CSV.");
            return;
        }

        Path csv = diretorio.resolve(ARQUIVO_CSV);
        try {
            Files.createDirectories(diretorio);
            try (Writer writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                // BOM UTF-8 para abrir com acentuação perfeita no Excel.
                writer.write('\uFEFF');
                writer.write("ID_Projeto;Titulo;Cliente;Categoria;Consultor;Especializacao;Progresso(%);Valor_Total(R$);Etapas_Atrasadas\n");

                for (Projeto p : banco.projetos) {
                    Cliente cliente = banco.clientes.stream()
                            .filter(c -> c.getId().equals(p.getClienteId()))
                            .findFirst().orElseThrow();
                    String nomeConsultor = banco.consultores.stream()
                            .filter(c -> c.getId().equals(p.getConsultorId()))
                            .map(Consultor::getNome)
                            .findFirst().orElse("Pendente");

                    writer.write(String.format("%s;%s;%s;%s;%s;%s;%.1f;%.2f;%d%n",
                            campoCsv(p.getId()), campoCsv(p.getTitulo()), campoCsv(cliente.getNome()),
                            cliente.getCategoriaTexto(), campoCsv(nomeConsultor), p.getArea().getDescricao(),
                            p.calcularProgresso(), p.calcularValorTotal(), p.contarEtapasAtrasadas()));
                }
            }
            System.out.println("[CSV EXCEL] Planilha exportada com sucesso em '" + csv + "'!");
            abrirArquivoSeSuportado(csv.toFile());
        } catch (IOException e) {
            System.out.println("[ERRO CSV] Falha ao exportar planilha: " + e.getMessage());
        }
    }

    private static String campoCsv(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private void abrirArquivoSeSuportado(File arquivo) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(arquivo);
            }
        } catch (Throwable ignored) {
            // Ignora silenciosamente se não houver aplicativo associado ao arquivo
        }
    }
}
