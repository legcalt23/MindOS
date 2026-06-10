package com.mindos.app.features.mindboost.memory.domain.metrics

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryTrialOutcome

/**
 * Computes memory metrics from scored trials. Pure, deterministic, no platform.
 * The honesty guarantees live here: delayed recall is a first-class metric, every
 * value carries its sample size and a confidence flag (doc §6 / §13), and central
 * tendency (RT) uses MAD-filtered medians while event counts (intrusions) use raw
 * data (project learning 2026-06-04).
 */
interface MemoryMetricsEngine {

    /** Metrics for a single scored trial/block of one exercise. */
    fun blockMetrics(trial: ScoredTrial): MemoryMetricSet

    /**
     * Consolidation: retention of delayed vs immediate recall (doc §6). Pairwise —
     * fed two accuracy measurements of the SAME material (session aggregation, Phase 7).
     */
    fun consolidationIndex(immediateAccuracy: Double, delayedAccuracy: Double, n: Int): MemoryMetricValue

    /**
     * Dual-task cost: proportional accuracy drop from single- to dual-task condition
     * (doc §6). Pairwise — fed by session aggregation (Phase 7).
     */
    fun dualTaskCost(singleAccuracy: Double, dualAccuracy: Double, n: Int): MemoryMetricValue
}

/** Production metrics engine. */
class DefaultMemoryMetricsEngine : MemoryMetricsEngine {

    override fun blockMetrics(trial: ScoredTrial): MemoryMetricSet = buildMap {
        putCommon(trial)
        putRetrievalSpeed(trial)
        putExerciseSpecific(trial)
    }

    private fun MutableMap<MemoryMetricKey, MemoryMetricValue>.putCommon(trial: ScoredTrial) {
        val accuracyKey =
            if (trial.delayMs > 0) MemoryMetricKey.DELAYED_RECALL_ACCURACY
            else MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY
        put(accuracyKey, proportion(accuracyKey, trial.hits, trial.studiedCount))

        put(
            MemoryMetricKey.INTRUSION_RATE,
            proportion(MemoryMetricKey.INTRUSION_RATE, trial.count(MemoryTrialOutcome.INTRUSION), trial.responses.size),
        )

        val omissions =
            if (trial.slotAligned) trial.count(MemoryTrialOutcome.OMISSION)
            else (trial.studiedCount - trial.hits).coerceAtLeast(0)
        put(MemoryMetricKey.OMISSION_RATE, proportion(MemoryMetricKey.OMISSION_RATE, omissions, trial.studiedCount))
    }

    private fun MutableMap<MemoryMetricKey, MemoryMetricValue>.putRetrievalSpeed(trial: ScoredTrial) {
        val hitRts = trial.responses
            .filter { it.outcome == MemoryTrialOutcome.HIT }
            .mapNotNull { it.rtMs?.value?.toDouble() }
        if (hitRts.isEmpty()) return
        val median = MemoryStatistics.median(MemoryStatistics.rejectOutliers(hitRts))
        put(
            MemoryMetricKey.RETRIEVAL_SPEED,
            MemoryMetricValue(
                key = MemoryMetricKey.RETRIEVAL_SPEED,
                value = median,
                n = hitRts.size,
                confident = hitRts.size >= MIN_N,
                unit = MemoryMetricUnit.MILLISECONDS,
            ),
        )
    }

    private fun MutableMap<MemoryMetricKey, MemoryMetricValue>.putExerciseSpecific(trial: ScoredTrial) {
        when (trial.exerciseId) {
            MemoryExerciseId.EX1_SPAN_COMPLEJO -> put(
                MemoryMetricKey.WORKING_MEMORY_SPAN,
                MemoryMetricValue(
                    key = MemoryMetricKey.WORKING_MEMORY_SPAN,
                    value = trial.hits.toDouble(), // partial-credit unit score
                    n = trial.studiedCount,
                    confident = trial.studiedCount >= MIN_N,
                    unit = MemoryMetricUnit.SPAN,
                ),
            )

            MemoryExerciseId.EX3_LISTA_ESTRATEGIA -> putArcClustering(trial)

            MemoryExerciseId.EX4_PARES_ASOCIADOS -> {
                put(
                    MemoryMetricKey.ASSOCIATIVE_INDEX,
                    proportion(MemoryMetricKey.ASSOCIATIVE_INDEX, trial.hits, trial.studiedCount),
                )
                put(
                    MemoryMetricKey.CONJUNCTION_ERROR_RATE,
                    proportion(
                        MemoryMetricKey.CONJUNCTION_ERROR_RATE,
                        trial.count(MemoryTrialOutcome.CONJUNCTION_ERROR),
                        trial.responses.size,
                    ),
                )
            }

            else -> Unit // other exercises are out of the V1 wave
        }
    }

    private fun MutableMap<MemoryMetricKey, MemoryMetricValue>.putArcClustering(trial: ScoredTrial) {
        val categories = trial.responses
            .filter { it.outcome == MemoryTrialOutcome.HIT }
            .mapNotNull { it.category }
        val arc = ArcClustering.arcIndex(categories)
        put(
            MemoryMetricKey.ARC_CLUSTERING,
            if (arc == null) {
                MemoryMetricValue.unavailable(MemoryMetricKey.ARC_CLUSTERING, MemoryMetricUnit.INDEX, categories.size)
            } else {
                MemoryMetricValue(
                    key = MemoryMetricKey.ARC_CLUSTERING,
                    value = arc,
                    n = categories.size,
                    confident = categories.size >= MIN_ARC_N,
                    unit = MemoryMetricUnit.INDEX,
                )
            },
        )
    }

    override fun consolidationIndex(immediateAccuracy: Double, delayedAccuracy: Double, n: Int): MemoryMetricValue {
        if (immediateAccuracy <= 0.0) {
            return MemoryMetricValue.unavailable(MemoryMetricKey.CONSOLIDATION_INDEX, MemoryMetricUnit.RATIO, n)
        }
        return MemoryMetricValue(
            key = MemoryMetricKey.CONSOLIDATION_INDEX,
            value = (delayedAccuracy / immediateAccuracy).coerceAtLeast(0.0),
            n = n,
            confident = n >= MIN_N,
            unit = MemoryMetricUnit.RATIO,
        )
    }

    override fun dualTaskCost(singleAccuracy: Double, dualAccuracy: Double, n: Int): MemoryMetricValue {
        if (singleAccuracy <= 0.0) {
            return MemoryMetricValue.unavailable(MemoryMetricKey.DUAL_TASK_COST, MemoryMetricUnit.RATIO, n)
        }
        return MemoryMetricValue(
            key = MemoryMetricKey.DUAL_TASK_COST,
            value = (singleAccuracy - dualAccuracy) / singleAccuracy,
            n = n,
            confident = n >= MIN_N,
            unit = MemoryMetricUnit.RATIO,
        )
    }

    /** Builds a PROPORTION metric, marked not-confident when the sample is too small or empty. */
    private fun proportion(key: MemoryMetricKey, numerator: Int, denominator: Int): MemoryMetricValue {
        if (denominator <= 0) return MemoryMetricValue.unavailable(key, MemoryMetricUnit.PROPORTION, denominator)
        return MemoryMetricValue(
            key = key,
            value = numerator.toDouble() / denominator,
            n = denominator,
            confident = denominator >= MIN_N,
            unit = MemoryMetricUnit.PROPORTION,
        )
    }

    private companion object {
        /** Minimum sample size to trust a proportion / span / RT estimate. */
        const val MIN_N = 4

        /** Minimum recalled items to trust an ARC clustering estimate. */
        const val MIN_ARC_N = 5
    }
}
