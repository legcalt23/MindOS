# ENG-CORE + ENG-01 Focus — blueprint V3

**Supersedes:** V2 (`REVISION_REQUIRED` — reviewed concurrently with edits, so no verdict binds to known bytes)
and V1 (`BLUEPRINT_REJECTED`). Both retained immutable.
**Status:** `CANDIDATE` — frozen at the hash below. **Not edited while under review.**

```
DESIGN_REVIEW_GATE          = PENDING
IN_SCOPE_TESTS              = GREEN
REPOSITORY_GLOBAL_BASELINE  = RED_WITH_PREEXISTING_FAILURES
```

## 0. Finding classification

`DESIGN_BLOCKER` (unresolved design contradiction — the only class that blocks pre-code) ·
`IMPLEMENTATION_ACCEPTANCE_CRITERION` (checkable only after code; gates the exit, not the start) ·
`MEASUREMENT_ONLY` · `OUT_OF_SCOPE` · `DISPROVED`.

## 1. V2 classifications, corrected

B1/B2/B3 were called "resolved". They are not; no code exists.

| # | Was | Now |
|---|---|---|
| B1 trial budget | "resolved" | `DESIGN_RESOLUTION_SPECIFIED` / `IMPLEMENTATION_OPEN` |
| B2 buildReport | "resolved" | `DESIGN_RESOLUTION_SPECIFIED` / `IMPLEMENTATION_OPEN` |
| B3 renderer | "resolved" | `DESIGN_RESOLUTION_SPECIFIED` / `IMPLEMENTATION_OPEN` |
| "~130–160 trials after the fix" | `CODE_CONFIRMED` | **`DESIGN_PROJECTION`** — arithmetic over an unbuilt loop |
| "Offline" | `CODE_CONFIRMED` | generation-without-network only; **the offline route is `NOT_TESTED`** |
| "Accessibility — arrows" | `CODE_CONFIRMED` | proves independence from colour/reading **only**; full accessibility `NOT_TESTED` |
| "Difficulty — all `lerp`-bounded" | `CODE_CONFIRMED` | proves **mathematical** bounds, not cognitive monotonicity |
| "Every trial demands a response" | implied engagement | proves response density; says **nothing** about motivation |
| registry ⟺ renderer test | implied route | proves **static registration**, not an end-to-end route |

## 2. T1–T7 (psychometrician, verbatim scope) + B1–B3 — full list

