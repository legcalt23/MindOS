> ⚠️ **SUPERSEDED / REOPENED (2026-07-11) por `SYSTEM1_SLICE_6R3_REOPEN.md`.** El CEO reabrió 6-R2 (veredicto
> independiente = REOPEN): su contrato evidence-only seguía siendo reconstruible en un perfil (falsación numérica
> — ver 6-R3 §6.3) y arrastraba un P0 cross-account. 6-R3 reescribió el contrato a **facts-only**, corrigió el P0
> `CognitiveSessionRunStore` (revisado APTO) y registró un P1 nuevo (`SessionRecordStore`, disposición PENDIENTE
> CEO). Estado vigente y autoridad de este trabajo: `SYSTEM1_SLICE_6R3_REOPEN.md`. Léase abajo solo como registro
> histórico del paso 6-R2.

# SYSTEM-1 · Slice 6-R **REOPEN** (6R2) — Adversarial re-audit & evidence-only rewrite

> **Verdict target of this reopening: REOPEN → work in progress.** The prior close (`SYSTEM1_SLICE_6R_REAUDIT.md`,
> verdict CONDITIONAL) protects RANK by a type invariant but does **not** mechanically protect the PROVISIONAL
> PROFILE: the ingestor emits an aggregator-shaped `ProfileEstimate` and the aggregator is not a universal barrier
> (its own blocker test proves 4 stable sessions pass). This document is the operative record of the reopening.
>
> **Scope (unchanged, hard):** PURE, NOT wired. No E9, profile, Global Score, rank, UI, Firestore, SyncQueue,
> outbox, analytics, cloud. No Slice 7. No destructive git. Foreign untracked files
> `core/scoring/normalization/InterSessionConfidence.kt` and `focus/data/local/IndexedSecureKeyValueStore.kt`
> are NOT edited.

---

## 0. Source-of-truth reconciliation (honesty finding — read first)

The reopening mandates reading a **"paquete 2.0"** in a fixed order: `00_CONTEXT_INDEX.md`,
`02_CEO_COPILOT_CHARTER.md`, `03_SCIENTIFIC_MEASUREMENT_CHARTER.md`, `01_MINDBOOST_PRODUCT_PRD.md`,
`09_DECISION_LOG.md`, `05_TECHNICAL_CURRENT_STATE.md`, `06_AI_DEVELOPMENT_OPERATING_SYSTEM.md`,
`07_PHASE_REVIEW_PROTOCOL.md`, `04_EXERCISE_QUALITY_STANDARD.md`.

**Verified absence.** None of these files exist in the repository (tracked or untracked). Confirmation:
`git ls-files | grep -iE "context_index|copilot_charter|measurement_charter|product_prd|decision_log|current_state|operating_system|review_protocol|quality_standard"` → no match; a repo-wide grep for `ADR-SCI-003`,
`ADR-SCI-004`, `CalibrationBlueprint`, `EvidenceQuantityMaturity`, `CEO_COPILOT_CHARTER`,
`SCIENTIFIC_MEASUREMENT_CHARTER` matches **only** my own `SYSTEM1_SLICE_6R_REAUDIT.md`, my `RankReadiness.kt`,
and the OLD `docs/MindBoost_PRD_Completo.md`.

**Consequence (no fabrication).** I cannot read or cite documents that are not present, and I will not pretend to.
The paquete 2.0 evidently lives in the CEO/ChatGPT context, not the repo; its **operative content for this task
is the reopening instruction itself** (its Sections 5–12 are self-contained: closed decisions, findings + required
outcomes + acceptance criteria, minimum architecture, mandatory tests, DoD). Operative authority I DO apply:

- The reopening instruction (Sections 5–13) — primary.
- `docs/mindboost_exercise_design/00_CHARTER.md` (the scientific charter that exists).
- `docs/mindboost_scoring/MASTER_PLAN_RECONCILED.md` (reconciled scoring plan; untracked).
- The cognitive-science corpus in `docs/knowledge/cognitive_science/`.
- The real code/tests (the artifact under audit).

**De-authorization honored.** `docs/MindBoost_PRD_Completo.md` is treated as historical/superseded. Its
`avg(last 5)`, EWMA `0.85/0.15`, `max 2 sessions/day`, and `STALE ≥ 30 días` are **NOT** treated as approved
authority. `ADR-SCI-003` (longitudinal estimator) and `ADR-SCI-004` (growth/trend) are NOT closed here.

---

## 1. Baseline evidence (Section 4)

- **Base commit SHA:** `71e17038733e23a603c8ecb85ee5ff9f965a62db` (branch: detached HEAD).
- **Worktree:** the pre-existing dirty set (E8/speed, brain-profile presentation, storage, hooks) was already
  modified at session start and is **not** mine. My tracked-modified file will be additive docs; the ingestion
  package is untracked. Foreign untracked files present and untouched: `InterSessionConfidence.kt`,
  `IndexedSecureKeyValueStore.kt`.

