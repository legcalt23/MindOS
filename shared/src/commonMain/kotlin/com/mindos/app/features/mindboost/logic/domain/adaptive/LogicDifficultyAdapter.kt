package com.mindos.app.features.mindboost.logic.domain.adaptive

import com.mindos.app.features.mindboost.logic.domain.model.FatigueSignal
import com.mindos.app.features.mindboost.logic.domain.model.LogicAttempt
import com.mindos.app.features.mindboost.logic.domain.model.LogicDifficulty
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicSkill
import com.mindos.app.features.mindboost.logic.domain.model.SkillState

enum class AdaptationAction {
    INCREASE,
    HOLD,
    DECREASE,
    MICRO_LESSON,
}

data class DifficultyDecision(
    val nextDifficulty: LogicDifficulty,
    val action: AdaptationAction,
    val reason: String,
)

interface DifficultyAdapter {
    fun decide(current: LogicDifficulty, recentAttempts: List<LogicAttempt>): DifficultyDecision
    fun updateSkillStates(current: List<SkillState>, attempts: List<LogicAttempt>): List<SkillState>
}

class DefaultLogicDifficultyAdapter : DifficultyAdapter {

    override fun decide(current: LogicDifficulty, recentAttempts: List<LogicAttempt>): DifficultyDecision {
        val recent = recentAttempts.takeLast(RECENT_WINDOW)
        if (recent.isEmpty()) return DifficultyDecision(current, AdaptationAction.HOLD, "Sin datos suficientes.")

        val averageIcr = recent.map { it.scoreBreakdown.totalIcr }.average()
        val averageJustification = recent.map { it.scoreBreakdown.justificationScore }.average()
        return when {
            hasRepeatedConceptualError(recent) -> DifficultyDecision(
                current.decrease(),
                AdaptationAction.MICRO_LESSON,
                "Se repite un error conceptual; conviene practicar la regla con más apoyo.",
            )
            averageIcr < LOW_ICR_THRESHOLD -> DifficultyDecision(
                current.decrease(),
                AdaptationAction.DECREASE,
                "El ICR reciente pide reducir carga y consolidar.",
            )
            averageIcr >= HIGH_ICR_THRESHOLD && averageJustification >= JUSTIFICATION_THRESHOLD ->
                highPerformanceDecision(current, recent)
            else -> DifficultyDecision(current, AdaptationAction.HOLD, "La dificultad actual sigue siendo informativa.")
        }
    }

    override fun updateSkillStates(current: List<SkillState>, attempts: List<LogicAttempt>): List<SkillState> {
        val currentBySkill = current.associateBy { it.skillId }
        val touched = attempts.flatMap { it.targetSkills }.toSet()
        val untouched = current.filterNot { it.skillId in touched }
        val updated = touched.map { skill ->
            val old = currentBySkill[skill] ?: SkillState(skill)
            val skillAttempts = attempts.filter { skill in it.targetSkills }
            val averageIcr = skillAttempts.map { it.scoreBreakdown.totalIcr }.average()
            val accuracy = skillAttempts.count { it.isCorrect }.toDouble() / skillAttempts.size
            val allErrors = skillAttempts.flatMap { it.errorTags }
            val errors = allErrors.distinct()
            val impulsiveCount = allErrors.count { it == LogicErrorTag.IMPULSIVE_RESPONSE }
            old.copy(
                thetaEstimate = (old.thetaEstimate + (averageIcr - TARGET_ICR) / THETA_SCALE)
                    .coerceIn(MIN_THETA, MAX_THETA),
                uncertainty = (old.uncertainty * UNCERTAINTY_DECAY).coerceAtLeast(MIN_UNCERTAINTY),
                attemptsCount = old.attemptsCount + skillAttempts.size,
                recentIcr = averageIcr,
                recentAccuracy = accuracy,
                recentConceptualErrors = errors,
                recentFatigueSignal = if (impulsiveCount >= IMPULSIVITY_SIGNAL_COUNT) {
                    FatigueSignal.POSSIBLE_IMPULSIVITY
                } else {
                    null
                },
            )
        }
        return (untouched + updated).sortedBy { it.skillId.ordinal }
    }

    private fun hasRepeatedConceptualError(attempts: List<LogicAttempt>): Boolean =
        attempts.flatMap { it.errorTags }
            .filterNot { it == LogicErrorTag.IMPULSIVE_RESPONSE || it == LogicErrorTag.OVERCONFIDENT_ERROR }
            .groupingBy { it }
            .eachCount()
            .values
            .any { it >= REPEATED_ERROR_COUNT }

    private fun highPerformanceDecision(
        current: LogicDifficulty,
        attempts: List<LogicAttempt>,
    ): DifficultyDecision {
        val probes = attempts.filter { it.isIsomorphicProbe }
        val transferConfirmed = probes.isNotEmpty() &&
            probes.all { it.isCorrect && it.scoreBreakdown.totalIcr >= TRANSFER_ICR_THRESHOLD }
        return if (transferConfirmed) {
            DifficultyDecision(
                current.increase(),
                AdaptationAction.INCREASE,
                "ICR y justificación altos, confirmados en un isomorfo.",
            )
        } else {
            DifficultyDecision(
                current,
                AdaptationAction.HOLD,
                "Buen rendimiento entrenado; la transferencia sigue en calibración.",
            )
        }
    }

    private companion object {
        const val RECENT_WINDOW = 6
        const val REPEATED_ERROR_COUNT = 2
        const val LOW_ICR_THRESHOLD = 55.0
        const val HIGH_ICR_THRESHOLD = 80.0
        const val JUSTIFICATION_THRESHOLD = 0.65
        const val TRANSFER_ICR_THRESHOLD = 70.0
        const val TARGET_ICR = 65.0
        const val THETA_SCALE = 100.0
        const val MIN_THETA = -3.0
        const val MAX_THETA = 3.0
        const val UNCERTAINTY_DECAY = 0.92
        const val MIN_UNCERTAINTY = 0.15
        const val IMPULSIVITY_SIGNAL_COUNT = 2
    }
}
