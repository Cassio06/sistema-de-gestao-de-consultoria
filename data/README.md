# Diretório de Persistência (`data/`)

Este diretório armazena a base JSON e a exportação CSV do **Sistema de Gestão de Consultoria**.

## Arquivos gerados

- `dados_planilha_certa.json` guarda os clientes, consultores, projetos e etapas disponíveis no menu.
- `dados_planilha_certa.bak.json` guarda a versão anterior válida para recuperação.
- `dados-corrompidos-*.json` preserva cópias do arquivo principal corrompido quando ele precisa ser substituído.
- `relatorio_planilha_certa.csv` reúne os dados gerenciais dos projetos.

Na ausência de arquivos válidos, o sistema inicia com uma base vazia. Os relatórios em PDF são gravados em `relatorios_pdf/`, na raiz.
