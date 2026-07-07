@file:Suppress("MaxLineLength") // This is a copy table: user-facing Spanish sentences are the data and read best whole.

package com.mindos.app.features.mindboost.emotions.domain.feedback

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionCapacity

/** How the confident governing value read against its direction (MC8u P0-1). NEUTRAL = confident but mid-band. */
enum class SignalStrength { STRONG, WEAK, NEUTRAL }

/**
 * The single source of per-capacity, session-scoped feedback copy (MC8u P0-1). Every string is scanner-clean,
 * non-clinical and about "en esta sesión" / "en esta tarea" — never a person-level trait or a life claim. The copy
 * varies by [EmotionCapacity] AND by [SignalStrength], so two exercises (or two performance levels) never share the
 * same reading. Enforced by EmotionFeedbackBuilderTest.
 */
object EmotionFeedbackCopy {

    fun whatTrained(capacity: EmotionCapacity): String = when (capacity) {
        EmotionCapacity.PAUSE_BEFORE_ACTION ->
            "En esta sesión entrenaste retener la respuesta hasta la señal correcta bajo una distracción leve."
        EmotionCapacity.POST_ERROR_RECOVERY ->
            "En esta sesión entrenaste retomar tu ritmo después de un error, sin arrastrarlo al siguiente intento."
        EmotionCapacity.EMOTION_THOUGHT_SEPARATION ->
            "En esta sesión entrenaste distinguir estado, pensamiento, impulso y hecho observable."
        EmotionCapacity.CALIBRATION_ACCURACY ->
            "En esta sesión entrenaste ajustar tu confianza a tu acierto real en la tarea."
        EmotionCapacity.AFFECTIVE_DECISION_QUALITY ->
            "En esta sesión entrenaste elegir con la evidencia cuando una opción resultaba más atractiva."
        EmotionCapacity.FRUSTRATION_TOLERANCE ->
            "En esta sesión entrenaste sostener tu estrategia ante una dificultad creciente."
        EmotionCapacity.REGULATION_STABILITY ->
            "En esta sesión entrenaste mantener un ritmo estable durante la tarea."
        EmotionCapacity.GRANULARITY ->
            "En esta sesión entrenaste nombrar con precisión la etiqueta funcional más ajustada."
    }

    /** The "what your performance showed" line for a CONFIDENT reading. */
    fun performance(capacity: EmotionCapacity, strength: SignalStrength): String = when (strength) {
        SignalStrength.STRONG -> strongSignal(capacity)
        SignalStrength.WEAK -> weakSignal(capacity)
        SignalStrength.NEUTRAL -> neutralSignal(capacity)
    }

    fun strongSignal(capacity: EmotionCapacity): String = when (capacity) {
        EmotionCapacity.PAUSE_BEFORE_ACTION -> "Tu rendimiento en esta tarea retuvo bien las respuestas de más."
        EmotionCapacity.POST_ERROR_RECOVERY -> "Tu rendimiento en esta tarea se recuperó con solidez tras los errores."
        EmotionCapacity.EMOTION_THOUGHT_SEPARATION -> "Tu rendimiento en esta tarea separó las categorías con precisión."
        EmotionCapacity.CALIBRATION_ACCURACY -> "Tu confianza se ajustó bien a tu acierto real en esta tarea."
        EmotionCapacity.AFFECTIVE_DECISION_QUALITY -> "Tus elecciones siguieron la evidencia en esta tarea."
        EmotionCapacity.FRUSTRATION_TOLERANCE -> "Sostuviste tu estrategia ante la dificultad en esta tarea."
        EmotionCapacity.REGULATION_STABILITY -> "Tu ritmo se mantuvo estable en esta tarea."
        EmotionCapacity.GRANULARITY -> "Nombraste las etiquetas con precisión en esta tarea."
    }

