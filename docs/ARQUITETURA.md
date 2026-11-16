# Organização do projeto — Planilha Certa

O código segue a estrutura convencional de um projeto Java com Maven. Cada pacote tem uma responsabilidade; todos pertencem à parte de Cássio nesta etapa.

```text
.
├── .mvn/wrapper/maven-wrapper.properties  # Versão do Maven usada pelo wrapper
├── mvnw                                 # Build em Linux/macOS
├── mvnw.cmd                             # Build em Windows
├── pom.xml                              # Configuração do projeto e geração do JAR
├── README.md                            # Visão geral e execução
├── docs/
│   ├── ARQUITETURA.md                    # Organização e dependências
│   ├── ESCOPO_CASSIO.md                  # Requisitos, TODOs e integrações
│   └── enunciados/                       # PDFs originais do professor
└── src/main/java/br/com/planilhacerta/
    ├── app/
    │   └── Main.java                     # Entrada e demonstração
    ├── domain/
    │   ├── enums/                        # Categoria, especialização e pagamento
    │   └── model/                        # Pessoa, clientes, consultor e contrato
    └── service/                          # Cadastros e coordenação da alocação
```

`target/` é criado pelo Maven para classes e JARs; está ignorado pelo Git. As pastas são mantidas conforme passam a ter código: os testes futuros devem ir em `src/test/java`, espelhando os pacotes das classes verificadas.

## Responsabilidade dos pacotes

| Pacote | Responsabilidade |
| --- | --- |
| `domain.enums` | Define os valores aceitos no domínio: `CategoriaCliente`, `Especializacao` e `ModalidadePagamento`. |
| `domain.model` | Representa o negócio e protege o estado de pessoas, clientes, consultores e contratos. As regras de histórico e limite de projetos ainda têm TODOs. |
| `service` | Oferece cadastros e consultas e coordena a seleção e a alocação de consultores. As regras pendentes estão descritas no roteiro. |
| `app` | Monta os exemplos e inicia a execução. A interface final será integrada com Lukas. |

A direção das dependências é `app → service → domain.model → domain.enums`; a aplicação também instancia os modelos diretamente. O domínio não depende do console, do Maven ou de armazenamento em arquivos.

## Decisões de organização

- As regras locais ficam nas entidades: por exemplo, o próprio `Consultor` deve impedir um quarto projeto ativo.
- Os serviços coordenam operações: `AlocacaoService` compara os consultores e organiza a prioridade dos contratos pendentes.
- Os cadastros usam listas em memória nesta etapa. A persistência em arquivos será integrada com Lukas.
- Os projetos são referenciados por identificador até a integração com Arthur, sem duplicar a implementação da classe `Projeto`.
- Não há bibliotecas de aplicação externas. O Maven padroniza compilação, versão do Java e empacotamento.

Os métodos pendentes continuam sinalizados por `TODO Cássio` e `UnsupportedOperationException`. A reorganização não conclui essas regras: elas são a sua parte prática, detalhada em [ESCOPO_CASSIO.md](ESCOPO_CASSIO.md).
