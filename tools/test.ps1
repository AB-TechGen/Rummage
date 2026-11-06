$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
New-Item -ItemType Directory -Force build/core-tests | Out-Null
$sources = @(Get-ChildItem src/main/java/dev/rummage/core,src/main/java/dev/rummage/net,src/test/java -Recurse -Filter *.java | ForEach-Object FullName)
& javac --release 25 -encoding UTF-8 -d build/core-tests $sources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& java -cp build/core-tests dev.rummage.EngineTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
