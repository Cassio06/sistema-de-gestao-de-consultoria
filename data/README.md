# Dados da consultoria

O menu integrado salva em `data/consultoria.json`. O backup é `consultoria.bak.json`. A exportação gerencial é `relatorio_planilha_certa.csv`, e os relatórios individuais ficam em `relatorios_pdf/`.

A base inclui clientes, consultores, contratos, etapas, alocações, faturas, pagamentos e o saldo de fidelidade. Se houver arquivos da preparação isolada de Lukas, mantenha-os para consulta. O esquema antigo não contém os vínculos de faturamento e requer migração específica.

O upload ao Google Drive é opcional e vem desativado. Configure a autenticação e a pasta em `.env` para habilitá-lo.