    fun weakSignal(capacity: EmotionCapacity): String = when (capacity) {
        EmotionCapacity.PAUSE_BEFORE_ACTION -> "En esta tarea aparecieron varias respuestas de más."
        EmotionCapacity.POST_ERROR_RECOVERY -> "En esta tarea los errores afectaron a los intentos siguientes."
        EmotionCapacity.EMOTION_THOUGHT_SEPARATION -> "En esta tarea costó separar algunas categorías."
        EmotionCapacity.CALIBRATION_ACCURACY -> "En esta tarea tu confianza no siguió a tu acierto real."
        EmotionCapacity.AFFECTIVE_DECISION_QUALITY -> "En esta tarea la opción más atractiva pesó más que la evidencia."
        EmotionCapacity.FRUSTRATION_TOLERANCE -> "En esta tarea la dificultad hizo tambalear tu estrategia."
        EmotionCapacity.REGULATION_STABILITY -> "En esta tarea tu ritmo fue irregular."
        EmotionCapacity.GRANULARITY -> "En esta tarea las etiquetas fueron poco precisas."
    }

    private fun neutralSignal(capacity: EmotionCapacity): String =
        "Tu rendimiento en esta tarea quedó en un punto intermedio de ${focusOf(capacity)}."

    /** The next-practice cue, direction- and strength-aware. */
    @Suppress("CyclomaticComplexMethod") // Copy table: capacity × strength cue selection; each branch is a literal.
    fun practice(capacity: EmotionCapacity, strength: SignalStrength): String = when (capacity) {
        EmotionCapacity.PAUSE_BEFORE_ACTION -> when (strength) {
            SignalStrength.STRONG -> "Mantén ese control y prueba una sesión con un poco más de exigencia."
            SignalStrength.WEAK -> "La próxima vez, espera un instante antes de responder y confirma la señal."
            SignalStrength.NEUTRAL -> "Sigue practicando la pausa breve antes de responder."
        }
        EmotionCapacity.POST_ERROR_RECOVERY -> when (strength) {
            SignalStrength.STRONG -> "Buen ritmo de recuperación; prueba una sesión con más intentos difíciles."
            SignalStrength.WEAK -> "Tras un error, haz una pausa breve y retoma el siguiente intento con calma."
            SignalStrength.NEUTRAL -> "Sigue practicando retomar el ritmo después de un error."
        }
        EmotionCapacity.EMOTION_THOUGHT_SEPARATION -> when (strength) {
            SignalStrength.STRONG -> "Buena precisión; prueba con frases más ambiguas para afinar."
            SignalStrength.WEAK -> "Antes de elegir, pregúntate si eso es un hecho observable o una interpretación."
            SignalStrength.NEUTRAL -> "Sigue practicando separar estado, pensamiento, impulso y hecho."
        }
        else -> when (strength) {
            SignalStrength.STRONG -> "Mantén el enfoque en ${focusOf(capacity)} y sube un poco la exigencia."
            SignalStrength.WEAK -> "La próxima vez, céntrate en ${focusOf(capacity)} con más calma."
            SignalStrength.NEUTRAL -> "Sigue practicando ${focusOf(capacity)}."
        }
    }

    /** Short focus phrase for a capacity — reused by the recommendation engine so cues stay consistent. */
    internal fun focusOf(capacity: EmotionCapacity): String = when (capacity) {
        EmotionCapacity.PAUSE_BEFORE_ACTION -> "la pausa antes de responder"
        EmotionCapacity.POST_ERROR_RECOVERY -> "la recuperación tras un error"
        EmotionCapacity.EMOTION_THOUGHT_SEPARATION -> "la separación de categorías"
        EmotionCapacity.CALIBRATION_ACCURACY -> "el ajuste de tu confianza"
        EmotionCapacity.AFFECTIVE_DECISION_QUALITY -> "elegir con la evidencia"
        EmotionCapacity.FRUSTRATION_TOLERANCE -> "sostener la estrategia"
        EmotionCapacity.REGULATION_STABILITY -> "un ritmo estable"
        EmotionCapacity.GRANULARITY -> "una etiqueta precisa"
    }

    /** Shown when the governing metric is NOT confident (insufficient sample) — never a performance judgment. */
    const val INSUFFICIENT_DATA_PERFORMANCE =
        "Esta sesión todavía no tiene muestra suficiente para una lectura de rendimiento estable. " +
            "Cuenta como práctica, no como medida."

    const val WHAT_NOT_TO_CONCLUDE =
        "Esto describe tu rendimiento en el ejercicio, no una conclusión sobre ti ni sobre tu vida emocional real."

    const val SAFETY_NOTE =
        "MindBoost cerró esta sesión para cuidar la calidad del entrenamiento. No es una evaluación clínica ni una " +
            "señal personal. No hay nada que corregir hoy."
}
