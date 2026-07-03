# PowerShell wrapper to run the watchdog script

param(
    [string]$WorkspacePath
)

# Resolve workspace path if not explicitly provided
if ([string]::IsNullOrEmpty($WorkspacePath)) {
    $scriptRoot = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
    $WorkspacePath = Resolve-Path (Join-Path $scriptRoot "..\..")
}

$watchdogScript = Join-Path $WorkspacePath "scripts\gmail_watchdog\watchdog.py"

Write-Host "Running watchdog.py from $watchdogScript..."
python $watchdogScript

if ($LASTEXITCODE -ne 0) {
    Write-Error "Watchdog execution failed with exit code $LASTEXITCODE"
    exit 1
}

exit 0
