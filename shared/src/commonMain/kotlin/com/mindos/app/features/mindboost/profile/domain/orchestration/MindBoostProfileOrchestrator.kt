package com.mindos.app.features.mindboost.profile.domain.orchestration

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile

/** Why the orchestrator suggests a module. Reason CODES, not free clinical text. */
enum class RecommendationReason {
    /** No module is currently available to train. */
    NONE_AVAILABLE,

    /** Pick this to gather more data (the dimension is calibrating or its confidence is still low). */
    GATHER_DATA,

    /** Pick this to develop your most-developing established area. */
    DEVELOP_AREA,
}

/** A single, descriptive training suggestion. */
data class TrainingRecommendation(
    val moduleId: CognitiveModuleId?,
    val dimension: CognitiveDimension?,
    val reason: RecommendationReason,
)

/**
 * Pure, deterministic recommender. Priority: dimensions still calibrating, then established-but-low-
 * confidence ("emerging"), then the lowest-valued established dimension. Non-clinical: it frames the
 * lowest area as "to develop", never "deficient". Ties break by [CognitiveModuleId] ordinal for
 * determinism.
 */
class MindBoostProfileOrchestrator {

    fun recommend(
        profile: MindBoostProfile,
        available: Set<CognitiveModuleId>,
    ): TrainingRecommendation {
        if (available.isEmpty()) {
            return TrainingRecommendation(null, null, RecommendationReason.NONE_AVAILABLE)
        }

        val ordered = available.sortedBy { it.ordinal }
        val calibrating = ordered.firstOrNull {
            profile.dimension(it.primaryDimension).status == DimensionStatus.CALIBRATING
        }
        val emerging = ordered
            .filter { profile.dimension(it.primaryDimension).status == DimensionStatus.EMERGING }
            .minByOrNull { profile.dimension(it.primaryDimension).confidence }
        val establishedLowest = ordered
            .filter { profile.dimension(it.primaryDimension).status == DimensionStatus.ESTABLISHED }
            .minByOrNull { profile.dimension(it.primaryDimension).value ?: 1.0 }

        val (module, reason) = when {
            calibrating != null -> calibrating to RecommendationReason.GATHER_DATA
            emerging != null -> emerging to RecommendationReason.GATHER_DATA
            else -> (establishedLowest ?: ordered.first()) to RecommendationReason.DEVELOP_AREA
        }
        return TrainingRecommendation(module, module.primaryDimension, reason)
    }
}
