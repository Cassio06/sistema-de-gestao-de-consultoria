param(
    [switch]$Test
)

$ErrorActionPreference = "Stop"
$repositoryRoot = $PSScriptRoot
$sourceRoots = @((Join-Path $repositoryRoot "src\main\java"))

if ($Test) {
    $sourceRoots += Join-Path $repositoryRoot "src\test\java"
}

$sources = foreach ($sourceRoot in $sourceRoots) {
    Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter "*.java" |
        ForEach-Object { $_.FullName }
}

if (-not $sources) {
    throw "Nenhum arquivo Java foi encontrado nas pastas-fonte."
}

$outputPath = Join-Path $repositoryRoot "out"
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

& javac --release 17 -d $outputPath $sources
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if ($Test) {
    & java -cp $outputPath br.com.planilhacerta.RegrasFaturamentoFidelidadeRecomendacaoTest
} else {
    & java -cp $outputPath br.com.planilhacerta.view.Main
}

exit $LASTEXITCODE
