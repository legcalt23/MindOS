package com.mindos.app.features.mindboost.speed.domain.score

/** Lente de feedback de Number Pulse Compare (MC9-A) — "velocidad con precisión". */
enum class NumberPulseFeedbackKind { CALIBRATING, FAST_LOW_PRECISION, SLOW_PRECISE, VARIABLE, BALANCED }

/**
 * Traduce el perfil preliminar 0–1 ya calculado por [SpeedScoreEngine] a un mensaje de feedback serio para
 * Number Pulse Compare. NO calcula un score nuevo ni emite claims: solo mapea la macro-categoría DESCRIPTIVA
 * de la corrida a una lente accionable de velocidad-con-precisión. El texto vive en `SpeedStrings` (capa de
 * presentación) para no meter copy de UI en el dominio; aquí solo la clasificación, pura y testeable.
 */
object NumberPulseFeedback {
    fun forScore(score: CognitiveProfileScore): NumberPulseFeedbackKind = when (score.macroCategory) {
        SpeedMacroCategory.CALIBRATING -> NumberPulseFeedbackKind.CALIBRATING
        SpeedMacroCategory.FAST_IMPULSIVE -> NumberPulseFeedbackKind.FAST_LOW_PRECISION
        SpeedMacroCategory.SLOW_PRECISE -> NumberPulseFeedbackKind.SLOW_PRECISE
        SpeedMacroCategory.VARIABLE -> NumberPulseFeedbackKind.VARIABLE
        SpeedMacroCategory.BALANCED -> NumberPulseFeedbackKind.BALANCED
    }
}
