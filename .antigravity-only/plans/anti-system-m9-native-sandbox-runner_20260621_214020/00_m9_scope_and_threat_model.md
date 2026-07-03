# Phase M9-A Scope and Threat Model

This document outlines the scope boundary and evaluates security threats against the current `/anti` orchestration runtime.

## 1. Scope and Boundary Definition
The scope of M9-A is limited to architectural design, threat modeling, and POC feasibility research for native sandboxing technologies. No codebase code modifications are executed in this phase.

## 2. Threat Model Matrix

The threat vectors below are classified based on the current process-level Python interception sandbox (`SANDBOX_ENFORCEMENT_PARTIAL`).

| Threat Vector | Classification | Technical Rationale & Mitigation |
| :--- | :--- | :--- |
| **subprocess bypass** | `NOT_BLOCKED` | The Python process wrapper does not block shell/system subprocess calls (e.g. `subprocess.run()`). This is allowed to enable Gradle compilations. |
| **compiled binary bypass** | `NOT_BLOCKED` | Native binary executions (e.g. C/C++ or Kotlin/Native compiled objects) bypass Python's file import/open hooks entirely. |
| **shell command bypass** | `NOT_BLOCKED` | Commands executed directly inside the OS command processor (e.g. Powershell/cmd) bypass the Python sandbox. |
| **IDE/editor filesystem bypass** | `PARTIALLY_BLOCKED` | The sandbox restricts file APIs invoked within the Python runtime, but cannot prevent IDE-level filesystem changes by the host editor. |
| **symlink/path traversal bypass** | `BLOCKED` | Blocked via `normalize_path` and `resolve_real_path` which resolve links and traversal sequences (`..`) to target paths before policy checks. |
| **environment variable leakage** | `NOT_BLOCKED` | Environment variables (such as API keys) are fully accessible to the process. M9-A proposes an environment allowlist to mitigate this. |
| **network access** | `NOT_BLOCKED` | The process-level sandbox does not restrict network sockets, leaving outgoing connections unblocked. |
| **transcript fabrication** | `PARTIALLY_BLOCKED` | Transcripts are written to local files and signed in package integrity logs, but can be fabricated if the runtime process is compromised. |
| **evidence ledger tampering** | `BLOCKED` | Modification of ledgers triggers verification failure since hashes are pinned in `package_integrity.md` and validated during replay. |
| **manifest/source hash drift** | `BLOCKED` | Registry source file hashes are verified statically; manifest mismatches trigger immediate exit codes. |
| **host filesystem overreach** | `PARTIALLY_BLOCKED` | Write permissions are blocked for paths outside `.antigravity-only` and workspace scratch, but read operations are partially allowed. |

## 3. Threat Model Conclusion
The process-level sandbox serves as a robust configuration validation guard, but lacks containment properties. Mitigating the remaining `NOT_BLOCKED` threats requires a transition to OS-level isolation (e.g. containerization).
