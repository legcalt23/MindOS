# S5_R3_REAUDIT_MASTER_REPORT — Focus V5 · S5 · R3 · adversarial reaudit of Paso 7

```
TASK: TASK-2026-07-22-FOCUS-V5-S5-R3-REAUDIT
REVIEW_CLASS: AI_ASSISTED_INDEPENDENT_REVIEW (performed by Claude; NOT a human/institutional external audit)
MANDATE: refute, don't defend; doc/tooling/ledger only; NO code/Gradle/mount/freeze/commit
HEAD described: 8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5 · branch product/focus2-v5 · package UNTRACKED
RECOMMENDED DECISION: R3 REMAINS CANDIDATA (REOPENED/BLOCKED). Do NOT freeze / mount / declare parity.
```

## 1. Scope & method

Read-only audit of the tree + reproduction of the R2 falsification + correction of docs/ledger/tooling.
Method: (A) snapshot & authority capture; (B) reproduce the falsification with an independent harness;
(C) 7 persisted adversarial context-fresh lenses via Workflow; (D) minimal in-scope corrections; (E) clean-room
re-run. Every material claim is epistemically labelled. No Gradle/tests/iOS/device run — those are `NOT_RUN`/
`BLOCKED`, never faked. Companion artifacts: `S5_R3_REAUDIT_FINDINGS.json`, `S5_R3_MULTIDIMENSIONAL_VERDICTS.md`,
`S5_R3_LENS_OUTPUTS.json`, `r2_falsification_*`, `S5_R3_EXTERNAL_MANIFEST.*`, `S5_R3_ELIGIBILITY_CONTRACT_SPEC.md`,
`S5_R3_DATAFLOW_THREATMODEL.md`, `S5_R3_BLOCKERS_MATRIX.md`, `S5_R3_REAUDIT_COMMAND_LOG.md`,
`S5_R3_REAUDIT_REDLINE.md`, `S5_R3_CEO_MEMO.md`.

## 2. What I tried to refute, and what survived

| Attempt | Outcome |
|---|---|
| "The R2 falsification is overstated / mis-rounded." | **SURVIVED (stronger).** Reproduced on the real scorer formula with 3 probits (Acklam=repo, Wichura-AS241, bisection-on-erf) agreeing to 1.3e-9; the flip is GENERIC (**1363/1363 feasible bases**), not two hand-built cases. `REPRODUCED`+`CORROBORATED_INDEPENDENTLY`. |
| "The falsified gate is live in code and needs retiring (MB-FOCUS-0047)." | **REFUTED.** grep `*.kt` for `monotonicGuard`/`mg-1`/`S_full`/`Sfull` = 0 hits; no d′-recompute-under-exclusion. The gate is DESIGN-ONLY; the live code already implements R3. The "code retirement pending" framing is misleading (corrected). |
| "The R3 replacement is scientifically sufficient." | **REFUTED.** Non-interference + coverage-monotonicity hold at contract level (harness), but the contract defines **no MNAR/missingness policy**, keeps a low-reliability metric (d′) with no SEM/test-retest gate, and left an orphan differential-exclusion constant. `R3_CONTRACT_SCIENTIFIC_VALIDITY = FAIL`. |
| "The Paso-7 package is audit-ready." | **REFUTED.** Review-class mislabeled EXTERNAL_AUDIT; the whole R3 chain is UNTRACKED (not in the cited HEAD); truncated 16-hex hashes; out-of-band ledger seal; security-APTO contradicted by its own privacy lens; package-closure conflict. All corrected here except UNTRACKED (needs a CEO commit decision). |
| "Accuracy is modality-fair (charter §14)." | **REFUTED (VERIFIED).** `FocusTracerScreen.kt:137` announces the correct flanker answer + congruency to the screen reader — accuracy is trivially solved under AT. |

The central science — the falsification that dominates any APTO — is intact and now the most rigorously
verified it has ever been. Everything built on top of it (implementation, platform, privacy, a11y, launch) is
`BLOCKED`/`FAIL`/`NOT_RUN`.

