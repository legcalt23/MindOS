# MindBoost Portfolio — Authority Map (V2d Phases 2 + 3 + 4)

> **Status (V2d Phase 4 applied).** An earlier revision claimed `SEED AUTHORITY REMOVED: YES` and
> `PHASE 2: PASS` while admitting the CSV still carried the seed's status columns and that
> `render.mjs`/`validate-portfolio.mjs` still consumed them. That was self-contradictory and was withdrawn:
> renaming `seed-portfolio.mjs → .RETIRED` proves the **executable** no longer participates in new
> generations; it proves **nothing** about the data it already materialised. Phase 4 finished the job.
>
> | | |
> |---|---|
> | `SEED EXECUTABLE RETIRED` | **YES** (Phase 2) |
> | `SEED-DERIVED LEGACY COLUMNS ACTIVE` | **NO** (Phase 4 — retired to `PORTFOLIO_V2C_LEGACY_CLAIMS.csv`) |
> | `PHASE 2 INTEGRATION_GATE` | **PASS** (closed by Phase 4) |
> | `MB-PORTFOLIO-0014` | **CLOSED** (all six closure criteria met with gate evidence) |
>
> The active CSV went from 49 columns to **16 editorial columns**. The 33 technical columns — whose real
> authority is code-observation, runtime/QA or scientific review — were moved, not deleted, to
> `PORTFOLIO_V2C_LEGACY_CLAIMS.csv` (`role=HISTORICAL_EVIDENCE`, `disclaimer=HISTORICAL_UNVERIFIED_CLAIMS`,
> carrying the source hash and the V2c snapshot reference). They are the provenance of the published
> "43 live / 44 mechanic" numbers — preserved so those numbers can be **traced**, not so they can be
> **believed**. `PORTFOLIO_ACTIVE_SCHEMA_GATE` rejects any of them returning as active input;
> `LEGACY_ISOLATION_GATE` rejects any active tool parsing them.
>
> Tests F5/M5 ("mutating the retired file changes nothing") prove **absence of an execution dependency
> only**. They are structurally incapable of detecting materialised-output governance — do not cite them as
> evidence of integration. What closed `MB-PORTFOLIO-0014` was P4-01/P4-02: reintroducing the legacy columns
> and proving the metrics are **byte-identical** while the schema gate goes red.

**Purpose.** V2c had one origin for both the claim and its verification: `seed-portfolio.mjs` hand-asserted
implementation status, wrote it into the CSV, and the validator re-checked the CSV against the CSV. This
document defines a **distinct authority per class of fact**, so no authority can attest another.

**Core principle.** Incomplete evidence must never become false certainty. The V2c encoding was
`"my probe did not find the relation" → false`. The correct encoding is
`"my strategy cannot resolve this architecture" → UNRESOLVED`.

---

## 1. The four authorities

| # | Authority | Owns | Artifact | May NOT do |
|---|---|---|---|---|
| 1 | **Target-slot (editorial)** | the 121 slots; `slotId`, `block`, `canonicalExerciseId`, `codeExerciseId`/`designExerciseId` assignments, `canonicalFamilyId`, manually approved identity and relations | `MINDBOOST_EXERCISE_PORTFOLIO.csv` | assert implementation, runtime or scientific facts |
| 2 | **Code-observation** | static observations only: declaration, mechanic/generator, renderer, route, root/hub — each with the symbol and hashed file that supports it | `PORTFOLIO_CODE_OBSERVATIONS.json` (from `observe-code.mjs`) | emit implementation status, readiness, runtime verdicts or scientific judgement |
| 3 | **Runtime/QA** | `NOT_RUN` / `VERIFIED` / `FAILED` / `BLOCKED_PLATFORM` — sourced **only** from executed tests, traces or dossiers | `PORTFOLIO_RUNTIME_EVIDENCE.json` (schema v1.0.0, `runs: []`) | ever be derived from static presence |
| 4 | **Scientific/design** | distinctness, construct validity, scientific approval | reviewed evidence (manual) | be inferred from code |

`PORTFOLIO_DERIVED_MODEL.json` **aggregates** them. It is a view, not an authority: it introduces no fact
that its inputs do not carry, and it cannot be more certain than its weakest axis.

---

## 2. Epistemic states

Booleans are banned on any axis where the absence of a token does not prove the absence of the function.

| State | Meaning | Precondition |
|---|---|---|
| `VERIFIED_PRESENT` | a **semantic binding acceptable for this axis** was observed | a bare code token is **not** sufficient (C5). If the strategy only knows "the identifier appears in code", it emits `UNRESOLVED` / `CODE_TOKEN_ONLY_NO_SEMANTIC_BINDING`. |
| `VERIFIED_ABSENT` | genuinely not there | requires `exhaustive: true` **and** an `exhaustivenessEvidenceId` naming a closed-world source set (C6). `exhaustive: true` alone is a self-assertion and proves nothing. **`VERIFIED_ABSENT_ALLOWED_STRATEGIES: 0`** — no strategy currently qualifies, which is safer than a self-attested flag. |
| `UNRESOLVED` | the strategy cannot resolve this architecture, or a declared source is missing | the default for every miss |
| `NOT_APPLICABLE` | the axis has no meaning for this block | — |

