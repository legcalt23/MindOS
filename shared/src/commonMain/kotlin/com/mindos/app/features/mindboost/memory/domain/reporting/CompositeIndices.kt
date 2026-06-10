package com.mindos.app.features.mindboost.memory.domain.reporting

import kotlin.math.abs

/**
 * A composite index value with an honesty flag. [value] is null / [confident] false
 * when the supporting sample is too small — the UI must then show it as provisional
 * (doc §6/§13).
 */
data class IndexValue(
    val value: Double?,
    val confident: Boolean,
    val caption: String,
) {
    companion object {
        fun unavailable(caption: String = "Datos insuficientes aún."): IndexValue =
            IndexValue(value = null, confident = false, caption = caption)
    }
}

/**
 * The composite indices of doc §6. Pure functions; each gates confidence on its
 * sample size so an under-powered index is never presented as a hard fact.
 */
object CompositeIndices {

    /** Learning Velocity: slope of improvement over recent sessions (raw, sign meaningful). */
    fun learningVelocity(slope: Double, n: Int): IndexValue =
        if (n < MIN_TREND_N) IndexValue.unavailable() else confident(slope, n)

    /** Retention Quality: (delayed/immediate) × (1 − normalized forgetting). */
    fun retentionQuality(immediate: Double, delayed: Double, forgettingRate: Double, n: Int): IndexValue {
        if (immediate <= 0.0) return IndexValue.unavailable()
        val ratio = (delayed / immediate).coerceIn(0.0, 1.0)
        val retained = ratio * (1.0 - forgettingRate.coerceIn(0.0, 1.0))
        return if (n < MIN_N) provisional(retained) else confident(retained, n)
    }

    /** Resilience: (1 − interference) × accuracy under pressure. */
    fun resilience(interferenceIndex: Double, accuracyUnderPressure: Double, n: Int): IndexValue {
        val value = (1.0 - interferenceIndex.coerceIn(0.0, 1.0)) * accuracyUnderPressure.coerceIn(0.0, 1.0)
        return if (n < MIN_N) provisional(value) else confident(value, n)
    }

    /** Calibration Quality: confidence-accuracy correlation × (1 − |bias|). */
    fun calibrationQuality(confidenceAccuracyCorrelation: Double, bias: Double, n: Int): IndexValue {
        val value = confidenceAccuracyCorrelation.coerceIn(0.0, 1.0) * (1.0 - abs(bias).coerceIn(0.0, 1.0))
        return if (n < MIN_N) provisional(value) else confident(value, n)
    }

    private fun confident(value: Double, n: Int): IndexValue =
        IndexValue(value, confident = true, caption = "Basado en $n muestras.")

    private fun provisional(value: Double): IndexValue =
        IndexValue(value, confident = false, caption = "Estimación preliminar — pocos datos.")

    private const val MIN_N = 4
    private const val MIN_TREND_N = 3
}
