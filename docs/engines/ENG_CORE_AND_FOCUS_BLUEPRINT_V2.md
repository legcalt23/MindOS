# ENG-CORE + ENG-01 Focus — blueprint V2

**Status:** `CANDIDATE` · **Supersedes:** `ENG_CORE_AND_FOCUS_BLUEPRINT.md` (`BLUEPRINT_REJECTED`, kept immutable as
a historical artifact — do not edit it further).
**Authority:** CEO minifase ENG-01, 2026-07-15. **Baseline:** HEAD `71e17038`.

---

## 0. Preflight — the record, corrected

### 0.1 The contradiction, resolved against me

Two statements were made last slice:

> "Now the real product change — wiring it in, because a pinned RNG nothing calls is exactly the empty engine the
> directive forbids." *(narration, mid-slice)*
> "`core/runtime` currently has **no production consumer** — the generators still call `Random(seed)`."
> *(close-out §9)*

**The close-out is true; the narration was an intention I announced and then did not carry out.** I measured the
blast radius of wiring 13 generators, judged it too large for the remaining budget, pivoted to the F6 metrics
fix, and never returned. I did not say so at the pivot. Verified now:

```
production consumers of DeterministicRng / SessionSeed / SeedSource / RngVersion : 0
test consumers                                                                    : 1  (DeterministicRngGoldenTest)
```

`ENG_CORE = PARTIAL_UNINTEGRATED` is the correct state and I accept it.

### 0.2 Exact inventory of the previous minifase

| File | State | Kind |
|---|---|---|
| `core/runtime/SessionSeed.kt` | `??` new | seed + version + SeedSource |
| `core/runtime/DeterministicRng.kt` | `??` new | pinned splitmix64 |
| `desktopTest/core/runtime/DeterministicRngGoldenTest.kt` | `??` new | 8 tests |
| `focus/domain/metrics/DefaultMetricsEngine.kt` | ` M` | **the only production class modified** |
| `commonTest/focus/domain/metrics/FocusIndexHonestyTest.kt` | `??` new | 3 tests |
| `docs/engines/`, `docs/portfolio/V2D_PAUSED…md`, `CLAUDE.md` | new / ` M` | docs |

Production diff, in full — two call sites and two extracted functions:
`BLOCK_STABILITY = MetricValue(1 - rtCv, …)` → `blockStability(rtCv, cleanRts.size)`;
`addFocusIndices(result, rtCv, lapses, rawHitRts.size)` → `+ cleanRts.size`, with both returning
`MetricValue.unavailable` at zero clean RTs. **Reverted changes: none.** No other production class was touched.

### 0.3 Repository baseline — honest

```
IN_SCOPE_GATES              = GREEN
REPOSITORY_GLOBAL_BASELINE  = RED_WITH_PREEXISTING_FAILURES
```

`:shared:desktopTest` = **3678 tests, 17 failures, 0 errors**, in exactly 6 classes, all untracked and **not in
HEAD**:
`CognitiveSessionEvidenceAdversarialTest`, `CognitiveSessionEvidenceExtractorTest`,
`CognitiveSessionEvidencePerformanceTest`, `CognitiveSessionEvidenceRtCriticalityTrainingFreeTest`,
`EvidencePublicShapeGoldenTest`, `SessionEvidenceStatePolicyTest`.
Proven not mine: reverting my `DefaultMetricsEngine` hunks to HEAD leaves them failing identically.

`:shared:detekt` = **RED**, 6 findings, **0 in my files**, all in foreign in-progress work:
`FirestoreDefaults.kt` (`LongParameterList` on a new `retryTransient`, `MagicNumber`, `MayBeConst`) and
`TimeoutDefaultsTest.kt` (`TooGenericExceptionThrown` ×2, `UseCheckOrError`).

**Do they affect the Focus route? Tested, not assumed.**

- **Evidence failures — NO.** `focus/**` imports `core.session.cognitive` **0 times**. The dependency runs the
  *other* way: the failing tests import `focus.data.local.{SecureKeyValueStore, InMemorySecureKeyValueStore,
  SecureReadResult}`. **This is a live constraint on this minifase, not a free pass:** seed persistence must
  **add to** that API, never change its shape, or it breaks foreign WIP. Recorded as a design constraint in §3.
