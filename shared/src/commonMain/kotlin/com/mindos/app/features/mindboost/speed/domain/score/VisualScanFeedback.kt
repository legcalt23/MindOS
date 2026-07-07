package com.mindos.app.features.mindboost.speed.domain.score

/** Lente de feedback de E5 · Búsqueda Visual (MC9-C) — "detección visual con precisión y estabilidad". */
enum class VisualScanFeedbackKind {
    CALIBRATING,
    GOOD_VISUAL_CONTROL,
    FAST_LOW_PRECISION,
    HIGH_VISUAL_LOAD_COST,
    UNSTABLE_SEARCH,
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
    fun forScore(score: CognitiveProfileScore): VisualScanFeedbackKind = when {
        score.macroCategory == SpeedMacroCategory.CALIBRATING -> VisualScanFeedbackKind.CALIBRATING
        score.macroCategory == SpeedMacroCategory.FAST_IMPULSIVE -> VisualScanFeedbackKind.FAST_LOW_PRECISION
        score.macroCategory == SpeedMacroCategory.VARIABLE -> VisualScanFeedbackKind.UNSTABLE_SEARCH
        score.ceilingPenaltyApplied -> VisualScanFeedbackKind.HIGH_VISUAL_LOAD_COST
        score.macroCategory == SpeedMacroCategory.SLOW_PRECISE -> VisualScanFeedbackKind.GOOD_VISUAL_CONTROL
        else -> VisualScanFeedbackKind.BALANCED
    }
}
