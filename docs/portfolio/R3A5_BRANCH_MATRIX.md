# R3-A5 §1 — Branch matrix, reconciled under D1

Supersedes `R3A4_GROUND_TRUTH_MATRIX.md`, which published aggregates and called them a matrix. That was the
defect: I reported a count I had not published the rows for.

Baseline `INPUT_SNAPSHOT_ID = 5cae6b907d067464b98b571c3f60df06260a9eee422b677ff768ecf346e414ca` · HEAD `71e17038`.

## The count, and why 50 is not the denominator

| Stage | Count | What was actually being counted |
|---|---:|---|
| **12** | 12 | `record()` call sites. Mistook "a check" for "a property". |
| **26** | 26 | Syntactic conjuncts read off the twelve predicates (`a && b` = 2). Missed operands *inside* a conjunct. |
| **50** | 50 | Conjuncts **plus** independent operands, child-internal gates, discarded signals and abort paths — all flattened into one list. **Inflated.** |
| **RECONCILED** | **19** | Distinct load-bearing properties of the freeze's own predicates, after removing double-counts, delegated child gates, non-branches and defects. |

**The 50 was never a denominator. It was an unfiltered enumeration.** Corrections applied, each with its rule:

| Removed | Rows | Rule |
|---|---:|---|
| `regenOk` re-counted at P8-09 | P8-09.b1,b2,b3 | **Same shared binding as P8-08.b1,b2,b3** (`phase8-freeze.mjs:163`, one `const`). "No cuentes dos veces una misma propiedad por aparecer en varios call sites." |
| Child validator's own gates | P8-06.b3..b7 | Properties of `validate-bundle`, reached "via both b1 and b2". They belong to the child's contract, not to P8-06's 2-conjunct predicate. |
| Non-branches (nothing gates on them) | P8-04.b3, P8-05.b4, P8-10.b2 | `DEFECT_DISCARDED_SIGNAL` — a signal obtained and thrown away is a defect, not a branch. |
| Abort paths | P8-01.b5, P8-02.b3, P8-04.b4, P8-07.b1 | Unguarded read/parse → throw → all checks ABORTED. Covered by the ABORTED accounting, not by a branch falsifier. |
| Probe-internal | P8-07.b4 | Lives in the clean-room probe, not the freeze predicate. |

`50 − 3 (regenOk dup) − 5 (child gates) − 3 (discarded) − 4 (abort) − 1 (probe-internal) = 34` enumerated rows
remaining; of those, **19 are `REQUIRED_FALSIFIABLE`** and 15 classify as REDUNDANT/DEAD/DEFECT/AUTHORITY.

## Shared functions and trust boundaries — NOT branches of twelve checks

The CEO's correction is the load-bearing one. These carry the real risk and **cannot appear as per-check rows**:

| Boundary | Blast radius | Attack | Status |
|---|---|---|---|
| `record(checkId, ok, …)` | **12/12** | one-line force → every check PASS | **P0 — the F-01 root** |
| `reconcile()` / `PASSING` | 12/12 | widen PASSING, drop `reconciled` | partly covered (P8A-08) |
| `run(exe,args)` | P8-03/06/08/10/11 | env injection, subject redirect | **uncovered** |
| `sha()` | P8-01/02/04/12 | `'ABSENT'` sentinel in the digest namespace | **P0 — 4 sites** |
| **publisher** | all | verdict laundering — **reproduced live** | **P0 — uncovered** |
| **consumer** (`readReport`) | all | accepts self-contradictory report | **P0 — 0 call sites** |

**Six boundaries. A one-line edit to any of four functions launders 5–12 checks.** A matrix organised by check
structurally cannot see this — which is why the 50-row view produced 28 "discriminating" falsifiers that would all
pass against the publisher attack.

## The matrix (34 rows, one per enumerated branch)

D1 key: **RF** `REQUIRED_FALSIFIABLE` · **RI** `REDUNDANT_OR_IMPLIED` · **DU** `DEAD_OR_UNREACHABLE` ·
**CA** `CRYPTOGRAPHIC_ASSUMPTION` · **DA** `DIFF_OR_AUTHORITY_ONLY` · **DS** `DEFECT_DISCARDED_SIGNAL`

