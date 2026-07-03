package com.mindos.app.features.mindboost.language.domain.gates

import kotlin.math.abs
import kotlinx.serialization.Serializable

/**
 * Fairness / DIF gate (v6 §5.3 thresholds, §8.6 retirement). A first, honest **placeholder**: real
 * DIF estimation needs cohort data and is a backend/longitudinal concern. What this gate does offline
 * is enforce the policy *given* a DIF figure and variant-validation status — escalating to monitor,
 * local review, or retirement, and requiring uncertainty copy when a variant is not validated or the
 * sample is too small to trust.
 */
fun interface FairnessGate {
    fun assess(signals: FairnessSignals): FairnessAssessment
}

/** Inputs for a fairness check. [difLogits] is the item's differential functioning in logits. */
@Serializable
data class FairnessSignals(
    val variantValidated: Boolean,
    val difLogits: Double,
    val sampleSize: Int,
    val userDispute: Boolean = false,
)

/** Escalating fairness actions (v6 §8.6). Ordered: NONE < MONITOR < REVIEW_LOCAL < RETIRE_FOR_GROUP. */
@Serializable
enum class FairnessAction { NONE, MONITOR, REVIEW_LOCAL, RETIRE_FOR_GROUP }

/** Outcome: the DIF figure, whether it is flagged, the action to take, and the UX copy requirement. */
@Serializable
data class FairnessAssessment(
    val difLogits: Double,
    val flagged: Boolean,
    val action: FairnessAction,
    val requiresUncertaintyCopy: Boolean,
)

/**
 * Default [FairnessGate]. Thresholds seeded from §5.3 (DIF allowed without review ≤ 0.25). Picks the
 * strongest action implied by any signal; flags whenever an action above MONITOR is taken or the
 * variant is unvalidated.
 */
class DefaultFairnessGate : FairnessGate {

    override fun assess(signals: FairnessSignals): FairnessAssessment {
        val dif = abs(signals.difLogits)
        val difAction = when {
            dif > DIF_RETIRE -> FairnessAction.RETIRE_FOR_GROUP
            dif > DIF_REVIEW -> FairnessAction.REVIEW_LOCAL
            dif > DIF_PUBLIC -> FairnessAction.MONITOR
            else -> FairnessAction.NONE
        }
        val variantAction = if (signals.variantValidated) FairnessAction.NONE else FairnessAction.MONITOR
        val disputeAction = if (signals.userDispute) FairnessAction.REVIEW_LOCAL else FairnessAction.NONE
        val action = maxOf(difAction, variantAction, disputeAction)

        val smallSample = signals.sampleSize < MIN_TRUSTWORTHY_SAMPLE
        val requiresUncertaintyCopy = !signals.variantValidated || smallSample
        val flagged = action.ordinal >= FairnessAction.REVIEW_LOCAL.ordinal || !signals.variantValidated

        return FairnessAssessment(
            difLogits = signals.difLogits,
            flagged = flagged,
            action = action,
            requiresUncertaintyCopy = requiresUncertaintyCopy,
        )
    }

    companion object {
        const val DIF_PUBLIC = 0.25
        const val DIF_REVIEW = 0.35
        const val DIF_RETIRE = 0.50
        const val MIN_TRUSTWORTHY_SAMPLE = 30
    }
}
