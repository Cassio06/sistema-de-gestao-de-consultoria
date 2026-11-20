package br.com.planilhacerta.app;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Path dados = Path.of(System.getProperty("consultoria.dados", "data"));
        Path pdfs = Path.of(System.getProperty("consultoria.relatorios", "relatorios_pdf"));
        try {
            new MenuIntegrado(new SistemaConsultoria(dados, pdfs)).iniciar();
        } catch (IllegalStateException e) {
            System.err.println("Não foi possível iniciar o sistema. " + e.getMessage());
            System.exit(1);
        }
    }
}
