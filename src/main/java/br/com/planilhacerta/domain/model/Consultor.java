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

    public List<String> getProjetosAtivos() {
        return List.copyOf(projetosAtivos);
    }

    public int getQuantidadeProjetosAtivos() {
        return projetosAtivos.size();
    }

    public boolean temDisponibilidade() {
        // TODO Cássio: comparar a quantidade de projetos com LIMITE_PROJETOS_ATIVOS.
        throw new UnsupportedOperationException("Cássio: implementar a verificação de disponibilidade.");
    }

    public void adicionarProjeto(String projetoId) {
        // TODO Cássio: validar o identificador, impedir repetição e respeitar o limite.
        // Só modificar a lista depois que todas as verificações passarem.
        throw new UnsupportedOperationException("Cássio: implementar a inclusão de projeto ativo.");
    }

    public void removerProjeto(String projetoId) {
        // TODO Cássio: liberar a vaga quando Arthur informar a conclusão do projeto.
        // Validar o identificador e definir o erro para projeto não encontrado.
        throw new UnsupportedOperationException("Cássio: implementar a liberação de projeto ativo.");
    }
}