| # | Finding | Class | Resolution |
|---|---|---|---|
| **T1** | §2's headline trial volume false as wired: `FocusSessionStateHolder.kt:195` calls `estimatedTrialCount()` with default `averageTrialMs = 3_800` (E1's budget, `SessionTemplate.kt:28`) ⇒ MAINTENANCE_5 = **55 trials ≈ 1.4 min**, not "100–150 / 3–5 min" | `IMPLEMENTATION_ACCEPTANCE_CRITERION` | §5 below: monotonic active-time termination. Claim retyped `DESIGN_PROJECTION` |
| **T2** | Per-condition descriptives do not exist: `MetricKey.kt:29` has only `CONGRUENCY_EFFECT`; no CONGRUENT_RT / INCONGRUENT_RT / per-condition ACCURACY; `sessionMetrics` (`DefaultMetricsEngine.kt:111-135`) emits none. The gold's entire feedback deliverable has no code path | `IMPLEMENTATION_ACCEPTANCE_CRITERION` | §4: E2 metric policy adds per-condition keys with per-cell `sampleSize` |
| **T3** | **Training ladder moves on a fabricated 0.0.** `valueOf(key, default = 0.0)` (`MetricKey.kt:75-78`) returns 0.0 when unconfident; `AdaptiveEngine.kt:32` consumes it; `:67` `accuracy < accDownThreshold (0.65)` ⇒ **StepDown**. Sealing `MetricResult` does not close it — the **default-taking accessor** is the leak | `IMPLEMENTATION_ACCEPTANCE_CRITERION` (**live product bug**) | §3: `Hold` contract, typed reason, no numeric default on the adaptive path |
| **T4** | One-key mashing: direction is Bernoulli(.5) (`E2FiltroGenerator.kt:45`) ⇒ ~50% HIT, **0 MISS**; RT stats take HIT RTs only ⇒ *excellent* RT_MEDIAN/RT_CV/BLOCK_STABILITY shown as feedback | `IMPLEMENTATION_ACCEPTANCE_CRITERION` | §6: factorial balancing + input-asymmetry quality gate. **Not** "cheating" |
| **T5** | §3 descriptor cannot reproduce an E2 session: missing resolved `DifficultyVector` (per-user mutable state — `difficultyMappingVersion` versions the *mapping*, not the *point on the curve*), `targetTrialCount`, `IndividualThresholds` | **partly `DESIGN_BLOCKER`** — now resolved in §7 | §7: `resolvedDifficultySnapshot` with effective values |
| **T6** | Golden test tautology + broken `neighbouring_seeds` | **`DISPROVED` as open** — **FIXED IN CODE** | Literals pinned (derived twice independently, agreeing); mutation-proven red; `CLAUDE.md` corrected |
| **T7** | Four invented indices live on the gold's path: `BLOCK_STABILITY` (`:419-430`), `FATIGUE_INDEX` (`:476-491`), `IES` (`:224-228`, guarded only at `acc <= 0.0` ⇒ returns 2×RT at 2AFC chance), `LISAS` (`:244-256`, two estimators one key), plus `LAPSES` (`:76`, a PVT threshold on a 900–1500 ms flanker window) | `IMPLEMENTATION_ACCEPTANCE_CRITERION` | §4: per-family allow-list. Hiding in UI is insufficient — they must not feed adaptation or persistence |
| **B1** | E1-calibrated trial budget | `DESIGN_RESOLUTION_SPECIFIED` / `IMPLEMENTATION_OPEN` | §5 |
| **B2** | `buildReport` exercise-blind (`FocusSessionAssembler.kt:134-149`) | `DESIGN_RESOLUTION_SPECIFIED` / `IMPLEMENTATION_OPEN` | §4 + §8 |
| **B3** | Renderer pop-out: target `FocusSignal` **60sp bold** vs flankers `FocusReadout` **44sp** | `DESIGN_RESOLUTION_SPECIFIED` / `IMPLEMENTATION_OPEN` | §8 |
| M1 | `formId` is provenance, not exposure; E2 has no item-exposure problem (~4-element stimulus space) | `MEASUREMENT_ONLY` | Overclaim withdrawn; sequence learning handled by a fresh unpredictable seed |
| M2 | Bernoulli congruency ⇒ cells 11±6 at n=55 | `MEASUREMENT_ONLY` → folded into §6 (factorial balancing serves T4 too) | |
| M3 | `RT_CV` is `sd(about mean)/median`, not a CV | `MEASUREMENT_ONLY` | Rename before any cross-study comparison; E2 does not use it (§4) |
| M4 | `FALSE_ALARMS` counts wrong-direction choices in E2 | `MEASUREMENT_ONLY` | Correct count, wrong construct; no SDT reading |
| M5 | `weightedAverage` = mean of medians under `RT_MEDIAN` | `MEASUREMENT_ONLY` | Moot at one block |
| M6 | Focus timebase gate | `MEASUREMENT_ONLY` | Unchanged |

## 3. T3 — adaptation is fail-closed

```
Available + sufficient          → StepUp / StepDown / Hold  (as measured)
Unavailable | insufficient | interrupted | quality-insufficient → Hold(reason)
```

`Hold` carries a **typed reason** and **preserves the previous difficulty**. `Unavailable` may never produce
StepDown *or* StepUp. The adaptive path takes **no** `default`-taking accessor; an exhaustive `when` replaces it.
A "safe-looking" accessor that still accepts `default = 0.0` is not retained.

## 4. T7 + T2 — per-family metric policy

E2's policy is an **allow-list**, not the universal Focus battery.

**E2 may produce:** trials presented · correct/incorrect · omissions · anticipations · **accuracy per condition**
· **median RT of correct responses per condition** · quality/sufficiency · distribution by target direction and
by response direction · **observed n per cell**.

**E2 must neither produce nor consume** (feedback *or* adaptation *or* cognitive persistence):
`LAPSES`, `RT_CV`, `BLOCK_STABILITY`, `FATIGUE_INDEX`, `IES`, `LISAS`, `SUSTAINED_FOCUS_INDEX`.
Suppressing them in UI alone is insufficient. `buildReport` reads E2's policy, never a fixed global list.

## 5. B1/T1 — duration is monotonic active time

No pregeneration sized by an assumed human RT. **Deterministic per-index stream / balanced deterministic
chunks**, so the stream cannot run dry and no supposed RT figure enters the design.

Session ends on **monotonic active time**: pause/background do not consume it; restoration preserves accumulated
active time; the in-flight trial may finish; maximum deviation declared and tested; progress UI is time-driven,
not trial-count-driven.

