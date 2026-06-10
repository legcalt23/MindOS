package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile

/** Overtraining assessment over a longer horizon than per-day recovery. */
sealed interface OvertrainingVerdict {
    data object Ok : OvertrainingVerdict
    data class Caution(val reason: String) : OvertrainingVerdict
    data class ForceRest(val reason: String) : OvertrainingVerdict
}

/** Detects chronic overtraining / degradation (08_personalization_plan.md §4). */
interface OvertrainingGuard {
    fun check(profile: UserCognitiveProfile, recent: List<SessionResult>): OvertrainingVerdict
}

/**
 * Default guard. Forces rest on chronic high fatigue, or on rising variability vs
 * baseline together with a downward accuracy trend (the classic "more volume, worse
 * results" overtraining picture). Flags caution on a downward trend alone. Never
 * rewards pushing through fatigue. [recent] is expected newest-first.
 */
class DefaultOvertrainingGuard : OvertrainingGuard {

    override fun check(profile: UserCognitiveProfile, recent: List<SessionResult>): OvertrainingVerdict {
        val sessions = recent.take(WINDOW)
        val fatigues = sessions.mapNotNull { metric(it, MetricKey.FATIGUE_INDEX) }
        val accuracies = sessions.mapNotNull { metric(it, MetricKey.ACCURACY) }

        val chronicFatigue = fatigues.size >= CHRONIC_COUNT && fatigues.take(CHRONIC_COUNT).all { it >= HIGH_FATIGUE }
        val cvDegraded = isRtCvDegraded(profile, sessions)
        val decliningAccuracy = isDecliningNewestFirst(accuracies)

        return when {
            chronicFatigue ->
                OvertrainingVerdict.ForceRest("Fatiga alta crónica en sesiones recientes — descanso forzado.")
            cvDegraded && decliningAccuracy ->
                OvertrainingVerdict.ForceRest(
                    "Variabilidad por encima de tu base con precisión cayendo — descanso forzado.",
                )
            decliningAccuracy ->
                OvertrainingVerdict.Caution("Tu precisión tiende a la baja — reduce carga y vigila el descanso.")
            else -> OvertrainingVerdict.Ok
        }
    }

    private fun isRtCvDegraded(profile: UserCognitiveProfile, sessions: List<SessionResult>): Boolean {
        val baselineCv = profile.thresholds.baselineRtCv ?: return false
        val recentCv = sessions.firstOrNull()?.let { metric(it, MetricKey.RT_CV) }
        return recentCv != null && recentCv > baselineCv * CV_DEGRADE_FACTOR
    }

    /** True when the more-recent half is clearly below the older half (values are newest-first). */
    private fun isDecliningNewestFirst(values: List<Double>): Boolean {
        if (values.size < MIN_TREND) return false
        val half = values.size / 2
        val newer = values.take(half).average()
        val older = values.takeLast(half).average()
        return newer < older - DECLINE_EPSILON
    }

    private fun metric(session: SessionResult, key: MetricKey): Double? =
        session.sessionMetrics[key]?.takeIf { it.confident }?.value

    private companion object {
        const val WINDOW = 6
        const val CHRONIC_COUNT = 3
        const val HIGH_FATIGUE = 0.75
        const val MIN_TREND = 4
        const val DECLINE_EPSILON = 0.05
        const val CV_DEGRADE_FACTOR = 1.5
    }
}
