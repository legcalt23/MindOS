# S5_R3_REAUDIT_V2_MASTER_REPORT — cross-model corrective reaudit

```
TASK: TASK-2026-07-22-FOCUS-V5-S5-R3-REAUDIT-V2
REVIEW_CLASS: AI_SELF_REAUDIT_WITH_CONTEXT_FRESH_LENSES + CROSS_MODEL_ADVERSARIAL_INPUT_RECEIVED
HUMAN_EXTERNAL_AUDIT: NOT_OBTAINED
HEAD described: 8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5 · package UNTRACKED · PROVENANCE: FAIL_UNANCHORED
RECOMMENDED DECISION: R3 REMAINS CANDIDATA / REOPENED — not approved, frozen, mounted, or parity.
```

## 1. Claims matrix — every material V1 claim, verified or refuted

Classes: `EXECUTED_PRIMARY` (real production code ran) · `STATIC_SOURCE_VERIFIED` · `REIMPLEMENTATION_REPRODUCED`
(Node reimpl of Kotlin) · `DOCUMENT_ASSERTION_ONLY` · `NOT_RUN` · `BLOCKED`.

| # | V1 claim | Verdict | Evidence / correction |
|---|---|---|---|
| 1 | "16 nuevos artefactos" | PARTIALLY_CONFIRMED | Imprecise (mixed tooling + evidence). Replaced by an exact manifest: **27 members** now enumerated. |
| 2 | "manifest 19/19" | CONFIRMED (at V1) | `EXECUTED_PRIMARY`. Now 27/27 with V2 additions. |
| 3 | which modified files in/out | CONFIRMED | `reaudit/v2/phase0_inventory.json`: 27 INCLUDE, 2 EXCLUDE (S5_R4_* — absolute usernames). |
| 4 | "all targets are UNTRACKED" | **REFUTED (overstatement)** | 3 members are TRACKED (ledger, `ADR_REGISTER_V5.md`, `validate-audit-ledger.mjs`). The R3 authority *docs* are untracked; not "all targets". |
| 5 | index "tracked/sealed" vs "UNTRACKED" | **CONFIRMED CONTRADICTION** | V1 scanner comment said the index "is tracked/sealed and cannot be edited" — false: it is UNTRACKED and WAS edited. Comment removed in the V2 registry-driven scanner. |
| 6 | "R3 checks 0/63" vs "0/0" | CLARIFIED | Denominators explicit: non-interference **0 violations / 63 checks**; monotonicity **0 violations** over a 0→1 sweep (101 steps). Contract-level only. |
| 7 | "0047..0055 OPEN" vs blockers listing only 0047/0048 | **CONFIRMED GAP** | Blockers matrix now lists all findings 0047–0057; enforced by `validate_cross_artifact.mjs` (OPEN_NOT_IN_BLOCKERS). |
| 8 | MB-FOCUS-0050b/0051b/0053b exist | **REFUTED** | Never existed as ledger rows — bogus `b` suffixes in V1 findings JSON. findings_v2 maps to real IDs; `LEDGER_B_SUFFIX` + `LEDGER_ID_DANGLING` checks added. New real IDs 0056/0057 created. |
| 9 | "history preserved" | PARTIALLY_REFUTED | For untracked files there is no git history. Pre-V1 bytes of handoff/index/capsule = `PREIMAGE_IRRECOVERABLE` (transcript only). V2 saves preimages (`reaudit/v2/preimages/`). |
| 10 | "all Phase E gates green" | CLARIFIED | Restated as **ALL EXECUTED GATES GREEN** + explicit NOT_RUN/BLOCKED list (§3). |
| 11 | "no product code touched" | CONFIRMED | `EXECUTED_PRIMARY` scope diff: product `.kt` untouched; V2 added ONE desktopTest file. |
| 12 | "R2 gate not in code" | CONFIRMED | `STATIC_SOURCE_VERIFIED`: grep `*.kt` = 0 hits; semantic check — no score-vs-score exclusion-rescoring in focus2. |
| 13 | "current code implements R3" | CONFIRMED | `STATIC_SOURCE_VERIFIED`: `FocusTemporalEligibility` = g(excludedFraction), one-sided ceiling. |
| 14 | "0 external focus2 refs" | CONFIRMED | `STATIC_SOURCE_VERIFIED`. |
| 15 | "1363/1363 feasible bases" | CONFIRMED **but scope-bounded** | Domain = {hits 20..60 step5, misses 5..30 step5, fa 5..30 step5, cr 20..60 step10, filtered CI_lower(S_full)>0}. NOT a universal claim; the property is that ANY base with excludable misses can be driven Absent then Present. |
| 16 | "three independent probits, corroborated independently" | **RECLASSIFIED** | Three algorithms in the SAME harness cross-check transcription (agree 1.3e-9). Honest class = `REIMPLEMENTATION_REPRODUCED`, NOT "corroborated independently" and NOT primary. Production parity = the Kotlin golden (NOT_RUN). |
| 17 | "R3 non-interference & monotonicity pass" | CONFIRMED at CONTRACT level | Harness models the contract shape, not the shipped `evaluate()` (lens F-L2-04). Kotlin property tests = **NOT_RUN**. |
| 18 | "security review corrected" | CONFIRMED (as downgrade) | Downgraded to `DOCUMENTATION_SECURITY_REVIEW_ONLY`; underlying GAP-1/GAP-2 remain OPEN (MB-FOCUS-0052). |
| 19 | "package provenance corrected" | PARTIALLY | Review-class/hashes/closure corrected; but **PROVENANCE = FAIL_UNANCHORED** (self-contained manifest ≠ authenticity). |
| 20 | "accuracy modality-fair" refuted | CONFIRMED (refutation stands) | `STATIC_SOURCE_VERIFIED` `FocusTracerScreen.kt:135-138` announces the correct answer + congruency. |

