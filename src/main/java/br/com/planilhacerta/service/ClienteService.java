package br.com.planilhacerta.service;

import br.com.planilhacerta.domain.model.Cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ClienteService {
    private final List<Cliente> clientes = new ArrayList<>();

    public void cadastrar(Cliente cliente) {
        Objects.requireNonNull(cliente, "Informe o cliente.");
        if (buscarPorId(cliente.getId()).isPresent()) {
            throw new IllegalArgumentException("Já existe um cliente com esse identificador.");
        }
        clientes.add(cliente);
    }

    public List<Cliente> listar() {
        return List.copyOf(clientes);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clientes.stream().filter(cliente -> cliente.getId().equals(id)).findFirst();
    }
}
