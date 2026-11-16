package br.com.planilhacerta.integration;

import br.com.planilhacerta.model.ModalidadePagamento;

public interface ContratoIntegracao {
    String getId();

    ClienteIntegracao getCliente();

    ProjetoIntegracao getProjeto();

    ModalidadePagamento getModalidadePagamento();
}
