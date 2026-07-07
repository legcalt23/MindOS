@file:Suppress("MaxLineLength") // Disclaimer sentence reads best whole.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue
import kotlin.math.roundToInt

/**
 * The honest MindBoost BLOCK score (0–100) for Emociones (MC8u-C §2). It restores PRD consistency with the other seven
 * cognitive blocks WITHOUT re-introducing the "loaded gun": it is a normalized reading of the governing TASK metric —
 * pure training performance INSIDE the exercise — never an emotional state, a personality trait, or a health signal.
 *
 * It is NULL (CALIBRATING) whenever the governing metric is not confident, so a low-confidence/gamed/empty session
 * never produces a fabricated number. Direction-normalized (lower-is-better metrics are inverted). RANGE/INFORMATIONAL
 * metrics have no honest 0–100 mapping and return null.
 */
object EmotionBlockScore {
    const val MIN = 0
    const val MAX = 100

    /** Mandatory disclaimer that must accompany any surfaced block score — task performance, not an emotional reading. */
    const val DISCLAIMER = "Este número es tu rendimiento en la tarea de entrenamiento, no una medida de tu estado emocional ni de tu salud."

    /** 0–100 training-performance score, or null (CALIBRATING) when the governing metric is not a confident reading. */
    fun of(metric: EmotionMetricValue?, direction: MetricDirection): Int? {
        val confident = metric?.takeIf { it.confident } ?: return null
        return normalize(confident.value.coerceIn(0.0, 1.0), direction)
            ?.let { (it * MAX).roundToInt().coerceIn(MIN, MAX) }
    }

    private fun normalize(value: Double, direction: MetricDirection): Double? = when (direction) {
        MetricDirection.HIGHER_IS_BETTER -> value
        MetricDirection.LOWER_IS_BETTER -> 1.0 - value
        MetricDirection.RANGE_IS_BETTER, MetricDirection.INFORMATIONAL_ONLY -> null
    }
}
