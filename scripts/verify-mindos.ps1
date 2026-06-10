<#
.SYNOPSIS
  Verificación de seguridad/integridad de MindOS antes de mergear el trabajo de un agente.
.DESCRIPTION
  NO modifica el repo. Comprueba:
    1. Que NO estás en main/master (los agentes no deben trabajar sobre la rama principal).
    2. Estado de git (limpio / sucio).
    3. Que no haya secretos trackeados.
    4. (opcional, con -Full) corre detekt + desktopTest + functions verify.
  Uso:
    powershell -File scripts/verify-mindos.ps1            # comprobaciones rápidas
    powershell -File scripts/verify-mindos.ps1 -Full      # + detekt + tests
#>
param([switch]$Full)
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path "$PSScriptRoot\.."
Set-Location $repo
$fail = $false

Write-Host "`n=== 1. Rama actual ===" -ForegroundColor Cyan
$branch = git rev-parse --abbrev-ref HEAD
if ($branch -in @('main','master')) {
    Write-Host "  [X] Estás en '$branch'. Los agentes NUNCA deben commitear aquí. Crea una rama/worktree." -ForegroundColor Red
    $fail = $true
} else {
    Write-Host "  [OK] rama de trabajo: $branch" -ForegroundColor Green
}

Write-Host "`n=== 2. Estado de git ===" -ForegroundColor Cyan
$dirty = git status --porcelain
if ($dirty) {
    Write-Host "  [!] árbol de trabajo SUCIO ($((($dirty -split "`n").Count)) archivos). Revisa antes de mergear." -ForegroundColor Yellow
} else {
    Write-Host "  [OK] árbol limpio" -ForegroundColor Green
}

Write-Host "`n=== 3. Secretos trackeados ===" -ForegroundColor Cyan
$secrets = git ls-files | Select-String -Pattern '\.env$|\.jks$|\.keystore$|key\.properties|serviceAccount|firebase-adminsdk|\.pem$|\.p12$|\.pfx$|id_rsa'
if ($secrets) {
    Write-Host "  [X] Posibles secretos trackeados:" -ForegroundColor Red
    $secrets | ForEach-Object { Write-Host "      $_" -ForegroundColor Red }
    $fail = $true
} else {
    Write-Host "  [OK] sin secretos trackeados (google-services.json / *.plist son config cliente, no secretos)" -ForegroundColor Green
}

if ($Full) {
    Write-Host "`n=== 4. detekt ===" -ForegroundColor Cyan
    & "$repo\gradlew.bat" detekt --console=plain; if ($LASTEXITCODE -ne 0) { $fail = $true }

    Write-Host "`n=== 5. Tests Desktop (KMP) ===" -ForegroundColor Cyan
    & "$repo\gradlew.bat" :shared:desktopTest --console=plain; if ($LASTEXITCODE -ne 0) { $fail = $true }

    Write-Host "`n=== 6. Functions verify (scan:secrets + typecheck + jest) ===" -ForegroundColor Cyan
    Push-Location "$repo\functions"
    npm run verify; if ($LASTEXITCODE -ne 0) { $fail = $true }
    Pop-Location
}

if ($fail) {
    Write-Host "`nResultado: FALLÓ alguna comprobación. NO mergees todavía.`n" -ForegroundColor Red
    exit 1
} else {
    Write-Host "`nResultado: OK.`n" -ForegroundColor Green
    exit 0
}
