package br.com.planilhacerta.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class Fatura {
    private final String id;
    private final String contratoId;
    private final String clienteId;
    private final List<FaturaItem> itens;
    private final BigDecimal valorBruto;
    private final BigDecimal descontoVip;
    private final BigDecimal descontoFidelidade;
    private final BigDecimal valorLiquido;
    private final LocalDate dataEmissao;
    private volatile LocalDate dataPagamento;

    public Fatura(String id, String contratoId, String clienteId, List<FaturaItem> itens,
                  BigDecimal descontoVip, BigDecimal descontoFidelidade, LocalDate dataEmissao) {
        this.id = exigirTexto(id, "id");
        this.contratoId = exigirTexto(contratoId, "contratoId");
        this.clienteId = exigirTexto(clienteId, "clienteId");
        this.itens = List.copyOf(Objects.requireNonNull(itens, "itens não podem ser nulos"));
        if (this.itens.isEmpty()) {
            throw new IllegalArgumentException("A fatura deve conter ao menos um item");
        }
        this.valorBruto = this.itens.stream().map(FaturaItem::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.descontoVip = validarDesconto(descontoVip, "descontoVip");
        this.descontoFidelidade = validarDesconto(descontoFidelidade, "descontoFidelidade");
        if (this.descontoVip.add(this.descontoFidelidade).compareTo(valorBruto) > 0) {
            throw new IllegalArgumentException("Os descontos não podem exceder o valor bruto");
        }
        this.valorLiquido = valorBruto.subtract(this.descontoVip).subtract(this.descontoFidelidade);
        this.dataEmissao = Objects.requireNonNull(dataEmissao, "dataEmissao não pode ser nula");
    }

    public String getId() {
        return id;
    }

    public String getContratoId() {
        return contratoId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public List<FaturaItem> getItens() {
        return itens;
    }

    public BigDecimal getValorBruto() {
        return valorBruto;
    }

    public BigDecimal getDescontoVip() {
        return descontoVip;
    }

    public BigDecimal getDescontoFidelidade() {
        return descontoFidelidade;
    }

    public BigDecimal getValorLiquido() {
        return valorLiquido;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public boolean isPaga() {
        return dataPagamento != null;
    }

    public static Emissao emitir(String id, String contratoId, String clienteId, List<FaturaItem> itens,
                                 BigDecimal descontoVip, BigDecimal descontoFidelidade, LocalDate dataEmissao) {
        return new Emissao(new Fatura(id, contratoId, clienteId, itens, descontoVip, descontoFidelidade, dataEmissao));
    }

    public static final class Emissao {
        private final Fatura fatura;

        private Emissao(Fatura fatura) {
            this.fatura = fatura;
        }

        public Fatura fatura() {
            return fatura;
        }

        public void marcarPaga(LocalDate dataPagamento) {
            if (fatura.isPaga()) {
                throw new IllegalStateException("A fatura já foi paga");
            }
            fatura.dataPagamento = Objects.requireNonNull(dataPagamento, "dataPagamento não pode ser nula");
        }
    }

    private static BigDecimal validarDesconto(BigDecimal valor, String campo) {
        valor = FaturaItem.exigirCentavos(valor, campo);
        if (valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo");
        }
        return valor;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }
        return valor;
    }
}
