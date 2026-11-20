package br.com.planilhacerta.service;

import br.com.planilhacerta.domain.model.Cliente;
import br.com.planilhacerta.domain.model.Contrato;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ContratoService {
    private final List<Contrato> contratos = new ArrayList<>();

    public synchronized void cadastrar(Contrato contrato) {
        Objects.requireNonNull(contrato, "Informe o contrato.");
        if (buscarPorId(contrato.getId()).isPresent()) {
            throw new IllegalArgumentException("Já existe um contrato com esse identificador.");
        }
        Cliente cliente = contrato.getCliente();
        synchronized (cliente) {
            cliente.registrarContrato(contrato);
            contratos.add(contrato);
        }
    }

    public synchronized List<Contrato> listar() {
        return List.copyOf(contratos);
    }

    public synchronized Optional<Contrato> buscarPorId(String id) {
        return contratos.stream().filter(contrato -> contrato.getId().equals(id)).findFirst();
    }
}
