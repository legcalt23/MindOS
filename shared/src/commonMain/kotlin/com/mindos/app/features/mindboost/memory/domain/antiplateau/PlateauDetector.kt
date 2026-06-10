package com.mindos.app.features.mindboost.memory.domain.antiplateau

import kotlin.math.abs

/** The 9 anti-plateau intervention protocols (doc §10). */
enum class InterventionProtocol {
    STIMULUS_ROTATION,
    RULE_ROTATION,
    MODALITY_CHANGE,
    ENCODING_STRATEGY_CHANGE,
    RETRIEVAL_TYPE_CHANGE,
    DELAY_MANIPULATION,
    INTERFERENCE_INCREASE,
    REST_REPROGRAMMING,
    GOAL_REPLANNING,
}

/** Outcome of plateau detection (doc §10). */
sealed interface PlateauStatus {
    /** Healthy progression — no intervention. */
    data object None : PlateauStatus

    /** Genuine plateau: stable performance the system cannot push without frustration. */
    data object Plateau : PlateauStatus

    /** Gains only on trained material, not on untrained probes — familiarity, not capacity. */
    data object FalseImprovement : PlateauStatus
}

/**
 * Signals for plateau / false-improvement detection (doc §10), measured over a
 * trailing window. Changes are post − pre over the window (negative = decline).
 */
data class PlateauSignals(
    val daysObserved: Int,
    val accuracyChange: Double,
    val speedImprovement: Double,
    val delayedRecallChange: Double,
    val difficultyChanged: Boolean,
    val trainedImprovement: Double,
    val untrainedImprovement: Double,
    val weeksWithoutImprovement: Int,
)

/** Detects plateau and false improvement (doc §10). Pure and deterministic. */
class PlateauDetector {

    fun detect(s: PlateauSignals): PlateauStatus = when {
        s.trainedImprovement > FALSE_TRAINED_MIN && s.untrainedImprovement < FALSE_UNTRAINED_MAX ->
            PlateauStatus.FalseImprovement

        s.daysObserved >= PLATEAU_DAYS &&
            abs(s.accuracyChange) < ACCURACY_STABLE &&
            s.speedImprovement <= 0.0 &&
            s.delayedRecallChange <= 0.0 &&
            !s.difficultyChanged ->
            PlateauStatus.Plateau

        else -> PlateauStatus.None
    }

    companion object {
        const val PLATEAU_DAYS = 7
        const val ACCURACY_STABLE = 0.03
        const val FALSE_TRAINED_MIN = 0.15
        const val FALSE_UNTRAINED_MAX = 0.05
    }
}

/** A recommended intervention plus whether the golden rule has triggered (doc §10). */
data class InterventionPlan(
    val protocol: InterventionProtocol?,
    val goldenRuleTriggered: Boolean,
    val rationale: String,
)

/**
 * Picks an intervention for a detected status (doc §10). The golden rule (2+ weeks
 * without improvement) overrides with [InterventionProtocol.GOAL_REPLANNING]:
 * reduce frequency, redirect time, emphasize transfer.
 */
class InterventionPlanner {

    fun plan(status: PlateauStatus, signals: PlateauSignals): InterventionPlan {
        if (signals.weeksWithoutImprovement >= GOLDEN_RULE_WEEKS) {
            return InterventionPlan(
                protocol = InterventionProtocol.GOAL_REPLANNING,
                goldenRuleTriggered = true,
                rationale = "Regla de oro: 2+ semanas sin mejora — redirigir tiempo y priorizar transferencia.",
            )
        }
        val protocol = when (status) {
            is PlateauStatus.FalseImprovement -> InterventionProtocol.STIMULUS_ROTATION
            is PlateauStatus.Plateau ->
                if (signals.delayedRecallChange < 0.0) {
                    InterventionProtocol.DELAY_MANIPULATION
                } else {
                    InterventionProtocol.ENCODING_STRATEGY_CHANGE
                }
            is PlateauStatus.None -> null
        }
        return InterventionPlan(protocol, goldenRuleTriggered = false, rationale = rationaleFor(status))
    }

    private fun rationaleFor(status: PlateauStatus): String = when (status) {
        is PlateauStatus.FalseImprovement -> "Mejora solo con material familiar — rotar estímulos."
        is PlateauStatus.Plateau -> "Meseta sostenida — cambiar una palanca (demora o estrategia)."
        is PlateauStatus.None -> "Progresión saludable — sin intervención."
    }

    private companion object {
        const val GOLDEN_RULE_WEEKS = 2
    }
}