Accepted constructs per axis (`AXIS_CONSTRUCTS`): enum member, map entry, `when`-branch/lambda, collection
entry (`listOf`/`setOf`/`mapOf`), property declaration, type declaration. An **incidental reference cannot
accredit any axis**.

Recorded per exercise **and per axis**: `state`, `evidencePath`, `evidenceSha256`, `symbol`, `construct`,
`matchRange`, `axisSupported`, `strategyId`, `strategyVersion`, `exhaustive`, `exhaustivenessEvidenceId`,
`evidenceDependency`, `reasonCode`, `reason`, `limitations`.

**Axes are never collapsed into one "wired".** `declaration`, `mechanic`, `generator`, `renderer`,
`exerciseRoute`, `blockReachability` are separate.

**`evidenceDependency`** (C4): when two axes rest on the same `path::symbol::matchRange`, both are marked
`SHARED`. Two labels over one observation are **not** independent corroboration, and gate **G13** rejects
presenting them as such.

---

## 3. Conservative derivation

**Component axes** (the only ones the aggregate uses): `declaration`, `mechanic`, `generator`.
**Reported separately, never folded in**: `exerciseRoute`, `renderer`, `blockReachability`.

**Block route ≠ exercise route (C3).** `blockReachability` (root screen exists + hub references the block)
is **context only**. It is identical for every id in a block, so it can never evidence that a *given*
exercise is reachable. Without an id→dispatch→session/renderer proof, `exerciseRoute = UNRESOLVED`.

Target-slot states — ontologically distinct, never collapsed (C2):

| State | Requires |
|---|---|
| `CODE_ASSIGNED_STATIC_VERIFIED` | a `codeExerciseId` **and** every applicable component axis `VERIFIED_PRESENT` |
| `CODE_ASSIGNED_STATIC_UNRESOLVED` | a `codeExerciseId` but some component axis `UNRESOLVED` |
| `DESIGN_ONLY_ASSIGNED` | a `designExerciseId` and **no** `codeExerciseId` |
| `TARGET_UNASSIGNED` | neither |

`CODE_ASSIGNED_STATIC_VERIFIED` means **static components observed**. It does **not** mean reachable,
playable or runtime-verified. There is **no `LIVE_REACHABLE`** anywhere in the model.

Enforced invariants: states sum to exactly **121**; design-only requires design∧¬code; unassigned requires
¬design∧¬code; auxiliaries never occupy or inflate a target slot; assigned candidates must reconcile with
the editorial `definitionStatus != UNASSIGNED` count.

Current reconciliation (reproducible via `derive-portfolio-model.mjs`):

```
CODE_ASSIGNED_STATIC_VERIFIED    9
CODE_ASSIGNED_STATIC_UNRESOLVED 56   → code-assigned 65
DESIGN_ONLY_ASSIGNED            19   → assigned candidates 84
TARGET_UNASSIGNED               37
                               ---
                               121   (auxiliaries excluded from the denominator)
```

`TRAINING_READY` remains **0/121**.

---

## 4. What the strategies can and cannot do

`kotlin-code-scan.mjs` strips comments, strings, raw strings and char literals so a token only counts in
**code position** — an id mentioned only in a KDoc or a string is not evidence (V2c counted it).

It is **not** a Kotlin parser and **not** a symbol resolver. It cannot distinguish a declaration from a
reference, cannot resolve imports, and cannot follow indirection. Every caller needing more than
"does this identifier appear as a code token here" **must** degrade to `UNRESOLVED`.

**Graphify/CodeGraph may be used for CANDIDATE DISCOVERY ONLY.** It is not wiring evidence, it cannot close
a finding, and it must not feed a portfolio state.

---

## 5. Block adapters (`block-adapters.mjs`)

V2c assumed one `wiredFile` and one `rendererFile` for every block. MindBoost has eight different
architectures, so those probes read the wrong file and emitted false negatives indistinguishable from real
absence (`MB-PORTFOLIO-0013`). Each block now declares its own strategy and its own limitations.

Known structural limitations, stated rather than hidden:

- **FOCUS** — the renderer is state-driven and **not id-addressable** (it dispatches on `FocusFieldState`, by
  design since the 2026-06-18 registry refactor). Its renderer axis is therefore permanently `UNRESOLVED`
  under token probing; resolving it needs call-graph analysis through `FocusSessionStateHolder`.