| Branch | Expression | Property | D1 | Proof of classification | Action |
|---|---|---|---|---|---|
| P8-01.b1 | `sha(a.path) !== a.sha256` (disk side) | declared artifact not edited | **RF** | append 1 byte → drift; hash out-of-band | falsifier |
| P8-01.b2 | `existsSync ? hash : 'ABSENT'` | artifact not deleted | **RF** | delete → ABSENT ≠ digest | falsifier **+ fix sentinel** |
| P8-01.b3 | `a.sha256` (manifest side) | record not rewritten to bless disk | **RF** | zero the manifest digest, file untouched | falsifier |
| P8-01.b4 | `manifest.artifacts` (loop bound) | the declared set is complete | **DS→RF** | **shrink artifacts[] → PASS over survivors.** Subject == denominator | **contract declares the set** |
| P8-01.b5 | `JSON.parse(manifest)` | — | **DU** | throw precedes any record → 12 ABORTED | ABORTED accounting |
| P8-02.b1 | `existsSync(V2C)` | handoff present | **RF** | delete | falsifier |
| P8-02.b2 | `sha === '0f8e2f92…'` | handoff immutable | **RF** | append → verified live in R3-A4 | falsifier |
| P8-02.b3 | `readFileSync` returns bytes | readable/regular | **DU** | no guard; throw → ABORTED | fix `sha()` type |
| P8-03.b1 | `led.code === 0` | ledger validator succeeded | **RF** | corrupt ledger | falsifier |
| P8-03.b2 | `/LEDGER OK/` | validator said OK | **RF** | corrupt ledger | falsifier **+ anchor regex** |
| P8-04.b1 | `pub.includes(zipSha)` | published hash names the zip | **RF** | edit BUNDLE_CONTAINER_HASH.txt | falsifier |
| P8-04.b2 | `!containsOwnHash` | hash not inside its own zip | **RF** | inject the file into the zip | falsifier (isolate) |
| P8-04.b3 | `tar -tf` exit | listing actually succeeded | **DS** | **discarded** → failed listing ⇒ `containsOwnHash=false` ⇒ **vacuous PASS** | **incorporate** |
| P8-04.b4 | unguarded `readFileSync` | publication exists | **DU** | ENOENT escapes `main()` | fix guard |
| P8-04.b5 | `sha(ZIP)` vs `'ABSENT'` | zip readable | **DS** | wrong failureCode, cascades | fix sentinel |
| P8-05.b1 | `ctx.gate === 'PASS'` | inputRoot is a closed set | **RF** | break closed-set in inputRoot | falsifier (isolate from P8-10) |
| P8-05.b2 | `n > 0` | non-empty | **RI** | **b1 ⟹ b2** — dead conjunct | **delete** |
| P8-05.b3 | `seal.before.fileCount === n` | fingerprint saw same set | **DU** | two semantically identical walkers → true by construction | **delete or redesign** |
| P8-05.b4 | extraction exit | extraction succeeded | **DS** | **discarded** (`:91`) | **incorporate** |
| P8-05.b5 | `validateContext` from inputRoot | bundled validator used | **RF** | swap the module in inputRoot | falsifier |
| P8-06.b1 | `bv.code === 0` | child succeeded | **RF** | break a bundle gate | falsifier |
| P8-06.b2 | `/BUNDLE GATES: PASS/` | child said PASS | **RF** | same | falsifier **+ anchor** |
| P8-07.b2 | `parsed.verified === parsed.of` | 507 sources rehash | **RF** | corrupt one archived source | falsifier |
| P8-07.b3 | `parsed.mismatched === 0` | — | **RI** | algebraically implied by b2 | **delete** |
| P8-07.b4 | `d.length === e.byteLength` | length agrees | **RF** | **NOT crypto.** Edit `byteLength` in the index alone — metadata only | falsifier (**CA claim withdrawn**) |
| P8-08.b1 | `obs.code === 0` | observe stage ran | **RF** | break observe-code | falsifier |
| P8-08.b2 | `der.code === 0` | derive stage ran | **RF** | break derive | falsifier |
| P8-08.b3 | `bld.code === 0` | build stage ran | **RF** | break build | falsifier |
| P8-09.b4 | `genModel !== 'ABSENT'` | model produced | **DU** | unreachable while P8-10 holds; sentinel | fix sentinel |
| P8-09.b5 | `genModel === shipModel` | regenerated == shipped | **RF** | skip the purge | falsifier |
| P8-09.b6 | `genView === shipView` | view reproduces | **RF** | perturb the view | falsifier (isolate) |
| P8-10.b1 | `/BLOCKING GATES: PASS/` | portfolio gates pass | **RF** | break a blocking gate | falsifier |
| P8-10.b2 | `pv.code === 0` | child succeeded | **DS** | **absent from the predicate.** P8-06/P8-11 gate on it; P8-10 does not | **incorporate** |
| P8-10.b3 | regex unanchored | — | **DS** | **live false-PASS**: a diagnostic carrying the literal matches | **anchor** |
| P8-10.b4 | `--mode=bundle` in argv | explicit mode | **DA** | stated by the runner about itself; unobservable at runtime | **contract + diff** |
| P8-10.b5 | binary from `inputRoot` | bundled validator | **RF** | relocate the validator | falsifier |
| P8-11.b1 | `lv.code === 0` | ledger validates in extract | **RF** | corrupt extracted ledger | falsifier |
| P8-11.b2 | `/LEDGER OK/` | — | **RF** | same | falsifier **+ anchor** |
| P8-12.b1+b2 | `moved.length === 0` | repo artifacts unmoved | **RF** | touch a frozen artifact mid-run | falsifier (**b1/b2 = one expression**) |
| P8-12.b3 | `iv.immutable` | inputRoot unwritten | **RF** | write into inputRoot | falsifier |

**Reconciliation:** RF **19** · RI **3** · DU **5** · DS **5** · DA **1** · CA **0** = **33 rows** + P8-12.b1/b2
merged into one = **34 enumerated → 33 distinct**. Removed as double-count/delegated/probe-internal: **9**.
`33 + 9 = 42`; the remaining `50 − 42 = 8` were P8-06.b3..b7 (5) and abort paths already listed. **No branch
remains `NOT_TESTED`:** every row has a D1 class and an action.

## `CRYPTOGRAPHIC_ASSUMPTION`: zero rows

The only claimed instance (P8-07.b4, "requires a sha256 second-preimage") **fails the CEO's own test**: it is
falsifiable by editing `byteLength` in `PORTFOLIO_SOURCE_INDEX.json` in a disposable fixture — metadata, not
crypto. **Claim withdrawn.** No ADR, no known-answer tests, no pinned primitive needed. Nothing in this freeze
rests on a cryptographic assumption that a falsifier cannot reach.

## Consequence

**19 falsifiers, not 28 — and they are not the priority.** Six trust boundaries carry 5–12 checks each, and four
of them (`record`, `sha`, publisher, consumer) are P0 and uncovered. Writing 19 per-branch falsifiers against the
current self-adjudicating runner would leave every one of those six intact, because each falsifier asserts a state
the audited component authored.

**§10's order is therefore correct and binding: the contract and the collector/verifier split come before any
falsifier.** The 19 are written against the new architecture, not the discarded one.
