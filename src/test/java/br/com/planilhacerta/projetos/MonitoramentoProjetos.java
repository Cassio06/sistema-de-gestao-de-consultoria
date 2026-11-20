
package br.com.planilhacerta.projetos;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MonitoramentoProjetos {

    private static final int DIAS_ALERTA = 3;

    // Verificar os prazos de um projeto
    public void verificarPrazos(Projeto projeto) {
        verificarPrazos(projeto, LocalDate.now());
    }

    // Permite informar a data para testes
    public void verificarPrazos(Projeto projeto,
                                LocalDate hoje) {

        Objects.requireNonNull(projeto,
                "O projeto não pode ser nulo.");

        Objects.requireNonNull(hoje,
                "A data não pode ser nula.");

        System.out.println(
                "\n===== MONITORAMENTO DE PROJETO =====");

        System.out.println("Projeto: " + projeto.getNome());
        System.out.println("ID: " + projeto.getId());
        String consultor = projeto.getIdConsultor();
        System.out.println(consultor == null ? "Aguardando alocação de consultor." : "Consultor: " + consultor);

        if (projeto.getEtapas().isEmpty()) {
            System.out.println("Nenhuma etapa cadastrada.");
        }

        for (Etapa etapa : projeto.getEtapas()) {

            long dias = ChronoUnit.DAYS.between(
                    hoje, etapa.getPrazo());

            System.out.println(
                    "\nEtapa: " + etapa.getNome());

            System.out.println(
                    "Prazo: " + etapa.getPrazo());

            StatusEtapa status =
                    etapa.getStatusAtual(hoje);

            if (status == StatusEtapa.CONCLUIDA) {

                if (etapa.foiConcluidaComAtraso()) {
                    System.out.println(
                            "[!] Concluída fora do prazo.");
                } else {
                    System.out.println(
                            "[OK] Etapa concluída.");
                }

            } else if (status == StatusEtapa.ATRASADA) {

                System.out.println(
                        "[X] Atrasada há "
                                + Math.abs(dias) + " dia(s).");

            } else if (dias <= DIAS_ALERTA) {

                System.out.println(
                        "[!] Atenção: faltam "
                                + dias + " dia(s).");

            } else {

                System.out.println(
                        "[OK] Dentro do prazo.");
            }
        }

        System.out.printf(Locale.forLanguageTag("pt-BR"),
                "%nProgresso: %.2f%%%n",
                projeto.calcularProgresso());

        System.out.println(
                "==================================");
    }

    // Calcular progresso médio de vários projetos
    public double calcularProgressoMedio(
            List<Projeto> projetos) {

        Objects.requireNonNull(projetos,
                "A lista não pode ser nula.");

        if (projetos.isEmpty()) {
            return 0.0;
        }

        double soma = 0;

        for (Projeto projeto : projetos) {
            Objects.requireNonNull(projeto,
                    "A lista contém projeto nulo.");

            soma += projeto.calcularProgresso();
        }

        return soma / projetos.size();
    }

    // Exibir painel com estatísticas
    public void exibirDesempenho(
            List<Projeto> projetos) {

        exibirDesempenho(projetos, LocalDate.now());
    }

    // Versão com data informada
    public void exibirDesempenho(
            List<Projeto> projetos,
            LocalDate hoje) {

        Objects.requireNonNull(projetos,
                "A lista não pode ser nula.");

        Objects.requireNonNull(hoje,
                "A data não pode ser nula.");

        int concluidos = 0;
        int naoConcluidos = 0;
        int comAtrasos = 0;

        for (Projeto projeto : projetos) {

            Objects.requireNonNull(projeto,
                    "A lista contém projeto nulo.");

            if (projeto.estaConcluido()) {
                concluidos++;
            } else {
                naoConcluidos++;
            }

            if (projeto.possuiAtrasos(hoje)) {
                comAtrasos++;
            }
        }

        System.out.println(
                "\n===== PAINEL DE DESEMPENHO =====");

        System.out.println(
                "Total de projetos: " + projetos.size());

        System.out.println(
                "Concluídos: " + concluidos);

        System.out.println(
                "Não concluídos: " + naoConcluidos);

        System.out.println(
                "Com etapas atrasadas: " + comAtrasos);

        System.out.printf(Locale.forLanguageTag("pt-BR"),
                "Progresso médio: %.2f%%%n",
                calcularProgressoMedio(projetos));

        System.out.println(
                "================================");
    }
}
