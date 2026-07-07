@file:Suppress("MaxLineLength") // Full sentences read best whole.

package com.mindos.app.features.mindboost.coach.presentation

/**
 * MC8u-F — user-facing AI Coach copy. Tone: scientific, direct, serious, adult; never infantile, never manipulative,
 * never fear-to-sell-Elite. "diagnóstico"/"medicación" appear ONLY inside the negation disclaimers ([NO_DIAGNOSIS]/
 * [NO_MEDICATION]); no "cura", no "IQ garantizado", no therapeutic voice. This file is outside the copy-scanner tree,
 * so a dedicated test ([AICoachStringsTest]) enforces the rules instead.
 */
object AICoachStrings {
    const val TITLE = "Coach con IA"
    const val ELITE_ENTRY = "Coach con IA: analiza tu progreso en las tareas y te sugiere qué practicar. Incluido en Elite."
    const val FREE_ENTRY = "El coach con IA es parte de Elite. Puedes seguir entrenando con normalidad sin él."

    // MC8u-G §3 key texts (must be present, serious, non-manipulative).
    const val ANALYZES = "IA Coach Elite analiza tu progreso dentro de MindBoost."
    const val STRUCTURED_SUMMARIES = "Usa resúmenes estructurados, no datos crudos."
    const val CAN_DELETE_MEMORY = "Puedes borrar la memoria IA."
    const val TRAINING_AVAILABLE_AT_LIMIT = "Tu entrenamiento sigue disponible aunque alcances el límite diario."

    const val CONSENT_TITLE = "Permitir que el coach analice tu progreso"
    const val CONSENT_BODY = "El coach usa un resumen de tu rendimiento en las tareas, no tus datos crudos ni información personal. Puedes retirar el permiso cuando quieras."
    const val CONSENT_ACCEPT = "Permitir"
    const val CONSENT_DECLINE = "Ahora no"
    const val CONSENT_REVOKE = "Retirar el permiso"
    const val CONSENT_REVOKED_INFO = "Has retirado el permiso. El coach no analizará tu progreso hasta que lo permitas de nuevo. Tu resumen guardado sigue ahí hasta que decidas borrarlo."

    const val INPUT_HINT = "Escribe tu pregunta sobre tu entrenamiento…"
    const val SEND = "Enviar"
    const val DAILY_LIMIT = "Has usado tus mensajes del coach por hoy. Tu entrenamiento sigue disponible; vuelve mañana."

    const val NO_DIAGNOSIS = "El coach no da diagnósticos ni valoraciones de salud. Solo comenta tu rendimiento en las tareas."
    const val NO_MEDICATION = "El coach no recomienda medicación. Para temas de salud, consulta a un profesional cualificado."
    const val NO_GUARANTEE = "El coach no sustituye a profesionales y no promete mejoras garantizadas."

    const val RECOMMENDATION_TITLE = "Sugerencia de práctica"
    const val RECOMMENDATION_CONFIRM = "Añadir a mi plan"
    const val RECOMMENDATION_REJECT = "Ahora no"
    const val RECOMMENDATION_APPLIED = "Hecho: he añadido esta práctica a tu plan."
    const val RECOMMENDATION_REJECTED = "De acuerdo, no he cambiado tu plan."
    const val RECOMMENDATION_PENDING = "Aplicar sugerencias al plan todavía no está disponible en esta versión. Tu plan no ha cambiado."
    const val SUMMARY_UPDATED = "He actualizado tu resumen de progreso."

    const val MEMORY_TITLE = "Lo que el coach recuerda de tu progreso"
    const val MEMORY_EMPTY = "Aún no hay resumen guardado. Entrena unas sesiones y el coach creará uno."
    const val MEMORY_LOCAL_ONLY = "Por ahora tu resumen se guarda solo en este dispositivo."
    const val DELETE_MEMORY = "Borrar lo que el coach recuerda de tu progreso"
    const val DELETE_MEMORY_CONFIRM = "El coach olvidará tu resumen guardado. Tu entrenamiento y tu progreso no se borran."
    const val MEMORY_DELETED = "Listo: el coach ha olvidado tu resumen guardado. Tu entrenamiento y tu progreso no se borran."

    /** Number of coach messages left today (a count is not content — safe to show). */
    fun messagesLeftToday(remaining: Int): String = "Mensajes del coach hoy: $remaining"

    /** Promotional/neutral strings (must never contain clinical/manipulative language). */
    val PROMOTIONAL: List<String> = listOf(
        TITLE, ELITE_ENTRY, FREE_ENTRY, ANALYZES, STRUCTURED_SUMMARIES, CAN_DELETE_MEMORY, TRAINING_AVAILABLE_AT_LIMIT,
        CONSENT_TITLE, CONSENT_BODY, CONSENT_ACCEPT, CONSENT_DECLINE, CONSENT_REVOKE, CONSENT_REVOKED_INFO,
        INPUT_HINT, SEND, DAILY_LIMIT, RECOMMENDATION_TITLE, RECOMMENDATION_CONFIRM, RECOMMENDATION_REJECT,
        RECOMMENDATION_APPLIED, RECOMMENDATION_REJECTED, RECOMMENDATION_PENDING, SUMMARY_UPDATED,
        MEMORY_TITLE, MEMORY_EMPTY, MEMORY_LOCAL_ONLY, DELETE_MEMORY, DELETE_MEMORY_CONFIRM, MEMORY_DELETED,
    )

    /** Disclaimer strings — the ONLY place a clinical noun / "guarantee" may appear, always as a negation. */
    val DISCLAIMERS: List<String> = listOf(NO_DIAGNOSIS, NO_MEDICATION, NO_GUARANTEE)
}
