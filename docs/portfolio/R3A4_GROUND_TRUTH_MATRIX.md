# R3-A4 §3 — Twelve-check ground-truth matrix

Derived by twelve independent per-check analyses + twelve adversarial attacks + a completeness critic
(25 agents, 5.9M subagent tokens). Delivered **before implementation**, as §3 requires.

Baseline: `INPUT_SNAPSHOT_ID = 5cae6b907d067464b98b571c3f60df06260a9eee422b677ff768ecf346e414ca` (71 inputs,
0 drift, verified). HEAD `71e17038`, detached.

## The headline

**50 load-bearing branches. 28 discriminating. 22 NOT_TESTED. Zero implemented.**

| Check | Branches | Discriminating | NOT_TESTED |
|---|---:|---:|---:|
| P8-01 | 5 | 3 | 2 |
| P8-02 | 3 | 2 | 1 |
| P8-03 | 2 | 2 | 0 |
| P8-04 | 5 | 1 | 4 |
| P8-05 | 5 | 1 | 4 |
| P8-06 | 7 | 7 | 0 |
| P8-07 | 4 | 1 | 3 |
| P8-08 | 3 | 3 | 0 |
| P8-09 | 6 | 1 | 5 |
| P8-10 | 5 | 2 | 3 |
| P8-11 | 2 | 2 | 0 |
| P8-12 | 3 | 3 | 0 |
| **Σ** | **50** | **28** | **22** |

I estimated 12, then 26. The real number is **50**. `P8A-12` — the "fix" that closed F-01 in R3-A3 — covers
**1 of 50**.

**The shape is the finding.** Checks at 100% (P8-03, P8-06, P8-08, P8-11, P8-12) have a **child process or a
fingerprint** as their subject — something with independent existence you can damage. Checks at ~1/5 (P8-04,
P8-05, P8-07, P8-09) have **hash-chained data that certifies itself** as their subject. You cannot falsify a
tautology.

## §10's closure condition is unsatisfiable as written

§10 requires "todas sus ramas load-bearing están cubiertas o no queda ninguna `NOT_TESTED`". Several branches are
**unfalsifiable by construction**, not merely untested:

| Branch | Why it cannot be falsified |
|---|---|
| P8-07.b4 | `d.length === e.byteLength` — falsifying it requires a **sha256 second-preimage** |
| P8-07.b3 | `mismatched === 0` — algebraically implied by b2 |
| P8-05.b2 | `n > 0` — dead: b1 ⟹ b2 |
| P8-05.b3 | `seal.before.fileCount === n` — true by construction for any static tree |
| P8-09.b4 | ABSENT guard — trigger unreachable while P8-10 holds |
| P8-04.b2 | `!containsOwnHash` — falsifiable only at an arithmetically unreachable fixed point |

**This needs a CEO ruling**, not a workaround: either the closure criterion becomes "every branch is discriminating
**or declared unfalsifiable with proof**", or R3-A can never close. I will not quietly redefine it.

## Three branches are discarded signals, not coverage gaps

| Branch | Defect |
|---|---|
| **P8-04.b3** | `tar -tf` exit code **thrown away** → a failed listing yields `containsOwnHash=false` → **vacuous PASS** |
| **P8-05.b4** | extraction exit code **thrown away** |
| **P8-10.b2** | `pv.code === 0` **absent from the predicate** — P8-06 and P8-11 both gate on it; P8-10 does not |

Plus **P8-10.b3**: the regex `/BLOCKING GATES: PASS/` is **unanchored** — a diagnostic line carrying that literal
is a **live false-PASS**.

## Residual F-01 — laundering families still uncaught

| Family | Uncaught in |
|---|---|
| **publisher-publishes-verified** | **8/12 outright; the scoped variant survives in all 12** |
| violations-dropped | 10/12 |
| ok-at-callsite (subject redirect) | 7/12 |
| record-forced-pass (conditioned) | 6/12 |
| checkid-changed (property-string rewrite) | 6/12 |
| denominator-reduced (data-level) | 4/12 |
| aggregator-ignores-fail | 4/12 — and only ever caught by `expectedExit`, never by `expectedState` |

### Publisher — VERIFIED LIVE, in a disposable copy

Leaving `record()`, `reconcile()`, the console line and `process.exitCode` **perfectly honest** and editing only
the `run8.publish` payload:

```
ground truth (out-of-band) : V2c actual 0c201c80… ≠ pinned 0f8e2f92…
operator sees              : PHASE 8: FREEZE_FAILED (11/12)  exit 1     ← honest
durable report             : verdict FREEZE_VERIFIED  status completed  ← lie
                             (P8-02 sits inside it as FAIL | V2C_MUTATED)
canonical tree             : 0/71 drifted
```

