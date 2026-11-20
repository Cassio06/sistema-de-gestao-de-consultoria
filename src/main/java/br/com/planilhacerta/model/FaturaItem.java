package br.com.planilhacerta.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class FaturaItem {
    private final String descricao;
    private final BigDecimal valor;

    public FaturaItem(String descricao, BigDecimal valor) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição do item não pode ser vazia");
        }
        this.descricao = descricao;
        this.valor = Objects.requireNonNull(valor, "O valor do item não pode ser nulo");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("O valor do item não pode ser negativo");
        }
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
