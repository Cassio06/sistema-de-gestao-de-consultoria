param([switch]$Test)

$ErrorActionPreference = 'Continue'
$codificacaoAnterior = [Console]::OutputEncoding
$mavenOptsAnterior = $env:MAVEN_OPTS

# As propriedades sun.* atendem ao Java 17.
$opcoesJava = @(
    '-Dstdout.encoding=UTF-8',
    '-Dstderr.encoding=UTF-8',
    '-Dsun.stdout.encoding=UTF-8',
    '-Dsun.stderr.encoding=UTF-8'
)
$opcoesUtf8 = $opcoesJava -join ' '

Push-Location -LiteralPath $PSScriptRoot -ErrorAction Stop
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $env:MAVEN_OPTS = "$mavenOptsAnterior $opcoesUtf8".Trim()
    & .\mvnw.cmd verify "-DargLine=$opcoesUtf8"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    if ($Test) {
        $classeTests = @(
            'br.com.planilhacerta.projetos.TesteProjetos',
            'br.com.planilhacerta.projetos.TesteAvancadoProjetos',
            'br.com.planilhacerta.RegrasFaturamentoFidelidadeRecomendacaoTest',
            'br.com.planilhacerta.FluxosPersistenciaRegressao'
        )
        foreach ($classe in $classeTests) {
            $saida = @(& java @opcoesJava '-Djava.awt.headless=true' -cp 'target/test-classes;target/planilha-certa.jar' $classe)
            $codigo = $LASTEXITCODE
            $saida
            if ($codigo -ne 0 -or ($saida -match '\[FALHOU\]')) { exit 1 }
        }
    } else {
        & java @opcoesJava -jar target/planilha-certa.jar
        exit $LASTEXITCODE
    }
} finally {
    $env:MAVEN_OPTS = $mavenOptsAnterior
    [Console]::OutputEncoding = $codificacaoAnterior
    Pop-Location
}
