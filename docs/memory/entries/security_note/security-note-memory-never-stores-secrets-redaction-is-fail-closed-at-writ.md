---
id: security-note-memory-never-stores-secrets-redaction-is-fail-closed-at-writ
type: SECURITY_NOTE
title: Memory never stores secrets; redaction is fail-closed at write and retrieval
tags: [memory-layer, security, redaction, privacy]
source: functions/scripts/scan-secrets.js
importance: critical
created_at: 2026-06-30
updated_at: 2026-06-30
version: 1
encrypted: false
access_level: internal
related: []
supersedes: null
superseded_by: null
metadata: {}
content_hash: 0d10b16ba5daf6efb807804e0a6489ce4ed8123d430c4e98122fd155ea3855d6
---

## Decision
The memory layer must never persist or surface secrets/PII. v1 does not encrypt — it REFUSES sensitive content instead (keep real secrets in the OS store / password manager).

## Control
lib/redaction reuses the repo secret scanner (scanContent + high-entropy heuristic) plus PII patterns (email, phone, IPv4, credit-card via Luhn, JWT). The write guard is fail-closed: a finding rejects the write and nothing is persisted. The retrieval scrubber re-scans every snippet before it reaches the model.

## Boundary (what must never cross)
No secret/token/API key/PII may enter an entry, the index, an embedding, or a prompt splice. Sensitive entries (if ever allowed) surface as title+metadata only.

## Verification
A canary test seeds a realistic fake key and asserts it never appears in any splice; `node .claude/memory/cli.mjs verify` runs the guard checks.
