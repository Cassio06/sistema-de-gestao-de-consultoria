param([switch]$Test)

$ErrorActionPreference = 'Continue'
Push-Location -LiteralPath $PSScriptRoot -ErrorAction Stop
try {
    & .\mvnw.cmd verify
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    if ($Test) {
        $classeTests = @(
            'br.com.planilhacerta.projetos.TesteProjetos',
            'br.com.planilhacerta.projetos.TesteAvancadoProjetos',
            'br.com.planilhacerta.RegrasFaturamentoFidelidadeRecomendacaoTest',
            'br.com.planilhacerta.FluxosPersistenciaRegressao'
        )
        foreach ($classe in $classeTests) {
            $saida = @(& java '-Djava.awt.headless=true' -cp 'target/test-classes;target/planilha-certa.jar' $classe)
            $codigo = $LASTEXITCODE
            $saida
            if ($codigo -ne 0 -or ($saida -match '\[FALHOU\]')) { exit 1 }
        }
    } else {
        & java -jar target/planilha-certa.jar
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
