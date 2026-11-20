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
│   ├── ESCOPO_CASSIO.md                  # Requisitos, regras e integrações
│   └── enunciados/                       # PDFs originais do professor
├── src/main/java/br/com/planilhacerta/
│   ├── app/
│   │   └── Main.java                     # Entrada e demonstração
│   ├── domain/
│   │   ├── enums/                        # Categoria, especialização e pagamento
│   │   └── model/                        # Pessoa, clientes, consultor e contrato
│   └── service/                          # Cadastros e coordenação da alocação
└── src/test/java/br/com/planilhacerta/
    └── service/CoreConsultoriaTest.java  # Testes do módulo
```

`target/` contém os arquivos gerados pelo Maven e está no `.gitignore`. Os testes ficam em `src/test/java`.

## Responsabilidade dos pacotes

| Pacote | Responsabilidade |
| --- | --- |
| `domain.enums` | Define os valores aceitos no domínio: `CategoriaCliente`, `Especializacao` e `ModalidadePagamento`. |
| `domain.model` | Representa pessoas, clientes, consultores e contratos. Protege o histórico do cliente e as três vagas de cada consultor. |
| `service` | Cadastra e consulta entidades, registra contratos no histórico e coordena a seleção e a alocação de consultores. |
| `app` | Monta os exemplos e inicia a execução. A interface final será integrada com Lukas. |

A direção das dependências é `app → service → domain.model → domain.enums`; a aplicação também instancia os modelos diretamente. O domínio não depende do console, do Maven ou de armazenamento em arquivos.

## Decisões de organização

- O próprio `Consultor` impede um quarto projeto ativo. `Cliente` protege a lista de contratos do titular.
- `AlocacaoService` organiza a fila VIP, seleciona a menor carga da especialização e evita atribuir um projeto ativo a outro consultor.
- Os cadastros usam listas em memória nesta etapa. A persistência em arquivos será integrada com Lukas.
- Os projetos são referenciados por identificador até a integração com Arthur, sem duplicar a implementação da classe `Projeto`.
- O módulo usa apenas a biblioteca padrão em tempo de execução. JUnit é usado nos testes, e o Maven compila e empacota o projeto.
