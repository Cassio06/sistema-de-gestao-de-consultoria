package br.com.planilhacerta.service;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.model.Consultor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ConsultorService {
    private final List<Consultor> consultores = new ArrayList<>();

    public void cadastrar(Consultor consultor) {
        Objects.requireNonNull(consultor, "Informe o consultor.");
        if (buscarPorId(consultor.getId()).isPresent()) {
            throw new IllegalArgumentException("Já existe um consultor com esse identificador.");
        }
        consultores.add(consultor);
    }

    public List<Consultor> listar() {
        return List.copyOf(consultores);
    }

    public Optional<Consultor> buscarPorId(String id) {
        return consultores.stream().filter(consultor -> consultor.getId().equals(id)).findFirst();
    }

    public List<Consultor> listarPorEspecializacao(Especializacao especializacao) {
        Objects.requireNonNull(especializacao, "Informe a especialização.");
        return consultores.stream()
                .filter(consultor -> consultor.getEspecializacao() == especializacao)
                .toList();
    }
}