### 1.1 Ingestion package — inventory (11 files, all untracked)

**Production (`shared/src/commonMain/.../profile/domain/ingestion/`):**
| # | File | Role |
|---|------|------|
| 1 | `CognitiveSessionIngestor.kt` | Orchestrator; defines `ProfileEstimate`, `CalibrationState`, `CognitiveSessionIngestion`, `ingest(blockId, runs, nowMs)` |
| 2 | `IngestionEvidence.kt` | `ExclusionReason`, `ExclusionCount`, `ScoreScaleKey`, `FreshnessStatus`/`FreshnessPolicy`, `EvidenceSummary` |
| 3 | `IngestionGates.kt` | Per-run predicates (`isMeasurementValid`, `profileExclusion`, `contributesToCalibration/Ranking`, `orderKey`, `dayEpoch`, `scaleKey`) |
| 4 | `ProfileScorePolicy.kt` | avg-last-5 → EWMA(0.85/0.15) estimate authority |
| 5 | `PerformanceTrendPolicy.kt` | least-squares descriptive net-direction trend |
| 6 | `RankReadiness.kt` | `RankBlockReason`, `RankPolicy`, `RankReadiness` (type-level fail-closed) |

**Test (`shared/src/desktopTest/.../profile/domain/ingestion/`):** `CognitiveSessionIngestorTest.kt`,
`ProfileScorePolicyTest.kt`, `PerformanceTrendPolicyTest.kt`, `RankReadinessTest.kt`,
`CognitiveSessionIngestorAggregatorBlockerTest.kt`.

### 1.2 Consumer inventory — `ModuleContribution` (the profile currency)

**Producers (9 live `ProfileSource` adapters + gate):** `SpeedProfileSource`, `LogicProfileSource`,
`FocusProfileSource`, `EmotionProfileSource`, `LanguageProfileSource`, `EpisodicProfileSource`,
`DecisionsProfileSource`, `PairsProfileSource`, `MemoryProfileSource`; plus `ProfileUpdateGate` (ScoreResult →
contribution) and helper `LanguageEvidenceDecision`.

**Consumer chain (live):** the 9 sources are wired via `MindBoostProfileSources` →
`MindBoostProfileService(sources, repo)` (`RememberBrainProfileStateHolder.kt:60`) →
`aggregator.aggregate(userId, contributions, now)` (`MindBoostProfileService.kt:36`) →
`MindBoostProfileAggregator` (the sole `.aggregate` call). UI entry points:
`rememberMindBoostBrainProfileStateHolder` → `ProfileWithBrainProfilePager`; and `TodayPlanStateHolder`.

**Ingestor wiring status:** `CognitiveSessionIngestor` / `ProfileEstimate` appear in **no** production file other
than their own package. The live profile is fed **only** by the 9 adapters. **No active route** exists from the
6-R contract to the aggregator, profile, Global Score, or rank. The `ProfileEstimate → ModuleContribution` fold
exists **only** in `CognitiveSessionIngestorAggregatorBlockerTest.kt` (a test). `CognitiveSessionHistoryRepository`
(slice 5) explicitly "produces NO ModuleContribution" — verified: it is a projection over the run store, not a
wire.

