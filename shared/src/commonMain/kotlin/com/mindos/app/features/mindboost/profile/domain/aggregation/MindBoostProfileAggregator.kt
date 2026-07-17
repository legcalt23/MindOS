package com.mindos.app.features.mindboost.profile.domain.aggregation

import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.features.mindboost.profile.domain.calibration.CalibrationPolicy
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.OverallSummary
import com.mindos.app.features.mindboost.profile.domain.model.ProvisionalGlobalIndex

/**
 * Gating thresholds for aggregation. A contribution only counts once it clears [minSufficiency] AND
 * [minConfidence]; a dimension is `ESTABLISHED` once its aggregated confidence reaches
 * [establishedConfidence]; the composite index needs at least [minEstablishedForComposite] established
 * dimensions. Defaults are conservative so the profile under-claims rather than over-claims.
 */
@Suppress("MagicNumber") // Defaults ARE the documented domain thresholds.
data class AggregationConfig(
    val minSufficiency: Double = 0.5,
    val minConfidence: Double = 0.4,
    val establishedConfidence: Double = 0.7,
    val minEstablishedForComposite: Int = 3,
)

/**
 * Pure engine that folds module [ModuleContribution]s into a [MindBoostProfile]. Never fabricates: a
 * dimension with no qualifying contribution stays `CALIBRATING` with a null value (never a silent 0).
 * Deterministic and side-effect-free, so it is fully unit-testable.
 *
 * [globalScoreEnabled] gates the cross-dimension composite (SYSTEM-1 Slice 6-R3, C2.5): by default it reads
 * the live [FeatureFlagsHub] (`mindboostGlobalScoreEnabled`, OFF everywhere), and is injectable so tests never
 * have to mutate global state. This is the ONLY authority for the composite/global barrier — no other class
 * duplicates this gate.
 */
