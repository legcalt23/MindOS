package com.mindos.app.features.mindboost.logic.domain.feedback

import com.mindos.app.features.mindboost.logic.domain.model.DistractorType
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicFeedback
import com.mindos.app.features.mindboost.logic.domain.model.LogicItem
import com.mindos.app.features.mindboost.logic.domain.model.LogicResponse
import com.mindos.app.features.mindboost.logic.domain.scoring.ScoringResult

fun interface FeedbackEngine {
    fun feedback(item: LogicItem, response: LogicResponse, scoring: ScoringResult): LogicFeedback
}

/** Deterministic feedback that diagnoses the next useful move without revealing the solution. */
class DefaultLogicFeedbackEngine : FeedbackEngine {

    override fun feedback(item: LogicItem, response: LogicResponse, scoring: ScoringResult): LogicFeedback {
        if (scoring.isCorrect) {
            return if (response.confidence.probability < LOW_CONFIDENCE_THRESHOLD) {
                LogicFeedback(
                    title = "Inferencia correcta, confianza baja",
                    diagnostic = "Tu razonamiento fue válido, pero tu confianza no reflejó esa evidencia.",
                    nextStep = "Revisa qué premisas y regla hicieron necesaria la conclusión.",
                )
            } else {
                LogicFeedback(
                    title = "Inferencia bien justificada",
                    diagnostic = "La respuesta, las premisas y la regla apuntan en la misma dirección.",
                    nextStep = "Comprueba si reconocerías la misma estructura con otra superficie.",
                )
            }
        }

        val distractor = item.answerOptions.firstOrNull { it.id == response.selectedAnswerId }?.distractorType
        return feedbackForDistractor(distractor, response)
    }

    private fun feedbackForDistractor(type: DistractorType?, response: LogicResponse): LogicFeedback = when (type) {
        DistractorType.AFFIRMING_CONSEQUENT -> diagnostic(
            LogicErrorTag.AFFIRMING_CONSEQUENT,
            "Has tratado el resultado como prueba suficiente de su posible causa.",
            "Busca si otra causa también podría producir ese resultado.",
        )
        DistractorType.DENYING_ANTECEDENT -> diagnostic(
            LogicErrorTag.DENYING_ANTECEDENT,
            "Has supuesto que, si no ocurre P, tampoco puede ocurrir Q.",
            "Comprueba si Q podría aparecer por otra vía.",
        )
        DistractorType.CONFIRMATION_BIAS -> diagnostic(
            LogicErrorTag.CONFIRMATION_BIAS,
            "Has elegido un caso que confirma la regla, pero no uno que pueda refutarla.",
            "Para refutar P → Q, busca P verdadero y Q falso.",
        )
        DistractorType.FALSE_CONTRADICTION -> diagnostic(
            LogicErrorTag.FALSE_CONTRADICTION,
            "Has marcado contradicción sin que dos afirmaciones se nieguen entre sí.",
            "Distingue incompatibilidad real de información simplemente distinta.",
        )
        DistractorType.MISSING_PREMISE -> diagnostic(
            LogicErrorTag.MISSING_PREMISE,
            "La conclusión necesita una premisa que no está disponible.",
            "Separa lo dicho de lo que parecería razonable asumir.",
        )
        DistractorType.OVERINFERENCE -> diagnostic(
            LogicErrorTag.OVERINFERENCE,
            "La conclusión va más allá de lo que permiten las premisas.",
            "Reduce la conclusión hasta que cada parte esté respaldada.",
        )
        DistractorType.BELIEF_BIAS -> diagnostic(
            LogicErrorTag.BELIEF_BIAS,
            "La opción puede sonar creíble, pero la credibilidad no demuestra validez.",
            "Evalúa la estructura aunque el contenido te resulte familiar.",
        )
        DistractorType.SURFACE_MATCH -> diagnostic(
            LogicErrorTag.SURFACE_MATCH,
            "Has emparejado por palabras o apariencia, no por relación lógica.",
            "Representa la relación abstracta antes de elegir.",
        )
        null -> LogicFeedback(
            title = "Revisa la cadena inferencial",
            diagnostic = if (response.confidence.probability >= HIGH_CONFIDENCE_THRESHOLD) {
                "La respuesta fue incorrecta con confianza alta. Conviene revisar qué evidencia faltaba."
            } else {
                "La respuesta no se sostiene todavía con las premisas y la regla elegidas."
            },
            nextStep = "Comprueba premisas, regla y conclusión por separado.",
        )
    }

    private fun diagnostic(error: LogicErrorTag, message: String, next: String) = LogicFeedback(
        title = "Patrón detectado",
        diagnostic = message,
        nextStep = next,
        errorTag = error,
    )

    private companion object {
        const val LOW_CONFIDENCE_THRESHOLD = 0.5
        const val HIGH_CONFIDENCE_THRESHOLD = 0.85
    }
}

