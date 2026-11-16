package br.com.planilhacerta.model;

import br.com.planilhacerta.integration.ClienteIntegracao;

import java.math.BigDecimal;
import java.util.Objects;

public class Cliente extends Pessoa implements ClienteIntegracao {
    private String empresa;
    private final boolean vip;
    private int pontosFidelidade;
    private BigDecimal valorPagoRestanteParaPonto = BigDecimal.ZERO;

    public Cliente(String id, String nome, String email, String empresa) {
        this(id, nome, email, empresa, false);
    }

    public Cliente(String id, String nome, String email, String empresa, boolean vip) {
        super(id, nome, email);
        this.empresa = empresa;
        this.vip = vip;
    }

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    @Override
    public boolean isVip() {
        return vip;
    }

    @Override
    public int getPontosFidelidade() {
        return pontosFidelidade;
    }

    public BigDecimal getValorPagoRestanteParaPonto() {
        return valorPagoRestanteParaPonto;
    }

    @Override
    public void resgatarPontosFidelidade(int pontos) {
        if (pontos <= 0 || pontos > pontosFidelidade) {
            throw new IllegalArgumentException("Saldo de pontos insuficiente");
        }
        pontosFidelidade -= pontos;
    }

    @Override
    public void registrarPagamentoParaFidelidade(BigDecimal valorPago) {
        Objects.requireNonNull(valorPago, "valorPago não pode ser nulo");
        if (valorPago.signum() < 0) {
            throw new IllegalArgumentException("O valor pago não pode ser negativo");
        }
        BigDecimal total = valorPagoRestanteParaPonto.add(valorPago);
        BigDecimal[] pontosEResto = total.divideAndRemainder(new BigDecimal("100"));
        pontosFidelidade = Math.addExact(pontosFidelidade, pontosEResto[0].intValueExact());
        valorPagoRestanteParaPonto = pontosEResto[1];
    }

    @Override
    public void exibirDetalhes() {
        System.out.println("[Cliente] ID: " + getId() + " | Nome: " + getNome()
                + " | E-mail: " + getEmail() + " | Empresa: " + empresa
                + " | Categoria: " + (vip ? "VIP" : "Regular")
                + " | Pontos: " + pontosFidelidade);
    }
}

