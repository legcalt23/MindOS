# S5_R3_LENS_COVERAGE — deterministic disposition of all 39 raw lens findings (Part 5)

```
SOURCE (raw): docs/engines/focus_v5/reaudit/S5_R3_LENS_OUTPUTS.json — 7 lenses, 39 findings.
INTEGRATION CLASS: COMPLETE_NON_TRUNCATED_INTEGRATION (deterministic coverage map).
NOT a "deterministic LLM integrator run" — the V1 integrator was under-fed (24 KB → 2/7 lenses). This map is the
DETERMINISTIC half the mandate requires: every raw lens finding gets EXACTLY ONE disposition. A fresh 7-agent LLM
re-integration was NOT re-executed this pass (owner: a future integrator invocation) and would be
COMPLETE_NON_TRUNCATED, never "deterministic", because an LLM step is not reproducible bit-for-bit.
```

Disposition legend: **ESCALATED**→canonical F-id · **FOLDED**→into an F-id (risk conserved) · **MITIGATED**→addressed
by V2 tooling/execution · **NOT_ESCALATED**→reviewed, sub-threshold/inert, reason given. Coverage: **39/39, unmapped = 0.**

## LENS 1 — science / claims / efficacy (4)
| raw | sev | disposition | target / reason |
|---|---|---|---|
| L1-R3-01 | P2 | ESCALATED | F-10 (MB-FOCUS-0056) d′ reliability UNKNOWN_UNTIL_MEASURED |
| L1-R3-02 | P2 | ESCALATED | F-11 (MB-FOCUS-0057) population/minors |
| L1-R3-03 | P3 | NOT_ESCALATED | PROVISIONAL-emission vs DoD wording nit; the C0 "no official measurement" half is now mechanically enforced (see L1-R3-04) → doc-consistency only |
| L1-R3-04 | P3 | MITIGATED (partial) | "no official measurement" half now EXECUTED_PRIMARY: `officialEligible_isInconstructible_forEveryInputCombination` (7/7 green). The "no capacity claim in COPY" half remains copy-scanner-unenforced → tracked under F-05/claims |

## LENS 2 — psychometrics / missingness (6)
| raw | sev | disposition | target / reason |
|---|---|---|---|
| F-L2-01 | P1 | FOLDED | F-04 (MB-FOCUS-0053) — underivable `excludedFraction` fail-open is part of the missing MNAR/backstop policy; note the majority-excluded backstop DOES exist (`majorityExcluded_trippsTheSessionBackstop`, EXECUTED_PRIMARY) |
| F-L2-02 | P1 | ESCALATED | F-04 (MB-FOCUS-0053) MNAR policy — PRIMARY |
| F-L2-03 | P1 | ESCALATED | F-09 (MB-FOCUS-0054) orphan `differentialExclusionDelta` |
| F-L2-04 | P2 | MITIGATED | "harness proves a toy, not shipped code" → now EXECUTED_PRIMARY on the real `FocusTemporalEligibility` (non-interference + monotonicity green, `S5_R3_KOTLIN_EXECUTION_REPORT.md`) |
| F-L2-05 | P2 | FOLDED | F-04 — `sessionExcludedMax=0.50` admitting ~49%-compromised sessions is a threshold-policy choice inside the missingness policy |
| F-L2-06 | P3 | NOT_ESCALATED | COVERAGE_INCOMPLETE internal-identity check adequacy; minor, Paso-4 refinement |

## LENS 3 — timing / input / KMP / parity (5)
| raw | sev | disposition | target / reason |
|---|---|---|---|
| L3-01 | P1 | ESCALATED | F-08 (MB-FOCUS-0048) seal metadata unpopulated |
| L3-02 | P2 | NOT_ESCALATED | main-thread-confinement assumed contract; wiring assumption, Paso 4 |
| L3-03 | P2 | FOLDED | F-05/F-08 — switch-sequential UI↔session glue (a11y + wiring) |
| L3-04 | P3 | NOT_ESCALATED | VRR/LTPO Hz asymmetry; already disclosed, limits CROSS_PLATFORM_EQUIVALENCE (verdicts doc) |
| L3-05 | P3 | NOT_ESCALATED | process-death restore built, unwired; inert, Paso 4 |

