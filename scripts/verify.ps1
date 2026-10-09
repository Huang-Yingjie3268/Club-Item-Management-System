param([string]$JavaHome = $env:JAVA_HOME)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
if ($JavaHome) {
    $taskJava = Join-Path $JavaHome 'bin\java.exe'
    $taskJavac = Join-Path $JavaHome 'bin\javac.exe'
} else {
    $taskJava = (Get-Command java -ErrorAction Stop).Source
    $taskJavac = (Get-Command javac -ErrorAction Stop).Source
}

Push-Location -LiteralPath $taskRoot
try {
    New-Item -ItemType Directory -Path 'build' -Force | Out-Null
    $taskSources = (Get-ChildItem -LiteralPath 'src', 'tests' -Filter '*.java').FullName
    & $taskJavac -encoding UTF-8 -Xlint:all -d build $taskSources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }

    $taskLog = Join-Path $taskRoot 'build\verification.log'
    & $taskJava -cp build RegressionTests 2>&1 | Set-Content -LiteralPath $taskLog -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw "Regression tests failed. See $taskLog" }
    Get-Content -LiteralPath $taskLog | Select-String -Pattern '^PASSED|^ALL PASSED'

    $taskSampleOutput = & $taskJava -cp build Main data/sample_commands.txt
    if ($LASTEXITCODE -ne 0) { throw 'Sample run failed.' }
    $taskExpected = Get-Content -LiteralPath 'data\sample_output.txt'
    if (Compare-Object -ReferenceObject @($taskExpected) -DifferenceObject @($taskSampleOutput) -SyncWindow 0) {
        throw 'Sample output differs from data/sample_output.txt.'
    }
    Write-Output 'Sample output matches the committed reference.'
} finally {
    Pop-Location
}
