package br.com.planilhacerta.view;

import br.com.planilhacerta.integration.ContratoIntegracao;
import br.com.planilhacerta.integration.EtapaIntegracao;
import br.com.planilhacerta.integration.ProjetoIntegracao;
import br.com.planilhacerta.model.Cliente;
import br.com.planilhacerta.model.Especializacao;
import br.com.planilhacerta.model.ModalidadePagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

final class ContratoConsoleAdapter implements ContratoIntegracao {
    private final String id;
    private final Cliente cliente;
    private final Projeto projeto;
    private final ModalidadePagamento modalidadePagamento;

    ContratoConsoleAdapter(String id, Cliente cliente, Projeto projeto,
                           ModalidadePagamento modalidadePagamento) {
        this.id = exigirTexto(id, "id");
        this.cliente = Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        this.projeto = Objects.requireNonNull(projeto, "projeto não pode ser nulo");
        this.modalidadePagamento = Objects.requireNonNull(
                modalidadePagamento, "modalidadePagamento não pode ser nula");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Cliente getCliente() {
        return cliente;
    }

    @Override
    public Projeto getProjeto() {
        return projeto;
    }

    @Override
    public ModalidadePagamento getModalidadePagamento() {
        return modalidadePagamento;
    }

    static final class Projeto implements ProjetoIntegracao {
        private final Especializacao especializacao;
        private final List<Etapa> etapas;
        private boolean entregaFinalFaturada;

        Projeto(Especializacao especializacao, List<Etapa> etapas) {
            this.especializacao = Objects.requireNonNull(
                    especializacao, "especializacao não pode ser nula");
            this.etapas = List.copyOf(Objects.requireNonNull(etapas, "etapas não podem ser nulas"));
        }

        @Override
        public List<Etapa> getEtapas() {
            return etapas;
        }

        @Override
        public Especializacao getEspecializacao() {
            return especializacao;
        }

        @Override
        public boolean isEntregaFinalFaturada() {
            return entregaFinalFaturada;
        }

        @Override
        public void marcarEntregaFinalFaturada() {
            if (entregaFinalFaturada) {
                throw new IllegalStateException("A entrega final já foi faturada");
            }
            entregaFinalFaturada = true;
        }
    }

    static final class Etapa implements EtapaIntegracao {
        private final String id;
        private final String nome;
        private final BigDecimal valor;
        private boolean concluida;
        private boolean faturada;

        Etapa(String id, String nome, BigDecimal valor) {
            this.id = exigirTexto(id, "id da etapa");
            this.nome = exigirTexto(nome, "nome da etapa");
            this.valor = Objects.requireNonNull(valor, "valor não pode ser nulo");
            if (valor.signum() < 0) {
                throw new IllegalArgumentException("O valor da etapa não pode ser negativo");
            }
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getNome() {
            return nome;
        }

        @Override
        public BigDecimal getValor() {
            return valor;
        }

        @Override
        public boolean isConcluida() {
            return concluida;
        }

        void concluir() {
            concluida = true;
        }

        @Override
        public boolean isFaturada() {
            return faturada;
        }

        @Override
        public void marcarFaturada() {
            if (faturada) {
                throw new IllegalStateException("A etapa já foi faturada");
            }
            faturada = true;
        }
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }
        return valor;
    }
}
