package com.mindos.app.features.mindboost.language.domain.adaptation

/**
 * Adaptation policy (v6 §3.4, §6.4). Selects the next item to maximize evidence/learning value
 * while penalizing exposure, fatigue and confounders — never optimizing streaks or time-in-app.
 */
fun interface AdaptationEngine {
    fun selectNext(candidates: List<ItemCandidate>, context: AdaptationContext): AdaptationDecision
}
