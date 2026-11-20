# Sistema de Gestão de Consultoria

## iCEV — Instituto de Ensino Superior

Programação Orientada a Objetos · Trabalho da P2 · Tema S10a
Relatório da equipe · Novembro de 2026

## Integrantes

- Cássio Filipe Meireles De Carvalho Braga
- Artur Henrique Da Silva Alencar
- Cezar Gabriel de Souza Vicente Mendes
- Lukas Souza Melo

## 1. Introdução

A Planilha Certa oferece consultoria em planejamento financeiro, gestão empresarial e análise de dados. O sistema acompanha o atendimento desde o cadastro do cliente até a conclusão do projeto, o pagamento e a emissão de relatórios. O público do menu são os responsáveis por cadastrar contratos, acompanhar as etapas e registrar cobranças.

## Fluxo de atendimento

1. O operador cadastra clientes e consultores. O cliente pode ser Regular ou VIP, e cada consultor tem uma especialização.
2. O operador cria um contrato e seu projeto, informa as etapas e escolhe cobrança por etapa ou na entrega final. O sistema procura um consultor com vaga na área exigida.
3. O operador atualiza as etapas. A aplicação indica atrasos e calcula o percentual concluído. A conclusão da última etapa libera a vaga do consultor e reprocessa a fila.
4. O operador emite a fatura, registra o pagamento e consulta a fidelidade. Os relatórios de desempenho podem ser visualizados no console e exportados em PDF.

## Repositório

