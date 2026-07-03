[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('Acquire', 'Release', 'Status')]
    [string]$Action,
    [string]$TaskId,
    [string]$Agent = 'secure_code_remediator',
    [string]$WorkspaceRoot
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) { $WorkspaceRoot = Join-Path $PSScriptRoot '..\..' }

function Fail([string]$Code) {
    [pscustomobject]@{ ok = $false; code = $Code } | ConvertTo-Json -Compress
    exit 1
}

$root = [IO.Path]::GetFullPath($WorkspaceRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
if (-not (Test-Path -LiteralPath $root -PathType Container)) { Fail 'workspace_not_found' }
if ($Action -ne 'Status' -and $TaskId -notmatch '^SEC-[A-Za-z0-9-]{3,80}$') { Fail 'invalid_task_id' }
if ($Action -ne 'Status' -and $Agent -ne 'secure_code_remediator') { Fail 'writer_not_authorized' }

$stateDir = Join-Path $root '.security-state'
[IO.Directory]::CreateDirectory($stateDir) | Out-Null
$lockPath = Join-Path $stateDir 'writer-lock.jsonl'
$stream = [IO.File]::Open($lockPath, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
try {
    $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8, $true, 1024, $true)
    $content = $reader.ReadToEnd()
    $reader.Dispose()
    $last = $null
    foreach ($line in ($content -split "`r?`n")) {
        if ($line.Trim()) { $last = $line | ConvertFrom-Json }
    }

    if ($Action -eq 'Status') {
        $active = $null -ne $last -and $last.event -eq 'acquired'
        [pscustomobject]@{ ok = $true; active = $active; task_id = $(if ($active) { $last.task_id } else { $null }) } | ConvertTo-Json -Compress
        exit 0
    }
    if ($Action -eq 'Acquire' -and $null -ne $last -and $last.event -eq 'acquired') { Fail 'writer_lock_occupied' }
    if ($Action -eq 'Release' -and ($null -eq $last -or $last.event -ne 'acquired')) { Fail 'writer_lock_not_active' }
    if ($Action -eq 'Release' -and $last.task_id -ne $TaskId) { Fail 'writer_lock_task_mismatch' }

    $event = if ($Action -eq 'Acquire') { 'acquired' } else { 'released' }
    $record = [ordered]@{
        schema = 'mindos.security.writer-lock/v1'
        timestamp_utc = [DateTime]::UtcNow.ToString('o')
        event = $event
        task_id = $TaskId
        agent = $Agent
    } | ConvertTo-Json -Compress
    $stream.Seek(0, [IO.SeekOrigin]::End) | Out-Null
    $writer = New-Object IO.StreamWriter($stream, (New-Object Text.UTF8Encoding($false)), 1024, $true)
    $writer.WriteLine($record)
    $writer.Flush()
    $stream.Flush($true)
    $writer.Dispose()
    [pscustomobject]@{ ok = $true; event = $event; task_id = $TaskId } | ConvertTo-Json -Compress
} finally {
    $stream.Dispose()
}
