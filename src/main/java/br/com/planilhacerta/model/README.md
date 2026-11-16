# Pacote `model`

Classes implementadas neste módulo:
- `ModalidadePagamento` (`POR_ETAPA`, `ENTREGA_FINAL`) e `Especializacao` (`FINANCEIRO`, `GESTAO`, `TI`).
- `Fatura` e `FaturaItem`, com detalhamento dos itens, valores, descontos e datas de emissão/pagamento.
- `Pagamento`, que representa o registro do pagamento integral de uma fatura.

Os modelos completos de cliente, contrato, projeto e etapa pertencem aos módulos correspondentes da equipe e conectam-se pelos contratos em `br.com.planilhacerta.integration`.
