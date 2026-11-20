# Planilha Certa — Sistema de Gestão de Consultoria

Projeto acadêmico de Programação Orientada a Objetos do iCEV, tema S10a.

## Executar

Use JDK 17 ou superior. A primeira compilação baixa o Maven e as dependências pela rede.

No Windows:

```powershell
.\mvnw.cmd clean verify
java -jar target/planilha-certa.jar
```

No Linux ou macOS:

```sh
./mvnw clean verify
java -jar target/planilha-certa.jar
```

O JAR abre o menu integrado. A opção **9** carrega dados fictícios. Use **6** para emitir o relatório, **7** para exportar CSV, **10** para emitir uma fatura e **11** para registrar o pagamento. A demonstração inclui uma etapa concluída, uma atrasada e uma pendente.

Para rodar a demonstração em uma base separada, informe os diretórios antes de `-jar`, por exemplo:

```text
java "-Dconsultoria.dados=data-demo" "-Dconsultoria.relatorios=relatorios-demo" -jar target/planilha-certa.jar
```

No Windows, `.\run.ps1 -Test` também roda os executáveis de regressão das branches.

## Fluxo integrado

- **Cássio** implementa clientes, consultores, contratos, histórico e fila de alocação VIP por especialização e carga, com três vagas por consultor.
- **Arthur** implementa projetos, etapas, status, atrasos e progresso. A conclusão da última etapa libera a vaga e reprocessa a fila.
- **Cezar** implementa faturamento por etapa ou entrega, desconto VIP, fidelidade após pagamento e recomendações.
- **Lukas** implementa o menu, a persistência JSON com backup, a exportação CSV e os relatórios no console e em PDF.

O sistema valida os vínculos ao salvar e carregar. O JSON integrado está em `data/consultoria.json`. Os PDFs são gerados em `relatorios_pdf/`. [Dados e backups](data/README.md)

## Documentação da P2

- [Enunciado S10a](docs/enunciados/S10a%20-%20Gest%C3%A3o%20de%20Consultoria.docx.pdf)
- [Trabalho P2](docs/enunciados/Trabalho%20P2.pdf)
- [Arquitetura](docs/ARQUITETURA.md)
- [Relatório da P2 em PDF](docs/RELATORIO_FINAL_P2.pdf)
- [Texto editável do relatório](docs/RELATORIO_P2.md)
