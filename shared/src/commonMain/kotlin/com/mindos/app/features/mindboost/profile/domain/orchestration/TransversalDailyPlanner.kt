package com.mindos.app.features.mindboost.profile.domain.orchestration

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile

/**
 * Pure, deterministic CROSS-module daily planner. Where [MindBoostProfileOrchestrator] picks ONE module,
 * this ranks the whole [available] set and returns the top [maxItems] as a "plan de hoy".
 *
 * Same honest priority tiers as the single recommender:
 *  1. CALIBRATING (still no reading) — ordered by MOST data first (closest to the first reading, most
 *     motivating), so the profile stops being blank sooner. Reason: GATHER_DATA.
 *  2. EMERGING (has a value, low confidence) — ordered by LEAST confident first. Reason: GATHER_DATA.
 *  3. ESTABLISHED — ordered by LOWEST value first ("area to develop", never "weak"). Reason: DEVELOP_AREA.
 * Ties break by [CognitiveModuleId] ordinal for full determinism.
 *
 * Exclusions (BETA/practice/unwired tracks) are the CALLER's job via [available] — the planner never
 * invents a module. Empty [available] → empty plan (the UI simply shows no card).
 */
class TransversalDailyPlanner {

    fun plan(
        profile: MindBoostProfile,
        available: Set<CognitiveModuleId>,
        maxItems: Int = DEFAULT_MAX_ITEMS,
    ): List<TrainingRecommendation> {
        if (available.isEmpty() || maxItems <= 0) return emptyList()
        return available
            .sortedWith(rankComparator(profile))
            .take(maxItems)
            .map { module ->
                val status = profile.dimension(module.primaryDimension).status
                TrainingRecommendation(module, module.primaryDimension, reasonFor(status))
            }
    }

    private fun rankComparator(profile: MindBoostProfile): Comparator<CognitiveModuleId> =
        compareBy<CognitiveModuleId> { tier(profile.dimension(it.primaryDimension).status) }
            .thenBy { withinTierKey(profile.dimension(it.primaryDimension)) }
            .thenBy { it.ordinal }

    private fun tier(status: DimensionStatus): Int = when (status) {
        DimensionStatus.CALIBRATING -> 0
        DimensionStatus.EMERGING -> 1
        DimensionStatus.ESTABLISHED -> 2
    }

    /** Ascending sort key within a tier (see class doc for the intent of each tier's ordering). */
    private fun withinTierKey(state: DimensionState): Double = when (state.status) {
        DimensionStatus.CALIBRATING -> -state.dataSufficiency // most data first
        DimensionStatus.EMERGING -> state.confidence // least confident first
        DimensionStatus.ESTABLISHED -> state.value ?: 1.0 // lowest value first
    }

    private fun reasonFor(status: DimensionStatus): RecommendationReason = when (status) {
        DimensionStatus.CALIBRATING, DimensionStatus.EMERGING -> RecommendationReason.GATHER_DATA
        DimensionStatus.ESTABLISHED -> RecommendationReason.DEVELOP_AREA
    }

    companion object {
        const val DEFAULT_MAX_ITEMS = 3
    }
}
