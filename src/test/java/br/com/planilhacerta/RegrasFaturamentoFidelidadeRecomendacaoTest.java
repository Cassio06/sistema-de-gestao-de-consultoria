package br.com.planilhacerta;

import br.com.planilhacerta.integration.ClienteIntegracao;
import br.com.planilhacerta.integration.ContratoIntegracao;
import br.com.planilhacerta.integration.EtapaIntegracao;
import br.com.planilhacerta.integration.ProjetoIntegracao;
import br.com.planilhacerta.model.Especializacao;
import br.com.planilhacerta.model.Fatura;
import br.com.planilhacerta.model.ModalidadePagamento;
import br.com.planilhacerta.model.Cliente;
import br.com.planilhacerta.service.FaturamentoService;
import br.com.planilhacerta.service.FidelidadeService;
import br.com.planilhacerta.service.RecomendacaoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RegrasFaturamentoFidelidadeRecomendacaoTest {
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 16);

    public static void main(String[] args) {
        testaFaturamentoPorEtapaEDuplicidade();
        testaFaturamentoFinal();
        testaDescontosVipEFidelidade();
        testaAcumuloDePontosEResgateComTeto();
        testaClienteDoMenuIntegradoAoProgramaDeFidelidade();
        testaRecomendacoesParaHistoricoERelatorioVip();
        System.out.println("Todos os testes de faturamento, fidelidade e recomendação passaram.");
    }

    private static void testaFaturamentoPorEtapaEDuplicidade() {
        ClienteFake cliente = new ClienteFake("cliente-etapas", false);
        EtapaFake concluida = new EtapaFake("etapa-1", "Diagnóstico", "200.00", true);
        EtapaFake pendente = new EtapaFake("etapa-2", "Entrega", "300.00", false);
        ContratoFake contrato = contrato("contrato-etapas", cliente, ModalidadePagamento.POR_ETAPA,
                Especializacao.FINANCEIRO, concluida, pendente);
        FaturamentoService service = new FaturamentoService();

        Fatura fatura = service.gerarFaturaPorEtapa(contrato, "etapa-1", HOJE);
        verificar(fatura.getItens().size() == 1, "A fatura parcial deve listar apenas a etapa cobrada");
        verificar(fatura.getValorBruto().compareTo(new BigDecimal("200.00")) == 0,
                "O valor bruto deve corresponder à etapa concluída");
        verificar(service.gerarFaturaPorEtapa(contrato, "etapa-1", HOJE) == fatura,
                "Uma nova solicitação da mesma etapa deve retornar a fatura existente");
        esperar(IllegalStateException.class,
                () -> service.gerarFaturaPorEtapa(contrato, "etapa-2", HOJE));
        esperar(IllegalStateException.class,
                () -> service.gerarFaturaEntregaFinal(contrato, HOJE));

        esperar(IllegalArgumentException.class,
                () -> service.registrarPagamento(fatura.getId(), new BigDecimal("199.99"), HOJE));
        verificar(!fatura.isPaga(), "Pagamento com valor incorreto não deve alterar a fatura");
        service.registrarPagamento(fatura.getId(), fatura.getValorLiquido(), HOJE);
        esperar(IllegalStateException.class,
                () -> service.registrarPagamento(fatura.getId(), fatura.getValorLiquido(), HOJE));
        verificar(service.getPagamentos().size() == 1, "Não deve ser registrado pagamento duplicado");
    }

    private static void testaFaturamentoFinal() {
        ClienteFake cliente = new ClienteFake("cliente-final", false);
        EtapaFake primeira = new EtapaFake("final-1", "Planejamento", "100.00", true);
        EtapaFake segunda = new EtapaFake("final-2", "Execução", "150.00", false);
        ContratoFake contrato = contrato("contrato-final", cliente, ModalidadePagamento.ENTREGA_FINAL,
                Especializacao.GESTAO, primeira, segunda);
        FaturamentoService service = new FaturamentoService();

        esperar(IllegalStateException.class, () -> service.gerarFaturaEntregaFinal(contrato, HOJE));
        segunda.concluida = true;
        Fatura fatura = service.gerarFaturaEntregaFinal(contrato, HOJE);
        verificar(fatura.getItens().size() == 2, "A fatura final deve detalhar todas as etapas");
        verificar(fatura.getValorBruto().compareTo(new BigDecimal("250.00")) == 0,
                "O valor final deve somar o valor de todas as etapas");
        verificar(service.gerarFaturaEntregaFinal(contrato, HOJE) == fatura,
                "A entrega final não deve gerar uma cobrança duplicada");
    }

    private static void testaDescontosVipEFidelidade() {
        ClienteFake cliente = new ClienteFake("cliente-vip", true);
        cliente.pontos = 100;
        EtapaFake etapa = new EtapaFake("vip-1", "Consultoria", "1000.00", true);
        ContratoFake contrato = contrato("contrato-vip", cliente, ModalidadePagamento.POR_ETAPA,
                Especializacao.TI, etapa);
        FaturamentoService service = new FaturamentoService();

        Fatura fatura = service.gerarFaturaPorEtapa(contrato, etapa.id, HOJE);
        verificar(fatura.getDescontoVip().compareTo(new BigDecimal("100.00")) == 0,
                "O desconto VIP deve ser 10% do valor bruto");
        verificar(fatura.getDescontoFidelidade().compareTo(new BigDecimal("45.00")) == 0,
                "O desconto fidelidade deve incidir após o desconto VIP");
        verificar(fatura.getValorLiquido().compareTo(new BigDecimal("855.00")) == 0,
                "O valor líquido deve refletir ambos os descontos");
        verificar(cliente.pontos == 0, "Os 100 pontos usados devem ser debitados ao preparar o contrato");

        service.registrarPagamento(fatura.getId(), fatura.getValorLiquido(), HOJE);
        verificar(cliente.pontos == 8, "R$ 855 pagos devem gerar 8 pontos");
        verificar(service.getPagamentos().get(0).getValor().compareTo(new BigDecimal("855.00")) == 0,
                "O pagamento registrado deve refletir o valor líquido da fatura");
    }

    private static void testaAcumuloDePontosEResgateComTeto() {
        ClienteFake cliente = new ClienteFake("cliente-pontos", false);
        FidelidadeService fidelidade = new FidelidadeService();
        fidelidade.registrarPagamento(cliente, new BigDecimal("90.00"));
        fidelidade.registrarPagamento(cliente, new BigDecimal("9.99"));
        verificar(cliente.pontos == 0, "Valores acumulados abaixo de R$ 100 não devem gerar ponto");
        fidelidade.registrarPagamento(cliente, new BigDecimal("0.01"));
        verificar(cliente.pontos == 1, "O saldo de pagamentos deve completar um ponto a cada R$ 100");

        cliente.pontos = 350;
        ContratoFake contrato = contrato("contrato-teto", cliente, ModalidadePagamento.POR_ETAPA,
                Especializacao.FINANCEIRO, new EtapaFake("teto-1", "Etapa", "100.00", true));
        verificar(fidelidade.prepararDesconto(contrato).compareTo(new BigDecimal("0.15")) == 0,
                "O desconto de fidelidade deve ter teto de 15%");
        verificar(fidelidade.getPontosResgatados(contrato.id) == 300,
                "O teto deve resgatar no máximo 300 pontos");
        verificar(cliente.pontos == 50, "Pontos acima do teto devem continuar no saldo");
        verificar(fidelidade.prepararDesconto(contrato).compareTo(new BigDecimal("0.15")) == 0,
                "Reutilizar o mesmo contrato não deve resgatar pontos novamente");

        ClienteFake contratoNovoCliente = new ClienteFake("cliente-proximo-contrato", false);
        contratoNovoCliente.pontos = 99;
        ContratoFake contratoJaIniciado = contrato("contrato-ja-iniciado", contratoNovoCliente,
                ModalidadePagamento.POR_ETAPA, Especializacao.GESTAO,
                new EtapaFake("iniciado-1", "Etapa", "100.00", true));
        verificar(fidelidade.registrarNovoContrato(contratoJaIniciado).signum() == 0,
                "O saldo deve ser reservado quando o contrato é criado");
        fidelidade.registrarPagamento(contratoNovoCliente, new BigDecimal("100.00"));
        verificar(fidelidade.prepararDesconto(contratoJaIniciado).signum() == 0,
                "Pontos ganhos após a criação não devem alterar o desconto daquele contrato");
        ContratoFake proximoContrato = contrato("proximo-contrato", contratoNovoCliente,
                ModalidadePagamento.POR_ETAPA, Especializacao.TI,
                new EtapaFake("proximo-1", "Etapa", "100.00", true));
        verificar(fidelidade.registrarNovoContrato(proximoContrato)
                        .compareTo(new BigDecimal("0.05")) == 0,
                "Pontos pagos devem conceder desconto ao próximo contrato");
    }

    private static void testaRecomendacoesParaHistoricoERelatorioVip() {
        ClienteFake vip = new ClienteFake("historico-vip", true);
        ClienteFake outroCliente = new ClienteFake("outro-cliente", false);
        ContratoFake financeiro = contrato("hist-fin", vip, ModalidadePagamento.ENTREGA_FINAL,
                Especializacao.FINANCEIRO, new EtapaFake("hist-1", "Financeiro", "10.00", true));
        ContratoFake tiDeOutroCliente = contrato("hist-ti-outro", outroCliente,
                ModalidadePagamento.ENTREGA_FINAL, Especializacao.TI,
                new EtapaFake("hist-2", "TI", "20.00", true));
        RecomendacaoService service = new RecomendacaoService();
        List<ContratoIntegracao> historico = List.of(financeiro, tiDeOutroCliente);

        List<Especializacao> esperadas = List.of(Especializacao.GESTAO, Especializacao.TI);
        verificar(service.recomendar(vip, historico).equals(esperadas),
                "Recomendações devem excluir apenas especializações do histórico daquele cliente");
        verificar(service.recomendarParaRelatorioVip(vip, historico).equals(esperadas),
                "O serviço deve fornecer recomendações para o relatório VIP");
        verificar(service.recomendarParaRelatorioVip(outroCliente, historico).isEmpty(),
                "Relatórios de clientes regulares não devem receber a seção VIP");
    }

    private static void testaClienteDoMenuIntegradoAoProgramaDeFidelidade() {
        Cliente cliente = new Cliente("cliente-menu", "Cliente do menu", "cliente@example.com",
                "Empresa", true);
        FidelidadeService fidelidade = new FidelidadeService();
        fidelidade.registrarPagamento(cliente, new BigDecimal("250.00"));

        verificar(cliente.isVip(), "O cadastro deve preservar a categoria VIP");
        verificar(cliente.getPontosFidelidade() == 2,
                "O cliente cadastrado deve acumular pontos pelas faturas pagas");
        verificar(cliente.getValorPagoRestanteParaPonto().compareTo(new BigDecimal("50.00")) == 0,
                "O saldo parcial para o próximo ponto deve permanecer no cliente");
    }

    private static ContratoFake contrato(String id, ClienteFake cliente, ModalidadePagamento modalidade,
                                         Especializacao especializacao, EtapaFake... etapas) {
        return new ContratoFake(id, cliente, modalidade, new ProjetoFake(especializacao, List.of(etapas)));
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }

    private static void esperar(Class<? extends Throwable> tipo, Runnable acao) {
        try {
            acao.run();
        } catch (Throwable erro) {
            if (tipo.isInstance(erro)) {
                return;
            }
            throw new AssertionError("Exceção inesperada", erro);
        }
        throw new AssertionError("Esperava " + tipo.getSimpleName());
    }

    private static final class ClienteFake implements ClienteIntegracao {
        private final String id;
        private final boolean vip;
        private int pontos;
        private BigDecimal resto = BigDecimal.ZERO;

        private ClienteFake(String id, boolean vip) {
            this.id = id;
            this.vip = vip;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isVip() {
            return vip;
        }

        @Override
        public int getPontosFidelidade() {
            return pontos;
        }

        @Override
        public void resgatarPontosFidelidade(int quantidade) {
            if (quantidade <= 0 || quantidade > pontos) {
                throw new IllegalArgumentException("Saldo de pontos insuficiente");
            }
            pontos -= quantidade;
        }

        @Override
        public void registrarPagamentoParaFidelidade(BigDecimal valorPago) {
            BigDecimal[] pontosEResto = resto.add(valorPago).divideAndRemainder(new BigDecimal("100.00"));
            pontos = Math.addExact(pontos, pontosEResto[0].intValueExact());
            resto = pontosEResto[1];
        }
    }

    private static final class EtapaFake implements EtapaIntegracao {
        private final String id;
        private final String nome;
        private final BigDecimal valor;
        private boolean concluida;
        private boolean faturada;

        private EtapaFake(String id, String nome, String valor, boolean concluida) {
            this.id = id;
            this.nome = nome;
            this.valor = new BigDecimal(valor);
            this.concluida = concluida;
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

        @Override
        public boolean isFaturada() {
            return faturada;
        }

        @Override
        public void marcarFaturada() {
            if (faturada) {
                throw new IllegalStateException("Etapa já faturada");
            }
            faturada = true;
        }
    }

    private static final class ProjetoFake implements ProjetoIntegracao {
        private final Especializacao especializacao;
        private final List<EtapaFake> etapas;
        private boolean entregaFaturada;

        private ProjetoFake(Especializacao especializacao, List<EtapaFake> etapas) {
            this.especializacao = especializacao;
            this.etapas = new ArrayList<>(etapas);
        }

        @Override
        public List<EtapaFake> getEtapas() {
            return List.copyOf(etapas);
        }

        @Override
        public Especializacao getEspecializacao() {
            return especializacao;
        }

        @Override
        public boolean isEntregaFinalFaturada() {
            return entregaFaturada;
        }

        @Override
        public void marcarEntregaFinalFaturada() {
            if (entregaFaturada) {
                throw new IllegalStateException("Entrega final já faturada");
            }
            entregaFaturada = true;
        }
    }

    private static final class ContratoFake implements ContratoIntegracao {
        private final String id;
        private final ClienteFake cliente;
        private final ModalidadePagamento modalidade;
        private final ProjetoFake projeto;

        private ContratoFake(String id, ClienteFake cliente, ModalidadePagamento modalidade,
                             ProjetoFake projeto) {
            this.id = id;
            this.cliente = cliente;
            this.modalidade = modalidade;
            this.projeto = projeto;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public ClienteFake getCliente() {
            return cliente;
        }

        @Override
        public ProjetoFake getProjeto() {
            return projeto;
        }

        @Override
        public ModalidadePagamento getModalidadePagamento() {
            return modalidade;
        }
    }
}
