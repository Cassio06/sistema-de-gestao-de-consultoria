# Pacote `service`

<<<<<<< HEAD
Serviços implementados neste módulo:
- `FaturamentoService`: faturas detalhadas por etapa concluída ou entrega final, conforme modalidade do contrato; desconto VIP; registro integral do pagamento e proteção contra cobranças/pagamentos duplicados.
- `FidelidadeService`: acumula valores pagos até cada R$ 100, credita pontos e reserva pontos para desconto de 5% por bloco de 100, limitado a 15% por contrato.
- `RecomendacaoService`: identifica especializações ausentes no histórico e fornece recomendações para relatórios VIP.

Os serviços recebem entidades pelas interfaces de integração em `br.com.planilhacerta.integration`. Os registros de faturas, pagamentos, pontos fracionários e idempotência são mantidos em memória; a persistência deve ser conectada pelo repositório da aplicação.
=======
Espaço reservado para as regras de negócio do **Sistema de Gestão de Consultoria**:
- Cadastro e gerenciamento de clientes, consultores e contratos.
- Alocação automática de consultores (máx. 3 projetos ativos, menor carga, prioridade para clientes VIP).
- Controle de etapas, verificação automática de atrasos e cálculo de progresso do projeto.
- Faturamento por etapa ou entrega final com desconto de 10% para clientes VIP.
- Programa de fidelidade (1 ponto a cada R$ 100 pagos; 5% de desconto a cada 100 pontos, máx. 15%) e recomendação de especializações ainda não contratadas.
>>>>>>> bcb3230c6ca31bbb350c3a51f6951675350f1c08
