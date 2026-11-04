# S5_R3_LOG_AND_PII — restricted vs sanitized logs + PII/secret scan (mandate Part 11 / Phase 6)

> **V2 remediation note (2026-07-23):** the prior version of this doc shipped the raw host/username literals
> (to "describe the sanitization map") AND described a `grep … | grep -v` scan whose exit code was **inverted**
> (a clean corpus exited 1, a dirty corpus exited 0). Both are corrected here. The raw literals are gone; the
> scan is now a deterministic Node tool (`scripts/pii-secret-scan.mjs`) with unambiguous exit codes and per-rule
> fixtures. See defect `R-DEF-03` in the remediation ledger.

## 1. The two logs (RAW_RESTRICTED vs SANITIZED_TRANSFER)

Phase 0 needs the real cwd to hash worktree bytes; the transferable package must not ship host paths. Resolved by
separating two tiers:

| log | location | contents | class |
|---|---|---|---|
| **RAW_RESTRICTED** | session scratchpad (NOT committed, NOT a package member) + the untracked `reaudit/v2/preimages/**` (host-local preimages of S5_R4 process docs that contain absolute Windows paths) | exact stdout/stderr with absolute worktree paths + hostname | `RAW_RESTRICTED` — session/host-local only; **never transferred** |
| **SANITIZED_TRANSFER** | `reaudit/v2/logs/command_log.jsonl`, `logs/gradle_TEST-*.xml`, `logs/focus2_full_suite_result.json`, `logs/pii_scan_report.json` (package members) | commands + verdicts with pseudonymized paths (`<USER>`, `<HOST>`, `<ISOLATED_WT>`, `<REPO>`), exit codes, test counts | package / transfer |

**Retention / cleanup of RAW_RESTRICTED (open residue, honest):** the `reaudit/v2/preimages/**` files are untracked
working-tree artifacts that still contain absolute `c:\users\<USER>\…` paths. They are **NOT** manifest members, so
they do **not** ship in the bundle — but they have **not been deleted from the working tree**. Status:
`RAW_RESTRICTED_NOT_CLEANED` (residue `R-DEF-04`). They must be either deleted or path-redacted before any wider
share of the working tree. The transferable package is clean (proven by the scan in §2); the working tree is not.

## 2. PII / secret scan (deterministic tool — no bare "0 hits")

Tool: **`scripts/pii-secret-scan.mjs`** (Node, zero-dep). Corpus: exactly the **manifest members** (or an explicit
`--files` set, e.g. a diff). Replaces the prior inverted `grep|grep -v` pipeline.

| field | value |
|---|---|
| exit codes | **0 = clean, 1 = findings, 2 = tool/selftest error** (unambiguous; a broken scanner cannot report clean — the selftest gates every run) |
| rules (16) | `win_user_path` (both `C:\` and `C:/`), `unix_home_path`, `github_pat` (`ghp_`/`github_pat_`), `aws_akia`, `gcp_api_key`, `slack_token`, `openai_key`, `jwt`, `bearer`, `private_key`, `firebase_url`, `url_with_creds`, `email` (**any TLD**, not just com/org/net), `ipv4`, `known_hostname`, `high_entropy` (mixed-case base64 ≥40, entropy ≥4.2, hex-digests excluded) |
| fixtures | positive **and** negative per rule; the selftest fails (exit 2) if any fixture regresses (it already caught two of my own bugs: a 33-char high-entropy positive < the 40 floor, and path/identifier false-positives) |
| allowlist | structured by **file + rule + value** (git/sha256 digests; `noreply@anthropic.com`; `example.*`/loopback IPs; the scanner exempts its own pattern literals). **Never** "line contains sanitize/replace". |
| output | `reaudit/v2/logs/pii_scan_report.json` — `{denominator_lines, hits_total, allowlisted, by_rule, findings, exit}` + `config_sha256` (tool self-hash) |
| result | after redacting the two command-log cwd paths + this doc's literals: **exit 0, 0 findings** over the manifest members |

**Disclosure of what the scan does and does NOT assert:**
- `NO_CUSTOM_PAYLOAD_PII_FOUND` in the committed package members — **asserted** (scan exit 0).
- `NO_PERSONAL_DATA_PROCESSED` by the product — **NOT asserted, and false to assert.** Crashlytics/telemetry SDKs
  collect device metadata + installation/SDK identifiers regardless of app payload; "no raw Throwable crosses the
  boundary" ≠ "no personal data". This is a product-privacy question for the controller/DPO (see privacy data map),
  not something a text scan can clear.

## 3. Reproduce
```bash
node scripts/pii-secret-scan.mjs --selftest   # gate the tool: exit 0 = fixtures pass, 2 = tool broken
node scripts/pii-secret-scan.mjs              # scan manifest members: exit 0 clean / 1 findings / 2 tool error
node scripts/pii-secret-scan.mjs --files <path…>   # scan an explicit set (e.g. a diff)
```