## LENS 4 — architecture / state / recovery / concurrency (5)
| raw | sev | disposition | target / reason |
|---|---|---|---|
| LENS4-01 | P0 | ESCALATED | F-02 (MB-FOCUS-0035) single-instance not enforced |
| LENS4-02 | P1 | ESCALATED | F-08 (MB-FOCUS-0048) drain discards typed outcome |
| LENS4-03 | P1 | FOLDED | F-08 + F-01 — cross-account collateral-wipe pattern (namespace isolation + privacy) |
| LENS4-04 | P2 | NOT_ESCALATED | `toGlobalStatus()` bridge; wiring, Paso 4 |
| LENS4-05 | P3 | NOT_ESCALATED | kill-switch has no code = consistent with CONTRACT_INERT (no flag exists) |

## LENS 5 — privacy / security / data-lifecycle (6)
| raw | sev | disposition | target / reason |
|---|---|---|---|
| L5-01 | P0 | ESCALATED | F-01 (MB-FOCUS-0052) revocation gap + no test |
| L5-02 | P0 | ESCALATED | F-01 (MB-FOCUS-0052) "security APTO" unsupported |
| L5-03 | P1 | FOLDED | F-08 + F-01 — GAP-2 cross-account wipe |
| L5-04 | P2 | NOT_ESCALATED | TRIAL_JOURNAL consent gate zero call-sites; inert/unwired, Paso 4 |
| L5-05 | P2 | ESCALATED (CEO/DPO) | CEO memo decision #3 + `S5_R3_DATAFLOW_THREATMODEL.md` — DPIA for disability-proxy signals |
| L5-06 | P3 | ESCALATED | F-11 (MB-FOCUS-0057) minors escalation |

## LENS 6 — provenance / authority / completeness (8)
| raw | sev | disposition | target / reason |
|---|---|---|---|
| L6-01 | P1 | ESCALATED | F-06 (MB-FOCUS-0050) authority chain UNTRACKED |
| L6-02 | P1 | ESCALATED | F-07 (MB-FOCUS-0049) review-class mislabel |
| L6-03 | P1 | FOLDED | F-06 — package-closure conflicting rules (manifest canonicalization declared) |
| L6-04 | P2 | FOLDED | F-06 — ledger out-of-band seal (now §8 self-excluding convention, in-band) |
| L6-05 | P2 | FOLDED | F-06 — truncated 16-hex digests (now full-SHA manifest) |
| L6-06 | P2 | ESCALATED | F-12 (MB-FOCUS-0055) GATE_CATALOG stale self-authority |
| L6-07 | P2 | MITIGATED | scanner "3-string" inadequacy → registry-driven redesign, `node:test` 19/19, real mutation 9/10 (`S5_R3_ROOT_OF_TRUST_AUDIT.md`) |
| L6-08 | P3 | MITIGATED | "VERIFIED_PRIMARY rounds up" → reclassified REIMPLEMENTATION_REPRODUCED, now golden **EXECUTED_PRIMARY** |

## LENS 7 — accessibility / equity / UX / safety (5)
| raw | sev | disposition | target / reason |
|---|---|---|---|
| L7-01 | P1 | ESCALATED | F-05 (MB-FOCUS-0051) screen-reader announces the answer |
| L7-02 | P2 | FOLDED | F-05 — accuracy not modality-fair for sequential/switch |
| L7-03 | P2 | FOLDED | F-05 — Android modality-selection heuristic is the only selector |
| L7-04 | P2 | NOT_ESCALATED | no Dynamic-Type/overflow capture; a11y UX, `S5_R3_ACCESSIBILITY_DECISION_RECORD.md` |
| L7-05 | P3 | FOLDED | F-05 — a11y suite proves geometry not construct (closing test = construct-preservation, not text-match) |

## Comparison to the V1/V2 manual integration
- **findings lost:** none (39/39 dispositioned; every P0/P1 escalated or folded).
- **severities changed:** none downgraded to hide risk; 4 raw findings MITIGATED by V2 execution/tooling (F-L2-04, L6-07, L6-08, L1-R3-04-partial) with cited evidence.
- **incorrect merges:** none — each FOLD conserves the risk in the target finding's evidence + closing test.
- **conflicts:** none unresolved.
- **new dependencies surfaced:** the R3 EXECUTED_PRIMARY run resolves F-L2-04's "toy vs shipped" charge — a
  dependency the V1 integrator could not have discharged (it predated the authorized build).
- **changed decisions:** F-03 evidence class REIMPLEMENTATION_REPRODUCED → EXECUTED_PRIMARY (golden ran).
