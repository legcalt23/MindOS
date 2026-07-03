# MindBoostProfile (Fase 2A) — Architecture

The global, functional **cognitive "brain profile"** (the swipe-left counterpart to the public app
profile card). It aggregates COARSE, non-PII signals from the MindBoost training modules into a unified
functional profile, with confidence gating, a baseline-aware orchestrator, and local-first encrypted
persistence. Package: `com.mindos.app.features.mindboost.profile`.

## Non-negotiable safety rules

- **Functional, never clinical.** Dimensions/statuses are descriptive (`CALIBRATING/EMERGING/ESTABLISHED`,
  "an area to develop") — never diagnoses, never "deficient".
- **Never fabricate.** A dimension with no qualifying signal is `CALIBRATING` with `value = null` — the UI
  must render "calibrating", NEVER a 0. `preliminary` propagates from uncalibrated modules.
- **Local-first, never uploaded.** Cognitive signals are sensitive. Persistence is the encrypted
  `SecureKeyValueStore` (Android AES-256-GCM Keystore; desktop/iOS in-memory), exactly like the other
  MindBoost cognitive data. **This deliberately diverges from the Fase-2A "Firebase" answer**: the binding
  repo rule (and `EmotionProfileReadOnly`: *"never anything that could be uploaded"*) wins. The card is
  public identity → Firebase; the brain profile is private cognition → local. Cloud sync would be a
  separate, consent-gated feature.

## Ports & adapters (one-way dependency)

Each module exposes a coarse contribution; the profile consumes it and NEVER reaches into module
internals — adapters live inside each module and depend on the profile contract, mirroring the existing
`EmotionProfileReadOnly`.

```
CognitiveModuleProfileSource (port, in profile)
   └── module adapters (in each module) → ModuleContribution
                                              │
MindBoostProfileService.refresh() ── pulls all sources ──► MindBoostProfileAggregator
                                              │                        │
                          MindBoostProfileRepository (save)   MindBoostProfile (8 DimensionState + OverallSummary)
                                              │                        │
                                      local encrypted store    MindBoostProfileOrchestrator → TrainingRecommendation
```

### Taxonomy
8 dimensions ↔ 8 modules (1:1 today; the model allows many→one later):
ATTENTION↔ENFOQUE, PROCESSING_SPEED↔VELOCIDAD, WORKING_MEMORY↔MEMORIA, REASONING↔LOGICA,
VERBAL↔LENGUAJE, DECISION_MAKING↔DECISIONES, EMOTIONAL_REGULATION↔EMOCIONES, CREATIVITY↔CREATIVIDAD.

### `ModuleContribution`
`normalizedScore: Double?` (null = not computable), `dataSufficiency`, `confidence` (all 0..1),
`trend`, `preliminary`, `updatedAtMs`. Validated.

### Aggregation (`MindBoostProfileAggregator`, pure)
Per dimension: keep contributions with non-null score AND `sufficiency ≥ minSufficiency` AND
`confidence ≥ minConfidence`. None qualify ⇒ `CALIBRATING`. Else value = confidence×sufficiency-weighted
mean; confidence = max; status `ESTABLISHED` if confidence ≥ `establishedConfidence`, else `EMERGING`;
trend from the highest-confidence contributor; `preliminary` if any contributor is preliminary. Overall
composite = mean over `ESTABLISHED` dimensions, only once ≥ `minEstablishedForComposite` (else null);
`strongest`/`developing` are the max/min established dimensions.

### Orchestration (`MindBoostProfileOrchestrator`, pure)
Priority among AVAILABLE modules: `CALIBRATING` (GATHER_DATA) → `EMERGING` lowest-confidence
(GATHER_DATA) → `ESTABLISHED` lowest-value (DEVELOP_AREA). Deterministic tie-breaks by module ordinal.

### Service (`MindBoostProfileService`)
`cached()` (last snapshot), `refresh()` (pull sources → aggregate → persist → return), `recommend()`.
A single failing source is isolated (cancellation rethrown); time is injected via `EpochClock`.

## Wave status

