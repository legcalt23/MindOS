@file:Suppress("MaxLineLength") // Full sentences read best whole.

package com.mindos.app.features.mindboost.coach.presentation

/**
 * MC8u-F — user-facing AI Coach copy. Tone: scientific, direct, serious, adult; never infantile, never manipulative,
 * never fear-to-sell-Elite. "diagnóstico"/"medicación" appear ONLY inside the negation disclaimers ([NO_DIAGNOSIS]/
 * [NO_MEDICATION]); no "cura", no "IQ garantizado", no therapeutic voice. This file is outside the copy-scanner tree,
 * so a dedicated test ([AICoachStringsTest]) enforces the rules instead.
 */
object AICoachStrings {
    const val ELITE_ENTRY = "Coach con IA: analiza tu progreso en las tareas y te sugiere qué practicar. Incluido en Elite."
    const val FREE_ENTRY = "El coach con IA es parte de Elite. Puedes seguir entrenando con normalidad sin él."

    const val CONSENT_TITLE = "Permitir que el coach analice tu progreso"
    const val CONSENT_BODY = "El coach usa un resumen de tu rendimiento en las tareas, no tus datos crudos ni información personal. Puedes retirar el permiso cuando quieras."
    const val CONSENT_ACCEPT = "Permitir"
    const val CONSENT_DECLINE = "Ahora no"

    const val DAILY_LIMIT = "Has usado tus mensajes del coach por hoy. Tu entrenamiento sigue disponible; vuelve mañana."

    const val NO_DIAGNOSIS = "El coach no da diagnósticos ni valoraciones de salud. Solo comenta tu rendimiento en las tareas."
    const val NO_MEDICATION = "El coach no recomienda medicación. Para temas de salud, consulta a un profesional cualificado."

    const val RECOMMENDATION_APPLIED = "Hecho: he añadido esta práctica a tu plan."
    const val SUMMARY_UPDATED = "He actualizado tu resumen de progreso."

    const val DELETE_MEMORY = "Borrar lo que el coach recuerda de tu progreso"
    const val DELETE_MEMORY_CONFIRM = "El coach olvidará tu resumen guardado. Tu entrenamiento y tu progreso no se borran."

    /** Promotional/neutral strings (must never contain clinical/manipulative language). */
    val PROMOTIONAL: List<String> = listOf(
        ELITE_ENTRY, FREE_ENTRY, CONSENT_TITLE, CONSENT_BODY, CONSENT_ACCEPT, CONSENT_DECLINE,
        DAILY_LIMIT, RECOMMENDATION_APPLIED, SUMMARY_UPDATED, DELETE_MEMORY, DELETE_MEMORY_CONFIRM,
    )

    /** Disclaimer strings — the ONLY place a clinical noun may appear, always as a negation. */
    val DISCLAIMERS: List<String> = listOf(NO_DIAGNOSIS, NO_MEDICATION)
}
