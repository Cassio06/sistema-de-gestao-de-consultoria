package br.com.planilhacerta.app;

import br.com.planilhacerta.domain.enums.Especializacao;
import br.com.planilhacerta.domain.model.Cliente;
import br.com.planilhacerta.domain.model.ClienteRegular;
import br.com.planilhacerta.domain.model.ClienteVIP;
import br.com.planilhacerta.domain.model.Consultor;
import br.com.planilhacerta.service.ClienteService;
import br.com.planilhacerta.service.ConsultorService;

public class Main {
    public static void main(String[] args) {
        ClienteService clientes = new ClienteService();
        ConsultorService consultores = new ConsultorService();

        clientes.cadastrar(new ClienteRegular("CLI-001", "Cliente Regular de Exemplo", "regular@exemplo.com"));
        clientes.cadastrar(new ClienteVIP("CLI-002", "Cliente VIP de Exemplo", "vip@exemplo.com"));
        consultores.cadastrar(new Consultor("CON-001", "Consultor de Exemplo", "consultor@exemplo.com",
                Especializacao.FINANCEIRO));

        System.out.println("Planilha Certa — esqueleto da parte de Cássio");
        for (Cliente cliente : clientes.listar()) {
            System.out.printf("%s | %s | prioridade VIP: %s%n", cliente.getNome(),
                    cliente.getCategoria(), cliente.temPrioridadeNaAlocacao());
        }
        System.out.println("Consultores cadastrados: " + consultores.listar().size());
        System.out.println("Dados de demonstração em memória; não são salvos ao encerrar.");
        System.out.println("Histórico, cadastro de contratos e alocação: implemente os TODOs de docs/ESCOPO_CASSIO.md.");
    }
}
