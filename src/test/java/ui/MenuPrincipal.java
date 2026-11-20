package ui;

import model.ModelosSistema.*;
import persistence.PersistenciaService;
import report.RelatorioService;
import util.ConsoleHelper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

public class MenuPrincipal {
    private final PersistenciaService persistencia;
    private final RelatorioService relatorioService;
    private BancoDados banco;
    private boolean alteracoesPendentes;

    public MenuPrincipal() {
        this.persistencia = new PersistenciaService();
        this.relatorioService = new RelatorioService();
        this.banco = persistencia.carregarDados();
    }

    public void iniciar() {
        int opcao;
        do {
            exibirCabecalhoComDashboard();
            System.out.println("1. Cadastrar Novo Cliente (Regular ou VIP)");
            System.out.println("2. Cadastrar Novo Consultor");
            System.out.println("3. Listar Pessoas Cadastradas (Clientes e Consultores)");
            System.out.println("4. Criar Projeto / Contrato (Alocação Automática)");
            System.out.println("5. Atualizar Status de Etapa de Projeto");
            System.out.println("6. Emitir Relatório de Desempenho (Console + PDF iText)");
            System.out.println("7. Exportar Planilha Gerencial (.CSV para Excel)");
            System.out.println("8. Salvar Dados Manualmente (JSON + Backup + Google Drive)");
            System.out.println("9. [Modelo Rápido] Carregar Cenário Pré-Pronto para Demonstração");
            System.out.println("0. Sair");
            System.out.println("======================================================");

            opcao = ConsoleHelper.lerInteiro("Escolha uma opção: ", 0, 9);

            switch (opcao) {
                case 1 -> cadastrarCliente();
                case 2 -> cadastrarConsultor();
                case 3 -> listarPessoas();
                case 4 -> criarProjeto();
                case 5 -> atualizarEtapa();
                case 6 -> emitirRelatorio();
                case 7 -> persistencia.exportarCSV(banco);
                case 8 -> salvarAlteracoes();
                case 9 -> carregarModeloPrePronto();
                case 0 -> {
                    if (alteracoesPendentes && !salvarAlteracoes()) {
                        System.out.println("[AVISO] Encerrando com alterações que não puderam ser gravadas.");
                    }
                    System.out.println("Encerrando o sistema Planilha Certa. Até logo!");
                }
            }
        } while (opcao != 0);
    }

    private void exibirCabecalhoComDashboard() {
        long totalVips = banco.clientes.stream().filter(Cliente::isVip).count();
        long totalAtrasadas = banco.projetos.stream()
                .mapToLong(Projeto::contarEtapasAtrasadas)
                .sum();

        System.out.println("\n======================================================");
        System.out.println("      SISTEMA DE CONSULTORIA - PLANILHA CERTA");
        System.out.println("======================================================");
        System.out.printf("[Resumo] Clientes: %d (%d VIP) | Consultores: %d | Projetos: %d%n",
                banco.clientes.size(), totalVips, banco.consultores.size(), banco.projetos.size());
        if (totalAtrasadas > 0) {
            System.out.printf("[ALERTA] Atenção: %d etapa(s) com prazo ATRASADO!%n", totalAtrasadas);
        } else {
            System.out.println("[Status] Todas as etapas estão dentro do prazo.");
        }
        System.out.println("------------------------------------------------------");
    }

    private void cadastrarCliente() {
        System.out.println("\n--- CADASTRO DE CLIENTE ---");
        String nome = ConsoleHelper.lerTextoObrigatorio("Nome do Cliente / Empresa: ");
        String email = ConsoleHelper.lerTextoObrigatorio("E-mail: ");
        String telefone = ConsoleHelper.lerTextoObrigatorio("Telefone: ");
        int tipo = ConsoleHelper.lerInteiro("Categoria (1 - Regular | 2 - VIP): ", 1, 2);

        String id = "CLI-" + (banco.clientes.size() + 1);
        Cliente novo = new Cliente(id, nome, email, telefone, tipo == 2);
        banco.clientes.add(novo);

        if (salvarAlteracoes()) {
            System.out.printf("Cliente '%s' (%s) cadastrado com ID %s!%n",
                    novo.getNome(), novo.getCategoriaTexto(), novo.getId());
        } else {
            System.out.println("[AVISO] Cliente mantido em memória, mas houve falha ao gravar no arquivo.");
        }
    }

    private void cadastrarConsultor() {
        System.out.println("\n--- CADASTRO DE CONSULTOR ---");
        String nome = ConsoleHelper.lerTextoObrigatorio("Nome do Consultor: ");
        String email = ConsoleHelper.lerTextoObrigatorio("E-mail: ");
        String telefone = ConsoleHelper.lerTextoObrigatorio("Telefone: ");
        Especializacao esp = escolherEspecializacao();

        String id = "CON-" + (banco.consultores.size() + 1);
        Consultor novo = new Consultor(id, nome, email, telefone, esp);
        banco.consultores.add(novo);

        if (salvarAlteracoes()) {
            System.out.printf("Consultor '%s' (%s) cadastrado com ID %s!%n",
                    novo.getNome(), esp.getDescricao(), novo.getId());
        } else {
            System.out.println("[AVISO] Consultor mantido em memória, mas houve falha ao gravar no arquivo.");
        }
    }

