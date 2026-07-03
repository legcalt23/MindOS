import os
import hashlib
import json

package_dir = r"c:\Users\curro\AndroidStudioProjects\MindOS\.antigravity-only\plans\anti-system-m9-native-sandbox-runner_20260621_214020"

files = [
    "00_m9_scope_and_threat_model.md",
    "01_native_sandbox_options.md",
    "02_runner_contract.md",
    "03_transcript_capture_contract.md",
    "04_replay_hardening_design.md",
    "05_poc_feasibility_decision.md",
    "06_remaining_risks.md",
    "07_final_verdict.md"
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
