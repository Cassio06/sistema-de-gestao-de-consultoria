# Planilha Certa — Sistema de Gestão de Consultoria

Projeto acadêmico em Java da disciplina de **Programação Orientada a Objetos** do **iCEV — Instituto de Ensino Superior**. **Planilha Certa** é a empresa descrita no enunciado: oferece planejamento financeiro, gestão empresarial e análise de dados.

Este esqueleto cobre a parte de **Cássio: entidades, cadastros e alocação de consultores**. O roteiro do que está pronto e do que você deve desenvolver está em [docs/ESCOPO_CASSIO.md](docs/ESCOPO_CASSIO.md).

## O que já funciona

- Classes de clientes Regular/VIP, consultores e contratos, com atributos privados, construtores e consultas.
- Categorias de cliente, especializações e modalidades de pagamento definidas em enums.
- Cadastro, listagem e busca de clientes e consultores em memória, com rejeição de identificadores repetidos.
- Validações básicas de campos obrigatórios, valor positivo e prazo final do contrato.
- Demonstração de herança e polimorfismo entre os tipos de cliente.

**Histórico de contratos, cadastro integrado de contratos, controle de projetos ativos e alocação ainda precisam ser implementados nos `TODOs`.** Esses métodos lançam `UnsupportedOperationException` enquanto estiverem incompletos. A demonstração chama apenas as partes prontas.

## Organização

O projeto usa **Maven**, com o código em [src/main/java/br/com/planilhacerta/](src/main/java/br/com/planilhacerta/):

- `domain/model`: entidades e regras que protegem seus dados.
- `domain/enums`: categorias, especializações e modalidades de pagamento.
- `service`: cadastros, consultas e coordenação da alocação.
- `app`: entrada da aplicação e demonstração.

A documentação e os PDFs ficam em `docs/`. Veja a árvore completa e as decisões em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Como compilar e executar

Use **JDK 17 ou superior**. Esta é a versão mínima escolhida para o esqueleto; o professor não exige uma versão específica. O Maven Wrapper incluído usa Maven 3.9.9, sem precisar de uma instalação separada do Maven. A primeira compilação precisa de internet para baixar o Maven e os plugins de build.

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

A execução mostra dois clientes de exemplo, um consultor e a diferença de prioridade entre Regular e VIP. Os dados são apenas de demonstração e ficam em memória até o encerramento. O comando `verify` compila e gera o JAR; **ainda não há testes automatizados no projeto**. Os cenários para conferir as regras após sua implementação estão no roteiro de Cássio.

## Limites desta etapa

Projetos/etapas são de Arthur; faturamento/fidelidade, de Cezar; persistência, relatórios e interface, de Lukas. A persistência em arquivos e o tratamento de erros no menu continuam obrigatórios para a entrega final, conforme o edital. Este esqueleto ainda não representa o sistema completo.

## Enunciados utilizados

- [S10a — Gestão de Consultoria](docs/enunciados/S10a%20-%20Gest%C3%A3o%20de%20Consultoria.docx.pdf): regras específicas da Planilha Certa.
- [Trabalho P2](docs/enunciados/Trabalho%20P2.pdf): requisitos gerais, POO, execução, persistência, apresentação e relatório.