**Duration reconciliation, honestly:** `MAINTENANCE_5.coreMs = 210_000` = **3.5 min**, and the holder ignores
`template.warmupMs`/`cooldownMs` (30 s each) in favour of fixed 4 s / 2.5 s constants. There is no policy that
produces the missing 90 s. **The promise changes to the honest duration** — the label must state what the core
actually delivers. I will not write that "5 minutes means 5 minutes" over a 3.5-minute core.

Measured separately: total perceived · active trial time · tutorial/practice · countdown · feedback · pause.

Time-based termination may leave a **partial factorial block**. Metrics use **observed counts only** and mark
per-cell insufficiency; perfect balance is never assumed.

## 6. T4 — factorial balancing + input asymmetry

Generator balances `target(left|right) × (congruent|incongruent)` in factorial blocks — the minimum unit comes
from the design (4 cells), **not an invented number** — deterministically shuffled within each block.

Evaluated: responses per direction · accuracy per target direction · response imbalance/entropy · omissions ·
anticipations · RT of correct responses · **real observations per cell**.

If both directions were presented sufficiently and responses are effectively one key:
`QUALITY_INSUFFICIENT_OR_INPUT_ASYMMETRY` ⇒ `adaptation = Hold`, `derived feedback = Unavailable`.
**Never called cheating** — it may be disengagement, misunderstanding, or a motor limitation. The gate is derived
by binomial model/simulation, documenting expected false-positive rate, mashing sensitivity, behaviour for a
legitimate low-performer, and effect of incomplete samples.

## 7. T5 — FORM_REPRODUCTION vs SESSION_REPLAY

```
FORM_REPRODUCTION = seed + versions + resolved configuration      → same potential stimulus sequence
SESSION_REPLAY    = FORM_REPRODUCTION + trials actually presented + responses + lifecycle
                    + relative monotonic timings                   → same trajectory and same result
```

E2 was mislabelled "pure PREGENERATED_REPLAY". Even fully pregenerated, a session that **ends on time** and
accepts anticipations depends on actions and timing. **The seed alone never reproduces the session.**

Persisted descriptor:

```
replaySchemaVersion · sessionId · seedHex · rngVersionId · engineContractVersion · generatorVersion
exerciseId · exerciseVersion · difficultyMappingVersion · resolvedDifficultySnapshot
sessionTemplateVersion · resolvedTimingPolicy · responseMappingVersion · contentVersion · formId
```

`resolvedDifficultySnapshot` carries E2's **effective values**: congruency proportion, flanker count, response
window, ISI, feedback duration, and any further parameter altering trials or scoring. **Never** a reference to
the mutable `DifficultyVector`, never only the mapping version. `formId` is determined by the descriptor's
**canonical serialization**.

## 8. B2/B3 — product

**Feedback (plain language, session-only):** overall accuracy this session · accuracy with aligned vs opposed
distractors *when sufficient* · typical pace per condition *when timing valid* · omissions/anticipations
formatively · insufficient-quality when applicable. **Never:** capacity · cognitive index · longitudinal growth ·
official score · congruency effect as a trait · vigilance lapses · inferred fatigue.

**Renderer:** target and flankers identical in size, weight, colour, contrast and visual family — **central
position is the only cue**. Prefer own vectors over Unicode glyph rendering. Checks: odd count and centrality ·
spacing on small screens · font/display size · RTL/localization · reduced motion · contrast · **accessible
semantics must not reveal the correct answer**. If the task cannot be equivalent under a screen reader, the
dossier says so honestly and Focus must offer an accessible alternative — two modalities training different
mechanisms are never called equivalent.

## 9. Persistence — contradiction resolved

V2 said identity writes in `SessionRecord`'s write-ahead path **and** that `SessionRecord` cannot hold it. Both
cannot stand without an atomicity contract.

**Decision: reproducibility is transversal to all eight engines, not a Focus concept.** Preference taken:
`SessionRecord` gains an **optional, versioned `ReproducibleSessionIdentity`** — additive, migrating, no
per-block identity stores, no destructive change to `SecureKeyValueStore` (whose API foreign untracked tests
consume). One record ⇒ one write ⇒ atomicity is free; no sidecar, no split-brain. A sidecar is used **only** if
additive extension proves concretely impossible, and then must satisfy: bound by `sessionId` · written
atomically with the write-ahead record or with demonstrated transactional recovery · no started session without
identity · no orphan identity · multi-session · local/guest as well as authenticated · erased with the account
and with local deletion · encrypted · offline.

## 10. Out of scope

V2d · Memory · the other 12 generators · official scoring · `d′` · `core/session` redesign · repairing foreign
WIP · time-of-day collection · exposure ledger · E1's inert axis · PVT-B constants.
