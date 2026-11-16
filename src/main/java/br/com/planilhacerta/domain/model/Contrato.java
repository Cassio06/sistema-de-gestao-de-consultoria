package br.com.planilhacerta.domain.model;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.enums.ModalidadePagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Contrato {
    private final String id;
    private final Cliente cliente;
    // Integração com Arthur: referência pelo identificador, sem implementar Projeto aqui.
    private final String projetoId;
    private final Especializacao especializacao;
    private final LocalDate dataInicio;
    private final LocalDate prazoFinal;
    private final BigDecimal valor;
    private final ModalidadePagamento modalidadePagamento;

    public Contrato(String id, Cliente cliente, String projetoId, Especializacao especializacao,
                    LocalDate dataInicio, LocalDate prazoFinal, BigDecimal valor,
                    ModalidadePagamento modalidadePagamento) {
        this.id = Pessoa.textoObrigatorio(id, "Identificador do contrato");
        this.cliente = Objects.requireNonNull(cliente, "Informe o cliente.");
        this.projetoId = Pessoa.textoObrigatorio(projetoId, "Identificador do projeto");
        this.especializacao = Objects.requireNonNull(especializacao, "Informe a especialização.");
        this.dataInicio = Objects.requireNonNull(dataInicio, "Informe a data de início.");
        this.prazoFinal = Objects.requireNonNull(prazoFinal, "Informe o prazo final.");
        this.valor = Objects.requireNonNull(valor, "Informe o valor.");
        this.modalidadePagamento = Objects.requireNonNull(modalidadePagamento, "Informe a modalidade.");
        if (prazoFinal.isBefore(dataInicio)) {
            throw new IllegalArgumentException("O prazo final não pode anteceder o início.");
        }
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor do contrato deve ser positivo.");
        }
    }

    public String getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getProjetoId() {
        return projetoId;
    }

    public Especializacao getEspecializacao() {
        return especializacao;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getPrazoFinal() {
        return prazoFinal;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public ModalidadePagamento getModalidadePagamento() {
        return modalidadePagamento;
    }
}
