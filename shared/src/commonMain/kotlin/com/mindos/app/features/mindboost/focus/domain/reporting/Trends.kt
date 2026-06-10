package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.RtStatistics
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import kotlin.math.abs

/** Direction of a metric trend (interpreted in the metric's own "better" direction). */
enum class TrendDirection { IMPROVING, STABLE, DECLINING, INSUFFICIENT }

/** Whether a metric is better when higher or lower. */
enum class BetterWhen { HIGHER, LOWER }

/** Maps each metric to the direction that counts as improvement. */
object MetricDirection {
    private val higherIsBetter = setOf(
        MetricKey.ACCURACY,
        MetricKey.POST_ERROR_ACCURACY,
        MetricKey.BLOCK_STABILITY,
        MetricKey.SUSTAINED_FOCUS_INDEX,
        MetricKey.INHIBITORY_CONTROL_INDEX,
        MetricKey.NEAR_TRANSFER_INDEX,
        MetricKey.RATE_CORRECT_SCORE,
        MetricKey.DISTRACTION_RESISTANCE_INDEX,
    )

    fun betterWhen(metric: MetricKey): BetterWhen =
        if (metric in higherIsBetter) BetterWhen.HIGHER else BetterWhen.LOWER
}

/** A metric's trend over recent sessions. */
data class MetricTrend(
    val metric: MetricKey,
    val direction: TrendDirection,
    val slope: Double,
    val confident: Boolean,
    val caption: String,
)

/** Analyzes how a metric is trending across sessions. */
interface TrendAnalyzer {
    fun analyze(metric: MetricKey, sessions: List<SessionResult>): MetricTrend
}

/**
 * Default analyzer. Fits a slope over the metric's confident values across sessions
 * (oldest → newest) and interprets it in the metric's own "better" direction. A
 * near-flat slope is reported as STABLE; too few points as INSUFFICIENT.
 * [sessions] is expected newest-first.
 */
class DefaultTrendAnalyzer : TrendAnalyzer {

    override fun analyze(metric: MetricKey, sessions: List<SessionResult>): MetricTrend {
        val series = sessions.asReversed()
            .mapNotNull { it.sessionMetrics[metric]?.takeIf { v -> v.confident }?.value }
        if (series.size < MIN_POINTS) {
            return MetricTrend(
                metric, TrendDirection.INSUFFICIENT, slope = 0.0, confident = false, caption = "Aún sin tendencia.",
            )
        }
        val xs = series.indices.map { it.toDouble() }
        val slope = RtStatistics.linearSlope(xs, series)
        val meanMagnitude = abs(series.average()) + EPSILON
        val direction = when {
            abs(slope) < STABLE_FRACTION * meanMagnitude -> TrendDirection.STABLE
            (slope > 0) == (MetricDirection.betterWhen(metric) == BetterWhen.HIGHER) -> TrendDirection.IMPROVING
            else -> TrendDirection.DECLINING
        }
        return MetricTrend(metric, direction, slope, confident = true, caption = caption(direction))
    }

    private fun caption(direction: TrendDirection): String = when (direction) {
        TrendDirection.IMPROVING -> "Mejorando."
        TrendDirection.STABLE -> "Estable."
        TrendDirection.DECLINING -> "A la baja — revisa descanso y estado."
        TrendDirection.INSUFFICIENT -> "Aún sin tendencia."
    }

    private companion object {
        const val MIN_POINTS = 3
        const val STABLE_FRACTION = 0.02
        const val EPSILON = 1e-6
    }
}
