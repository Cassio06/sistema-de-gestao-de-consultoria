package br.com.planilhacerta.app;

import br.com.planilhacerta.app.SistemaConsultoria.Estado;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import persistence.GoogleDriveService;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class RepositorioIntegrado {
    private static final int VERSAO = 1;
    private static final Gson JSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter escritor, LocalDate data) throws IOException {
                    escritor.value(data.toString());
                }

                @Override
                public LocalDate read(JsonReader leitor) throws IOException {
                    if (leitor.peek() != JsonToken.STRING) {
                        throw new JsonSyntaxException("Data inválida");
                    }
                    return LocalDate.parse(leitor.nextString());
                }
            }.nullSafe())
            .create();

    private final Path diretorio;
    private final Path principal;
    private final Path backup;
    private final GoogleDriveService drive = new GoogleDriveService();
    private byte[] revisaoPrincipal;

    public RepositorioIntegrado(Path diretorio) {
        this.diretorio = Objects.requireNonNull(diretorio).toAbsolutePath().normalize();
        principal = this.diretorio.resolve("consultoria.json");
        backup = this.diretorio.resolve("consultoria.bak.json");
    }

    public synchronized Optional<Estado> carregar() {
        try {
            Files.createDirectories(diretorio);
            try (FileChannel canal = abrirCanal(); FileLock bloqueio = canal.lock()) {
                return carregarComBloqueio();
            }
        } catch (IOException | OverlappingFileLockException e) {
            throw new IllegalStateException("Falha ao abrir a base integrada em " + diretorio + ". " + e.getMessage(), e);
        }
    }

    private Optional<Estado> carregarComBloqueio() throws IOException {
        byte[] conteudo = lerBytes(principal);
        JsonParseException erroPrincipal = null;
        if (conteudo != null) {
            try {
                Estado estado = lerEstado(conteudo);
                revisaoPrincipal = revisao(conteudo);
                return Optional.of(estado);
            } catch (JsonParseException e) {
                erroPrincipal = e;
            }
        }

        byte[] conteudoBackup = lerBytes(backup);
        if (conteudoBackup == null) {
            if (erroPrincipal != null) {
                throw new IOException("O arquivo consultoria.json é inválido e não há backup.", erroPrincipal);
            }
            revisaoPrincipal = null;
            return Optional.empty();
        }

        Estado estado;
        try {
            estado = lerEstado(conteudoBackup);
        } catch (JsonParseException e) {
            IOException falha = new IOException("Não há uma cópia válida da base integrada.", e);
            if (erroPrincipal != null) {
                falha.addSuppressed(erroPrincipal);
            }
            throw falha;
        }
        if (conteudo != null) {
            preservarCorrompido(conteudo);
        }
        gravarAtomico(principal, conteudoBackup);
        revisaoPrincipal = revisao(conteudoBackup);
        System.out.println("[RECUPERAÇÃO] Base integrada restaurada de " + backup.getFileName() + ".");
        return Optional.of(estado);
    }

    public synchronized void salvar(Estado estado) throws IOException {
        byte[] novo = serializar(estado);
        Files.createDirectories(diretorio);
        try (FileChannel canal = abrirCanal(); FileLock bloqueio = canal.lock()) {
            byte[] atual = lerBytes(principal);
            if (!Arrays.equals(revisaoPrincipal, revisao(atual))) {
                throw new IOException("A base integrada foi alterada por outra sessão. Reabra o sistema antes de salvar.");
            }
            if (!Arrays.equals(atual, novo)) {
                if (atual != null) {
                    gravarAtomico(backup, atual);
                }
                gravarAtomico(principal, novo);
                revisaoPrincipal = revisao(novo);
            }
        } catch (OverlappingFileLockException e) {
            throw new IOException("A base integrada está sendo usada por outra sessão.", e);
        }
        try {
            drive.enviarBackup(principal.toFile());
        } catch (RuntimeException e) {
            System.out.println("[GOOGLE DRIVE] Falha na sincronização (gravação local concluída): " + e.getMessage());
        }
    }

    private static byte[] serializar(Estado estado) throws IOException {
        try {
            SistemaConsultoria.validar(Objects.requireNonNull(estado));
            JsonObject documento = new JsonObject();
            documento.addProperty("versao", VERSAO);
            documento.add("estado", JSON.toJsonTree(estado));
            return JSON.toJson(documento).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw new IOException("O estado integrado não pode ser gravado. " + e.getMessage(), e);
        }
    }

    private static Estado lerEstado(byte[] conteudo) throws IOException {
        JsonObject documento;
        try {
            String texto = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(conteudo)).toString();
            try (JsonReader leitor = new JsonReader(new StringReader(texto))) {
                JsonElement valor = lerJson(leitor, 0);
                if (!valor.isJsonObject() || leitor.peek() != JsonToken.END_DOCUMENT) {
                    throw new JsonSyntaxException("Estrutura JSON inválida");
                }
                documento = valor.getAsJsonObject();
            }
        } catch (IOException | NumberFormatException e) {
            throw new JsonSyntaxException("JSON inválido", e);
        }

        JsonElement versao = documento.get("versao");
        if (!numero(versao)) {
            throw new JsonSyntaxException("Versão ausente ou inválida");
        }
        if (versao.getAsBigDecimal().compareTo(BigDecimal.valueOf(VERSAO)) != 0) {
            throw new IOException("Versão da base integrada não suportada: " + versao);
        }
        JsonElement estadoJson = documento.get("estado");
        if (documento.size() != 2 || estadoJson == null || !estadoJson.isJsonObject()) {
            throw new JsonSyntaxException("Estrutura da base integrada inválida");
        }

        try {
            Estado estado = JSON.fromJson(estadoJson, Estado.class);
            // Gson aceita campos ausentes e coerções de tipos. A comparação exige o formato gravado pela aplicação.
            if (!jsonEquivalente(estadoJson, JSON.toJsonTree(estado))) {
                throw new JsonSyntaxException("Campos ou tipos inválidos no estado integrado");
            }
            SistemaConsultoria.validar(estado);
            return estado;
        } catch (RuntimeException e) {
            throw new JsonSyntaxException("Estado integrado inválido. " + e.getMessage(), e);
        }
    }

    private static JsonElement lerJson(JsonReader leitor, int profundidade) throws IOException {
        if (profundidade > 64) {
            throw new JsonSyntaxException("Estrutura JSON profunda demais");
        }
        return switch (leitor.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject objeto = new JsonObject();
                leitor.beginObject();
                while (leitor.hasNext()) {
                    String campo = leitor.nextName();
                    if (objeto.has(campo)) {
                        throw new JsonSyntaxException("Campo JSON repetido: " + campo);
                    }
                    objeto.add(campo, lerJson(leitor, profundidade + 1));
                }
                leitor.endObject();
                yield objeto;
            }
            case BEGIN_ARRAY -> {
                JsonArray valores = new JsonArray();
                leitor.beginArray();
                while (leitor.hasNext()) {
                    valores.add(lerJson(leitor, profundidade + 1));
                }
                leitor.endArray();
                yield valores;
            }
            case NUMBER -> new JsonPrimitive(new BigDecimal(leitor.nextString()));
            case STRING -> new JsonPrimitive(leitor.nextString());
            case BOOLEAN -> new JsonPrimitive(leitor.nextBoolean());
            case NULL -> {
                leitor.nextNull();
                yield JsonNull.INSTANCE;
            }
            default -> throw new JsonSyntaxException("Valor JSON ausente ou inválido");
        };
    }

    private static boolean jsonEquivalente(JsonElement esperado, JsonElement obtido) {
        if (esperado.isJsonObject() && obtido.isJsonObject()) {
            JsonObject objetoEsperado = esperado.getAsJsonObject();
            JsonObject objetoObtido = obtido.getAsJsonObject();
            return objetoEsperado.keySet().equals(objetoObtido.keySet()) && objetoEsperado.entrySet().stream()
                    .allMatch(campo -> jsonEquivalente(campo.getValue(), objetoObtido.get(campo.getKey())));
        }
        if (esperado.isJsonArray() && obtido.isJsonArray()) {
            JsonArray valoresEsperados = esperado.getAsJsonArray();
            JsonArray valoresObtidos = obtido.getAsJsonArray();
            if (valoresEsperados.size() != valoresObtidos.size()) {
                return false;
            }
            for (int i = 0; i < valoresEsperados.size(); i++) {
                if (!jsonEquivalente(valoresEsperados.get(i), valoresObtidos.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (numero(esperado) && numero(obtido)) {
            return esperado.getAsBigDecimal().compareTo(obtido.getAsBigDecimal()) == 0;
        }
        return esperado.equals(obtido);
    }

    private static boolean numero(JsonElement valor) {
        return valor != null && valor.isJsonPrimitive() && valor.getAsJsonPrimitive().isNumber();
    }

    private FileChannel abrirCanal() throws IOException {
        return FileChannel.open(diretorio.resolve("consultoria.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    private static byte[] lerBytes(Path caminho) throws IOException {
        try {
            return Files.readAllBytes(caminho);
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    private static byte[] revisao(byte[] conteudo) {
        if (conteudo == null) {
            return null;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(conteudo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private void preservarCorrompido(byte[] conteudo) throws IOException {
        Path copia = Files.createTempFile(diretorio, "consultoria-corrompida-", ".json");
        try {
            Files.write(copia, conteudo);
        } catch (IOException e) {
            Files.deleteIfExists(copia);
            throw e;
        }
    }

    private void gravarAtomico(Path destino, byte[] conteudo) throws IOException {
        Path temporario = Files.createTempFile(diretorio, "consultoria-", ".tmp");
        boolean movido = false;
        try {
            Files.write(temporario, conteudo);
            try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
                canal.force(true);
            }
            Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            movido = true;
        } finally {
            if (!movido) {
                Files.deleteIfExists(temporario);
            }
        }
    }
}
