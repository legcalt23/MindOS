# Sandbox Enforcement M8 Audit

This document details the sandbox enforcer mechanisms and boundaries.

## 1. Sandbox Mechanism
The sandbox works at the process boundary of Python:
* **Interception:** File operations (`open`, `read_text`, `write_text`) are wrapped by `sandbox_enforcer.py`.
* **Policies:** A JSON policy maps allowed directories and denies sensitive files (e.g. `.env`, `.env.local`, `google-services.json`, private keys, keystores).
* **Scope:** Write access is strictly confined to `.antigravity-only/` paths and workspace scratch.

## 2. Enforcement Boundary
* **Status:** `SANDBOX_ENFORCEMENT_PARTIAL`
* **Limitations:** Python process wrapping does not restrict lower-level subprocess escapes (such as running shell commands, executing native binary modules, or calling C library wrappers directly). Subprocess execution remains permitted to support Gradle/Kotlin builds.
* **Security Level:** Defense-in-depth, local execution boundary only. Not verified for untrusted code execution.
