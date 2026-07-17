# ENG-CORE + ENG-01 Focus — blueprint for review

**Status:** `BLUEPRINT_REJECTED` — `efficacy-guardian: NO APTO`; `psychometrician: REVIEW_INCOMPLETE`.
**Authority:** CEO pivot directive 2026-07-15 §1.1, §2.1, §3, §9.
**Baseline:** HEAD `71e17038`. Every fact below was read out of the repo, not assumed.

---

## VERDICTS — read before the blueprint body

Per §9, a `NO APTO` blocks implementation of the reviewed design. **ENG-01 Focus is not implemented and must not
be, until the blockers below are closed.** The blueprint body is preserved unedited below except where it stated
something false (§2.3, corrected in place and marked); it is a rejected artifact, not guidance.

### `efficacy-guardian` — **NO APTO**

Eleven blockers. Four were verified against the code before being accepted — none were taken on the reviewer's
authority:

| ID | Finding | Verification |
|---|---|---|
| **F6** | **Zero data yields a *perfect* score.** `RtStatistics.median([]) = 0.0` ⇒ `rtCv = 0.0` ⇒ `BLOCK_STABILITY = 1.0` ⇒ `SUSTAINED_FOCUS_INDEX = 100.0`. A user who never responds is rated maximally focused. The charter forbids fabricating a 0; this is its inversion, and flattery is what a training product is structurally tempted to ship. | **CONFIRMED — reproduced as a failing test**, then fixed. `FocusIndexHonestyTest` |
| **F4** | E1 declares `governingAxis = TIME_ON_TASK`; `E1DifficultyMapping` never reads `timeOnTask`. The governing axis is inert — the difficulty-engine twin of a detector with no call-site. | **CONFIRMED** — `grep -c timeOnTask E1VigiaGenerator.kt` = **0** |
| **F7** | `SUSTAINED_FOCUS_INDEX = 100·(1−rtCv)·(1−lapseRate)` omits `FALSE_ALARMS`, which E1 declares a primary metric. On a rare-signal detection task, tapping constantly raises the index. No d′/criterion anywhere. | **CONFIRMED** — `addFocusIndices` never receives false alarms |
| **F1** | The slice scoped out seed persistence while §2 claimed it closed the reproducibility gap. Without a persisted seed, replay only replays sessions that never happened. | **ACCEPTED** — §2's claim was overreach |
| **F2** | **The near-neighbour-seed claim in §2.3 is wrong.** XorWow scrambles the seed; `Random(t)` and `Random(t+1)` are unrelated. Entropy fixes predictability, **not** exposure. | **ACCEPTED — my error.** Corrected in §2.3, in `PlatformSeedSource`'s KDoc, and in `DeterministicRngGoldenTest` |
| F3 | `SessionReplay` output must be type-level non-scoreable, enforced by a build scanner | ACCEPTED — not built |
| F5/Q3 | E1 is PVT metrics on a Mackworth schedule. At 3 min it yields **~16 RT samples at the easy end (which is simple RT, not vigilance) and ~2 at the hard end** — the window where E1 measures sustained vigilance in 3–5 min is **empty**. Harder ⇒ more vigilance-like ⇒ fewer samples: the axis that raises validity destroys the sample. Fix = PVT-B (signal every trial, ISI 1–4 s, lapse threshold **355 ms** not 500, ≥40 valid RTs); **drop `VIGILANCE_DECREMENT_SLOPE`** at product block lengths | ACCEPTED — not implemented |
| Q4 | Shared seed ⇒ shared schedule ⇒ implicit sequence learning inflates RT on E1, whose core manipulation *is* temporal uncertainty. Six-point fail-closed exposure policy required | ACCEPTED — not implemented |
| — | `MIN_N_CONGRUENCY = 3` — confident congruency/switch-cost from 3 trials per cell. Affects E2/E5/E6 today | **CONFIRMED** at `DefaultMetricsEngine.kt:466` |
| Q6.4 | Time-of-day/sleep is the dominant PVT confound and is recorded nowhere. Like the seed, **unrecoverable retroactively** | ACCEPTED — not implemented |
| — | `vigilanceDecrement` regresses RT on **hit ordinal**, not elapsed time, with ISI randomised 1200–7000 ms — the x-axis is "which hit", not "when", yet the unit is reported as `SLOPE` | ACCEPTED — not fixed |

**Gold exercise ruling: E1 Vigía, conditional on F4/F5.** E4 disqualified (its SSD staircase is *online* ⇒ **not
seed-replayable by construction**, which is self-defeating for the exercise meant to prove determinism; SSRT not
estimable in 3–5 min; strategic slowing gameable unsupervised). E5 disqualified (colour + reading dependency
fails accessibility; `CONGRUENCY_EFFECT` is a difference score with ICC ≈ 0.4–0.6 — the reliability paradox —
unusable for tracking an individual). E2 = runner-up and second exercise. E1 wins because lapses/RT-CV are **not**
difference scores and PVT is among the most reliable cognitive measures available — but it is a **state** measure
dominated by sleep and circadian phase, which makes it the worst candidate for showing longitudinal progress.

