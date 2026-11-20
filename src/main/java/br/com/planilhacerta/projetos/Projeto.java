
package br.com.planilhacerta.projetos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Projeto {

    private final int id;
    private final int idCliente;
    private final int idConsultor;
    private final String nome;
    private final String descricao;
    private final LocalDate dataInicio;
    private final LocalDate prazoFinal;

    private final List<Etapa> etapas;

    // Construtor
    public Projeto(int id, int idCliente,
                   int idConsultor, String nome,
                   String descricao,
                   LocalDate dataInicio,
                   LocalDate prazoFinal) {

        if (id <= 0 || idCliente <= 0
                || idConsultor <= 0) {
            throw new IllegalArgumentException(
                    "Os identificadores devem ser positivos."
            );
        }

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do projeto é obrigatório."
            );
        }

        Objects.requireNonNull(dataInicio,
                "A data inicial é obrigatória.");

        Objects.requireNonNull(prazoFinal,
                "O prazo final é obrigatório.");

        if (prazoFinal.isBefore(dataInicio)) {
            throw new IllegalArgumentException(
                    "O prazo final não pode ser anterior ao início."
            );
        }

        this.id = id;
        this.idCliente = idCliente;
        this.idConsultor = idConsultor;
        this.nome = nome;
        this.descricao = descricao == null ? "" : descricao;
        this.dataInicio = dataInicio;
        this.prazoFinal = prazoFinal;

        this.etapas = new ArrayList<>();
    }

    // Adicionar uma etapa ao projeto
    public void adicionarEtapa(Etapa etapa) {

        Objects.requireNonNull(etapa,
                "A etapa não pode ser nula.");

        if (etapa.getPrazo().isBefore(dataInicio)
                || etapa.getPrazo().isAfter(prazoFinal)) {
            throw new IllegalArgumentException(
                    "O prazo da etapa deve estar dentro do projeto."
            );
        }

        for (Etapa existente : etapas) {
            if (existente.getNome().equalsIgnoreCase(
                    etapa.getNome())) {
                throw new IllegalArgumentException(
                        "Já existe uma etapa com esse nome."
                );
            }
        }

        etapa.vincularProjeto(this.id);
        etapas.add(etapa);
    }

    // Calcular o progresso do projeto
    public double calcularProgresso() {

        if (etapas.isEmpty()) {
            return 0.0;
        }

        int concluidas = 0;

        for (Etapa etapa : etapas) {
            if (etapa.isConcluida()) {
                concluidas++;
            }
        }

        return (concluidas * 100.0) / etapas.size();
    }

    // Calcular o valor total do projeto
    public BigDecimal getValorTotal() {

        BigDecimal total = BigDecimal.ZERO;

        for (Etapa etapa : etapas) {
            total = total.add(etapa.getValor());
        }

        return total;
    }

    public boolean estaConcluido() {
        if (etapas.isEmpty()) {
            return false;
        }

        for (Etapa etapa : etapas) {
            if (!etapa.isConcluida()) {
                return false;
            }
        }

        return true;
    }

    // Verificar etapas atrasadas
    public boolean possuiAtrasos(LocalDate hoje) {

        Objects.requireNonNull(hoje,
                "A data é obrigatória.");

        for (Etapa etapa : etapas) {
            if (etapa.estaAtrasada(hoje)) {
                return true;
            }
        }

        return false;
    }

    // Retornar lista protegida de etapas
    public List<Etapa> getEtapas() {
        return Collections.unmodifiableList(etapas);
    }

    // Métodos de consulta
    public int getId() {
        return id;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public int getIdConsultor() {
        return idConsultor;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getPrazoFinal() {
        return prazoFinal;
    }
}
