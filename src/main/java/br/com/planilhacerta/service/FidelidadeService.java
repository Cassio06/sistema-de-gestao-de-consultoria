package br.com.planilhacerta.service;

import br.com.planilhacerta.integration.ClienteIntegracao;
import br.com.planilhacerta.integration.ContratoIntegracao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class FidelidadeService {
    private static final int PONTOS_POR_BLOCO = 100;
    private static final int MAXIMO_BLOCOS = 3;
    private static final BigDecimal PERCENTUAL_POR_BLOCO = new BigDecimal("0.05");

    private final Map<String, DescontoReservado> descontosPorContrato = new HashMap<>();

    public synchronized void registrarPagamento(ClienteIntegracao cliente, BigDecimal valorPago) {
        Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        Objects.requireNonNull(valorPago, "valorPago não pode ser nulo");
        if (valorPago.signum() < 0) {
            throw new IllegalArgumentException("O valor pago não pode ser negativo");
        }
        cliente.registrarPagamentoParaFidelidade(valorPago);
    }

    public synchronized BigDecimal prepararDesconto(ContratoIntegracao contrato) {
        return comDesconto(contrato, Function.identity());
    }

    synchronized <T> T comDesconto(ContratoIntegracao contrato, Function<BigDecimal, T> operacao) {
        Objects.requireNonNull(contrato, "contrato não pode ser nulo");
        String contratoId = contrato.getId();
        if (contratoId == null || contratoId.isBlank()) {
            throw new IllegalArgumentException("O contrato deve possuir id");
        }
        DescontoReservado existente = descontosPorContrato.get(contratoId);
        if (existente != null) {
            return operacao.apply(existente.percentual());
        }

        ClienteIntegracao cliente = Objects.requireNonNull(contrato.getCliente(),
                "O contrato deve possuir cliente");
        int pontos = calcularBlocosDisponiveis(cliente) * PONTOS_POR_BLOCO;
        DescontoReservado desconto = new DescontoReservado(contratoId, pontos);
        T resultado = operacao.apply(desconto.percentual());
        if (pontos > 0) {
            cliente.resgatarPontosFidelidade(pontos);
        }
        descontosPorContrato.put(contratoId, desconto);
        return resultado;
    }

    public synchronized BigDecimal registrarNovoContrato(ContratoIntegracao contrato) {
        return prepararDesconto(contrato);
    }

    public synchronized BigDecimal calcularDescontoDisponivel(ClienteIntegracao cliente) {
        Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        return PERCENTUAL_POR_BLOCO.multiply(BigDecimal.valueOf(calcularBlocosDisponiveis(cliente)));
    }

    public synchronized int getPontosResgatados(String contratoId) {
        DescontoReservado desconto = descontosPorContrato.get(contratoId);
        return desconto == null ? 0 : desconto.pontosResgatados();
    }

    synchronized List<DescontoReservado> exportarDescontos() {
        return List.copyOf(descontosPorContrato.values());
    }

    synchronized void restaurarDescontos(Map<String, DescontoReservado> descontos) {
        descontosPorContrato.clear();
        descontosPorContrato.putAll(descontos);
    }

    private static int calcularBlocosDisponiveis(ClienteIntegracao cliente) {
        return Math.min(cliente.getPontosFidelidade() / PONTOS_POR_BLOCO, MAXIMO_BLOCOS);
    }

    public record DescontoReservado(String contratoId, int pontosResgatados) {
        public DescontoReservado {
            if (contratoId == null || contratoId.isBlank()) {
                throw new IllegalArgumentException("O contrato deve possuir id");
            }
            int maximoPontos = MAXIMO_BLOCOS * PONTOS_POR_BLOCO;
            if (pontosResgatados < 0 || pontosResgatados > maximoPontos
                    || pontosResgatados % PONTOS_POR_BLOCO != 0) {
                throw new IllegalArgumentException("Reserva de pontos inválida para o contrato " + contratoId);
            }
        }

        public BigDecimal percentual() {
            return PERCENTUAL_POR_BLOCO.multiply(BigDecimal.valueOf(pontosResgatados / PONTOS_POR_BLOCO));
        }
    }
}
