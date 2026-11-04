# S5_R3_FINDINGS_LINEAGE — immutable lineage: raw lens → V1 → V2 → ledger

```
TASK: TASK-2026-07-22-FOCUS-V5-S5-R3-REAUDIT-V2 (Part 3)
REVIEW_CLASS: AI_SELF_REAUDIT_WITH_CONTEXT_FRESH_LENSES + CROSS_MODEL_ADVERSARIAL_INPUT_RECEIVED
AUTHORITATIVE REGISTER: MINDBOOST_AUDIT_LEDGER.md §2 (append-only). The register — NOT any findings JSON — is the
                        canonical meaning of a ledger ID. Findings artifacts REFERENCE the register; when they
                        disagree, the register wins and the artifact error is recorded here.
```

## 0. Absolute rule applied

> A published ledger ID never changes meaning retroactively. If reused, restore its meaning and mint a new ID.

**Finding: NO register ID was reused with a different meaning.** The apparent reuse (`MB-FOCUS-0054`) lived only in
the **superseded V1 findings JSON**, not in the append-only register. Ground truth (transcript
`f810f46e…jsonl`, occurrence counts): `MB-FOCUS-0054` co-occurs with "orphan/differentialExclusion" **17×** and with
"population/minors" **6×**; every one of the 6 is the erroneous V1 `findings.json#RA-12` `ledger` field or a
downstream echo of it. The register row `MB-FOCUS-0054` has read `differentialExclusionDelta orphan constant` since
it was first written in V1. Population/minors **never had a register row in V1** — it first got one in **V2** as
`MB-FOCUS-0057`. So the correct action was already taken (a *new* ID for population), and no restoration is owed.
The residual defect is a **superseded-artifact mislabel**, corrected in `findings_v2.json` and recorded below; the
V1 `findings.json` is preserved unedited as the historical record of that error (per the 2026-07-20 "preserve the
erroneous artifact, reconcile in a new doc" learning).

## 1. Lens → V1 canonical → V2 canonical → ledger (immutable)

Raw lens IDs are the per-lens finding tags inside `reaudit/S5_R3_LENS_OUTPUTS.json` (7 lenses; the raw file records
40+ lens-level findings, deliberately un-deduplicated). "action" is the disposition from raw→canonical and V1→V2.

| raw_lens_id(s) | V1 canonical | V1 ledger (as written) | V2 canonical | V2 ledger (register) | action | justification | closing criterion |
|---|---|---|---|---|---|---|---|
| efficacy-review L1-R3-04; provenance | RA-01 | `MB-FOCUS-0049` | F-07 | `MB-FOCUS-0049` | PRESERVED | review-class mislabel; same ID, same meaning | no forward EXTERNAL_AUDIT claim in package |
| provenance/package-closure | RA-02 **+** RA-11 | `MB-FOCUS-0050` (+ bogus `0050b`) | F-06 | `MB-FOCUS-0050` | MERGED | untracked-chain + truncated-hash/out-of-band-seal are one provenance defect; `0050b` was never a register row | `package_root_sha256` in a signed commit / immutable CI artifact |
| psychometrics-falsification | RA-03 | `MB-FOCUS-0047` | F-03 | `MB-FOCUS-0047` | PRESERVED (evidence class UPGRADED) | R2 gate design-only; same ID. **V2: golden EXECUTED_PRIMARY** (was REIMPLEMENTATION_REPRODUCED) | ADR register R2=SUPERSEDED; Paso-4 diff has no `S_adm` recompute-under-exclusion |
| privacy-security L5-01/02 | RA-04 | `MB-FOCUS-0052` | F-01 | `MB-FOCUS-0052` | PRESERVED | security "APTO" downgrade + focus2 revocation gap; same ID | focus2 revocation regression test green; cross-account erase test |
| accessibility L7-01 (L7-02 residual) | RA-05 | `MB-FOCUS-0051` | F-05 | `MB-FOCUS-0051` | PRESERVED | screen-reader answer leak; same ID | construct-preservation test + manual TalkBack/VoiceOver/Switch |
| psychometrics-missingness F-L2-02 | RA-06 | `MB-FOCUS-0053` | F-04 | `MB-FOCUS-0053` | PRESERVED | no MNAR policy; same ID | missingness sensitivity + bounds per tracer |
| psychometrics-missingness F-L2-03 | RA-07 | `MB-FOCUS-0053b` **(bogus suffix)** | F-09 | `MB-FOCUS-0054` | REMAPPED | orphan `differentialExclusionDelta`; V1 mis-cited a non-existent `0053b`; register row is `0054` | constant has a tested consumer or is removed |
| arch-state-recovery LENS4-02/03; timing-kmp L3-01 | RA-08 | `MB-FOCUS-0048` | F-08 | `MB-FOCUS-0048` | PRESERVED (folds 3 lens findings) | wiring preconditions; same ID | seal populates 4 fields; drain asserts outcome; cross-account erase test |
| arch-state-recovery LENS4-01 | RA-09 | `MB-FOCUS-0035` | F-02 | `MB-FOCUS-0035` | PRESERVED (sharpened) | single-instance not enforced; pre-existing ID | concurrency test proves 2nd session refused/isolated |
| efficacy L1-R3-01 (reliability) | RA-10 | `MB-FOCUS-0051b` **(bogus suffix)** | F-10 | `MB-FOCUS-0056` | REMAPPED → NEW | reliability paradox on retained d′; V1 mis-cited a non-existent `0051b`; V2 minted the real new row `0056` | no cross-session contrast without a measured reliability coefficient |
| provenance (hash hygiene) | RA-11 | `MB-FOCUS-0050b` **(bogus suffix)** | (folded into F-06) | `MB-FOCUS-0050` | MERGED | truncated-hash defect is part of the provenance defect `0050`; `0050b` never existed | manifest full-SHA + external anchor (= F-06) |
| governance (population/minors) | RA-12 | `MB-FOCUS-0054` **(MISLABEL)** | F-11 | `MB-FOCUS-0057` | REMAPPED → NEW | V1 `findings.json` wrote `0054` for population, but the register row `0054` is the orphan constant; population had no row until V2 minted `0057` | population/minors decision recorded; DPIA high-risk screening pre-exposure |
| provenance GATE_CATALOG | RA-13 | `MB-FOCUS-0055` | F-12 | `MB-FOCUS-0055` | PRESERVED | stale gate-catalog self-authority; same ID | scanner FAILs if GATE_CATALOG asserts authority vocab / index negates disposition |

## 2. The mandate's 15 questions, answered

1. **Why V1=13, V2=12?** V1 RA-02 and RA-11 were two findings sharing one provenance defect (`MB-FOCUS-0050`);
   V2 **merged** them into F-06. No finding was lost — the merge conserves both risks (untracked chain + truncated
   hashes) in F-06's evidence + closing test. 13 − 1 (merge) = 12.
2. **Where did each of the 40+ raw findings go?** Each raw lens finding maps to exactly one canonical row above
   (see the "raw_lens_id(s)" column). The many-to-one folds: RA-08 folds LENS4-02/LENS4-03/L3-01; RA-02+RA-11 fold
   the provenance-lens hash/tracking findings. The deterministic per-lens disposition is in the coverage map
   (`S5_R3_LENS_COVERAGE.md`, Part 5). No raw finding is unmapped.
3. **What happened to `0050b`?** Never a register row. A V1 findings-JSON bookkeeping suffix for "the hash-hygiene
   half of the provenance defect". Its risk lives in `MB-FOCUS-0050` (F-06). The V2 validator now FAILs on any
   `b`-suffix ledger ref (`LEDGER_B_SUFFIX`).
4. **`0051b`?** Never a register row. V1 suffix for the reliability finding (RA-10). Its risk is the **new** real
   row `MB-FOCUS-0056` (F-10).
5. **`0053b`?** Never a register row. V1 suffix for the orphan-constant finding (RA-07). Its meaning is the register
   row `MB-FOCUS-0054` (F-09).
6. **Why only `0056` and `0057` created?** Those are the only two risks V2 surfaced that had **no** existing
   register row: d′ reliability (`0056`) and target-population/minors (`0057`). Everything else already had a real
   row (0035, 0047–0055); the bogus `b` suffixes were re-pointed to existing rows, not given new ones.
7. **Historical meaning of `0052`?** Unchanged V1→V2: "security 'APTO' retired → DOCUMENTATION_SECURITY_REVIEW_ONLY;
   focus2 subject-revocation gap untested." (RA-04 = F-01.)
8. **Historical meaning of `0054`?** Unchanged in the **register** since first written: "`differentialExclusionDelta`
   orphan constant (R2's within-session MNAR detector dropped in R3)." The V1 `findings.json` mislabelled it as
   population (RA-12); that artifact error is corrected in V2 (F-09 = orphan; population moved to `0057`).
9. **Was any ID reused with different semantics?** **In the append-only register: NO** (proven by transcript, §0).
   In the superseded V1 findings artifact: `0054` was *mis-referenced* for population — an artifact error, not a
   register reuse. No restoration owed; V2 already assigned population its own new ID (`0057`).
10. **Which finding absorbed the truncated-hash problem?** `MB-FOCUS-0050` / F-06 (merged from RA-11). Fix = full
    64-hex manifest; residual = external anchor (FAIL_UNANCHORED).
11. **Which finding is the orphan constant?** `MB-FOCUS-0054` / F-09 (`differentialExclusionDelta`,
    `TemporalQualityThresholds.kt:25`, 0 consumers).
12. **Which is population/minors?** `MB-FOCUS-0057` / F-11.
13. **Which is reliability?** `MB-FOCUS-0056` / F-10 (d′ reliability UNKNOWN_UNTIL_MEASURED).
14. **Which is revocation?** `MB-FOCUS-0052` / F-01 (security downgrade + focus2 SubjectRevocationAuthority not
    wired, untested).
15. **Which is the old false "security APTO"?** `MB-FOCUS-0052` / F-01 — the same row that retired the APTO label.

## 3. Enforcement

The cross-artifact validator (`validate_cross_artifact.mjs`, Part 4) enforces this lineage mechanically:
`LEDGER_B_SUFFIX` (no `…b`), `LEDGER_ID_DANGLING` (every ref is a real register row), `LEDGER_ID_NONUNIQUE`
(no two V2 findings share a register ID), and `V1_LEDGER_DRIFT` (each V1 `ledger` ref is either a real register ID
or explicitly recorded here as a corrected mislabel). If a future edit re-points a register row's meaning, the
lineage table's "closing criterion" no longer matches the register row text and the drift check fails.
