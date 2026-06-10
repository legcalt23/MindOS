package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet

/**
 * An honest composite focus score (0..100) with an explicit uncertainty band. The
 * band is WIDE when little data backs it and narrows as evidence accrues; when the
 * score is not [confident] the UI must show it as provisional, never as a hard fact.
 */
data class FocusScore(
    val value: Double?,
    val lowerBound: Double?,
    val upperBound: Double?,
    val confident: Boolean,
    val caption: String,
)

/** Computes a [FocusScore] from a session's (or aggregate) metrics. */
interface FocusScoreCalculator {
    fun compute(metrics: MetricSet): FocusScore
}

/**
 * Default calculator. Averages the confident, honest 0..100 components available
 * (sustained focus, accuracy, block stability, inhibitory control). The band width
 * is driven by the smallest supporting sample — less data, wider band.
 */
class DefaultFocusScoreCalculator : FocusScoreCalculator {

    override fun compute(metrics: MetricSet): FocusScore {
        val components = collectComponents(metrics)
        if (components.isEmpty()) {
            return FocusScore(
                value = null, lowerBound = null, upperBound = null, confident = false,
                caption = "Datos insuficientes para un Focus Score.",
            )
        }
        val value = components.map { it.value }.average()
        val minN = components.minOf { it.n }
        val maturity = (minN.toDouble() / MATURE_N).coerceIn(0.0, 1.0)
        val margin = MIN_MARGIN + MAX_EXTRA_MARGIN * (1.0 - maturity)
        val confident = components.size >= MIN_COMPONENTS && minN >= MIN_N
        return FocusScore(
            value = value,
            lowerBound = (value - margin).coerceAtLeast(0.0),
            upperBound = (value + margin).coerceAtMost(MAX_SCORE),
            confident = confident,
            caption = caption(confident, margin),
        )
    }

    private data class Component(val value: Double, val n: Int)

    private fun collectComponents(metrics: MetricSet): List<Component> = buildList {
        fun confident(key: MetricKey) = metrics[key]?.takeIf { it.confident }
        confident(MetricKey.SUSTAINED_FOCUS_INDEX)?.let { add(Component(it.value, it.n)) }
        confident(MetricKey.INHIBITORY_CONTROL_INDEX)?.let { add(Component(it.value, it.n)) }
        confident(MetricKey.ACCURACY)?.let { add(Component(it.value * PERCENT, it.n)) }
        confident(MetricKey.BLOCK_STABILITY)?.let { add(Component(it.value * PERCENT, it.n)) }
    }

    private fun caption(confident: Boolean, margin: Double): String = when {
        !confident -> "Estimación preliminar — pocos datos todavía."
        margin <= NARROW_MARGIN -> "Estimación estable."
        else -> "Estimación con margen amplio; mejora con más sesiones."
    }

    private companion object {
        const val MAX_SCORE = 100.0
        const val PERCENT = 100.0
        const val MATURE_N = 60.0
        const val MIN_MARGIN = 3.0
        const val MAX_EXTRA_MARGIN = 17.0
        const val NARROW_MARGIN = 6.0
        const val MIN_COMPONENTS = 2
        const val MIN_N = 20
    }
}
