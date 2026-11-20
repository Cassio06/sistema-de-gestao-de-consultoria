package br.com.planilhacerta.integration;

import java.math.BigDecimal;

public interface EtapaIntegracao {
    String getId();

    String getNome();

    BigDecimal getValor();

    boolean isConcluida();

    boolean isFaturada();

    void marcarFaturada();
}
