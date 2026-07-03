package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.Serializable

/**
 * Confounder/fatigue context entities (v6 §4.4, §5.6). These are *inputs* the scoring spine
 * consumes to decide whether an attempt may update the profile. The engines that *compute*
 * them from RT trends and exposure history (FatigueGate, ConfounderEngine) are a later
 * slice; here they are plain value carriers so scoring can be tested in isolation.
 */

/** Fatigue snapshot. [level] is 0.0 (fresh) .. 1.0 (exhausted). */
@Serializable
data class FatigueState(
    val level: Proportion,
    val risingLatency: Boolean = false,
) {
    /** High fatigue forbids strong profile updates (§4.4, §5.6). */
    val isHigh: Boolean get() = level.value >= HIGH_THRESHOLD

    companion object {
        const val HIGH_THRESHOLD = 0.7
        val FRESH = FatigueState(Proportion.ZERO, risingLatency = false)
    }
}

/**
 * Confounder risk snapshot (§5.6). Any high risk means the attempt is informative for
 * feedback but not for strongly moving the ability estimate.
 */
@Serializable
data class ConfounderAssessment(
    val priorKnowledgeRisk: DependencyLevel = DependencyLevel.LOW,
    val variantBiasRisk: Boolean = false,
    val externalDomainUndeclared: Boolean = false,
) {
    val hasHighPriorKnowledge: Boolean get() = priorKnowledgeRisk == DependencyLevel.HIGH

    companion object {
        val NONE = ConfounderAssessment()
    }
}
