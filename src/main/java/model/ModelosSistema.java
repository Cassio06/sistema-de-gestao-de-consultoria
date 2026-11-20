package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ModelosSistema {

    public enum Especializacao {
        FINANCEIRO("Planejamento Financeiro"),
        GESTAO("Gestão Empresarial"),
        TI("Análise de Dados e TI");

        private final String descricao;
        Especializacao(String descricao) { this.descricao = descricao; }
        public String getDescricao() { return descricao; }
    }

    public enum StatusEtapa {
        PENDENTE, EM_ANDAMENTO, CONCLUIDA, ATRASADA
    }

    // --- PESSOAS ---
    public static abstract class Pessoa {
        protected String id;
        protected String nome;
        protected String email;
        protected String telefone;

        public Pessoa(String id, String nome, String email, String telefone) {
            this.id = id;
            this.nome = nome;
            this.email = email;
            this.telefone = telefone;
        }

        public String getId() { return id; }
        public String getNome() { return nome; }
        public String getEmail() { return email; }
        public String getTelefone() { return telefone; }
    }

    public static class Cliente extends Pessoa {
        private boolean vip;
        private int pontosFidelidade;
        private List<String> especializacoesContratadas = new ArrayList<>();

        public Cliente(String id, String nome, String email, String telefone, boolean vip) {
            super(id, nome, email, telefone);
            this.vip = vip;
            this.pontosFidelidade = 0;
        }

        public void garantirListas() {
            if (especializacoesContratadas == null) {
                especializacoesContratadas = new ArrayList<>();
            }
        }

        public boolean isVip() { return vip; }
        public void setVip(boolean vip) { this.vip = vip; }
        public int getPontosFidelidade() { return pontosFidelidade; }
        public void adicionarPontos(int pontos) { this.pontosFidelidade += pontos; }
        public List<String> getEspecializacoesContratadas() {
            garantirListas();
            return especializacoesContratadas;
        }

        public void registrarEspecializacao(Especializacao esp) {
            garantirListas();
            if (!especializacoesContratadas.contains(esp.name())) {
                especializacoesContratadas.add(esp.name());
            }
        }

        // Regra de negócio: recomenda áreas ainda não contratadas (usada no Relatório VIP)
        public List<Especializacao> gerarRecomendacoes() {
            garantirListas();
            return Arrays.stream(Especializacao.values())
                    .filter(e -> !especializacoesContratadas.contains(e.name()))
                    .collect(Collectors.toList());
        }

        public String getCategoriaTexto() {
            return vip ? "VIP" : "Regular";
        }
    }

    public static class Consultor extends Pessoa {
        private Especializacao especializacao;
        private int projetosAtivos;

        public Consultor(String id, String nome, String email, String telefone, Especializacao especializacao) {
            super(id, nome, email, telefone);
            this.especializacao = especializacao;
            this.projetosAtivos = 0;
        }

        public Especializacao getEspecializacao() { return especializacao; }
        public int getProjetosAtivos() { return projetosAtivos; }
        public boolean podeReceberProjeto() { return projetosAtivos < 3; }
        public void incrementarProjetos() { this.projetosAtivos++; }
        public void decrementarProjetos() { if (this.projetosAtivos > 0) this.projetosAtivos--; }
    }

    // --- PROJETOS E ETAPAS ---
    public static class Etapa {
        private String nome;
        private LocalDate prazo;
        private double valor;
        private StatusEtapa status;

        public Etapa(String nome, LocalDate prazo, double valor) {
            this.nome = nome;
            this.prazo = prazo;
            this.valor = valor;
            this.status = StatusEtapa.PENDENTE;
        }

        public StatusEtapa getStatusAtualizado() {
            if (status == null) {
                status = StatusEtapa.PENDENTE;
            }
            if (status != StatusEtapa.CONCLUIDA && prazo != null && LocalDate.now().isAfter(prazo)) {
                this.status = StatusEtapa.ATRASADA;
            }
            return status;
        }

        public String getNome() { return nome; }
        public LocalDate getPrazo() { return prazo; }
        public double getValor() { return valor; }
        public void setStatus(StatusEtapa status) { this.status = status; }
    }

    public static class Projeto {
        private String id;
        private String titulo;
        private String clienteId;
        private String consultorId;
        private Especializacao area;
        private List<Etapa> etapas = new ArrayList<>();

        public Projeto(String id, String titulo, String clienteId, String consultorId, Especializacao area) {
            this.id = id;
            this.titulo = titulo;
            this.clienteId = clienteId;
            this.consultorId = consultorId;
            this.area = area;
        }

        public void garantirListas() {
            if (etapas == null) {
                etapas = new ArrayList<>();
            } else {
                etapas.removeIf(Objects::isNull);
            }
        }

        public double calcularProgresso() {
            garantirListas();
            if (etapas.isEmpty()) return 0.0;
            long concluidas = etapas.stream()
                    .filter(e -> e.getStatusAtualizado() == StatusEtapa.CONCLUIDA)
                    .count();
            return (concluidas * 100.0) / etapas.size();
        }

        public double calcularValorTotal() {
            garantirListas();
            return etapas.stream().mapToDouble(Etapa::getValor).sum();
        }

        public long contarEtapasAtrasadas() {
            garantirListas();
            return etapas.stream()
                    .filter(e -> e.getStatusAtualizado() == StatusEtapa.ATRASADA)
                    .count();
        }

        public String getId() { return id; }
        public String getTitulo() { return titulo; }
        public String getClienteId() { return clienteId; }
        public String getConsultorId() { return consultorId; }
        public Especializacao getArea() { return area; }
        public List<Etapa> getEtapas() {
            garantirListas();
            return etapas;
        }
    }

    // --- CONTAINER PARA PERSISTÊNCIA JSON ---
    public static class BancoDados {
        public List<Cliente> clientes = new ArrayList<>();
        public List<Consultor> consultores = new ArrayList<>();
        public List<Projeto> projetos = new ArrayList<>();

        public void sanitizar() {
            if (clientes == null) clientes = new ArrayList<>();
            if (consultores == null) consultores = new ArrayList<>();
            if (projetos == null) projetos = new ArrayList<>();

            clientes.removeIf(Objects::isNull);
            consultores.removeIf(Objects::isNull);
            projetos.removeIf(Objects::isNull);

            clientes.forEach(Cliente::garantirListas);
            projetos.forEach(Projeto::garantirListas);
        }
    }
}