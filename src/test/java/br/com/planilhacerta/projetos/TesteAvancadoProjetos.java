
package br.com.planilhacerta.projetos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class TesteAvancadoProjetos {

    public static void main(String[] args) {

        LocalDate hoje = LocalDate.of(2026, 11, 19);

        MonitoramentoProjetos monitor =
                new MonitoramentoProjetos();

        System.out.println(
                "===== TESTES AVANCADOS =====");

        // TESTE 1: Projeto sem etapas
        Projeto vazio = new Projeto(
                10, 101, 201,
                "Projeto vazio",
                "Teste sem etapas",
                hoje,
                hoje.plusDays(30)
        );

        verificar("Projeto vazio tem progresso zero",
                vazio.calcularProgresso() == 0);

        verificar("Projeto vazio nao esta concluido",
                !vazio.estaConcluido());

        // TESTE 2: Prazo fora do projeto
        Etapa foraDoPrazo = new Etapa(
                "Etapa fora do prazo",
                hoje.plusDays(40),
                new BigDecimal("500")
        );

        try {
            vazio.adicionarEtapa(foraDoPrazo);
            verificar("Prazo invalido rejeitado", false);
        } catch (IllegalArgumentException e) {
            verificar("Prazo invalido rejeitado", true);
        }

        // TESTE 3: Etapa concluida com atraso
        Etapa atrasada = new Etapa(
                "Analise inicial",
                hoje.minusDays(5),
                new BigDecimal("300")
        );

        atrasada.concluir(hoje);

        verificar("Conclusao atrasada identificada",
                atrasada.foiConcluidaComAtraso());

        verificar("Etapa concluida nao fica pendente",
                atrasada.isConcluida());

        // TESTE 4: Lista vazia
        List<Projeto> listaVazia = new ArrayList<>();

        verificar("Media de lista vazia igual a zero",
                monitor.calcularProgressoMedio(
                        listaVazia) == 0);

        // TESTE 5: Nome duplicado
        Etapa etapa1 = new Etapa(
                "Implementacao",
                hoje.plusDays(5),
                new BigDecimal("400")
        );

        Etapa etapa2 = new Etapa(
                "implementacao",
                hoje.plusDays(6),
                new BigDecimal("500")
        );

        vazio.adicionarEtapa(etapa1);

        try {
            vazio.adicionarEtapa(etapa2);
            verificar("Nome duplicado rejeitado", false);
        } catch (IllegalArgumentException e) {
            verificar("Nome duplicado rejeitado", true);
        }

        // TESTE 6: Lista de etapas protegida
        try {
            vazio.getEtapas().clear();
            verificar("Lista protegida", false);
        } catch (UnsupportedOperationException e) {
            verificar("Lista protegida", true);
        }

        // TESTE 7: Etapa vencida
        Etapa vencida = new Etapa(
                "Revisao",
                hoje.minusDays(2),
                new BigDecimal("200")
        );

        verificar("Atraso detectado",
                vencida.estaAtrasada(hoje));

        // TESTE 8: Etapa dentro do prazo
        Etapa futura = new Etapa(
                "Entrega futura",
                hoje.plusDays(10),
                new BigDecimal("600")
        );

        verificar("Etapa futura nao esta atrasada",
                !futura.estaAtrasada(hoje));

        // TESTE 9: Valor total
        verificar("Valor total correto",
                vazio.getValorTotal().compareTo(
                        new BigDecimal("400")) == 0);

        // TESTE 10: Projeto com uma etapa
        // Verifica a regra atual de conclusao
        etapa1.concluir(hoje);


        verificar("Projeto com uma etapa concluida esta concluido",
                vazio.estaConcluido());


        Projeto projetoA = new Projeto(
                100, 101, 201,
                "Projeto A",
                "Teste de compartilhamento",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );

        Projeto projetoB = new Projeto(
                200, 102, 202,
                "Projeto B",
                "Teste de compartilhamento",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30)
        );

        Etapa etapaCompartilhada = new Etapa(
                "Analise inicial",
                LocalDate.of(2026, 11, 10),
                new BigDecimal("500.00")
        );

        projetoA.adicionarEtapa(etapaCompartilhada);

        boolean compartilhamentoBloqueado = false;

        try {
            projetoB.adicionarEtapa(etapaCompartilhada);
        } catch (IllegalStateException e) {
            compartilhamentoBloqueado = true;
        }

        verificar(
                "Mesma etapa nao pode pertencer a dois projetos",
                compartilhamentoBloqueado
        );

        System.out.println(
                "\n===== FIM DOS TESTES =====");
    }

    private static void verificar(
            String descricao, boolean resultado) {

        if (resultado) {
            System.out.println("[OK] " + descricao);
        } else {
            System.out.println("[FALHOU] " + descricao);
        }
    }
}
