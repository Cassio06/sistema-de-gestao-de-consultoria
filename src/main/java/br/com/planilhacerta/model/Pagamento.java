package br.com.planilhacerta.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class Pagamento {
    private final String id;
    private final String faturaId;
    private final BigDecimal valor;
    private final LocalDate data;

    public Pagamento(String id, String faturaId, BigDecimal valor, LocalDate data) {
        this.id = exigirTexto(id, "id");
        this.faturaId = exigirTexto(faturaId, "faturaId");
        this.valor = Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("O valor do pagamento não pode ser negativo");
        }
        this.data = Objects.requireNonNull(data, "data não pode ser nula");
    }

    public String getId() {
        return id;
    }

    public String getFaturaId() {
        return faturaId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }
        return valor;
    }
}
