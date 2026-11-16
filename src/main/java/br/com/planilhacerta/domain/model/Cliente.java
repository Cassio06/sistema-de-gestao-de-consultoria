package br.com.planilhacerta.domain.model;

import br.com.planilhacerta.domain.enums.CategoriaCliente;

import java.util.ArrayList;
import java.util.List;

public abstract class Cliente extends Pessoa {
    private final List<Contrato> historicoContratos = new ArrayList<>();

    protected Cliente(String id, String nome, String email) {
        super(id, nome, email);
    }

    public abstract CategoriaCliente getCategoria();

    public abstract boolean temPrioridadeNaAlocacao();

    public List<Contrato> getHistoricoContratos() {
        return List.copyOf(historicoContratos);
    }

    public void registrarContrato(Contrato contrato) {
        // TODO Cássio: verificar se o contrato pertence a este cliente.
        // Impedir repetição de identificador antes de adicionar ao histórico.
        throw new UnsupportedOperationException("Cássio: implementar o vínculo com o histórico.");
    }
}
