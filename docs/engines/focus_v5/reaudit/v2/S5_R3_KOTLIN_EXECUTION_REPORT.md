# S5_R3_KOTLIN_EXECUTION_REPORT — production-scorer + R3-contract EXECUTION (Part 7)

```
AUTHORIZATION: mandate S5·R3·V2 COMPLETION Part 7 — isolated non-destructive worktree build authorized.
ISOLATION: git worktree --detach @ HEAD (8a06b9a) in a temp dir; --no-daemon; --offline; isolated
           --project-cache-dir; shared ~/.gradle dependency cache only (read-mostly, no build-dir contention with
           the IDE worktree). NO commit/push/merge/wiring. Original IDE worktree untouched.
RESULT CLASS: EXECUTED_PRIMARY (real production Kotlin ran on the JVM/desktop target).
```

This report retires the prior `PRODUCTION_SCORER_PARITY = NOT_RUN` ceiling. The CEO was right to require a real
run: it changed the answer. Gradle is **not** blocked; the earlier "foreign WIP would not compile" hedge is
**refuted by execution** — the live WIP tree compiled clean.

## A · Golden production-scorer parity (COMMITTED code)

| field | value |
|---|---|
| source-set | `shared` · `desktopTest` (JVM) |
| FQ symbol under test | `com.mindos.app.features.mindboost.focus2.domain.scoring.DescriptiveScorer.scoreCells` |
| production source | `DescriptiveScorer.kt` sha256 `9f087c7dd77e5865288fbc003470b6e2632402869f009f4801877844e5113a7d` (+ `Probit.kt` `c74e9586…`, `DescriptiveModel.kt` `d548f655…`) — **TRACKED at HEAD, unmodified** |
| test source | `DescriptiveScorerGoldenReauditTest.kt` sha256 `9dde6e35cee4c437633132c5babfd4e46a30134d6f7535ff7b9d9b9332b2d452` |
| Gradle task | `:shared:desktopTest --tests …DescriptiveScorerGoldenReauditTest --no-daemon --offline --project-cache-dir <tmp>` |
| exit code | **0** (`BUILD SUCCESSFUL in 2m 37s`; task `:shared:desktopTest` **executed**, not up-to-date) |
| JUnit XML | `tests="2" skipped="0" failures="0" errors="0"`; 0 `<failure>`/`<error>`; timestamp `2026-07-22T21:40:14Z` |
| assertions | `assertEquals(1.310847010469, dPrime(30,10,10,30), 1e-6)`; `= 1.680015897775` (30,5,10,30); `= 1.292907666197` (15,5,10,30); + non-monotonic flip `d1 > sFull ≥ d2` |
| tolerance | 1e-6 (Acklam inverse-normal error ~1.15e-9 ≪ bound) |
| failures | none |
| proof it calls PRODUCTION, not a formula copy | the compiler emitted `w: …DescriptiveScorerGoldenReauditTest.kt:36:19 No cast needed` on `(d as Descriptive.Present)` — it compiled the test against the real `DescriptiveScorer` return type |

**The production Kotlin scorer yields exactly the three d′ values and reproduces the Absent→Present flip.** This
upgrades MB-FOCUS-0047 / F-03 from `REIMPLEMENTATION_REPRODUCED` to **`EXECUTED_PRIMARY`**.

## B · R3 contract properties on the real `evaluate()` (UNCOMMITTED Paso-3 WIP)

Ran in a second isolated build: the HEAD worktree with the **live working-tree** `focus2` + `core.timing` source
overlaid (the R3 impl `FocusTemporalEligibility`/`FocusEvidenceStatePolicy` + the untracked `core.timing`
temporal-class layer are Paso-3 WIP, **absent from HEAD**). **The overlaid live WIP tree COMPILED clean**
(warnings only) — refuting the "won't compile" hedge.

