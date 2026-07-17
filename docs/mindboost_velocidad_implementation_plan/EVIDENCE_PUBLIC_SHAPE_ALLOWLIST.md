# Evidence public-shape allowlist — SYSTEM-1 6R3 §18 C3

> Scope: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence/**`
> (10 files). This document is the field-by-field allowlist the C3 contract requires: for every public field of
> every public DTO/enum in the package — name, type, raw source, legitimate purpose, raw-fact-vs-derived-state,
> authorized consumer (today: NONE), what it lets a reader infer alone, what it lets a reader infer combined with
> other fields, and why it is kept. Enforced by `EvidencePublicShapeGoldenTest` (exact-shape reflection golden),
> `EvidenceScannerParityTest` (Gradle task <-> test-side rule-list parity), and `EvidenceShapeBypassMutationTest`
> (automated bypass-vector suite, retiring the CEO's manual §13 procedure — see §5 below).
>
> Author: `test-engineer`, W2A of the 6R3 §18 contract. Production is FROZEN for this pass — this document and its
> three companion tests are read-only with respect to `core/session/cognitive/evidence/**`; no field described
> here was added, removed, or retyped by this pass.

## 0. Honesty framing (read before the tables)

The CEO's mandate is explicit: be honest that **counts + timestamps DO permit inferring coverage/activity** — how
much a subject has engaged with a given exercise scale, over what span, how many distinct days. This document
does **not** claim that a determined reader combining every field in `CognitiveSessionEvidence` learns *nothing*.
It claims something narrower and defensible:

- **Accepted inference (by design):** "how much" (counts), "how spread out" (distinct-day count, span), "which
  scale/exercise/version" (identity fields), "is cross-scale pooling ever silently allowed" (never — the
  comparability verdict says so explicitly). None of this requires or reveals a **score**, a **capacity estimate**,
  a **trend**, a **confidence/reliability value**, or a **readiness-for-something band** — because none of those
  exist anywhere in this contract's output (§7/§18 C2.3 removed every one of them; the psychometrician's §6.3
  falsification worked ONLY because the pre-6R3 contract still shipped `orderedNormalizedScores`, sufficiency
  ratios, and a maturity band — all now deleted, not merely hidden).
- **Rejected inference (structurally impossible from this contract's shipped fields alone):** a mean, a
  coefficient of variation, a trend, a "best comparable" observation, a data-sufficiency ratio, a calibration
  state, a freshness/staleness verdict, or a rank-readiness signal. There is no score-shaped or ratio-shaped field
  left to feed any of those formulas (verified per-type below; also independently pinned by
  `EvidencePublicShapeGoldenTest`).

**The hard barrier is NOT "zero information can ever be inferred" — it is three separable, independently-enforced
layers, and this contract is honest about which layer does what:**

1. **DTO minimization** (§7/§18 C2.1–C2.3, this document + the golden test) — the shipped fields themselves carry
   no evaluative interpretation, only raw facts and canonical states. This is what stops a psychometrician-style
   reconstruction of a profile proxy.
2. **Consumer isolation** (§10/§18 C3, the `evidenceBoundaryScanner` + `evidenceConsumerSweepScanner` Gradle tasks
   and their desktopTest twins) — no commonMain file outside this package may even *name* one of its public types,
   so nothing downstream can read these facts today regardless of what could theoretically be computed from them.
3. **Official-result authorization** (a separate, not-yet-built bridge; `OFFICIAL_ELIGIBLE`/`PROFILE_ELIGIBLE`/
   `RANK_ELIGIBLE` are structurally unreachable from every enum this contract emits) — even if a future, reviewed
   bridge is built to read these facts, it cannot declare an official/profile/rank result from them alone; that
   requires a separate blueprint/equating/calibration authority this contract deliberately does not contain.

A consumer that legitimately reads these public facts and does its **own** arithmetic (e.g. divides
`observationCount` by `distinctDayCount`) cannot be stopped by a DTO-shape scanner — that is an
information-theoretic fact about publishing any raw counts at all, not a hole in this contract. The scanner's job
(§10) is narrower and honest about it: *stop reintroduction* of an evaluative field/shape into the DTO itself, not
*prevent all downstream arithmetic over legitimately-public facts*. See §6 for the residual gaps this pass found
in the *scanner's own* coverage (which the golden test partially, not wholly, backstops).

## 1. Type inventory (cross-checked against the consumer-sweep's own `protectedTypeNames`)

20 shape-carrying public types (DTOs / enums / sealed-interface variants / thin carrier classes) get full
field-shape goldens. 3 additional public singleton objects (`LineagePolicy`, `SessionEvidenceStatePolicy`,
`CognitiveSessionEvidenceExtractor`) are pure policy/function holders — not DTOs — pinned by version-constant
VALUE instead of field reflection (§3). `SessionEvidenceGates` is `internal` (module-only visibility) and is
**not** part of the public shape this document covers (it is still on the consumer-sweep's protected-name list as
defense-in-depth, but carries no public field surface of its own).

| # | Type | File | Kind |
|---|---|---|---|
| 1 | `CognitiveSessionEvidence` | CognitiveSessionEvidence.kt | data class |
| 2 | `ExerciseScaleEvidence` | ExerciseScaleEvidence.kt | data class |
| 3 | `ScoreScaleKey` | MeasurementLineage.kt | data class |
| 4 | `ComparabilityVerdict` (+3 variants) | MeasurementLineage.kt | sealed interface |
| 5 | `MeasurementLineage` | MeasurementLineage.kt | data class |
| 6 | `ExclusionReason` | SessionEvidenceAudit.kt | enum (13 values) |
| 7 | `ExclusionCount` | SessionEvidenceAudit.kt | data class |
| 8 | `StateCount` | SessionEvidenceAudit.kt | data class |
| 9 | `RouteEligibilityCount` | SessionEvidenceAudit.kt | data class |
| 10 | `PolicyVersionBundle` | SessionEvidenceAudit.kt | data class |
| 11 | `SessionEvidenceAudit` | SessionEvidenceAudit.kt | data class |
| 12 | `SessionEvidenceState` | SessionEvidenceState.kt | enum (4 values) |
| 13 | `EvidenceRouteEligibility` | SessionEvidenceState.kt | enum (4 values) |
| 14 | `SessionEvidenceDisposition` | SessionEvidenceState.kt | data class |
| 15 | `RtCriticality` | RtCriticalityRegistry.kt | enum (3 values) |
| 16 | `RtCriticalityDeclaration` | RtCriticalityRegistry.kt | data class |
| 17 | `RtCriticalityRegistry` | RtCriticalityRegistry.kt | class (2 private fields) |
| 18 | `ClockProvenance` | EvaluationAsOf.kt | enum (2 values) |
| 19 | `EvaluationAsOf` | EvaluationAsOf.kt | data class |
| 20 | `SubjectScopedSessionBatch` | SubjectScopedSessionBatch.kt | class (2 fields) |

`SessionEvidenceStatePolicy.dispositionOf`'s own return type, `SessionEvidenceDisposition` (#14), deserves an
explicit honesty note: it is a **standalone pure-function output** — a caller must invoke
`SessionEvidenceStatePolicy.dispositionOf(reason)` directly to obtain one. It is NOT itself embedded as a field
inside `CognitiveSessionEvidence` or `SessionEvidenceAudit` — today's actual emitted evidence graph carries only
the **aggregated** re-bucketing (`SessionEvidenceAudit.stateCounts` / `.routeEligibilityCounts`), never a
per-session raw disposition list. This mirrors the reopen doc's own flagged assumption (§12.2 item 3): the
canonical per-session state machinery is built and tested, but not wired into any *emitted* collection — reported
here again for the final psychometrician/efficacy-guardian pass to rule on, not resolved by this test pass.

## 2. Per-type field allowlist

### 2.1 `CognitiveSessionEvidence` — the whole-block report

**`blockId: ScoreModuleId`**
- Fuente: `CognitiveSessionRun.blockId` — the cognitive domain (e.g. `SPEED`/`MEMORY`/`LOGIC`) the extraction was
  scoped to. `ScoreModuleId` is an EXTERNAL enum owned by `core.scoring` (9 values today), not part of this
  package's own public shape — its own value set is `core.scoring`'s concern, tracked by its own tests.
- Finalidad legítima: scopes the whole report to one domain; without it the report is uninterpretable.
- Raw-fact vs derivado: raw identity fact (copied straight from the run/caller, no transform).
- Consumer autorizado (hoy): NINGUNO (§0 layer 2 — consumer-sweep-enforced).
- Infer solo: which cognitive domain this report describes — necessary context, not itself informative about
  performance.
- Infer combinado (con `scaleGroups`): "how much the subject has engaged with domain X" — ACCEPTED coverage
  inference (§0).
- Por qué se conserva: the report is meaningless without it; there is no interpretation-free way to omit it.

**`scaleGroups: List<ExerciseScaleEvidence>`**
- Fuente: the extractor's own grouping of qualifying, deduplicated, non-excluded runs by `ScoreScaleKey`.
- Finalidad legítima: shows how many DISTINCT, mutually-incompatible measurement scales exist for this block —
  and each scale's own raw facts (§2.2).
- Raw-fact vs derivado: a structural PARTITION of raw facts (grouping), never a statistic/estimate over them.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: how many distinct exercise-scale identities the subject has run, with per-scale counts/dates —
  ACCEPTED coverage/activity inference.
- Infer combinado (con `crossScaleComparability`): confirms whether cross-scale pooling ever silently occurred —
  never, structurally (a positive absence).
- Por qué se conserva: this IS the contract's entire descriptive payload — removing it removes the evidence.

**`crossScaleComparability: ComparabilityVerdict?`**
- Fuente: derived structurally from `scaleGroups.size` (>=2 groups => `Incompatible`; else `null`) — §18 C2.2:
  a REAL construction site, not a silent substitute for the count.
- Finalidad legítima: signals a KNOWN cross-scale incompatibility so a downstream reader never pools differing
  scales without being told.
- Raw-fact vs derivado: a STRUCTURAL/logical STATE derived from a count — never a similarity score.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: only whether >=2 distinct scales exist — the same information `scaleGroups.size` already carries.
- Infer combinado: no additional information beyond what `scaleGroups` already reveals directly.
- Por qué se conserva: makes the non-pooling guarantee an explicit, testable VALUE instead of an implicit
  "consumer must remember to check group count" (§18 C2.2's own words).

**`audit: SessionEvidenceAudit`**
- Fuente: aggregated minimized-personal counts of the whole extraction (§2.11).
- Finalidad legítima: conservation verification (no run silently lost/double-counted) and exclusion/state/
  eligibility breakdowns.
- Raw-fact vs derivado: raw counts + an AGGREGATE re-bucketing of them — never evaluative.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: counts per exclusion reason/state/eligibility — includes anti-gaming-ADJACENT reasons
  (`DATA_QUALITY`, `TIMING_COMPROMISED`) the audit's own KDoc explicitly flags as a "mechanism leak" that must
  not be user-facing at this granularity.
- Infer combinado: cross-referencing the 3 breakdowns (`exclusions`/`stateCounts`/`routeEligibilityCounts`) cannot
  reveal MORE than the `exclusions` list alone — Conservation D ties all three totals to the SAME source.
- Por qué se conserva: auditability/conservation-checking is a scientific-integrity requirement (03 charter,
  P1-12).

**`clockProvenance: ClockProvenance`**
- Fuente: `EvaluationAsOf.provenance` — the injected "as of" clock's trust level at extraction time.
- Finalidad legítima: lets a downstream reader judge how much to trust future-exclusion derived from "now".
- Raw-fact vs derivado: raw fact about the EXTRACTION CALL's environment, not about the subject.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: nothing about the subject — only the caller's clock trust at extraction time.
- Infer combinado: none.
- Por qué se conserva: a best-effort-monotonic caller must be distinguishable from an authoritative one, WITHOUT
  weakening the always-applied future-exclusion rule.

### 2.2 `ExerciseScaleEvidence` — one scale's facts

**`lineage: MeasurementLineage`** — see §2.5 for its own sub-fields.
- Fuente: the group's `ScoreScaleKey` + the frozen `LineagePolicy.VERSION` pin.
- Finalidad legítima: identifies EXACTLY which measurement scale this group's facts describe.
- Raw-fact vs derivado: raw identity (scale key) + a conservative CEILING state (comparability).
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: which exercise/version/scoring the subject engaged with — ACCEPTED activity/coverage inference
  (which content, not how well).
- Infer combinado (con `observationCount`/`spanMs`): "how much, and over what period" for ONE specific scale —
  ACCEPTED coverage inference, never a performance estimate (no score field exists to combine with).
- Por qué se conserva: without the identity, the counts/span below would be uninterpretable numbers.

**`observationCount: Int`**
- Fuente: `sorted.size` — count of qualifying (dedup + disposed-eligible) runs in the group.
- Finalidad legítima: the raw evidence-QUANTITY fact — never divided by a threshold IN this contract (§7
  mandate; the retired `observationCountProgress`/`cappedObservationCount` ratios did that and were deleted).
- Raw-fact vs derivado: raw fact (a count).
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: how many qualifying sessions of one scale the subject has — ACCEPTED coverage/activity inference.
- Infer combinado (con `distinctDayCount`): the psychometrician's §6.3 falsification showed that count + day-
  spread + span, reconstructed WITHOUT any score field, still cannot recompute a mean/CV/trend (no score input
  exists in-contract) — the remaining combined-inference ceiling is "engagement pattern" (grinding-same-day vs
  spread-out), never a capacity estimate.
- Por qué se conserva: the single most honest "how much evidence exists" fact this contract can ship; §7
  explicitly keeps raw counts as the honest floor.

**`distinctDayCount: Int`**
- Fuente: `sorted.map { dayEpoch }.distinct().size` — UTC-approximate day-spread of the group.
- Finalidad legítima: an alternative dimension to raw count that resists trivial grinding-inflation — NOT itself
  a sufficiency verdict in this contract.
- Raw-fact vs derivado: raw fact (a count), labeled UTC-approx, never trusted for rank (§7).
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: over how many distinct days activity occurred — ACCEPTED coverage inference.
- Infer combinado (con `observationCount`): reveals whether sessions cluster in one day ("grinding") vs spread —
  an engagement-PATTERN inference, still not a score.
- Por qué se conserva: §7 explicitly keeps it as a raw count vs the retired 0..1 `distinctDayProgress` ratio
  (deleted) — "raw count" (kept) vs "ratio-to-threshold" (removed) is exactly the C3 boundary line.

**`spanMs: Long?`**
- Fuente: `orderKey(last) - orderKey(first)`, clamped >= 0; `null` when fewer than 2 observations.
- Finalidad legítima: the raw first-to-last temporal span. A consumer wanting a recency/staleness VERDICT must
  build one downstream — this contract ships none.
- Raw-fact vs derivado: raw fact (a duration in millis) — deliberately kept as a plain nullable `Long`, NOT a
  `Duration` type, per the Tier-1 scanner's own KDoc: the ONE type-token exception the scanner intentionally does
  not ban (see `EvidenceShapeScanRules`'s own control test `does_not_flag_nullable_span_ms`).
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: the raw time window of one scale's activity.
- Infer combinado (con `observationCount`/`distinctDayCount`): lets a reader compute a naive "sessions per day"
  or "average gap" — an ENGAGEMENT-CADENCE inference requiring the reader's OWN arithmetic (never shipped as a
  ratio by this contract) — ACCEPTED per §0's info-theoretic stance: the contract cannot stop a consumer's
  arithmetic over legitimately-public raw facts; it only refuses to SHIP the ratio itself.
- Por qué se conserva: §7 explicitly keeps `spanMs` as the one `Long`-typed exception (a recency/staleness
  VERDICT was removed; the raw span a consumer COULD build one from was not) — the contract draws the line at
  "ship the verdict" vs "ship the raw material", accepting the latter is honestly reconstructable by the reader's
  own effort.

### 2.3 `ScoreScaleKey` — the 4-tuple scale identity

**`blockId: ScoreModuleId`** — see §2.1 (same external type, same field role at the key level).

**`exerciseId: String`**
- Fuente: `CognitiveSessionRun.exerciseId` (stable machine name, e.g. `"E8"`).
- Finalidad legítima: identity component of the scale key.
- Raw-fact vs derivado: raw fact.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: which exercise.
- Infer combinado (con `exerciseVersion`/`scoringVersion`): pins the EXACT measurement-instrument version used.
- Por qué se conserva: without it, two different exercises' evidence could wrongly appear pooled.

**`exerciseVersion: String`**
- Fuente: `CognitiveSessionRun.exerciseVersion` (the exercise's own declared content/generator version, e.g.
  `"1.0.0-mc9d"`).
- Finalidad legítima: a version bump on the SAME exercise is treated as a DIFFERENT scale — never silently
  pooled.
- Raw-fact vs derivado: raw fact.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: which build/content version of the exercise.
- Infer combinado (con `scoringVersion`): the full "is this the same measurement instrument" check.
- Por qué se conserva: an un-equated version change materially shifts scores (charter §21 lineage; Passell/
  Nicosia device/version confounds) — dropping this field would silently re-enable cross-version pooling.

**`scoringVersion: String`**
- Fuente: `CognitiveSessionRun.scoringVersion`.
- Finalidad legítima: a scoring-formula change is ALSO treated as a different scale.
- Raw-fact vs derivado: raw fact.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: which scoring formula produced the run's score.
- Infer combinado: completes the 4-tuple identity — §6.3's falsification depended on exploiting a SHARED scale
  key across sessions to justify pooling; this field is exactly what prevents that when it legitimately differs.
- Por qué se conserva: same lineage rationale as `exerciseVersion`.

### 2.4 `ComparabilityVerdict` — sealed interface, 3 variants

**`ComparabilityNotEstablished` (data object, 0 fields)**
- Fuente: default ceiling assigned to every WITHIN-scale group (`MeasurementLineage.comparability`).
- Finalidad legítima: the HONEST ceiling this contract can assert about ONE scale group — key equality is
  necessary but not sufficient for genuine comparability (device/difficulty/modality lineage uncaptured).
- Raw-fact vs derivado: a STATE — a conservative admission of ignorance, never an optimistic claim.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo/combinado: nothing beyond "no stronger claim is made" — it is the floor, not a magnitude.
- Por qué se conserva: the contract must NEVER claim more than key-equality; this is that explicit non-claim.

**`Incompatible` (data object, 0 fields)**
- Fuente: constructed by the extractor at BLOCK level whenever `scaleGroups.size >= 2` (§18 C2.2 — a real
  construction site, per the CEO's explicit requirement that this NOT be silently implied by count alone).
- Finalidad legítima: a POSITIVE, known cross-scale incompatibility signal — differing scale keys are NEVER
  pooled.
- Raw-fact vs derivado: a STATE derived from a count, but represents a KNOWN fact (incompatibility), not an
  estimate.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: >=2 distinct scales exist — the SAME information already public via `scaleGroups.size`, no new
  information.
- Infer combinado: none additional.
- Por qué se conserva: makes the non-pooling guarantee an explicit, testable VALUE (§18 C2.2's own words: "not
  silently substituted by the group count").

**`Bridged(bridgeVersion: String)` (data class, 1 field)**
- Fuente: NONE today — no construction site exists in production (verified by
  `bridged_comparability_has_no_construction_site_in_production` in `CognitiveSessionEvidenceBoundaryTest`).
- Finalidad legítima: reserved vocabulary for a FUTURE, real, versioned equating authority (ADR-SCI-008) —
  unreachable/fail-closed today.
- Raw-fact vs derivado: purely a reserved STATE, no live instance.
- Consumer autorizado (hoy): NINGUNO (and structurally cannot be, since nothing constructs it).
- Infer solo/combinado: N/A — cannot be observed in any real output today.
- Por qué se conserva: removing the vocabulary would force a future equating authority to invent a NEW type
  instead of filling an already-reviewed slot; keeping it (unreachable, source-scan-pinned) makes its eventual
  first construction site a reviewable diff, not a silent unlock.

### 2.5 `MeasurementLineage`

**`scaleKey: ScoreScaleKey`** — see §2.3.

**`comparability: ComparabilityVerdict`** — the group's OWN within-scale ceiling (see §2.4); always
`ComparabilityNotEstablished` in practice today — only the BLOCK-level `crossScaleComparability` (§2.1) ever
becomes `Incompatible`.

**`lineagePolicyVersion: String`**
- Fuente: `LineagePolicy.VERSION` (frozen constant, `"measurement-lineage-v1"`).
- Finalidad legítima: pins WHICH version of the compatibility rule produced this comparability ceiling, so a
  future rule change (e.g. adding device/difficulty components once the run captures them) is detectable.
- Raw-fact vs derivado: a version stamp — unchanging per extraction, not evaluative.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: only a version string — no subject information at all.
- Infer combinado: lets a reader group evidence extracted under different lineage-policy versions separately —
  itself a facts-only, legitimate use.
- Por qué se conserva: without it, a silently-upgraded compatibility rule could be misread as if it always
  applied — versioning is the anti-drift discipline the §21 lineage charter requires.

### 2.6 `ExclusionReason` (enum, 13 values)

Each value is a raw-fact classification of WHY one run did not qualify for an evidence group — never a
performance signal. Two values (`DATA_QUALITY`, `TIMING_COMPROMISED`) are explicitly flagged by
`SessionEvidenceAudit`'s own KDoc as anti-gaming-ADJACENT ("mechanism leak") and must not be surfaced to the user
at this granularity.

| Value | Fuente / condición | Qué revela |
|---|---|---|
| `NOT_FOR_BLOCK` | run belongs to a different `blockId` | irrelevant-scope filter, not about performance |
| `IDENTITY_CONFLICT` | two runs shared a sessionId with differing payloads (both dropped, fail-closed) | data-integrity defect |
| `QUARANTINED_INCONSISTENT_TIME` | run's own timestamps are impossible (completed after last update) | device/clock defect, never re-admitted |
| `FUTURE_TIMESTAMP` | run's event time is after the injected "as of" clock | clock-skew safety valve |
| `NOT_COMPLETED` | session was abandoned/incomplete | engagement/completion fact |
| `DATA_QUALITY` | `dataQualityCompromised` flag from upstream | anti-gaming-ADJACENT — mechanism leak (audit KDoc) |
| `TIMING_COMPROMISED` | timing integrity flagged COMPROMISED | device/clock jank, not ability |
| `RT_CRITICALITY_UNKNOWN` | no `RtCriticalityRegistry` dossier entry for (exerciseId, exerciseVersion) | measurement metadata gap, not about the subject |
| `TIMEBASE_NON_AUTHORITATIVE` | exercise IS declared RT-critical but this run's clock wasn't frame-locked | device-capability fact |
| `NOT_VALID` | run's own score category isn't VALID | upstream scoring gate |
| `PRELIMINARY` | score marked preliminary | soft evidence, still PROVISIONAL-ceiling |
| `LOW_CONFIDENCE` | score marked lowConfidence | soft evidence, same ceiling as PRELIMINARY |
| `PURPOSE_EXCLUDED` | session purpose (PRACTICE/DEBUG/TRAINING_FREE) never evidence-bearing (§7/P1-06) | product/mode fact, not ability |

- Consumer autorizado (hoy): NINGUNO for all 13 values.
- Infer solo (per value, as a COUNT in the audit): "how often this specific non-evidence condition occurred" —
  an operational/device/product fact, never a capacity signal.
- Infer combinado: summed against `rawInputCount`/`forBlockCount` (Conservation A–D in `SessionEvidenceAudit`),
  reveals the SAME totals those conservation checks already expose — no NEW information beyond what
  `SessionEvidenceAudit`'s own fields already carry.
- Por qué se conserva: without a granular reason, the audit could not distinguish "no evidence because the
  device's clock failed" from "no evidence because the subject never trained" — both matter for honest reporting,
  but neither is itself a score.

### 2.7 `ExclusionCount(reason: ExclusionReason, count: Int)`
- Fuente/Finalidad: a (reason, count) tally pair — see §2.6 per-reason discussion; `count` is a raw non-negative
  tally (`init` guard).
- Raw-fact vs derivado: raw aggregate.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo/combinado: identical to the per-reason discussion in §2.6 — no new information beyond it.
- Por qué se conserva: the audit's Conservation C/D invariants require this exact shape (reason + non-negative
  count) to be checkable at all.

### 2.8 `StateCount(state: SessionEvidenceState, count: Int)`
- Fuente: §18 C2.1 re-bucketing of the SAME exclusion tally + included count through `SessionEvidenceStatePolicy`
  — never a second, independently-derived source of truth.
- Finalidad legítima: emits the Scientific Charter §7 canonical evidence-state distribution as a real, auditable
  fact (not merely computable on demand).
- Raw-fact vs derivado: a re-bucketed AGGREGATE of already-conserved facts.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: how many sessions fell into each of the 4 canonical states.
- Infer combinado (con `routeEligibilityCounts`): both are DELIBERATELY orthogonal re-bucketings of the SAME
  source (Conservation D ties both totals to `rawInputCount - duplicateCollapsedCount`) — combining them reveals
  no THIRD hidden axis, both are already exhaustively enumerated and small.
- Por qué se conserva: §18 C2.1 explicit CEO mandate — the canonical state must be EMITTED as a real fact, not
  merely computable by re-deriving `SessionEvidenceStatePolicy` from the raw exclusions list.

### 2.9 `RouteEligibilityCount(eligibility: EvidenceRouteEligibility, count: Int)`
- Same treatment as §2.8, through the eligibility mapping instead of the state mapping — its own orthogonal axis
  (Exercise Quality Standard §3.2's per-execution routing ceiling, distinct from "how much accumulated evidence a
  version has").
- Consumer autorizado (hoy): NINGUNO.
- Por qué se conserva: same §18 C2.1 mandate as `StateCount`.

### 2.10 `PolicyVersionBundle` (5 `String` fields)

| Campo | Fuente | Estado |
|---|---|---|
| `evidenceRuleVersion` | `CognitiveSessionEvidenceExtractor.RULE_VERSION` | LIVE — versions the whole extraction rule |
| `lineagePolicyVersion` | `LineagePolicy.VERSION` | LIVE — versions the comparability rule (duplicated here at audit/whole-extraction scope vs each group's own `MeasurementLineage.lineagePolicyVersion`; kept because a mixed-vintage batch, if it ever existed, would show a per-group value differing from this top-level one) |
| `evidenceQuantityPolicyVersion` | frozen retired constant `"evidence-quantity-v1"` | HISTORICAL marker only — the quantity-BAND policy itself was deleted in §7 |
| `rankPolicyVersion` | frozen retired constant `"rank-policy-v1"` | HISTORICAL marker only — the block-readiness-for-ranking policy was deleted in §18 C2.3 |
| `freshnessPolicyVersion` | frozen retired constant `"freshness-policy-v1"` | HISTORICAL marker only — the recency-verdict policy was deleted in §7 |

- Finalidad legítima: lets a downstream reader detect a rule change instead of trusting one ambiguous string
  (P1-12), and keeps a HISTORICAL `PolicyVersionBundle` honestly attributable even after its policy was retired.
- Raw-fact vs derivado: 5 version stamps — build/policy metadata, never subject data.
- Consumer autorizado (hoy): NINGUNO for all 5.
- Infer solo/combinado: nothing about the subject at all — pure policy/build provenance.
- Por qué se conserva: verbatim from the extractor's own KDoc — "so a previously-extracted `PolicyVersionBundle`
  stays honestly attributable... instead of being silently reinterpreted."

### 2.11 `SessionEvidenceAudit` (12 fields)

| Campo | Fuente | Infer solo | Infer combinado |
|---|---|---|---|
| `rawInputCount` | `batch.runs.size` (ALL runs handed in, before any filter) | total session-runs the subject has EVER recorded across ALL blocks — ACCEPTED activity/coverage | vs `forBlockCount`: fraction of total activity belonging to THIS block — still a coverage fact |
| `forBlockCount` | runs filtered to `blockId` | scope-filtered raw count | as above |
| `uniqueIdentityCount` | dedup survivors (identical-payload duplicates collapsed to one) | de-dup'd population size | vs `duplicateCollapsedCount`: how much re-submission/retry noise existed — operational, not ability |
| `duplicateCollapsedCount` | count of collapsed IDENTICAL duplicates | resubmission noise | as above |
| `identityConflictRunCount` | count of runs dropped for TRUE conflicting-payload duplicates | integrity-adjacent — a client bug or tamper attempt could both produce a conflict; the count alone cannot distinguish (flagged mechanism-leak-adjacent, same family as `DATA_QUALITY`) | vs `exclusions[IDENTITY_CONFLICT]`: same figure, no new information |
| `includedUniqueCount` | eligible-and-grouped survivor count | equals the sum of `scaleGroups[*].observationCount` | no new info beyond that sum |
| `excludedUniqueCount` | survivors that did NOT qualify | total excluded | vs `exclusions` breakdown: sums to the same total, no new info |
| `exclusions: List<ExclusionCount>` | §2.6/§2.7 | per-reason breakdown, including mechanism-leak-adjacent reasons | see §2.6 |
| `distinctScaleCount` | `scaleGroups.size` | redundant-but-convenient — same info as `scaleGroups.size` / `crossScaleComparability != null` | zero NEW information beyond what is already public elsewhere in the SAME output |
| `policyVersions: PolicyVersionBundle` | §2.10 | build/policy provenance only | none |
| `stateCounts: List<StateCount>` | §2.8 | canonical state distribution | no new info beyond `exclusions` + `includedUniqueCount` (Conservation D) |
| `routeEligibilityCounts: List<RouteEligibilityCount>` | §2.9 | canonical eligibility distribution | same as above |

- Consumer autorizado (hoy): NINGUNO for all 12 fields.
- Por qué se conserva (whole type): auditability/conservation-checking (P1-12) is a scientific-integrity
  requirement — the `init` block's Conservation A–D invariants would be unverifiable without every one of these
  12 fields present in exactly this shape.

### 2.12 `SessionEvidenceState` (enum, 4 values: `TRAINING_ONLY`, `PROVISIONAL`, `OFFICIAL_ELIGIBLE`,
`INVALIDATED`)
- Fuente: `SessionEvidenceStatePolicy.stateOf(exclusionReason)`, a pure TOTAL mapping (no subscription/payment
  input on its signature — a paid plan cannot raise evidence eligibility).
- Finalidad legítima: the Scientific Charter §7 canonical vocabulary for ONE session's accumulated-evidence
  disposition. `OFFICIAL_ELIGIBLE` is structurally unreachable — `SessionEvidenceStatePolicy` never constructs it
  (no blueprint/comparability/calibration bridge exists).
- Raw-fact vs derivado: a STATE derived from `ExclusionReason` (itself derived from raw facts) — never a score.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: which of 4 canonical buckets a session fell into.
- Infer combinado (con `EvidenceRouteEligibility` for the SAME session, via `SessionEvidenceDisposition`): the
  two axes are DELIBERATELY orthogonal (per KDoc: "how much accumulated evidence a version has" vs "what one
  execution may feed") — combining them reveals no THIRD hidden axis; both are already exhaustively enumerated
  and small (4 x 4 = 16 combinations, all of which are one of the two enums' own already-public values).
- Por qué se conserva: §18 C2.1 explicit CEO mandate — the canonical state must be EMITTED, not merely
  computable.

### 2.13 `EvidenceRouteEligibility` (enum, 4 values: `TRAINING_ONLY`, `PROVISIONAL_PROFILE_ELIGIBLE`,
`PROFILE_ELIGIBLE`, `RANK_ELIGIBLE`)
- Same treatment as §2.12, its own axis (Exercise Quality Standard §3.2's per-execution routing ceiling).
  `PROFILE_ELIGIBLE` and `RANK_ELIGIBLE` are both structurally unreachable — never auto-derive from one another,
  and `SessionEvidenceStatePolicy` never constructs either.
- Consumer autorizado (hoy): NINGUNO.
- Por qué se conserva: same §18 C2.1 mandate.

### 2.14 `SessionEvidenceDisposition(state, routeEligibility, reasons)`
**`state`/`routeEligibility`** — see §2.12/§2.13.

**`reasons: List<ExclusionReason>`**
- Fuente: `listOfNotNull(exclusionReason)` — TODAY always 0 or 1 entries (the extractor only ever surfaces the
  single FIRST-in-cascade reason).
- Finalidad legítima: mirrors the run model's own multi-reason shape (`EligibilityTrace.rejectionReasons`) for
  forward-compatibility — a future extractor detecting SIMULTANEOUS reasons could grow this list without
  changing the shape.
- Raw-fact vs derivado: a list wrapping a raw `ExclusionReason?`.
- Consumer autorizado (hoy): NINGUNO — and, per the §1 honesty note, this whole type has no live construction
  site wired into any emitted collection today; only the pure `dispositionOf` function produces one, on demand.
- Infer solo/combinado: the SAME single reason already counted in the audit's `exclusions` tally, at
  per-session-hypothetical granularity — no new information beyond what the aggregated audit already exposes,
  since it is not actually emitted per-session anywhere today.
- Por qué se conserva: shape-forward-compatibility, and because `SessionEvidenceStatePolicy.dispositionOf` needs
  a return type bundling state + eligibility + reasons for its own convenience.

### 2.15 `RtCriticality` (enum, 3 values: `RT_CRITICAL`, `RT_NON_CRITICAL`, `UNKNOWN`)
- Fuente: `RtCriticalityRegistry.criticalityOf(exerciseId, exerciseVersion)` lookup against a hardcoded dossier
  list; fail-closed default `UNKNOWN`.
- Finalidad legítima: gates whether a run's RT-dependent signal is trustworthy (§18 C4) — replaces the retired
  hardcoded `{SPEED}` literal.
- Raw-fact vs derivado: a STATE about the MEASUREMENT DESIGN, not about the subject.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: a per-exercise measurement-design fact (does this exercise's contract need a frame-locked clock) —
  IDENTICAL for every subject who ran that exercise; carries ZERO subject-specific information by itself.
- Infer combinado (con `TIMEBASE_NON_AUTHORITATIVE` exclusion counts): reveals how often the subject's device
  failed to meet the exercise's timing bar — a DEVICE-CAPABILITY inference, not an ability inference.
- Por qué se conserva: needed to gate RT-sensitive metrics fail-closed without hardcoding a bare exercise-id set.

### 2.16 `RtCriticalityDeclaration(exerciseId, exerciseVersion, criticality)`
- Fuente: hardcoded dossier entries (today: exactly ONE — SPEED E8, `exerciseId="E8"`,
  `exerciseVersion="1.0.0-mc9d"`, `criticality=RT_CRITICAL`).
- Finalidad legítima: the versioned (exerciseId, exerciseVersion) -> criticality pin.
- Raw-fact vs derivado: static CATALOG data — not derived from any subject's runs at all; identical for every
  subject.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo/combinado: zero subject information — a static catalog record.
- Por qué se conserva: the single, sourced, auditable dossier entry the registry needs; adding a new one is a
  deliberate, reviewed act (per the registry's own KDoc), not a silent default.

### 2.17 `RtCriticalityRegistry` (class, 2 PRIVATE fields: `declared`, `fallback`)
- `declared: List<RtCriticalityDeclaration>` (private, defaults to `PRODUCTION_DECLARED`) — the injectable seam
  (tests can supply a different one; production always gets the fail-closed default). No subject data.
- `fallback: RtCriticality` (private, defaults to `UNKNOWN`) — the fail-closed default for any undeclared pair.
  No subject data.
- Consumer autorizado (hoy): NINGUNO.
- Nota: both fields are PRIVATE (implementation detail, not technically "public API" in the strict Kotlin-
  visibility sense) — pinned anyway by `EvidencePublicShapeGoldenTest` for defense-in-depth, since the contract
  asks for exhaustiveness over the WHOLE package, not only its `public`-modifier surface.
- Por qué se conserva: without an injectable seam, the `RT_NON_CRITICAL`/`RT_CRITICAL` gating paths would only be
  testable indirectly.

### 2.18 `ClockProvenance` (enum, 2 values: `AUTHORITATIVE_INJECTED`, `BEST_EFFORT_MONOTONIC`)
- Fuente: how the CALLER obtained "now" (`EvaluationAsOf.authoritative(...)` vs `.bestEffort(...)` factory used).
- Finalidad legítima: annotate clock trust WITHOUT weakening the future-exclusion rule (always applied
  regardless of provenance — a clock-skew safety valve, never a quality gate).
- Raw-fact vs derivado: raw fact about the CALLING environment's clock availability — no subject data
  whatsoever.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo/combinado: nothing about the subject.
- Por qué se conserva: lets a downstream reader distinguish an authoritative "now" from a best-effort one.

### 2.19 `EvaluationAsOf(epochMs, provenance)`
**`epochMs: Long`**
- Fuente: an injected, real epoch value (never a `Clock.System` read internally — keeps the extractor pure and
  deterministic).
- Finalidad legítima: the "as of" reference for future-exclusion.
- Raw-fact vs derivado: raw fact, but about the EXTRACTION CALL, not the subject.
- Consumer autorizado (hoy): NINGUNO.
- Infer solo: when an extraction was run — operational metadata.
- Infer combinado: none of subject-relevance.
- Por qué se conserva: required for the future-exclusion safety valve (§7/P1-09) — a bare `nowMs: Long` was
  replaced with this typed, provenance-carrying value.

**`provenance: ClockProvenance`** — see §2.18.

### 2.20 `SubjectScopedSessionBatch(subjectToken, runs)`
**`subjectToken: String`**
- Fuente: a pseudonymous scope label (namespace alias) supplied by the repository layer — NOT verified by this
  type itself (honest limitation documented in its own KDoc: "this type therefore CANNOT verify that runs
  genuinely belongs to one subject").
- Finalidad legítima: forces an explicit "this is a subject-scoped batch" assertion at every call site — a
  construction-time audit chokepoint, never an access-control boundary.
- Raw-fact vs derivado: pseudonymous, NOT anonymized (per its own KDoc: "can relink one subject's records but is
  not, by itself, an identifier").
- Consumer autorizado (hoy): NINGUNO. If this field were ever logged/serialized it would be a re-linkable
  pseudonymous identifier — `evidence_package_carries_no_serializable_annotation_yet` (boundary test) ensures no
  accidental persistence/serialization schema exists yet for this or any evidence type.
- Infer solo: identifies a scope, not a person, by itself.
- Infer combinado: none beyond what the scope label itself already is.
- Por qué se conserva: the entire subject-scoping discipline (the P0C-01 fix direction) depends on an explicit,
  typed, non-bare-`List` input — dropping this field would silently re-permit a bare, unscoped
  `List<CognitiveSessionRun>` call site.

**`runs: List<CognitiveSessionRun>`**
- Fuente: the raw run history handed in by the caller (defensively copied on construction).
- Finalidad legítima: the extractor's actual input data.
- Raw-fact vs derivado: this is an INPUT carrier, not an evidence-package OUTPUT field.
  `CognitiveSessionRun` (with its raw scores/timestamps) is a different, much richer type living in the PARENT
  `core.session.cognitive` package, entirely OUT of this contract's own output shape — the evidence types never
  re-expose raw run fields, only counts/spans/identity derived from them.
- Consumer autorizado (hoy): NINGUNO for this evidence contract's purposes. The raw runs list itself, if
  inspected directly (bypassing the extractor entirely), carries the FULL raw run detail — but that is the
  PARENT store/type's own concern (`SecureCognitiveSessionRunStore`), not something this evidence contract adds.
- Por qué se conserva: §7's own words — "the raw runs versioned are PRESERVED in the store... minimizing the DTO
  does not destroy evidence for append/recompute/auditory" — the minimization discipline applies to this
  contract's OWN OUTPUT, not retroactively to the store's already-reviewed input authority.

## 3. Version-constant singleton objects (not DTOs — pinned by value, §3 of the golden test)

| Object | Public member | Value | Nature |
|---|---|---|---|
| `LineagePolicy` | `VERSION` | `"measurement-lineage-v1"` | rule-version pin, no subject data |
| `SessionEvidenceStatePolicy` | `VERSION` | `"session-evidence-state-v2"` | rule-version pin, no subject data |
| `CognitiveSessionEvidenceExtractor` | `RULE_VERSION` | `"cognitive-session-evidence-v2"` | rule-version pin, no subject data |
| `RtCriticalityRegistry` (companion) | `VERSION` | `"rt-criticality-registry-v2"` | catalog-version pin, no subject data |

`SessionEvidenceGates` is `internal` — not part of this document's public-shape scope at all (module-only
visibility; excluded from both the golden test and this allowlist).

## 4. Consumer authorization — today: NONE

Every field/type in §2 and §3 has the SAME answer in its "consumer autorizado" column: **NINGUNO**. This is
enforced mechanically, not by convention:
- `evidenceConsumerSweepScanner` (Gradle task, `check`-wired) + its desktopTest twin
  (`no_commonMain_file_outside_the_evidence_package_references_its_public_types`) fail the build if any
  commonMain file outside this package references one of its public type names — the allowlist is empty except
  for one pre-existing KDoc cross-reference (`SecureCognitiveSessionRunStore.kt`, a doc mention, never an import
  or construction).
- No `profile.*` import exists anywhere in the package (`evidenceBoundaryScanner` + its twin).
- No wiring to an aggregator/remote sink exists (`Firestore`/`SyncQueue`/`.aggregate(`/`MindBoostProfileService`
  all banned by the same boundary test).

## 5. Manual §13 procedure — RETIRED

The reopen doc's §13 gate battery included a **one-time landing verification**: "temporarily add
`val staleness: Double` to `ExerciseScaleEvidence.kt`, watch `:shared:evidenceBoundaryScanner` +
`:shared:desktopTest` go RED, then revert, never commit the temporary edit." That manual procedure is **retired
as a gate requirement** by this pass. Its purpose — proving the scanner actually discriminates a forbidden shape
from a legitimate one — is now served permanently and repeatably by:
- `EvidenceShapeScanRulesTest` (pre-existing, untouched) — 8 Tier-1 RED + 10 Tier-2 RED + 4 GREEN control cases.
- `EvidenceShapeBypassMutationTest` (new, this pass) — 10 additional RED bypass-vector cases (nullable/collection
  Double/Float, `Duration`, `typealias`, bare `Map`/`Any`) + 6 HONEST BLIND-SPOT cases (scaled Int/Long, String
  magnitude, embedded `.toDouble()`, extension property, renamed wrapper type) + a meta-test
  (`mutationSuiteActuallyFailsOnEachBypass`) proving the whole suite is not a vacuous no-op.
- `EvidencePublicShapeGoldenTest` (new, this pass) — the exhaustive exact-shape golden that DOES catch a real
  `val staleness: Double` (or any other) addition to `ExerciseScaleEvidence` or any of the other 19 pinned types.

No agent should re-run the manual §13 procedure against the real tree going forward; the automated suite above is
the permanent substitute the CEO's contract asked for.

## 6. Honest residual gaps

Two genuinely uncaught gaps were found and are reported here, not silently fixed (production is frozen for this
pass):

1. **A new extension property or top-level function declared INSIDE the evidence package**, computing a scaled
   ratio via `Int`/`Long` from existing raw facts (e.g. `val CognitiveSessionEvidence.includedPercent: Int
   get() = ...`), and avoiding every Tier-1/Tier-2 token, is invisible to **both** the text scanner (no banned
   token) **and** the golden test (not a stored field on any `data class`, so it never appears in
   `declaredFields`). It is NOT a re-introduction of a removed FIELD, so neither barrier's specific job (stop
   reintroduction of a banned FIELD SHAPE) covers it. It IS caught by the already-landed
   `evidenceConsumerSweepScanner` ONLY if the extension references a protected type name from OUTSIDE the
   package — an in-package extension referencing its own receiver type by name is not itself the cross-package
   wiring that scanner targets. This is the single most significant residual gap this pass found; closing it
   would require a NEW rule (e.g. "no computed property/top-level function may exist in this package outside the
   pure extractor's own private helpers") that is out of this pass's frozen-production scope — flagged for the
   next review wave.
2. **A brand-new, differently-named wrapper type** (e.g. `data class Magnitude(val raw: Int)`) is invisible to
   the text scanner (no banned token) and to the golden test (not yet in its enumerated inventory of 20 types) —
   inherent to any "enumerate & pin" strategy. Closing this fully would require either a positive allowlist of
   ALL type declarations in the package (fail if an UNLISTED top-level type appears at all) or continuous manual
   review at each change; this pass adds the mutation test's honest disclosure (§ above) rather than a new
   structural rule, since inventing a "no new top-level type may be added without a matching golden entry" gate
   is itself a meaningful new mechanical barrier that should be a reviewed design decision, not a byproduct of a
   test-authoring pass.

Both gaps are demonstrated as failing (empty scan result) by dedicated tests in `EvidenceShapeBypassMutationTest`
so they are falsifiable claims, not prose assertions.
