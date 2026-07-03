# Sandbox Test Results

This document records the results of the sandbox enforcement test suite.

## 1. Test Command & Execution
- **Command:** `python .antigravity-only/scripts/test_sandbox_enforcement.py`
- **Result:** `PASS`
- **Exit Code:** `0`

---

## 2. Test Execution Details

| Test Case | Description | Status | Verification Evidence |
|---|---|---|---|
| **test_01** | Reading `.env` is blocked. | `PASS` | Raised `AntiSandboxViolation` |
| **test_02** | Reading `.env.local` is blocked. | `PASS` | Raised `AntiSandboxViolation` |
| **test_03** | Reading `google-services.json` is blocked. | `PASS` | Raised `AntiSandboxViolation` |
| **test_04** | Reading `*.pem` is blocked. | `PASS` | Raised `AntiSandboxViolation` |
| **test_05** | Reading `*.key` is blocked. | `PASS` | Raised `AntiSandboxViolation` |
| **test_06** | Relative traversal to denied path is blocked. | `PASS` | `../../.env` resolved and blocked |
| **test_07** | Case-variant denied path is blocked on Windows. | `PASS` | `.ENV` and `GOOGLE-SERVICES.JSON` blocked |
| **test_08** | Symlink/junction to denied path is blocked. | `PASS` | Resolved target and blocked traversal |
| **test_09** | Writing outside package root is blocked. | `PASS` | Blocked write to `build.gradle.kts` |
| **test_10** | Allowed package read succeeds. | `PASS` | Reading `.antigravity-only/VERSION.md` allowed |
| **test_11** | Allowed package write succeeds. | `PASS` | Writing to `.antigravity-only/plans/` allowed |
| **test_12** | Violation error message is sanitized. | `PASS` | Message contains `SECURITY_BLOCKED` and hides paths |
