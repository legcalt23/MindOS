<#
.SYNOPSIS
  Audita el entorno local para trabajar MindOS con agentes (Vibe Kanban + Claude Code + Codex).
.DESCRIPTION
  Solo LECTURA. No instala ni modifica nada. Imprime versiones y un veredicto.
  Uso:  pwsh -File scripts/check-ai-env.ps1   (o)   powershell -File scripts/check-ai-env.ps1
#>
$ErrorActionPreference = 'SilentlyContinue'

function Probe([string]$name, [string]$cmd, [string]$versionArgs) {
    $src = (Get-Command $cmd).Source
    if (-not $src) {
        Write-Host ("  [X] {0,-14} no encontrado en PATH" -f $name) -ForegroundColor Red
        return $false
    }
    $raw   = (& $cmd $versionArgs.Split(' ') 2>&1 | Out-String)
    $lines = @($raw -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 })
    $ver   = if ($lines.Count -gt 0) { $lines[0].Trim() } else { "presente ($src)" }
    Write-Host ("  [OK] {0,-13} {1}" -f $name, $ver) -ForegroundColor Green
    return $true
}

Write-Host "`n=== MindOS · Auditoría de entorno AI ===`n" -ForegroundColor Cyan

$ok = $true
$ok = (Probe 'Node'    'node'    '--version')  -and $ok
$ok = (Probe 'npm'     'npm'     '--version')  -and $ok
$ok = (Probe 'Git'     'git'     '--version')  -and $ok
$ok = (Probe 'Docker'  'docker'  '--version')  -and $ok
$ok = (Probe 'Java'    'java'    '-version')   -and $ok
$ok = (Probe 'Claude'  'claude'  '--version')  -and $ok
$ok = (Probe 'Codex'   'codex'   '--version')  -and $ok
# Antigravity CLI (agy): informativo. Vibe Kanban aún NO lo soporta como agente (jun 2026).
Probe 'Antigravity' 'agy' '--version' | Out-Null

Write-Host "`n=== Gradle wrapper ===" -ForegroundColor Cyan
if (Test-Path "$PSScriptRoot\..\gradlew.bat") {
    Write-Host "  [OK] gradlew.bat presente" -ForegroundColor Green
} else {
    Write-Host "  [X] gradlew.bat ausente" -ForegroundColor Red; $ok = $false
}

Write-Host "`n=== Android SDK ===" -ForegroundColor Cyan
$lp = "$PSScriptRoot\..\local.properties"
if (Test-Path $lp) {
    $sdk = (Get-Content $lp | Select-String '^sdk.dir=').Line
    Write-Host "  [OK] local.properties -> $sdk" -ForegroundColor Green
} else {
    Write-Host "  [!] local.properties ausente (necesario para builds Android; no para Desktop)" -ForegroundColor Yellow
}

Write-Host "`n=== Auth de agentes (revisar manualmente) ===" -ForegroundColor Cyan
Write-Host "  Claude Code: ejecuta 'claude'  -> /login  (suscripción, sin API key)"
Write-Host "  Codex:       ejecuta 'codex login'         (suscripción ChatGPT, sin API key)"

Write-Host "`n=== Vibe Kanban (registry) ===" -ForegroundColor Cyan
$vk = npm view vibe-kanban version 2>$null
if ($vk) { Write-Host "  [OK] vibe-kanban@$vk disponible vía npx" -ForegroundColor Green }
else     { Write-Host "  [!] no se pudo consultar el registry (¿sin red?)" -ForegroundColor Yellow }

if ($ok) {
    Write-Host "`nVeredicto: entorno LISTO para Vibe Kanban.`n" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nVeredicto: faltan herramientas. Revisa las líneas [X] arriba.`n" -ForegroundColor Red
    exit 1
}
