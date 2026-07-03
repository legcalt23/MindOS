package com.mindos.app.features.mindboost.language.domain.repair

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.repair.RepairNextAction.ADVANCE_TO_TRANSFER
import com.mindos.app.features.mindboost.language.domain.repair.RepairNextAction.ESCALATE_MICROLESSON
import com.mindos.app.features.mindboost.language.domain.repair.RepairNextAction.RECONSOLIDATE
import com.mindos.app.features.mindboost.language.domain.repair.RepairNextAction.SCHEDULE_MAINTENANCE

/**
 * Default [RepairEngine] with a built-in catalog covering every [ErrorType] (so [planFor] is
 * total). Pure and offline.
 *
 * [detectPattern] counts frequency over the RAW error list — deduping before counting would
 * make a "≥2 occurrences" pattern impossible to detect (project rule, CLAUDE.md 2026-06-15).
 */
class DefaultRepairEngine(
    private val patternThreshold: Int = DEFAULT_THRESHOLD,
) : RepairEngine {

    override fun detectPattern(errors: List<ErrorType>): ErrorPattern? =
        errors.groupingBy { it }.eachCount() // RAW counts, not deduplicated
            .maxByOrNull { it.value }
            ?.takeIf { it.value >= patternThreshold }
            ?.let { (errorType, count) ->
                ErrorPattern(
                    errorType = errorType,
                    frequency = count,
                    primaryTrait = errorType.primaryTraits.first(),
                    severity = severityFor(count),
                )
            }

    override fun planFor(pattern: ErrorPattern): RepairPlan = planForError(pattern.errorType)

    override fun planForError(errorType: ErrorType): RepairPlan = CATALOG.getValue(errorType)

    override fun resolveOutcome(plan: RepairPlan, improved: Boolean): RepairOutcome =
        RepairOutcome(
            errorType = plan.errorType,
            improved = improved,
            nextAction = if (improved) plan.nextIfImprove else plan.nextIfFail,
        )

    private fun severityFor(frequency: Int): RepairSeverity = when {
        frequency >= HIGH_FREQUENCY -> RepairSeverity.HIGH
        frequency >= MEDIUM_FREQUENCY -> RepairSeverity.MEDIUM
        else -> RepairSeverity.LOW
    }

    companion object {
        const val DEFAULT_THRESHOLD = 2
        const val MEDIUM_FREQUENCY = 3
        const val HIGH_FREQUENCY = 4

        private val CATALOG: Map<ErrorType, RepairPlan> = mapOf(
            ErrorType.INF_OVER to RepairPlan(
                ErrorType.INF_OVER,
                explanation = "Patrón: conclusión no respaldada por la evidencia mínima del texto.",
                microLesson = "Si el texto no lo dice ni lo implica necesariamente, queda como hipótesis.",
                isomorphicRetry = "Texto isomórfico con un distractor causal; marca solo lo soportado.",
                verificationCriterion = "Cada conclusión elegida tiene una cita textual que la respalda.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.INF_UNDER to RepairPlan(
                ErrorType.INF_UNDER,
                explanation = "Patrón: te quedas en lo literal y no completas el puente implícito.",
                microLesson = "Conecta causa, intención o consecuencia cuando el texto las implica.",
                isomorphicRetry = "Texto nuevo donde la respuesta exige un puente causal o intencional.",
                verificationCriterion = "La inferencia añade lo implícito sin inventar datos externos.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.LIT_DIST to RepairPlan(
                ErrorType.LIT_DIST,
                explanation = "Patrón: alteras información explícita del texto (literalismo distorsionado).",
                microLesson = "Localiza la frase exacta antes de responder; no parafrasees de memoria.",
                isomorphicRetry = "Texto nuevo; señala la frase literal que sostiene cada dato.",
                verificationCriterion = "La respuesta coincide con la información explícita, sin añadidos.",
                nextIfFail = RECONSOLIDATE,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.SYN_ROLE to RepairPlan(
                ErrorType.SYN_ROLE,
                explanation = "Patrón: confundes quién hace qué a quién (inversión de roles).",
                microLesson = "Mapea sujeto-acción-objeto antes de decidir.",
                isomorphicRetry = "Frase nueva con sujeto y objeto intercambiables; identifica el agente.",
                verificationCriterion = "Asignas correctamente agente y paciente de la acción.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.SYN_SCOPE to RepairPlan(
                ErrorType.SYN_SCOPE,
                explanation = "Patrón: te equivocas en el alcance de la negación o los cuantificadores.",
                microLesson = "Marca el operador (no, todos, solo) y delimita qué abarca.",
                isomorphicRetry = "Frase nueva con negación de alcance ambiguo; resuelve el alcance.",
                verificationCriterion = "Interpretas correctamente el alcance del operador.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.SUM_NO_THESIS to RepairPlan(
                ErrorType.SUM_NO_THESIS,
                explanation = "Patrón: incluiste detalles secundarios y perdiste la tesis.",
                microLesson = "Primero la tesis, luego solo el detalle que la sostiene (escalera 5→3→1).",
                isomorphicRetry = "Texto nuevo con un detalle llamativo distractor; resume conservando la tesis.",
                verificationCriterion = "El resumen empieza por la idea central y omite el ruido.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.SUM_DISTORT to RepairPlan(
                ErrorType.SUM_DISTORT,
                explanation = "Patrón: tu síntesis cambia el significado del original.",
                microLesson = "Comprime sin alterar el matiz: misma afirmación, menos palabras.",
                isomorphicRetry = "Texto nuevo; resume y comprueba la fidelidad frase a frase.",
                verificationCriterion = "El resumen preserva el matiz y la dirección del original.",
                nextIfFail = RECONSOLIDATE,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.ARG_GAP to RepairPlan(
                ErrorType.ARG_GAP,
                explanation = "Patrón: la conclusión no se sigue de las premisas.",
                microLesson = "Encadena premisa → evidencia → conclusión y detecta el eslabón ausente.",
                isomorphicRetry = "Argumento nuevo con un salto lógico; señala el eslabón débil.",
                verificationCriterion = "La conclusión está soportada por una cadena explícita.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.ARG_STRAW to RepairPlan(
                ErrorType.ARG_STRAW,
                explanation = "Patrón: rebates una versión debilitada del argumento ajeno (hombre de paja).",
                microLesson = "Reconstruye la postura contraria en su versión más fuerte antes de responder.",
                isomorphicRetry = "Texto nuevo; resume la tesis contraria de forma caritativa y luego responde.",
                verificationCriterion = "Tu reconstrucción es fiel y reconocible para quien la sostiene.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.CRIT_SOURCE to RepairPlan(
                ErrorType.CRIT_SOURCE,
                explanation = "Patrón: aceptas o rechazas sin evaluar la fuente.",
                microLesson = "Separa dato, opinión, sesgo e interés de la fuente.",
                isomorphicRetry = "Noticia nueva; clasifica qué es dato, qué es opinión y qué es posible interés.",
                verificationCriterion = "Distingues afirmaciones verificables de juicios e intereses.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.PRAG_TONE to RepairPlan(
                ErrorType.PRAG_TONE,
                explanation = "Patrón: el tono no se ajusta a la audiencia o el canal.",
                microLesson = "Ajusta el tono a audiencia-canal-riesgo sin perder la verdad del mensaje.",
                isomorphicRetry = "Misma información, audiencia nueva; reescribe con el tono adecuado.",
                verificationCriterion = "El mensaje mantiene el contenido y encaja con la audiencia.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.PROD_VAGUE to RepairPlan(
                ErrorType.PROD_VAGUE,
                explanation = "Patrón: el texto es vago y poco concreto.",
                microLesson = "Usa verbos operativos y datos concretos; evita abstracciones huecas.",
                isomorphicRetry = "Reescribe una frase vaga dada en una versión concreta y precisa.",
                verificationCriterion = "Cada afirmación es concreta y comprobable.",
                nextIfFail = RECONSOLIDATE,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.PROD_BLOAT to RepairPlan(
                ErrorType.PROD_BLOAT,
                explanation = "Patrón: sobra texto y baja la densidad de información (verborrea).",
                microLesson = "Comprime por densidad: elimina lo que no añade información.",
                isomorphicRetry = "Reduce un párrafo dado a la mitad conservando el núcleo.",
                verificationCriterion = "El texto final dice lo mismo con menos palabras.",
                nextIfFail = RECONSOLIDATE,
                nextIfImprove = ADVANCE_TO_TRANSFER,
            ),
            ErrorType.META_MIS to RepairPlan(
                ErrorType.META_MIS,
                explanation = "Patrón: tu confianza no se alinea con tu acierto (sobre/infraconfianza).",
                microLesson = "Predice tu acierto antes de responder y compara con el resultado.",
                isomorphicRetry = "Bloque nuevo; declara confianza por ítem y revisa la calibración.",
                verificationCriterion = "Tu confianza media se acerca a tu acierto real.",
                nextIfFail = ESCALATE_MICROLESSON,
                nextIfImprove = SCHEDULE_MAINTENANCE,
            ),
        )
    }
}
