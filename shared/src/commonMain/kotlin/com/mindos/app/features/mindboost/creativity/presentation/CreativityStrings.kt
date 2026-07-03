package com.mindos.app.features.mindboost.creativity.presentation

import com.mindos.app.features.mindboost.creativity.domain.model.CreativityPracticeLabel
import com.mindos.app.features.mindboost.creativity.domain.validation.CreativityValidationError

/** User-facing copy for the Creativity feature. All claims are safe (practice, non-clinical). */
object CreativityStrings {
    const val FEATURE_TITLE = "Creatividad"
    const val MENU_SUBTITLE = "Práctica de pensamiento divergente, local y sin conexión."

    /** Mandatory safety disclaimer — shown on the menu and on the result screen. */
    const val DISCLAIMER = "Solo práctica funcional · No es una nota · Proxy interno y preliminar"

    const val BACK = "Volver"
    const val IDEA_PLACEHOLDER = "Escribe una idea…"
    const val ADD_IDEA = "Añadir"
    const val SUBMIT = "Terminar y ver resultado"
    const val SUBMITTING = "Guardando…"
    const val ANOTHER_ROUND = "Otra ronda"
    const val IDEAS_PREFIX = "Ideas añadidas: "

    const val RESULT_TITLE = "Resultado de tu práctica"
    const val FLUENCY_LABEL = "Fluidez (ideas distintas)"
    const val ELABORATION_LABEL = "Elaboración (palabras por idea)"

    fun labelText(label: CreativityPracticeLabel): String = when (label) {
        CreativityPracticeLabel.KEEP_PRACTICING -> "Sigue practicando"
        CreativityPracticeLabel.STEADY_PRACTICE -> "Práctica constante"
        CreativityPracticeLabel.STRONG_LOCAL_SESSION -> "Sesión local fuerte"
    }

    fun rejectionText(reason: CreativityValidationError): String = when (reason) {
        CreativityValidationError.EMPTY_RESPONSE -> "Escribe al menos una idea."
        CreativityValidationError.BELOW_MIN_IDEAS -> "Añade más ideas distintas para terminar."
        CreativityValidationError.TIMING_NOT_MONOTONIC -> "No se pudo registrar el tiempo. Inténtalo de nuevo."
    }
}
