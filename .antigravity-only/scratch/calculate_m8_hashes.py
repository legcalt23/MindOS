import os
import hashlib
import json

package_dir = r"c:\Users\curro\AndroidStudioProjects\MindOS\.antigravity-only\plans\anti-system-m8-real-subagents_20260621_204632"

files = [
    "00_real_subagent_registry.md",
    "01_sandbox_enforcement_audit.md",
    "02_cli_status_reconciliation_audit.md",
    "03_cli_replay_audit.md",
    "04_test_execution_report.md",
    "05_remaining_risks_and_mitigations.md",
    "06_final_verdict.md",
    "evidence_ledger.md",
    "transcripts/transcript.jsonl"
]

def calculate_sha256(filepath):
    sha256 = hashlib.sha256()
    with open(filepath, 'rb') as f:
        content = f.read()
    # Normalize Windows line endings to LF
    normalized_content = content.replace(b'\r\n', b'\n')
    sha256.update(normalized_content)
    return sha256.hexdigest().upper()

hashes = {}
for filename in files:
    filepath = os.path.join(package_dir, filename)
    hashes[filename] = calculate_sha256(filepath)

print("HAVE HASHES:")
print(json.dumps(hashes, indent=2))
