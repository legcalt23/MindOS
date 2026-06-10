<#
.SYNOPSIS
  Lanza Vibe Kanban en local (vía npx) para orquestar agentes sobre MindOS.
.DESCRIPTION
  - 100% local. No usa servidor externo.
  - Comprueba que claude y codex estén en PATH antes de arrancar.
  - Avisa si estás en main/master (no es bloqueante: Vibe Kanban crea worktrees por tarea).
  - Vibe Kanban abrirá una UI web local (imprime la URL en consola).
  Uso:
    powershell -File scripts/start-vibe-kanban.ps1
  Parar: Ctrl+C en esta ventana.
#>
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path "$PSScriptRoot\.."
Set-Location $repo

Write-Host "`n=== Pre-chequeos ===" -ForegroundColor Cyan
$missing = @()
foreach ($c in 'claude','codex','git','node') {
    if (-not (Get-Command $c -ErrorAction SilentlyContinue)) { $missing += $c }
}
if ($missing.Count) {
    Write-Host "  [X] Falta en PATH: $($missing -join ', '). Abre una terminal NUEVA o instala las CLIs." -ForegroundColor Red
    exit 1
}
Write-Host "  [OK] claude, codex, git, node disponibles" -ForegroundColor Green

$branch = git rev-parse --abbrev-ref HEAD
if ($branch -in @('main','master')) {
    Write-Host "  [!] Estás en '$branch'. Vibe Kanban crea worktrees por tarea, pero crea una rama base de trabajo si vas a tocar el repo manualmente." -ForegroundColor Yellow
} else {
    Write-Host "  [OK] rama actual: $branch" -ForegroundColor Green
}

Write-Host "`n=== Arrancando Vibe Kanban (npx) ===" -ForegroundColor Cyan
Write-Host "  Primera ejecución: descarga el binario. La UI se abrirá en el navegador." -ForegroundColor DarkGray
Write-Host "  Repo a añadir dentro de la UI: $repo`n" -ForegroundColor DarkGray

npx vibe-kanban
