package br.com.planilhacerta.domain.model;

import br.com.planilhacerta.domain.enums.CategoriaCliente;

public class ClienteVIP extends Cliente {
    public ClienteVIP(String id, String nome, String email) {
        super(id, nome, email);
    }

    @Override
    public CategoriaCliente getCategoria() {
        return CategoriaCliente.VIP;
    }

    @Override
    public boolean temPrioridadeNaAlocacao() {
        return true;
    }
}
