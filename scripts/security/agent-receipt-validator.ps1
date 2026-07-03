[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$InputFile,
    [string]$WorkspaceRoot
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($WorkspaceRoot)) { $WorkspaceRoot = Join-Path $PSScriptRoot '..\..' }
function Reject([string]$Code) {
    [pscustomobject]@{ valid = $false; code = $Code } | ConvertTo-Json -Compress
    exit 1
}

$root = [IO.Path]::GetFullPath($WorkspaceRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
$fullInput = [IO.Path]::GetFullPath($InputFile)
$prefix = $root + [IO.Path]::DirectorySeparatorChar
if ($fullInput -ne $root -and -not $fullInput.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { Reject 'receipt_outside_workspace' }
if ([IO.Path]::GetFileName($fullInput) -match '(?i)^(\.env($|\.)|auth\.json$|secrets?\.|credentials?\.|.*\.(pem|key|p12|pfx|jks|keystore)$)') { Reject 'sensitive_receipt_path' }
if (-not (Test-Path -LiteralPath $fullInput -PathType Leaf)) { Reject 'receipt_not_found' }
if ((Get-Item -LiteralPath $fullInput).Length -gt 1048576) { Reject 'receipt_too_large' }
try { $receipt = Get-Content -LiteralPath $fullInput -Raw | ConvertFrom-Json } catch { Reject 'invalid_json' }

$required = @('schema','task_id','agent','autonomy','scope_observed','actions','files_read','files_written','commands','findings','network_used','secrets_accessed','result','handoff_to','limitations')
$actual = @($receipt.PSObject.Properties.Name)
foreach ($field in $required) { if ($actual -notcontains $field) { Reject ('missing_' + $field) } }
foreach ($field in $actual) { if ($required -notcontains $field) { Reject ('unknown_' + $field) } }
if ($receipt.schema -ne 'mindos.security.agent-receipt/v1') { Reject 'invalid_schema' }
if ($receipt.task_id -notmatch '^SEC-[A-Za-z0-9-]{3,80}$') { Reject 'invalid_task_id' }

$agents = @(
    'security_orchestrator','security_lead_ciso','appsec_auditor','secure_code_remediator',
    'threat_modeling_agent','dependency_supply_chain','security_qa_verifier','documentation_sanitizer',
    'incident_response_commander','red_team_lab_authorized','cloud_security_auditor',
    'iam_secrets_crypto_auditor','devsecops_cicd_auditor','detection_engineering','blue_team_soc',
    'forensics_agent','compliance_privacy_agent'
)
if ($agents -notcontains $receipt.agent) { Reject 'agent_not_allowlisted' }
if (@('A0','A1','A2','A3') -notcontains $receipt.autonomy) { Reject 'invalid_autonomy' }
if ($receipt.scope_observed -isnot [bool] -or -not $receipt.scope_observed) { Reject 'scope_not_observed' }
if ($receipt.network_used -isnot [bool] -or $receipt.network_used) { Reject 'network_must_be_false' }
if ($receipt.secrets_accessed -isnot [bool] -or $receipt.secrets_accessed) { Reject 'secrets_must_be_false' }
foreach ($field in @('actions','files_read','files_written','commands','findings','limitations')) {
    if ($null -eq $receipt.$field -or $receipt.$field -is [string]) { Reject ($field + '_must_be_array') }
}

$allowedActions = @('read','local_test','write','hash','classify','report','verify','coordinate')
foreach ($action in @($receipt.actions)) {
    if ($action -isnot [string] -or $allowedActions -notcontains $action) { Reject 'invalid_action' }
}
$handoffTargets = @($agents + @('human_owner','none'))
if ($handoffTargets -notcontains $receipt.handoff_to) { Reject 'invalid_handoff' }

foreach ($path in @($receipt.files_read) + @($receipt.files_written)) {
    if ($path -isnot [string] -or [IO.Path]::IsPathRooted($path) -or $path -match '(^|[\\/])\.\.([\\/]|$)') { Reject 'invalid_file_path' }
    if ($path -match '(?i)(^|[\\/])(\.env($|\.)|auth\.json$|\.sandbox-secrets$|secrets?($|\.)|credentials?($|\.)|.*\.(pem|key|p12|pfx|jks|keystore|kubeconfig)$)') { Reject 'sensitive_file_path' }
}
foreach ($finding in @($receipt.findings)) {
    if ($finding -isnot [string] -or $finding -notmatch '^[A-Za-z0-9_.:-]{1,80}$') { Reject 'invalid_finding_ref' }
}
foreach ($limitation in @($receipt.limitations)) {
    if ($limitation -isnot [string] -or $limitation -notmatch '^[A-Za-z0-9 _.-]{1,120}$') { Reject 'invalid_limitation' }
}

$writes = @($receipt.files_written)
if ($writes.Count -gt 0 -and $receipt.agent -ne 'secure_code_remediator') { Reject 'writer_not_authorized' }

if ($receipt.agent -eq 'secure_code_remediator') {
    if ($receipt.autonomy -ne 'A2') { Reject 'remediator_requires_A2' }
    if ($receipt.result -eq 'verified') { Reject 'remediator_cannot_verify' }
    if ($receipt.handoff_to -ne 'security_qa_verifier') { Reject 'remediator_handoff_required' }
} elseif ($receipt.autonomy -eq 'A2') {
    Reject 'A2_reserved_for_remediator'
}
if ($receipt.result -eq 'verified' -and $receipt.agent -ne 'security_qa_verifier') { Reject 'verification_not_authorized' }
if (@('needs_review','verified','failed','blocked','not_actionable') -notcontains $receipt.result) { Reject 'invalid_result' }

foreach ($command in @($receipt.commands)) {
    $names = @($command.PSObject.Properties.Name)
    foreach ($field in @('name','exit_code','evidence_hash')) { if ($names -notcontains $field) { Reject 'incomplete_command_evidence' } }
    if ($command.name -notmatch '^[A-Za-z0-9 _.-]{1,120}$') { Reject 'invalid_command_label' }
    if ($command.exit_code -isnot [int] -and $command.exit_code -isnot [long]) { Reject 'invalid_exit_code' }
    if ($command.evidence_hash -notmatch '^sha256:[a-fA-F0-9]{64}$') { Reject 'invalid_evidence_hash' }
}

$receiptJson = $receipt | ConvertTo-Json -Compress -Depth 8
if ($receiptJson -match '(?i)-----BEGIN .*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9_]{20,}|eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}|\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b') { Reject 'dlp_candidate' }

[pscustomobject]@{ valid = $true; task_id = $receipt.task_id; agent = $receipt.agent; result = $receipt.result } | ConvertTo-Json -Compress
