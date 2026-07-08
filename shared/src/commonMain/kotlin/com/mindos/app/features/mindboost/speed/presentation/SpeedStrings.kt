package com.mindos.app.features.mindboost.speed.presentation

/**
 * Cadenas de UI de Velocidad Mental. SOLO mensajes operativos/descriptivos (§9,
 * acceptance #17/#20). PROHIBIDO cualquier claim de mejora cognitiva, percentil,
 * perfil o comparación normativa.
 *
 * Permitido: "Datos registrados correctamente", "Bloque completado",
 * "Precisión del bloque: X%", "Tiempo mediano de respuesta: Y ms".
 * Prohibido: "Tu velocidad mental mejoró", "Tienes perfil impulsivo",
 * "Estás por encima del promedio", "Tu capacidad cognitiva aumentó".
 */
object SpeedStrings {
    const val FEATURE_TITLE = "Velocidad Mental"
    const val MVP_NOTE =
        "Entrenamiento preliminar. Resultados orientativos hasta completar la validación técnica; " +
            "la velocidad se interpreta junto con precisión y estabilidad."

    /** Hub track subtitle. Explains the beta gate in user terms without over-committing to a device rule. */
    const val HUB_SUBTITLE =
        "Mide tu rapidez de respuesta. Beta: resultados orientativos; " +
            "la precisión depende del dispositivo. Sin puntuación validada aún."

    const val E1_TITLE = "E1 · Destello"
    const val E1_INSTRUCTIONS = "Toca la pantalla en cuanto aparezca el destello."
    const val E2_TITLE = "E2 · Comparación"
    const val E2_INSTRUCTIONS = "Aparecen dos números. Toca rápido el lado del número mayor, sin fallar."
    const val E4_TITLE = "E4 · Cambio de Regla"
    const val E4_INSTRUCTIONS =
        "La regla cambia durante la sesión. Aplica la regla activa (color o forma) y adáptate rápido sin fallar."
    const val E5_TITLE = "E5 · Búsqueda Visual"
    const val E5_INSTRUCTIONS =
        "Localiza el objetivo entre los distractores y toca el panel donde aparece, con velocidad y precisión."
    const val E3_TITLE = "E3 · Cifrado"
    const val E3_INSTRUCTIONS = "Mira la clave. ¿El símbolo y el valor coinciden? Izquierda = sí, derecha = no."
    const val E6_TITLE = "E6 · Multivía"
    const val E6_INSTRUCTIONS = "Toca el lado que coincide con la señal."
    const val E7_TITLE = "E7 · Compuerta"
    const val E7_INSTRUCTIONS = "Puerta abierta: pulsa la dirección de la flecha. Cerrada: pulsa la contraria."

    const val START = "Iniciar bloque"
    const val WAIT = "Espera…"
    const val TAP_NOW = "¡Ahora!"
    const val TOO_SOON = "Demasiado pronto. Espera a la señal."
    const val LATE = "Tarde"
    const val BLOCK = "Bloque"
    const val LIMIT = "Límite"
    const val BLOCK_COMPLETED = "Bloque completado."
    const val DATA_SAVED = "Datos registrados correctamente."
    const val RUN_AGAIN = "Repetir bloque"
    const val BACK = "Volver"

    const val LEFT = "Izquierda"
    const val RIGHT = "Derecha"

    // Etiquetas descriptivas (sin claims). Se interpolan con valores medidos.
    const val LABEL_ACCURACY = "Precisión del bloque"
    const val LABEL_MEDIAN_RT = "Tiempo mediano de respuesta"
    const val LABEL_TRIALS = "Trials registrados"
    const val LABEL_ANTICIPATIONS = "Anticipaciones"
    const val LABEL_QUALITY = "Calidad técnica de la medición"
    const val LABEL_PRECISION = "Precisión de marca temporal"
    const val NOT_AVAILABLE = "no disponible"
    const val MILLIS_SUFFIX = " ms"

