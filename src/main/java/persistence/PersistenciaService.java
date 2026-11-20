package persistence;

import com.google.gson.*;
import model.ModelosSistema.*;

import java.awt.Desktop;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PersistenciaService {
    private static final String ARQUIVO_JSON = "dados_planilha_certa.json";
    private static final String ARQUIVO_BACKUP = "dados_planilha_certa.bak.json";
    private static final String ARQUIVO_CSV = "relatorio_planilha_certa.csv";
    private final Gson gson;
    private final GoogleDriveService driveService;

    public PersistenciaService() {
        // Adaptador para evitar erro de reflexão com LocalDate no Java 17+
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                        LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                .create();
        this.driveService = new GoogleDriveService();
    }

    public void salvarDados(BancoDados banco) {
        try {
            // Cria cópia de segurança (.bak.json) antes de sobrescrever o arquivo principal
            Path pathPrincipal = Paths.get(ARQUIVO_JSON);
            if (Files.exists(pathPrincipal) && Files.size(pathPrincipal) > 0) {
                Files.copy(pathPrincipal, Paths.get(ARQUIVO_BACKUP), StandardCopyOption.REPLACE_EXISTING);
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(ARQUIVO_JSON), StandardCharsets.UTF_8)) {
                gson.toJson(banco, writer);
                System.out.println("[PERSISTÊNCIA] Dados salvos em '" + ARQUIVO_JSON + "' (backup .bak atualizado).");
            }

            // Desafio Extra: Tentativa de backup no Google Drive
            driveService.enviarBackup(new File(ARQUIVO_JSON));
        } catch (IOException e) {
            System.out.println("[ERRO DE ARQUIVO] Não foi possível salvar os dados: " + e.getMessage());
        }
    }

    public BancoDados carregarDados() {
        Path pathPrincipal = Paths.get(ARQUIVO_JSON);
        Path pathBackup = Paths.get(ARQUIVO_BACKUP);

        if (!Files.exists(pathPrincipal)) {
            if (Files.exists(pathBackup)) {
                System.out.println("[RECUPERAÇÃO] Arquivo principal ausente. Restaurando a partir de '" + ARQUIVO_BACKUP + "'...");
                BancoDados recuperado = lerArquivoJson(ARQUIVO_BACKUP);
                if (recuperado != null) {
                    salvarDados(recuperado);
                    return recuperado;
                }
            }
            System.out.println("[INFO] Nenhum arquivo anterior encontrado. Iniciando base de dados limpa.");
            return new BancoDados();
        }

        BancoDados dados = lerArquivoJson(ARQUIVO_JSON);
        if (dados != null) {
            System.out.printf("[PERSISTÊNCIA] Base carregada: %d cliente(s), %d consultor(es), %d projeto(s).%n",
                    dados.clientes.size(), dados.consultores.size(), dados.projetos.size());
            return dados;
        }

        // Fallback automático caso o JSON principal esteja corrompido
        System.out.println("[AVISO] Arquivo JSON principal corrompido! Tentando recuperar do backup (.bak)...");
        if (Files.exists(pathBackup)) {
            BancoDados recuperado = lerArquivoJson(ARQUIVO_BACKUP);
            if (recuperado != null) {
                System.out.println("[RECUPERAÇÃO] Dados restaurados com sucesso a partir do arquivo de backup!");
                salvarDados(recuperado);
                return recuperado;
            }
        }

        System.out.println("[AVISO] Não foi possível recuperar o backup. Iniciando base limpa.");
        return new BancoDados();
    }

    private BancoDados lerArquivoJson(String caminho) {
        try (Reader reader = new InputStreamReader(new FileInputStream(caminho), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, BancoDados.class);
        } catch (JsonSyntaxException | IOException e) {
            return null;
        }
    }

    public void exportarCSV(BancoDados banco) {
        if (banco.projetos.isEmpty()) {
            System.out.println("[AVISO] Não há projetos cadastrados para exportar para CSV.");
            return;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(ARQUIVO_CSV), StandardCharsets.UTF_8)) {
            // BOM UTF-8 para abrir com acentuação perfeita diretamente no Microsoft Excel
            writer.write('\uFEFF');
            writer.write("ID_Projeto;Titulo;Cliente;Categoria;Consultor;Especializacao;Progresso(%);Valor_Total(R$);Etapas_Atrasadas\n");

            for (Projeto p : banco.projetos) {
                String nomeCliente = banco.clientes.stream()
                        .filter(c -> c.getId().equals(p.getClienteId()))
                        .map(Cliente::getNome)
                        .findFirst()
                        .orElse("N/A");
                String categoria = banco.clientes.stream()
                        .filter(c -> c.getId().equals(p.getClienteId()))
                        .map(Cliente::getCategoriaTexto)
                        .findFirst()
                        .orElse("Regular");
                String nomeConsultor = banco.consultores.stream()
                        .filter(con -> con.getId().equals(p.getConsultorId()))
                        .map(Consultor::getNome)
                        .findFirst()
                        .orElse("Pendente");

                writer.write(String.format("%s;%s;%s;%s;%s;%s;%.1f;%.2f;%d%n",
                        p.getId(),
                        p.getTitulo().replace(";", ","),
                        nomeCliente.replace(";", ","),
                        categoria,
                        nomeConsultor.replace(";", ","),
                        p.getArea().getDescricao(),
                        p.calcularProgresso(),
                        p.calcularValorTotal(),
                        p.contarEtapasAtrasadas()));
            }

            System.out.println("[CSV EXCEL] Planilha exportada com sucesso em '" + ARQUIVO_CSV + "'!");
            abrirArquivoSeSuportado(new File(ARQUIVO_CSV));
        } catch (IOException e) {
            System.out.println("[ERRO CSV] Falha ao exportar planilha: " + e.getMessage());
        }
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
