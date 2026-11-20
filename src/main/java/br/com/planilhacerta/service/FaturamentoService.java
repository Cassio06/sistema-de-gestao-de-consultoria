package br.com.planilhacerta.service;

import br.com.planilhacerta.integration.ContratoIntegracao;
import br.com.planilhacerta.integration.EtapaIntegracao;
import br.com.planilhacerta.integration.ProjetoIntegracao;
import br.com.planilhacerta.model.Fatura;
import br.com.planilhacerta.model.FaturaItem;
import br.com.planilhacerta.model.ModalidadePagamento;
import br.com.planilhacerta.model.Pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class FaturamentoService {
    private static final BigDecimal DESCONTO_VIP = new BigDecimal("0.10");

    private final FidelidadeService fidelidadeService;
    private final Map<String, Fatura.Emissao> emissoesPorId = new LinkedHashMap<>();
    private final Map<ChaveCobranca, Fatura.Emissao> emissoesPorCobranca = new LinkedHashMap<>();
    private final Map<String, Pagamento> pagamentosPorFatura = new LinkedHashMap<>();
    private final Map<String, ContratoIntegracao> contratosPorId = new HashMap<>();

    public FaturamentoService() {
        this(new FidelidadeService());
    }

    public FaturamentoService(FidelidadeService fidelidadeService) {
        this.fidelidadeService = Objects.requireNonNull(fidelidadeService,
                "fidelidadeService não pode ser nulo");
    }

    public synchronized Fatura gerarFaturaPorEtapa(
            ContratoIntegracao contrato, String etapaId, LocalDate dataEmissao) {
        validarModalidade(contrato, ModalidadePagamento.POR_ETAPA);
        exigirTexto(etapaId, "etapaId");
        Objects.requireNonNull(dataEmissao, "dataEmissao não pode ser nula");
        EtapaIntegracao etapa = obterEtapaConcluida(contrato, etapaId);
        ChaveCobranca chave = new ChaveCobranca(contrato.getId(), etapaId);
        Fatura.Emissao existente = emissoesPorCobranca.get(chave);
        if (existente != null) {
            return existente.fatura();
        }
        if (etapa.isFaturada()) {
            throw new IllegalStateException("A etapa já foi faturada fora deste serviço");
        }
        return registrarCobranca(contrato, chave, List.of(new FaturaItem(etapa.getNome(), etapa.getValor())),
                dataEmissao, etapa::marcarFaturada);
    }

    public synchronized Fatura gerarFaturaEntregaFinal(
            ContratoIntegracao contrato, LocalDate dataEmissao) {
        validarModalidade(contrato, ModalidadePagamento.ENTREGA_FINAL);
        Objects.requireNonNull(dataEmissao, "dataEmissao não pode ser nula");
        ProjetoIntegracao projeto = contrato.getProjeto();
        List<? extends EtapaIntegracao> etapas = obterEtapasDaEntrega(projeto);
        ChaveCobranca chave = new ChaveCobranca(contrato.getId(), null);
        Fatura.Emissao existente = emissoesPorCobranca.get(chave);
        if (existente != null) {
            return existente.fatura();
        }
        if (projeto.isEntregaFinalFaturada()) {
            throw new IllegalStateException("A entrega final já foi faturada fora deste serviço");
        }
        List<FaturaItem> itens = etapas.stream()
                .map(etapa -> new FaturaItem(etapa.getNome(), etapa.getValor()))
                .toList();
        return registrarCobranca(contrato, chave, itens, dataEmissao, projeto::marcarEntregaFinalFaturada);
    }

    public synchronized Pagamento registrarPagamento(
            String faturaId, BigDecimal valorPago, LocalDate dataPagamento) {
        exigirTexto(faturaId, "faturaId");
        Fatura.Emissao emissao = emissoesPorId.get(faturaId);
        if (emissao == null) {
            throw new IllegalArgumentException("Fatura não encontrada: " + faturaId);
        }
        if (pagamentosPorFatura.containsKey(faturaId)) {
            throw new IllegalStateException("A fatura já possui pagamento registrado");
        }
        Fatura fatura = emissao.fatura();
        validarPagamento(fatura, valorPago, dataPagamento);
        ContratoIntegracao contrato = contratosPorId.get(fatura.getContratoId());
        Pagamento pagamento = new Pagamento(UUID.randomUUID().toString(), faturaId,
                fatura.getValorLiquido(), dataPagamento);
        fidelidadeService.registrarPagamento(contrato.getCliente(), pagamento.getValor());
        emissao.marcarPaga(dataPagamento);
        pagamentosPorFatura.put(faturaId, pagamento);
        return pagamento;
    }

    public synchronized List<Fatura> getFaturas() {
        return emissoesPorId.values().stream().map(Fatura.Emissao::fatura).toList();
    }

    public synchronized List<Pagamento> getPagamentos() {
        return List.copyOf(pagamentosPorFatura.values());
    }

    public synchronized Estado exportarEstado() {
        List<CobrancaRegistrada> cobrancas = new ArrayList<>();
        emissoesPorCobranca.forEach((chave, emissao) -> {
            Fatura fatura = emissao.fatura();
            List<ItemRegistrado> itens = fatura.getItens().stream()
                    .map(item -> new ItemRegistrado(item.getDescricao(), item.getValor()))
                    .toList();
            cobrancas.add(new CobrancaRegistrada(fatura.getId(), chave.contratoId(), fatura.getClienteId(),
                    chave.etapaId(), itens, fatura.getDescontoVip(), fatura.getDescontoFidelidade(),
                    fatura.getDataEmissao().toString()));
        });
        List<PagamentoRegistrado> pagamentos = pagamentosPorFatura.values().stream()
                .map(pagamento -> new PagamentoRegistrado(pagamento.getId(), pagamento.getFaturaId(),
                        pagamento.getValor(), pagamento.getData().toString()))
                .toList();
        return new Estado(cobrancas, pagamentos, fidelidadeService.exportarDescontos());
    }

    /**
     * Recarrega os registros financeiros sobre os contratos e saldos de clientes já carregados.
     * Os marcadores de cobrança devem atualizar o projeto canônico sem registrar pagamentos.
     */
    public synchronized void restaurarEstado(Estado estado, List<? extends ContratoIntegracao> contratos) {
        Objects.requireNonNull(contratos, "contratos não podem ser nulos");
        Estado dados = estado == null ? new Estado(null, null, null) : estado;
        Map<String, ContratoIntegracao> contratosCarregados = new HashMap<>();
        for (ContratoIntegracao contrato : contratos) {
            exigirTexto(contrato.getId(), "contratoId");
            if (contratosCarregados.putIfAbsent(contrato.getId(), contrato) != null) {
                throw new IllegalArgumentException("Contrato repetido: " + contrato.getId());
            }
        }

        Map<String, FidelidadeService.DescontoReservado> descontosCarregados = new LinkedHashMap<>();
        for (FidelidadeService.DescontoReservado desconto : dados.descontosReservados()) {
            exigirContrato(contratosCarregados, desconto.contratoId());
            if (descontosCarregados.putIfAbsent(desconto.contratoId(), desconto) != null) {
                throw new IllegalArgumentException("Reserva repetida: " + desconto.contratoId());
            }
        }

        Map<String, Fatura.Emissao> faturasCarregadas = new LinkedHashMap<>();
        Map<ChaveCobranca, Fatura.Emissao> cobrancasCarregadas = new LinkedHashMap<>();
        List<Runnable> marcacoes = new ArrayList<>();
        for (CobrancaRegistrada cobranca : dados.cobrancas()) {
            ContratoIntegracao contrato = exigirContrato(contratosCarregados, cobranca.contratoId());
            if (!contrato.getCliente().getId().equals(cobranca.clienteId())) {
                throw new IllegalArgumentException("Cliente incorreto na fatura " + cobranca.faturaId());
            }
            ChaveCobranca chave = new ChaveCobranca(cobranca.contratoId(), cobranca.etapaId());
            List<FaturaItem> itens = cobranca.itens().stream()
                    .map(item -> new FaturaItem(item.descricao(), item.valor()))
                    .toList();
            validarCobrancaRestaurada(contrato, chave, cobranca, itens, descontosCarregados.get(contrato.getId()));
            Fatura.Emissao emissao = Fatura.emitir(cobranca.faturaId(), cobranca.contratoId(),
                    cobranca.clienteId(), itens, cobranca.descontoVip(), cobranca.descontoFidelidade(),
                    LocalDate.parse(cobranca.dataEmissao()));
            if (faturasCarregadas.putIfAbsent(cobranca.faturaId(), emissao) != null
                    || cobrancasCarregadas.putIfAbsent(chave, emissao) != null) {
                throw new IllegalArgumentException("Cobrança repetida para a fatura " + cobranca.faturaId());
            }
            marcacoes.add(prepararMarcacao(contrato, chave));
        }

        Map<String, Pagamento> pagamentosCarregados = new LinkedHashMap<>();
        Set<String> pagamentosIds = new HashSet<>();
        for (PagamentoRegistrado registro : dados.pagamentos()) {
            Fatura.Emissao emissao = faturasCarregadas.get(registro.faturaId());
            if (emissao == null) {
                throw new IllegalArgumentException("Fatura não encontrada para o pagamento " + registro.id());
            }
            LocalDate data = LocalDate.parse(registro.data());
            Fatura fatura = emissao.fatura();
            validarPagamento(fatura, registro.valor(), data);
            Pagamento pagamento = new Pagamento(registro.id(), registro.faturaId(), fatura.getValorLiquido(), data);
            if (!pagamentosIds.add(registro.id())
                    || pagamentosCarregados.putIfAbsent(registro.faturaId(), pagamento) != null) {
                throw new IllegalArgumentException("Pagamento repetido: " + registro.id());
            }
            emissao.marcarPaga(data);
        }

        marcacoes.forEach(Runnable::run);
        fidelidadeService.restaurarDescontos(descontosCarregados);
        contratosPorId.clear();
        contratosPorId.putAll(contratosCarregados);
        emissoesPorId.clear();
        emissoesPorId.putAll(faturasCarregadas);
        emissoesPorCobranca.clear();
        emissoesPorCobranca.putAll(cobrancasCarregadas);
        pagamentosPorFatura.clear();
        pagamentosPorFatura.putAll(pagamentosCarregados);
    }

    public record Estado(List<CobrancaRegistrada> cobrancas, List<PagamentoRegistrado> pagamentos,
                         List<FidelidadeService.DescontoReservado> descontosReservados) {
        public Estado {
            cobrancas = cobrancas == null ? List.of() : List.copyOf(cobrancas);
            pagamentos = pagamentos == null ? List.of() : List.copyOf(pagamentos);
            descontosReservados = descontosReservados == null ? List.of() : List.copyOf(descontosReservados);
        }
    }

    public record CobrancaRegistrada(String faturaId, String contratoId, String clienteId, String etapaId,
                                     List<ItemRegistrado> itens, BigDecimal descontoVip,
                                     BigDecimal descontoFidelidade, String dataEmissao) {
        public CobrancaRegistrada {
            itens = List.copyOf(itens);
        }
    }

    public record ItemRegistrado(String descricao, BigDecimal valor) {
    }

    public record PagamentoRegistrado(String id, String faturaId, BigDecimal valor, String data) {
    }

    private Fatura registrarCobranca(ContratoIntegracao contrato, ChaveCobranca chave, List<FaturaItem> itens,
                                     LocalDate dataEmissao, Runnable marcarCobranca) {
        BigDecimal valorBruto = itens.stream().map(FaturaItem::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descontoVip = contrato.getCliente().isVip()
                ? calcularDesconto(valorBruto, DESCONTO_VIP) : BigDecimal.ZERO;
        Fatura.Emissao emissao = fidelidadeService.comDesconto(contrato, percentual -> {
            BigDecimal descontoFidelidade = calcularDesconto(valorBruto.subtract(descontoVip), percentual);
            Fatura.Emissao preparada = Fatura.emitir(UUID.randomUUID().toString(), contrato.getId(),
                    contrato.getCliente().getId(), itens, descontoVip, descontoFidelidade, dataEmissao);
            marcarCobranca.run();
            return preparada;
        });
        contratosPorId.putIfAbsent(contrato.getId(), contrato);
        emissoesPorId.put(emissao.fatura().getId(), emissao);
        emissoesPorCobranca.put(chave, emissao);
        return emissao.fatura();
    }

    private static ContratoIntegracao exigirContrato(Map<String, ContratoIntegracao> contratos, String id) {
        ContratoIntegracao contrato = contratos.get(id);
        if (contrato == null) {
            throw new IllegalArgumentException("Contrato não encontrado no estado financeiro: " + id);
        }
        return contrato;
    }

    private static void validarCobrancaRestaurada(ContratoIntegracao contrato, ChaveCobranca chave,
                                                  CobrancaRegistrada cobranca, List<FaturaItem> itens,
                                                  FidelidadeService.DescontoReservado reserva) {
        List<? extends EtapaIntegracao> etapas = chave.etapaId() == null
                ? obterEtapasDaEntrega(contrato.getProjeto()) : List.of(obterEtapaConcluida(contrato, chave.etapaId()));
        if (itens.size() != etapas.size()) {
            throw new IllegalArgumentException("Itens da fatura não correspondem às etapas cobradas.");
        }
        BigDecimal bruto = BigDecimal.ZERO;
        for (int i = 0; i < itens.size(); i++) {
            FaturaItem item = itens.get(i);
            EtapaIntegracao etapa = etapas.get(i);
            if (!item.getDescricao().equals(etapa.getNome()) || item.getValor().compareTo(etapa.getValor()) != 0) {
                throw new IllegalArgumentException("Item da fatura não corresponde à etapa " + etapa.getId());
            }
            bruto = bruto.add(item.getValor());
        }
        BigDecimal vip = contrato.getCliente().isVip() ? calcularDesconto(bruto, DESCONTO_VIP) : BigDecimal.ZERO;
        BigDecimal percentual = reserva == null ? BigDecimal.ZERO : reserva.percentual();
        BigDecimal desconto = calcularDesconto(bruto.subtract(vip), percentual);
        if (vip.compareTo(cobranca.descontoVip()) != 0 || desconto.compareTo(cobranca.descontoFidelidade()) != 0) {
            throw new IllegalArgumentException("Descontos da fatura inconsistentes com o contrato.");
        }
    }

    private static Runnable prepararMarcacao(ContratoIntegracao contrato, ChaveCobranca chave) {
        if (chave.etapaId() == null) {
            validarModalidade(contrato, ModalidadePagamento.ENTREGA_FINAL);
            ProjetoIntegracao projeto = contrato.getProjeto();
            obterEtapasDaEntrega(projeto);
            return () -> {
                if (!projeto.isEntregaFinalFaturada()) {
                    projeto.marcarEntregaFinalFaturada();
                }
            };
        }
        validarModalidade(contrato, ModalidadePagamento.POR_ETAPA);
        EtapaIntegracao etapa = obterEtapaConcluida(contrato, chave.etapaId());
        return () -> {
            if (!etapa.isFaturada()) {
                etapa.marcarFaturada();
            }
        };
    }

    private static EtapaIntegracao obterEtapaConcluida(ContratoIntegracao contrato, String etapaId) {
        EtapaIntegracao etapa = contrato.getProjeto().getEtapas().stream()
                .filter(item -> etapaId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Etapa não pertence ao contrato"));
        if (!etapa.isConcluida()) {
            throw new IllegalStateException("Só é possível faturar etapas concluídas");
        }
        return etapa;
    }

    private static List<? extends EtapaIntegracao> obterEtapasDaEntrega(ProjetoIntegracao projeto) {
        List<? extends EtapaIntegracao> etapas = projeto.getEtapas();
        if (etapas.isEmpty() || etapas.stream().anyMatch(etapa -> !etapa.isConcluida())) {
            throw new IllegalStateException("A entrega final exige todas as etapas concluídas");
        }
        return etapas;
    }

    private record ChaveCobranca(String contratoId, String etapaId) {
    }

    private static BigDecimal calcularDesconto(BigDecimal base, BigDecimal percentual) {
        return base.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
    }

    private static void validarPagamento(Fatura fatura, BigDecimal valorPago, LocalDate dataPagamento) {
        Objects.requireNonNull(valorPago, "valorPago não pode ser nulo");
        Objects.requireNonNull(dataPagamento, "dataPagamento não pode ser nula");
        if (valorPago.compareTo(fatura.getValorLiquido()) != 0) {
            throw new IllegalArgumentException("O pagamento deve ser igual ao valor líquido da fatura");
        }
        if (dataPagamento.isBefore(fatura.getDataEmissao())) {
            throw new IllegalArgumentException("O pagamento não pode anteceder a emissão da fatura");
        }
    }

    private static void validarModalidade(ContratoIntegracao contrato, ModalidadePagamento esperada) {
        Objects.requireNonNull(contrato, "contrato não pode ser nulo");
        Objects.requireNonNull(contrato.getProjeto(), "O contrato deve possuir projeto");
        Objects.requireNonNull(contrato.getCliente(), "O contrato deve possuir cliente");
        if (contrato.getModalidadePagamento() != esperada) {
            throw new IllegalStateException("A modalidade do contrato não permite esta cobrança");
        }
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }
    }
}
