# Arquitetura do sistema integrado

O JAR executa `br.com.planilhacerta.app.Main`. O menu trata entradas, delega operações a `SistemaConsultoria` e solicita a gravação depois de cada alteração.

- `domain.model` e `service` registram clientes, consultores e contratos. O histórico pertence ao cliente. A fila usa a categoria do cliente por polimorfismo.
- `projetos` representa projetos e etapas. O contrato e o projeto compartilham o mesmo ID. A conclusão de todas as etapas libera a vaga e reprocessa a fila.
- As interfaces de `integration` ligam os contratos e etapas aos serviços de faturamento, fidelidade e recomendações. Os valores usam `BigDecimal`.
- `RepositorioIntegrado` salva os registros em JSON com Gson e restaura contratos, etapas, alocações, faturas, pagamentos e fidelidade. O backup conserva a versão válida anterior.
- `RelatoriosIntegrados` adapta os dados para os formatos de console, CSV e PDF com iText.

Os testes JUnit verificam os serviços e os fluxos integrados. O script Windows `run.ps1 -Test` também executa os roteiros de regressão que vieram das branches.

Os menus isolados usados pelos roteiros de regressão ficam em `src/test/java` e são executados pelo script de testes. O JAR abre o menu integrado.
