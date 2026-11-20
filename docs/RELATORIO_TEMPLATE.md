# Roteiro do Relatório Final e Apresentação — Trabalho da P2

> **Instruções de Entrega (conforme `Trabalho P2.pdf`):**
> - O relatório final deve ser entregue em formato **PDF** no dia anterior à apresentação (apresentações nos dias **23 e 26 de novembro**, conforme cronograma).
> - Extensão mínima do PDF coletivo: **4 páginas no total**.
> - Este arquivo serve como esqueleto/template para a redação do documento final da equipe.
> - A seção 6 do edital (6.1 a 6.5) descreve a **apresentação oral**; seus tópicos foram incluídos aqui como roteiro, junto com os itens obrigatórios do relatório (seção 8).

---

## 1. Identificação da Equipe

- **Instituição:** iCEV — Instituto de Ensino Superior
- **Disciplina:** Programação Orientada a Objetos (Trabalho da P2)
- **Projeto:** Sistema de Gestão de Consultoria
- **Tema atribuído:** [S10a — Gestão de Consultoria](enunciados/S10a%20-%20Gest%C3%A3o%20de%20Consultoria.docx.pdf)
- **Integrantes:**
  - Cezar Gabriel de Souza Vicente Mendes
  - Artur Henrique Da Silva Alencar
  - Lukas Souza Melo
  - Cássio Filipe Meireles De Carvalho Braga

---

## 2. Introdução ao Projeto (Seção 6.1)

- **Visão Geral:** Descrição do problema enfrentado pela empresa de consultoria (planejamento financeiro, gestão empresarial e análise de dados/TI), público-alvo e principais funcionalidades do sistema.
- **Fluxo Geral do Sistema:** Como clientes (`Regular` e `VIP`), consultores, contratos, projetos, etapas e faturas interagem do cadastro até a emissão de relatórios e acúmulo de fidelidade.

---

## 3. Contribuição Individual de Cada Integrante (Seção 8)

*(Detalhar o que cada integrante desenvolveu no projeto)*

| Integrante | Branch | Responsabilidades e Contribuições no Projeto |
| :--- | :--- | :--- |
| **Cássio Filipe Meireles De Carvalho Braga** | `Cassio` | **Core, Entidades e Alocação:** Modelagem base de POO (herança, polimorfismo, abstração e encapsulamento em Clientes, Consultores e Contratos); cadastro de clientes (Regular/VIP com histórico) e consultores por especialização (financeiro, gestão, TI); algoritmo de alocação automática (menor carga, máx. 3 ativos, prioridade VIP). |
| **Artur Henrique Da Silva Alencar** | `Arthur` | **Projetos, Etapas e Prazos:** Gerenciamento de projetos divididos em etapas (análise inicial, implementação, revisão final) com prazos e valores; controle de status das etapas (pendente, em andamento, concluída) e regra de marcação como atrasada; cálculo automático do progresso do projeto. |
| **Cezar Gabriel de Souza Vicente Mendes** | `Cezar` | **Faturamento, Fidelidade e Recomendações:** Pagamento por etapa concluída ou entrega final e emissão de faturas detalhadas (10% de desconto automático para VIP); programa de fidelidade (1 ponto a cada R$ 100 pagos, 100 pontos = 5% de desconto até 15%); recomendação de especializações ainda não contratadas. |
| **Lukas Souza Melo** | `Lukas` | **Persistência, Relatórios PDF e Interface/Erros:** Persistência de dados em arquivos (JSON, CSV ou TXT) e tentativa do desafio Google Drive; emissão de relatórios de desempenho (console e PDF com iText, incluindo recomendações para VIP); menu/interface principal e tratamento de erros. |

### Relato individual — Cezar (rascunho para revisar antes da entrega)

> Ajuste este texto para refletir sua experiência pessoal. Os itens entre colchetes não podem ser confirmados pelo código.

Implementei os serviços de faturamento, fidelidade e recomendação, os modelos de fatura e pagamento e os testes dessas regras. Para integrar os módulos sem depender das classes concretas de contrato, projeto e etapa, usei interfaces de integração. Uma dificuldade técnica foi garantir que cobranças e pagamentos duplicados não gerassem registros repetidos; tratei cada cobrança com uma chave própria e cobri o comportamento com testes. Também usei `BigDecimal` para valores monetários e preservei no cliente o valor pago que ainda não completou R$ 100. Aprendi a separar regras de negócio das entidades concretas e a validar as regras com testes automatizados. O que mais gostei de implementar foi **[descrever uma parte que realmente gostou]**. Outra dificuldade que enfrentei foi **[descrever uma dificuldade pessoal e como a resolveu]**.

---

## 4. Estrutura do Código e Decisões de Design OO (Seção 6.2)

