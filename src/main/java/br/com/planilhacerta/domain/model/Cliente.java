package br.com.planilhacerta.domain.model;

import br.com.planilhacerta.domain.enums.CategoriaCliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Cliente extends Pessoa {
    private final List<Contrato> historicoContratos = new ArrayList<>();

    protected Cliente(String id, String nome, String email) {
        super(id, nome, email);
    }

    public abstract CategoriaCliente getCategoria();

    public abstract boolean temPrioridadeNaAlocacao();

    public synchronized List<Contrato> getHistoricoContratos() {
        return List.copyOf(historicoContratos);
    }

    public synchronized void registrarContrato(Contrato contrato) {
        Objects.requireNonNull(contrato, "Informe o contrato.");
        if (contrato.getCliente() != this) {
            throw new IllegalArgumentException("O contrato pertence a outro cliente.");
        }
        if (historicoContratos.stream().anyMatch(item -> item.getId().equals(contrato.getId()))) {
            throw new IllegalArgumentException("O contrato já consta no histórico do cliente.");
        }
        historicoContratos.add(contrato);
    }
}
