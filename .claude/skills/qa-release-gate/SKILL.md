---
name: qa-release-gate
description: Use before approving a Mindboost feature, release, migration, new exercise, assessment, AI coach behavior or analytics change.
---

# QA Release Gate

Return one verdict:

- APPROVE
- REVISE
- BLOCK

Block if:

- unsupported medical/clinical/IQ claim
- no clear cognitive target
- no measurable scoring
- no tests for important behavior
- unnecessary sensitive cognitive data collection
- inaccessible critical flow
- confusing or manipulative UX
- migration/release risk has no rollback path

Output:

1. Verdict
2. Critical issues
3. Medium issues
4. Minor issues
5. Missing tests
6. Safety concerns
7. Required fixes
8. Approval criteria
