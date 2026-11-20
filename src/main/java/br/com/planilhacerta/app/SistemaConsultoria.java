package br.com.planilhacerta.app;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.enums.ModalidadePagamento;
import br.com.planilhacerta.domain.model.*;
import br.com.planilhacerta.integration.*;
import br.com.planilhacerta.model.Fatura;
import br.com.planilhacerta.model.Pagamento;
import br.com.planilhacerta.projetos.*;
import br.com.planilhacerta.service.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public final class SistemaConsultoria {
    public record DadosCliente(String id, String nome, String email, boolean vip, int pontos, BigDecimal saldo) {}
    public record DadosConsultor(String id, String nome, String email, Especializacao area) {}
    public record DadosEtapa(String id, String nome, LocalDate prazo, BigDecimal valor) {}
    public record EtapaSalva(DadosEtapa dados, boolean iniciada, LocalDate conclusao) {}
    public record DadosContrato(String id, String clienteId, String projetoId, String nome, Especializacao area,
                               LocalDate inicio, LocalDate prazo, ModalidadePagamento forma, String consultorId,
                               List<EtapaSalva> etapas) {}
    public record Estado(List<DadosCliente> clientes, List<DadosConsultor> consultores,
                         List<DadosContrato> contratos, FaturamentoService.Estado financeiro) {
        public Estado {
            clientes = List.copyOf(clientes);
            consultores = List.copyOf(consultores);
            contratos = List.copyOf(contratos);
            Objects.requireNonNull(financeiro, "Estado financeiro ausente.");
        }
    }

    private final ClienteService clientes = new ClienteService();
    private final ConsultorService consultores = new ConsultorService();
    private final ContratoService contratos = new ContratoService();
    private final AlocacaoService alocacao = new AlocacaoService();
    private final FidelidadeService fidelidade = new FidelidadeService();
    private final FaturamentoService faturamento = new FaturamentoService(fidelidade);
    private final RecomendacaoService recomendacoes = new RecomendacaoService();
    private final Map<String, br.com.planilhacerta.model.Cliente> saldos = new LinkedHashMap<>();
    private final Map<String, ContratoCobranca> porContrato = new LinkedHashMap<>();
    private final Map<String, ProjetoCobranca> porProjeto = new LinkedHashMap<>();
    private final RepositorioIntegrado repositorio;
    private final RelatoriosIntegrados relatorios;

    public SistemaConsultoria(Path dados, Path pdfs) {
        this(new RepositorioIntegrado(dados), new RelatoriosIntegrados(dados, pdfs));
        repositorio.carregar().ifPresent(this::restaurar);
    }

    private SistemaConsultoria(RepositorioIntegrado repositorio, RelatoriosIntegrados relatorios) {
        this.repositorio = repositorio;
        this.relatorios = relatorios;
    }

    public static void validar(Estado estado) {
        new SistemaConsultoria((RepositorioIntegrado) null, null).restaurar(estado);
    }

    public void cadastrarCliente(String id, String nome, String email, boolean vip) {
        Cliente cliente = vip ? new ClienteVIP(id, nome, email) : new ClienteRegular(id, nome, email);
        clientes.cadastrar(cliente);
        saldos.put(cliente.getId(), new br.com.planilhacerta.model.Cliente(
                cliente.getId(), cliente.getNome(), cliente.getEmail(), cliente.getNome(), vip));
    }

    public void cadastrarConsultor(String id, String nome, String email, Especializacao area) {
        consultores.cadastrar(new Consultor(id, nome, email, area));
        alocarPendentes();
    }

    public void criarContrato(String id, String clienteId, String projetoId, String nome, Especializacao area,
                             LocalDate inicio, LocalDate prazo, ModalidadePagamento forma, List<DadosEtapa> etapas) {
        ContratoCobranca contrato = montar(new DadosContrato(id, clienteId, projetoId, nome, area, inicio, prazo,
                forma, null, etapas.stream().map(e -> new EtapaSalva(e, false, null)).toList()));
        fidelidade.registrarNovoContrato(contrato);
        alocarPendentes();
    }

    public void iniciarEtapa(String projetoId, String etapaId) {
        ProjetoCobranca projeto = projetoAlocado(projetoId);
        projeto.etapa(etapaId).iniciar();
    }

    public void concluirEtapa(String projetoId, String etapaId, LocalDate data) {
        ProjetoCobranca vinculo = projetoAlocado(projetoId);
        Projeto projeto = vinculo.projeto;
        if (Objects.requireNonNull(data, "Informe a data de conclusão.").isBefore(projeto.getDataInicio())) {
            throw new IllegalArgumentException("A conclusão não pode anteceder o projeto.");
        }
        vinculo.etapa(etapaId).concluir(data);
        if (projeto.estaConcluido()) {
            consultores.buscarPorId(projeto.getIdConsultor()).orElseThrow().removerProjeto(projeto.getId());
            alocarPendentes();
        }
    }

    private ProjetoCobranca projetoAlocado(String projetoId) {
        ProjetoCobranca vinculo = buscarProjeto(projetoId);
        if (vinculo.projeto.getIdConsultor() == null) {
            throw new IllegalStateException("O projeto ainda aguarda alocação de consultor.");
        }
        return vinculo;
    }

    private ProjetoCobranca buscarProjeto(String id) {
        return Optional.ofNullable(porProjeto.get(id))
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado."));
    }

    private void alocarPendentes() {
        List<Contrato> pendentes = contratos.listar().stream().filter(c -> {
            Projeto projeto = buscarProjeto(c.getProjetoId()).projeto;
            return projeto.getIdConsultor() == null && !projeto.estaConcluido();
        }).toList();
        List<Consultor> candidatos = consultores.listar();
        for (Contrato contrato : alocacao.ordenarPorPrioridade(pendentes)) {
            alocacao.alocar(contrato, candidatos).ifPresent(consultor ->
                    buscarProjeto(contrato.getProjetoId()).projeto.alocarConsultor(consultor.getId()));
        }
    }

    public Fatura emitirFatura(String contratoId, String etapaId, LocalDate data) {
        ContratoCobranca contrato = Optional.ofNullable(porContrato.get(contratoId))
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado."));
        Objects.requireNonNull(data, "Informe a data de emissão.");
        if (data.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data de emissão não pode estar no futuro.");
        }
        List<Etapa> etapas = etapaId == null ? contrato.vinculo.projeto.getEtapas() : List.of(contrato.vinculo.etapa(etapaId));
        if (etapas.stream().anyMatch(e -> e.getDataConclusao() != null && data.isBefore(e.getDataConclusao()))) {
            throw new IllegalArgumentException("A emissão não pode anteceder a conclusão das etapas cobradas.");
        }
        return etapaId == null ? faturamento.gerarFaturaEntregaFinal(contrato, data)
                : faturamento.gerarFaturaPorEtapa(contrato, etapaId, data);
    }

    public Pagamento pagar(String faturaId, BigDecimal valor, LocalDate data) {
        if (Objects.requireNonNull(data, "Informe a data do pagamento.").isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data de pagamento não pode estar no futuro.");
        }
        return faturamento.registrarPagamento(faturaId, valor, data);
    }

    public List<Cliente> clientes() { return clientes.listar(); }
    public List<Consultor> consultores() { return consultores.listar(); }
    public List<Contrato> contratos() { return contratos.listar(); }
    public List<Projeto> projetos() { return porProjeto.values().stream().map(p -> p.projeto).toList(); }
    public List<Fatura> faturas() { return faturamento.getFaturas(); }
    public List<Pagamento> pagamentos() { return faturamento.getPagamentos(); }

    public int pontos(String id) {
        return saldo(id).getPontosFidelidade();
    }

    private br.com.planilhacerta.model.Cliente saldo(String id) {
        return Optional.ofNullable(saldos.get(id))
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    public List<br.com.planilhacerta.model.Especializacao> recomendar(String id) {
        return recomendacoes.recomendar(saldo(id), List.copyOf(porContrato.values()));
    }

    public void emitirRelatorio(String projetoId) {
        ProjetoCobranca vinculo = buscarProjeto(projetoId);
        Contrato contrato = vinculo.contrato;
        relatorios.emitir(vinculo.projeto, contrato,
                consultores.buscarPorId(vinculo.projeto.getIdConsultor()).orElse(null),
                pontos(contrato.getCliente().getId()), recomendar(contrato.getCliente().getId()));
    }

    public void exportarCSV() { relatorios.exportar(projetos(), contratos(), clientes(), consultores()); }

    public void carregarDemonstracao() {
        if (clientes.buscarPorId("CLI-DEMO").isPresent() || consultores.buscarPorId("CON-DEMO").isPresent()
                || porContrato.containsKey("CTR-DEMO") || porProjeto.containsKey("PRJ-DEMO")) {
            throw new IllegalStateException("A demonstração já foi carregada ou seus identificadores estão em uso.");
        }
        LocalDate hoje = LocalDate.now();
        cadastrarCliente("CLI-DEMO", "Empresa de demonstração", "demo@example.test", true);
        cadastrarConsultor("CON-DEMO", "Consultora Financeira", "consultora@example.test", Especializacao.FINANCEIRO);
        criarContrato("CTR-DEMO", "CLI-DEMO", "PRJ-DEMO", "Reestruturação de fluxo de caixa",
                Especializacao.FINANCEIRO, hoje.minusDays(5), hoje.plusDays(12), ModalidadePagamento.POR_ETAPA,
                List.of(new DadosEtapa("ETP-1", "Análise inicial", hoje.minusDays(3), new BigDecimal("2000")),
                        new DadosEtapa("ETP-2", "Implementação", hoje.minusDays(1), new BigDecimal("3000")),
                        new DadosEtapa("ETP-3", "Revisão final", hoje.plusDays(10), new BigDecimal("1500"))));
        if (buscarProjeto("PRJ-DEMO").projeto.getIdConsultor() != null) {
            concluirEtapa("PRJ-DEMO", "ETP-1", hoje.minusDays(3));
            iniciarEtapa("PRJ-DEMO", "ETP-2");
        }
    }

    public void salvar() throws IOException {
        repositorio.salvar(estado());
    }

    private Estado estado() {
        List<DadosCliente> pessoas = clientes.listar().stream().map(c -> {
            var saldo = saldo(c.getId());
            return new DadosCliente(c.getId(), c.getNome(), c.getEmail(), saldo.isVip(),
                    saldo.getPontosFidelidade(), saldo.getValorPagoRestanteParaPonto());
        }).toList();
        List<DadosConsultor> equipe = consultores.listar().stream().map(c ->
                new DadosConsultor(c.getId(), c.getNome(), c.getEmail(), c.getEspecializacao())).toList();
        List<DadosContrato> contratosSalvos = porContrato.values().stream().map(c -> {
            Contrato contrato = c.contrato;
            Projeto projeto = c.vinculo.projeto;
            List<EtapaSalva> etapas = projeto.getEtapas().stream().map(e -> new EtapaSalva(
                    new DadosEtapa(e.getId(), e.getNome(), e.getPrazo(), e.getValor()),
                    e.getStatusRegistrado() == StatusEtapa.EM_ANDAMENTO, e.getDataConclusao())).toList();
            return new DadosContrato(contrato.getId(), contrato.getCliente().getId(), projeto.getId(),
                    projeto.getNome(), contrato.getEspecializacao(), contrato.getDataInicio(),
                    contrato.getPrazoFinal(), contrato.getModalidadePagamento(), projeto.getIdConsultor(), etapas);
        }).toList();
        return new Estado(pessoas, equipe, contratosSalvos, faturamento.exportarEstado());
    }

    private void restaurar(Estado estado) {
        for (DadosCliente c : estado.clientes()) {
            cadastrarCliente(c.id(), c.nome(), c.email(), c.vip());
            saldos.put(c.id(), new br.com.planilhacerta.model.Cliente(
                    c.id(), c.nome(), c.email(), c.nome(), c.vip(), c.pontos(), c.saldo()));
        }
        for (DadosConsultor c : estado.consultores()) {
            consultores.cadastrar(new Consultor(c.id(), c.nome(), c.email(), c.area()));
        }
        for (DadosContrato c : estado.contratos()) {
            montar(c);
        }
        faturamento.restaurarEstado(estado.financeiro(), List.copyOf(porContrato.values()));
        alocarPendentes();
    }

    private ContratoCobranca montar(DadosContrato dados) {
        Cliente titular = clientes.buscarPorId(dados.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente do contrato não encontrado."));
        Projeto projeto = new Projeto(dados.projetoId(), titular.getId(), dados.consultorId(),
                dados.nome(), "", dados.inicio(), dados.prazo());
        if (porProjeto.containsKey(projeto.getId())) {
            throw new IllegalArgumentException("Já existe um contrato para esse projeto.");
        }
        BigDecimal total = BigDecimal.ZERO;
        Set<String> ids = new HashSet<>();
        for (EtapaSalva item : dados.etapas()) {
            DadosEtapa e = item.dados();
            BigDecimal valor = e.valor();
            if (valor.signum() <= 0 || valor.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("Valor de etapa deve ser positivo e em centavos.");
            }
            Etapa etapa = new Etapa(e.id(), e.nome(), e.prazo(), valor);
            if (!ids.add(etapa.getId())) {
                throw new IllegalArgumentException("Identificador de etapa repetido.");
            }
            if (item.conclusao() != null) {
                if (item.conclusao().isBefore(dados.inicio())) {
                    throw new IllegalArgumentException("A conclusão não pode anteceder o projeto.");
                }
                etapa.concluir(item.conclusao());
            } else if (item.iniciada()) {
                etapa.iniciar();
            }
            projeto.adicionarEtapa(etapa);
            total = total.add(valor);
        }
        Contrato contrato = new Contrato(dados.id(), titular, projeto.getId(), dados.area(), dados.inicio(),
                dados.prazo(), total, dados.forma());
        if (contratos.buscarPorId(contrato.getId()).isPresent()) {
            throw new IllegalArgumentException("Já existe um contrato com esse identificador.");
        }
        if (projeto.getIdConsultor() != null) {
            Consultor consultor = consultores.buscarPorId(projeto.getIdConsultor())
                    .orElseThrow(() -> new IllegalArgumentException("Consultor do projeto não cadastrado."));
            if (consultor.getEspecializacao() != dados.area()) {
                throw new IllegalArgumentException("O consultor pertence a outra especialização.");
            }
            if (!projeto.estaConcluido()) {
                consultor.adicionarProjeto(projeto.getId());
            }
        } else if (projeto.getEtapas().stream().anyMatch(e -> e.getStatusRegistrado() != StatusEtapa.PENDENTE)) {
            throw new IllegalArgumentException("Há etapas iniciadas sem consultor alocado.");
        }
        contratos.cadastrar(contrato);
        ProjetoCobranca projetoCobranca = new ProjetoCobranca(projeto, contrato);
        ContratoCobranca contratoCobranca = new ContratoCobranca(contrato, saldo(titular.getId()), projetoCobranca);
        porProjeto.put(projeto.getId(), projetoCobranca);
        porContrato.put(contrato.getId(), contratoCobranca);
        return contratoCobranca;
    }

    private record ContratoCobranca(Contrato contrato, ClienteIntegracao cliente,
                                   ProjetoCobranca vinculo) implements ContratoIntegracao {
        public String getId() { return contrato.getId(); }
        public ClienteIntegracao getCliente() { return cliente; }
        public ProjetoIntegracao getProjeto() { return vinculo; }
        public br.com.planilhacerta.model.ModalidadePagamento getModalidadePagamento() {
            return br.com.planilhacerta.model.ModalidadePagamento.valueOf(contrato.getModalidadePagamento().name());
        }
    }

    private static final class ProjetoCobranca implements ProjetoIntegracao {
        private final Projeto projeto;
        private final Contrato contrato;
        private final List<EtapaCobranca> etapas;
        private boolean faturado;

        ProjetoCobranca(Projeto projeto, Contrato contrato) {
            this.projeto = projeto;
            this.contrato = contrato;
            this.etapas = projeto.getEtapas().stream().map(EtapaCobranca::new).toList();
        }

        Etapa etapa(String id) {
            return etapas.stream().filter(e -> e.getId().equals(id)).findFirst().map(e -> e.etapa)
                    .orElseThrow(() -> new IllegalArgumentException("Etapa não encontrada."));
        }

        public List<EtapaCobranca> getEtapas() { return etapas; }
        public br.com.planilhacerta.model.Especializacao getEspecializacao() {
            return br.com.planilhacerta.model.Especializacao.valueOf(contrato.getEspecializacao().name());
        }
        public boolean isEntregaFinalFaturada() { return faturado; }
        public void marcarEntregaFinalFaturada() { faturado = true; }
    }

    private static final class EtapaCobranca implements EtapaIntegracao {
        private final Etapa etapa;
        private boolean faturada;

        EtapaCobranca(Etapa etapa) { this.etapa = etapa; }
        public String getId() { return etapa.getId(); }
        public String getNome() { return etapa.getNome(); }
        public BigDecimal getValor() { return etapa.getValor(); }
        public boolean isConcluida() { return etapa.isConcluida(); }
        public boolean isFaturada() { return faturada; }
        public void marcarFaturada() { faturada = true; }
    }
}
