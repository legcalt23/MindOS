[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$RecordFile,
    [string]$WorkspaceRoot
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) { $WorkspaceRoot = Join-Path $PSScriptRoot '..\..' }
function Reject([string]$Code) {
    [pscustomobject]@{ appended = $false; code = $Code } | ConvertTo-Json -Compress
    exit 1
}

$root = [IO.Path]::GetFullPath($WorkspaceRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
if (-not (Test-Path -LiteralPath $root -PathType Container)) { Reject 'workspace_not_found' }
$fullRecord = [IO.Path]::GetFullPath($RecordFile)
$prefix = $root + [IO.Path]::DirectorySeparatorChar
if ($fullRecord -ne $root -and -not $fullRecord.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { Reject 'record_outside_workspace' }
if ([IO.Path]::GetFileName($fullRecord) -match '(?i)^(\.env($|\.)|auth\.json$|secrets?\.|credentials?\.|.*\.(pem|key|p12|pfx|jks|keystore)$)') { Reject 'sensitive_record_path' }
if (-not (Test-Path -LiteralPath $fullRecord -PathType Leaf)) { Reject 'record_not_found' }
if ((Get-Item -LiteralPath $fullRecord).Length -gt 65536) { Reject 'record_too_large' }
try { $record = Get-Content -LiteralPath $fullRecord -Raw | ConvertFrom-Json } catch { Reject 'invalid_json' }

$allowed = @('schema','timestamp_utc','task_id','agent','event','artifact_type','artifact_hash','relative_path','result','approval_refs','notes')
$required = @('schema','timestamp_utc','task_id','agent','event','artifact_type','artifact_hash','result')
$actual = @($record.PSObject.Properties.Name)
foreach ($field in $required) { if ($actual -notcontains $field) { Reject ('missing_' + $field) } }
foreach ($field in $actual) { if ($allowed -notcontains $field) { Reject ('field_denied_' + $field) } }
if ($record.schema -ne 'mindos.security.evidence-ledger/v1') { Reject 'invalid_schema' }
if ($record.task_id -notmatch '^SEC-[A-Za-z0-9-]{3,80}$') { Reject 'invalid_task_id' }
if ($record.artifact_hash -notmatch '^sha256:[a-fA-F0-9]{64}$') { Reject 'invalid_artifact_hash' }
$parsedDate = [DateTime]::MinValue
if (-not [DateTime]::TryParse($record.timestamp_utc, [ref]$parsedDate)) { Reject 'invalid_timestamp' }
foreach ($field in @('agent','event','artifact_type','result')) {
    if ($record.$field -notmatch '^[A-Za-z0-9_.-]{1,80}$') { Reject ('invalid_' + $field) }
}
if ($actual -contains 'notes' -and ($record.notes -notmatch '^[A-Za-z0-9 _.-]{0,120}$')) { Reject 'invalid_notes' }
if ($actual -contains 'approval_refs' -and $record.approval_refs -is [string]) { Reject 'approval_refs_must_be_array' }
foreach ($approval in @($record.approval_refs)) {
    if ($approval -isnot [string] -or $approval -notmatch '^[A-Za-z0-9_.:-]{1,80}$') { Reject 'invalid_approval_ref' }
}

if ($actual -contains 'relative_path') {
    $path = [string]$record.relative_path
    if ([IO.Path]::IsPathRooted($path) -or $path -match '(^|[\\/])\.\.([\\/]|$)') { Reject 'invalid_relative_path' }
    if ($path -match '(?i)(^|[\\/])(\.env($|\.)|auth\.json$|secrets?($|\.)|credentials?($|\.)|.*\.(pem|key|p12|pfx|jks|keystore|kubeconfig)$)') { Reject 'sensitive_relative_path' }
}

$json = $record | ConvertTo-Json -Compress -Depth 5
if ($json -match '(?i)-----BEGIN .*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9_]{20,}|eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}|\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b') { Reject 'dlp_candidate' }

$stateDir = Join-Path $root '.security-state'
[IO.Directory]::CreateDirectory($stateDir) | Out-Null
$ledger = Join-Path $stateDir 'evidence-ledger.jsonl'
$stream = [IO.File]::Open($ledger, [IO.FileMode]::Append, [IO.FileAccess]::Write, [IO.FileShare]::None)
try {
    $writer = New-Object IO.StreamWriter($stream, (New-Object Text.UTF8Encoding($false)))
    $writer.WriteLine($json)
    $writer.Flush()
    $stream.Flush($true)
    $writer.Dispose()
} finally {
    $stream.Dispose()
}

[pscustomobject]@{ appended = $true; task_id = $record.task_id; artifact_hash = $record.artifact_hash } | ConvertTo-Json -Compress