- **Detekt findings — NO for the vertical.** `FirestoreDefaults` is imported by exactly one Focus file,
  `data/sync/GitLiveFocusSessionFirestoreGateway.kt` — the **cloud-sync** seam, not the offline training route
  the gold vertical proves. All six findings are style-level; both compile targets exit 0. Not fixed (§14: don't
  repair foreign work that doesn't block the vertical), and `:shared:detekt` is therefore **not claimable as
  green**.

I will not write "all gates green" while the repository baseline is red.

---

## 1. Resolution matrix — the eleven findings

Evidence types: `CODE_CONFIRMED` · `LITERATURE_SUPPORTED` · `DESIGN_JUDGMENT` · `DISPROVED` · `UNVERIFIED`.
"An agent said so" is not evidence and appears nowhere below.

| # | Finding | Type | Evidence | Resolution | Code/test | Gate |
|---|---|---|---|---|---|---|
| F6 | Zero data ⇒ 100/100 focus, 1.0 stability | `CODE_CONFIRMED` | Reproduced as a failing test: *"a user who never responded scored 100.0/100"*; `median([])=0.0` → `rtCv=0` → `1-0=1` | **Half-closed.** Fabrication removed. **Not closed:** `MetricValue.unavailable` still carries `value = 0.0` — the forbidden sentinel one layer down. §4 replaces it with a sealed `MetricResult` having **no numeric accessor** when unavailable | `FocusIndexHonestyTest` (3, incl. discriminating arm) → extend to `MetricResult` | `desktopTest` |
| F4 | E1 `governingAxis = TIME_ON_TASK`, never read | `CODE_CONFIRMED` | `grep -c timeOnTask E1VigiaGenerator.kt` = **0**; declared at `ExerciseCatalog.kt:31` | **Moot for the gold** (E1 is not the gold, §2). E1's inert axis stays an open P1 with an owner; **not silently repaired** | — | declared debt |
| F7 | Focus index omits false alarms ⇒ tapping wins | `CODE_CONFIRMED` | `addFocusIndices` never receives false alarms; E1 declares `FALSE_ALARMS` primary | **Retyped after review — the V2 draft papered this over.** `SUSTAINED_FOCUS_INDEX` is **already never displayed**: `FocusSessionAssembler.buildReport` (`:134-149`) emits a fixed four-line list (ACCURACY, RT_MEDIAN, RT_CV, LAPSES). Claiming "suppressed at the gold's feedback layer" was an action verb over a no-op, and it implied I had read a layer I had not. Honest row: **moot for the gold (index not displayed); the index stays wrong and is retained as debt for E1's future.** The real feedback defect is B2 below | — | declared debt |
| F1 | Slice scoped out seed persistence yet claimed to close reproducibility | `CODE_CONFIRMED` | 0 production consumers; `SessionRecord` has no seed field | **Land it in ENG-01.** Reproducible identity persisted **before the first trial** (§3) | `ReproducibleSessionIdentity` + write-ahead + restore tests | `desktopTest` |
| F2 | "Consecutive wall-clock seeds ⇒ near-neighbour sequences" | **`DISPROVED`** | **My claim, and it was false.** XorWow/splitmix64 scramble the seed; pinned by `neighbouring_seeds_do_not_produce_neighbouring_streams` | Retracted in V1 §2.3, `PlatformSeedSource` KDoc, and the test. Real defects kept: **same-millisecond collision** + **predictability**. **Entropy ≠ exposure control** | existing golden test | `desktopTest` |
| F3 | Replay must be non-scoreable | `DESIGN_JUDGMENT` | — | **Type-level** (§5): distinct replay type, persistence API refuses it, idempotent by `sessionId`. Scanner is defence-in-depth only, never the primary barrier | replay-cannot-persist tests | `desktopTest` |
| F5/Q3 | E1 = PVT metrics on a Mackworth schedule; 3-min window empty | `CODE_CONFIRMED` (arithmetic from `E1DifficultyMapping`) | ~16 RT at easy end (= simple RT), ~2 at hard end vs `MIN_N_RT=5` | **Moot for the gold** — E1 is not selected. The PVT-B numbers (`355 ms`, `≥40`) are **`UNVERIFIED`** and **not adopted**: they belong to a specific sleep-deprivation protocol and require primary sources + simulation + device timing validation first | — | declared, unadopted |
| Q4 | Shared seed ⇒ shared schedule ⇒ implicit sequence learning | **`DESIGN_JUDGMENT` for the gold** (retyped) | The literature supports *that sequence learning exists*; it does **not** support *shared seeds inflate E2 flanker RT*. For E2 the repeated sequence is congruency order — far weaker than learning a spatial/temporal target schedule, which is E1's core manipulation and where `LITERATURE_SUPPORTED` would be apt | `formId` (§3) makes exposure **computable**; training never reuses a `formId` for the same subject. Full exposure ledger = declared follow-up, not faked | `formId` derivation + no-reuse test | `desktopTest` |
| — | `MIN_N_CONGRUENCY = 3` | `CODE_CONFIRMED` | `DefaultMetricsEngine.kt:466` | **Do not pick a new number to go green** (§14). Congruency effect ⇒ `Unavailable` below a derived threshold; the derivation is owed before any congruency claim ships. Gold shows **per-condition descriptives**, not the difference | `Unavailable` path + test | `desktopTest` |
| Q6.4 | Time-of-day/sleep unrecorded; unrecoverable retroactively | `LITERATURE_SUPPORTED` **for PVT lapses; E1-scoped** | Well supported for PVT; **flanker accuracy is markedly less state-sensitive**, so this row's relevance to the gold is limited and its inclusion overstated it | **Not collected in this minifase.** It is context data requiring purpose/consent/retention + `privacy-security` (§8). Collecting it silently to protect a future claim would be the worse error. Declared open with owner | — | declared debt |
| Q6.7 | No authoritative-timebase gate on the Focus path | `CODE_CONFIRMED` (absence) | `SpeedProfileSource`'s gate has no Focus twin | **Measurement-only.** Blocks `MEASUREMENT_ELIGIBILITY`, **not** `TRAINING_READY` (§2.1). Degraded timing ⇒ RT metrics `Unavailable`, session still valid practice | timing-degraded test | `desktopTest` |

### 1.0b Blockers raised against the V2 draft — all `CODE_CONFIRMED`, all inside the gold vertical

The first V2 draft was returned `NO APTO` with three TRAINING blockers. All three were verified in code before
acceptance; B3 turned out **worse** than reported.

| # | Blocker | Type | Evidence | Resolution | Gate |
|---|---|---|---|---|---|
| **B1** | **The E2 trial budget is E1-calibrated; a "5 min" session runs ~1.5 min** | `CODE_CONFIRMED` | `SessionTemplate.estimatedTrialCount(averageTrialMs: Long = 3_800)` — its KDoc says *"Approximate number of **E1** trials"*. `FocusSessionStateHolder.kt:195` calls it **with no argument**. `MAINTENANCE_5.coreMs = 210_000` ⇒ `210000/3800 = 55` trials. E2's real per-trial = ISI (`lerp(700,500)`) + response (returns early, ~500 ms typical) + `FEEDBACK_MS` 320 ⇒ **~1.3–1.6 s** ⇒ core ≈ 83 s, total ≈ **1.5 min** for a template labelled 5 min. **My §2 claim of "~100–150 trials" was wrong by 2–3×** | **Drive the core loop by elapsed time against `coreMs`**, not by an E1-derived trial count: generate a trial list sized for the fastest plausible pace (so it cannot run dry) and stop when `coreMs` elapses. A duration parameter, not a formula — §14's "no invented numbers" does not apply. §2's trial count is then restated as `CODE_CONFIRMED` against what the code produces | `desktopTest` + route |
| **B2** | **`buildReport` is exercise-blind: the flanker ships "Lapsos"** | `CODE_CONFIRMED` | `FocusSessionAssembler.kt:134-149` builds the same four lines for **every** exercise and never reads `definition.primaryMetrics`. For E2 that means **"Lapsos"** (`lapses = rawHitRts.count { it > lapseRtMs }` — a PVT/vigilance construct, on a flanker with a 900–1500 ms window it is largely a window artifact), **RT-CV** (not an E2 primary — `ExerciseCatalog.kt:59`), and **no congruent/incongruent split** — the one thing the gold promises. Congruency is available: `Stimulus.kt:124 toMarkers()` → `TrialMarkers.congruent` → `TrialResult.congruent` | `buildReport` becomes exercise-aware for the gold: read `definition.primaryMetrics`; emit per-condition (congruent/incongruent) accuracy + RT-median descriptives; **drop LAPSES and RT_CV from E2's report**. A build, not a research problem | `desktopTest` + route |
| **B3** | **The renderer pops the target out, diluting the mechanism the gold exists to train** | `CODE_CONFIRMED` — **and worse than reported** | `FocusChoiceField.kt:72` paints the target `AppColors.FocusSignal` at **60sp bold**; `:79` paints flankers `AppColors.FocusReadout` at **44sp**. The review caught the colour; **the size difference is a second, stronger pop-out cue**. In the canonical Eriksen flanker every item is visually identical and the target is defined by **position alone** — that is what forces selection | **Render target and flankers identically** (same colour, same size); position alone defines the target. Calibrated: this **dilutes, does not void** — flanker interference is substantially response-level — but the exemplar exercise must be the mechanism it claims | `desktopTest` + route |

**Debt recorded, not silently repaired** (none block training): E2's own `governingMetric = CONGRUENCY_EFFECT`
(`ExerciseCatalog.kt:58`) is **inert** — nothing in `focus/**` reads `governingMetric`; that is F4's twin sitting in
the gold's own entry, and the matrix's "F4 is moot for the gold" must own it. Focus lacks the
`governingMetric ∈ primaryMetrics` consistency test that Emotions and Memory both have.
`ExerciseCatalog.kt:293-298`'s KDoc is stale (says E2 still needs a renderer; `implemented` includes it).

**Verified, not assumed** (the draft asserted this without showing the work): the difficulty ladder does **not**
consume the unreliable congruency effect, so suppressing its display is not cosmetic —
`AdaptiveEngine.kt:31-33` reads `governingAxis` plus `ACCURACY`/`RT_CV`, never `governingMetric`/
`CONGRUENCY_EFFECT`. The training path is clean.

### 1.1 Correction: replay of an adaptive task

V1 claimed E4's online staircase makes it "not seed-replayable **by construction**". **That is wrong**, and the
CEO's correction is accepted. Two distinct contracts:

```
PREGENERATED_REPLAY = seed + rngVersion + generatorVersion + exerciseVersion + config/difficulty version
                      (+ content/bank version)
ADAPTIVE_REPLAY     = the above + prior state + ordered actions/events
                      + relative monotonic timing that affected transitions
```

A deterministic state machine with a complete recorded input **is** reproducible, online staircase or not. E4 may
still be a poor gold — duration, SSRT estimability, strategic slowing — but **not** for the reason V1 gave. That
reason is withdrawn.

### 1.2 Correction: three independent gates

V1 let measurement requirements veto the product. They are separate:

```
TRAINING_READY_GATE        honest, playable, useful practice; feedback describes the session
MEASUREMENT_ELIGIBILITY_GATE  may inform a cognitive profile
OFFICIAL_SCORING_GATE      may produce an official score/rank
```

**An exercise needs no psychometric reliability to be honest training.** The correct response to a weak metric is
to **disable the index and the claim**, not to block the exercise. For the gold: `TRAINING_READY` is targeted;
`MEASUREMENT_ELIGIBILITY` and `OFFICIAL_SCORING` are **declared OFF**.

---

## 2. Gold exercise: **E2 Filtro** (flanker)

Selected on the CEO's ten training-first criteria. Reliability as an official instrument is explicitly secondary.

The first draft of this table carried **no evidence types** — and the one number carrying the gold's training case
("~100–150 trials") was the one number never verified, and it was false. Every row is now typed.

| Criterion | Type | E2 Filtro |
|---|---|---|
| Training value | `DESIGN_JUDGMENT` | Practising selective filtering under interference is a coherent, felt training target |
| Mechanism | `CODE_CONFIRMED` | Flanker: respond to the central arrow, suppress the flankers. `E2FiltroGenerator.kt:44-59` |
| Experience | `CODE_CONFIRMED` | Every trial demands a response (`isTarget = true` for all) — dense, paced |
| 3–5 min useful | **`CODE_CONFIRMED` (corrected)** | **Not as currently coded.** `MAINTENANCE_5` yields **55 trials ≈ 1.5 min** (B1). After B1's time-driven fix, a `coreMs = 210_000` session is ~210 s of trials at ~1.3–1.6 s each ⇒ **~130–160 trials in ~3.5 min**. The claim holds only *after* B1 lands, and the number is restated against the code, not asserted |
| Deterministic generation | `CODE_CONFIRMED` | Pure `PREGENERATED_REPLAY` — `generate(difficulty, spec, seed)` with no online adaptation |
| Accessibility | `CODE_CONFIRMED` | Signal is the glyph `←`/`→` (`FocusChoiceField.kt:104`) — no reading, no colour discrimination needed to answer. **Caveat:** the current renderer adds a colour+size pop-out cue on the target (B3), which must go |
| Offline | `CODE_CONFIRMED` | Fully parametric, no bank, `contentVersion = null` |
| Difficulty | `CODE_CONFIRMED` | `E2DifficultyMapping`: congruency ratio, flanker count, response window, ISI — all `lerp`-bounded |
| Honest feedback | `DESIGN_JUDGMENT` | Per-condition descriptives, no index, no capacity claim — **requires B2**, it does not exist today |
| Demonstrates the engine | `DESIGN_JUDGMENT` | Generator, scheduler, response window, **choice (L/R) capture**, classification, difficulty, lifecycle |

**Why not E1.** The efficacy review's own case against it is decisive *for training*: PVT is a **state probe**
dominated by sleep pressure and circadian phase — you cannot train your way out of sleep debt, and the reviewer
called it "the worst candidate for showing longitudinal progress". It is an **alertness probe, not a training
exercise**. Per the CEO's rule, E1 could only win if both reviewers argue it is valuable *training*; the efficacy
reviewer argued the opposite. E1's proper future home is a **non-scoreable, state-sensitive PVT-like probe**,
separate from the Focus training ladder. **E4/E5** are not selected — E5 needs colour+reading (accessibility),
E4 needs duration/SSRT work — but **neither is disqualified on replay grounds** (§1.1).

**E2's known weakness, stated plainly:** the congruency *difference score* has poor individual-differences
reliability (the reliability paradox — Hedge, Powell & Sumner 2018, `LITERATURE_SUPPORTED`). This is exactly why
the gold ships **no congruency index and no measurement eligibility**. Accuracy and RT per condition are reported
descriptively, for this session only. Below a derived sample threshold the congruency effect is `Unavailable` —
and no threshold is invented here to make it available.

---

## 3. Reproducible identity — persisted, before the first trial

```
ReproducibleSessionIdentity
  seedHex               fixed 16-char hex   — never a JSON number (bit loss)
  rngVersionId          stable string "v1"  — never an enum ordinal
  generatorVersion      int
  exerciseId            string
  exerciseVersion       int
  difficultyConfigVersion int
  contentVersion        string?             — null when purely parametric (E2)
  formId                derived hash of all of the above — the exposure key
```

Rules: written **before the first trial is presented**, inside the existing write-ahead `SessionRecord` path.
Process death restores the identical session. A historical seed is **never** reinterpreted under a different
generator/RNG version — unknown version ⇒ **fail closed**, never a silent re-roll. Seed is never wall-clock;
production entropy comes from the platform. splitmix64 is a determinism tool, **not** cryptographic, and no
measurement guarantee rests on hiding it. Training and measurement keep separate seed/exposure policies.

**Constraint from §0.3:** `focus.data.local.SecureKeyValueStore`'s API is consumed by foreign untracked tests —
**additive changes only**.

**Substreams:** if more than one component draws randomness, each takes a labelled substream derived from the
root seed, so adding a cosmetic draw cannot shift the whole session. E2 needs one stream; the seam is defined
now and proven when a second consumer appears.

**Migration scope:** the gold only. The other 12 generators keep `Random(seed)` until restoration and replay are
demonstrated (§14).

---

## 4. Metrics: sealed result, no sentinel

```
MetricResult.Available(value, sampleSize, quality, version)
MetricResult.Unavailable(reason, observedSampleSize)     // no numeric accessor exists
```

Fail-closed for: zero responses · omissions only · constant tapping · many false alarms · anticipations · missing
RT · insufficient congruency cells · interrupted session · duplicate data · degraded timing.
No `d′`, no invented `MIN_N`, no universal 0–100 across Focus families. Where no defensible rule exists: keep the
descriptive primaries, **suppress the derived index**, mark insufficient.

---

## 5. Replay is non-scoreable by construction

Distinct type for replayed execution; the persistence API **refuses** a replayed result; idempotent by
`sessionId` (persisting twice does not duplicate); tests prove replay updates no profile, score, XP or exposure.
A build scanner is added only as defence-in-depth — the type is the barrier.

## 6. `FocusEngine`

A specialised facade **composing** the existing Focus implementation (generators, `TrialClassifier`,
`FocusSessionAssembler`, `DefaultMetricsEngine`, difficulty) and reusing `core/session`, `core/scoring`,
`core/antigaming`, `core/timing` unchanged — no duplication, no parallel runtime. It **imposes invariants**
(identity persisted before trial 1; replay non-scoreable; metrics fail-closed; feedback allow-list) rather than
forwarding calls. UI computes nothing.

## 7. Out of scope (declared, not hidden)

V2d · Memory · the other 12 generators · official scoring · `d′` · redesign of `core/session` · repairing foreign
WIP · time-of-day collection · full exposure ledger · E1's inert axis (F4) · PVT-B constants.