class MindBoostProfileAggregator(
    private val config: AggregationConfig = AggregationConfig(),
    private val globalScoreEnabled: () -> Boolean = { FeatureFlagsHub.current.mindboostGlobalScoreEnabled },
) {

    companion object {
        /**
         * OFFICIAL composite scientific-approval gate (SCO-015). Hardcoded `false`: ADR-SCI-006 (the global
         * weighting/correlation model) is not approved. Flipping this to `true` is a deliberate, reviewed CODE
         * CHANGE citing the approved ADR — NEVER a runtime/remote flag. SYSTEM-1 6R3 R3-FINAL §34 D5: this const
         * alone is INSUFFICIENT to unlock an official composite. The former gate (all 9 `CognitiveDimension`
         * ESTABLISHED) was a category error — charter §20.2 requires the 8 visible MOTORS §11-CALIBRATED and
         * cross-comparable (8 motors ≠ 9 dimensions; ESTABLISHED is a confidence band, not calibration). A real
         * official composite additionally requires an 8-motor→required-dimension map, per-motor CALIBRATED state,
         * an ADR-SCI-008 equating bridge, and an approved weighting model — none of which exist. So [summarize]
         * holds `compositeIndex = null` structurally; a defensible official number is a future code change, not a
         * const flip.
         */
        const val ADR_SCI_006_APPROVED: Boolean = false

        /**
         * PROVISIONAL global-proxy scientific-approval gate (SYSTEM-1 6R3 R3-FINAL §33/§34 D4). A code constant,
         * NOT a runtime/remote flag: flipping `mindboostGlobalScoreEnabled` ON must NEVER surface a global NUMBER.
         * Today [ProvisionalGlobalIndex] carries no scalar at all (coverage only), so this const is a forward
         * barrier: reintroducing a provisional number requires this `true` (a reviewed code change) PLUS an
         * approved ADR-SCI-006 weighting model + ADR-SCI-008 comparability bridge + inseparable uncertainty
         * presentation. `false` today ⇒ coverage-without-number is the only shape the profile can hold.
         */
        const val PROVISIONAL_GLOBAL_PROXY_APPROVED: Boolean = false
    }

    fun aggregate(
        userId: String,
        contributions: List<ModuleContribution>,
        nowMs: Long,
    ): MindBoostProfile {
        val byDimension = contributions.groupBy { it.dimension }
        val states = CognitiveDimension.entries.associateWith { dimension ->
            buildDimensionState(dimension, byDimension[dimension].orEmpty())
        }
        return MindBoostProfile(
            userId = userId,
            dimensions = states,
            overall = summarize(states),
            updatedAtMs = nowMs,
        )
    }

    private fun buildDimensionState(
        dimension: CognitiveDimension,
        contributions: List<ModuleContribution>,
    ): DimensionState {
        val qualifying = contributions.filter {
            it.normalizedScore != null &&
                it.dataSufficiency >= config.minSufficiency &&
                it.confidence >= config.minConfidence
        }
        if (qualifying.isEmpty()) return DimensionState.calibrating(dimension)

        val totalWeight = qualifying.sumOf { it.confidence * it.dataSufficiency }
        val value = if (totalWeight <= 0.0) {
            qualifying.mapNotNull { it.normalizedScore }.average()
        } else {
            qualifying.sumOf { (it.normalizedScore ?: 0.0) * it.confidence * it.dataSufficiency } / totalWeight
        }
        val dominant = qualifying.maxBy { it.confidence }
        val confidence = qualifying.maxOf { it.confidence }
        val confidenceStatus = if (confidence >= config.establishedConfidence) {
            DimensionStatus.ESTABLISHED
        } else {
            DimensionStatus.EMERGING
        }
        // DC-3: a session-based module caps the dimension's maturity by its valid cross-session evidence.
        // Absent evidence (trial-accumulation modules) leaves the confidence-derived status untouched; a
        // status capped below EMERGING means too little cross-session evidence to display → CALIBRATING (null).
        val status = dominant.sessionEvidence
            ?.let { CalibrationPolicy.applyCeiling(confidenceStatus, it) }
            ?: confidenceStatus
        return if (status == DimensionStatus.CALIBRATING) {
            DimensionState.calibrating(dimension)
        } else {
            DimensionState(
                dimension = dimension,
                value = value.coerceIn(0.0, 1.0),
                confidence = confidence,
                dataSufficiency = qualifying.maxOf { it.dataSufficiency },
                trend = dominant.trend,
                status = status,
                preliminary = qualifying.any { it.preliminary },
                contributingModules = qualifying.map { it.moduleId },
            )
        }
    }

    private fun summarize(states: Map<CognitiveDimension, DimensionState>): OverallSummary {
        val established = states.values.filter {
            it.status == DimensionStatus.ESTABLISHED && it.value != null
        }
        if (established.size < config.minEstablishedForComposite) {
            return OverallSummary(
                compositeIndex = null,
                provisionalIndex = null,
                establishedDimensionCount = established.size,
                strongest = null,
                developing = null,
            )
        }
        return OverallSummary(
            // §34 D5: the official composite is structurally null — no unweighted-mean-over-dimensions formula; a
            // defensible official number needs an approved ADR-SCI-006/008 model (see ADR_SCI_006_APPROVED KDoc).
            compositeIndex = null,
            provisionalIndex = provisionalCompositeIndex(established),
            establishedDimensionCount = established.size,
            strongest = established.maxBy { it.value ?: 0.0 }.dimension,
            developing = established.minBy { it.value ?: 1.0 }.dimension,
        )
    }

    /**
     * SCO-014 PROVISIONAL global COVERAGE view: only computed while [globalScoreEnabled] returns true (the
     * `mindboostGlobalScoreEnabled` kill switch, default OFF everywhere). SYSTEM-1 6R3 R3-FINAL §33/§34 D4:
     * carries NO composite number — only which dimensions are covered vs missing — so no flag flip can surface a
     * psychometrically-indefensible unweighted mean. NEVER feeds ranking.
     */
    private fun provisionalCompositeIndex(established: List<DimensionState>): ProvisionalGlobalIndex? =
        if (!globalScoreEnabled()) {
            null
        } else {
            val covered = established.map { it.dimension }
            ProvisionalGlobalIndex(
                coveredDimensions = covered,
                missingDimensions = CognitiveDimension.entries.filterNot { it in covered },
            )
        }
}
