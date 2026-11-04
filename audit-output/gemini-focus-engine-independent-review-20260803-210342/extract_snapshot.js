const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const yaml = require('yaml'); // may not be installed, better to format yaml manually

const rawData = fs.readFileSync('audit-output/gemini-focus-engine-independent-review-20260803-210342/raw_snapshot.json', 'utf16le');
// Since it's from powershell, it might have BOM or be somewhat tricky, let's try to parse it
const data = JSON.parse(rawData.replace(/^\uFEFF/, ''));

const untracked_or_modified_files = (data.git_status || '').split('\n').map(l => l.trim()).filter(l => l);

const snapshot = {
    audit_id: 'gemini-focus-engine-independent-review-20260803-210342',
    timestamp: '2026-08-03T21:03:42+02:00',
    environment: {
        os: (data.os || '').trim(),
        java_version: (data.java_version || '').split('\n')[0].trim(),
        gradle_version: (data.gradle_version || '').split('\n')[0].trim(),
        node_version: (data.node_version || '').trim(),
        npm_version: (data.npm_version || '').trim(),
        kotlin_version: (data.kotlin_version || '').trim(),
        tools_available: ['git', 'java', 'gradlew', 'node', 'npm']
    },
    git: {
        commit_sha: (data.git_head || '').trim(),
        branch: 'product/focus2-v5',
        dirty_state: untracked_or_modified_files.length > 0,
        untracked_or_modified_files: untracked_or_modified_files,
        worktree: (data.git_worktree || '').trim()
    },
    canonical_commands: [
        './gradlew :shared:desktopTest --continue',
        './gradlew build',
        './gradlew detekt',
        'npm run test:storage-rules:static'
    ],
    platforms: ['JVM', 'Android (via KMP)', 'iOS (via KMP)'],
    physical_devices_verified: 'UNKNOWN_PENDING_INSPECTION'
};

// Manually format YAML since we don't know if 'yaml' is installed
function toYaml(obj, indent=0) {
    let result = '';
    const sp = ' '.repeat(indent);
    for (const key in obj) {
        const val = obj[key];
        if (Array.isArray(val)) {
            result += `${sp}${key}:\n`;
            for (const item of val) {
                result += `${sp}  - ${JSON.stringify(item)}\n`;
            }
        } else if (typeof val === 'object' && val !== null) {
            result += `${sp}${key}:\n` + toYaml(val, indent+2);
        } else {
            result += `${sp}${key}: ${JSON.stringify(val)}\n`;
        }
    }
    return result;
}

fs.writeFileSync('audit-output/gemini-focus-engine-independent-review-20260803-210342/01_FORENSIC_SNAPSHOT.yaml', toYaml(snapshot), 'utf8');

function hashDir(dirPath) {
    const hashes = {};
    if (!fs.existsSync(dirPath)) return hashes;
    
    function walk(dir) {
        const files = fs.readdirSync(dir);
        for (const file of files) {
            const filepath = path.join(dir, file);
            const stat = fs.statSync(filepath);
            if (stat.isDirectory()) {
                walk(filepath);
            } else {
                const content = fs.readFileSync(filepath);
                hashes[filepath.replace(/\\/g, '/')] = crypto.createHash('sha256').update(content).digest('hex');
            }
        }
    }
    walk(dirPath);
    return hashes;
}

const allHashes = {};
Object.assign(allHashes, hashDir('docs/context_pack'));
Object.assign(allHashes, hashDir('docs/engines/focus_v5'));
Object.assign(allHashes, hashDir('audit-output/focus-engine-audit-20260803-2037'));

fs.writeFileSync('audit-output/gemini-focus-engine-independent-review-20260803-210342/snapshot_hashes.json', JSON.stringify(allHashes, null, 2), 'utf8');

console.log("Snapshot and hashes created successfully.");
