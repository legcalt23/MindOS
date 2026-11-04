# S5_R3_ELIGIBILITY_CONTRACT_SPEC — testable extraction + edge-case matrix

```
ROLE: testable extraction of the R3 temporal-eligibility contract (NOT the normative source)
NORMATIVE SOURCE: docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md §R3-1..R3-22 (D-R3-01..04)
CODE OF RECORD: shared/src/commonMain/.../focus2/domain/evidence/FocusTemporalEligibility.kt (SHADOW, UNMOUNTED)
REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW
```

> This file does not re-legislate the contract; the ADR does. It states the contract as **testable
> properties** and an **edge-case matrix**, and maps each to the reaudit harness so a third party can
> re-run the checks. Where a property is `DESIGNED_NOT_RUN` in code, that is said plainly.

## 1. The contract, as properties (D-R3-01, §R3-1)

Eligibility is `E = g(τ)` where `τ` is **temporal-capture metadata only** (frame-observation class,
coverage, excluded fraction) — never an outcome, RT, d′, criterion, CI, score, or future progress.

| # | Property | Normative | Code | Reaudit check | Result |
|---|---|---|---|---|---|
| P1 | **Outcome-independence (non-interference).** Holding τ fixed and varying outcomes/RT arbitrarily, `E` does not change. | D-R3-01, §R3-1 | `FocusTemporalEligibility.evaluate` reads `snapshot.temporalCells` buckets + class; never reads d′/verdict | harness `r3_contract_checks.non_interference_violations` | **0 / 63** `STATICALLY_VERIFIED`+`REPRODUCED` |
| P2 | **Coverage-monotonicity.** Adding technically-invalid windows never moves `Excluded/Indeterminate → Eligible`. | §R3-1 (Lema 2, one-sided adders) | one-sided ceiling `excludedFraction > sessionExcludedMax → demote`; adders only ADD causes | harness `r3_contract_checks.monotonicity_violations` | **0** `REPRODUCED` |
| P3 | **Typed absence (third state).** Probe-less / v1 record → `NotCaptured`, never a fabricated 0 (min) or 1 (max). | §R3-1 | `evaluate(...)`: `probeCapability==null \|\| cells.isEmpty()` → `NotCaptured`; `FULLY_EXCLUDED=1.0` fail-closed | read `FocusTemporalEligibility.kt:54-62,101` | `STATICALLY_VERIFIED` |
| P4 | **Official ceiling = PROVISIONAL.** `OFFICIAL_ELIGIBLE` is inconstructible; nothing RT-critical enters official evidence in v1. | D-R3-02 | `FocusEvidenceStatePolicy.evidenceOf` never returns `OFFICIAL_ELIGIBLE` | read `FocusEvidenceStatePolicy.kt:42-48` | `STATICALLY_VERIFIED` |
| P5 | **NotCaptured does not demote accuracy.** Absent temporal capture triggers no backstop. | §R3-13 | `NO_BACKSTOP=0.0` only in the accuracy path; demotion only via excluded-fraction backstop | read `FocusEvidenceStatePolicy.kt:45,60` | `STATICALLY_VERIFIED` |

**The R2 gate these replace is design-only.** grep `*.kt` for `monotonicGuard\|mg-1\|S_full\|Sfull` = 0 hits;
no d′-recomputation-under-exclusion in focus2 code. The falsified predicate (`S_adm ≤ S_full` over nested
exclusion) never became a code path (harness `reachability_note`).

## 2. Edge-case matrix (expected `E` / evidence state)

| Case | τ input | Expected E | Expected evidence | Basis |
|---|---|---|---|---|
| Probe-less platform (desktop/iOS-no-nanos) | capability null | `NotCaptured` | PROVISIONAL (accuracy still trains) | P3, P5 |
| v1 record (empty temporal cells) | cells empty | `NotCaptured` | PROVISIONAL | P3 |
| Full authoritative capture, low exclusion | class T1, coverage complete, exclFrac ≤ max | `Eligible` (shadow) | PROVISIONAL (never official) | P1, P4 |
| Same capture, high exclusion | exclFrac > sessionExcludedMax | `Excluded(SESSION_BACKSTOP_EXCEEDED)` | TRAINING_ONLY | P2 |
| Non-authoritative class (T0) | class ≠ T1_FRAME_OBSERVED | `Excluded(NON_AUTHORITATIVE_CLASS)` | PROVISIONAL (accuracy) / RT shadow blocked | §R3 |
| Coverage incomplete | windowsClassified ≠ resolvedTrials | `Excluded(COVERAGE_INCOMPLETE)` | per backstop | §R3 |
| Underivable excluded fraction | denominator 0 | fail-closed (`FULLY_EXCLUDED=1.0`) → cannot pass ceiling | TRAINING_ONLY | P3 |
| Scorer invalidated (degenerate/no-discrimination) | any τ | n/a (accuracy axis) | INVALIDATED | `FocusEvidenceStatePolicy.kt:43` |

## 3. Open questions the contract does NOT close (carry to Paso 4/empirical)

- **MNAR of `excludedFraction` itself** (§3.3 of the mandate). Outcome-independence (P1) removes a *direct*
  dependence on d′, but technically-invalid capture may still correlate with device tier, thermal state,
  fatigue, or accessibility modality → missing-not-at-random. `HYPOTHESIS_TO_TEST`; owner = Paso 4 shadow
  simulations + Paso 6 device matrix. This spec asserts P1–P5 only, NOT absence of MNAR.
- **Wiring.** Everything above is `SHADOW`/`UNMOUNTED`. `evaluate(...)` has no production call-site
  (`CONTRACT_INERT_NO_WIRING`, verified: 0 external `focus2.*` refs). Paso 4 "wires the candidate".
- **Preregistered thresholds/tertiles/versions/migration** frozen before results: `TemporalQualityThresholds`
  carries a `version`; the numeric ceilings' preregistration is `DESIGNED_NOT_RUN` (owner: Paso 4).

## 4. How to re-run

```bash
node docs/engines/focus_v5/reaudit/r2_falsification_harness.mjs
```

`r3_contract_checks.pass == true` demonstrates P1 (non-interference) and P2 (monotonicity) at the
contract level. P3–P5 are `STATICALLY_VERIFIED` by reading the two cited files; a Kotlin test executing
them is `NOT_RUN` here (no Gradle in this reaudit — hand to `:shared:desktopTest`).
