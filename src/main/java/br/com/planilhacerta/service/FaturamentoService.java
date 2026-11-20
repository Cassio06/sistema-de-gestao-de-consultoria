package br.com.planilhacerta.service;

import br.com.planilhacerta.integration.ContratoIntegracao;
import br.com.planilhacerta.integration.EtapaIntegracao;
import br.com.planilhacerta.model.Fatura;
import br.com.planilhacerta.model.FaturaItem;
import br.com.planilhacerta.model.ModalidadePagamento;
import br.com.planilhacerta.model.Pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FaturamentoService {
    private static final BigDecimal DESCONTO_VIP = new BigDecimal("0.10");

    private final FidelidadeService fidelidadeService;
    private final Map<String, Fatura> faturasPorId = new HashMap<>();
    private final Map<ChaveCobranca, String> faturasPorCobranca = new HashMap<>();
    private final Map<String, Pagamento> pagamentosPorFatura = new HashMap<>();
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
        EtapaIntegracao etapa = contrato.getProjeto().getEtapas().stream()
                .filter(item -> item.getId().equals(etapaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Etapa não pertence ao contrato"));
        if (!etapa.isConcluida()) {
            throw new IllegalStateException("Só é possível faturar etapas concluídas");
        }

        ChaveCobranca chaveCobranca = new ChaveCobranca(contrato.getId(), etapa.getId(), false);
        Fatura existente = obterFaturaExistente(chaveCobranca);
        if (existente != null) {
            return existente;
        }
        if (etapa.isFaturada()) {
            throw new IllegalStateException("A etapa já foi faturada fora deste serviço");
        }
        Fatura fatura = criarFatura(contrato,
                List.of(new FaturaItem(etapa.getNome(), etapa.getValor())), chaveCobranca, dataEmissao);
        etapa.marcarFaturada();
        return fatura;
    }

    public synchronized Fatura gerarFaturaEntregaFinal(
            ContratoIntegracao contrato, LocalDate dataEmissao) {
        validarModalidade(contrato, ModalidadePagamento.ENTREGA_FINAL);
        Objects.requireNonNull(dataEmissao, "dataEmissao não pode ser nula");
        List<? extends EtapaIntegracao> etapas = contrato.getProjeto().getEtapas();
        if (etapas.isEmpty() || etapas.stream().anyMatch(etapa -> !etapa.isConcluida())) {
            throw new IllegalStateException("A entrega final exige todas as etapas concluídas");
        }

        ChaveCobranca chaveCobranca = new ChaveCobranca(contrato.getId(), null, true);
        Fatura existente = obterFaturaExistente(chaveCobranca);
        if (existente != null) {
            return existente;
        }
        if (contrato.getProjeto().isEntregaFinalFaturada()) {
            throw new IllegalStateException("A entrega final já foi faturada fora deste serviço");
        }
        List<FaturaItem> itens = new ArrayList<>();
        for (EtapaIntegracao etapa : etapas) {
            itens.add(new FaturaItem(etapa.getNome(), etapa.getValor()));
        }
        Fatura fatura = criarFatura(contrato, itens, chaveCobranca, dataEmissao);
        contrato.getProjeto().marcarEntregaFinalFaturada();
        return fatura;
    }

    public synchronized Pagamento registrarPagamento(
            String faturaId, BigDecimal valorPago, LocalDate dataPagamento) {
        exigirTexto(faturaId, "faturaId");
        Fatura fatura = faturasPorId.get(faturaId);
        if (fatura == null) {
            throw new IllegalArgumentException("Fatura não encontrada: " + faturaId);
        }
        if (fatura.isPaga() || pagamentosPorFatura.containsKey(faturaId)) {
            throw new IllegalStateException("A fatura já possui pagamento registrado");
        }
        Objects.requireNonNull(valorPago, "valorPago não pode ser nulo");
        if (valorPago.compareTo(fatura.getValorLiquido()) != 0) {
            throw new IllegalArgumentException("O pagamento deve ser igual ao valor líquido da fatura");
        }
        Objects.requireNonNull(dataPagamento, "dataPagamento não pode ser nula");
        ContratoIntegracao contrato = contratosPorId.get(fatura.getContratoId());
        if (contrato == null) {
            throw new IllegalStateException("O contrato associado à fatura não está disponível");
        }

        Pagamento pagamento = new Pagamento(UUID.randomUUID().toString(), faturaId, valorPago, dataPagamento);
        fidelidadeService.registrarPagamento(contrato.getCliente(), valorPago);
        fatura.marcarPaga(dataPagamento);
        pagamentosPorFatura.put(faturaId, pagamento);
        return pagamento;
    }

    public synchronized List<Fatura> getFaturas() {
        return Collections.unmodifiableList(new ArrayList<>(faturasPorId.values()));
    }

    public synchronized List<Pagamento> getPagamentos() {
        return Collections.unmodifiableList(new ArrayList<>(pagamentosPorFatura.values()));
    }

    private Fatura criarFatura(ContratoIntegracao contrato, List<FaturaItem> itens,
                               ChaveCobranca chaveCobranca, LocalDate dataEmissao) {
        BigDecimal valorBruto = itens.stream().map(FaturaItem::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descontoVip = contrato.getCliente().isVip()
                ? calcularDesconto(valorBruto, DESCONTO_VIP) : BigDecimal.ZERO;
        BigDecimal percentualFidelidade = fidelidadeService.prepararDesconto(contrato);
        BigDecimal descontoFidelidade = calcularDesconto(
                valorBruto.subtract(descontoVip), percentualFidelidade);

        Fatura fatura = new Fatura(UUID.randomUUID().toString(), contrato.getId(),
                contrato.getCliente().getId(), itens, descontoVip, descontoFidelidade, dataEmissao);
        contratosPorId.putIfAbsent(contrato.getId(), contrato);
        faturasPorId.put(fatura.getId(), fatura);
        faturasPorCobranca.put(chaveCobranca, fatura.getId());
        return fatura;
    }

    private Fatura obterFaturaExistente(ChaveCobranca chaveCobranca) {
        String faturaId = faturasPorCobranca.get(chaveCobranca);
        return faturaId == null ? null : faturasPorId.get(faturaId);
    }

    private record ChaveCobranca(String contratoId, String etapaId, boolean entregaFinal) {
    }

    private static BigDecimal calcularDesconto(BigDecimal base, BigDecimal percentual) {
        return base.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
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
