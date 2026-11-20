package br.com.planilhacerta.app;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.enums.ModalidadePagamento;
import br.com.planilhacerta.domain.model.Cliente;
import br.com.planilhacerta.domain.model.ClienteRegular;
import br.com.planilhacerta.domain.model.ClienteVIP;
import br.com.planilhacerta.domain.model.Consultor;
import br.com.planilhacerta.domain.model.Contrato;
import br.com.planilhacerta.service.AlocacaoService;
import br.com.planilhacerta.service.ClienteService;
import br.com.planilhacerta.service.ConsultorService;
import br.com.planilhacerta.service.ContratoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        ClienteService clientes = new ClienteService();
        ConsultorService consultores = new ConsultorService();
        ContratoService contratos = new ContratoService();
        AlocacaoService alocacao = new AlocacaoService();

        Cliente regular = new ClienteRegular("CLI-001", "Cliente Regular de Exemplo", "regular@example.test");
        Cliente vip = new ClienteVIP("CLI-002", "Cliente VIP de Exemplo", "vip@example.test");
        clientes.cadastrar(regular);
        clientes.cadastrar(vip);

        Consultor financeiro = new Consultor("CON-001", "Consultora Financeira", "financeiro@example.test",
                Especializacao.FINANCEIRO);
        financeiro.adicionarProjeto("PRJ-ATIVO-01");
        financeiro.adicionarProjeto("PRJ-ATIVO-02");
        consultores.cadastrar(financeiro);
        consultores.cadastrar(new Consultor("CON-002", "Consultor de TI", "ti@example.test", Especializacao.TI));

        LocalDate hoje = LocalDate.now();
        Contrato contratoRegular = new Contrato("CTR-001", regular, "PRJ-001", Especializacao.FINANCEIRO,
                hoje, hoje.plusDays(30), new BigDecimal("1500.00"), ModalidadePagamento.ENTREGA_FINAL);
        Contrato contratoVip = new Contrato("CTR-002", vip, "PRJ-002", Especializacao.FINANCEIRO,
                hoje, hoje.plusDays(15), new BigDecimal("2400.00"), ModalidadePagamento.POR_ETAPA);
        contratos.cadastrar(contratoRegular);
        contratos.cadastrar(contratoVip);

        System.out.println("Planilha Certa — Entidades, cadastros e alocação");
        for (Cliente cliente : clientes.listar()) {
            System.out.printf("%s | %s | Contratos no histórico: %d%n", cliente.getNome(), cliente.getCategoria(),
                    cliente.getHistoricoContratos().size());
        }

        System.out.println("\nFila de alocação com uma vaga em Financeiro");
        List<Consultor> disponiveis = consultores.listar();
        for (Contrato contrato : alocacao.ordenarPorPrioridade(contratos.listar())) {
            exibirAlocacao(contrato, alocacao.alocar(contrato, disponiveis));
        }

        financeiro.removerProjeto(contratoVip.getProjetoId());
        System.out.println("\nVaga liberada após a conclusão do projeto " + contratoVip.getProjetoId());
        exibirAlocacao(contratoRegular, alocacao.alocar(contratoRegular, disponiveis));
    }

    private static void exibirAlocacao(Contrato contrato, Optional<Consultor> consultor) {
        System.out.printf("%s | %s | %s%n", contrato.getProjetoId(), contrato.getCliente().getCategoria(),
                consultor.map(Consultor::getNome).orElse("Aguardando consultor disponível"));
    }
}
