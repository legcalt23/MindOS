# R3-A3 — findings

Snapshot: `1214818288e0fd59716b3339704afeecc5fa06feeca76c9a8e0ff09f382cf899` · HEAD `71e17038` (detached)

## STATUS: R3-A is **NOT APTO**. `test-engineer` returned NO APTO twice (#3 and #4). F-01 is only PARTIALLY closed.

## Open P1 — blocking

### F-01 (P1) — **PARTIALLY CLOSED**. The numerator is protected for ONE check out of twelve.
`record()` could launder every FAIL into PASS with one line, and all twelve adversarial tests stayed green. Raised
by `test-engineer` #3 with a working reproduction: corrupt the V2c handoff — the artifact that recovered the
poisoned CSV in MB-PORTFOLIO-0018 — and the runner published `FREEZE_VERIFIED 12/12 reconciled=true
status=completed` while the suite reported `12/12 KILLED`.

Root cause, in the reviewer's words: **every oracle was expressed relative to the artifact under test.** TE-1's
lesson had been applied thoroughly to the denominator and not at all to the numerator.

Fix: **P8A-12**, whose oracle is *ground truth* — a pinned artifact is provably corrupt, therefore the run must
fail — a claim `record()` cannot influence. Both arms carry the same corruption; only `record()` differs.

| Arm | Verdict |
|---|---|
| corrupted V2c, `record()` INTACT | `FREEZE_FAILED (11/12)` exit 1 |
| corrupted V2c, `record()` LAUNDERED | `FREEZE_VERIFIED (12/12)` exit 0 |

**But this is NOT a fix for the defect — it is a fix for the reproduction.** `test-engineer` #4 proved the class
survives. Forcing `ok=true` at the **P8-01** call site (leaving `record()`'s body untouched, so P8A-12's string
match is never even consulted) and corrupting an artifact only P8-01 depends on
(`PORTFOLIO_V2C_LEGACY_CLAIMS.csv`):

```
PHASE 8: FREEZE_VERIFIED (12/12)
published P8-01 state: PASS | detail: "1 artifact(s) drifted since the manifest was written: PORTFOLIO_V2C_LEGACY_CLAIMS.csv"
                       ^^^^ self-contradictory: PASS with an admitted drift in the same record
PHASE 8 ADVERSARIAL: 14/14 KILLED   exit=0    <- total blindness
```

P8A-12 is a ground-truth oracle for **exactly one of twelve** checks. A targeted lie about any of P8-01, P8-03…P8-12
still publishes a fully self-consistent `FREEZE_VERIFIED (12/12)` while the suite reports 14/14 KILLED.

**Required to close:** a ground-truth oracle per check — corrupt one artifact per check and assert the corresponding
`record()` reports FAIL — rather than one blanket state-mutation plus one hand-picked artifact. NOT attempted: it is
real work that must be done and verified in one piece, and the session had no budget left to close it honestly.

This is the same lesson a third time: **I fixed the reproduction I was handed instead of the property it exposed.**

## Closed in R3-A3

### F-02 (P1) — the denominator was frozen only relative to itself
`PHASE8_CHECKS.length !== EXPECTED_CHECK_COUNT` compared two values in the same file that move together, and every
adversarial test *imported* `EXPECTED_CHECK_COUNT` from the module it was policing. Deleting P8-12 and decrementing
the constant produced `FREEZE_VERIFIED (11/11)` with the suite at `12/12 KILLED`. The denominator was defended
against **runtime** shrinkage and not at all against **edit** shrinkage.

Fix: `CONTRACT_CHECK_COUNT = 12` and `CONTRACT_CHECK_IDS` pinned as literals in `phase8-checks.mjs`; the suite now
declares `LOCAL_EXPECTED_COUNT`/`LOCAL_EXPECTED_IDS` **locally instead of importing them** (+ P8A-00b).

Revert-test of the reviewer's exact attack — 3 violations where there were previously 0:
```
EXPECTED_CHECK_COUNT is 11; the contract pins 12. The denominator is not editable.
registry declares 11 checks; the contract pins 12
registry ids [P8-01..P8-11] != contract ids [P8-01..P8-12] — a check was renamed, removed, reordered or added
```

### F-03 (P1) — stale green survived a hard kill
`writeFileSync` direct to the canonical report path, with no `runId`, no `snapshotId`, no atomic rename. An
*exception* was caught and still wrote `FREEZE_FAILED`, which is what made this look safe. A **hard kill** (SIGKILL,
OOM, power) before the write left the previous green report byte-perfect and unattributable.