    // Dashboard v1 (MVP-3). Resultado PRELIMINAR y no validado; lenguaje funcional, no normativo.
    const val DASHBOARD_TITLE = "Perfil funcional (preliminar)"
    const val DASHBOARD_OPEN = "Ver perfil preliminar"
    const val DASHBOARD_DISCLAIMER =
        "Resultado PRELIMINAR y sin calibrar. Proxy funcional y orientativo, no una nota. " +
            "Describe solo el patrón de esta corrida y aún no está validado en dispositivos."
    const val DASHBOARD_BAR_SPEED = "Velocidad útil"
    const val DASHBOARD_BAR_CONTROL = "Precisión / control"
    const val DASHBOARD_BAR_STABILITY = "Estabilidad"
    const val DASHBOARD_TOTAL = "Índice preliminar"
    const val DASHBOARD_CEILING_NOTE = "Índice limitado por baja precisión o inestabilidad."
    const val DASHBOARD_PATTERN = "Patrón de la corrida"

    // Feedback post-corrida de E2 · Comparación (MC9-A). Serio y accionable, nunca arcade/infantil ni con
    // promesas de mejora amplia; DESCRIBE la corrida bajo la lente "velocidad con precisión" (todas mencionan ambas).
    const val FEEDBACK_TITLE = "Lectura de la corrida"
    const val FEEDBACK_CALIBRATING =
        "Aún faltan datos para leer tu equilibrio entre velocidad y precisión. Sigue practicando."
    const val FEEDBACK_FAST_LOW_PRECISION =
        "Tu velocidad fue alta, pero la precisión cayó. En MindBoost la velocidad solo suma cuando mantiene el control."
    const val FEEDBACK_SLOW_PRECISE =
        "Priorizaste la precisión sobre la velocidad. Puedes intentar responder algo más rápido sin perder precisión."
    const val FEEDBACK_VARIABLE =
        "Tu ritmo fue variable. Busca velocidad estable con buena precisión antes de subir la intensidad."
    const val FEEDBACK_BALANCED =
        "Buen equilibrio entre velocidad y precisión. Puedes subir la intensidad."

    // Feedback post-corrida de E4 · Cambio de Regla (MC9-B). Serio y accionable, lente flexibilidad-con-precisión.
    const val RS_FEEDBACK_CALIBRATING =
        "Aún faltan datos para leer tu adaptación al cambio de regla. Sigue practicando."
    const val RS_FEEDBACK_LOW_PRECISION =
        "Tu velocidad fue alta, pero la precisión cayó. La velocidad solo suma cuando mantiene el control."
    const val RS_FEEDBACK_HIGH_SWITCH_COST =
        "El coste de cambio fue alto: reduce la velocidad hasta estabilizar la regla nueva."
    const val RS_FEEDBACK_GOOD_ADAPTATION =
        "Te adaptaste bien al cambio de regla manteniendo precisión."
    const val RS_FEEDBACK_BALANCED =
        "Buen equilibrio entre velocidad y flexibilidad."

    // Feedback post-corrida de E5 · Búsqueda Visual (MC9-C). Lente detección-visual-con-precisión: cada mensaje
    // menciona la búsqueda/detección y la precisión o estabilidad. Serio, sin arcade ni claims oficiales.
    const val VS_FEEDBACK_CALIBRATING =
        "Aún faltan datos para leer tu búsqueda visual con precisión. Sigue practicando."
    const val VS_FEEDBACK_GOOD_VISUAL_CONTROL =
        "Buena detección visual con velocidad controlada y precisión."
    const val VS_FEEDBACK_FAST_LOW_PRECISION =
        "Detectamos respuestas rápidas incorrectas: localiza el objetivo antes de responder para no perder precisión."
    const val VS_FEEDBACK_HIGH_VISUAL_LOAD_COST =
        "La carga visual afectó tu precisión: reduce la velocidad hasta estabilizar la búsqueda."
    const val VS_FEEDBACK_UNSTABLE_SEARCH =
        "Tu estabilidad cayó con la densidad visual. Busca un ritmo constante de exploración con buena precisión."
    const val VS_FEEDBACK_STABLE_REDUCE_LATENCY =
        "Detección estable con buena precisión; el siguiente objetivo es reducir la latencia sin perder aciertos."
    const val VS_FEEDBACK_BALANCED =
        "Buen equilibrio entre exploración visual y precisión."

