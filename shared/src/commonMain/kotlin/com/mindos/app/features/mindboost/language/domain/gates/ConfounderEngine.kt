package com.mindos.app.features.mindboost.language.domain.gates

import com.mindos.app.features.mindboost.language.domain.model.ConfounderAssessment
import com.mindos.app.features.mindboost.language.domain.model.DependencyLevel
import kotlinx.serialization.Serializable

/**
 * Confounder assessment (v6 §5.6). Computes the [ConfounderAssessment] scoring consumes, so an
 * attempt that is informative for feedback but contaminated for measurement does not strongly
 * move the ability estimate.
 */
fun interface ConfounderEngine {
    fun assess(signals: ConfounderSignals): ConfounderAssessment
}

/**
 * Inputs from the item + the response. [suspiciousSpeedAccuracy] = an ultra-fast, fully-correct
 * answer that suggests prior knowledge rather than in-task reasoning (§5.6 "alto riesgo de
 * conocimiento previo"). [variantValidated] false means the user's language variant has not been
 * DIF-validated for this item (§8.5).
 */
@Serializable
data class ConfounderSignals(
    val priorKnowledgeDependency: DependencyLevel = DependencyLevel.LOW,
    val variantValidated: Boolean = true,
    val externalDomainDeclared: Boolean = true,
    val suspiciousSpeedAccuracy: Boolean = false,
)

/** Default [ConfounderEngine]. Pure passthrough + escalation rules. */
class DefaultConfounderEngine : ConfounderEngine {

    override fun assess(signals: ConfounderSignals): ConfounderAssessment {
        val priorRisk = if (signals.suspiciousSpeedAccuracy) {
            DependencyLevel.HIGH
        } else {
            signals.priorKnowledgeDependency
        }
        return ConfounderAssessment(
            priorKnowledgeRisk = priorRisk,
            variantBiasRisk = !signals.variantValidated,
            externalDomainUndeclared = !signals.externalDomainDeclared,
        )
    }
}
