
package br.com.planilhacerta.projetos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TesteProjetos {

    public static void main(String[] args) {

        // Data fixa para reproduzir os testes
        LocalDate hoje = LocalDate.of(2026, 11, 19);

        // Lista de projetos
        List<Projeto> projetos = new ArrayList<>();

        // ===== PROJETO 1 =====
        Projeto projeto1 = new Projeto(
                1, 101, 201,
                "Consultoria Financeira",
                "Organização financeira empresarial",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );

        Etapa analise1 = new Etapa(
                "Análise inicial",
                LocalDate.of(2026, 11, 10),
                new BigDecimal("500.00")
        );

        Etapa implementacao1 = new Etapa(
                "Implementação",
                LocalDate.of(2026, 11, 21),
                new BigDecimal("1000.00")
        );

        Etapa revisao1 = new Etapa(
                "Revisão final",
                LocalDate.of(2026, 11, 28),
                new BigDecimal("500.00")
        );

        projeto1.adicionarEtapa(analise1);
        projeto1.adicionarEtapa(implementacao1);
        projeto1.adicionarEtapa(revisao1);

        analise1.concluir(LocalDate.of(2026, 11, 9));
        implementacao1.iniciar();

        projetos.add(projeto1);

        // ===== PROJETO 2 =====
        Projeto projeto2 = new Projeto(
                2, 102, 202,
                "Consultoria em TI",
                "Modernização de sistemas",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );

        Etapa analise2 = new Etapa(
                "Análise inicial",
                LocalDate.of(2026, 11, 5),
                new BigDecimal("700.00")
        );

        Etapa implementacao2 = new Etapa(
                "Implementação",
                LocalDate.of(2026, 11, 12),
                new BigDecimal("1500.00")
        );

        Etapa revisao2 = new Etapa(
                "Revisão final",
                LocalDate.of(2026, 11, 25),
                new BigDecimal("800.00")
        );

        projeto2.adicionarEtapa(analise2);
        projeto2.adicionarEtapa(implementacao2);
        projeto2.adicionarEtapa(revisao2);

        analise2.concluir(LocalDate.of(2026, 11, 5));
        implementacao2.iniciar();

        projetos.add(projeto2);

        // ===== PROJETO 3 =====
        Projeto projeto3 = new Projeto(
                3, 103, 203,
                "Consultoria de Gestão",
                "Melhoria dos processos internos",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 18)
        );

        Etapa analise3 = new Etapa(
                "Análise inicial",
                LocalDate.of(2026, 11, 5),
                new BigDecimal("400.00")
        );

        Etapa implementacao3 = new Etapa(
                "Implementação",
                LocalDate.of(2026, 11, 12),
                new BigDecimal("800.00")
        );

        Etapa revisao3 = new Etapa(
                "Revisão final",
                LocalDate.of(2026, 11, 18),
                new BigDecimal("400.00")
        );

        projeto3.adicionarEtapa(analise3);
        projeto3.adicionarEtapa(implementacao3);
        projeto3.adicionarEtapa(revisao3);

        analise3.concluir(LocalDate.of(2026, 11, 4));
        implementacao3.concluir(LocalDate.of(2026, 11, 11));
        revisao3.concluir(LocalDate.of(2026, 11, 18));

        projetos.add(projeto3);

        // ===== MONITORAMENTO =====
        MonitoramentoProjetos monitor =
                new MonitoramentoProjetos();

        for (Projeto projeto : projetos) {
            monitor.verificarPrazos(projeto, hoje);
        }

        // ===== PAINEL DE DESEMPENHO =====
        monitor.exibirDesempenho(projetos, hoje);

        // ===== VALORES DOS PROJETOS =====
        System.out.println("\n===== VALORES =====");

        for (Projeto projeto : projetos) {
            System.out.println(
                    projeto.getNome() + ": R$ "
                            + projeto.getValorTotal()
            );
        }

        // ===== TESTES DE VALIDAÇÃO =====
        System.out.println("\n===== TESTES DE VALIDAÇÃO =====");

        try {
            new Etapa(
                    "Etapa inválida",
                    hoje,
                    new BigDecimal("-100")
            );
            System.out.println("[FALHOU] Valor negativo");
        } catch (IllegalArgumentException e) {
            System.out.println("[OK] Valor negativo");
        }


        try {
            Projeto projetoTeste = new Projeto(
                    99, 101, 201,
                    "Projeto com quatro etapas",
                    "Teste de quantidade de etapas",
                    LocalDate.of(2026, 11, 1),
                    LocalDate.of(2026, 11, 30)
            );

            for (int i = 1; i <= 4; i++) {
                Etapa etapa = new Etapa(
                        "Etapa " + i,
                        LocalDate.of(2026, 11, 20 + i),
                        new BigDecimal("100.00")
                );

                projetoTeste.adicionarEtapa(etapa);
            }

            if (projetoTeste.getEtapas().size() == 4) {
                System.out.println("[OK] Projeto aceita quatro etapas");
            } else {
                System.out.println("[FALHOU] Projeto aceita quatro etapas");
            }

        } catch (Exception e) {
            System.out.println("[FALHOU] Projeto aceita quatro etapas");
            System.out.println("Motivo: " + e.getMessage());
        }

        try {
            analise1.concluir(hoje);
            System.out.println("[FALHOU] Conclusão duplicada");
        } catch (IllegalStateException e) {
            System.out.println("[OK] Conclusão duplicada");
        }

        System.out.println("\nTestes finalizados.");
    }
}