- **SPEED** — two dispatch patterns coexist (shared prompt generators; per-exercise screens one directory
  down). Both are probed; neither probe is exhaustive.
- **LOGIC / DECIDE / CREATE / LANG** — the root screen doubles as the renderer, so those two axes are not
  independent evidence.
- **DECIDE** — `DecisionsFlowStage` enumerates flow **stages** (CheckIn/BayesArena/Reversibility/Result),
  which are not the same ontological kind as exercise ids elsewhere. Treating them as exercises is an
  editorial modelling decision, not a code fact.
- **EMOTION** — the registry keys on generator **class** names; the id→class link is a numeric-stem
  convention, not a resolved reference.
- **MEMORY** — generator files are PascalCase (`Ex1SpanGenerator`) while ids are SCREAMING_SNAKE
  (`EX1_SPAN_COMPLEJO`); the stem match is case-insensitive and not exhaustive.
- **Route** — proves **block** reachability only (root screen exists + hub references the block). It does
  **not** prove that a given exercise is reachable inside its block.

---

## 6. Retired

`seed-portfolio.mjs` → `seed-portfolio.mjs.RETIRED` (kept as historical evidence of where the V2c
"43 live / 65 code-declared / 44 mechanic" numbers came from; renamed so it cannot be imported or run).
`build-portfolio.mjs`, `render.mjs` and `validate-portfolio.mjs` do not reference it. Tests F5/M5 prove
rewriting every literal in it changes no derived result.

---

## 7. Phase 4 — the orthogonal, non-shadowing state model

V2c carried one monolithic `implementationStatus` answering seven unrelated questions, dispatched through an
`if / else if` chain. Both are gone.

**Orthogonal.** Each question has its own axis and its own authority: `assignment` ·
`staticComponents{declaration, mechanic, generator}` · `renderer` · `exerciseRoute` · `blockReachability` ·
`runtime` · `platform{android, ios}` · `qa{11 axes}` · `scientificDistinctness` · `measurement` · `readiness`.
A row may be VERIFIED on one and UNKNOWN on the next without either contaminating the other.
`STATIC_COMPONENTS_PRESENT` does not mean reachable, playable, tested, measurable or TRAINING_READY, and
`blockReachability` never satisfies `exerciseRoute`.

**Non-shadowing.** Fail-before F4-2/F4-3/F4-7 proved the V2c chain evaluated ONE category per row: a row that
was simultaneously UNASSIGNED, LIVE_REACHABLE, mechanic-less, route-less and generator-contradicting produced
**2 diagnostics, both from the assignment family, and none about "live"**. Invariant families are now a data
structure, not a branch chain — `validateSlot` runs every family against every row and returns every
diagnostic. P4-08 proves three simultaneous defects yield diagnostics from three distinct families.

**Derived, never read.** `TRAINING_READY` requires positive evidence on every axis; any `UNRESOLVED`,
`NOT_RUN`, `UNKNOWN` or `BLOCKED_PLATFORM` blocks it. It cannot be read from any file — a forged readiness in
the published model is caught by `TRAINING_READY_DERIVATION_GATE`, which names every blocking axis.
`RUNTIME_PLAYABILITY_GATE` is derived from the runtime authority (V2c hard-coded it, so a fully-formed
VERIFIED run left it unchanged — F4-4). Current: **TRAINING_READY 0/121**.

**One formula per metric.** `metrics.mjs` is the single registry: `compute` IS the formula, `formula` is its
rendering, `semantic` says what the number does *not* mean. `FORMULA_TRUTH_GATE` fails if either drifts from
the other (P4-13/P4-14). Render, validator and per-block counts all read this registry; nothing recomputes a
metric locally.

**Non-circular view.** `GENERATED_VIEW_GATE` (byte-exact) is backed by `MD_PUBLISHED_NUMBERS_GATE`, which
re-parses the published bytes and compares them to authority-derived values **without calling the renderer**
— so builder and validator sharing `renderPortfolio` can no longer be mutually consistent and wrong (F4-5).

## 8. Declared debt (NOT fixed by Phase 4)

- The scanner is still not a Kotlin parser: **renderer 0/121** and **exerciseRoute 0/121** remain UNRESOLVED
  pending call-graph resolution (`MB-PORTFOLIO-0013`, MITIGATED, not CLOSED).
- The Phase 3 bundle contains tooling and metadata only — **0 `.kt` files, no CSV** — so a clean-room cannot
  recompute a single source hash from it (`MB-PORTFOLIO-0017`). `PHASE_3_IN_REPO_GATE: PASS`;
  `PHASE_3_SOURCE_EVIDENCE_HANDOFF_GATE: PENDING`; `EXTERNAL_REPRODUCIBILITY: NO` until Phase 8 ships the
  source bytes.
- No exercise is closed, verified or approved by any of this. Building the extractor closes nothing.
