# CEO MEMO — Focus V5 · S5 · R3 reaudit (one page)

**Recommended decision (one sentence):** Keep **ADR-FOCUS-TEMPORAL-QUALITY at R3 CANDIDATA / REOPENED** — the R2
falsification is confirmed and dominant, the R3 replacement is directionally right but scientifically incomplete,
and 5 of 7 adversarial lenses returned FAIL — so do **not** freeze, mount, declare parity, or promote. **Max
honest ceiling = R3 CANDIDATA · CONTRACT_INERT · C0 descriptive · Android internal tracer only.**

**What survived my attempt to refute it.** The falsification is now the most rigorously verified it has ever been:
reproduced on the real scorer formula by 3 independent probits (agree to 1.3e-9), generic across **1363/1363**
feasible bases (not two hand-built cases), digest `2df300f3…`. It correctly killed the R2 gate. **New:** that gate
never existed in code (design-only) — so the prior "retire the gate in code" task was a phantom; the real pending
work is different.

**What did not survive (must not be promoted).** R3_CONTRACT_SCIENTIFIC_VALIDITY = **FAIL** (no missingness/MNAR
policy; d′ reliability unquantified). PRIVACY = **FAIL** (revocation gap untested; cross-account wipe pattern).
ACCESSIBILITY = **FAIL** (the screen reader announces the correct answer → accuracy is not modality-fair,
verified). PROVENANCE = **FAIL→corrected** except the package is UNTRACKED. iOS/physical = **BLOCKED/NOT_RUN**.

## Decisions you must make (one batch — pick one option per row)

| # | Decision | Options (recommended first) | Consequence |
|---|---|---|---|
| 1 | **`TRAINING_FREE`** — may a user train when a session yields no official measurement? | (a) **YES, train-freely; UI states "not measured" honestly** · (b) no, block training on unmeasurable sessions | (a) unblocks W3 + honest UX; (b) blocks training for probe-less/AT users — likely wrong. |
| 2 | **Target population + minors** (undefined; only in an excluded doc) | (a) **adults-only 18+, age-gate before any S6 exposure** · (b) include minors with GDPR-K/DPO path · (c) defer, no human exposure | (a) simplest, lets S6 proceed; (b) heavy legal/DPO; (c) stalls S6. Needed before ANY human data. |
| 3 | **C-06 / retention & telemetry policy** for disability-proxy signals (screenReader/switch), crash reports, exports | (a) **minimize + DPO DPIA before wiring** · (b) proceed with current summary-only minimization | (a) required for health-adjacent data; (b) risk if these are "health data" under GDPR. |
| 4 | **Commit the R3 package?** (today UNTRACKED, not git-reproducible) | (a) **commit on a branch to give git provenance** · (b) keep worktree-only, rely on byte-manifest | (a) fixes MB-FOCUS-0050; (b) integrity rests on the manifest, not git. |

## What I changed (docs/tooling/ledger only — NO code, NO Gradle, NO mount, NO freeze, NO commit)

Rewrote the handoff self-contained; corrected the authority index + evidence capsule; built a full-SHA-256
external manifest + a reproducible falsification harness; enhanced the discoverability scanner (quarantine);
reframed MB-FOCUS-0047 and seeded MB-FOCUS-0049..0055; resealed the ledger. All green: harness exit 0, scanner
self-test 6/6 + real, ledger validator matches. Details: `S5_R3_REAUDIT_MASTER_REPORT.md`,
`S5_R3_MULTIDIMENSIONAL_VERDICTS.md`, `S5_R3_REAUDIT_FINDINGS.json`.

**Next single allowed action:** none autonomously — await your decision on the four items above. The next
build/gate/wiring step (Paso 3/4) is a separate authorized invocation with its own `T-DR`; iOS and the physical
device matrix remain blocked.
