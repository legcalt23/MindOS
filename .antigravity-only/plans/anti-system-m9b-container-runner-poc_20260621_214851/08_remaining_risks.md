# Remaining Risks and Mitigations

This document lists the security, operational, and containment risks identified in Phase M9-B.

## 1. Subprocess Interception Limits
The `local_ephemeral` fallback backend runs code on the host process and does not enforce OS-level boundary containment.

## 2. Risk Mitigation Actions
Strictly cap execution status as `NOT_NATIVE_SANDBOX` and deny high-stakes autonomous execution (NO_GO_HIGH_STAKES).
