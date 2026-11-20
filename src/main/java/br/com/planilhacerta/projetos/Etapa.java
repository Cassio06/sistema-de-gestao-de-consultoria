
package br.com.planilhacerta.projetos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Etapa {

    private String nome;
    private LocalDate prazo;
    private BigDecimal valor;
    private StatusEtapa status;
    private LocalDate dataConclusao;
    private Integer idProjeto;


    // Construtor
    public Etapa(String nome, LocalDate prazo,
                 BigDecimal valor) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome da etapa é obrigatório."
            );
        }

        Objects.requireNonNull(prazo,
                "O prazo é obrigatório.");

        Objects.requireNonNull(valor,
                "O valor é obrigatório.");

        if (valor.signum() < 0) {
            throw new IllegalArgumentException(
                    "O valor não pode ser negativo."
            );
        }

        this.nome = nome;
        this.prazo = prazo;
        this.valor = valor;
        this.status = StatusEtapa.PENDENTE;
    }

    // Iniciar uma etapa
    public void iniciar() {
        if (status != StatusEtapa.PENDENTE) {
            throw new IllegalStateException(
                    "A etapa não está pendente."
            );
        }

        status = StatusEtapa.EM_ANDAMENTO;
    }

    // Concluir uma etapa
    public void concluir() {
        concluir(LocalDate.now());
    }

    // Concluir em uma data específica
    public void concluir(LocalDate data) {

        Objects.requireNonNull(data,
                "A data é obrigatória.");

        if (status == StatusEtapa.CONCLUIDA) {
            throw new IllegalStateException(
                    "Esta etapa já foi concluída."
            );
        }

        status = StatusEtapa.CONCLUIDA;
        dataConclusao = data;
    }

    // Consultar status atualizado
    public StatusEtapa getStatusAtual() {
        return getStatusAtual(LocalDate.now());
    }

    public StatusEtapa getStatusAtual(LocalDate hoje) {

        Objects.requireNonNull(hoje);

        if (status != StatusEtapa.CONCLUIDA
                && hoje.isAfter(prazo)) {
            return StatusEtapa.ATRASADA;
        }

        return status;
    }

    // Verificar atraso
    public boolean estaAtrasada(LocalDate hoje) {
        return getStatusAtual(hoje)
                == StatusEtapa.ATRASADA;
    }

    // Verificar se foi entregue atrasada
    public boolean foiConcluidaComAtraso() {
        return dataConclusao != null
                && dataConclusao.isAfter(prazo);
    }

    // Métodos de consulta
    public String getNome() {
        return nome;
    }

    public LocalDate getPrazo() {
        return prazo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public StatusEtapa getStatus() {
        return getStatusAtual();
    }

    public LocalDate getDataConclusao() {
        return dataConclusao;
    }

    public boolean isConcluida() {
        return status == StatusEtapa.CONCLUIDA;
    }

    public void vincularProjeto(int idProjeto) {
        if (idProjeto <= 0) {
            throw new IllegalArgumentException(
                    "O ID do projeto deve ser positivo."
            );
        }

        if (this.idProjeto != null) {
            throw new IllegalStateException(
                    "Esta etapa ja pertence a um projeto."
            );
        }

        this.idProjeto = idProjeto;
    }

    public Integer getIdProjeto() {
        return idProjeto;
    }

}