**Aggregator thresholds (barrier analysis):** `MindBoostProfileAggregator` config `minSufficiency≈0.5`,
`minConfidence≈0.4`. The blocker test proves: a **1-session** provisional (`dataSufficiency=0.2`) is DROPPED, but
**4 stable same-scale sessions** (`dataSufficiency=0.8`, `stabilityHeuristic=1−e^{−0.6}≈0.451 ≥ 0.4`) **pass** →
the dimension surfaces a value. **The aggregator is therefore NOT a mechanical barrier** to a false provisional
profile (6R-P1-02 confirmed by the code's own test).

### 1.3 Platform state

- **Desktop (JVM):** target of the executable gates; runnable here.
- **Android:** `:shared:compileAndroidMain` is the module Android compile (AGP 9 KMP plugin — no
  `compileDebugKotlinAndroid`).
- **iOS:** Kotlin/Native iOS toolchain is **not** available on this Windows host (`compileKotlinIosSimulatorArm64`
  not registered) → **NOT VERIFIED** (deferred to macOS/CI; not falsified, not claimed green). The ingestion
  code is 100% `commonMain` (no new expect/actual), which reduces but does not remove iOS risk.

---

## 2. Findings — first-pass self-confirmation by direct inspection

(Independent read-only falsification by `efficacy-guardian`, `psychometrician`, `privacy-security` is running in
parallel and will corroborate/extend this table before the contract is frozen.)

| ID | Verdict | Evidence (file:symbol) | Note |
|----|---------|------------------------|------|
| **6R-P1-01** legacy formula as authority | **REAL** | `ProfileScorePolicy` avg-5/EWMA cited from the de-authorized PRD; no error/equating/practice model | Reclassify legacy/experimental; must not feed a live estimate |
| **6R-P1-02** mechanical route to false profile | **REAL** | `ProfileEstimate(currentEstimate, stabilityHeuristic, sessionProgress,…)` = `ModuleContribution(normalizedScore, confidence, dataSufficiency)`; AggregatorBlockerTest folds it; aggregator not universal barrier (§1.2) | Central failure — remove the mappable shape + add dependency boundary + scanner |
| **6R-P1-03** confidence/sufficiency falsification | **REAL** | `stabilityHeuristic` (CV×sample) → `confidence`; `sessionProgress` (count/5) → `dataSufficiency`; KDoc admits non-orthogonality | Keep unknowns null/UNKNOWN; no aggregator-named fields |
| **6R-P1-04** scale key incomplete + recency select | **REAL** | `ScoreScaleKey` = 4-tuple; `selectScale` picks newest by `(orderKey, sessionId)`, excludes rest (`CognitiveSessionIngestor.kt:182`) | Old-version-fresh-timestamp displaces series; add lineage contract + NON_COMPARABLE; version from registry not recency |
| **6R-P1-05** exercise series ≠ block | **REAL** | key holds one `exerciseId`; no subskill/blueprint/coverage/equating | Terminate at per-exercise-scale evidence; no block estimate |
| **6R-P1-06** eligibility flags forgeable | **REAL** | `SessionEligibility` is a public `data class` with bare Booleans (`CognitiveSessionEligibility.kt:57`); `IngestionGates` trusts `profileEligible` for timebase/anti-gaming; no provenance/version | Sealed/versioned policy result or re-derive; private ctor + factory; fail-closed |
| **6R-P1-07** subject isolation documentary | **REAL** | `ingest(blockId, runs: List<CognitiveSessionRun>, nowMs)`; run carries no subject; KDoc "one user" only | Subject-scoped batch built by authoritative factory |
| **6R-P1-08** maturity confused with calibration | **REAL** | `CalibrationState.status`/`isEstablished()`; `CalibrationPolicy` ESTABLISHED=5 sessions/3 days, **no 48h/blueprint** | Rename `EvidenceQuantityMaturity`; `CALIBRATED` unconstructable; **NEW:** `TRAINING_FREE` currently accelerates cal/rank — conflicts with acceptance (flag) |
| **6R-P1-09** temporal policy incomplete | **REAL** | `nowMs: Long`; future excluded only while `>nowMs` (matures with time); `dayEpoch` UTC epoch-day; `STALE_AFTER_MS` from old PRD; `spanMs = max−min` Long (overflow) | Typed `EvaluationAsOf`/trusted clock; quarantine futures; tz-aware day policy; per-lane freshness |
| **6R-P1-10** arbitrary tie-break | **REAL** | EWMA `ordered = sortedWith(compareBy(orderKey, sessionId))` — sessionId decides equal-timestamp weight | Commutative/authoritative/conservative rule; declare window/min-n/outlier |
| **6R-P1-11** consumers not fully audited | **PARTIAL→ADDRESSED** | §1.2 inventory done; no active route; but no scanner prevents re-wiring | Add architecture scanner test |
| **6R-P1-12** evidence invariants incomplete | **REAL** | duplicates collapse silently (uncounted); `conflictRuns` counts runs; single `ruleVersion` string | Add conservation invariants + `PolicyVersionBundle`; NaN/Inf/−0.0 handling |
| **6R-P1-13** evidence/gates insufficient | **REAL (process)** | files untracked; iOS NOT VERIFIED; prior "3/3 APTO" counted a contingent APTO | Run gates on final diff; iOS on CI; honest status |
| **6R-P2-01** PII misclassification | **REAL** | `EvidenceSummary` "PII-free" but carries timestamps + `usedScaleKey` | Classify minimized cognitive/personal; define persist/log/export/show |
| **6R-P2-02** citation attribution | **TO VERIFY** | psychometrician auditing; check Passell 33954913 vs Nicosia 35953659 | Fix source→claim table |
| **6R-P2-03** unbounded complexity | **REAL** | full-history recompute each read; no volume bound | Bound + benchmark; no irreversible accumulators |
| **6R-P3-01** terminology/docs | **REAL** | `ProfileEstimate`/`CalibrationState` names imply profile/calibration | Canonical evidence terminology; fix docs |

**New discrepancy (not in the CEO list):** `SessionPurpose.TRAINING_FREE.mayContributeToCalibration()` and
`.mayContributeToRanking()` both return `true` (`CognitiveSessionEnums.kt:28-33`). The 6R-P1-08 acceptance
requires "Training Free nunca acelera calibración/rango." This is a `core/session/cognitive` enum used by the
sealed E8 productive wiring (slice 4B) — changing it has cross-slice blast radius. **Flagged for CEO**, not
silently changed. (In the evidence-only rewrite the ingestor no longer emits a live calibration/maturity
authority, so the acceleration concept is downstream; the lane assignment must still not present TRAINING_FREE as
calibration acceleration.)

---

## 3. Target design (Section 7 minimum) — to be frozen after reviewer gates

```
SubjectScopedSessionBatch (authoritative, non-forgeable)
  → validation / dedup / quarantine (futures & conflicts quarantined, not silently matured)
  → grouping by explicit MeasurementLineage compatibility (NON_COMPARABLE when unknown)
  → per-exercise-scale EVIDENCE summaries  (ExerciseScaleEvidenceSummary)
  → NO block estimate · NO ModuleContribution · NO profile/global/rank wiring
```

Non-negotiable properties to encode (each mapped to a finding, verified by a test):
1. Output type is **evidence-only**; it has **no** `normalizedScore`/`confidence`/`dataSufficiency`-shaped fields
   and does **not** import `ModuleContribution`/aggregator/profile-model types. **Architecture scanner test**
   fails if `ingestion` imports those or if a `toModuleContribution` reappears. (P1-02, P1-11)
2. Confidence/sufficiency/reliability/error remain **null/UNKNOWN** with no model; separated
   `observationCountProgress` / `temporalCoverage` / `dispersionHeuristic` never fill a `confidence`/
   `dataSufficiency` field. (P1-03)
3. All compatible series preserved; active scale from a trusted lineage, never from the most-recent session;
   unknown/absent lineage ⇒ NON_COMPARABLE. `exerciseId ∈ blockId` verified. (P1-04, P1-05)
4. Rank stays fail-closed by the existing `RankReadiness` type invariant. (kept)
5. `EvidenceQuantityMaturity` separated from any `CALIBRATED` notion; `CALIBRATED` unconstructable. (P1-08)
6. Legacy `ProfileScorePolicy`/EWMA reclassified as experimental-shadow or removed; not producing a live estimate. (P1-01)
7. `SubjectScopedSessionBatch` built only by an authoritative factory; `ingest` cannot take a bare list. (P1-07)
8. Eligibility accepted only as a versioned/sealed policy result or re-derived; forgeable bare Booleans rejected. (P1-06)
9. Typed trusted clock / `EvaluationAsOf`; future runs quarantined; tz-aware day policy; per-lane freshness; no
   Long-subtraction overflow. (P1-09)
10. Commutative/authoritative ordering; conservation invariants + `PolicyVersionBundle`; NaN/Inf/−0.0 explicit. (P1-10, P1-12)
11. Evidence summary classified as minimized cognitive/personal; internal reasons never leak to public copy. (P2-01)

---

## 4. Reviewer gate results (read-only falsification) — ALL THREE UPHOLD REOPEN

### 4.1 efficacy-guardian (fake-profile / farming) — REOPEN upheld
- **P1-02 BLOCKER** with exact construction: 4 `validRun` same scale, `normalized=0.8`, 4 days → `ProfileEstimate(0.8, sessionProgress 0.8, stabilityHeuristic 0.451)` → `ModuleContribution` clears `minSufficiency 0.5` & `minConfidence 0.4` → `PROCESSING_SPEED = 0.8 / EMERGING` (a real value). Aggregator is NOT a barrier.
- **ADD-1 (NEW):** `MAX_SESSIONS_PER_DAY_ACCELERATE=2` applied ONLY in `rankOf`; `calibrationOf`/`estimateOf` ignore it → same-day grinding accelerates provisional maturity, contradicting `RankPolicy`'s own KDoc.
- **ADD-2 (NEW):** calibration ESTABLISHED uses the approximate UTC epoch-day with NO span floor → reachable in ~24h straddling midnight.
- **ADD-3 (NEW):** RT-critical authoritative timebase is a *trusted boolean*; `clockSource` never inspected at the gate.
- **TRAINING_FREE confirmed self-serve farming vehicle** (feeds calibration+rank; user-selectable). INITIAL_ASSESSMENT correctly provisional-only; PRACTICE/DEBUG never count.

### 4.2 psychometrician (construct validity) — REOPEN upheld
- **P1-01:** avg-5/EWMA is an unvalidated engineering heuristic (no SEM, no practice-effect correction [Goldberg 2015 PMID 27239497], no difficulty equating, arbitrary 0.85 gain, regime switch at n=5). Must be **shadow/experimental, never a live level**.
- **P1-03:** the forbidden aliasing lives in `CognitiveSessionIngestorAggregatorBlockerTest` (a *passing* template). `stabilityHeuristic` re-imports sample size via `1−e^{−0.15n}` → NOT orthogonal to `sessionProgress` → double-counts sample. CV is scale-sensitive (penalizes low-but-stable). Both must stay UNKNOWN with no model.
- **P1-04:** recency-selection displaces a valid series; difficulty/device/modality pooling under one 4-tuple is a construct-irrelevant confound (**Passell 2021 PMID 33954913**, **Nicosia 2023 PMID 35953659**). Lineage must be a versioned compatibility contract → `NON_COMPARABLE`; active version from a trusted registry, not recency.
- **P1-05:** output is **dimension-labeled** (`moduleId = blockId.toCognitiveModuleId()`, `CognitiveSessionIngestor.kt:149`) — a single-`exerciseId` trend presented as module capacity. Must terminate at per-exercise-scale evidence; dimension stays UNKNOWN until a `CalibrationBlueprint` exists.
- **P1-10:** EWMA non-commutative; equal-timestamp order decided by `sessionId` (arbitrary). `PerformanceTrendPolicy` regresses on **ordinal index, not elapsed time**; all longitudinal fns are non-robust (no outlier policy).
- **Citation audit CLEAN:** every PMID in code/docs verified correct; the CEO-flagged `Passell↔35953659` mis-attribution **does not exist** (Passell 33954913 is absent from the repo; 35953659 is correctly Nicosia). **P2-02 = nothing to fix; ADD Passell+Nicosia** to the lineage contract.

### 4.3 privacy-security — 3 REAL, 2 escalated
- **P1-07 escalated:** store namespace is the static string `mindboost_session_runs` (not per-uid) AND `FirebaseSessionRepository.signOut()` never purges AccountBound → shared-device cross-user contamination is structural & pre-exists 6-R. Needs `SubjectScopedSessionBatch` + a separate `signOut`-purge fix.
- **P1-06 escalated:** live bypass `CognitiveSessionRunFixtures.validEligibility()` builds `profileEligible=true`; `SessionEligibilityDto.toDomain()` rebuilds booleans from JSON with no re-validation, no `policyVersion`. **Blast-radius fact:** E8 seals via `SessionEligibilityPolicy.evaluate()` (`GoNoGoCognitiveSessionAdapter.kt:259`), `internal` is module-wide in `:shared` (no real barrier), a `private` ctor breaks the mandatory DTO decode ⇒ source-side hardening is a **separate slice**.
- **P2-01:** `EvidenceSummary` "PII-free" is an overclaim (ms timestamps + `usedScaleKey`/exerciseId + internal anti-gaming exclusion reasons = behavioral fingerprint + mechanism leak). Classify minimized cognitive/personal; define a safe projection before any wiring.

---

## 5. FROZEN contract design (evidence-only) — post-synthesis

The rewrite **eliminates the block estimate and every aggregator-shaped field**. The contract computes only
**descriptive, order-independent, single-exercise-scale EVIDENCE** and lives where importing profile/rank types is
the wrong dependency direction. (`kmp-architect` validates the boundary before implementation.)

**Relocation & boundary (P1-02, P1-11):** move the contract from `profile.domain.ingestion` →
`core.session.cognitive.evidence` (peer of the feature-free history read-model). The evidence package imports ONLY
core (`ScoreModuleId`, `CognitiveSessionRun`, cognitive enums). It does **not** import `ModuleContribution`,
`MindBoostProfileAggregator`, `CognitiveModuleId`, `DimensionStatus`, `CalibrationPolicy`, `ProfileTrend`,
`SessionEvidence`, or `toCognitiveModuleId`. An **architecture-scanner test** (desktopTest, mirroring the
copy-scanner) fails the build if any evidence file imports those symbols or if `toModuleContribution`/
`ProfileEstimate` reappear. The old `profile.domain.ingestion` package **and** `CognitiveSessionIngestorAggregatorBlockerTest`
(the forbidden-wiring template) are **deleted**.

**Deletions (P1-01, P1-03, P1-10):** `ProfileScorePolicy` (EWMA/avg-5) and `PerformanceTrendPolicy` are **removed**
— unvalidated longitudinal estimators that add no value to a pure evidence contract and only invite mis-wiring
(`ADR-SCI-003`/`004` remain open). No `currentEstimate`, no `stabilityHeuristic`, no `confidence`/`dataSufficiency`
field, no trend, no EWMA fold (so the order/tie-break defect dissolves — descriptive mean is commutative).

**New output types (`core.session.cognitive.evidence`):**
1. `SubjectScopedSessionBatch(subjectToken, runs)` — ctor non-public; built only by an authoritative factory a
   repository calls. `ingest` takes this, never a bare `List` (P1-07). `subjectToken` = pseudonymous namespace
   alias, documented as pseudonymous-not-anonymous.
2. `MeasurementLineage` = `ScoreScaleKey`(blockId, exerciseId, exerciseVersion, scoringVersion) + a
   `ComparabilityVerdict` (`COMPARABLE_WITHIN_SCALE` | `NON_COMPARABLE`) + `lineagePolicyVersion`. KDoc cites
   Passell/Nicosia for the device/modality gap. Cross-key ⇒ `NON_COMPARABLE`; no recency selection.
3. `ExerciseScaleEvidence` — ONE per compatible lineage group (all groups preserved, none discarded):
   `lineage`, `observationCount`, `observationCountProgress` (labeled progress-to-window, NOT sufficiency),
   `distinctDayCount`/`temporalCoverage` (labeled, UTC-approx caveat), `spanMs?` (overflow-safe),
   `descriptiveExerciseMean: Double?` + `recentValidScores` (bounded, raw — descriptive of THIS exercise, NOT a
   capacity estimate, NOT equated), `dispersionHeuristic: Double?` (labeled), `freshness` (per-lane),
   `evidenceQuantityMaturity` (see 4), `rank: RankReadiness` (fail-closed type invariant, kept).
   **No dimension/`CognitiveModuleId` label** — only the factual core `blockId`.
4. `EvidenceQuantityMaturity` enum: `INSUFFICIENT` | `EMERGING_EVIDENCE` | `SUFFICIENT_QUANTITY` — explicitly NOT
   "ESTABLISHED"/"CALIBRATED"; `CALIBRATED` is unconstructable (does not exist). Day-cap applied consistently
   (ADD-1) and span floor required for the top band (ADD-2).
5. `IngestionEvidenceSummary` — audit with conservation invariants (P1-12): `rawInputCount`,
   `uniqueIdentityCount`, `includedUniqueCount`, `excludedUniqueCount`, `duplicateCount`,
   `identityConflictRunCount`, `init{}` conservation check; NaN/Inf/−0.0 handled explicitly; `PolicyVersionBundle`
   (not one ambiguous string). KDoc reclassified minimized-personal (P2-01).
6. `EvaluationAsOf` — typed trusted-clock wrapper (provenance), replacing bare `nowMs: Long` (P1-09). Future runs
   are **quarantined** (intrinsic future = completedAt inconsistent with created/updated → permanent
   `QUARANTINED`; nowMs-relative future → excluded this evaluation, never silently matured). Per-lane freshness.
   `spanMs` computed overflow-safe. UTC day key kept but never feeds a top maturity band without the span floor.

**Eligibility re-derivation (P1-06 in-contract, ADD-3):** the contract does NOT trust
`profileEligible`/`calibrationEligible`/`rankingEligible`. It re-derives measurement validity from raw run fields
AND re-derives timebase authoritativeness from `clockSource` (RT-critical ⇒ require `FRAME_MONOTONIC`;
SYSTEM_MONOTONIC/WALL_CLOCK_DERIVED/UNKNOWN → non-authoritative). Anti-gaming coverage is not on the run →
`NOT_INDEPENDENTLY_VERIFIABLE`; the evidence therefore makes **no** official-eligibility claim (it is evidence-only).

**Wiring preconditions flagged (out of this pure slice, with owners):**
- W1 (session layer): seal/version `SessionEligibility` + re-validate `SessionEligibilityDto.toDomain()` + persist
  anti-gaming raw fact — `core/session/cognitive`, blast radius into sealed E8.
- W2 (app session): `signOut()` must purge AccountBound namespaces (or switch active namespace) — pre-existing,
  app-wide, outside SYSTEM-1.
- W3 (product): `TRAINING_FREE` acceleration of calibration/rank (`SessionPurpose`) vs 6R-P1-08 acceptance — CEO
  product decision; enum change ripples into E8.
- W4 (catalog): a registry to verify `exerciseId ∈ blockId` and a trusted active-version authority (P1-04).
- W5 (profile): a `CalibrationBlueprint` + difficulty-equating + subskill + measurement-error model before any
  block/dimension estimate (P1-01/04/05; `ADR-SCI-003/004`).

---

## 6. Implementation (what was built)

New package **`core.session.cognitive.evidence`** (9 production files, commonMain, pure, no expect/actual):
`EvaluationAsOf.kt` (+`ClockProvenance`), `SubjectScopedSessionBatch.kt` (private ctor + `of()` + defensive copy),
`MeasurementLineage.kt` (`ScoreScaleKey`, `ComparabilityVerdict`, `MeasurementLineage`, `LineagePolicy`),
`RankReadiness.kt` (relocated verbatim; type-invariant fail-closed), `SessionEvidenceAudit.kt` (`ExclusionReason`,
`ExclusionCount`, `FreshnessStatus`/`FreshnessPolicy`, `PolicyVersionBundle`, `SessionEvidenceAudit` with
conservation `init`), `ExerciseScaleEvidence.kt` (`EvidenceQuantityMaturity`, `EvidenceQuantityPolicy`,
`ExerciseScaleEvidence`), `SessionEvidenceGates.kt` (re-derivation, no forgeable-flag trust, timebase from
`clockSource`), `CognitiveSessionEvidence.kt` (top output), `CognitiveSessionEvidenceExtractor.kt` (pure
orchestrator).

**Tests (7 files, desktopTest):** `CognitiveSessionEvidenceExtractorTest` (34 behavioral+adversarial),
`CognitiveSessionEvidenceBoundaryTest` (the mechanical no-wiring scanner: 3 tests — no `profile.*` import, no
retired symbol, no aggregator/sink), `RankReadinessTest`, `EvidenceQuantityPolicyTest`, `EvaluationAsOfTest`,
`SubjectScopedSessionBatchTest`, `SessionEvidenceAuditTest`.

**Deleted (untracked, no external importer):** the whole `profile/domain/ingestion/` package (6 prod + 5 test) —
including `ProfileScorePolicy` (avg-5/EWMA), `PerformanceTrendPolicy` (trend), `ProfileEstimate`,
`CalibrationState`, and `CognitiveSessionIngestorAggregatorBlockerTest` (the forbidden-wiring template).

### Decision Log (this reopening)
- **DL-1:** `ProfileScorePolicy` (avg-5 + EWMA 0.85/0.15) and `PerformanceTrendPolicy` — **REMOVED**, not kept as
  shadow. Rationale: de-authorized formulas from a superseded PRD, no error/equating/practice model, attractive
  nuisances that invite mis-wiring. **Reintroduction condition:** a validated estimator behind `ADR-SCI-003`
  (longitudinal) + a difficulty-equating/subskill model + a measurement-error model. Versions retired:
  `profile-score-policy-prd-v1`, `descriptive-trend-v1`.
- **DL-2:** The evidence contract is relocated to `core.session.cognitive.evidence` and **cannot import
  `profile.*`** — enforced by `CognitiveSessionEvidenceBoundaryTest` (build fails on a profile import or a retired
  profile-currency symbol). This is the mechanical no-wiring barrier (P1-02/P1-11).
- **DL-3:** New versioned policies: `cognitive-session-evidence-v1`, `measurement-lineage-v1`,
  `evidence-quantity-v1`, `rank-policy-v1`, `freshness-policy-v1` (bundled in `PolicyVersionBundle`).

### Finding → resolution (in the pure contract)
| Finding | Resolution | Where |
|---|---|---|
| P1-01 legacy estimator | DELETED (DL-1) | package removed |
| P1-02 mechanical route to false profile | evidence-only output + relocation + boundary scanner | `CognitiveSessionEvidence*`, `CognitiveSessionEvidenceBoundaryTest` |
| P1-03 confidence/sufficiency falsification | no such field exists; labeled progress ratios only | `ExerciseScaleEvidence` |
| P1-04 recency select / lineage | all scales preserved (per-group), no recency pick; `MeasurementLineage` + Passell/Nicosia | `CognitiveSessionEvidenceExtractor.buildScaleGroups`, `MeasurementLineage` |
| P1-05 exercise ≠ block | per-exercise `ExerciseScaleEvidence`, only core `blockId`, no dimension label | output types |
| P1-06 forgeable flags | re-derive; never read the 3 lane booleans; timebase from `clockSource` | `SessionEvidenceGates` (W1 flagged for source-side sealing) |
| P1-07 subject isolation | `SubjectScopedSessionBatch` (type barrier; honest limit) | `SubjectScopedSessionBatch` (W-precondition: real repository) |
| P1-08 maturity≠calibration + ADD-1/ADD-2 | `EvidenceQuantityMaturity`, day-cap + span-floor consistently | `EvidenceQuantityPolicy`, extractor |
| P1-09 temporal | `EvaluationAsOf`, future excluded, intrinsic quarantine, per-lane freshness, overflow-safe span | extractor, `FreshnessPolicy` |
| P1-10 tie-break / fold | EWMA removed → order-independent; scores are a sorted list, no scalar depends on ties | extractor |
| P1-12 conservation | `SessionEvidenceAudit` conservation `init` + `PolicyVersionBundle` | `SessionEvidenceAudit` |
| P1-13 evidence/gates | executable gates run on final diff (below) | §7 |
| P2-01 PII class | minimized-personal KDoc; no leak of internal reasons; no raw ids/scores in audit | `SessionEvidenceAudit` |
| P2-02 citation | CLEAN (no mis-attribution existed); Passell/Nicosia ADDED to lineage | `MeasurementLineage` |
| ADD-3 timebase | re-derived from `clockSource` for RT-critical blocks | `SessionEvidenceGates.timebaseAuthoritative` |

### Wiring preconditions (out of this pure slice — owners)
W1 seal/version `SessionEligibility` + DTO re-validation (core/session/cognitive; E8 blast radius) ·
W2 `signOut()` purge of AccountBound namespaces (app session; pre-existing) ·
W3 `TRAINING_FREE` calibration/rank acceleration vs 6R-P1-08 acceptance (product; enum change) ·
W4 registry to verify `exerciseId ∈ blockId` + trusted active-version authority ·
W5 `CalibrationBlueprint` + equating + subskill + measurement-error model before ANY block estimate
(`ADR-SCI-003/004`).

## 7. Gate results

| Gate | Command | Result |
|---|---|---|
| Evidence tests | `:shared:desktopTest --tests "…evidence.*"` | ✅ 48/48 pass |
| Full regression | `:shared:desktopTest --rerun-tasks` | ✅ BUILD SUCCESSFUL, no regressions |
| Static analysis | `:shared:detekt --rerun-tasks` | ✅ 0 findings |
| Android compile | `:shared:compileAndroidMain` | ✅ BUILD SUCCESSFUL |
| Copy safety ×3 | `:shared:mindboostCopySafetyScanner{,CommonTest,DesktopTest}` | ✅ green |
| **Boundary scanner (NEW build gate)** | `:shared:evidenceBoundaryScanner` (wired to `check`) | ✅ green |
| iOS | `:shared:compileKotlinIosSimulatorArm64` | ⛔ **NOT VERIFIED** (no toolchain on Windows) |

**`evidenceBoundaryScanner`** was added to `shared/build.gradle.kts` (twin of `mindboostCopySafetyScanner`,
wired to `check`) as the build-config-level mechanical barrier: it fails the build if any file in
`core/session/cognitive/evidence/` imports `profile.*` or names a retired profile-currency symbol. This makes the
no-wiring barrier's own removal a reviewable build-config diff (not a silently-deletable test), per 6R-P1-02's
"scanner que falle … no test descriptivo como barrera". `CognitiveSessionEvidenceBoundaryTest` is kept as
defense-in-depth.

## 8. Final review gate — 4/4, no P0/P1

- **diff-auditor: APTO** — all 10 DoD items PASS; foreign files untouched; W1–W5 flagged not silently
  resolved/dropped; iOS honestly NOT VERIFIED.
- **efficacy-guardian: RESOLVED (no P0/P1)** — the fake-profile & farming vectors are mechanically closed;
  farming reaches at most a day-capped + 48h-span-floored quantity band, never capability; eligibility+timebase
  re-derived; rank unreachable by type invariant; boundary enforced.
- **psychometrician: HONEST** — estimator/trend/confidence/dimension-label genuinely gone (grep-zero); no live
  over-claim; three MINOR naming residuals (all disclosed).
- **privacy-security: PASS** — type barrier, re-derivation, audit classification verified; W1/W2 confirmed
  genuinely untouched (E8 sealing, `SessionEligibility`, `signOut()` unchanged).

**Reviewer-driven improvements applied (all green after):** removed dead `SessionEvidenceGates.isMeasurementValid`;
renamed `temporalCoverage → distinctDayProgress` (avoids a sufficiency reading); added the safe-projection
P2-01 KDoc flag to `orderedNormalizedScores`; promoted the boundary barrier to a Gradle scanner.

**Non-blocking follow-ups (documented, not defects):** (a) consider renaming `EvidenceQuantityMaturity →
EvidenceQuantityBand` (adequately disambiguated by values+KDoc; a future rename); (b) `RT_CRITICAL_BLOCKS` is a
hand-maintained `{SPEED}` set — extend when FOCUS's RT paradigms wire; (c) the 48h floor is independently declared
in `RankPolicy.MIN_SPAN_MS` and `EvidenceQuantityPolicy.SUFFICIENT_MIN_SPAN_MS` (defensibly separate versioned
policies); (d) "normalized" is an overloaded term inherited from the score currency; (e) `COMPARABLE_WITHIN_SCALE`
reads stronger than "comparable modulo uncontrolled adaptive difficulty" (disclosed in prose; resolves when
equating exists).

## 9. VERDICT — CONDITIONAL

The **pure contract is complete**: no P0/P1 remains, all executable gates are green, 4/4 fresh-context reviewers
APTO, and every contract-level risk capable of altering a score/profile/rank is **mechanically barred** in the
pure contract (evidence-only output with no aggregator-shaped field; `evidenceBoundaryScanner` + boundary test;
`RankReadiness` type invariant; `SubjectScopedSessionBatch`; eligibility re-derivation). It is **CONDITIONAL**, not
DONE, because: **iOS is NOT VERIFIED** (no Windows toolchain — deferred to macOS/CI), and **wiring remains blocked
by five enumerated, fail-closed preconditions (W1–W5)**. No wiring to profile/Global Score/rank/E9/UI/Firestore/
SyncQueue/analytics; no Slice 7 — awaiting CEO decision.
