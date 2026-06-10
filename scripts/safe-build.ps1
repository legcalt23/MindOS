<#
.SYNOPSIS
  Build seguro de MindOS por target, sin tocar lógica de negocio ni firmar release.
.DESCRIPTION
  Wrapper fino sobre gradlew. Nunca ejecuta 'clean' ni tareas destructivas salvo que lo pidas con -Clean.
  Targets:
    desktop  -> :shared:desktopTest        (rápido, no necesita Android SDK)
    android  -> :androidApp:assembleDebug   (necesita local.properties con sdk.dir)
    detekt   -> análisis estático
    functions-> functions: scan:secrets + typecheck + jest
    all      -> desktop + detekt + functions
  Uso:
    powershell -File scripts/safe-build.ps1 desktop
    powershell -File scripts/safe-build.ps1 android
    powershell -File scripts/safe-build.ps1 all
    powershell -File scripts/safe-build.ps1 android -Clean   # limpia antes (cache corrupto)
#>
param(
    [Parameter(Position=0)][ValidateSet('desktop','android','detekt','functions','all')]
    [string]$Target = 'desktop',
    [switch]$Clean
)
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path "$PSScriptRoot\.."
Set-Location $repo
$gw = "$repo\gradlew.bat"

function Run-Gradle([string[]]$tasks) {
    if ($Clean) { & $gw clean --console=plain; if ($LASTEXITCODE -ne 0) { throw "clean falló" } }
    & $gw @tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "gradle $($tasks -join ' ') falló (exit $LASTEXITCODE)" }
}

function Run-Functions {
    Push-Location "$repo\functions"
    try {
        if (-not (Test-Path "node_modules")) { npm ci }
        npm run verify
        if ($LASTEXITCODE -ne 0) { throw "functions verify falló" }
    } finally { Pop-Location }
}

switch ($Target) {
    'desktop'   { Run-Gradle @(':shared:desktopTest') }
    'android'   { Run-Gradle @(':androidApp:assembleDebug') }
    'detekt'    { Run-Gradle @('detekt') }
    'functions' { Run-Functions }
    'all'       { Run-Gradle @(':shared:desktopTest'); Run-Gradle @('detekt'); Run-Functions }
}

Write-Host "`nsafe-build [$Target] OK.`n" -ForegroundColor Green