- **Ola 1 — domain + contract + aggregator + orchestrator** ✅ (12 tests).
- **Ola 2 — local encrypted persistence + service** ✅ (8 tests). Detekt-clean; `desktopTest` green.
- **Ola 3 — real module adapters** ✅ (23 tests). Adapters live in `profile/data/adapter/` and consume
  ONLY each module's existing public read surface (zero edits to other modules). Wired into the service by
  `mindBoostProfileSources(...)`. Research + adversarial verification done via a workflow before coding.
  - **EMOCIONES** → `EmotionProfileSource` (stable `EmotionProfileReadOnly`). `normalizedScore` is ALWAYS
    null — the snapshot's `tier` is a session-count maturity ladder, not a quality score → EMOTIONAL_REGULATION
    stays CALIBRATING while surfacing real sufficiency/confidence/trend. (`STEADY`→`STABLE` trend mapping.)
  - **VELOCIDAD** → `SpeedProfileSource`. `SpeedScoreEngine.total` is never null and collapses to a silent 0
    for degenerate runs, so the adapter gates on `usefulSpeed != null` && `pc > 0` && `rtMedianNanos != null`
    (else null/CALIBRATING). Confidence capped at 0.6 (< ESTABLISHED) since the score is always preliminary.
  - **ENFOQUE** → `FocusProfileSource`. `FocusScore.value / 100` (null when no confident component);
    confidence from `archetypeConfidence`, sufficiency from `totalSessions`, trend from `SUSTAINED_FOCUS_INDEX`.
  - **LÓGICA** → `LogicProfileSource`. mean recent `averageIcr / 100`; confidence blends volume + per-skill
    (1 − uncertainty); always preliminary.
  - **LENGUAJE** → `LanguageProfileSource`. mean `EvidenceState.rank / 5` of traits ≥ E1 (NOT `thetaMean`,
    hardcoded 0; NOT ungated accuracy); confidence = reliability × (1 − ½·practiceRisk).
  - **MEMORIA** and **DECISIONES** intentionally NOT wired (no honest per-user score: memory `performance`
    is difficulty-relative; decisiones persists nothing per user) → those dimensions stay CALIBRATING.
- **Ola 4 — brain-profile screen** ✅ (9 tests). `BrainProfileUiModelBuilder` (pure) maps the domain
  profile + recommendation → a display model with non-clinical Spanish copy; a CALIBRATING dimension shows
  "—" and `score0to100 = null` (NEVER 0). `MindBoostBrainProfileStateHolder` shows the cached snapshot then
  refreshes. `BrainProfilePolygon` is an honest radar: measured dimensions are bright vertices at their
  value; calibrating ones are hollow markers near the centre (not plotted at 0), and the filled shape only
  spans measured vertices. `MindBoostBrainProfileScreen` (scrollable) renders overall index, radar,
  recommendation, per-dimension breakdown, and the not-a-diagnosis disclaimer. Composables gated by
  compile (desktop + Android) + Detekt (no visual unit test). NOT yet wired into Home (Ola 5).
- **Ola 5 — wiring + swipe-left + Android gate** ✅ (Fase 2A COMPLETE). `mindBoostProfileSources(...)` now
  takes nullable surfaces and skips absent modules. `rememberMindBoostBrainProfileStateHolder(userId)`
  builds the service by reading each module's coarse signal from the SAME encrypted namespace it writes to
  (Focus/Speed/Logic/Emotions; `LocalEmotionRepository` is an `EmotionProfileReadOnly` directly) and
  persists the snapshot to a new `mindboost_profile` namespace (added to `AccountBound`, so account deletion
  erases it). LENGUAJE has no production store yet → passed null → VERBAL stays CALIBRATING. The own-profile
  destination in `HomeScreen` is now a `HorizontalPager`: page 0 = app card, swipe LEFT = brain profile
  (`ProfileWithBrainProfilePager`). Gated: desktop + Android compile, Detekt, `:androidApp:compileDebugKotlin`.
  NOT verifiable here: on-device swipe/pager UX (pager vs the card's internal horizontal drags) needs a device.
