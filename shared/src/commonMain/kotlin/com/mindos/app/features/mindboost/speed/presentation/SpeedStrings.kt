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
    const val FEATURE_TITLE = "Velocidad Mental · Medición (MVP-0)"
    const val MVP_NOTE = "Módulo de medición. Perfil preliminar, no validado y orientativo; no es una nota."

    const val E1_TITLE = "E1 · Destello"
    const val E1_INSTRUCTIONS = "Toca la pantalla en cuanto aparezca el destello."
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
}
