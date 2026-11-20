package br.com.planilhacerta.view;

import br.com.planilhacerta.integration.ContratoIntegracao;
import br.com.planilhacerta.model.Cliente;
import br.com.planilhacerta.model.Especializacao;
import br.com.planilhacerta.model.Fatura;
import br.com.planilhacerta.model.FaturaItem;
import br.com.planilhacerta.model.ModalidadePagamento;
import br.com.planilhacerta.model.Pagamento;
import br.com.planilhacerta.service.FaturamentoService;
import br.com.planilhacerta.service.FidelidadeService;
import br.com.planilhacerta.service.RecomendacaoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GerenciadorFaturamento {
    private final Scanner scanner;
    private final GerenciadorClientes gerenciadorClientes;
    private final FidelidadeService fidelidadeService = new FidelidadeService();
    private final FaturamentoService faturamentoService = new FaturamentoService(fidelidadeService);
    private final RecomendacaoService recomendacaoService = new RecomendacaoService();
    private final List<ContratoConsoleAdapter> contratos = new ArrayList<>();

    public GerenciadorFaturamento(Scanner scanner, GerenciadorClientes gerenciadorClientes) {
        this.scanner = scanner;
        this.gerenciadorClientes = gerenciadorClientes;
    }

    public void criarContrato() {
        if (gerenciadorClientes.getClientes().isEmpty()) {
            System.out.println("Cadastre um cliente antes de criar um contrato.");
            return;
        }

        Cliente cliente = selecionarCliente();
        if (cliente == null) {
            return;
        }

        System.out.print("ID do contrato: ");
        String contratoId = scanner.nextLine().trim();
        if (contratoId.isEmpty() || contratos.stream().anyMatch(c -> c.getId().equals(contratoId))) {
            System.out.println("ID vazio ou já utilizado.");
            return;
        }

        Especializacao especializacao = selecionarEspecializacao();
        ModalidadePagamento modalidade = selecionarModalidade();
        int quantidadeEtapas = lerInteiro("Quantidade de etapas: ", 1);
        List<ContratoConsoleAdapter.Etapa> etapas = new ArrayList<>();
        for (int indice = 1; indice <= quantidadeEtapas; indice++) {
            System.out.println("Etapa " + indice + ":");
            System.out.print("  ID: ");
            String etapaId = scanner.nextLine().trim();
            if (etapaId.isEmpty() || etapas.stream().anyMatch(e -> e.getId().equals(etapaId))) {
                System.out.println("ID de etapa vazio ou repetido; tente novamente.");
                indice--;
                continue;
            }
            System.out.print("  Nome: ");
            String nome = scanner.nextLine().trim();
            BigDecimal valor = lerValor("  Valor (R$): ");
            try {
                etapas.add(new ContratoConsoleAdapter.Etapa(etapaId, nome, valor));
            } catch (IllegalArgumentException erro) {
                System.out.println("Etapa inválida: " + erro.getMessage());
                indice--;
            }
        }

        ContratoConsoleAdapter contrato = new ContratoConsoleAdapter(
                contratoId, cliente,
                new ContratoConsoleAdapter.Projeto(especializacao, etapas), modalidade);
        BigDecimal percentualFidelidade = fidelidadeService.registrarNovoContrato(contrato);
        contratos.add(contrato);
        System.out.println("Contrato cadastrado. Desconto de fidelidade reservado: "
                + percentualFidelidade.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString()
                + "%; pontos resgatados: " + fidelidadeService.getPontosResgatados(contratoId) + ".");
    }

    public void listarContratos() {
        if (contratos.isEmpty()) {
            System.out.println("Nenhum contrato cadastrado.");
            return;
        }
        System.out.println("\n--- CONTRATOS ---");
        for (ContratoConsoleAdapter contrato : contratos) {
            System.out.println("Contrato " + contrato.getId() + " | Cliente: "
                    + contrato.getCliente().getId() + " | " + contrato.getModalidadePagamento()
                    + " | " + contrato.getProjeto().getEspecializacao());
            for (ContratoConsoleAdapter.Etapa etapa : contrato.getProjeto().getEtapas()) {
                System.out.println("  " + etapa.getId() + " - " + etapa.getNome()
                        + " | R$ " + etapa.getValor().toPlainString()
                        + " | " + (etapa.isConcluida() ? "Concluída" : "Pendente")
                        + (etapa.isFaturada() ? " | Faturada" : ""));
            }
        }
    }

    public void concluirEtapa() {
        ContratoConsoleAdapter contrato = selecionarContrato();
        if (contrato == null) {
            return;
        }
        ContratoConsoleAdapter.Etapa etapa = selecionarEtapa(contrato);
        if (etapa == null) {
            return;
        }
        if (etapa.isFaturada()) {
            System.out.println("A etapa já foi faturada e não pode ser alterada.");
            return;
        }
        etapa.concluir();
        System.out.println("Etapa marcada como concluída.");
    }

    public void emitirFatura() {
        ContratoConsoleAdapter contrato = selecionarContrato();
        if (contrato == null) {
            return;
        }

        try {
            Fatura fatura;
            if (contrato.getModalidadePagamento() == ModalidadePagamento.POR_ETAPA) {
                ContratoConsoleAdapter.Etapa etapa = selecionarEtapa(contrato);
                if (etapa == null) {
                    return;
                }
                fatura = faturamentoService.gerarFaturaPorEtapa(
                        contrato, etapa.getId(), LocalDate.now());
            } else {
                fatura = faturamentoService.gerarFaturaEntregaFinal(contrato, LocalDate.now());
            }
            exibirFatura(fatura);
            if (confirmar("Registrar o pagamento desta fatura agora? (s/n): ")) {
                registrarPagamento(fatura);
            } else {
                System.out.println("Fatura emitida e pendente. Selecione novamente para registrar o pagamento.");
            }
        } catch (IllegalArgumentException | IllegalStateException erro) {
            System.out.println("Não foi possível emitir a fatura: " + erro.getMessage());
        }
    }

    public void listarFaturas() {
        List<Fatura> faturas = faturamentoService.getFaturas();
        if (faturas.isEmpty()) {
            System.out.println("Nenhuma fatura emitida.");
            return;
        }
        System.out.println("\n--- FATURAS ---");
        for (Fatura fatura : faturas) {
            System.out.println(fatura.getId() + " | Contrato: " + fatura.getContratoId()
                    + " | R$ " + fatura.getValorLiquido().toPlainString()
                    + " | " + (fatura.isPaga() ? "Paga" : "Pendente"));
        }
        if (confirmar("Registrar pagamento de uma fatura pendente? (s/n): ")) {
            registrarPagamentoPendente(faturas);
        }
    }

    public void consultarFidelidade() {
        if (gerenciadorClientes.getClientes().isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.");
            return;
        }
        Cliente cliente = selecionarCliente();
        if (cliente == null) {
            return;
        }
        System.out.println("Cliente: " + cliente.getNome() + " | Pontos: "
                + cliente.getPontosFidelidade() + " | Valor acumulado para o próximo ponto: R$ "
                + cliente.getValorPagoRestanteParaPonto().toPlainString()
                + " | Desconto disponível no próximo contrato: "
                + fidelidadeService.calcularDescontoDisponivel(cliente)
                .multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
    }

    public void consultarRecomendacoes() {
        if (gerenciadorClientes.getClientes().isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.");
            return;
        }
        Cliente cliente = selecionarCliente();
        if (cliente == null) {
            return;
        }
        List<? extends ContratoIntegracao> historico = List.copyOf(contratos);
        List<Especializacao> recomendacoes = cliente.isVip()
                ? recomendacaoService.recomendarParaRelatorioVip(cliente, historico)
                : recomendacaoService.recomendar(cliente, historico);
        if (recomendacoes.isEmpty()) {
            System.out.println(cliente.isVip()
                    ? "Nenhuma recomendação disponível."
                    : "Este cliente já contratou todas as especializações.");
            return;
        }
        System.out.println("Especializações recomendadas: " + recomendacoes);
    }

    private void registrarPagamentoPendente(List<Fatura> faturas) {
        System.out.print("ID da fatura: ");
        String faturaId = scanner.nextLine().trim();
        Fatura fatura = faturas.stream()
                .filter(item -> item.getId().equals(faturaId))
                .findFirst()
                .orElse(null);
        if (fatura == null || fatura.isPaga()) {
            System.out.println("Fatura pendente não encontrada.");
            return;
        }
        registrarPagamento(fatura);
    }

    private void registrarPagamento(Fatura fatura) {
        try {
            int pontosAntes = contratos.stream()
                    .filter(contrato -> contrato.getId().equals(fatura.getContratoId()))
                    .map(ContratoConsoleAdapter::getCliente)
                    .mapToInt(Cliente::getPontosFidelidade)
                    .findFirst()
                    .orElse(0);
            Pagamento pagamento = faturamentoService.registrarPagamento(
                    fatura.getId(), fatura.getValorLiquido(), LocalDate.now());
            Cliente cliente = contratos.stream()
                    .filter(contrato -> contrato.getId().equals(fatura.getContratoId()))
                    .map(ContratoConsoleAdapter::getCliente)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cliente do contrato não encontrado"));
            int pontosGanhos = cliente.getPontosFidelidade() - pontosAntes;
            System.out.println("Pagamento registrado: R$ " + pagamento.getValor().toPlainString()
                    + ". Pontos creditados: " + pontosGanhos + ".");
        } catch (IllegalArgumentException | IllegalStateException erro) {
            System.out.println("Não foi possível registrar o pagamento: " + erro.getMessage());
        }
    }

    private void exibirFatura(Fatura fatura) {
        System.out.println("\n--- FATURA " + fatura.getId() + " ---");
        for (FaturaItem item : fatura.getItens()) {
            System.out.println(item.getDescricao() + ": R$ " + item.getValor().toPlainString());
        }
        System.out.println("Subtotal: R$ " + fatura.getValorBruto().toPlainString());
        System.out.println("Desconto VIP: R$ " + fatura.getDescontoVip().toPlainString());
        System.out.println("Desconto fidelidade: R$ "
                + fatura.getDescontoFidelidade().toPlainString());
        System.out.println("Total: R$ " + fatura.getValorLiquido().toPlainString());
    }

    private Cliente selecionarCliente() {
        System.out.print("ID do cliente (vazio para cancelar): ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            return null;
        }
        Cliente cliente = gerenciadorClientes.buscarPorId(id);
        if (cliente == null) {
            System.out.println("Cliente não encontrado.");
        }
        return cliente;
    }

    private ContratoConsoleAdapter selecionarContrato() {
        if (contratos.isEmpty()) {
            System.out.println("Nenhum contrato cadastrado.");
            return null;
        }
        System.out.print("ID do contrato (vazio para cancelar): ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            return null;
        }
        ContratoConsoleAdapter contrato = contratos.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (contrato == null) {
            System.out.println("Contrato não encontrado.");
        }
        return contrato;
    }

    private ContratoConsoleAdapter.Etapa selecionarEtapa(ContratoConsoleAdapter contrato) {
        System.out.println("Etapas do contrato:");
        for (ContratoConsoleAdapter.Etapa etapa : contrato.getProjeto().getEtapas()) {
            System.out.println(etapa.getId() + " - " + etapa.getNome()
                    + " (" + (etapa.isConcluida() ? "Concluída" : "Pendente") + ")");
        }
        System.out.print("ID da etapa (vazio para cancelar): ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            return null;
        }
        ContratoConsoleAdapter.Etapa etapa = contrato.getProjeto().getEtapas().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
        if (etapa == null) {
            System.out.println("Etapa não encontrada.");
        }
        return etapa;
    }

    private Especializacao selecionarEspecializacao() {
        Especializacao[] especializacoes = Especializacao.values();
        System.out.println("Especialização:");
        for (int indice = 0; indice < especializacoes.length; indice++) {
            System.out.println((indice + 1) + ". " + especializacoes[indice]);
        }
        return especializacoes[lerInteiro("Opção: ", 1, especializacoes.length) - 1];
    }

    private ModalidadePagamento selecionarModalidade() {
        System.out.println("Modalidade de pagamento:");
        System.out.println("1. Por etapa concluída");
        System.out.println("2. Na entrega final");
        return lerInteiro("Opção: ", 1, 2) == 1
                ? ModalidadePagamento.POR_ETAPA : ModalidadePagamento.ENTREGA_FINAL;
    }

    private int lerInteiro(String prompt, int minimo) {
        return lerInteiro(prompt, minimo, Integer.MAX_VALUE);
    }

    private int lerInteiro(String prompt, int minimo, int maximo) {
        while (true) {
            System.out.print(prompt);
            try {
                int valor = Integer.parseInt(scanner.nextLine().trim());
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
            } catch (NumberFormatException erro) {
                System.out.println("Digite um número válido.");
                continue;
            }
            System.out.println("Escolha um valor entre " + minimo + " e " + maximo + ".");
        }
    }

    private BigDecimal lerValor(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                BigDecimal valor = new BigDecimal(scanner.nextLine().trim().replace(',', '.'));
                if (valor.signum() < 0) {
                    System.out.println("O valor não pode ser negativo.");
                    continue;
                }
                return valor.setScale(2, RoundingMode.UNNECESSARY);
            } catch (NumberFormatException erro) {
                System.out.println("Digite um valor monetário válido.");
            } catch (ArithmeticException erro) {
                System.out.println("O valor deve ter no máximo duas casas decimais.");
            }
        }
    }

    private boolean confirmar(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim().equalsIgnoreCase("s");
    }
}
