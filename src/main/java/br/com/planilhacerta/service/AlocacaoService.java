package br.com.planilhacerta.service;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.model.Consultor;
import br.com.planilhacerta.domain.model.Contrato;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class AlocacaoService {
    private static final Object BLOQUEIO_ALOCACAO = new Object();
    private final Set<Consultor> consultoresConhecidos = new LinkedHashSet<>();

    public Optional<Consultor> selecionarConsultor(Especializacao exigida, List<Consultor> consultores) {
        Objects.requireNonNull(exigida, "Informe a especialização exigida.");
        return List.copyOf(consultores).stream()
                .filter(consultor -> consultor.getEspecializacao() == exigida && consultor.temDisponibilidade())
                .min(Comparator.comparingInt(Consultor::getQuantidadeProjetosAtivos)
                        .thenComparing(Consultor::getId));
    }

    public List<Contrato> ordenarPorPrioridade(List<Contrato> contratosPendentes) {
        return List.copyOf(contratosPendentes).stream()
                .sorted(Comparator.comparing((Contrato contrato) -> contrato.getCliente().temPrioridadeNaAlocacao())
                        .reversed())
                .toList();
    }

    public Optional<Consultor> alocar(Contrato contrato, List<Consultor> consultores) {
        Objects.requireNonNull(contrato, "Informe o contrato.");
        List<Consultor> candidatos = List.copyOf(consultores);
        String projetoId = contrato.getProjetoId();
        synchronized (BLOQUEIO_ALOCACAO) {
            // Uma nova tentativa pode trazer só os consultores disponíveis.
            consultoresConhecidos.addAll(candidatos);
            Optional<Consultor> atual = consultoresConhecidos.stream()
                    .filter(consultor -> consultor.getProjetosAtivos().contains(projetoId))
                    .findFirst();
            if (atual.isPresent()) {
                if (atual.get().getEspecializacao() != contrato.getEspecializacao()) {
                    throw new IllegalStateException("O projeto está ativo em outra especialização.");
                }
                return atual;
            }

            Optional<Consultor> selecionado = selecionarConsultor(contrato.getEspecializacao(), candidatos);
            selecionado.ifPresent(consultor -> consultor.adicionarProjeto(projetoId));
            return selecionado;
        }
    }
}
