package br.com.planilhacerta.app;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.enums.ModalidadePagamento;
import br.com.planilhacerta.domain.model.Cliente;
import br.com.planilhacerta.domain.model.Consultor;
import br.com.planilhacerta.domain.model.Contrato;
import br.com.planilhacerta.model.Fatura;
import br.com.planilhacerta.model.FaturaItem;
import br.com.planilhacerta.model.Pagamento;
import br.com.planilhacerta.projetos.Etapa;
import br.com.planilhacerta.projetos.Projeto;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public final class MenuIntegrado {
    private static final DateTimeFormatter DATA_BR =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final SistemaConsultoria sistema;
    private final Scanner entrada;

    public MenuIntegrado(SistemaConsultoria sistema) {
        this.sistema = Objects.requireNonNull(sistema);
        this.entrada = new Scanner(System.in);
    }

    public void iniciar() {
        try {
            while (true) {
                exibirMenu();
                int opcao = inteiro("Escolha uma opção: ", 0, 13);
                if (opcao == 0) {
                    return;
                }
                try {
                    executar(opcao);
                } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                    System.out.println("Não foi possível concluir a operação. " + e.getMessage());
                }
            }
        } catch (FimDaEntrada e) {
            System.out.println("\nEntrada encerrada.");
        }
    }

    private void exibirMenu() {
        System.out.println("""

                Planilha Certa — Gestão de Consultoria
                 1. Cadastrar cliente
                 2. Cadastrar consultor
                 3. Listar clientes e consultores
                 4. Criar contrato e projeto
                 5. Atualizar uma etapa
                 6. Emitir relatório de desempenho
                 7. Exportar CSV
                 8. Salvar dados
                 9. Carregar cenário de demonstração
                10. Emitir fatura
                11. Registrar pagamento
                12. Listar contratos, projetos, faturas e pagamentos
                13. Consultar fidelidade e recomendações
                 0. Sair
                """);
    }

    private void executar(int opcao) throws IOException {
        switch (opcao) {
            case 1 -> cadastrarCliente();
            case 2 -> cadastrarConsultor();
            case 3 -> listarPessoas();
            case 4 -> criarContrato();
            case 5 -> atualizarEtapa();
            case 6 -> sistema.emitirRelatorio(texto("ID do projeto: "));
            case 7 -> sistema.exportarCSV();
            case 8 -> {
                sistema.salvar();
                System.out.println("Dados salvos.");
            }
            case 9 -> {
                sistema.carregarDemonstracao();
                salvarAlteracao("Cenário de demonstração carregado.");
            }
            case 10 -> emitirFatura();
            case 11 -> pagar();
            case 12 -> listarOperacoes();
            case 13 -> consultarFidelidade();
            default -> throw new IllegalArgumentException("Opção inválida.");
        }
    }

    private void cadastrarCliente() {
        String id = texto("ID do cliente: ");
        String nome = texto("Nome do cliente: ");
        String email = texto("E-mail: ");
        boolean vip = inteiro("Categoria (1 Regular, 2 VIP): ", 1, 2) == 2;
        sistema.cadastrarCliente(id, nome, email, vip);
        salvarAlteracao("Cliente cadastrado.");
    }

    private void cadastrarConsultor() {
        String id = texto("ID do consultor: ");
        String nome = texto("Nome do consultor: ");
        String email = texto("E-mail: ");
        sistema.cadastrarConsultor(id, nome, email, especializacao());
        salvarAlteracao("Consultor cadastrado.");
    }

    private void listarPessoas() {
        System.out.println("\nClientes");
        List<Cliente> clientes = sistema.clientes();
        if (clientes.isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.");
        }
        for (Cliente cliente : clientes) {
            System.out.printf("%s | %s | %s | %s | %d contrato(s)%n", cliente.getId(), cliente.getNome(),
                    cliente.getEmail(), cliente.getCategoria(), cliente.getHistoricoContratos().size());
        }

        System.out.println("\nConsultores");
        List<Consultor> consultores = sistema.consultores();
        if (consultores.isEmpty()) {
            System.out.println("Nenhum consultor cadastrado.");
        }
        for (Consultor consultor : consultores) {
            System.out.printf("%s | %s | %s | %s | %d/%d projeto(s) ativo(s)%n",
                    consultor.getId(), consultor.getNome(), consultor.getEmail(), consultor.getEspecializacao(),
                    consultor.getQuantidadeProjetosAtivos(), Consultor.LIMITE_PROJETOS_ATIVOS);
        }
    }

    private void criarContrato() {
        String id = texto("ID do contrato: ");
        String clienteId = texto("ID do cliente: ");
        String projetoId = texto("ID do projeto: ");
        String nome = texto("Nome do projeto: ");
        Especializacao area = especializacao();
        LocalDate inicio = data("Início", false);
        LocalDate prazo = data("Prazo final", false);
        ModalidadePagamento forma = inteiro("Pagamento (1 por etapa, 2 entrega final): ", 1, 2) == 1
                ? ModalidadePagamento.POR_ETAPA : ModalidadePagamento.ENTREGA_FINAL;
        int quantidade = inteiro("Quantidade de etapas: ", 1, Integer.MAX_VALUE);
        List<SistemaConsultoria.DadosEtapa> etapas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            System.out.println("\nEtapa " + (i + 1));
            etapas.add(new SistemaConsultoria.DadosEtapa(texto("ID da etapa: "), texto("Nome da etapa: "),
                    data("Prazo da etapa", false), valor("Valor da etapa em R$: ")));
        }

        sistema.criarContrato(id, clienteId, projetoId, nome, area, inicio, prazo, forma, etapas);
        Projeto projeto = buscarProjeto(projetoId);
        String consultorId = projeto.getIdConsultor();
        String resultado = consultorId == null ? "Aguardando consultor disponível."
                : "Consultor alocado: " + consultorId + ".";
        salvarAlteracao("Contrato e projeto cadastrados. " + resultado);
    }

    private void atualizarEtapa() {
        String projetoId = texto("ID do projeto: ");
        Projeto projeto = buscarProjeto(projetoId);
        if (projeto.getEtapas().isEmpty()) {
            System.out.println("O projeto ainda não tem etapas.");
            return;
        }

        exibirEtapas(projeto);
        String etapaId = texto("ID da etapa: ");
        int opcao = inteiro("Ação (1 iniciar, 2 concluir): ", 1, 2);
        if (opcao == 1) {
            sistema.iniciarEtapa(projetoId, etapaId);
            salvarAlteracao("Etapa iniciada.");
            return;
        }

        sistema.concluirEtapa(projetoId, etapaId, data("Data de conclusão", true));
        salvarAlteracao(projeto.estaConcluido() ? "Etapa e projeto concluídos." : "Etapa concluída.");
    }

    private void emitirFatura() {
        String contratoId = texto("ID do contrato: ");
        Contrato contrato = sistema.contratos().stream()
                .filter(item -> item.getId().equals(contratoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contratoId));

        String etapaId = null;
        if (contrato.getModalidadePagamento() == ModalidadePagamento.POR_ETAPA) {
            exibirEtapas(buscarProjeto(contrato.getProjetoId()));
            etapaId = texto("ID da etapa concluída: ");
        }
        Fatura fatura = sistema.emitirFatura(contratoId, etapaId, data("Data de emissão", true));
        exibirFatura(fatura);
        salvarAlteracao("Fatura emitida.");
    }

    private void pagar() {
        String faturaId = texto("ID da fatura: ");
        Fatura fatura = sistema.faturas().stream()
                .filter(item -> item.getId().equals(faturaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada: " + faturaId));
        System.out.println("Total da fatura em R$: " + fatura.getValorLiquido().toPlainString());
        sistema.pagar(faturaId, valor("Valor do pagamento em R$: "), data("Data do pagamento", true));
        salvarAlteracao("Pagamento registrado.");
    }

    private void listarOperacoes() {
        System.out.println("\nContratos");
        List<Contrato> contratos = sistema.contratos();
        if (contratos.isEmpty()) {
            System.out.println("Nenhum contrato cadastrado.");
        }
        for (Contrato contrato : contratos) {
            System.out.printf("%s | Cliente %s | Projeto %s | %s | %s | R$ %s%n",
                    contrato.getId(), contrato.getCliente().getId(), contrato.getProjetoId(),
                    contrato.getEspecializacao(), contrato.getModalidadePagamento(), contrato.getValor().toPlainString());
        }

        System.out.println("\nProjetos");
        List<Projeto> projetos = sistema.projetos();
        if (projetos.isEmpty()) {
            System.out.println("Nenhum projeto cadastrado.");
        }
        for (Projeto projeto : projetos) {
            String consultorId = projeto.getIdConsultor();
            System.out.printf("%s | %s | %s | %.1f%% concluído%n",
                    projeto.getId(), projeto.getNome(),
                    consultorId == null ? "Aguardando consultor" : "Consultor " + consultorId,
                    projeto.calcularProgresso());
            exibirEtapas(projeto);
        }

        System.out.println("\nFaturas");
        List<Fatura> faturas = sistema.faturas();
        if (faturas.isEmpty()) {
            System.out.println("Nenhuma fatura emitida.");
        }
        faturas.forEach(this::exibirFatura);

        System.out.println("\nPagamentos");
        List<Pagamento> pagamentos = sistema.pagamentos();
        if (pagamentos.isEmpty()) {
            System.out.println("Nenhum pagamento registrado.");
        }
        for (Pagamento pagamento : pagamentos) {
            System.out.printf("%s | Fatura %s | R$ %s | %s%n", pagamento.getId(), pagamento.getFaturaId(),
                    pagamento.getValor().toPlainString(), pagamento.getData().format(DATA_BR));
        }
    }

    private void exibirFatura(Fatura fatura) {
        System.out.printf("%s | Contrato %s | %s | %s%n", fatura.getId(), fatura.getContratoId(),
                fatura.getDataEmissao().format(DATA_BR), fatura.isPaga() ? "Paga" : "Em aberto");
        for (FaturaItem item : fatura.getItens()) {
            System.out.printf("  %s | R$ %s%n", item.getDescricao(), item.getValor().toPlainString());
        }
        System.out.printf("  Bruto R$ %s | Desconto VIP R$ %s | Fidelidade R$ %s | Total R$ %s%n",
                fatura.getValorBruto().toPlainString(), fatura.getDescontoVip().toPlainString(),
                fatura.getDescontoFidelidade().toPlainString(), fatura.getValorLiquido().toPlainString());
    }

    private void consultarFidelidade() {
        String clienteId = texto("ID do cliente: ");
        System.out.println("Pontos de fidelidade: " + sistema.pontos(clienteId));
        var recomendacoes = sistema.recomendar(clienteId);
        if (recomendacoes.isEmpty()) {
            System.out.println("Nenhuma especialização recomendada.");
        } else {
            System.out.println("Especializações recomendadas: " + recomendacoes);
        }
    }

    private Projeto buscarProjeto(String id) {
        return sistema.projetos().stream()
                .filter(projeto -> projeto.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado: " + id));
    }

    private void exibirEtapas(Projeto projeto) {
        for (Etapa etapa : projeto.getEtapas()) {
            System.out.printf("  %s | %s | %s | prazo %s | R$ %s%n", etapa.getId(), etapa.getNome(),
                    etapa.getStatusAtual(), etapa.getPrazo().format(DATA_BR), etapa.getValor().toPlainString());
        }
    }

    private void salvarAlteracao(String mensagem) {
        System.out.println(mensagem);
        try {
            sistema.salvar();
        } catch (IOException e) {
            System.out.println("Alteração mantida em memória. Falha ao salvar: " + e.getMessage());
        }
    }

    private Especializacao especializacao() {
        int opcao = inteiro("Especialização (1 Financeiro, 2 Gestão, 3 TI): ", 1, 3);
        return switch (opcao) {
            case 1 -> Especializacao.FINANCEIRO;
            case 2 -> Especializacao.GESTAO;
            default -> Especializacao.TI;
        };
    }

    private String linha(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!entrada.hasNextLine()) {
            throw new FimDaEntrada();
        }
        return entrada.nextLine().trim();
    }

    private String texto(String prompt) {
        while (true) {
            String texto = linha(prompt);
            if (!texto.isBlank()) {
                return texto;
            }
            System.out.println("Preencha este campo.");
        }
    }

    private int inteiro(String prompt, int min, int max) {
        while (true) {
            try {
                int numero = Integer.parseInt(linha(prompt));
                if (numero >= min && numero <= max) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // A mesma orientação vale para texto e número fora do intervalo.
            }
            if (max == Integer.MAX_VALUE) {
                System.out.printf("Informe um número inteiro maior ou igual a %d.%n", min);
            } else {
                System.out.printf("Digite um número inteiro de %d a %d.%n", min, max);
            }
        }
    }

    private BigDecimal valor(String prompt) {
        while (true) {
            try {
                BigDecimal numero = new BigDecimal(linha(prompt).replace(',', '.'));
                if (numero.signum() > 0) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // A orientação abaixo também cobre zero e números negativos.
            }
            System.out.println("Informe um valor positivo, por exemplo 1500,00.");
        }
    }

    private LocalDate data(String campo, boolean hojeSeVazio) {
        String formato = hojeSeVazio ? " (dd/mm/aaaa ou Enter para hoje): " : " (dd/mm/aaaa): ";
        while (true) {
            String texto = linha(campo + formato);
            if (hojeSeVazio && texto.isBlank()) {
                return LocalDate.now();
            }
            try {
                return texto.contains("/") ? LocalDate.parse(texto, DATA_BR) : LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                System.out.println("Informe uma data válida no formato dd/mm/aaaa.");
            }
        }
    }

    private static final class FimDaEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
