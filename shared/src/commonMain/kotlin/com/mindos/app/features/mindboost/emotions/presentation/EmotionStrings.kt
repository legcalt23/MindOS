package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionLabel

/**
 * ALL user-visible copy for the emotions module, centralized as Kotlin constants (i18n-ready; no
 * resource/asset-loaded visible text in v1). Every string here is functional, non-clinical, and
 * scanner-clean — the terminology gate scans this file. Sober coach tone; no casino, no over-promising.
 */
object EmotionStrings {

    const val MODULE_TITLE = "Emociones"
    const val MODULE_SUBTITLE = "Entrenamiento funcional de regulación aplicado al rendimiento."
    const val PROFILE_VISIBLE_NAME = "Perfil funcional de regulación en entrenamiento"

    /** The five approved disclaimers (decision I; #5 rewritten to avoid forbidden lexemes). */
    val DISCLAIMERS: List<String> = listOf(
        "Esto es entrenamiento funcional, no evaluación clínica.",
        "Puedes pausar o salir en cualquier momento.",
        "Una pausa estratégica también cuenta como regulación.",
        "Los resultados describen tu rendimiento dentro del entrenamiento, no tu salud mental.",
        "No usamos estos datos con fines clínicos ni para identificar condiciones de salud.",
    )

    // Always-visible safety controls.
    const val SAFETY_PAUSE = "Pausar"
    const val SAFETY_REDUCE = "Reducir intensidad"
    const val SAFETY_EXIT = "Salir"
    const val REDUCE_PRESSURE_PREF = "Reducir presión temporal"

    // Proxy-armed mid-session check-in (observes load, never names an emotion).
    const val OVERLOAD_CHECK_IN = "Detectamos una señal de carga alta en el ejercicio. " +
        "¿Quieres ajustar la intensidad?"
    const val CHECK_IN_INTENSITY_PROMPT = "Del 1 al 5, ¿cuánta intensidad notas ahora?"

    // Micro-pause overlay (neutral, low-motion).
    const val MICRO_PAUSE = "Pausa breve. Mira fuera de la pantalla, baja el ritmo y decide si " +
        "quieres continuar o salir."
    const val CONTINUE = "Continuar"

    // Safe close after a stop directive.
    const val DESREGULATION_CLOSE = "Has terminado el ejercicio por hoy. No pasa nada por parar."
    const val SESSION_SHORTENED = "Acortamos la sesión para mantenerla cómoda."

    // Honest terminal when the session could not be persisted at all (MC8h). Never a false "saved".
    const val SAVE_FAILED_TITLE = "No pudimos guardar esta sesión"
    const val SAVE_FAILED_BODY = "Ha ocurrido un problema al guardar tu progreso, así que esta práctica no ha " +
        "quedado registrada. Puedes volver a intentarlo cuando quieras."
    const val SAVE_FAILED_ACTION = "Volver"

    // EM3 fixed category labels.
    const val CAT_INTERNAL_STATE = "Estado interno"
    const val CAT_THOUGHT = "Pensamiento"
    const val CAT_IMPULSE = "Impulso"
    const val CAT_FACT = "Hecho observable"

    // EM2 / EM1 immediate, neutral feedback.
    const val NEUTRAL_NEGATIVE = "No esa vez."
    const val PAUSE_CORRECT = "Pausa correcta"
    const val IMPULSIVE_RESPONSE = "Respuesta de más"

    // Self-report scale.
    const val SELF_REPORT_TITLE = "¿Cómo llegas a esta sesión?"
    const val VALENCE = "Tono (negativo a positivo)"
    const val AROUSAL = "Activación (baja a alta)"
    const val INTENSITY = "Intensidad"
    const val LABEL_OPTIONAL = "Etiqueta (opcional)"

    /** Display name for a category (EM3 button label). */
    fun categoryLabel(category: EmotionCategory): String = when (category) {
        EmotionCategory.INTERNAL_STATE -> CAT_INTERNAL_STATE
        EmotionCategory.THOUGHT -> CAT_THOUGHT
        EmotionCategory.IMPULSE -> CAT_IMPULSE
        EmotionCategory.FACT -> CAT_FACT
    }

    /** Display name for an optional self-report label. Functional, never clinical. */
    @Suppress("CyclomaticComplexMethod") // Exhaustive label→display mapping; the `when` IS the table.
    fun emotionLabel(label: EmotionLabel): String = when (label) {
        EmotionLabel.CALMA -> "Calma"
        EmotionLabel.INTERES -> "Interés"
        EmotionLabel.TENSION -> "Tensión"
        EmotionLabel.FRUSTRACION -> "Frustración"
        EmotionLabel.IRRITACION -> "Irritación"
        EmotionLabel.IMPACIENCIA -> "Impaciencia"
        EmotionLabel.DECEPCION -> "Decepción"
        EmotionLabel.PREOCUPACION -> "Preocupación"
        EmotionLabel.INSEGURIDAD -> "Inseguridad"
        EmotionLabel.BLOQUEO -> "Bloqueo"
        EmotionLabel.CANSANCIO -> "Cansancio"
        EmotionLabel.CONFIANZA -> "Confianza"
        EmotionLabel.SATISFACCION -> "Satisfacción"
        EmotionLabel.NEUTRALIDAD -> "Neutralidad"
    }
}