    // Etiquetas de macro-patrón: DESCRIPTIVAS de la corrida, nunca afirmaciones sobre la persona.
    const val PATTERN_CALIBRATING = "En calibración"
    const val PATTERN_FAST_IMPULSIVE = "Rápido con poco control"
    const val PATTERN_SLOW_PRECISE = "Lento y preciso"
    const val PATTERN_VARIABLE = "Variable"
    const val PATTERN_BALANCED = "Equilibrado"

    // Panel de depuración (solo visible en builds debug).
    const val DEBUG_PANEL_TITLE = "Depuración (solo debug)"
    const val DEBUG_EXPORT = "Exportar datos brutos (JSON/CSV)"
    const val DEBUG_EXPORT_DISABLED = "Exportación deshabilitada en release."
    const val DEBUG_DELETE = "Borrar mis datos de medición"
    const val DEBUG_DELETED = "Datos de medición borrados."

    /**
     * Every user-visible (non-debug) string. Backs `SpeedStringsHonestyTest`: no internal milestone/build
     * labels ("MVP-0", "Hito", "MC9-A"), preliminary framing stays visible, and no official/rank/normative
     * claim appears while the physical-validation gate §15 is open.
     */
    val VISIBLE_COPY: List<String> = listOf(
        FEATURE_TITLE, MVP_NOTE, HUB_SUBTITLE,
        E1_TITLE, E1_INSTRUCTIONS, E2_TITLE, E2_INSTRUCTIONS, E4_TITLE, E4_INSTRUCTIONS,
        E5_TITLE, E5_INSTRUCTIONS,
        E3_TITLE, E3_INSTRUCTIONS, E6_TITLE, E6_INSTRUCTIONS, E7_TITLE, E7_INSTRUCTIONS,
        START, WAIT, TAP_NOW, TOO_SOON, LATE, BLOCK, LIMIT, BLOCK_COMPLETED, DATA_SAVED, RUN_AGAIN, BACK,
        LEFT, RIGHT, LABEL_ACCURACY, LABEL_MEDIAN_RT, LABEL_TRIALS, LABEL_ANTICIPATIONS, LABEL_QUALITY,
        LABEL_PRECISION, NOT_AVAILABLE, MILLIS_SUFFIX,
        DASHBOARD_TITLE, DASHBOARD_OPEN, DASHBOARD_DISCLAIMER, DASHBOARD_BAR_SPEED, DASHBOARD_BAR_CONTROL,
        DASHBOARD_BAR_STABILITY, DASHBOARD_TOTAL, DASHBOARD_CEILING_NOTE, DASHBOARD_PATTERN,
        PATTERN_CALIBRATING, PATTERN_FAST_IMPULSIVE, PATTERN_SLOW_PRECISE, PATTERN_VARIABLE, PATTERN_BALANCED,
        FEEDBACK_TITLE, FEEDBACK_CALIBRATING, FEEDBACK_FAST_LOW_PRECISION, FEEDBACK_SLOW_PRECISE,
        FEEDBACK_VARIABLE, FEEDBACK_BALANCED,
        RS_FEEDBACK_CALIBRATING, RS_FEEDBACK_LOW_PRECISION, RS_FEEDBACK_HIGH_SWITCH_COST,
        RS_FEEDBACK_GOOD_ADAPTATION, RS_FEEDBACK_BALANCED,
        VS_FEEDBACK_CALIBRATING, VS_FEEDBACK_GOOD_VISUAL_CONTROL, VS_FEEDBACK_FAST_LOW_PRECISION,
        VS_FEEDBACK_HIGH_VISUAL_LOAD_COST, VS_FEEDBACK_UNSTABLE_SEARCH, VS_FEEDBACK_STABLE_REDUCE_LATENCY,
        VS_FEEDBACK_BALANCED,
    )
}
