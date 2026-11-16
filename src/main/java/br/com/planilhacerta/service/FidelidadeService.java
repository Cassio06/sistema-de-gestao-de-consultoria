package br.com.planilhacerta.service;

import br.com.planilhacerta.integration.ClienteIntegracao;
import br.com.planilhacerta.integration.ContratoIntegracao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FidelidadeService {
    private static final int PONTOS_POR_BLOCO = 100;
    private static final int MAXIMO_BLOCOS = 3;
    private static final BigDecimal PERCENTUAL_POR_BLOCO = new BigDecimal("0.05");

    private final Map<String, DescontoContrato> descontosPorContrato = new HashMap<>();

    public synchronized void registrarPagamento(ClienteIntegracao cliente, BigDecimal valorPago) {
        Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        Objects.requireNonNull(valorPago, "valorPago não pode ser nulo");
        if (valorPago.signum() < 0) {
            throw new IllegalArgumentException("O valor pago não pode ser negativo");
        }
        cliente.registrarPagamentoParaFidelidade(valorPago);
    }

    public synchronized BigDecimal prepararDesconto(ContratoIntegracao contrato) {
        Objects.requireNonNull(contrato, "contrato não pode ser nulo");
        if (contrato.getId() == null || contrato.getId().isBlank()) {
            throw new IllegalArgumentException("O contrato deve possuir id");
        }
        DescontoContrato descontoExistente = descontosPorContrato.get(contrato.getId());
        if (descontoExistente != null) {
            return descontoExistente.percentual();
        }

        ClienteIntegracao cliente = Objects.requireNonNull(contrato.getCliente(),
                "O contrato deve possuir cliente");
        int blocos = Math.min(cliente.getPontosFidelidade() / PONTOS_POR_BLOCO, MAXIMO_BLOCOS);
        int pontosResgatados = blocos * PONTOS_POR_BLOCO;
        BigDecimal percentual = PERCENTUAL_POR_BLOCO.multiply(BigDecimal.valueOf(blocos));
        if (pontosResgatados > 0) {
            cliente.resgatarPontosFidelidade(pontosResgatados);
        }
        descontosPorContrato.put(contrato.getId(), new DescontoContrato(percentual, pontosResgatados));
        return percentual;
    }

    public synchronized BigDecimal registrarNovoContrato(ContratoIntegracao contrato) {
        return prepararDesconto(contrato);
    }

    public synchronized BigDecimal calcularDescontoDisponivel(ClienteIntegracao cliente) {
        Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        int blocos = Math.min(cliente.getPontosFidelidade() / PONTOS_POR_BLOCO, MAXIMO_BLOCOS);
        return PERCENTUAL_POR_BLOCO.multiply(BigDecimal.valueOf(blocos));
    }

    public synchronized int getPontosResgatados(String contratoId) {
        DescontoContrato desconto = descontosPorContrato.get(contratoId);
        return desconto == null ? 0 : desconto.pontosResgatados();
    }

    private record DescontoContrato(BigDecimal percentual, int pontosResgatados) {
    }
}
