package br.com.planilhacerta.service;

import br.com.planilhacerta.integration.ClienteIntegracao;
import br.com.planilhacerta.integration.ContratoIntegracao;
import br.com.planilhacerta.model.Especializacao;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RecomendacaoService {
    public List<Especializacao> recomendar(
            ClienteIntegracao cliente, List<? extends ContratoIntegracao> historico) {
        Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        Objects.requireNonNull(historico, "historico não pode ser nulo");
        Set<Especializacao> contratadas = EnumSet.noneOf(Especializacao.class);
        for (ContratoIntegracao contrato : historico) {
            Objects.requireNonNull(contrato, "O histórico não pode conter contratos nulos");
            if (contrato.getCliente().getId().equals(cliente.getId())) {
                contratadas.add(contrato.getProjeto().getEspecializacao());
            }
        }

        List<Especializacao> recomendacoes = new ArrayList<>();
        for (Especializacao especializacao : Especializacao.values()) {
            if (!contratadas.contains(especializacao)) {
                recomendacoes.add(especializacao);
            }
        }
        return List.copyOf(recomendacoes);
    }

    public List<Especializacao> recomendarParaRelatorioVip(
            ClienteIntegracao cliente, List<? extends ContratoIntegracao> historico) {
        Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        return cliente.isVip() ? recomendar(cliente, historico) : List.of();
    }
}
