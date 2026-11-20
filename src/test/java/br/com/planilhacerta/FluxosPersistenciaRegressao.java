package br.com.planilhacerta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import model.ModelosSistema.Cliente;
import model.ModelosSistema.Especializacao;
import model.ModelosSistema.Etapa;
import model.ModelosSistema.Projeto;
import model.ModelosSistema.StatusEtapa;
import report.RelatorioService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FluxosPersistenciaRegressao {
    private static final String PRINCIPAL = "dados_planilha_certa.json";
    private static final String BACKUP = "dados_planilha_certa.bak.json";
    private static final String LEGADO = """
            {"clientes":[{"id":"CLI-1","nome":"Recuperado","email":"teste@example.com","telefone":"123"}],
             "consultores":[],"projetos":[]}
            """;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path raiz = Files.createTempDirectory("consultoria-test-");
        recuperarBackup(raiz.resolve("data-invalida"), LEGADO.replace("\"projetos\":[]", """
                "projetos":[{"id":"PRJ-1","titulo":"Teste","clienteId":"CLI-1","area":"TI",
                "etapas":[{"nome":"Etapa","prazo":"2026-02-30","valor":100,"status":"PENDENTE"}]}]
                """));
        recuperarBackup(raiz.resolve("campo-ausente"), "{}");
        recuperarBackup(raiz.resolve("projeto-orfao"), LEGADO.replace("\"projetos\":[]", """
                "projetos":[{"id":"PRJ-1","titulo":"Teste","clienteId":"CLI-2","area":"TI",
                "etapas":[{"nome":"Etapa","prazo":"2099-03-01","valor":100,"status":"PENDENTE"}]}]
                """));
        migrarBaseDaRaiz(raiz.resolve("migracao"));
        testarMenuERelatorios(raiz.resolve("relatorios"));
        preservarPDFEmFalhaDeGeracao(raiz.resolve("pdf-anterior"));
        impedirSobrescritaDeOutraSessao(raiz.resolve("duas-sessoes"));
        try (var arquivos = Files.walk(raiz)) {
            for (Path arquivo : arquivos.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(arquivo);
            }
        }
        System.out.println("Passaram 7 cenários de persistência, menu e relatórios.");
    }

    private static void recuperarBackup(Path pasta, String principal) throws Exception {
        Path data = Files.createDirectories(pasta.resolve("data"));
        Files.writeString(data.resolve(PRINCIPAL), principal);
        Files.writeString(data.resolve(BACKUP), LEGADO);
        executarMenu(pasta, "0");
        JsonObject recuperada = lerBase(pasta);
        exigir(recuperada.getAsJsonArray("projetos").isEmpty(), "O projeto inválido permaneceu na base recuperada");
        JsonObject cliente = recuperada.getAsJsonArray("clientes").get(0).getAsJsonObject();
        exigir("Recuperado".equals(cliente.get("nome").getAsString()), "O backup deveria ser carregado");
        exigir(LEGADO.equals(Files.readString(data.resolve(BACKUP))), "O backup válido foi alterado durante a recuperação");
    }

    private static void migrarBaseDaRaiz(Path pasta) throws Exception {
        Files.createDirectory(pasta);
        Path baseAntiga = pasta.resolve(PRINCIPAL);
        Files.writeString(baseAntiga, LEGADO);
        executarMenu(pasta, "0");
        JsonObject cliente = lerBase(pasta).getAsJsonArray("clientes").get(0).getAsJsonObject();
        exigir("Recuperado".equals(cliente.get("nome").getAsString()), "A base da raiz deveria ser migrada para data/");
        exigir(LEGADO.equals(Files.readString(baseAntiga)), "A migração alterou o arquivo original");
    }

    private static void testarMenuERelatorios(Path pasta) throws Exception {
        Files.createDirectory(pasta);
        Files.writeString(pasta.resolve(".env"), """
                GOOGLE_DRIVE_ENABLED=false
                GOOGLE_APPLICATION_CREDENTIALS=credentials.json
                """);
        Files.writeString(pasta.resolve("credentials.json"), "{}");
        String saida = executarMenu(pasta, "inválido", "9", "9", "6", "1", "", "7", "0");
        exigir(saida.contains("[ERRO]"), "O menu deveria pedir novamente um número inválido");
        exigir(!saida.contains("[GOOGLE DRIVE]"), "O Drive foi usado com o backup desativado");
        exigir(lerBase(pasta).getAsJsonArray("clientes").size() == 1, "A demonstração foi duplicada");
        exigir(saida.contains("Progresso Geral"), "Relatório de desempenho ausente no console");
        exigir(saida.contains("Status: CONCLUIDA | Progresso: 100%")
                        && saida.contains("Status: ATRASADA | Progresso: 0%")
                        && saida.contains("Status: PENDENTE | Progresso: 0%"),
                "O console deve mostrar o progresso de cada etapa");
        exigir(Files.exists(pasta.resolve("data/relatorio_planilha_certa.csv")), "CSV não gerado");

        Path pdf = pasta.resolve("relatorios_pdf/Relatorio_PRJ-DEMO1_TechSolutions_Ltda.pdf");
        try (PdfDocument documento = new PdfDocument(new PdfReader(pdf.toString()))) {
            String texto = PdfTextExtractor.getTextFromPage(documento.getPage(1));
            exigir(texto.contains("Benefício Cliente VIP") && texto.contains("Gestão Empresarial"),
                    "O PDF deveria incluir recomendações para VIP");
            exigir(Pattern.compile("(?s)CONCLUIDA\\s+100%.*ATRASADA\\s+0%.*PENDENTE\\s+0%").matcher(texto).find(),
                    "O PDF deve mostrar o progresso de cada etapa");
        }
    }

    private static void impedirSobrescritaDeOutraSessao(Path pasta) throws Exception {
        Files.createDirectory(pasta);
        Path saida = pasta.resolve("sessao-antiga.txt");
        Process antiga = montarProcesso(pasta, saida).start();
        try {
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (!lerSaida(saida).contains("Escolha uma") && antiga.isAlive() && System.nanoTime() < limite) {
                Thread.sleep(20);
            }
            exigir(lerSaida(saida).contains("Escolha uma"), "A sessão antiga não abriu a base");
            executarMenu(pasta, "9", "0");
            try (var entrada = antiga.getOutputStream()) {
                entrada.write("8\n0\n".getBytes(StandardCharsets.UTF_8));
            }
            String texto = concluirProcesso(antiga, saida);
            exigir(texto.contains("outra sess"), "A sessão desatualizada deveria rejeitar a gravação");
            exigir(lerBase(pasta).getAsJsonArray("clientes").size() == 1, "O cadastro mais recente foi sobrescrito");
        } finally {
            if (antiga.isAlive()) {
                antiga.destroyForcibly();
            }
        }
    }

    private static void preservarPDFEmFalhaDeGeracao(Path pasta) throws Exception {
        Cliente cliente = new Cliente("CLI-1", "Cliente Regular", "demo@example.test", "000", false);
        Projeto projeto = new Projeto("PRJ-1", "Diagnóstico", cliente.getId(), null, Especializacao.TI);
        Etapa analise = new Etapa("Análise", LocalDate.now(), 100);
        analise.setStatus(StatusEtapa.EM_ANDAMENTO);
        projeto.getEtapas().add(analise);

        RelatorioService relatorios = new RelatorioService(pasta);
        relatorios.gerarRelatorioPDF(projeto, cliente, null);
        Path pdf = pasta.resolve("Relatorio_PRJ-1_Cliente_Regular.pdf");
        byte[] original = Files.readAllBytes(pdf);
        try (PdfDocument documento = new PdfDocument(new PdfReader(pdf.toString()))) {
            String texto = PdfTextExtractor.getTextFromPage(documento.getPage(1));
            exigir(!texto.contains("Benefício Cliente VIP"), "Cliente regular recebeu a seção VIP");
            exigir(Pattern.compile("EM_ANDAMENTO\\s+0%").matcher(texto).find(),
                    "O PDF deve identificar a etapa em andamento como ainda não concluída");
        }

        projeto.getEtapas().add(new Etapa("Sem prazo", null, 100));
        relatorios.gerarRelatorioPDF(projeto, cliente, null);
        exigir(Arrays.equals(original, Files.readAllBytes(pdf)), "A falha alterou o PDF válido anterior");
        try (var arquivos = Files.list(pasta)) {
            exigir(arquivos.count() == 1, "A falha deixou um arquivo temporário");
        }
    }

    private static JsonObject lerBase(Path pasta) throws Exception {
        return JsonParser.parseString(Files.readString(pasta.resolve("data").resolve(PRINCIPAL))).getAsJsonObject();
    }

    private static String executarMenu(Path pasta, String... entradas) throws Exception {
        Path entrada = pasta.resolve("entrada.txt");
        Path saida = pasta.resolve("saida.txt");
        Files.writeString(entrada, String.join("\n", entradas) + "\n");
        Process processo = montarProcesso(pasta, saida).redirectInput(entrada.toFile()).start();
        return concluirProcesso(processo, saida);
    }

    private static ProcessBuilder montarProcesso(Path pasta, Path saida) {
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                .map(caminho -> Path.of(caminho).toAbsolutePath().toString())
                .collect(Collectors.joining(File.pathSeparator));
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-cp", classpath, "ui.MenuPrincipal").directory(pasta.toFile())
                .redirectErrorStream(true).redirectOutput(saida.toFile());
        if (Files.exists(pasta.resolve(".env"))) {
            builder.environment().remove("GOOGLE_DRIVE_ENABLED");
            builder.environment().remove("GOOGLE_APPLICATION_CREDENTIALS");
            builder.environment().remove("GOOGLE_CREDENTIALS_PATH");
        } else {
            builder.environment().put("GOOGLE_DRIVE_ENABLED", "false");
        }
        return builder;
    }

    private static String concluirProcesso(Process processo, Path saida) throws Exception {
        if (!processo.waitFor(20, TimeUnit.SECONDS)) {
            processo.destroyForcibly();
            throw new AssertionError("O menu não encerrou em " + saida.getParent());
        }
        String texto = lerSaida(saida);
        exigir(processo.exitValue() == 0, "Falha do menu em " + saida.getParent() + "\n" + texto);
        return texto;
    }

    private static String lerSaida(Path saida) throws Exception {
        return new String(Files.readAllBytes(saida), StandardCharsets.UTF_8);
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
