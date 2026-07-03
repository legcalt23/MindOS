[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Path,
    [string]$WorkspaceRoot
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) { $WorkspaceRoot = Join-Path $PSScriptRoot '..\..' }
function Fail([string]$Code) {
    [pscustomobject]@{ allowed = $false; code = $Code } | ConvertTo-Json -Compress
    exit 1
}

$root = [IO.Path]::GetFullPath($WorkspaceRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
$candidate = if ([IO.Path]::IsPathRooted($Path)) { [IO.Path]::GetFullPath($Path) } else { [IO.Path]::GetFullPath((Join-Path $root $Path)) }
$prefix = $root + [IO.Path]::DirectorySeparatorChar
if ($candidate -ne $root -and -not $candidate.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { Fail 'path_outside_workspace' }

$relative = $candidate.Substring($root.Length).TrimStart('\', '/')
$segments = $relative -split '[\\/]'
$blocked = '(?i)^(\.env($|\.)|auth\.json$|\.sandbox-secrets$|credentials?($|\.)|secrets?($|\.)|tokens?($|\.)|id_(rsa|dsa|ecdsa|ed25519)($|\.)|.*\.(pem|key|p12|pfx|jks|keystore|kubeconfig)$|keychain($|\.)|certs?$)'
foreach ($segment in $segments) {
    if ($segment -match $blocked) { Fail 'sensitive_path_name' }
}

[pscustomobject]@{ allowed = $true; relative_path = ($relative -replace '\\', '/') } | ConvertTo-Json -Compress
