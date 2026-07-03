[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\..'))
$controls = Join-Path $repo 'scripts\security'
$testRoot = Join-Path $repo ('build\security-agentic-tests\' + [Guid]::NewGuid().ToString('N'))
$workspace = Join-Path $testRoot 'workspace'
[IO.Directory]::CreateDirectory($workspace) | Out-Null

function Invoke-Control([string]$Script, [string[]]$Arguments) {
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $Script @Arguments 2>&1 | Out-String
    [pscustomobject]@{ Code = $LASTEXITCODE; Output = $output.Trim() }
}

function Assert-Code([string]$Name, $Result, [int]$Expected) {
    if ($Result.Code -ne $Expected) {
        throw "$Name expected exit $Expected but received $($Result.Code): $($Result.Output)"
    }
    Write-Output "PASS $Name"
}

$writer = Join-Path $controls 'writer-lock.ps1'
$first = Invoke-Control $writer @('-Action','Acquire','-TaskId','SEC-TEST-LOCK-001','-WorkspaceRoot',$workspace)
Assert-Code 'writer lock acquire' $first 0
$collision = Invoke-Control $writer @('-Action','Acquire','-TaskId','SEC-TEST-LOCK-002','-WorkspaceRoot',$workspace)
Assert-Code 'writer lock collision fails closed' $collision 1
$release = Invoke-Control $writer @('-Action','Release','-TaskId','SEC-TEST-LOCK-001','-WorkspaceRoot',$workspace)
Assert-Code 'writer lock release' $release 0

$pathGuard = Join-Path $controls 'secret-path-guard.ps1'
$safePath = Invoke-Control $pathGuard @('-Path','src/safe.kt','-WorkspaceRoot',$workspace)
Assert-Code 'safe path accepted' $safePath 0
$secretPath = Invoke-Control $pathGuard @('-Path','.env','-WorkspaceRoot',$workspace)
Assert-Code 'secret path rejected without read' $secretPath 1
$escapePath = Invoke-Control $pathGuard @('-Path','../outside.txt','-WorkspaceRoot',$workspace)
Assert-Code 'workspace escape rejected' $escapePath 1

$dlp = Join-Path $controls 'dlp-output-check.ps1'
$cleanDlp = Invoke-Control $dlp @('-Text','sanitized finding with sha256 metadata only')
Assert-Code 'clean output accepted' $cleanDlp 0
$canary = 'AKIA' + 'ABCDEFGHIJKLMNOP'
$blockedDlp = Invoke-Control $dlp @('-Text',$canary)
Assert-Code 'synthetic secret canary rejected' $blockedDlp 1
if ($blockedDlp.Output.Contains($canary)) { throw 'DLP output reproduced the synthetic canary' }
$outsideCandidate = Join-Path $testRoot 'outside-candidate.txt'
'clean but out of scope' | Set-Content -LiteralPath $outsideCandidate -Encoding UTF8
$outsideDlp = Invoke-Control $dlp @('-InputFile',$outsideCandidate,'-WorkspaceRoot',$workspace)
Assert-Code 'DLP file outside workspace rejected before read' $outsideDlp 1

$egress = Join-Path $controls 'egress-allowlist-check.ps1'
$blockedEgress = Invoke-Control $egress @('-Uri','https://example.invalid/resource')
Assert-Code 'egress denied by default' $blockedEgress 1
$allowedEgress = Invoke-Control $egress @('-Uri','https://updates.example.invalid/resource','-AllowedHost','updates.example.invalid')
Assert-Code 'exact allowlist is evaluated without connecting' $allowedEgress 0

$receiptValidator = Join-Path $controls 'agent-receipt-validator.ps1'
$validReceiptPath = Join-Path $testRoot 'valid-receipt.json'
$validReceipt = [ordered]@{
    schema = 'mindos.security.agent-receipt/v1'
    task_id = 'SEC-TEST-RECEIPT-001'
    agent = 'appsec_auditor'
    autonomy = 'A1'
    scope_observed = $true
    actions = @('read')
    files_read = @('src/safe.kt')
    files_written = @()
    commands = @()
    findings = @('F-001')
    network_used = $false
    secrets_accessed = $false
    result = 'needs_review'
    handoff_to = 'security_orchestrator'
    limitations = @()
}
$validReceipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $validReceiptPath -Encoding UTF8
$validResult = Invoke-Control $receiptValidator @('-InputFile',$validReceiptPath)
Assert-Code 'complete read-only receipt accepted' $validResult 0
$outsideReceipt = Invoke-Control $receiptValidator @('-InputFile',$validReceiptPath,'-WorkspaceRoot',$workspace)
Assert-Code 'receipt outside workspace rejected before read' $outsideReceipt 1

$invalidReceiptPath = Join-Path $testRoot 'invalid-receipt.json'
$invalidReceipt = $validReceipt | ConvertTo-Json -Depth 6 | ConvertFrom-Json
$invalidReceipt.files_written = @('src/unauthorized.kt')
$invalidReceipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $invalidReceiptPath -Encoding UTF8
$invalidResult = Invoke-Control $receiptValidator @('-InputFile',$invalidReceiptPath)
Assert-Code 'auditor write receipt rejected' $invalidResult 1

$incompleteReceiptPath = Join-Path $testRoot 'incomplete-receipt.json'
$incompleteReceipt = $validReceipt | ConvertTo-Json -Depth 6 | ConvertFrom-Json
$incompleteReceipt.PSObject.Properties.Remove('network_used')
$incompleteReceipt | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $incompleteReceiptPath -Encoding UTF8
$incompleteResult = Invoke-Control $receiptValidator @('-InputFile',$incompleteReceiptPath)
Assert-Code 'incomplete receipt rejected' $incompleteResult 1

$selfVerifiedPath = Join-Path $testRoot 'self-verified-receipt.json'
$selfVerified = $validReceipt | ConvertTo-Json -Depth 6 | ConvertFrom-Json
$selfVerified.agent = 'secure_code_remediator'
$selfVerified.autonomy = 'A2'
$selfVerified.result = 'verified'
$selfVerified.handoff_to = 'security_qa_verifier'
$selfVerified | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $selfVerifiedPath -Encoding UTF8
$selfVerifiedResult = Invoke-Control $receiptValidator @('-InputFile',$selfVerifiedPath)
Assert-Code 'remediator self-verification rejected' $selfVerifiedResult 1

$ledgerAppend = Join-Path $controls 'evidence-ledger-append.ps1'
$recordPath = Join-Path $workspace 'record.json'
$record = [ordered]@{
    schema = 'mindos.security.evidence-ledger/v1'
    timestamp_utc = [DateTime]::UtcNow.ToString('o')
    task_id = 'SEC-TEST-LEDGER-001'
    agent = 'appsec_auditor'
    event = 'receipt_validated'
    artifact_type = 'agent_receipt'
    artifact_hash = 'sha256:' + ('a' * 64)
    relative_path = 'build/security-evidence/receipt.json'
    result = 'needs_review'
    approval_refs = @()
    notes = 'metadata-only'
}
$record | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $recordPath -Encoding UTF8
$appendResult = Invoke-Control $ledgerAppend @('-RecordFile',$recordPath,'-WorkspaceRoot',$workspace)
Assert-Code 'sanitized ledger record appended' $appendResult 0

$badRecordPath = Join-Path $workspace 'bad-record.json'
$badRecord = $record | ConvertTo-Json -Depth 6 | ConvertFrom-Json
$badRecord | Add-Member -NotePropertyName prompt -NotePropertyValue 'untrusted raw content'
$badRecord | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $badRecordPath -Encoding UTF8
$badAppend = Invoke-Control $ledgerAppend @('-RecordFile',$badRecordPath,'-WorkspaceRoot',$workspace)
Assert-Code 'raw prompt field rejected by ledger' $badAppend 1

Write-Output "OK security_controls temp=$($testRoot.Substring($repo.Length).TrimStart('\')) network_calls=0"
