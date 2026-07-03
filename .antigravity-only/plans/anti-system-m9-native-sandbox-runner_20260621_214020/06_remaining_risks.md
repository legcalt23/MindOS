# Remaining Risks M9-A Report

This document reviews the residual risks associated with the design and proposed isolation runner.

## 1. Local Tooling Dependencies
* **Risk:** Docker Desktop on Windows requires WSL2 backend and virtualization enabled in BIOS. Developer workstations lacking these setups will fail to run the containerized runner.
* **Mitigation:** Fallback to a non-containerized `LEGACY_COMPAT_MODE` with strict warning logging is supported, but cannot guarantee containment.

## 2. Shared Workspace Volume Caching
* **Risk:** Speeding up Gradle compilations requires caching build directories across container runs. If write permissions are misconfigured on these cache directories, host filesystem leakage could occur.
* **Mitigation:** Strict mount boundaries and ephemeral build copies are used to isolate build caches.

## 3. Threat Classification Capping
* **Risk:** The Python-level process wrapper lacks robust shell-escape blockades.
* **Mitigation:** Retain verdict `SANDBOX_ENFORCEMENT_PARTIAL` and `NO_GO_HIGH_STAKES` strictly. Do not allow autonomous critical reviewer runs until native sandbox enforcement is fully verified in code.
