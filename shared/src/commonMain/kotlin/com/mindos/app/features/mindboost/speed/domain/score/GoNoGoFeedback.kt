package com.mindos.app.features.mindboost.speed.domain.score

/**
 * Lente de feedback de E8 · Go/No-Go Velocity — "inhibición con velocidad y precisión". Clasifica la corrida en un
 * caso accionable a partir de las señales REALES del mapper + el índice honesto; el texto vive en `SpeedStrings`.
 * Puro y testeable. NO emite claims clínicos ni etiquetas personales fuertes; el sesgo/impulsividad se comunica
 * como CONDUCTA de la corrida ("respondiste antes de confirmar"), nunca como rasgo del usuario.
 */
enum class GoNoGoFeedbackKind {
    /** Muestra insuficiente / baja confianza para un patrón. */
    CALIBRATING,

    /** Muchas comisiones en NO-GO: prioriza inhibir antes de acelerar (NUNCA feedback positivo por velocidad). */
    HIGH_COMMISSION,

    /** Muchas omisiones en GO: faltan respuestas a señales GO (no es "lentitud" genérica). */
    HIGH_OMISSION,

    /** Muchas respuestas prematuras: responder antes de confirmar el estímulo. */
    HIGH_ANTICIPATION,

    /** Rendimiento inestable (alta variabilidad del TR). */
    UNSTABLE,

    /** Buen control inhibitorio Y buena velocidad. */
    STRONG_CONTROL_GOOD_SPEED,

    /** Buen control inhibitorio pero TR lento: reducir latencia manteniendo cero comisiones. */
    STRONG_CONTROL_REDUCE_LATENCY,

    /** Fallback honesto: equilibrado / preliminar sin una señal dominante. */
    BALANCED,
}

object GoNoGoFeedback {

    const val MIN_TRIALS = 8

    /** TR de acierto GO (ms) por encima del cual "buen control" se acompaña de "reduce la latencia". */
    const val SLOW_GO_RT_MS = 550.0

    /**
     * Feedback accionable de E8. Prioridad (importa para que cada caso sea alcanzable y honesto): comisión alta →
     * omisión alta → anticipación alta → inestabilidad → buen control (rápido vs lento) → equilibrio/preliminar.
     * La comisión alta se evalúa PRIMERO: la velocidad nunca produce feedback positivo si falla la inhibición.
     */
    fun forRun(metrics: GoNoGoMetrics, score: GoNoGoScore): GoNoGoFeedbackKind {
        // Data-quality gate FIRST: unreliable measurement (impossible RT) never yields positive feedback.
        val notScoreable = score.dataQualityCompromised || score.index == null ||
            score.category == GoNoGoCategory.CALIBRATING || metrics.totalTrials < MIN_TRIALS
        if (notScoreable) {
            return GoNoGoFeedbackKind.CALIBRATING
        }
        return highCommission(metrics)
            ?: highOmission(metrics)
            ?: highAnticipation(metrics)
            ?: unstable(metrics)
            ?: strongControl(metrics)
            ?: fallback(score)
    }

    private fun highCommission(metrics: GoNoGoMetrics): GoNoGoFeedbackKind? {
        val commission = metrics.commissionRate.measuredOrNull()
        return if (commission != null && commission >= GoNoGoScorer.HIGH_COMMISSION) {
            GoNoGoFeedbackKind.HIGH_COMMISSION
        } else {
            null
        }
    }

    private fun highOmission(metrics: GoNoGoMetrics): GoNoGoFeedbackKind? {
        val omission = metrics.omissionRate.measuredOrNull()
        return if (omission != null && omission >= GoNoGoScorer.HIGH_OMISSION) {
            GoNoGoFeedbackKind.HIGH_OMISSION
        } else {
            null
        }
    }

    private fun highAnticipation(metrics: GoNoGoMetrics): GoNoGoFeedbackKind? {
        val anticipation = metrics.anticipationRate.measuredOrNull()
        return if (anticipation != null && anticipation >= GoNoGoScorer.HIGH_ANTICIPATION) {
            GoNoGoFeedbackKind.HIGH_ANTICIPATION
        } else {
            null
        }
    }

    private fun unstable(metrics: GoNoGoMetrics): GoNoGoFeedbackKind? {
        val stability = metrics.stabilityIndex.measuredOrNull()
        return if (stability != null && stability < GoNoGoScorer.LOW_STABILITY) GoNoGoFeedbackKind.UNSTABLE else null
    }

    private fun strongControl(metrics: GoNoGoMetrics): GoNoGoFeedbackKind? {
        val inhibitory = metrics.inhibitoryControlIndex.measuredOrNull()
        val commission = metrics.commissionRate.measuredOrNull()
        val isStrong = inhibitory != null && commission != null &&
            inhibitory >= GoNoGoScorer.STRONG_CONTROL_MIN && commission <= GoNoGoScorer.LOW_COMMISSION
        if (!isStrong) return null
        val speed = metrics.correctGoSpeedMs.measuredOrNull()
        return if (speed != null && speed > SLOW_GO_RT_MS) {
            GoNoGoFeedbackKind.STRONG_CONTROL_REDUCE_LATENCY
        } else {
            GoNoGoFeedbackKind.STRONG_CONTROL_GOOD_SPEED
        }
    }

    private fun fallback(score: GoNoGoScore): GoNoGoFeedbackKind =
        if (score.lowConfidence) GoNoGoFeedbackKind.CALIBRATING else GoNoGoFeedbackKind.BALANCED
}