Fix: `atomic-report.mjs` — start marker → temp → fsync → atomic rename → marker cleared last; reports carry
`runId`/`snapshotId`/`status`; `readReport()` rejects `RUN_INCOMPLETE`, `SNAPSHOT_MISMATCH`, `NOT_COMPLETED`,
`UNPARSEABLE`. Twelve fixtures (`atomic-report-tests.mjs`), all discriminating. AR-08 is the decisive one: a
byte-perfect green **with the right snapshot** is still rejected, because the marker outs it. Revert-test confirmed
the old design accepts that same green.

### F-04 (P1) — P5-19 was a third tautology, exposed by a stale filename
`SELF = new Set(['BUNDLE_MANIFEST.json', 'SHA256SUMS.txt'])` held the **repo-mode** filename in a bundle-mode set.
It matched nothing, so `declared.size === m.artifacts.length` was trivially true; `missing.length === 1` was
arithmetic on a set built one line earlier; and **`BUNDLE_SHA256SUMS.txt` was never opened**. Its message claimed
"the reverse is checked too" — nothing was checked.

Fix: reads both files off disk, asserts they agree today, mutates each direction independently. Discrimination
proved by divergence — sums intact → PASS, one line dropped → FAIL, file restored (`79ed4255…` both sides).

### F-05 (P3) — no child-process timeouts
`phase8-freeze.mjs` (1 `execFileSync`) and `clean-room-test.mjs` (3) had none. A hung child hangs the gate forever,
and a hung freeze reports nothing rather than reporting a failure. Fixed: 240 s, matching the adversarial suite.

### F-06 — snapshot tool defects, self-found
1. **Not idempotent** — its own outputs matched the in-scope pattern, so a re-run over an unchanged tree produced a
   different id and §11's invalidation rule would fire on every run.
2. **Misclassified my own artifacts as FOREIGN** — `CLEAN_ROOM_RUNBOOK.md`, `DELIVERY_MANIFEST.json`,
   `SHA256SUMS.txt`, the candidate ZIP and every `PHASE*_RESULT.json`.
3. **Circular** — running the gates rewrote the result files, changing the very id those results were bound to.

Fixed by an explicit three-way split: INPUTS define the id; OUTPUTS are bound to it via `snapshotId` and excluded
from it; FOREIGN is recorded untouched. Verified stable: running the full battery does not move the id.

## Open — NOT claimed as fixed

| Id | Sev | Finding | Owner |
|---|---|---|---|
| **F-07** | P2 | `readReport()` has **no production call site**. The enforcement half of snapshot binding is implemented and tested but never invoked; no gate rejects a stale/mismatched/unfinished report. | R3-A follow-up |
| **F-08** | P2 | `currentSnapshotId()` **copies** the id out of `R3A_FINAL_SNAPSHOT.json` rather than measuring the tree. The binding is self-asserted: a report can claim a pristine snapshot while the tree is mutated. | R3-A follow-up |
| **F-09** | P2 | 6 of 7 report writers bypass `beginRun`/`publish`. `mutation-registry.mjs` accepts a **forged** `PHASE5_MUTATION_RESULT.json` (fake snapshotId, 29 invented ids) and reports `RECONCILED 65/65`. `PHASE5_*` and `R03_*` are not in `BUNDLE_MANIFEST.artifacts`, so P8-01 does not pin them either. | R3-B (registry authority) |
| **F-10** | P2 | The P8-09 `REGENERATED` purge — the only thing preventing shipped-vs-shipped — has **no test**. A silent regeneration no-op (a tool exiting 0 while writing nothing) is caught only by the purge. | R3-A follow-up |
| **F-11** | P3 | `targetChanged` is per-patch, not per-replace: a semantics-preserving refactor can make one `.replace()` a no-op while another still fires, so no ORACLE FAILURE raises and the evidence text becomes false. | R3-A follow-up |
| **F-12** | P3 | `R3A_FINAL_SNAPSHOT.json`'s own recorded output hash can never match (hashed before write), so the `outputs` set can never be verified whole. | cosmetic |

### F-09 was observed live, not theorised
During R3-A3, `mutation-registry.mjs` read a **stale** `PHASE5_MUTATION_RESULT.json` from a superseded run and
reported `phase5 28/29 · TOTAL 64/65`. The suite itself reported 29/29. The registry has no run identity and no
snapshot binding — the same stale-report class as F-03, in a different consumer. Recorded rather than fixed,
because the registry is R3-B's subject.

## Pattern

Four of the six closed findings are the same defect: **an oracle whose terms cannot be false** (clean room `6/10 as
6/6`, P8A-08, P5-19) or **an oracle expressed relative to the thing it audits** (`record()` laundering, the
imported denominator). A passing test is not evidence. Only a test that fails when the property breaks is evidence,
and the only way to know which you have is to break the property and watch.
