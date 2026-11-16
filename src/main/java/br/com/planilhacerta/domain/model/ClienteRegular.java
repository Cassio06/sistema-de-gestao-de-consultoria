package br.com.planilhacerta.domain.model;

import br.com.planilhacerta.domain.enums.CategoriaCliente;

public class ClienteRegular extends Cliente {
    public ClienteRegular(String id, String nome, String email) {
        super(id, nome, email);
    }

    @Override
    public CategoriaCliente getCategoria() {
        return CategoriaCliente.REGULAR;
    }

    @Override
    public boolean temPrioridadeNaAlocacao() {
        return false;
    }
}