**Credit the reviewer gave, which I had missed:** `FocusProfileSource.kt:55-61` is a correctly-built fail-closed
gate, and it is the only reason F6/F7 are not live profile breaches today. ATTENTION stays CALIBRATING because
that gate denies everything. The blueprint should have said so.

### `psychometrician` — **REVIEW_INCOMPLETE, not APTO**

The agent **terminated with an API error mid-response** and returned a fragment, not a verdict. There is no
psychometric sign-off on this blueprint. It must be re-run before ENG-01 proceeds. A review that did not finish
is not an approval, and is not counted as one here.

---

## 1. Baseline — what actually exists (verified, not claimed)

The premise "build eight engines" does not survive contact with the tree. MindBoost is **not** greenfield:

| Block | commonMain files | lines | Reachable from hub? | Exercise ids in code |
|---|---:|---:|---|---|
| Focus | 110 | 11,652 | yes — `FocusRootScreen` | **13 wired end-to-end** (E1…E13) |
| Speed | 67 | 8,869 | yes — `SpeedRootScreen` | 8 (E1…E8) |
| Memory | 94 | 8,328 | yes — `MemoryRootScreen` | EX1…EX? |
| Emotions | 83 | 6,641 | yes — `EmotionRootScreen` | 15 catalogued, 6 playable |
| Language | 56 | 5,326 | yes — `LanguageRootScreen` | 6 |
| Decisions | 28 | 2,953 | yes — `DecisionsRootScreen` | 3-stage flow |
| Creativity | 24 | 1,473 | yes — `CreativityRootScreen` | 2 |
| Logic | 15 | 2,222 | yes — `LogicRootScreen` | several |
| **core/** | 64 | 5,919 | — | shared across all 8 |

Tests: **424 files** (175 desktopTest + 249 commonTest) under `mindboost/`.

**A shared runtime already exists** and all eight blocks import it: `core/session` (lifecycle, write-ahead,
recovery, revocation, per-user encrypted persistence), `core/scoring` (`ScoreStateMachine`, `MinSampleGate`,
sealed `ScoreResult`), `core/antigaming` (11 detectors + red-team suite), `core/bank` (`BankIntegrityAuthority`),
`core/timing` (`FrameTimingProbe`), `core/copy` (`ForbiddenLexicon` + build scanners).

`validate-portfolio --mode=repo` reproduces honestly: `TRAINING_READY 0/121`,
`121/121 slots have no proven per-exercise route`, `37 slots have no assignment`, `56 code-assigned slots`.
Both are true simultaneously: **substantial product exists; no exercise has passed the CEO's §10 DoD.**

### Consequence for the plan

Building eight fresh engines beside eight real ones would produce exactly what the directive forbids —
*ocho motores vacíos*. **ENG-CORE must be extracted from the mature block and proven on it, not invented.**
Focus is the most mature (110 files, 13 exercises, a registry with a consistency test that fails the build if a
generator is wired without a render path). This is why the order below starts at Focus.

---

## 2. The real gap — determinism is architecturally possible and operationally absent

The Focus generator contract is **already pure**:

```kotlin
interface TrialGenerator {
    val exerciseId: ExerciseId
    fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus>
}
```

A session's entire stimulus sequence is a pure function of `(difficulty, spec, seed)`. But:

```kotlin
// FocusRootScreen.kt:42
var sessionSeed by remember { mutableStateOf(graph.epochClock.nowMillis()) }
// FocusRootScreen.kt:63
onStart = { exerciseId, template -> sessionSeed = graph.epochClock.nowMillis(); … }
```

The seed is **wall-clock, minted in a composable, and never persisted**. `SessionRecord` has no seed field.
`grep` for replay/seed-persistence across `mindboost/` returns nothing.

Four consequences, all load-bearing:

1. **No session is reproducible.** A real session cannot be replayed, audited, or pinned by a golden test. The
   CEO's §8 mandates ("Seed reproducible", "Replay", "Golden tests", "≥10.000 seeds") are currently unmeetable.
2. **No restoration.** Process death loses the sequence; the session cannot resume on the same stimuli.
3. ~~**Correlated seeds.** `Random(t)` and `Random(t+1)` for wall-clock `t` are not independent draws.
   Consecutive sessions are near-neighbours in seed space.~~ **RETRACTED (F2) — this was false.** XorWow and
   splitmix64 both scramble the seed into state, so `t` and `t+1` produce statistically unrelated streams. The
   true defect is narrower: sessions started **in the same millisecond receive identical sequences**, and it is
   undetectable because the seed is not stored.
4. **Predictable seeds.** Wall-clock is observable to the user. A predictable seed is a gaming surface for any
   exercise where knowing the sequence helps (target position, go/no-go schedule).
5. **Exposure is not computable at all.** With no persisted seed, the item history of every session ever run is
   unrecoverable, so practice/exposure can never be separated from improvement — retroactively or otherwise.
   **Entropy is not exposure control**: `SeedSource` fixes 4, and does nothing for this.

**Additional determinism trap:** `kotlin.random.Random(seed)` returns `XorWowRandom`. Stable in practice; the
stdlib **does not contractually guarantee the algorithm across Kotlin versions**. A golden test or a replay that
must hold across a Kotlin upgrade cannot rest on it. A pinned, versioned PRNG (~40 lines) removes this.

---

## 3. ENG-CORE proposal — the kernel, scoped to what is missing

Only the genuinely-missing transversal capability. Everything already shared (lifecycle, persistence, scoring
state machine, anti-gaming, bank integrity, timing, copy safety) is **reused, not rebuilt**.

```
core/runtime/
├── SessionSeed          value class; the seed + its RngVersion. Never a bare Long.
├── RngVersion           enum; V1 = the pinned algorithm. A seed is meaningless without it.
├── DeterministicRng     pinned splitmix64-derived PRNG. Same (seed, version) ⇒ same stream, forever.
├── SeedFactory          mints seeds from injected entropy — NOT wall-clock. Testable.
└── SessionReplay        (seed, version, difficulty, spec) → the exact stimulus sequence.
```

Invariants proposed:
- A `SessionSeed` carries its `RngVersion`; replaying under a different version is an error, never a silent
  re-roll.
- `SeedFactory` takes an entropy source by injection; production supplies a non-wall-clock source; tests supply
  a fixed one. Wall-clock is never a seed.
- `DeterministicRng` is pinned by golden vectors — if the algorithm ever changes, the test fails, not the users.
- Replay is a **pure** function. No clock, no IO, no platform.

Explicitly **out of scope** for this slice (declared, not silently skipped): persisting the seed into
`SessionRecord` (schema change on verified persistence — needs its own slice with migration + erasure review),
typed cross-block event bus, difficulty contract unification.

---

## 4. ENG-01 Focus — proposed contract

Focus already has: 13 generators + registry, `TrialClassifier` (go/no-go, choice, stop-signal),
`FocusSessionAssembler` (the authority on outcome/metrics), `DefaultMetricsEngine` (RT median, RT-CV, lapses,
anticipations, omissions, IES, SSRT), difficulty adaptation, `SessionRecorder` write-ahead, encrypted persistence.

It lacks a **stable versioned contract** and **proven determinism**. The proposal is a facade over the real code
that (a) names the engine's capabilities, (b) takes `SessionSeed` instead of a bare `Long`, (c) makes replay a
first-class operation.

## 5. Gold exercise — candidates, for the reviewers to rule on

I am **not** pre-selecting. CEO §12 criteria: cognitive value, clarity, content generation, no permissions,
offline, accessibility, ability to demonstrate the engine.

| Candidate | Construct | Generation | Response | Demonstrates |
|---|---|---|---|---|
| **E1 Vigía** | sustained vigilance (SART-like), time-bounded | fully parametric, infinite | single-tap | scheduler, response window, lapse/omission classification, vigilance decrement, RT+accuracy separated, pacing |
| **E2 Filtro** | selective filtering (flanker) | parametric | choice L/R | interference, distractor similarity, congruency effect |
| **E4 Freno** | response inhibition (stop-signal) | parametric + **online** staircase SSD | single-tap + withhold | SSRT, online difficulty, the hardest lifecycle |
| **E5 Conflicto** | interference control (Stroop/Simon) | parametric | choice L/R | congruency, conflict adaptation |

### Questions I need the reviewers to answer

1. **Which candidate is the right gold exercise**, on construct value and on ability to demonstrate the engine?
2. **E1's dependency:** vigilance decrement needs a long block. Does a session short enough to be usable
   (~3–5 min) still measure sustained attention, or does it collapse into simple RT?
3. **Seed independence:** is there a psychometric objection to correlated/predictable seeds beyond the gaming
   surface — e.g. does a near-neighbour seed produce a materially similar sequence (practice/exposure contamination
   across consecutive sessions)?
4. **Replay and measurement:** if two sessions share a seed, they share the stimulus sequence. Is that a
   *feature* (parallel forms / test-retest) or a *confound* (item exposure)? What must the exposure policy be?
5. **Claims:** what may a Focus session honestly tell the user after one block? The product is `TRAINING_ONLY`;
   no official score, no global index, no transfer claims. Is "practice" framing sufficient, or does even
   session-level feedback overclaim without a baseline?
6. **Confounds** in the chosen candidate that the current metrics do not separate.

## 6. Non-negotiables carried from the charter

`null ≠ 0`. Insufficient data ⇒ CALIBRATING / "—", never a fabricated 0. `TRAINING_ONLY`; no official scoring,
no global index, no transfer claims, no far-transfer language. `dataSufficiency` ⊥ `confidence`. The eight blocks
are **visible interfaces and functional groupings** — not eight independent intelligences, not eight validated
psychometric factors.
