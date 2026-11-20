package br.com.planilhacerta.integration;

import java.math.BigDecimal;

public interface ClienteIntegracao {
    String getId();

    boolean isVip();

    int getPontosFidelidade();

    void resgatarPontosFidelidade(int pontos);

    void registrarPagamentoParaFidelidade(BigDecimal valorPago);
}
