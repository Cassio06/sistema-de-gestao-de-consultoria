# Minha parte — Planilha Certa

**Responsável:** Cássio Filipe Meireles De Carvalho Braga  
**Escopo:** Core, Entidades e Alocação.

## O que o professor pede na sua parte

O item 1 de **Requisitos Específicos** do [PDF de Gestão de Consultoria](enunciados/S10a%20-%20Gest%C3%A3o%20de%20Consultoria.docx.pdf), junto dos requisitos obrigatórios 1 e 3, define:

- Cadastrar clientes Regular e VIP e manter o histórico de contratos anteriores.
- Cadastrar consultores nas especializações Financeiro, Gestão e TI.
- Gerenciar contratos com prazos e valores. A modalidade de pagamento por etapa ou entrega final vem do item 4 e precisa constar no contrato.
- Escolher automaticamente o consultor da especialização exigida com **menos projetos ativos**.
- Respeitar **no máximo 3 projetos ativos por consultor**.
- Priorizar projetos de clientes VIP na alocação.

O [edital da P2](enunciados/Trabalho%20P2.pdf) também exige aplicação justificada dos quatro pilares de POO, organização do código e tratamento de erros. O trecho inicial sobre horas e tarefas é um **exemplo de enunciado**; as funcionalidades específicas da equipe são as do S10a.

## Estrutura que já foi preparada

O código segue a estrutura Maven em `src/main/java/br/com/planilhacerta/`. As entidades estão em `domain/model`, os enums em `domain/enums`, os serviços em `service` e a demonstração em `app`. Veja [ARQUITETURA.md](ARQUITETURA.md) para localizar cada responsabilidade.

| Classe (conforme o pacote acima) | O que já está pronto |
| --- | --- |
| `Pessoa.java` | Base abstrata com identificador, nome e e-mail; validações de campos obrigatórios. |
| `Cliente.java` | Base abstrata, contratos dos métodos de categoria/prioridade e consulta protegida do histórico. |
| `ClienteRegular.java` e `ClienteVIP.java` | Construtores e respostas específicas de categoria e prioridade. |
| `Consultor.java` | Especialização, lista protegida de identificadores de projetos ativos e contagem. |
| `Contrato.java` | Cliente, projeto por identificador, especialização do serviço, datas, valor e modalidade de pagamento. |
| `CategoriaCliente.java`, `Especializacao.java`, `ModalidadePagamento.java` | Valores permitidos pelo enunciado. |
| `ClienteService.java` e `ConsultorService.java` | Cadastrar, listar e buscar por identificador; filtro de especialização para consultores. |
| `ContratoService.java` | Lista e busca de contratos, com cadastro ainda pendente. |
| `AlocacaoService.java` | Assinaturas e orientações para você escrever o algoritmo. |
| `Main.java` | Demonstração executável das partes prontas. |

Campos como identificador, nome e e-mail são escolhas iniciais de modelagem; o PDF não especifica todos os dados do cadastro. `BigDecimal` foi usado para valores e `LocalDate` para datas. Não existe campo de pontos nesta etapa: a fidelidade será integrada com Cezar.

## Sua implementação, na ordem sugerida

Os métodos abaixo têm `TODO Cássio` e lançam uma exceção indicando que estão incompletos. Substitua essa exceção pela implementação de cada regra.

1. **`Consultor.temDisponibilidade()`**: verificar se a quantidade de projetos ativos está abaixo do limite de 3.
2. **`Consultor.adicionarProjeto()`**: validar o identificador, rejeitar projeto repetido e impedir um quarto projeto. Fazer as verificações antes de alterar a lista.
3. **`Consultor.removerProjeto()`**: liberar a vaga ao receber a conclusão de Arthur e tratar identificadores inválidos ou não encontrados.
4. **`Cliente.registrarContrato()`**: confirmar que o contrato pertence ao cliente e impedir repetição no histórico.
5. **`ContratoService.cadastrar()`**: validar o contrato, impedir identificadores repetidos e registrar tanto no serviço quanto no histórico do cliente. Uma falha não deve deixar apenas uma das listas alterada.
6. **`AlocacaoService.selecionarConsultor()`**: filtrar a área exigida, excluir quem já tem 3 projetos e escolher a menor carga. Sem candidato, retornar `Optional.empty()`.
7. **`AlocacaoService.ordenarPorPrioridade()`**: devolver os contratos pendentes em uma nova lista com VIP antes de Regular, chamando `temPrioridadeNaAlocacao()`.
8. **`AlocacaoService.alocar()`**: usar a seleção e registrar o projeto no consultor escolhido, impedindo nova alocação de um projeto já ativo em qualquer consultor. Sem candidato, devolver `Optional.empty()` e deixar o pedido pendente para nova tentativa.

