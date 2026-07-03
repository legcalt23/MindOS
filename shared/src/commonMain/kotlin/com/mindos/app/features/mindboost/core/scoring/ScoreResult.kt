package com.mindos.app.features.mindboost.core.scoring

/**
 * INFRA-01 · Universal, read-only scoring VERDICT for one session of any module. It is NOT an engine:
 * it is derived from the module's native score via a (future) adapter. It answers two questions with
 * auditable honesty:
 *   1. Was it playable? (always yes unless crash) — independent of state.
 *   2. May it UPDATE the cognitive profile? — ONLY if state == VALID ([canUpdateProfile]).
 *
 * Invariants enforced in [init], not by convention:
 *  - `null != 0`: a non-VALID state can never carry a primary signal.
 *  - R-NO-REDUCE: a non-VALID state can never set [updatesProfile] = true.
 *
 * Construction goes through [ScoreResultFactory] (the state machine is the authority); the constructor
 * is a defensive last line.
 */
@Suppress("LongParameterList") // A boundary record aggregating cohesive sub-blocks; see doc 04 §2.
data class ScoreResult(
    val provenance: ScoreProvenance,
    val state: ScoreState,
    val primarySignal: Signal?,
    val secondarySignals: List<Signal>,
    val sampling: Sampling,
    val updatesProfile: Boolean,
    val preliminary: Boolean,
    val dataQuality: DataQuality,
    val claimLevel: ClaimLevel,
    val copySafetyFlags: Set<CopySafetyFlag>,
) {
    init {
        require((state == ScoreState.VALID) == (primarySignal != null)) {
            "VALID iff primarySignal present (null != 0)."
        }
        require(state == ScoreState.VALID || !updatesProfile) {
            "A non-VALID state must never update the profile (R-NO-REDUCE)."
        }
    }
}

/** True iff this verdict may write to the profile — guaranteed `== (state == VALID)` by [init]. */
fun ScoreResult.canUpdateProfile(): Boolean = updatesProfile

/** Who/when/which-version produced the score. [pseudoUserId] is a device-bound hash, never a raw uid. */
@Suppress("LongParameterList") // Provenance record; cohesive identity metadata.
data class ScoreProvenance(
    val exerciseId: String,
    val moduleId: ScoreModuleId,
    val sessionId: String,
    val pseudoUserId: String,
    val timestampMs: Long,
    val scoringVersion: String,
    val itemBankVersion: String? = null,
)

/** Device/measurement quality inputs. [frameDropFraction] is HARD-populated by INFRA-02, not constant. */
@Suppress("MagicNumber") // [0,1] bounds are domain invariants, not magic.
data class DataQuality(
    val deviceTimingQuality: TimebaseQuality,
    val criticalFrameJitter: Boolean,
    val frameDropFraction: Double,
    val itemBankIntegrity: BankIntegrity,
    val metricStability: MetricStability,
    val invalidatedBy: InvalidationReason?,
) {
    init { require(frameDropFraction in 0.0..1.0) { "frameDropFraction must be in [0,1]." } }
}

/** Sampling sufficiency for the metric cell (INFRA-06 supplies the nMin table). */
@Suppress("MagicNumber") // [0,1] bound is a domain invariant.
data class Sampling(
    val nEffective: Int,
    val minNRequired: Int,
    val confidence: Double,
    val crossSessionAggregated: Boolean,
    val confidenceInterval: ClosedRange<Double>?,
) {
    val meetsMinN: Boolean get() = nEffective >= minNRequired

    init { require(confidence in 0.0..1.0) { "confidence must be in [0,1]." } }
}

/** A single signal. `value == null` ALWAYS means "not computable", NEVER "low performance". */
@Suppress("LongParameterList") // Signal record with optional bounds/metadata.
data class Signal(
    val key: SignalKey,
    val value: Double?,
    val lowerBound: Double?,
    val upperBound: Double?,
    val unit: SignalUnit,
    val uncertaintyLabel: UncertaintyLabel,
    val required: Boolean = false,
)

/**
 * Anti-gaming inputs: whether a real detector fired, and how much tested coverage exists. Invariant
 * (Fase 0): [DetectorCoverage.ADEQUATE]/[DetectorCoverage.PARTIAL] may only be supplied by a scorer that
 * invokes a tested anti-gaming control — enforced by `AdequateCoverageInvariantTest`.
 */
data class AntiGaming(
    val isClean: Boolean,
    val detectorCoverage: DetectorCoverage,
)
