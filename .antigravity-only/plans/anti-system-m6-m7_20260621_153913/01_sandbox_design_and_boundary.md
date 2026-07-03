# Sandbox Design and Boundary

This document outlines the programmatic sandbox design and enforcement boundary implemented in Phase M6–M7.

## 1. Sandbox Design Heuristics
The sandbox enforcer core (`sandbox_enforcer.py`) acts at all `/anti` file-access boundaries to protect against unauthorized reads, writes, path traversals, and key exposures:
- **Canonical Real Path Resolution:** Resolves all symlinks, junctions, and relative segments (`..`) using `os.path.realpath` before applying policy decisions.
- **Environment Variable Expansion:** Expands all environment-variable-injected paths using `os.path.expandvars` to prevent escape attempts via `%SECRET_PATH%` or `$SECRET_PATH`.
- **Windows Case Normalization:** Uses `os.path.normcase` as the final step of resolution, forcing all paths to lowercase on Windows systems to block case-variant bypasses (e.g., `.ENV` or `Google-Services.JSON`).
- **Denied Regex Patterns:** Enforces case-insensitive regex checks for high-stakes secrets and config files (like `.env`, `.env.*`, `*secret*`, `*credential*`, `*.pem`, `*.key`, `*.p12`, `*.keystore`, `google-services.json`, etc.).
- **Strict Write Boundaries:** Denies all file writes/deletes outside the approved package root (`.antigravity-only/` and the root report file) or allowed scratch directories.

---

## 2. Sandbox Enforcement Boundary
> [!WARNING]
> **Defensive Limit:** In-process Python interception is defense-in-depth only. It is not a complete sandbox. It cannot prevent subagents from calling native compiled binaries or bypasses if invoked outside of Python's wrapper scope.

Since direct sandbox enforcement cannot be wired into a compiled or IDE-owned native runtime layer, we declare a partial boundary:

```text
PARTIAL_ENFORCEMENT_BOUNDARY
```

As a result, the final status for the system under this boundary is capped at `GO_INTERNAL_VERIFIED` for controlled internal `/anti` review execution, and is not approved for unsupervised high-stakes operations.
