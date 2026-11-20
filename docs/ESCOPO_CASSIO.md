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

## Estrutura implementada

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
| `ContratoService.java` | Cadastro, lista e busca de contratos, com atualização do histórico do cliente. |
| `AlocacaoService.java` | Seleção por especialização e carga, pedido VIP antes de Regular e proteção contra alocação duplicada. |
| `Main.java` | Demonstração de cadastros, histórico, disputa pela última vaga e liberação para o próximo contrato. |

Os cadastros usam identificador, nome e e-mail. O contrato usa `BigDecimal` para o valor informado e `LocalDate` para as datas.

## Regras do módulo

- `ContratoService.cadastrar()` rejeita IDs já cadastrados e registra o contrato no histórico do titular. A entidade `Cliente` valida o vínculo e a unicidade antes de gravar o histórico.
- `Consultor` guarda até três identificadores de projetos ativos. A remoção de um identificador ativo libera uma vaga. Identificadores vazios, repetidos ou ausentes na remoção são recusados.
- `AlocacaoService.ordenarPorPrioridade()` devolve a fila com clientes VIP antes de clientes regulares. A ordem de chegada é mantida dentro de cada categoria, e o chamador usa a fila retornada para processar as alocações.
- Dentro da especialização exigida, a seleção compara a quantidade de projetos ativos. Quando dois consultores têm a mesma carga, vence o menor identificador em ordem lexicográfica.
- Use uma instância de alocação para o cadastro de consultores. Ao restaurar a base, forneça a lista completa na primeira chamada para localizar vínculos existentes. Uma tentativa posterior com uma lista filtrada reaproveita o vínculo ativo quando o projeto já foi alocado. Sem consultor elegível, `Optional.empty()` indica que o contrato deve seguir na fila.

## Como explicar POO na apresentação

- **Abstração:** `Pessoa` e `Cliente` representam bases comuns; as classes concretas representam clientes, consultores e contratos do domínio.
- **Encapsulamento:** atributos privados e cópias das listas impedem alterações externas. Os métodos de negócio protegem o limite de projetos e o histórico.
- **Herança:** `Cliente` e `Consultor` herdam de `Pessoa`; `ClienteRegular` e `ClienteVIP` herdam de `Cliente`.
- **Polimorfismo:** uma referência `Cliente` responde à categoria e à prioridade conforme seu tipo concreto. A ordenação usa `temPrioridadeNaAlocacao()` da classe concreta.

`Contrato` se relaciona com `Cliente` por associação; não há motivo para forçar herança entre eles.

### Trecho para o relatório da equipe

```java
public synchronized boolean temDisponibilidade() {
    return projetosAtivos.size() < LIMITE_PROJETOS_ATIVOS;
}
```

O limite fica na entidade `Consultor`. Tanto o serviço de alocação quanto o próprio cadastro de um projeto ativo verificam essa disponibilidade. [Consultor.java](../src/main/java/br/com/planilhacerta/domain/model/Consultor.java)

## Integração com a equipe

- **Arthur — Projetos e Etapas:** contratos e consultores guardam o identificador do projeto. A integração deve vincular o consultor retornado à classe `Projeto` e chamar `removerProjeto()` no consultor quando o projeto for concluído.
- **Cezar — Faturamento e Fidelidade:** o módulo fornece categoria, histórico de contratos, especializações contratadas, valor e modalidade de pagamento. O contrato guarda o valor informado para o faturamento.
- **Lukas — Persistência, Relatórios e Interface:** ele usará os serviços no menu e cuidará de salvar/carregar os dados em arquivos. As exceções de validação e a falta de consultor disponível devem virar mensagens compreensíveis na interface. Combinem também como restaurar históricos e projetos ativos ao carregar os dados.

## Validação

O Maven executa testes do histórico, das validações de contrato, das vagas, da especialização e da menor carga, da prioridade VIP e de pedidos de alocação repetidos e concorrentes. O `Main` demonstra o cadastro e uma fila real de dois contratos com uma vaga. [CoreConsultoriaTest.java](../src/test/java/br/com/planilhacerta/service/CoreConsultoriaTest.java)
