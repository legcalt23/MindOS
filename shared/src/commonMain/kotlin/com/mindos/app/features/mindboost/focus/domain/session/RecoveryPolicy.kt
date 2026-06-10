package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile

/** Whether the user should train today, and at what intensity. */
sealed interface RecoveryGate {
    data object FullGo : RecoveryGate
    data class Reduced(val volumeMultiplier: Double, val reason: String) : RecoveryGate
    data class RestRecommended(val reason: String) : RecoveryGate
}

/**
 * Decides recovery state from recent training. Anti-overtraining: safety outranks
 * progression (07_ §3, 08_ §4). [recent] is expected newest-first.
 */
interface RecoveryPolicy {
    fun gate(profile: UserCognitiveProfile, recent: List<SessionResult>): RecoveryGate
}

/**
 * Default policy. Two signals: the intra-session fatigue index, and RT-CV
 * degradation relative to the user's calibrated baseline (variability that does
 * not recover between sessions). Both pointing up forces rest.
 */
class DefaultRecoveryPolicy : RecoveryPolicy {

    override fun gate(profile: UserCognitiveProfile, recent: List<SessionResult>): RecoveryGate {
        val sessions = recent.take(LOOKBACK)
        val fatigues = sessions.mapNotNull { confidentMetric(it, MetricKey.FATIGUE_INDEX) }
        val sustainedHighFatigue = fatigues.size >= SUSTAINED_COUNT &&
            fatigues.take(SUSTAINED_COUNT).all { it >= HIGH_FATIGUE }
        val recentModerateFatigue = fatigues.firstOrNull()?.let { it >= MODERATE_FATIGUE } == true
        val cvDegraded = isRtCvDegraded(profile, sessions)

        return when {
            sustainedHighFatigue ->
                RecoveryGate.RestRecommended("Fatiga alta sostenida — descanso recomendado.")
            cvDegraded && recentModerateFatigue ->
                RecoveryGate.RestRecommended("Variabilidad por encima de tu base con fatiga — descanso.")
            recentModerateFatigue || cvDegraded ->
                RecoveryGate.Reduced(REDUCED_MULTIPLIER, "Señales de fatiga reciente — menos volumen.")
            else -> RecoveryGate.FullGo
        }
    }

    private fun isRtCvDegraded(profile: UserCognitiveProfile, sessions: List<SessionResult>): Boolean {
        val baselineCv = profile.thresholds.baselineRtCv ?: return false
        val recentCv = sessions.firstOrNull()?.let { confidentMetric(it, MetricKey.RT_CV) }
        return recentCv != null && recentCv > baselineCv * CV_DEGRADE_FACTOR
    }

    private fun confidentMetric(session: SessionResult, key: MetricKey): Double? =
        session.sessionMetrics[key]?.takeIf { it.confident }?.value

    private companion object {
        const val LOOKBACK = 3
        const val SUSTAINED_COUNT = 2
        const val HIGH_FATIGUE = 0.75
        const val MODERATE_FATIGUE = 0.55
        const val REDUCED_MULTIPLIER = 0.7
        const val CV_DEGRADE_FACTOR = 1.5
    }
}