## 3. Findings (canonical set in `S5_R3_REAUDIT_FINDINGS.json`; full lens detail in `S5_R3_LENS_OUTPUTS.json`)

**P0** — RA-04/MB-FOCUS-0052 security-APTO retired (contradicted by own privacy lens; GAP-1 untested);
RA-09/MB-FOCUS-0035 single-instance-not-enforced silently corrupts write-ahead (mount blocker).
**P1** — RA-01 review-class mislabel (corrected); RA-02/MB-FOCUS-0050 UNTRACKED chain; RA-03/MB-FOCUS-0047
design-only reachability (corrected); RA-05/MB-FOCUS-0051 screen-reader answer-leak (VERIFIED);
RA-06/MB-FOCUS-0053 no MNAR policy; RA-08/MB-FOCUS-0048 wiring-precondition defects (seal metadata unpopulated,
drain discards typed outcome, shared-namespace wipe).
**P2** — RA-07/MB-FOCUS-0054 orphan constant; RA-10 reliability paradox on retained d′; RA-11 truncated hashes /
out-of-band seal (corrected); RA-12 target-population/minors undefined (only in an excluded doc);
RA-13/MB-FOCUS-0055 GATE_CATALOG stale authority (mitigated by index + scanner).

## 4. Cross-lens integration (done by hand — the workflow integrator was under-fed)

**Honest defect of this reaudit:** the Workflow integrator agent received only 2 of 7 lens outputs because the
orchestrator sliced its input to 24 KB. Its verdict is therefore partial and is NOT used as the clearance. I
performed the integration over all 7 persisted lenses:

- **Contradiction resolved (psychometrics vs architecture on excludedFraction null-handling):** `FocusEvidence`
  `StatePolicy` treats null `excludedFraction` as fail-OPEN (`NO_BACKSTOP=0.0` → PROVISIONAL) while
  `FocusTemporalEligibility` treats it fail-CLOSED (`FULLY_EXCLUDED=1.0`). This is **intended** (NotCaptured must
  not demote *accuracy* evidence — the two serve different axes), not a bug — but the same null mapping to
  opposite fail directions in adjacent policies is a latent-risk (P2/P3), flagged for the wiring phase.
- **Convergence:** psychometrics (F-L2-02) and my own eligibility-spec §3 independently name the missing MNAR
  policy → elevated to MB-FOCUS-0053.
- **Coverage gaps no lens covered** (integrator-flagged, confirmed): capsule.json vs .md drift (checked — no
  material drift after correction); the new `WindowedTemporalProbe.*` platform family cross-parity (deferred to
  Paso 4/6); nobody re-ran the harness (I did — digest `2df300f3…`). The 2 modified iOS files
  (`DMListScreenIcon.ios.kt`, `AppMediaViewer.ios.kt`) are pre-existing from the iOS-port task, **not this
  reaudit** — untouched here (CLAUDE.md rule 10).

## 5. Corrections applied (docs/tooling/ledger only)

See `S5_R3_REAUDIT_REDLINE.md` for the per-file redline. Summary: handoff rewritten self-contained; index
review-class/UNTRACKED/scanner-limit/quarantine notes; capsule.json review-class fields; scanner rule 4
(quarantine) + self-test case; ledger MB-FOCUS-0047 reframed + MB-FOCUS-0049..0055 added + event + checkpoint +
reseal; full-SHA-256 external manifest built. **No code, no Gradle, no mount, no freeze, no commit.**

## 6. Verdict

`S5_EXIT_READINESS = FAIL`, `LAUNCH_READINESS = FAIL`. The R2 falsification is `PASS_WITH_PRIMARY_EVIDENCE` and
dominates: it justifies the reopening and it is the strongest result. The R3 replacement is directionally correct
but scientifically incomplete (no MNAR policy) and its a11y/privacy/wiring surfaces FAIL. **Recommendation:
R3 REMAINS CANDIDATA / REOPENED — do not promote.** Decisions the CEO must make are batched in `S5_R3_CEO_MEMO.md`.
