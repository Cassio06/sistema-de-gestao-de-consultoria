package br.com.planilhacerta.service;

import br.com.planilhacerta.domain.model.Contrato;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContratoService {
    private final List<Contrato> contratos = new ArrayList<>();

    public void cadastrar(Contrato contrato) {
        // TODO Cássio: validar contrato e impedir identificadores repetidos.
        // Registrar no histórico do cliente e na lista de contratos de forma consistente.
        // Se uma validação falhar, nenhuma das duas listas deve ser alterada.
        throw new UnsupportedOperationException("Cássio: implementar o cadastro de contrato e histórico.");
    }

    public List<Contrato> listar() {
        return List.copyOf(contratos);
    }

    public Optional<Contrato> buscarPorId(String id) {
        return contratos.stream().filter(contrato -> contrato.getId().equals(id)).findFirst();
    }
}