- **Arquitetura de Pacotes:** Organização em `model`, `repository`, `service`, `view` e `util`.
- **Justificativa dos Pilares de POO:**
  - **Abstração:** Como os elementos do domínio da consultoria foram modelados.
  - **Encapsulamento:** Como as regras de limite de projetos por consultor, prazos de etapas e saldo de pontos foram protegidas.
  - **Herança:** Justificativa para a hierarquia de classes adotada (ex.: `ClienteRegular` e `ClienteVIP`).
  - **Polimorfismo:** Como descontos, prioridades e relatórios personalizados foram tratados polimorficamente.
- **Contribuição de Lukas**. [Texto individual em PDF](RELATORIO_LUKAS.pdf).
- **Classe de Destaque (Obrigatório no Relatório):**
  - *Inserir aqui o trecho de código ou captura de tela (screenshot) de uma classe considerada importante no projeto e explicar seu funcionamento.*

### 4.1 Demonstração Prática do Sistema (Seção 6.3 — apresentação)
- *Roteiro de demonstração com exemplos reais: cadastro de cliente VIP e Regular → criação de contrato/projeto → alocação automática de consultor → avanço/atraso de etapas e progresso → faturamento com descontos e pontos → relatório de desempenho (console/PDF) com recomendações.*

---

## 5. Funcionalidades Implementadas e Pendentes (Seção 8)

### 5.1 Funcionalidades Concluídas
- [x] Cadastro de clientes (`Regular`/`VIP`), consultores (`Financeiro`, `Gestão`, `TI`) e contratos. *(Confirmar com os responsáveis pelos módulos.)*
- [x] Alocação automática de consultores (menor carga, máx. 3 projetos ativos, prioridade VIP).
- [x] Gerenciamento de projetos em etapas com prazos, valores, detecção de atraso e cálculo de progresso.
- [x] Emissão de relatórios de desempenho (com seção extra de recomendações para clientes VIP).
- [x] Faturamento por etapa concluída ou entrega final, com fatura detalhada, desconto VIP de 10% e registro do pagamento integral.
- [x] Fidelidade (1 ponto por R$ 100 pagos, saldo parcial carregado, desconto de 5% por 100 pontos até 15%) e recomendações de especializações ausentes do histórico.
- [x] Persistência em arquivos (`CSV` / `JSON` / `TXT`) e tratamento de erros.

### 5.2 Funcionalidades Não Concluídas e Justificativa

- O menu integrado liga os contratos e projetos do domínio ao faturamento, às recomendações e aos relatórios. Os testes verificam o fluxo até o pagamento e a reabertura da base.
- A persistência local em JSON, o backup e os relatórios PDF e CSV foram validados. O envio real ao Google Drive permanece opcional e requer uma conta autorizada.

---

## 6. Experiência do Grupo, Desafios e Soluções (Seções 6.4 e 8)

- **Integração entre módulos:** contrato, projeto e etapa pertencem a partes diferentes do trabalho. Os serviços usam `ClienteIntegracao`, `ContratoIntegracao`, `ProjetoIntegracao` e `EtapaIntegracao`, reduzindo dependência de implementações concretas; a equipe ainda precisa validar os contratos de interface ao juntar as branches.
- **Precisão financeira e fidelidade:** valores monetários e arredondamento são tratados com `BigDecimal`; o cliente mantém o valor pago restante para completar um ponto. Os pontos só são creditados após o pagamento registrado, e testes verificam o resgate e o saldo parcial.
- **Cobranças repetidas:** chaves por contrato/etapa ou contrato/entrega e verificações de pagamento impedem duplicidades; testes cobrem esses casos.
- **Aprendizados e experiência individual/coletiva:** cada integrante deve acrescentar o que realmente gostou e aprendeu. O relato de Cezar acima é um rascunho técnico, não substitui a revisão pessoal.

---

## 7. Bibliotecas Utilizadas e Motivo das Escolhas (Seção 8)

- **Java Standard Library (JDK 17):** `java.math.BigDecimal` e `RoundingMode` para valores e arredondamento monetário; `java.time.LocalDate` para datas; `java.util` para coleções, identificadores e entrada no console. Os testes também usam apenas a biblioteca padrão e são executados por um programa Java, sem JUnit.
- **Dependências externas:** não há Gson, Jackson, OpenCSV, iText ou outra biblioteca externa configurada ou usada nos fontes atuais. Persistência e exportação para PDF ainda não estão implementadas; portanto, não devem ser apresentadas como bibliotecas utilizadas.

---

## 8. Conclusão e Melhorias Futuras (Seção 6.5)

- Sugestões de extensões futuras para o Sistema de Gestão de Consultoria (ex.: interface gráfica web/desktop, notificações automáticas de etapas próximas do vencimento, dashboard financeiro e sincronização em nuvem).

---

## 9. Referências Consultadas (Seção 8)

- Oracle. *Java SE 17 API — BigDecimal*. https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html
- Oracle. *Java SE 17 API — LocalDate*. https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDate.html
- Oracle. *Java SE 17 API — Collections Framework*. https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/package-summary.html
- **Materiais da disciplina e outras fontes efetivamente consultadas:** [adicionar título, autor/instituição e URL; não listar fontes que não foram usadas].
