package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype

/**
 * Normalized behavioural signals an archetype is inferred from. Decoupling the
 * heuristic from raw metrics keeps it pure and exhaustively testable; the
 * [ArchetypeSignalsFactory] builds these from real metrics.
 */
data class ArchetypeSignals(
    val accuracy: Double,
    val rtMedianMs: Double,
    val rtCv: Double,
    val falseAlarmRate: Double,
    val lapseRate: Double,
    /** Accuracy drop under pressure (0 = unaffected, 1 = collapses). */
    val pressureDelta: Double,
    val totalSessions: Int,
)

/**
 * Result of inference: the archetype, a confidence in 0..1 (low early on), and a
 * runner-up — the archetype is probabilistic and revisable, never a fixed label.
 */
data class ArchetypeInferenceResult(
    val archetype: UserArchetype,
    val confidence: Double,
    val secondGuess: UserArchetype?,
)

/** Infers a (revisable) archetype from behavioural signals. */
interface ArchetypeInference {
    fun infer(signals: ArchetypeSignals): ArchetypeInferenceResult
}

/**
 * Default rule-based inference. Each archetype gets a 0..1 score from normalized
 * signals; the highest wins, with confidence = margin × data maturity. Before
 * enough sessions exist the user is a low-confidence BEGINNER.
 * See 08_personalization_plan.md §1.
 */
class DefaultArchetypeInference : ArchetypeInference {

    override fun infer(signals: ArchetypeSignals): ArchetypeInferenceResult {
        if (signals.totalSessions < BEGINNER_SESSIONS) {
            return ArchetypeInferenceResult(UserArchetype.BEGINNER, confidence = EARLY_CONFIDENCE, secondGuess = null)
        }

        val rtFast = rtFastScore(signals.rtMedianMs)
        val faScore = clamp(signals.falseAlarmRate / FA_HIGH_RATE)
        val lapseScore = clamp(signals.lapseRate / LAPSE_HIGH_RATE)
        val rtCvScore = clamp(signals.rtCv / RT_CV_HIGH)
        val acc = signals.accuracy.coerceIn(0.0, 1.0)
        val pd = signals.pressureDelta.coerceIn(0.0, 1.0)

        val scores = mapOf(
            UserArchetype.IMPULSIVE to mean(faScore, faScore, rtFast),
            UserArchetype.FAST_BUT_INACCURATE to mean(1.0 - acc, rtFast) * (1.0 - faScore),
            UserArchetype.SLOW_BUT_ACCURATE to mean(acc, 1.0 - rtFast),
            UserArchetype.LOW_CONCENTRATION to mean(rtCvScore, lapseScore),
            UserArchetype.ADVANCED to mean(acc, 1.0 - rtCvScore, 1.0 - faScore),
            UserArchetype.ELITE to mean(acc, 1.0 - rtCvScore, 1.0 - pd),
        )

        val ranked = scores.entries.sortedByDescending { it.value }
        val top = ranked[0]
        val second = ranked[1]
        val maturity = clamp(signals.totalSessions.toDouble() / MATURE_SESSIONS)
        val confidence = ((top.value - second.value) * maturity).coerceIn(MIN_CONFIDENCE, 1.0)
        return ArchetypeInferenceResult(top.key, confidence, second.key)
    }

    /** 1.0 at [RT_FAST_MS] or faster, 0.0 at [RT_SLOW_MS] or slower. */
    private fun rtFastScore(rtMedianMs: Double): Double =
        clamp(1.0 - (rtMedianMs - RT_FAST_MS) / (RT_SLOW_MS - RT_FAST_MS))

    private fun clamp(v: Double): Double = v.coerceIn(0.0, 1.0)

    private fun mean(vararg values: Double): Double = values.average()

    private companion object {
        const val BEGINNER_SESSIONS = 3
        const val EARLY_CONFIDENCE = 0.2
        const val RT_FAST_MS = 250.0
        const val RT_SLOW_MS = 550.0
        const val FA_HIGH_RATE = 0.15
        const val LAPSE_HIGH_RATE = 0.15
        const val RT_CV_HIGH = 0.40
        const val MATURE_SESSIONS = 8.0
        const val MIN_CONFIDENCE = 0.05
    }
}

/** Builds [ArchetypeSignals] from the most recent confident metrics (or the baseline). */
object ArchetypeSignalsFactory {

    // A flat sequence of metric extractions with baseline fallbacks; clearest inline.
    @Suppress("CyclomaticComplexMethod")
    fun from(baseline: BaselineSnapshot?, recent: List<SessionResult>, totalSessions: Int): ArchetypeSignals {
        val metrics = recent.firstOrNull()?.sessionMetrics ?: emptyMap()
        fun value(key: MetricKey): Double? = metrics[key]?.takeIf { it.confident }?.value
        fun rate(key: MetricKey): Double? = metrics[key]?.takeIf { it.confident }?.let {
            if (it.n > 0) it.value / it.n else null
        }
        fun baselineValue(key: MetricKey): Double? = baseline?.perMetric?.get(key)

        return ArchetypeSignals(
            accuracy = value(MetricKey.ACCURACY) ?: baselineValue(MetricKey.ACCURACY) ?: DEFAULT_ACCURACY,
            rtMedianMs = value(MetricKey.RT_MEDIAN) ?: baselineValue(MetricKey.RT_MEDIAN) ?: DEFAULT_RT_MS,
            rtCv = value(MetricKey.RT_CV) ?: baselineValue(MetricKey.RT_CV) ?: DEFAULT_RT_CV,
            falseAlarmRate = rate(MetricKey.FALSE_ALARMS) ?: 0.0,
            lapseRate = rate(MetricKey.LAPSES) ?: 0.0,
            pressureDelta = value(MetricKey.PRESSURE_DELTA) ?: 0.0,
            totalSessions = totalSessions,
        )
    }

    private const val DEFAULT_ACCURACY = 0.5
    private const val DEFAULT_RT_MS = 400.0
    private const val DEFAULT_RT_CV = 0.25
}
