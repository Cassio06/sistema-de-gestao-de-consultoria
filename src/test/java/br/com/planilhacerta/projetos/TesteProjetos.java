
package br.com.planilhacerta.projetos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TesteProjetos {

    public static void main(String[] args) {

        // Datas relativas para repetir o cenário em qualquer dia.
        LocalDate hoje = LocalDate.now();

        // Lista de projetos
        List<Projeto> projetos = new ArrayList<>();

        // ===== PROJETO 1 =====
        Projeto projeto1 = new Projeto(
                1, 101, 201,
                "Consultoria Financeira",
                "Organização financeira empresarial",
                hoje.minusDays(18),
                hoje.plusDays(11)
        );

        Etapa analise1 = new Etapa(
                "Análise inicial",
                hoje.minusDays(9),
                new BigDecimal("500.00")
        );

        Etapa implementacao1 = new Etapa(
                "Implementação",
                hoje.plusDays(2),
                new BigDecimal("1000.00")
        );

        Etapa revisao1 = new Etapa(
                "Revisão final",
                hoje.plusDays(9),
                new BigDecimal("500.00")
        );

        projeto1.adicionarEtapa(analise1);
        projeto1.adicionarEtapa(implementacao1);
        projeto1.adicionarEtapa(revisao1);

        analise1.concluir(hoje.minusDays(10));
        implementacao1.iniciar();

        projetos.add(projeto1);

        // ===== PROJETO 2 =====
        Projeto projeto2 = new Projeto(
                2, 102, 202,
                "Consultoria em TI",
                "Modernização de sistemas",
                hoje.minusDays(18),
                hoje.plusDays(11)
        );

        Etapa analise2 = new Etapa(
                "Análise inicial",
                hoje.minusDays(14),
                new BigDecimal("700.00")
        );

        Etapa implementacao2 = new Etapa(
                "Implementação",
                hoje.minusDays(7),
                new BigDecimal("1500.00")
        );

        Etapa revisao2 = new Etapa(
                "Revisão final",
                hoje.plusDays(6),
                new BigDecimal("800.00")
        );

        projeto2.adicionarEtapa(analise2);
        projeto2.adicionarEtapa(implementacao2);
        projeto2.adicionarEtapa(revisao2);

        analise2.concluir(hoje.minusDays(14));
        implementacao2.iniciar();

        projetos.add(projeto2);

        // ===== PROJETO 3 =====
        Projeto projeto3 = new Projeto(
                3, 103, 203,
                "Consultoria de Gestão",
                "Melhoria dos processos internos",
                hoje.minusDays(18),
                hoje.minusDays(1)
        );

        Etapa analise3 = new Etapa(
                "Análise inicial",
                hoje.minusDays(14),
                new BigDecimal("400.00")
        );

        Etapa implementacao3 = new Etapa(
                "Implementação",
                hoje.minusDays(7),
                new BigDecimal("800.00")
        );

        Etapa revisao3 = new Etapa(
                "Revisão final",
                hoje.minusDays(1),
                new BigDecimal("400.00")
        );

        projeto3.adicionarEtapa(analise3);
        projeto3.adicionarEtapa(implementacao3);
        projeto3.adicionarEtapa(revisao3);

        analise3.concluir(hoje.minusDays(15));
        implementacao3.concluir(hoje.minusDays(8));
        revisao3.concluir(hoje.minusDays(1));

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
            throw new AssertionError("Valor negativo");
        } catch (IllegalArgumentException e) {
            System.out.println("[OK] Valor negativo");
        }


        try {
            Projeto projetoTeste = new Projeto(
                    99, 101, 201,
                    "Projeto com quatro etapas",
                    "Teste de quantidade de etapas",
                    hoje.minusDays(18),
                    hoje.plusDays(11)
            );

            for (int i = 1; i <= 4; i++) {
                Etapa etapa = new Etapa(
                        "Etapa " + i,
                        hoje.plusDays(i + 1),
                        new BigDecimal("100.00")
                );

                projetoTeste.adicionarEtapa(etapa);
            }

            if (projetoTeste.getEtapas().size() == 4) {
                System.out.println("[OK] Projeto aceita quatro etapas");
            } else {
                throw new AssertionError("Projeto aceita quatro etapas");
            }

        } catch (Exception e) {
            throw new AssertionError("O projeto deveria aceitar quatro etapas.", e);
        }

        try {
            analise1.concluir(hoje);
            throw new AssertionError("Conclusão duplicada");
        } catch (IllegalStateException e) {
            System.out.println("[OK] Conclusão duplicada");
        }

        System.out.println("\nTestes finalizados.");
    }
}
