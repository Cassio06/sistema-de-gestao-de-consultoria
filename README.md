# Planilha Certa — Sistema de Gestão de Consultoria

Projeto acadêmico em Java da disciplina de **Programação Orientada a Objetos** do **iCEV — Instituto de Ensino Superior**. **Planilha Certa** é a empresa descrita no enunciado: oferece planejamento financeiro, gestão empresarial e análise de dados.

Esta branch contém o módulo de **entidades, cadastros e alocação de consultores**, sob responsabilidade de Cássio. [Escopo do módulo](docs/ESCOPO_CASSIO.md)

## O que já funciona

- Classes de clientes Regular/VIP, consultores e contratos, com atributos privados, construtores e consultas.
- Categorias de cliente, especializações e modalidades de pagamento definidas em enums.
- Cadastro, listagem e busca de clientes e consultores em memória, com rejeição de identificadores repetidos.
- Cadastro de contratos com registro no histórico do cliente e proteção contra duplicidades.
- Seleção do consultor da especialização exigida com menor carga, com limite de três projetos ativos por consultor e liberação de vaga ao concluir um projeto.
- Fila de alocação com prioridade VIP e ordem de chegada dentro da mesma categoria.
- Validações básicas de campos obrigatórios, valor positivo e prazo final do contrato.
- Demonstração de herança e polimorfismo entre os tipos de cliente.

## Organização

O projeto usa **Maven**, com o código em [src/main/java/br/com/planilhacerta/](src/main/java/br/com/planilhacerta/):

- `domain/model`: entidades e regras que protegem seus dados.
- `domain/enums`: categorias, especializações e modalidades de pagamento.
- `service`: cadastros, consultas e coordenação da alocação.
- `app`: entrada da aplicação e demonstração.

A documentação e os PDFs ficam em `docs/`. Veja a árvore completa e as decisões em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Como compilar e executar

Use **JDK 17 ou superior**. O projeto inclui o Maven Wrapper com Maven 3.9.9. A primeira compilação precisa de internet para baixar o Maven, os plugins e as dependências de teste.

Na raiz do projeto, em Linux/macOS:

```bash
./mvnw clean verify
java -jar target/planilha-certa.jar
```

No Windows (PowerShell):

```powershell
.\mvnw.cmd clean verify
java -jar target/planilha-certa.jar
```

No IntelliJ IDEA, abra o `pom.xml` como projeto Maven e selecione um JDK 17 ou superior. A classe de entrada é `br.com.planilhacerta.app.Main`.

O comando `verify` compila o módulo, executa os testes JUnit e gera o JAR. A demonstração cadastra dois contratos e mostra a última vaga em Financeiro sendo ocupada primeiro pelo VIP. Ao liberar essa vaga, o cliente Regular é atendido. Os dados do exemplo ficam em memória.

## Limites desta etapa

Os serviços deste módulo usam o identificador de projeto. A integração final deve conectar os projetos e etapas de Arthur, o faturamento e a fidelidade de Cezar e o menu e a persistência de Lukas.

## Enunciados utilizados

- [S10a — Gestão de Consultoria](docs/enunciados/S10a%20-%20Gest%C3%A3o%20de%20Consultoria.docx.pdf): regras específicas da Planilha Certa.
- [Trabalho P2](docs/enunciados/Trabalho%20P2.pdf): requisitos gerais, POO, execução, persistência, apresentação e relatório.
