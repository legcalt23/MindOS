# Verification Script Results

This document records the validation results and command-line support of the hardened verification script `verify_subagents.py`.

---

## 1. CLI Arguments Supported
The script has been updated to support the following flags:
- `--package [path]`: (Required) Path to the target review package directory.
- `--json`: Format output to stdout exclusively as JSON.
- `--strict`: Promote warnings (like cautious claims warnings) to validation failures.
- `--write-failures`: Generate `13_verification_failures.md` on failure.

---

## 2. Test Execution Log

```bash
python .antigravity-only/scripts/verify_subagents.py \
  --package .antigravity-only/plans/anti-system-review_20260621_150701/ \
  --json \
  --strict \
  --write-failures
```

- **Exit Code:** `0`
- **Verification Status:** `PASS`
- **Output Trace:**
```json
{
  "status": "PASS",
  "failures": [],
  "warnings": [],
  "final_status_allowed": true
}
```

## 3. Strict Verification Capabilities
The script successfully enforces:
1.  **Completeness:** Rejects packages missing any of the 15 required files.
2.  **Structural Integrity:** Verifies that no section headings are empty.
3.  **Wording Cautiousness:** Checks for forbidden absolute claims (`validated`, `fully audited`, etc.) and warns or fails the package if they lack evidence citations.
4.  **Trace Mismatch Detection:** Rejects runs where a non-invoked agent matrix vote is detected.
5.  **Hash Verification:** Validates calculated SHA-256 integrity hashes for all generated files.
