# Ambiente de Trabalho — Branch `Lukas`

**Responsável:** Lukas Souza Melo  
**Módulo:** Persistência, Relatórios PDF e Interface/Erros

---

## 🎯 Escopo de Responsabilidades

1. **Persistência de Dados e Desafio Google Drive:**
   - Salvar e recuperar dados do sistema em arquivos (`JSON`, `CSV` ou `TXT`) no diretório `data/`.
   - Estruturar/tentar o **desafio extra** de sincronizar ou armazenar os arquivos de dados no **Google Drive**.
2. **Emissão de Relatórios de Desempenho:**
   - Gerar relatórios de desempenho do projeto com detalhes de cada etapa (status, prazo e progresso) tanto no **console** quanto em **PDF (usando a biblioteca iText)**.
   - Incluir uma **seção adicional de recomendações** nos relatórios destinados a clientes **VIP**.
3. **Menu Principal / Interface e Tratamento de Erros:**
   - Criar o menu interativo principal integrando todos os módulos do sistema.
   - Garantir tratamento robusto de erros: o sistema **não deve quebrar** diante de entradas inválidas do usuário ou arquivos de dados ausentes/corrompidos.

---

## Entrega de Lukas Souza Melo

Lukas implementou a persistência local em JSON, os relatórios com iText e o menu principal no commit `cdf1389`. No commit `caf1c96`, aprimorou a recuperação do backup e a validação de entradas. A versão atual da branch reúne essa entrega e os ajustes de revisão.

| Componente | Implementação na branch |
| :--- | :--- |
| [PersistenciaService](src/main/java/persistence/PersistenciaService.java) | Lê e grava a base em `data/`, recupera o backup e exporta o CSV gerencial. |
| [RelatorioService](src/main/java/report/RelatorioService.java) | Exibe o progresso geral e o prazo, status, valor e progresso de cada etapa no console e em PDF. A seção de recomendações aparece para clientes VIP. |
| [MenuPrincipal](src/main/java/ui/MenuPrincipal.java) e [ConsoleHelper](src/main/java/util/ConsoleHelper.java) | Apresentam o menu, validam a entrada e carregam os dados fictícios da demonstração. |
| [GoogleDriveService](src/main/java/persistence/GoogleDriveService.java) | Implementa o backup opcional conforme as opções do arquivo `.env`. O envio para uma conta real ainda depende de credenciais e validação. |

A [contribuição individual resumida em PDF](docs/RELATORIO_LUKAS.pdf) acompanha a [seção editável no relatório da equipe](docs/RELATORIO_TEMPLATE.md#lukas-souza-melo). O mínimo de quatro páginas se aplica ao PDF completo do grupo.

## Demonstração prática

Na raiz da branch, compile com JDK 17 ou superior e Maven.

```text
mvn compile org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies
```

No Windows, execute

```powershell
java -cp "target/classes;target/dependency/*" ui.MenuPrincipal
```

No Linux ou macOS, execute

```bash
java -cp "target/classes:target/dependency/*" ui.MenuPrincipal
```

1. Escolha **9** para carregar a demonstração, que contém um cliente VIP fictício, uma consultora e um projeto de reestruturação do fluxo de caixa.
2. Escolha **6** e selecione o projeto. O relatório mostra cerca de 33% de progresso geral, com 100% na etapa concluída e 0% nas etapas em aberto. Confira os status, os prazos e as recomendações VIP no PDF em `relatorios_pdf/`.
3. Escolha **7** para exportar a planilha gerencial em `data/`. Saia com **0** e abra o programa novamente para mostrar que os dados foram preservados.

## Validação

Os cenários de regressão cobrem recuperação de arquivos inconsistentes, migração do JSON legado, proteção contra sobrescrita por outra sessão, configuração local do Drive, respostas do menu e relatórios para clientes VIP e regulares.

---

## Integração com a equipe

Na reunião das branches, o menu, a persistência e os relatórios devem ser ligados às entidades e à alocação do Cássio, aos projetos e etapas do Arthur e aos serviços de faturamento, fidelidade e recomendações do Cezar. O roteiro acima cobre a demonstração do módulo de Lukas na branch atual.