    private void listarPessoas() {
        System.out.println("\n--- CLIENTES CADASTRADOS ---");
        if (banco.clientes.isEmpty()) {
            System.out.println("Nenhum cliente cadastrado.");
        } else {
            for (Cliente c : banco.clientes) {
                System.out.printf("[%s] %s | %s | E-mail: %s | Pontos: %d%n",
                        c.getId(), c.getNome(), c.getCategoriaTexto(), c.getEmail(), c.getPontosFidelidade());
            }
        }

        System.out.println("\n--- CONSULTORES CADASTRADOS ---");
        if (banco.consultores.isEmpty()) {
            System.out.println("Nenhum consultor cadastrado.");
        } else {
            for (Consultor con : banco.consultores) {
                System.out.printf("[%s] %s | Área: %s | Projetos Ativos: %d/3%n",
                        con.getId(), con.getNome(), con.getEspecializacao().getDescricao(), con.getProjetosAtivos());
            }
        }
        ConsoleHelper.pausar();
    }

    private void criarProjeto() {
        if (banco.clientes.isEmpty() || banco.consultores.isEmpty()) {
            System.out.println("[AVISO] Cadastre pelo menos 1 cliente e 1 consultor antes de criar um projeto.");
            return;
        }

        System.out.println("\n--- NOVO PROJETO / CONTRATO DE CONSULTORIA ---");
        for (int i = 0; i < banco.clientes.size(); i++) {
            Cliente c = banco.clientes.get(i);
            System.out.printf("%d. %s (%s)%n", i + 1, c.getNome(), c.getCategoriaTexto());
        }
        int idxCliente = ConsoleHelper.lerInteiro("Selecione o cliente: ", 1, banco.clientes.size()) - 1;
        Cliente cliente = banco.clientes.get(idxCliente);

        String titulo = ConsoleHelper.lerTextoObrigatorio("Título do Projeto: ");
        Especializacao area = escolherEspecializacao();

        // Alocação automática: consultor da área com menos projetos ativos (máx 3)
        Optional<Consultor> consultorEscolhido = banco.consultores.stream()
                .filter(c -> c.getEspecializacao() == area && c.podeReceberProjeto())
                .min(Comparator.comparingInt(Consultor::getProjetosAtivos));

        if (consultorEscolhido.isEmpty()) {
            System.out.println("[ERRO] Não há consultores disponíveis nesta área (limite máximo de 3 projetos atingido).");
            return;
        }

        Consultor consultor = consultorEscolhido.get();
        consultor.incrementarProjetos();
        cliente.registrarEspecializacao(area);

        String idProj = "PRJ-" + (banco.projetos.size() + 1);
        Projeto projeto = new Projeto(idProj, titulo, cliente.getId(), consultor.getId(), area);

        System.out.println("Configure as 3 etapas padrão do projeto:");
        String[] nomesEtapas = {"Análise Inicial", "Implementação", "Revisão Final"};
        for (String nomeEtapa : nomesEtapas) {
            System.out.println(">> Etapa: " + nomeEtapa);
            LocalDate prazo = ConsoleHelper.lerData("   Prazo de entrega");
            double valor = ConsoleHelper.lerDecimalPositivo("   Valor da etapa (R$): ");
            if (cliente.isVip()) {
                valor *= 0.90; // 10% desconto VIP
                System.out.printf("   [Desconto VIP 10%% aplicado] Valor final da etapa: R$ %.2f%n", valor);
            }
            projeto.getEtapas().add(new Etapa(nomeEtapa, prazo, valor));
        }

        banco.projetos.add(projeto);

        if (salvarAlteracoes()) {
            System.out.printf("Projeto '%s' criado! Consultor alocado automaticamente: %s.%n",
                    projeto.getTitulo(), consultor.getNome());
            System.out.printf("[CONTRATO & FATURAMENTO] Contrato CTR-%s gerado | Cliente: %s | Valor Total: R$ %.2f%n",
                    projeto.getId(), cliente.getNome(), projeto.calcularValorTotal());
        } else {
            System.out.println("[AVISO] Projeto criado em memória, mas houve falha ao gravar no arquivo.");
        }
    }

