# S5_R3_MULTIDIMENSIONAL_VERDICTS — Focus V5 · S5 · R3 (reaudit 2026-07-22)

```
REVIEW_CLASS: AI_SELF_REAUDIT_WITH_CONTEXT_FRESH_LENSES + CROSS_MODEL_ADVERSARIAL_INPUT_RECEIVED (V2) — HUMAN_EXTERNAL_AUDIT: NOT_OBTAINED
HEAD described: 8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5 · package UNTRACKED (working-tree)
Recommended decision: R3 REMAINS CANDIDATA — do NOT freeze / mount / declare parity. See CEO memo.
```

States: `PASS_WITH_PRIMARY_EVIDENCE` · `PASS_WITH_LIMITATIONS` · `FAIL` · `BLOCKED` · `NOT_RUN` · `NOT_APPLICABLE`.
Each carries its own ceiling + evidence. **They are deliberately NOT collapsed into one verdict.**

| Verdict axis | State | Ceiling / meaning | Evidence |
|---|---|---|---|
| **R2_FALSIFICATION_ARITHMETIC** | `PASS` · `EXECUTED_HEAD_SELECTED_GOLDENS` (V2 reclassified — was overclaimed EXECUTED_PRIMARY/"parity") | The Absent→Present-under-more-exclusion flip is real on the HEAD-tracked scorer. **2 golden vectors, NOT global parity** (no independent spec, no coverage measure). | **Isolated Gradle run, exit 0:** `DescriptiveScorerGoldenReauditTest` 2/2 green pins `DescriptiveScorer.kt` (**TRACKED@HEAD, unmodified** → HEAD-selected-goldens) to S_full 1.310847010469 / D1 1.680015897775→Absent / D2 1.292907666197→Present + non-monotonic flip. Harness output-digest `2df300f3…`, **1363/1363 WITHIN_ENUMERATED_DOMAIN**, 3 probits agree 1.3e-9. NOT parity: 2 vectors ≠ a validated spec. |
| **R3_PRODUCTION_PROPERTIES** | `PASS` · `EXECUTED_UNCOMMITTED_CANDIDATE_OVERLAY` (V2 reclassified — was overclaimed "production evaluate()") | Contract properties hold on the **UNTRACKED candidate** `evaluate()`; NOT production, NOT committed. Full focus2 suite green ≠ scientific validity. | **Isolated overlay build, exit 0 (2026-07-22T22:41–22:45Z, tree digest `c16f9e0c`):** the **FULL focus2 suite = 27 classes / 214 tests / 0 fail / 0 err / 0 skip** (`--rerun-tasks`, 11 tasks executed), correcting the prior cherry-pick of 2 classes. Includes the previously-mislabeled-"Paso 4" tests **now actually run**: `FocusSessionRestoreTest`, `FocusSessionLifecycleTest`, `FocusTemporalRuntimeTest`, `FocusTemporalSchemaCompatTest`, `FocusEventReconciliationTest`. HONEST CEILING: `FocusTemporalEligibility.kt`/`FocusEvidenceStatePolicy.kt` are **UNTRACKED working-tree files** → candidate-overlay, not committed/wired/production; 214 green = code-matches-its-own-tests (internal consistency), **not** validity/measurement-readiness/parity. Still NOT_RUN (tests DO NOT EXIST yet): threshold±epsilon, ULP, NaN/±Inf, concurrent sessions, account-switch/multi-account wipe, process-death, refresh-rate 60/90/120/144/VRR, explicit clock-domain-mismatch, ms-vs-nanos. Android=NOT_RUN, iOS=BLOCKED, physical=NOT_RUN. Evidence: `reaudit/v2/logs/focus2_full_suite_result.json`. |
| **R2_FALSIFICATION_REACHABILITY** | `NOT_APPLICABLE` (design-only) | The gate was NEVER a code path — the counterexample refutes a *design*. D1/D2 are valid count configs; the gate predicate is not. | grep `*.kt` `monotonicGuard\|mg-1\|S_full\|Sfull` = **0 hits**; no d′-recompute-under-exclusion in focus2. `VERIFIED_PRIMARY`. |
| **R3_CONTRACT_SCIENTIFIC_VALIDITY** | `FAIL` | Non-interference + coverage-monotonicity PASS at contract level, but the contract defines **NO missingness/MNAR policy** and retains a low-reliability metric. | Harness r3 checks pass (0/0). BUT F-L2-02 (no MNAR policy), F-L2-03 (orphan `differentialExclusionDelta`, R2's within-session detector dropped), L1-R3-01 (d′ inherits reliability paradox, no SEM/test-retest gate). |
| **R3_DOCUMENTATION_AND_PROVENANCE** | `FAIL_UNANCHORED` (V2) | Review-class, hashes, closure, canonicalization, scanner: **corrected**. But a self-contained manifest proves integrity BETWEEN members, NOT authenticity — the root is not anchored outside the package (no signed commit/CI artifact), and the chain is UNTRACKED. Per mandate §9 this is FAIL, not PASS_WITH_LIMITATIONS. | Full-SHA manifest (`cb56ec58…`) + canonicalization declared + registry-driven scanner (node:test 19/19) + cross-artifact validator. Anchor = CEO commit decision (MB-FOCUS-0050). |
| **R3_IMPLEMENTATION_STATUS** | `BLOCKED` / `DESIGNED_NOT_RUN` | Everything is `CONTRACT_INERT_NO_WIRING`; W1–W5 + MB-FOCUS-0047/0048 + new findings open. | 0 external `focus2.*` refs; `focus2Enabled` = 0 hits; L3-01 seal-metadata not populated by the only prod seal caller; LENS4-02 drain discards typed outcome. |
| **ANDROID_COMPILE_UNIT_SIM** | `NOT_RUN` (this reaudit) | Prior slices reported PASS at INTERNAL_TECHNICAL_TRACER; **not re-executed here** (no Gradle). | Reaudit is doc/evidence-only; scorer fidelity `STATICALLY_VERIFIED`. Not independently re-run. |
| **ANDROID_MANUAL_DEVICE** | `NOT_RUN` | No device exercise. | S6 design only. |
| **IOS_COMPILE_UNIT_SIM** | `BLOCKED` (`BLOCKED_IOS`) | No macOS host. | environment. |
| **IOS_MANUAL_DEVICE** | `NOT_RUN` / `BLOCKED` | — | environment. |
| **CROSS_PLATFORM_EQUIVALENCE** | `BLOCKED` | Cannot claim parity: iOS blocked, physical not run, Hz-detection asymmetric. | L3-04 (Android Hz detection weaker than iOS); no equating/`CalibrationBlueprint` (W5). |
| **PRIVACY_DATA_LIFECYCLE** | `FAIL` | Revocation gap untested; cross-account wipe pattern reintroduced; disability-proxy DPIA + minors open. | L5-01 (GAP-1 no regression test), L5-03 (GAP-2), L5-05 (DPIA screenReader/switch proxies), L5-06 (no minors escalation). |
| **ACCESSIBILITY_MEASUREMENT_EQUIVALENCE** | `FAIL` | Accuracy is NOT modality-fair as claimed. | **L7-01 VERIFIED**: `FocusTracerScreen.kt:137` screen-reader announces the correct answer + congruency; L7-02 fixed 1200 ms window not modality-adjusted; L7-04 no Dynamic-Type/overflow safety. |
| **CLAIMS_LEVEL** | `PASS_WITH_LIMITATIONS` | C0 descriptive is the ceiling and holds in live copy; the "no-capacity" half has no mechanical enforcement. | L1 efficacy: anti-capacity rule + far-transfer lexicon hold; L1-R3-04: copy scanner does not enforce the no-subscore/no-capacity half. |
| **S5_EXIT_READINESS** | `FAIL` | Not ready to freeze R3. | 5/7 lenses FAIL; MNAR undefined; a11y/privacy FAIL; W1–W5 open. |
| **LAUNCH_READINESS** | `FAIL` | Not launchable, not alpha, not parity. | inert + iOS blocked + no efficacy evidence (C0) + privacy/a11y FAIL. |

## Reading

- The **science of the falsification** is the strongest result (primary evidence). It **dominates** and justifies
  the reopening — that has not weakened.
- The **R3 replacement** is directionally right (outcome-independent, monotone) but **scientifically incomplete**
  (no missingness/MNAR policy, reliability of d′ unquantified) → `R3_CONTRACT_SCIENTIFIC_VALIDITY = FAIL`, not a
  clearance.
- Everything downstream (implementation, platform, privacy, a11y, launch) is `BLOCKED`/`FAIL`/`NOT_RUN`. No axis
  supports promotion beyond **R3 CANDIDATA**.