**Prioridade VIP depende da fila de pedidos.** O chamador deve ordenar os contratos pendentes antes de chamar `alocar()` para cada um. Ordenar consultores não resolve a prioridade entre clientes. O PDF não exige retirar consultor de projeto Regular já iniciado.

O professor não define o desempate entre consultores com a mesma carga ou entre clientes da mesma categoria. Combine e documente uma regra com a equipe. Manter a ordem de chegada é uma possibilidade. A definição de erros para entradas inválidas também fica para sua implementação.

Depois dos métodos, acrescente à demonstração um fluxo de contrato e alocação. Na integração final, verifique com a equipe o cadastro completo e a necessidade de edição/remoção, sem permitir que essas operações invalidem vínculos existentes.

## Como explicar POO na apresentação

- **Abstração:** `Pessoa` e `Cliente` representam bases comuns; as classes concretas representam clientes, consultores e contratos do domínio.
- **Encapsulamento:** atributos privados e cópias das listas impedem alterações externas. Seus métodos de negócio vão proteger o limite de projetos e o histórico.
- **Herança:** `Cliente` e `Consultor` herdam de `Pessoa`; `ClienteRegular` e `ClienteVIP` herdam de `Cliente`.
- **Polimorfismo:** uma referência `Cliente` responde à categoria e à prioridade conforme seu tipo concreto. Use esse comportamento na ordenação, como mostrado no `Main`.

`Contrato` se relaciona com `Cliente` por associação; não há motivo para forçar herança entre eles.

## Integração com a equipe

- **Arthur — Projetos e Etapas:** nesta etapa, contratos e consultores guardam o identificador do projeto. Combine como obter esse identificador, a especialização exigida, atribuir o consultor retornado à classe `Projeto` e liberar a vaga quando o projeto deixar de estar ativo. Mantenham a mesma especialização no contrato e no projeto. A referência por identificador é uma escolha provisória para seu esqueleto compilar sem implementar a parte dele.
- **Cezar — Faturamento e Fidelidade:** forneça categoria, histórico de contratos, especializações contratadas, valor e modalidade de pagamento. Os **10% de desconto VIP**, os pontos e o desconto progressivo pertencem ao módulo dele. O valor no contrato representa o valor informado, sem cálculo de desconto neste esqueleto.
- **Lukas — Persistência, Relatórios e Interface:** ele usará os serviços no menu e cuidará de salvar/carregar os dados em arquivos. As exceções de validação e a falta de consultor disponível devem virar mensagens compreensíveis na interface. Combinem também como restaurar históricos e projetos ativos ao carregar os dados.

## Cenários para você conferir ao terminar

- [ ] Cliente Regular e VIP cadastrados; identificadores repetidos recusados.
- [ ] Contrato aparece no histórico do cliente correto, sem repetição.
- [ ] Nome vazio, contrato sem cliente, valor inválido e prazo anterior ao início são recusados.
- [ ] Consultor Financeiro nunca recebe projeto de TI ou Gestão.
- [ ] Entre consultores da mesma área com 0, 1 e 2 projetos, vence o de 0.
- [ ] Consultor com 3 projetos não recebe outro; após liberar um, volta a ter vaga.
- [ ] Quando VIP e Regular disputam a última vaga da mesma área na fila, VIP é atendido primeiro.
- [ ] Sem consultor da área ou sem vagas, o pedido continua pendente sem alocação indevida.
- [ ] O mesmo projeto não fica duplicado no consultor nem alocado a dois consultores.

O `Main` atual demonstra somente os cadastros prontos; esses cenários de negócio ficam para sua implementação e verificação. Para o relatório final, registre o que você implementou, dificuldades, referências e uma classe importante, como pede o edital.
