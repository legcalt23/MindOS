# Portfolio 121 V2c — Evidence package (kill false-greens)

READ-ONLY audit. No production code changed. Base commit `71e17038` (dirty worktree; deliverables UNTRACKED
→ **hash-identifiable snapshot**). Authoritative hash list: `SHA256SUMS.txt`; delivery set: `DELIVERY_MANIFEST.json`.

## False-green → regression (each CEO-reproduced case now fails before / passes after)

| # | CEO false-green (passed V2b) | V2c gate that now catches it | Proof |
|---|---|---|---|
| 1 | `E1_VIGIA`→`TOTALLY_FAKE_CODE_ID` passed | STATIC_CODE_ID_RECONCILIATION_GATE | oracle #1 FAIL "invented/unknown target codeExerciseId" |
| 2 | MD "Slots defined 999/121" passed | GENERATED_VIEW_GATE (byte-exact) | oracle #4 FAIL "differs from CSV" |
| 3 | empty `SHA256SUMS.txt` passed | IN_REPO_PACKAGE_GATE | oracle #5 FAIL "SHA256SUMS.txt is empty" |
| 4 | design-only `TRAINING_READY` passed | STATUS_CROSS_INVARIANTS + TARGET_LAUNCH_READINESS | oracle #9 FAIL "TRAINING_READY but implementationStatus" |
| 5 | fake current-verification strings passed | CLAIM_EVIDENCE_GATE (real-path check) | oracle #11 FAIL "evidencePaths not a real file" |
| 6 | design-only `iosStatus=UNKNOWN` passed | STATUS_CROSS_INVARIANTS_GATE | oracle #13 FAIL "DESIGN_ONLY platform not NOT_IMPLEMENTED" |
| 7 | live `androidStatus=NOT_APPLICABLE` passed | STATUS_CROSS_INVARIANTS_GATE | oracle #14 FAIL "LIVE androidStatus" |
| 8 | race test deleted a legit lock | isolated lock-tests (never touch real lock) | lock-tests T2 (live survives) + T7 (real lock untouched) |
| 9 | expired takeover → multiple winners | fencing-token CAS takeover | lock-tests T3: 100 rounds × 24 → exactly 1 winner each |

## Lock safety (A) — `node scripts/portfolio/lock-tests.mjs` → PASS (isolated temp; real lock untouched)

```
T1 empty lock 8/24 competitors → exactly 1 winner
T2 live lock → 0 winners, lock byte-intact, holder token still valid
T3 expired takeover ×100 rounds/24 competitors → exactly 1 winner each (bad=0)
T4 release wrong token → rejected; correct token → accepted
T5 old owner after takeover → cannot write or release (fencing)
T6 crash before rename → original ledger intact; completed write is atomic
T7 real ledger lock state unchanged
```
The V2c lock: random fencing tokens; injectable `LEDGER_PATH`/`LOCK_PATH`; expired-takeover via an O_EXCL
takeover-claim keyed to the observed token (CAS — a late racer can never reap a fresh lock); temp+fsync+atomic
rename for ledger writes. `LEDGER_LOCK_SAFETY_GATE` is tied to `LOCK_SAFETY_RESULT.json` (no static bluff).

## Code reconciliation (B) — `node scripts/portfolio/extract-code-inventory.mjs`

70 observed code ids across 8 blocks (declared/wired): FOCUS 13/13 · SPEED 8/3 · LOGIC 5/5 · LANG 7/7 ·
MEMORY 16/3 · DECIDE 4/4 · EMOTION 15/6 · CREATE 2/2. `STATIC_CODE_ID_RECONCILIATION_GATE` compares
observed = target ∪ aux ∪ allowlist(`Result`). Speed shows 3/8 wired-generators detectable statically →
those 5 live rows carry `generatorOrBankStatus=UNRESOLVED_STATIC` (honest) and GENERATOR_RENDERER = PARTIAL.

## Gate results — `node scripts/portfolio/validate-portfolio.mjs`

