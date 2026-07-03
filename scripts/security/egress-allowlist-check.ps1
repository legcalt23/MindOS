[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Uri,
    [string[]]$AllowedHost = @()
)

$ErrorActionPreference = 'Stop'
function Deny([string]$Reason) {
    [pscustomobject]@{ allowed = $false; reason = $Reason } | ConvertTo-Json -Compress
    exit 1
}

$parsed = $null
if (-not [Uri]::TryCreate($Uri, [UriKind]::Absolute, [ref]$parsed)) { Deny 'invalid_uri' }
if ($parsed.Scheme -ne 'https') { Deny 'scheme_denied' }
if (-not [string]::IsNullOrEmpty($parsed.UserInfo)) { Deny 'userinfo_denied' }
$targetHost = $parsed.IdnHost.ToLowerInvariant().TrimEnd('.')
$ip = $null
if ([Net.IPAddress]::TryParse($targetHost, [ref]$ip)) { Deny 'ip_literal_denied' }
if ($targetHost -eq 'localhost' -or $targetHost.EndsWith('.localhost')) { Deny 'local_host_denied' }
$normalized = @($AllowedHost | ForEach-Object { $_.ToLowerInvariant().TrimEnd('.') })
if ($normalized -notcontains $targetHost) { Deny 'host_not_allowlisted' }

[pscustomobject]@{ allowed = $true; scheme = 'https'; host = $targetHost } | ConvertTo-Json -Compress
