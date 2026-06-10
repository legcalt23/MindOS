package com.mindos.app.features.mindboost.memory.domain.session

/** Whether the user should train now, and at what intensity (doc §7 recovery). */
sealed interface MemoryRecoveryGate {
    data object FullGo : MemoryRecoveryGate
    data class Reduced(val volumeMultiplier: Double, val reason: String) : MemoryRecoveryGate
    data class RestRecommended(val reason: String) : MemoryRecoveryGate
}

/**
 * Inputs for the recovery decision. Time is injected (hours computed by the caller
 * from an EpochClock); this layer never reads the clock.
 *
 * @param hoursSinceLastSameModuleSession null if no prior same-module session.
 * @param consecutiveTrainingDays days trained in a row, including today.
 * @param recentSessionPerformance recent session performance scores, newest-first.
 */
data class MemoryRecoveryInput(
    val hoursSinceLastSameModuleSession: Double?,
    val consecutiveTrainingDays: Int,
    val recentSessionPerformance: List<Double>,
)

/** Decides recovery state. Anti-overtraining: safety outranks progression (doc §7). */
interface MemoryRecoveryPolicy {
    fun gate(input: MemoryRecoveryInput): MemoryRecoveryGate

    /** Intra-session: true if performance dropped >25% from the session start (doc §7.1). */
    fun shouldRestIntraSession(startPerformance: Double, currentPerformance: Double): Boolean
}

/** Default recovery policy. */
class DefaultMemoryRecoveryPolicy : MemoryRecoveryPolicy {

    override fun gate(input: MemoryRecoveryInput): MemoryRecoveryGate = when {
        input.hoursSinceLastSameModuleSession != null &&
            input.hoursSinceLastSameModuleSession < MIN_HOURS_SAME_MODULE ->
            MemoryRecoveryGate.RestRecommended(
                "Mínimo ${MIN_HOURS_SAME_MODULE.toInt()}h entre sesiones del mismo módulo.",
            )

        input.consecutiveTrainingDays >= MAX_CONSECUTIVE_DAYS ->
            MemoryRecoveryGate.RestRecommended("Día de descanso semanal: llevas $MAX_CONSECUTIVE_DAYS días seguidos.")

        isDeclining(input.recentSessionPerformance) ->
            MemoryRecoveryGate.Reduced(REDUCED_MULTIPLIER, "Rendimiento a la baja — reducir volumen.")

        else -> MemoryRecoveryGate.FullGo
    }

    override fun shouldRestIntraSession(startPerformance: Double, currentPerformance: Double): Boolean {
        if (startPerformance <= 0.0) return false
        return currentPerformance < startPerformance * (1.0 - INTRA_SESSION_DROP)
    }

    /** Declining if newest is meaningfully below the oldest in the window. */
    private fun isDeclining(performanceNewestFirst: List<Double>): Boolean {
        if (performanceNewestFirst.size < MIN_TREND_SESSIONS) return false
        val newest = performanceNewestFirst.first()
        val oldest = performanceNewestFirst.last()
        return newest < oldest - DECLINE_THRESHOLD
    }

    private companion object {
        const val MIN_HOURS_SAME_MODULE = 4.0
        const val MAX_CONSECUTIVE_DAYS = 6
        const val MIN_TREND_SESSIONS = 3
        const val DECLINE_THRESHOLD = 0.15
        const val REDUCED_MULTIPLIER = 0.7
        const val INTRA_SESSION_DROP = 0.25
    }
}
