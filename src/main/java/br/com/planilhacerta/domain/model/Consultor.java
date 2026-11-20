package br.com.planilhacerta.domain.model;

import br.com.planilhacerta.domain.enums.Especializacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Consultor extends Pessoa {
    public static final int LIMITE_PROJETOS_ATIVOS = 3;

    private final Especializacao especializacao;
    private final List<String> projetosAtivos = new ArrayList<>();

    public Consultor(String id, String nome, String email, Especializacao especializacao) {
        super(id, nome, email);
        this.especializacao = Objects.requireNonNull(especializacao, "Informe a especialização.");
    }

    public Especializacao getEspecializacao() {
        return especializacao;
    }

    public synchronized List<String> getProjetosAtivos() {
        return List.copyOf(projetosAtivos);
    }

    public synchronized int getQuantidadeProjetosAtivos() {
        return projetosAtivos.size();
    }

    public synchronized boolean temDisponibilidade() {
        return projetosAtivos.size() < LIMITE_PROJETOS_ATIVOS;
    }

    public synchronized void adicionarProjeto(String projetoId) {
        String id = textoObrigatorio(projetoId, "Identificador do projeto");
        if (projetosAtivos.contains(id)) {
            throw new IllegalArgumentException("O projeto já está ativo neste consultor.");
        }
        if (!temDisponibilidade()) {
            throw new IllegalStateException("O consultor já atingiu o limite de projetos ativos.");
        }
        projetosAtivos.add(id);
    }

    public synchronized void removerProjeto(String projetoId) {
        String id = textoObrigatorio(projetoId, "Identificador do projeto");
        if (!projetosAtivos.remove(id)) {
            throw new IllegalArgumentException("O projeto não está ativo neste consultor.");
        }
    }
}