| field | value |
|---|---|
| FQ symbols | `…focus2.domain.evidence.FocusTemporalEligibility` (`63e8a494…`) · `FocusEvidenceStatePolicy` (`33bfcf9d…`) — **UNTRACKED Paso-3 WIP** |
| tests | `FocusTemporalEligibilityTest` (`5593a0f9…`) **9/9** · `FocusEvidenceStatePolicyTest` (`40ae7a16…`) **7/7** · golden **2/2** = **18/18** |
| Gradle task | `:shared:desktopTest --tests …evidence.* --tests …DescriptiveScorerGoldenReauditTest --no-daemon --offline` |
| exit / build | **0** · `BUILD SUCCESSFUL in 1m 43s` · `compileTestKotlinDesktop` executed clean |
| XML | eligibility `tests="9" failures="0" errors="0"`; policy `tests="7" failures="0" errors="0"`; 0 failure/error children |

### R3 property coverage (what the 16 green tests actually assert — no overclaim)

| mandate property | covered? | test |
|---|---|---|
| outcome non-interference (d′/outcomes never change eligibility) | ✅ EXECUTED_PRIMARY | `outcomesAndRt_doNotChangeTemporalEligibility_nonInterference` |
| monotonicity under increasing exclusion | ✅ EXECUTED_PRIMARY | `addingExcludedWindows_isMonotone_neverReopensEligibility` |
| denominator (attributed discards count into excludedFraction) | ✅ | `attributedDiscards_countIntoTheExcludedFraction` |
| all-excluded / empty | ✅ | `emptyCells_isNotCaptured_neverEligibleNorExcluded` |
| metadata absent (null capability) | ✅ | `nullCapability_v1Record_isNotCaptured` |
| threshold (majority-excluded backstop) | ✅ | `majorityExcluded_trippsTheSessionBackstop` |
| non-authoritative timebase excluded | ✅ | `fallbackCapability_isExcluded_nonAuthoritativeClass` |
| partial coverage excluded | ✅ | `partialCoverage_isExcluded_coverageIncomplete` |
| reason-code priority (invalidation precedence) | ✅ | `invalidation_winsOverEverything` |
| C0 invariant: official measurement inconstructible for EVERY input | ✅ | `officialEligible_isInconstructible_forEveryInputCombination` |
| RT-shadow gated by modality+native-precision+eligible-temporal | ✅ | `rtShadow_requiresModality_nativePrecision_andEligibleTemporal` |
| ±epsilon boundary sweep | ❌ NOT_RUN | Paso 4 |
| restore/serialization round-trip | ❌ NOT_RUN | `FocusSessionRestore*` exist, not in this filter — Paso 4 |
| lifecycle interruption / process-death | ❌ NOT_RUN | Paso 4 |
| explicit clock-domain-mismatch | ⚠️ partial (fallback/non-authoritative only) | Paso 4 |
| refresh-rate change | ❌ NOT_RUN | Paso 4 |
| full SessionEligibility composition (end-to-end) | ⚠️ partial (`FocusEvidenceStatePolicy` composes temporal+evidence) | Paso 4 |
| NaN/Inf numeric | ⚠️ scorer-side only (golden; loglinear guarantees finite) | Paso 4 |

**Honest scope:** the R3 contract's two *defining* properties (outcome-independence + monotonicity) plus the C0
"no user-facing measurement" invariant are now EXECUTED_PRIMARY green on the production `evaluate()`. The remaining
mandate-listed properties (restore, lifecycle, refresh-rate, explicit clock-mismatch, epsilon sweep) are NOT_RUN
and owned by Paso 4. These 16 tests are Paso-3-authored (I ran them; I did not write them) — so this is "the
existing R3 suite passes on the live code," not "a comprehensive R3 property suite exists."

## C · What this does NOT establish

- Not committed → provenance still `FAIL_UNANCHORED` (F-06). The R3 impl remains UNTRACKED Paso-3 WIP.
- Not wired → `NO_STATIC_WIRING_FOUND` stands (0 external `focus2.*` refs; `focus2Enabled` absent).
- Android/iOS/physical → still `NOT_RUN`/`BLOCKED_IOS`/`NOT_RUN`.
- MNAR (F-04), reliability (F-10), a11y modality-fairness (F-05), revocation (F-01) → design/empirical gaps a
  passing unit test cannot close.

## D · Raw evidence

Sanitized JUnit XMLs: `docs/engines/focus_v5/reaudit/v2/logs/gradle_TEST-*.xml`. Raw build logs (host paths) held
RAW_RESTRICTED in the session scratchpad; command log `logs/command_log.jsonl` (EXECUTED_PRIMARY entries).