[Sistema de Gestão de Consultoria · código-fonte e instruções de execução](https://github.com/Cassio06/sistema-de-gestao-de-consultoria/tree/main)

# 2. Funcionalidades implementadas

## Cadastros, contratos e alocação

O cadastro identifica clientes e consultores e mantém os contratos no histórico do cliente. Cada contrato reúne a especialização, o projeto, os prazos, o valor e a modalidade de pagamento. A alocação filtra os consultores pela área e procura quem tem menos projetos ativos, respeitando o limite de três. Quando há fila, os clientes VIP são processados antes dos regulares, com ordem de chegada dentro de cada categoria.

## Projetos, etapas e prazos

Cada etapa tem identificador, prazo e valor. Pode ficar pendente, em andamento ou concluída. Uma etapa em aberto fica atrasada a partir do dia seguinte ao vencimento. O progresso geral divide a quantidade de etapas concluídas pelo total. Assim, um projeto com uma etapa concluída de três apresenta cerca de 33,3% de progresso.

## Faturamento, fidelidade e recomendações

A cobrança por etapa exige a conclusão da etapa correspondente. A cobrança na entrega final exige que todas estejam concluídas. A fatura detalha o valor bruto, os descontos e o valor líquido. Clientes VIP recebem 10% de desconto. O pagamento registrado acumula um ponto por R$ 100 pagos, com saldo monetário residual para o próximo pagamento. Cada 100 pontos dão 5% de desconto de fidelidade no próximo contrato elegível, até 15%. As recomendações indicam especializações ausentes do histórico do cliente.

## Relatórios, armazenamento e tratamento de erros

O relatório de desempenho mostra prazo, status, valor e progresso das etapas e inclui recomendações quando o cliente é VIP. O menu também exporta um CSV gerencial. Os dados são gravados em JSON, e o backup mantém a versão anterior à última alteração gravada. O menu repete a solicitação quando o formato da entrada é inválido. Uma base ausente permite iniciar sem registros, ou recuperar o backup se houver uma cópia válida.

# 3. Contribuições individuais

## Cássio Filipe Meireles De Carvalho Braga

Modelei clientes, consultores e contratos e desenvolvi a alocação automática. O que mais gostei foi transformar a prioridade VIP em uma regra que pudesse ser testada. A maior dificuldade foi combinar a fila de espera com o limite de três projetos e evitar que um pedido repetido ocupasse duas vagas. Aprendi a usar polimorfismo para representar a prioridade e a proteger as regras de capacidade na própria entidade Consultor.

## Artur Henrique Da Silva Alencar

Implementei projetos, etapas, prazos e o cálculo de progresso. O que mais gostei foi acompanhar a mudança de status à medida que o projeto avançava. A maior dificuldade foi distinguir uma etapa em aberto que passou do prazo de uma etapa concluída depois do vencimento. Aprendi a separar a data de conclusão do status exibido e a testar casos como datas futuras e projetos que ainda aguardam consultor.

## Cezar Gabriel de Souza Vicente Mendes

Implementei faturamento, registro de pagamentos, fidelidade e recomendações. O que mais gostei foi ligar o pagamento à pontuação do cliente. A maior dificuldade foi aplicar descontos e guardar os valores que ainda não completavam R$ 100, sem gerar cobranças duplicadas. Aprendi a usar BigDecimal para controlar o arredondamento e a separar a emissão da fatura do pagamento que gera pontos.

## Lukas Souza Melo

Implementei o menu, a persistência em JSON, a exportação CSV e os relatórios em PDF. O que mais gostei foi ver os dados de demonstração virarem um relatório com recomendações para clientes VIP. A maior dificuldade foi recuperar a base quando o arquivo principal estava inválido e preservar uma cópia utilizável. Aprendi a testar a gravação, a reabertura do sistema e a recuperação do backup como etapas distintas.

# 4. Estrutura do código e decisões de POO

## Organização dos módulos

O menu delega as operações a SistemaConsultoria. O pacote de domínio concentra pessoas, categorias e contratos. Os serviços implementam regras de alocação e faturamento. O pacote de projetos controla as etapas. O repositório e os relatórios adaptam os dados do domínio para os formatos de armazenamento e apresentação.

- Abstração. Pessoa e Cliente concentram atributos e operações comuns. As interfaces de integração descrevem os dados necessários ao faturamento.
- Herança. Cliente e Consultor derivam de Pessoa. ClienteRegular e ClienteVIP especializam Cliente.
- Encapsulamento. Consultor mantém a lista de projetos ativos protegida e aplica seu limite ao incluir um projeto. Etapa controla as transições de status.
- Polimorfismo. O serviço consulta a prioridade por meio de uma referência Cliente. O comportamento concreto decorre de ClienteRegular ou ClienteVIP.

## Trecho de código — classe Consultor

```java
public synchronized boolean temDisponibilidade() {
    return projetosAtivos.size() < LIMITE_PROJETOS_ATIVOS;
}
```

O método concentra a regra de capacidade na própria entidade. O serviço pode consultar a disponibilidade durante a seleção, e a entidade volta a verificar o limite ao registrar um projeto ativo. Assim, a regra vale também para outros pontos do código que usem Consultor.

[Classe Consultor no repositório](https://github.com/Cassio06/sistema-de-gestao-de-consultoria/blob/5f62bc7/src/main/java/br/com/planilhacerta/domain/model/Consultor.java)

## Desafios da integração

Os módulos passaram a compartilhar identificadores de cliente, contrato e projeto. Na reabertura da base, a aplicação valida os vínculos entre registros e restaura a fila. Para prevenir registros duplicados, a lógica de cobrança identifica cada fatura pelo contrato e pela etapa ou entrega correspondente.

# 5. Validação, limitações e referências

## Validação

A compilação Maven passou com 13 testes JUnit e quatro executáveis de regressão. No menu, o cenário fictício mostrou uma etapa concluída, uma atrasada e uma pendente. A cobrança de R$ 2.000,00 para o cliente VIP totalizou R$ 1.800,00 e, após o pagamento, o sistema registrou 18 pontos. O teste também verificou os arquivos PDF e CSV, a reabertura e a recuperação de dados após corromper a base principal.

## Funcionalidades não concluídas e melhorias futuras

O desafio de envio ao Google Drive tem configuração e código, mas o envio com uma conta autorizada ainda precisa ser validado. A versão integrada usa um esquema JSON com vínculos de faturamento, portanto a importação de bases antigas exige uma migração específica ainda não implementada. Entre as melhorias possíveis estão notificações de vencimento e um painel financeiro.

## Bibliotecas utilizadas e motivo das escolhas

- Gson 2.10.1 converte o estado da aplicação em JSON e lê a base ao reabrir o programa.
- iText 7.2.5 organiza o texto e as tabelas dos relatórios de desempenho no PDF.
- JUnit Jupiter 5.14.4 automatiza a verificação de regras e fluxos integrados.
- SLF4J Simple 1.7.36 fornece a implementação de logging das bibliotecas.
- Google API Client, Drive API v3 e Google Auth tratam o acesso e a autenticação do backup opcional.
- A biblioteca padrão do Java fornece BigDecimal para valores monetários e LocalDate para prazos.

## Referências consultadas

- iCEV. Trabalho da P2 e enunciado S10a — Gestão de Consultoria. [Enunciados da disciplina](https://github.com/Cassio06/sistema-de-gestao-de-consultoria/tree/main/docs/enunciados).
- Oracle. [BigDecimal — Java SE 17 API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html).
- Google. [Gson User Guide](https://google.github.io/gson/UserGuide.html).
- Equipe Planilha Certa. [Código e configuração Maven da versão validada](https://github.com/Cassio06/sistema-de-gestao-de-consultoria/tree/5f62bc7).