All 14 current tests and all 28 proposed falsifiers assert `state`+`exit`. **None asserts the published verdict.**
`readReport()` would certify this report valid.

### Property-string rewrite — untestable at runtime

`contractViolations()` pins ids, count, order, `dependsOn`, and `expectedFailureCode` **presence**. It never reads
`property`. The registry's prose is the only statement of what a check *means*, and it ships verbatim into the
report. Any "not caught" row above can be shipped with a matching `property` rewrite, and the check then honestly
does what it says. **No runtime oracle can reach this** — only diff review.

## Structural findings the per-check view cannot see

1. **The unit of test is wrong.** Four shared functions carry all 50 branches: `record()` (12 checks),
   `reconcile()`/`PASSING` (12), `run()` (P8-03/06/08/10/11), `sha()` (P8-01/02/04/12). **A one-line edit to any
   of the four launders 5–12 checks at once.** A matrix organised by check structurally cannot see this.
2. **The `'ABSENT'` sentinel is one defect found four times as four unrelated things** (P8-01.b2, P8-02, P8-04.b5,
   P8-12 — where `'ABSENT'==='ABSENT'` reads as *"byte-identical"*). It is a string in the same namespace as a
   digest. **This repo's own CLAUDE.md forbids it verbatim** — 2026-07-04 (*never use a sentinel whose value is in
   the domain*) and 2026-07-05 (*a read that collapses failure to absence turns erasure into false success*).
   Nobody cited either. One type change fixes all four.
3. **Only P8-02 is externally anchored — and it certifies a museum piece.** The V2c handoff digest is the single
   pinned literal in the whole freeze; V2c has no forward function. **The V2d candidate — the thing actually being
   shipped — has zero pinned literals.**
4. **`MB-V2D-R3-A2 §4` does not exist.** Cited as the normative source by `phase8-checks.mjs:2`,
   `phase8-freeze.mjs:2`, `phase8-adversarial-tests.mjs:2`. **Verified absent from the repo.** It existed only as
   a chat directive, since compacted. The twelve-check contract audits conformance to a phantom.
5. **Nobody closes the audit loop.** P8-03/P8-11 validate the ledger's internal consistency; nothing validates its
   *agreement with the run*. MB-PORTFOLIO-0017's CLOSED evidence says *"drift 0/45"* against a manifest carrying
   **48** — provably stale. The closure text can be hand-written to say 12/12 and pass every check.
6. **P8-01.b4 — the manifest is its own denominator.** `artifacts[]` is simultaneously the subject under test and
   the loop bound, with no pinned count. Delete an entry and P8-01 reports **PASS over the survivors**. This is
   exactly the edit-shrinkage attack R3-A3 fixed for the *check registry* — and applied to nothing else.

## The recommendation: do not write the 28 falsifiers

**Delete the runner's verdict.**

`record(checkId, ok, detail)` → `record(checkId, evidence)`, where evidence is raw observation only —
`{subjectPath, subjectSha256, executable, executableSha256, argv, cwd, envSubset, exitCode, stdout,
declaredCount, iteratedCount}`. No boolean about the property. `state`, `failureCode`, `passed` and `verdict`
become a pure `decide(evidence)` declared in the pinned contract and applied **by the reader**, never by the
runner.

**Kill radius: six of nine families at once** — ok-at-callsite, predicate-inverted, predicate-removed,
record-forced-pass, aggregator-ignores-fail, publisher-publishes-verified. They are one attack wearing six hats:
*the artifact that says PASS is written by the thing under test.* Remove the adjudication and none of them has
anywhere to live.

Two cheap additions cover the rest:
- **Evidence names its subject and the subject's sha256** → the reader re-hashes. Kills P8-03's zip-redirect,
  P8-10's env-redirect, P8-11's validator-relocation, P8-09's purge-removal — all currently invisible because
  **nothing records what was actually read**.
- **Pin the data denominators as literals** beside `CONTRACT_CHECK_COUNT`: 48 artifacts, 507 source entries, 49
  frozen keys, the blocking id set, a sha256 per property string. ~15 lines.

**One refactor + 15 lines of literals, versus 28 falsifiers each needing a 5-step re-seal, a baseline control and
a hostile-env arm.** The critic costed the latter in weeks.

## Why the twelve agents' own verdicts were the tell

**All twelve returned `FALSIFIERS_INSUFFICIENT`. None drew the conclusion.** Twelve independent verdicts of
"insufficient" is not twelve falsifier-design problems. It is one architecture problem. Nobody said *stop writing
falsifiers* — which is the same failure I made three times: fixing the reproduction instead of the property.