    private void atualizarEtapa() {
        if (banco.projetos.isEmpty()) {
            System.out.println("[AVISO] Nenhum projeto cadastrado.");
            return;
        }

        Projeto projeto = selecionarProjeto();
        for (int i = 0; i < projeto.getEtapas().size(); i++) {
            Etapa e = projeto.getEtapas().get(i);
            System.out.printf("%d. %s [%s]%n", i + 1, e.getNome(), e.getStatusAtualizado());
        }

        int idxEtapa = ConsoleHelper.lerInteiro("Escolha a etapa: ", 1, projeto.getEtapas().size()) - 1;
        Etapa etapa = projeto.getEtapas().get(idxEtapa);

        int novoStatus = ConsoleHelper.lerInteiro("Novo status (1 - Em Andamento | 2 - Concluída): ", 1, 2);

        if (etapa.getStatusAtualizado() == StatusEtapa.CONCLUIDA) {
            System.out.println("[AVISO] Esta etapa já está CONCLUÍDA. Nenhuma alteração ou pontuação extra foi aplicada.");
            return;
        }

        if (novoStatus == 1) {
            etapa.setStatus(StatusEtapa.EM_ANDAMENTO);
        } else {
            etapa.setStatus(StatusEtapa.CONCLUIDA);
            // Credita pontos de fidelidade (1 ponto a cada R$ 100 pagos)
            Cliente cliente = buscarClientePorId(projeto.getClienteId());
            if (cliente != null) {
                int pontos = (int) (etapa.getValor() / 100);
                cliente.adicionarPontos(pontos);
                System.out.printf("[FIDELIDADE] +%d pontos creditados para %s!%n", pontos, cliente.getNome());
            }
        }

        if (salvarAlteracoes()) {
            System.out.println("Status da etapa atualizado com sucesso!");
        } else {
            System.out.println("[AVISO] Status atualizado em memória, mas houve falha ao gravar no arquivo.");
        }
    }

    private void emitirRelatorio() {
        if (banco.projetos.isEmpty()) {
            System.out.println("[AVISO] Nenhum projeto cadastrado para gerar relatório.");
            return;
        }

        Projeto projeto = selecionarProjeto();
        Cliente cliente = buscarClientePorId(projeto.getClienteId());
        Consultor consultor = buscarConsultorPorId(projeto.getConsultorId());

        relatorioService.exibirRelatorioConsole(projeto, cliente, consultor);
        relatorioService.gerarRelatorioPDF(projeto, cliente, consultor);
        ConsoleHelper.pausar();
    }

    private void carregarModeloPrePronto() {
        if (buscarClientePorId("CLI-DEMO1") != null) {
            System.out.println("[AVISO] O cenário de demonstração já está cadastrado.");
            return;
        }
        Cliente cVip = new Cliente("CLI-DEMO1", "TechSolutions Ltda", "contato@techsolutions.com", "(86) 99999-0001", true);
        cVip.registrarEspecializacao(Especializacao.FINANCEIRO);
        cVip.adicionarPontos(120);

        Consultor con1 = new Consultor("CON-DEMO1", "Mariana Costa", "mariana@planilhacerta.com", "(86) 98888-1111", Especializacao.FINANCEIRO);
        con1.incrementarProjetos();

        Projeto proj = new Projeto("PRJ-DEMO1", "Reestruturação Fluxo de Caixa", cVip.getId(), con1.getId(), Especializacao.FINANCEIRO);
        Etapa e1 = new Etapa("Análise Inicial", LocalDate.now().minusDays(2), 1800.0);
        e1.setStatus(StatusEtapa.CONCLUIDA);
        Etapa e2 = new Etapa("Implementação", LocalDate.now().minusDays(1), 2700.0); // Ficará ATRASADA automaticamente
        Etapa e3 = new Etapa("Revisão Final", LocalDate.now().plusDays(10), 1350.0);

        proj.getEtapas().add(e1);
        proj.getEtapas().add(e2);
        proj.getEtapas().add(e3);

        banco.clientes.add(cVip);
        banco.consultores.add(con1);
        banco.projetos.add(proj);

        if (salvarAlteracoes()) {
            System.out.println("[MODELO] Cenário pré-pronto carregado com sucesso (1 Cliente VIP, 1 Consultor e 1 Projeto com etapas)!");
        } else {
            System.out.println("[AVISO] Cenário carregado em memória, mas houve falha ao gravar no arquivo.");
        }
    }

    private boolean salvarAlteracoes() {
        alteracoesPendentes = !persistencia.salvarDados(banco);
        return !alteracoesPendentes;
    }

    private Especializacao escolherEspecializacao() {
        System.out.println("Especializações: 1 - Financeiro | 2 - Gestão | 3 - TI");
        int op = ConsoleHelper.lerInteiro("Escolha a especialização: ", 1, 3);
        return switch (op) {
            case 1 -> Especializacao.FINANCEIRO;
            case 2 -> Especializacao.GESTAO;
            default -> Especializacao.TI;
        };
    }

    private Projeto selecionarProjeto() {
        for (int i = 0; i < banco.projetos.size(); i++) {
            Projeto p = banco.projetos.get(i);
            System.out.printf("%d. [%s] %s (Progresso: %.0f%%)%n",
                    i + 1, p.getId(), p.getTitulo(), p.calcularProgresso());
        }
        int idx = ConsoleHelper.lerInteiro("Selecione o projeto: ", 1, banco.projetos.size()) - 1;
        return banco.projetos.get(idx);
    }

    private Cliente buscarClientePorId(String id) {
        return banco.clientes.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private Consultor buscarConsultorPorId(String id) {
        return banco.consultores.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    public static void main(String[] args) {
        new MenuPrincipal().iniciar();
    }
}
