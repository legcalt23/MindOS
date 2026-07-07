package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionLabel
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier

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

    // Result screen (MC8u P0-4: every user-visible result string lives here so the copy/terminology scanners cover it).
    const val RESULT_WHAT_TRAINED = "Qué entrenaste"
    const val RESULT_PERFORMANCE = "Qué mostró tu rendimiento"
    const val RESULT_PRACTICE = "Qué practicar ahora"
    const val RESULT_LIMITS = "Qué no mide este ejercicio"
    const val RESULT_NEXT = "Siguiente recomendado"
    const val RESULT_BACK = "Volver"
    const val RESULT_TRANSFER_PREFIX = "Transferencia: "
    const val RESULT_SAFETY_TITLE = "Sesión cerrada para cuidar la calidad"

    // MC8u-B §3/§5 — visible, non-punitive safety & confidence copy on the result screen.
    const val RESULT_SESSION_NOT_COUNTED =
        "Esta sesión queda registrada como práctica, pero no cuenta para calibración porque no alcanzó las " +
            "condiciones mínimas de fiabilidad."
    const val RESULT_INTENSITY_REDUCED =
        "Ajustamos la intensidad a la baja durante la sesión para mantenerla cómoda. No es una señal personal."
    // §5: LOW_CONFIDENCE is about THIS session's sample; CALIBRATING is about the 5-valid-sessions progress. Distinct.
    const val RESULT_LOW_CONFIDENCE =
        "La muestra de esta sesión todavía es limitada. Usa este resultado como orientación de práctica, no como " +
            "lectura estable de rendimiento."
    const val RESULT_CALIBRATING =
        "Aún estás calibrando: se necesitan varias sesiones válidas para una lectura oficial. " +
            "No refleja un rendimiento bajo."

    /** The number of PRD-valid sessions needed for the official range (mirrors EmotionAggregateFactory). */
    const val VALID_SESSION_TARGET = 5

    /** Progress toward the valid sessions the official range needs (never shows a range before the target). */
    fun validSessionsProgress(valid: Int): String =
        "Sesiones válidas para calibración: ${valid.coerceIn(0, VALID_SESSION_TARGET)}/$VALID_SESSION_TARGET"

    // History card (MC8u-B §4).
    const val HISTORY_TITLE = "Tu progreso en Emociones"
    const val HISTORY_EMPTY = "Aún no has registrado ninguna sesión. Empieza cuando quieras."
    const val HISTORY_LAST_SESSION = "Última sesión registrada"
    const val HISTORY_VALID_LABEL = "Sesión válida para calibración"
    const val HISTORY_PRACTICE_LABEL = "Sesión de práctica (no calibra)"
    const val HISTORY_STATE_PREFIX = "Estado: "
    const val HISTORY_NEXT_PREFIX = "Siguiente práctica sugerida: "
    const val HISTORY_NOT_CLINICAL = "No es una evaluación clínica."

    // MC8u-D §6 — calibration-progress card (block screen / dashboard). Non-clinical; every state is a LABEL, never
    // color-only, so the surface is legible without relying on hue.
    const val PROGRESS_TITLE = "Progreso de calibración"
    const val PROGRESS_PROVISIONAL = "Perfil provisional"
    const val PROGRESS_OFFICIAL_UNLOCKED = "Rango oficial desbloqueado"
    const val PROGRESS_SESSION_NOT_COUNTED = "Sesión no contada"
    const val PROGRESS_LOW_CONFIDENCE = "Baja confianza de medición"
    const val PROGRESS_LAST_VALID = "Última sesión: válida"
    const val PROGRESS_LAST_PRACTICE = "Última sesión: práctica (no cuenta)"
    const val PROGRESS_SCORE_CALIBRATING = "—"
    const val PROGRESS_SCORE_PREFIX = "Rendimiento de la tarea: "
    const val PROGRESS_NEXT_PREFIX = "Siguiente práctica sugerida: "
    const val PROGRESS_DISCLAIMER =
        "Este número refleja tu rendimiento en las tareas de MindBoost, no cómo te sientes en tu día a día."

    /**
     * MC8u-B §6 — human-readable, non-clinical label for a calibration [ProfileTier]; the result/history surfaces
     * render THIS, never the raw enum name (which would leak developer tokens like "EXPLORATORY"). Neutral wording:
     * it describes how much data exists, never a performance verdict.
     */
    fun tierLabel(tier: ProfileTier): String = when (tier) {
        ProfileTier.EXPLORATORY -> "Explorando (muestra inicial)"
        ProfileTier.PRELIMINARY -> "Muestra limitada"
        ProfileTier.INITIAL_TREND -> "Tendencia inicial"
        ProfileTier.STABLE -> "Lectura estable"
    }

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

    /**
     * MC8q: human-readable, non-clinical label for a transfer-confidence value — rendered on the result screen
     * INSTEAD of the raw [TransferLabel] enum name (which surfaced developer jargon like "PLACEBO_RISK" to users).
     * Honest and never over-promising: it states the LIMITS of transfer, never a health/far-transfer claim.
     */
    fun transferLabel(label: TransferLabel): String = when (label) {
        TransferLabel.TASK_ONLY -> "Solo en este ejercicio"
        TransferLabel.PLAUSIBLE -> "Transferencia plausible, no demostrada"
        TransferLabel.NOT_DEMONSTRATED -> "Transferencia no demostrada"
        TransferLabel.PLACEBO_RISK -> "Sin evidencia de transferencia"
    }

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
