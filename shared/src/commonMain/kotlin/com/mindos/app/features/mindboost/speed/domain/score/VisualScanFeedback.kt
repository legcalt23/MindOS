package com.mindos.app.features.mindboost.speed.domain.score

/** Lente de feedback de E5 · Búsqueda Visual (MC9-C) — "detección visual con precisión y estabilidad". */
enum class VisualScanFeedbackKind {
    CALIBRATING,
    GOOD_VISUAL_CONTROL,
    FAST_LOW_PRECISION,
    HIGH_VISUAL_LOAD_COST,
    UNSTABLE_SEARCH,

    /** MC9-C-R2: alta precisión pero RT lento — detección estable, siguiente objetivo reducir la latencia. */
    STABLE_REDUCE_LATENCY,
    BALANCED,
}

/**
 * Traduce el perfil preliminar 0–1 ya calculado por [SpeedScoreEngine] a una lente de feedback seria para la
 * búsqueda visual. NO calcula un score nuevo ni emite claims, y NO inventa una métrica de carga visual que el
 * motor no mide: cada categoría mapea a una señal REAL del score (macro-categoría descriptiva + techo de
 * precisión/estabilidad). El texto vive en `SpeedStrings` (capa de presentación); aquí solo la clasificación,
 * pura y testeable.
 *
 * Mapeo honesto (orden importa para que cada categoría sea alcanzable):
 * - CALIBRATING: datos insuficientes (macro CALIBRATING).
 * - FAST_LOW_PRECISION: rápido con bajo control (macro FAST_IMPULSIVE) — respuestas rápidas incorrectas.
 * - UNSTABLE_SEARCH: alta variabilidad (macro VARIABLE) — la búsqueda no se estabilizó.
 * - HIGH_VISUAL_LOAD_COST: el techo §9 se aplicó (precisión<0.80 o CV>0.30) sin ser el patrón impulsivo/variable
 *   puro — la carga de esta corrida coincidió con una caída de precisión/estabilidad (lectura por-corrida, NO una
 *   medida validada por nivel de carga).
 * - GOOD_VISUAL_CONTROL: preciso y controlado (macro SLOW_PRECISE) sin penalización.
 * - BALANCED: velocidad, control y estabilidad equilibrados.
 */
object VisualScanFeedback {

    // Umbrales de feedback (MC9-C-R). Señales medidas priorizadas; fallback al patrón macro cuando no hay señal.
    const val CALIBRATION_MIN_TRIALS = 6
    const val FAST_WRONG_MAX = 0.20
    const val HIGH_LOAD_COST_MAX = 0.15
    const val LOW_STABILITY_MAX = 0.60
    const val GOOD_ACCURACY_MIN = 0.85
    const val LOW_FAST_WRONG_MAX = 0.10
    const val SLOW_CORRECT_RT_MS = 700.0

    /** Fallback macro-only (sin telemetría por-trial): mapea la macro-categoría + techo del score. */
    fun forScore(score: CognitiveProfileScore): VisualScanFeedbackKind = when {
        score.macroCategory == SpeedMacroCategory.CALIBRATING -> VisualScanFeedbackKind.CALIBRATING
        score.macroCategory == SpeedMacroCategory.FAST_IMPULSIVE -> VisualScanFeedbackKind.FAST_LOW_PRECISION
        score.macroCategory == SpeedMacroCategory.VARIABLE -> VisualScanFeedbackKind.UNSTABLE_SEARCH
        score.ceilingPenaltyApplied -> VisualScanFeedbackKind.HIGH_VISUAL_LOAD_COST
        score.macroCategory == SpeedMacroCategory.SLOW_PRECISE -> VisualScanFeedbackKind.GOOD_VISUAL_CONTROL
        else -> VisualScanFeedbackKind.BALANCED
    }

