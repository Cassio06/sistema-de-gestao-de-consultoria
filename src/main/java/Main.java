import br.com.planilhacerta.projetos.Etapa;
import br.com.planilhacerta.projetos.MonitoramentoProjetos;
import br.com.planilhacerta.projetos.Projeto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        System.out.println("================================");
        System.out.println("       PLANILHA CERTA");
        System.out.println("  Gerenciamento de Consultorias");
        System.out.println("================================");

        LocalDate hoje = LocalDate.now();

        Projeto projeto = new Projeto(
                1,
                101,
                201,
                "Consultoria Financeira",
                "Organizacao financeira empresarial",
                hoje.minusDays(10),
                hoje.plusDays(20)
        );

        Etapa analise = new Etapa(
                "Analise inicial",
                hoje.minusDays(3),
                new BigDecimal("500.00")
        );

        Etapa implementacao = new Etapa(
                "Implementacao",
                hoje.plusDays(2),
                new BigDecimal("1000.00")
        );

        Etapa revisao = new Etapa(
                "Revisao final",
                hoje.plusDays(15),
                new BigDecimal("500.00")
        );

        projeto.adicionarEtapa(analise);
        projeto.adicionarEtapa(implementacao);
        projeto.adicionarEtapa(revisao);

        analise.concluir(hoje.minusDays(4));
        implementacao.iniciar();

        MonitoramentoProjetos monitor =
                new MonitoramentoProjetos();

        monitor.verificarPrazos(projeto, hoje);
        monitor.exibirDesempenho(List.of(projeto), hoje);

        System.out.println("\nValor total: R$ "
                + projeto.getValorTotal());

        System.out.println("\nDemonstracao finalizada.");
    }
}
