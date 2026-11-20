package br.com.planilhacerta.integration;

import br.com.planilhacerta.model.Especializacao;

import java.util.List;

public interface ProjetoIntegracao {
    List<? extends EtapaIntegracao> getEtapas();

    Especializacao getEspecializacao();

    boolean isEntregaFinalFaturada();

    void marcarEntregaFinalFaturada();
}
