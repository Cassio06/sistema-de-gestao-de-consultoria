package br.com.planilhacerta.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class FaturaItem {
    private final String descricao;
    private final BigDecimal valor;

    public FaturaItem(String descricao, BigDecimal valor) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descrição do item não pode ser vazia");
        }
        this.descricao = descricao;
        this.valor = exigirCentavos(valor, "O valor do item");
        if (this.valor.signum() < 0) {
            throw new IllegalArgumentException("O valor do item não pode ser negativo");
        }
    }

    static BigDecimal exigirCentavos(BigDecimal valor, String campo) {
        Objects.requireNonNull(valor, campo + " não pode ser nulo");
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException erro) {
            throw new IllegalArgumentException(campo + " deve ser expresso em centavos inteiros", erro);
        }
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