```
STATIC_CODE_ID_RECONCILIATION_GATE: PASS
GENERATOR_RENDERER_RECONCILIATION_GATE: PARTIAL (5 UNRESOLVED_STATIC generators; class-registry blocks)
ROUTE_REACHABILITY_STATIC_GATE: PARTIAL (block-level static only)
RUNTIME_PLAYABILITY_GATE: NOT_RUN
TARGET_SLOT_SEMANTICS_GATE: PASS
STATUS_CROSS_INVARIANTS_GATE: PASS
CLAIM_EVIDENCE_GATE: PASS
GENERATED_VIEW_GATE: PASS
IN_REPO_PACKAGE_GATE: PASS
EXTERNAL_HANDOFF_AVAILABILITY_GATE: PENDING (MB-PORTFOLIO-0004)
LEDGER_LOCK_SAFETY_GATE: PASS
LEDGER_LOCK_ENFORCEMENT_GATE: PARTIAL
DESIGN_SPEC_COMPLETENESS_GATE: FAIL (37 undesigned)
SLOT_ASSIGNMENT_COMPLETENESS_GATE: FAIL (37 unassigned)
IMPLEMENTATION_COMPLETENESS_GATE: FAIL (78 not live)
TARGET_LAUNCH_READINESS_GATE: FAIL (0/121; CAT-005 requires 121/121)
BLOCKING GATES: PASS
```

## Mutation oracle (C) — `node scripts/portfolio/mutation-test.mjs` → PASS 18/18

Each mutation changes exactly ONE field/artifact in an isolated temp fixture (shared RFC-4180
parser/serializer; single-field-change verified) and asserts the EXPECTED gate FAILs for the EXPECTED
diagnostic; a crash / wrong gate / wrong diagnostic = TEST FAILURE. All 18 caught by the expected gate+diagnostic.

## Counts (reproduced from CSV, auxiliaries excluded)

targetRows 121/121 · auxInTarget 0 · **assigned candidates 84/121 · distinctness-approved 0/121** ·
code-declared 65 · mechanic-implemented 44 · live-reachable 43 · hidden 1 · catalog-only 21 · design-only 19 ·
unassigned 37 · current-snapshot-verified 0 · TRAINING_READY 0. Per block (defined/15 · live · hidden ·
catalog · design · unassigned): Enfoque 13·13·0·0·0·2 · Velocidad 8·8·0·0·0·7 · Logica 10·4·0·0·6·5 ·
Lenguaje 8·6·0·0·2·7 · Memoria 15·3·0·12·0·0 · Decisiones 7·1·1·0·5·8 · Emociones 15·6·0·9·0·0 ·
Creatividad 8·2·0·0·6·7 · EXTRA-121 unassigned.

## Auxiliaries (never target slots)

`AUX-DECIDE-CHECKIN` (context check-in, APPROVED) · `AUX-LOGIC-CONDITIONAL` (alternate form, **PROVISIONAL** —
dedup NOT scientifically verified, `MB-CATALOG-0004`) · `AUX-MEMORY-EX16` (overlay, PROVISIONAL, NOT the #121
candidate) · `AUX-LANG-MIXED` (composite, APPROVED).

## First-vertical (J) — HYPOTHESIS ONLY, not implemented, not approved

`LOGIC-03` (Necessary/Sufficient) remains a **hypothesis** for the first vertical; "cognitive independence",
"offline", "unambiguous scoring" require exact per-exercise evidence not yet gathered. Runner-up `LANG-05`.
Both static banks (~6 items) lack a `BankValidator` (memorization risk). Excluded: Focus E1 / Speed E8
(RT-critical). Hard prerequisite: a real macOS/CI vein. After V2c the portfolio FREEZES → back to the P0s.

## Limits & blockers

0 current-snapshot code verification (READ-ONLY); iOS BLOCKED (Windows); per-exercise QA UNKNOWN; deliverables
UNTRACKED → hash-identifiable only (`MB-PORTFOLIO-0007`); Decision Log BLOCKED (concurrent WIP → `CAT_121_DIRECTIVE_PATCH.md`
only); distinctness 0/121 approved → MANUAL_REVIEW (`MB-CATALOG-0004`); lock advisory (enforcement PARTIAL,
`MB-LEDGER-0001`); external artifact availability PENDING (`MB-PORTFOLIO-0004`); GENERATOR_RENDERER/ROUTE PARTIAL.
