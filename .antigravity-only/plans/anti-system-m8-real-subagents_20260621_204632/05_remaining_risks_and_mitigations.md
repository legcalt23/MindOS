# Remaining Risks & Mitigations M8 Report

This document reports the remaining system risks and mitigations.

## 1. Process-Level Sandbox Limitations
* **Risk:** Python-level wraps do not block subprocess escapes (e.g. `subprocess.run()`, shell operations, direct DLL/C library file calls).
* **Mitigation:** Subprocess execution is allowed purely to support Gradle/KMP compilation. Process-level wrapper serves as a defense-in-depth boundary for standard file API operations.

## 2. Hardcoded Source Hashes
* **Risk:** Hashes of `03_agent_specialization_surgery.md` and `11_final_upgraded_founder_council_plan.md` are hardcoded in the registry script. If those documents are changed, registry validation will trigger a checksum mismatch.
* **Mitigation:** Change management process requires updating `anti_agent_registry.py` hashes whenever the underlying surgery documents are updated.

## 3. High-Stakes Autonomous Operation
* **Risk:** System is not yet qualified for fully autonomous or unsupervised critical decisions.
* **Mitigation:** Retain verdict `NO_GO_HIGH_STAKES` strictly. Ensure all operational uses are under local human pair-programming verification.
