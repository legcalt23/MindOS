[CmdletBinding(DefaultParameterSetName = 'Text')]
param(
    [Parameter(Mandatory = $true, ParameterSetName = 'Text')]
    [string]$Text,
    [Parameter(Mandatory = $true, ParameterSetName = 'File')]
    [string]$InputFile,
    [string]$WorkspaceRoot,
    [int]$MaxBytes = 2097152
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) { $WorkspaceRoot = Join-Path $PSScriptRoot '..\..' }
function Block([string[]]$Types) {
    [pscustomobject]@{ allowed = $false; finding_types = @($Types | Sort-Object -Unique); count = @($Types).Count } | ConvertTo-Json -Compress
    exit 1
}

if ($PSCmdlet.ParameterSetName -eq 'File') {
    $root = [IO.Path]::GetFullPath($WorkspaceRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
    $fullInput = [IO.Path]::GetFullPath($InputFile)
    $prefix = $root + [IO.Path]::DirectorySeparatorChar
    if ($fullInput -ne $root -and -not $fullInput.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { Block @('input_outside_workspace') }
    $leaf = [IO.Path]::GetFileName($fullInput)
    if ($leaf -match '(?i)^(\.env($|\.)|auth\.json$|secrets?\.|credentials?\.|.*\.(pem|key|p12|pfx|jks|keystore)$)') { Block @('sensitive_path_name') }
    if (-not (Test-Path -LiteralPath $fullInput -PathType Leaf)) { Block @('input_not_found') }
    if ((Get-Item -LiteralPath $fullInput).Length -gt $MaxBytes) { Block @('input_too_large') }
    $candidate = [IO.File]::ReadAllText($fullInput)
} else {
    if ([Text.Encoding]::UTF8.GetByteCount($Text) -gt $MaxBytes) { Block @('input_too_large') }
    $candidate = $Text
}

$patterns = [ordered]@{
    private_key = '-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----'
    aws_key = 'AKIA[0-9A-Z]{16}'
    github_token = 'gh[pousr]_[A-Za-z0-9_]{20,}'
    slack_token = 'xox[baprs]-[A-Za-z0-9-]{10,}'
    stripe_key = '(?i)sk_(live|test)_[A-Za-z0-9]{16,}'
    jwt = 'eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}'
    email = '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b'
    phone = '(?<![\w-])(?:\+?\d(?:[ .()]?\d){8,14})(?![\w-])'
}
$hits = New-Object Collections.Generic.List[string]
foreach ($entry in $patterns.GetEnumerator()) {
    if ([regex]::IsMatch($candidate, $entry.Value)) { $hits.Add($entry.Key) }
}
if ($hits.Count -gt 0) { Block $hits.ToArray() }

[pscustomobject]@{ allowed = $true; finding_types = @(); count = 0 } | ConvertTo-Json -Compress
