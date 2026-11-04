import json, yaml, os, hashlib, subprocess

with open(r'audit-output\gemini-focus-engine-independent-review-20260803-210342\raw_snapshot.json', 'r', encoding='utf-16le') as f:
    data = json.load(f)

snapshot = {
    'audit_id': 'gemini-focus-engine-independent-review-20260803-210342',
    'timestamp': '2026-08-03T21:03:42+02:00',
    'environment': {
        'os': data.get('os', '').strip(),
        'java_version': data.get('java_version', '').split('\n')[0].strip(),
        'gradle_version': data.get('gradle_version', '').split('\n')[0].strip(),
        'node_version': data.get('node_version', '').strip(),
        'npm_version': data.get('npm_version', '').strip(),
        'kotlin_version': data.get('kotlin_version', '').strip(),
        'tools_available': ['git', 'java', 'gradlew', 'node', 'npm', 'python']
    },
    'git': {
        'commit_sha': data.get('git_head', '').strip(),
        'branch': 'product/focus2-v5',
        'dirty_state': bool(data.get('git_status', '').strip()),
        'untracked_or_modified_files': [line.strip() for line in data.get('git_status', '').strip().split('\n') if line.strip()],
        'worktree': data.get('git_worktree', '').strip()
    },
    'canonical_commands': [
        './gradlew :shared:desktopTest --continue',
        './gradlew build',
        './gradlew detekt',
        'npm run test:storage-rules:static'
    ],
    'platforms': ['JVM', 'Android (via KMP)', 'iOS (via KMP)'],
    'physical_devices_verified': 'UNKNOWN_PENDING_INSPECTION'
}

with open(r'audit-output\gemini-focus-engine-independent-review-20260803-210342\01_FORENSIC_SNAPSHOT.yaml', 'w', encoding='utf-8') as f:
    yaml.dump(snapshot, f, sort_keys=False, default_flow_style=False)

def hash_dir(path):
    hashes = {}
    if not os.path.exists(path): return hashes
    for root, _, files in os.walk(path):
        for file in files:
            filepath = os.path.join(root, file)
            with open(filepath, 'rb') as f:
                hashes[filepath.replace('\\', '/')] = hashlib.sha256(f.read()).hexdigest()
    return hashes

all_hashes = {}
all_hashes.update(hash_dir(r'docs\context_pack'))
all_hashes.update(hash_dir(r'docs\engines\focus_v5'))
all_hashes.update(hash_dir(r'audit-output\focus-engine-audit-20260803-2037'))

with open(r'audit-output\gemini-focus-engine-independent-review-20260803-210342\snapshot_hashes.json', 'w', encoding='utf-8') as f:
    json.dump(all_hashes, f, indent=2)

print("Snapshot and hashes created.")