    /**
     * Feedback accionable de E5 usando las señales REALES de la corrida cuando existen ([VisualScanMetrics]);
     * si una señal es `Unavailable` cae al patrón macro del score. Prioridad: impulsividad (rápido+incorrecto) →
     * coste de carga visual medido → inestabilidad → buen control → equilibrio. El sesgo lateral NO se expone
     * como feedback al usuario (es quality-diagnostic, no se patologiza).
     */
    fun forRun(metrics: VisualScanMetrics, score: CognitiveProfileScore): VisualScanFeedbackKind {
        if (metrics.totalTrials < CALIBRATION_MIN_TRIALS || score.macroCategory == SpeedMacroCategory.CALIBRATING) {
            return VisualScanFeedbackKind.CALIBRATING
        }
        return impulsive(metrics, score)
            ?: highLoad(metrics, score)
            ?: unstable(metrics, score)
            ?: preciseButSlow(metrics)
            ?: goodControl(metrics, score)
            ?: VisualScanFeedbackKind.BALANCED
    }

    /** MC9-C-R2: alta precisión + RT de aciertos lento ⇒ detección estable, siguiente objetivo reducir latencia. */
    private fun preciseButSlow(metrics: VisualScanMetrics): VisualScanFeedbackKind? {
        val accuracy = metrics.accuracy.measuredOrNull()
        val speed = metrics.correctResponseSpeedMs.measuredOrNull()
        val preciseAndSlow = accuracy != null && accuracy >= GOOD_ACCURACY_MIN &&
            speed != null && speed > SLOW_CORRECT_RT_MS
        return if (preciseAndSlow) VisualScanFeedbackKind.STABLE_REDUCE_LATENCY else null
    }

    private fun impulsive(metrics: VisualScanMetrics, score: CognitiveProfileScore): VisualScanFeedbackKind? {
        val fastWrong = metrics.fastWrongRate.measuredOrNull()
        val fastAndWrong = fastWrong != null && fastWrong >= FAST_WRONG_MAX
        return if (fastAndWrong || score.macroCategory == SpeedMacroCategory.FAST_IMPULSIVE) {
            VisualScanFeedbackKind.FAST_LOW_PRECISION
        } else {
            null
        }
    }

    private fun highLoad(metrics: VisualScanMetrics, score: CognitiveProfileScore): VisualScanFeedbackKind? {
        val loadCost = metrics.highLoadCost.measuredOrNull()
        val measuredHigh = loadCost != null && loadCost >= HIGH_LOAD_COST_MAX
        val fallbackCeiling = loadCost == null && score.ceilingPenaltyApplied
        return if (measuredHigh || fallbackCeiling) VisualScanFeedbackKind.HIGH_VISUAL_LOAD_COST else null
    }

    private fun unstable(metrics: VisualScanMetrics, score: CognitiveProfileScore): VisualScanFeedbackKind? {
        val stability = metrics.stabilityIndex.measuredOrNull()
        val measuredUnstable = stability != null && stability < LOW_STABILITY_MAX
        val fallbackVariable = stability == null && score.macroCategory == SpeedMacroCategory.VARIABLE
        return if (measuredUnstable || fallbackVariable) VisualScanFeedbackKind.UNSTABLE_SEARCH else null
    }

    private fun goodControl(metrics: VisualScanMetrics, score: CognitiveProfileScore): VisualScanFeedbackKind? {
        val accuracy = metrics.accuracy.measuredOrNull()
        val fastWrong = metrics.fastWrongRate.measuredOrNull()
        val lowFastWrong = fastWrong == null || fastWrong <= LOW_FAST_WRONG_MAX
        val preciseAndControlled = accuracy != null && accuracy >= GOOD_ACCURACY_MIN && lowFastWrong
        return if (preciseAndControlled || score.macroCategory == SpeedMacroCategory.SLOW_PRECISE) {
            VisualScanFeedbackKind.GOOD_VISUAL_CONTROL
        } else {
            null
        }
    }
}
