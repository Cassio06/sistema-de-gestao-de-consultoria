# Pacote `service`

Serviços implementados neste módulo:
- `FaturamentoService`: faturas detalhadas por etapa concluída ou entrega final, conforme modalidade do contrato; desconto VIP; registro integral do pagamento e proteção contra cobranças/pagamentos duplicados.
- `FidelidadeService`: acumula valores pagos até cada R$ 100, credita pontos e reserva pontos para desconto de 5% por bloco de 100, limitado a 15% por contrato.
- `RecomendacaoService`: identifica especializações ausentes no histórico e fornece recomendações para relatórios VIP.

Os serviços recebem entidades pelas interfaces de integração em `br.com.planilhacerta.integration`. Os registros de faturas, pagamentos, pontos fracionários e idempotência são mantidos em memória; a persistência deve ser conectada pelo repositório da aplicação.
