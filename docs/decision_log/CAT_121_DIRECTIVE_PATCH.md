# PROPOSED Decision Log entry — 121-exercise directive (NOT LANDED)

> **Status: BLOCKED / PROPOSED PATCH ONLY.** `docs/context-pack/09_DECISION_LOG.md` is under an active
> concurrent governance rewrite (ledger `MB-MEMORY-0005`) and has a divergent duplicate copy
> (`docs/context_pack/…`, `MB-MEMORY-0004`). Editing it now would clobber foreign WIP (CLAUDE.md §4.10).
> This file is the **proposed** entry to be applied by the Decision-Log owner **under lock** after the
> concurrent rewrite lands. **The decision is NOT canonically closed** until then.

## ID collision check (A6)

- `CAT-005` **already exists** and means *"all target exercises must be technically complete and available
  at first launch…"* — **DO NOT reuse or overwrite it.**
- Both Decision-Log copies (hyphen + underscore) currently contain `CAT-001 … CAT-012`.
- **Next apparently-free id = `CAT-013`** — but the concurrent worktree may have consumed it. The owner
  MUST re-read the live Decision Log **under lock** and assign the next genuinely-free `CAT-0NN` before landing.

## Proposed entry (assign the verified next-free id at land time)

**CAT-013 (proposed) — Operative catalog target = 121 (CEO directive, 2026-07-14).**

- **Prior approved minimum:** 120 (`CAT-003`/`CAT-004`, unchanged and NOT superseded).
- **Operative target now:** **121**, per CEO directive of 2026-07-14.
- **Structure:** 8 blocks × 15 base slots (= 120) + **1 EXTRA slot** (= 121).
- **EXTRA (#121):** remains **UNASSIGNED** until a future audit demonstrates, for its candidate exercise:
  independent mechanic, own construct/subskill, own scoring, own dose/adaptation, standalone experience,
  incremental value over the other 120, and no double-counting. Existing-in-code is **not** scientific evidence
  (e.g. `EX16_CALIBRACION_CONFIANZA` is an overlay auxiliary, **not** a candidate — `MB-CATALOG-0003`).
- **121 does NOT authorize** counting overlays, probes, alternate forms, context check-ins, or composite/mixed
  sessions toward the target (they live in `MINDBOOST_EXERCISE_AUXILIARIES.csv`).
- **Cross-refs:** ledger `MB-CATALOG-0001` (denominator authority), `MB-CATALOG-0003` (identity/dedup).

## Landing checklist (owner, under lock)

1. Acquire the Decision-Log write lock; confirm the concurrent governance rewrite has landed.
2. Re-read the live Decision Log; pick the next genuinely-free `CAT-0NN`.
3. Insert the entry above with that id; do **not** modify `CAT-003`/`CAT-004`/`CAT-005`.
4. Reconcile the hyphen vs underscore duplicate first (`MB-MEMORY-0004`) so one Decision-Log owner remains.
5. Update ledger `MB-CATALOG-0001` closure evidence with the landed id; flip `DECISION_LOG_121_CANONICAL` to YES.
