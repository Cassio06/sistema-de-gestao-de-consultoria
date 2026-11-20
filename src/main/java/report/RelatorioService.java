package report;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import model.ModelosSistema.*;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String LEGENDA_PROGRESSO =
            "O progresso por etapa indica conclusão (100% concluída e 0% em aberto).";

    // Cores legíveis para destaque visual no PDF
    private static final DeviceRgb COR_VERMELHO = new DeviceRgb(200, 30, 30);
    private static final DeviceRgb COR_VERDE = new DeviceRgb(22, 130, 56);
    private static final DeviceRgb COR_LARANJA = new DeviceRgb(204, 112, 0);
    private static final DeviceRgb COR_AZUL_VIP = new DeviceRgb(25, 85, 166);
    private final Path diretorio;

    public RelatorioService() {
        this(Path.of("relatorios_pdf"));
    }

    public RelatorioService(Path diretorio) {
        this.diretorio = diretorio;
    }

    public void exibirRelatorioConsole(Projeto projeto, Cliente cliente, Consultor consultor) {
        System.out.println("\n=======================================================");
        System.out.println("      PLANILHA CERTA - RELATÓRIO DE DESEMPENHO");
        System.out.println("=======================================================");
        System.out.printf("Projeto: %s (%s)%n", projeto.getTitulo(), projeto.getId());
        System.out.printf("Área: %s%n", projeto.getArea().getDescricao());
        System.out.printf("Cliente: %s [%s] | Pontos Fidelidade: %d%n",
                cliente.getNome(), cliente.getCategoriaTexto(), cliente.getPontosFidelidade());
        System.out.printf("Consultor Responsável: %s%n", consultor != null ? consultor.getNome() : "Não alocado");
        System.out.printf("Progresso Geral: %.1f%% | Valor Total: R$ %.2f%n",
                projeto.calcularProgresso(), projeto.calcularValorTotal());
        System.out.println("-------------------------------------------------------");
        System.out.println("ETAPAS DO PROJETO:");

        for (Etapa etapa : projeto.getEtapas()) {
            StatusEtapa status = etapa.getStatusAtualizado();
            System.out.printf(" - %-22s | Prazo: %s | R$ %8.2f | Status: %s | Progresso: %d%%%n",
                    etapa.getNome(),
                    etapa.getPrazo().format(FMT),
                    etapa.getValor(),
                    status,
                    progressoDaEtapa(status));
        }
        System.out.println(LEGENDA_PROGRESSO);

        // Requisito: Seção adicional de recomendações apenas para clientes VIP
        if (cliente.isVip()) {
            System.out.println("-------------------------------------------------------");
            System.out.println("★ SEÇÃO EXCLUSIVA VIP - RECOMENDAÇÕES DE SERVIÇOS:");
            List<Especializacao> recomendacoes = cliente.gerarRecomendacoes();
            if (recomendacoes.isEmpty()) {
                System.out.println("   Você já contratou todas as nossas especialidades! Aproveite 10% de desconto VIP contínuo.");
            } else {
                for (Especializacao esp : recomendacoes) {
                    System.out.println("   -> Conheça nossa consultoria em: " + esp.getDescricao());
                }
            }
        }
        System.out.println("=======================================================\n");
    }

    public void gerarRelatorioPDF(Projeto projeto, Cliente cliente, Consultor consultor) {
        try {
            String nome = String.format("Relatorio_%s_%s.pdf",
                    sanitizarNomeArquivo(projeto.getId()), sanitizarNomeArquivo(cliente.getNome()));
            Path destino = diretorio.resolve(nome);
            Files.createDirectories(diretorio);
            Path temporario = Files.createTempFile(diretorio, "relatorio-", ".tmp");
            try {
                escreverPDF(temporario, projeto, cliente, consultor);
                try {
                    Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporario);
            }
            System.out.println("[PDF iText] Relatório gerado com sucesso em: " + destino);
            abrirPdfNaTela(destino.toFile());
        } catch (IOException | RuntimeException e) {
            System.out.println("[ERRO PDF] Não foi possível gerar o PDF: " + e.getMessage());
        }
    }

    private void escreverPDF(Path arquivo, Projeto projeto, Cliente cliente, Consultor consultor) throws IOException {
        try (Document document = new Document(new PdfDocument(new PdfWriter(arquivo.toString())))) {
            // Cabeçalho
            Paragraph titulo = new Paragraph("PLANILHA CERTA - CONSULTORIA EMPRESARIAL")
                    .setBold()
                    .setFontSize(16)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(titulo);

            document.add(new Paragraph("Relatório Oficial de Desempenho de Projeto")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(15));

            // Dados Gerais
            document.add(new Paragraph("Dados do Contrato e Equipe").setBold().setFontSize(12));
            document.add(new Paragraph("Projeto: " + projeto.getTitulo() + " (ID: " + projeto.getId() + ")"));
            document.add(new Paragraph("Especialização: " + projeto.getArea().getDescricao()));
            document.add(new Paragraph("Cliente: " + cliente.getNome() + " | Categoria: " + cliente.getCategoriaTexto()));
            document.add(new Paragraph("Consultor Alocado: " + (consultor != null ? consultor.getNome() : "Pendente")));
            document.add(new Paragraph(String.format("Progresso Atual: %.1f%% | Valor Total: R$ %.2f",
                    projeto.calcularProgresso(), projeto.calcularValorTotal())).setMarginBottom(15));

            // Tabela de Etapas com destaque colorido por status
            document.add(new Paragraph("Detalhamento das Etapas").setBold().setFontSize(12).setMarginBottom(5));
            Table tabela = new Table(UnitValue.createPercentArray(new float[]{3.6f, 1.9f, 1.8f, 2.6f, 1.7f}))
                    .useAllAvailableWidth();

            String[] cabecalhos = {"Etapa", "Prazo", "Valor (R$)", "Status", "Progresso"};
            for (String h : cabecalhos) {
                tabela.addHeaderCell(new Cell().add(new Paragraph(h).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY));
            }

            for (Etapa e : projeto.getEtapas()) {
                tabela.addCell(e.getNome());
                tabela.addCell(e.getPrazo().format(FMT));
                tabela.addCell(String.format("R$ %.2f", e.getValor()));

                StatusEtapa status = e.getStatusAtualizado();
                Paragraph pStatus = new Paragraph(status.name()).setBold();
                if (status == StatusEtapa.ATRASADA) {
                    pStatus.setFontColor(COR_VERMELHO);
                } else if (status == StatusEtapa.CONCLUIDA) {
                    pStatus.setFontColor(COR_VERDE);
                } else if (status == StatusEtapa.EM_ANDAMENTO) {
                    pStatus.setFontColor(COR_LARANJA);
                }
                tabela.addCell(new Cell().add(pStatus));
                tabela.addCell(progressoDaEtapa(status) + "%");
            }
            document.add(tabela);
            document.add(new Paragraph(LEGENDA_PROGRESSO).setFontSize(9).setMarginTop(4));

            // Seção Extra Exclusiva para VIP
            if (cliente.isVip()) {
                document.add(new Paragraph("\nRecomendação Personalizada (Benefício Cliente VIP)")
                        .setBold()
                        .setFontSize(12)
                        .setFontColor(COR_AZUL_VIP));

                List<Especializacao> recomendacoes = cliente.gerarRecomendacoes();
                if (recomendacoes.isEmpty()) {
                    document.add(new Paragraph("Parabéns! Sua empresa já utiliza todo o ecossistema da Planilha Certa."));
                } else {
                    document.add(new Paragraph("Com base no seu histórico, recomendamos expandir seus resultados com:"));
                    for (Especializacao esp : recomendacoes) {
                        document.add(new Paragraph("• " + esp.getDescricao() + " (10% de desconto VIP aplicável)"));
                    }
                }
            }

            document.add(new Paragraph("\nDocumento gerado em: " + LocalDate.now().format(FMT))
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.RIGHT));
        }
    }

    private static int progressoDaEtapa(StatusEtapa status) {
        return status == StatusEtapa.CONCLUIDA ? 100 : 0;
    }

    private void abrirPdfNaTela(File arquivoPdf) {
        try {
            if (Desktop.isDesktopSupported() && arquivoPdf.exists()) {
                Desktop.getDesktop().open(arquivoPdf);
                System.out.println("[PDF iText] Abrindo relatório automaticamente no visualizador padrão...");
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            System.out.println("[INFO] Não foi possível abrir o PDF automaticamente. Acesse " + arquivoPdf);
        }
    }

    private String sanitizarNomeArquivo(String texto) {
        if (texto == null || texto.isBlank()) {
            return "sem_nome";
        }
        String semAcentos = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcentos.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
