# Filesystem Isolation Results

This document details filesystem isolation tests performed during the Phase M9-B POC.

## 1. Path Constraints
Since the fallback path `LOCAL_EPHEMERAL_WORKSPACE_ONLY` is active, the maximum sandbox status is `NOT_NATIVE_SANDBOX`.

## 2. Write Limits
The runner writes only under the approved `scratch/` and `.antigravity-only/` directories. Attempting to write outside these paths triggers a sandbox enforcement error.