## 2. What was corrected (executable, verified)

- **Authority scanner REDESIGNED** (`scripts/validate-authority-entrypoints.mjs` + `.test.mjs` + `authority-registry.json`): registry-driven, sentinel-delimited authority block (a marker quoted "obsolete" or in prose no longer counts), 16 structured error codes, JSON output, import-safe. **`node:test` 19/19**, the mandate's 4-part contraejemplo now FAILS, mutation adequacy proves every rule is load-bearing.
- **Cross-artifact validator** (`reaudit/v2/validate_cross_artifact.mjs`): findings↔ledger↔blockers coherence, dangling/`b`-suffix ledger IDs, evidence-path existence, cross-doc state agreement, manifest canonicalization. GREEN.
- **Findings-V2 schema** (`reaudit/v2/findings_v2.json`): P0 decomposed into `impact_severity` (CRITICAL…) / `current_reachability` (INERT_NOT_WIRED…) / `launch_priority` (BLOCKER_BEFORE_MOUNT…), so a defect can be CRITICAL yet inert.
- **Evidence reclassified** to closed classes; **provenance → FAIL_UNANCHORED** with a declared canonicalization algorithm.
- **Ledger**: MB-FOCUS-0056 (reliability UNKNOWN_UNTIL_MEASURED) + 0057 (population/minors) added; 0047/0055 corrected; resealed (130 findings, `1df990ce…`).
- **Kotlin production-scorer golden test** written (`DescriptiveScorerGoldenReauditTest.kt`).

## 3. Executed gates vs NOT_RUN/BLOCKED

**ALL EXECUTED GATES GREEN:** ledger validator (130 findings, `1df990ce…`) · scanner `node:test` 19/19 · scanner real PASS · cross-artifact validator OK · R2 harness (digest `2df300f3…`, 1363/1363, `REIMPLEMENTATION_REPRODUCED`) · manifest 27/27 (`cb56ec58…`) · scope diff (no product code) · secret/PII scan clean.

**NOT_RUN / BLOCKED (never substituted by intention):**
- `PRODUCTION_SCORER_EXECUTION` (Kotlin golden) = **NOT_RUN** — Gradle blocked by repo rule (IDE daemon) + foreign focus2 WIP in the tree that would fail compilation for reasons outside this task.
- R3 Kotlin property tests = **NOT_RUN** (same).
- Android compile/unit, iOS compile/sim/device, physical device matrix = **BLOCKED/NOT_RUN**.
- External provenance anchor (signed commit/CI) = **NOT_DONE** (commit not authorized).

## 4. Honestly NOT fully delivered in this pass (owners named)

- **Phase 2.1** property-search hardening (negative controls, property-based feasibility enumeration, seed manifest) — the harness reproduces + broad-searches but is not yet a full property-based generator. Owner: psychometrician.
- **Phase 2.4** Kotlin R3 property tests (non-interference/monotonicity/edge cases on the real `evaluate()`) — written spec only, code+run NOT_DONE. Owner: Paso 4.
- **Phase 5/6/7 deep protocols** (preregistration plan, a11y construct-equivalence full design, privacy data-map with legal questions) — captured as findings (0053/0056/0051/0057) + the eligibility spec + data-flow doc; full standalone protocols are follow-ups. Owner: psychometrician / a11y / privacy+DPO.
- **Full deterministic integrator rerun with 7/7 lenses** — the V1 integrator was under-fed (24 KB → 2/7); V2 does the cross-lens integration by hand (this report + findings_v2). A fresh 7/7 integrator agent run = NOT_DONE this pass.

## 5. Verdict

The R2 falsification survives and is correctly scoped. Every downstream axis (implementation, privacy, a11y, platform,
provenance) is FAIL/BLOCKED/NOT_RUN. **R3 stays CANDIDATA.** A human external reviewer would review evidence, not
substitute it: the missing MNAR policy, unmeasured reliability, and unanchored provenance are not closable by review.
