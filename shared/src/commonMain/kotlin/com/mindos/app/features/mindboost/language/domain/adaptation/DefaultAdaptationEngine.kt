package com.mindos.app.features.mindboost.language.domain.adaptation

import com.mindos.app.features.mindboost.language.domain.model.DependencyLevel

/**
 * Default [AdaptationEngine] (v6 §6.4). Pure, offline. Scores each candidate with a weighted
 * utility and picks the maximum:
 *
 * ```
 * utility = wInfo·expectedInformation + wPriority·constructPriority + wRepair·repairValue
 *         + wTransfer·transferValue   + wNovelty·novelty
 *         − wExposure·exposureRisk    − wFatigue·fatigueLoad − wConfounder·confounderRisk
 * ```
 *
 * Repeated items (exposure) and high-load items under fatigue are penalized, so the engine
 * naturally avoids them without hard exclusion. This is an explainable approximation; the path
 * to IRT/CAT item information is left open (swap [expectedInformation]).
 */
class DefaultAdaptationEngine : AdaptationEngine {

    override fun selectNext(
        candidates: List<ItemCandidate>,
        context: AdaptationContext,
    ): AdaptationDecision {
        if (candidates.isEmpty()) {
            return AdaptationDecision(null, emptyList(), "No hay ítems candidatos disponibles.")
        }
        val ranked = candidates.map { score(it, context) }.sortedByDescending { it.utility }
        val top = ranked.first()
        return AdaptationDecision(top.candidate, ranked, rationaleFor(top))
    }

    private fun score(candidate: ItemCandidate, context: AdaptationContext): ScoredCandidate {
        val information = context.profile.traitEstimates[candidate.primaryTrait]
            ?.let { 1.0 - it.reliability } ?: 1.0
        val priority = if (candidate.primaryTrait in context.goalTraits) 1.0 else NON_GOAL_PRIORITY
        val repair = if (candidate.repairTargets.any { it in context.activeRepairTargets }) 1.0 else 0.0
        val exposureRisk = (context.exposure.timesSeen(candidate.taskId).toDouble() / MAX_EXPOSURE)
            .coerceIn(0.0, 1.0)
        val novelty = 1.0 - exposureRisk
        val fatigueLoad = candidate.fatigueCost * (FATIGUE_BASE_LOAD + context.fatigue.level.value)
        val confounder = (dependencyWeight(candidate.priorKnowledgeDependency) +
            if (candidate.variantBiasRisk) 1.0 else 0.0) / CONFOUNDER_NORMALIZER

        val components = mapOf(
            "information" to W_INFO * information,
            "priority" to W_PRIORITY * priority,
            "repair" to W_REPAIR * repair,
            "transfer" to W_TRANSFER * candidate.transferDistance,
            "novelty" to W_NOVELTY * novelty,
            "exposurePenalty" to -W_EXPOSURE * exposureRisk,
            "fatiguePenalty" to -W_FATIGUE * fatigueLoad,
            "confounderPenalty" to -W_CONFOUNDER * confounder,
        )
        return ScoredCandidate(candidate, components.values.sum(), components)
    }

    private fun dependencyWeight(level: DependencyLevel): Double = when (level) {
        DependencyLevel.LOW -> 0.0
        DependencyLevel.MEDIUM -> MEDIUM_DEPENDENCY_WEIGHT
        DependencyLevel.HIGH -> 1.0
    }

    private fun rationaleFor(top: ScoredCandidate): String {
        val dominant = top.components.filterValues { it > 0.0 }.maxByOrNull { it.value }?.key
        val reason = when (dominant) {
            "information" -> "aporta la mayor información de medición"
            "repair" -> "trabaja un patrón de error activo"
            "priority" -> "encaja con tu objetivo"
            "transfer" -> "favorece la transferencia"
            "novelty" -> "es material nuevo para ti"
            else -> "equilibra información y carga"
        }
        return "Seleccioné ${top.candidate.taskId.value} porque $reason."
    }

    companion object {
        // Utility weights — mirror the §6.4 NextActionScore coefficients.
        const val W_INFO = 0.22
        const val W_PRIORITY = 0.18
        const val W_REPAIR = 0.20
        const val W_TRANSFER = 0.15
        const val W_NOVELTY = 0.07
        const val W_EXPOSURE = 0.18
        const val W_FATIGUE = 0.14
        const val W_CONFOUNDER = 0.12

        const val NON_GOAL_PRIORITY = 0.3
        const val FATIGUE_BASE_LOAD = 0.5
        const val MEDIUM_DEPENDENCY_WEIGHT = 0.5
        const val CONFOUNDER_NORMALIZER = 2.0
        const val MAX_EXPOSURE = 5.0
    }
}
