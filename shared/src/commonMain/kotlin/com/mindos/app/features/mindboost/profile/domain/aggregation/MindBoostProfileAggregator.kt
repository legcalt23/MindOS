package com.mindos.app.features.mindboost.profile.domain.aggregation

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.OverallSummary

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
 */
class MindBoostProfileAggregator(private val config: AggregationConfig = AggregationConfig()) {

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
        val status = if (confidence >= config.establishedConfidence) {
            DimensionStatus.ESTABLISHED
        } else {
            DimensionStatus.EMERGING
        }
        return DimensionState(
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

    private fun summarize(states: Map<CognitiveDimension, DimensionState>): OverallSummary? {
        val established = states.values.filter {
            it.status == DimensionStatus.ESTABLISHED && it.value != null
        }
        if (established.size < config.minEstablishedForComposite) {
            return OverallSummary(
                compositeIndex = null,
                establishedDimensionCount = established.size,
                strongest = null,
                developing = null,
            )
        }
        val composite = established.mapNotNull { it.value }.average()
        return OverallSummary(
            compositeIndex = composite,
            establishedDimensionCount = established.size,
            strongest = established.maxBy { it.value ?: 0.0 }.dimension,
            developing = established.minBy { it.value ?: 1.0 }.dimension,
        )
    }
}
