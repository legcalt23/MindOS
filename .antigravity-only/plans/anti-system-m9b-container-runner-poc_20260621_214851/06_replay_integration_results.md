# Replay Integration Results

This document evaluates replay verification results and legacy compatibility.

## 1. Verification Logic
Replaying a manifest checks all file hashes against `run_manifest.json` and `package_integrity.md`, and validates transcript hash chains.

## 2. Compatibility Outcomes
- Tampered transcript files trigger a replay failure (exit code 1).
- Missing transcript files trigger an inconclusive replay (exit code 2).
