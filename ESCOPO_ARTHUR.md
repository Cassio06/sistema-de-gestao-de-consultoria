# Modulo de Projetos, Etapas e Prazos — Branch Arthur

## Responsabilidade

Implementacao do gerenciamento de projetos e etapas do sistema Planilha Certa.

## Funcionalidades implementadas

- Cadastro de projetos com identificadores de texto, com alocação de consultor na criação ou depois.
- Cadastro de etapas com nome, prazo e valor.
- Controle de status: PENDENTE, EM_ANDAMENTO, CONCLUIDA e ATRASADA.
- Identificacao automatica de etapas atrasadas.
- Calculo percentual do progresso dos projetos.
- Calculo do valor total das etapas.
- Validacao de prazos e valores.
- Rejeição de uma data de conclusão futura.
- Bloqueio de nomes duplicados dentro de um projeto.
- Bloqueio do compartilhamento da mesma instancia de etapa entre projetos.
- Suporte a projetos com diferentes quantidades de etapas.
- Monitoramento de prazos com alertas de vencimento.
- Painel de desempenho com progresso medio e indicadores dos projetos.

## Arquivos implementados

### Codigo principal

- src/main/java/br/com/planilhacerta/projetos/StatusEtapa.java
- src/main/java/br/com/planilhacerta/projetos/Etapa.java
- src/main/java/br/com/planilhacerta/projetos/Projeto.java
- src/main/java/br/com/planilhacerta/projetos/MonitoramentoProjetos.java
- src/main/java/Main.java

### Testes

- src/test/java/br/com/planilhacerta/projetos/TesteProjetos.java
- src/test/java/br/com/planilhacerta/projetos/TesteAvancadoProjetos.java

## Testes realizados

Compilacao realizada com Java 17.

- 3 verificacoes principais aprovadas.
- 19 verificações avançadas, incluindo data futura, compatibilidade de IDs de texto e alocação posterior.
- Demonstracao pelo Main executada corretamente.

## Integracao com os colegas

A integracao definitiva com os modulos de clientes, consultores,
contratos, faturamento, persistencia e relatorios ainda esta pendente.

Projeto aceita identificadores como `PRJ-001`, `CLI-001` e `CON-001`.
O construtor com IDs de texto aceita `null` no consultor enquanto a alocação estiver pendente.
Depois que `AlocacaoService.alocar()` devolver o consultor, use
`projeto.alocarConsultor(consultor.getId())` para guardar o vínculo.
Quando todas as etapas forem concluídas, o fluxo integrado deve chamar
`consultor.removerProjeto(projeto.getId())` e processar os contratos que aguardam vaga.

O monitoramento e o painel de desempenho funcionam como
demonstracao independente em console.
