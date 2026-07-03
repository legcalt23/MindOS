@file:Suppress("LongMethod")

package com.mindos.app.features.mindboost.logic.domain.content

import com.mindos.app.features.mindboost.logic.domain.model.AnswerOption
import com.mindos.app.features.mindboost.logic.domain.model.CounterexampleCase
import com.mindos.app.features.mindboost.logic.domain.model.DistractorType
import com.mindos.app.features.mindboost.logic.domain.model.JustificationClaim
import com.mindos.app.features.mindboost.logic.domain.model.LogicDifficulty
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicExerciseType
import com.mindos.app.features.mindboost.logic.domain.model.LogicItem
import com.mindos.app.features.mindboost.logic.domain.model.LogicPremise
import com.mindos.app.features.mindboost.logic.domain.model.LogicSkill
import com.mindos.app.features.mindboost.logic.domain.model.ReasoningRule
import com.mindos.app.features.mindboost.logic.domain.model.SurfaceForm
import com.mindos.app.features.mindboost.logic.domain.model.TransferLevel

fun interface LogicContentProvider {
    fun sessionItems(): List<LogicItem>
}

/**
 * Initial authored pack. It is deliberately declarative: a future generative provider can
 * implement the same contract and keep the scorer, session engine and UI unchanged.
 */
class DefaultLogicContentProvider : LogicContentProvider {

    override fun sessionItems(): List<LogicItem> = listOf(
        deductionItem(),
        conditionalItem(),
        necessarySufficientItem(),
        consistencyItem(),
        counterexampleItem(),
        conditionalIsomorphItem(),
    )

    private fun deductionItem() = LogicItem(
        id = "logic-e1-audit-01",
        exerciseType = LogicExerciseType.DEDUCTION_FROM_PREMISES,
        targetSkills = listOf(
            LogicSkill.NECESSARY_DEDUCTION,
            LogicSkill.CONFIDENCE_CALIBRATION,
        ),
        difficultyLevel = LogicDifficulty.FOUNDATION,
        logicalStructureId = "modus_ponens_two_premises",
        surfaceVariantId = "software_audit",
        surfaceForm = SurfaceForm.SOFTWARE,
        prompt = "¿Qué conclusión se sigue necesariamente?",
        premises = listOf(
            LogicPremise("p1", "Todo archivo auditado tiene un registro de revisión."),
            LogicPremise("p2", "El archivo K fue auditado."),
            LogicPremise("p3", "El archivo K pertenece al equipo azul."),
        ),
        answerOptions = listOf(
            option(
                "a",
                "El archivo K tiene un registro de revisión.",
                true,
                "Aplicaste la regla a las premisas necesarias.",
            ),
            option(
                "b",
                "Todo archivo del equipo azul fue auditado.",
                false,
                "La pertenencia al equipo azul no permite generalizar.",
                DistractorType.OVERINFERENCE,
            ),
            option(
                "c",
                "No se puede determinar si K tiene registro.",
                false,
                "Las dos primeras premisas sí determinan la conclusión.",
                DistractorType.MISSING_PREMISE,
            ),
        ),
        correctAnswerId = "a",
        validReasoningRules = listOf(ReasoningRule.MODUS_PONENS),
        requiredPremiseIds = listOf("p1", "p2"),
        expectedJustificationClaim = JustificationClaim.CONCLUSION_FOLLOWS,
        explanation = "Si todo archivo auditado tiene registro y K fue auditado, K tiene registro.",
        commonErrorTags = listOf(LogicErrorTag.OVERINFERENCE, LogicErrorTag.MISSING_PREMISE),
        isIsomorphicProbe = false,
        transferLevel = TransferLevel.TRAINED,
        hints = listOf("Identifica la regla general y el caso particular."),
    )

    private fun conditionalItem() = LogicItem(
        id = "logic-e3-access-01",
        exerciseType = LogicExerciseType.CONDITIONAL_TEST,
        targetSkills = listOf(
            LogicSkill.CONDITIONAL_REASONING,
            LogicSkill.PROOF_REFUTATION,
        ),
        difficultyLevel = LogicDifficulty.DEVELOPING,
        logicalStructureId = CONDITIONAL_REFUTATION_STRUCTURE,
        surfaceVariantId = "access_card",
        surfaceForm = SurfaceForm.EVERYDAY,
        prompt = "Regla: si una tarjeta está activada, se enciende una luz verde. ¿Qué caso refuta la regla?",
        premises = listOf(
            LogicPremise("p1", "La regla solo falla si aparece una tarjeta activada sin luz verde."),
        ),
        answerOptions = listOf(
            option(
                "a",
                "Tarjeta activada y luz verde.",
                false,
                "Confirma, pero no intenta refutar.",
                DistractorType.CONFIRMATION_BIAS,
            ),
            option("b", "Tarjeta activada y luz apagada.", true, "Es P verdadero con Q falso."),
            option(
                "c",
                "Tarjeta desactivada y luz apagada.",
                false,
                "No activa la condición de la regla.",
                DistractorType.DENYING_ANTECEDENT,
            ),
            option(
                "d",
                "Luz verde en una tarjeta cualquiera.",
                false,
                "Ver Q no demuestra que P sea su única causa.",
                DistractorType.AFFIRMING_CONSEQUENT,
            ),
        ),
        correctAnswerId = "b",
        validReasoningRules = listOf(ReasoningRule.CONDITIONAL_FALSIFICATION, ReasoningRule.MODUS_TOLLENS),
        requiredPremiseIds = listOf("p1"),
        expectedJustificationClaim = JustificationClaim.RULE_IS_REFUTED,
        explanation = "Un condicional P → Q queda refutado por P verdadero y Q falso.",
        commonErrorTags = listOf(
            LogicErrorTag.AFFIRMING_CONSEQUENT,
            LogicErrorTag.DENYING_ANTECEDENT,
            LogicErrorTag.CONFIRMATION_BIAS,
        ),
        isIsomorphicProbe = false,
        transferLevel = TransferLevel.TRAINED,
        hints = listOf("Busca un caso que haga verdadera la condición y falso el resultado."),
    )

    private fun necessarySufficientItem() = LogicItem(
        id = "logic-e4-publish-01",
        exerciseType = LogicExerciseType.NECESSARY_SUFFICIENT,
        targetSkills = listOf(
            LogicSkill.NECESSARY_SUFFICIENT,
            LogicSkill.VERBAL_FORMALIZATION,
        ),
        difficultyLevel = LogicDifficulty.DEVELOPING,
        logicalStructureId = "necessary_not_sufficient",
        surfaceVariantId = "publish_email",
        surfaceForm = SurfaceForm.SOFTWARE,
        prompt = "Para publicar un artículo es necesario verificar el email. Verificarlo no garantiza la publicación.",
        premises = listOf(
            LogicPremise("p1", "Sin email verificado no se puede publicar."),
            LogicPremise("p2", "Puede haber otros requisitos además del email."),
        ),
        answerOptions = listOf(
            option("a", "Verificar el email es necesario, pero no suficiente.", true, "Distinguiste ambos conceptos."),
            option(
                "b",
                "Verificar el email es suficiente, pero no necesario.",
                false,
                "Invierte la relación indicada.",
                DistractorType.DENYING_ANTECEDENT,
            ),
            option(
                "c",
                "Verificar el email es necesario y suficiente.",
                false,
                "Añade una garantía que la segunda premisa niega.",
                DistractorType.OVERINFERENCE,
            ),
            option(
                "d",
                "No es necesario ni suficiente.",
                false,
                "Ignora la condición explícita.",
                DistractorType.MISSING_PREMISE,
            ),
        ),
        correctAnswerId = "a",
        validReasoningRules = listOf(ReasoningRule.NECESSARY_SUFFICIENT_CLASSIFICATION),
        requiredPremiseIds = listOf("p1", "p2"),
        expectedJustificationClaim = JustificationClaim.CONDITIONS_CLASSIFIED,
        explanation = "La verificación es requerida, pero por sí sola no basta.",
        commonErrorTags = listOf(LogicErrorTag.OVERINFERENCE, LogicErrorTag.DENYING_ANTECEDENT),
        isIsomorphicProbe = false,
        transferLevel = TransferLevel.TRAINED,
        hints = listOf("Pregunta primero si puede ocurrir sin email; después, si el email lo garantiza."),
    )

    private fun consistencyItem() = LogicItem(
        id = "logic-e5-reservation-01",
        exerciseType = LogicExerciseType.CONTRADICTION_DETECTION,
        targetSkills = listOf(
            LogicSkill.CONSISTENCY,
            LogicSkill.PROOF_REFUTATION,
        ),
        difficultyLevel = LogicDifficulty.DEVELOPING,
        logicalStructureId = "false_contradiction_control",
        surfaceVariantId = "reservation_status",
        surfaceForm = SurfaceForm.BUSINESS,
        prompt = "¿El conjunto contiene una contradicción?",
        premises = listOf(
            LogicPremise("p1", "Ninguna reserva sin pago se confirma."),
            LogicPremise("p2", "La reserva R no tiene pago."),
            LogicPremise("p3", "La reserva R está pendiente."),
        ),
        answerOptions = listOf(
            option(
                "a",
                "Sí: p1 y p3 se contradicen.",
                false,
                "Pendiente no significa confirmada.",
                DistractorType.FALSE_CONTRADICTION,
            ),
            option(
                "b",
                "Sí: p2 y p3 se contradicen.",
                false,
                "No pagar y estar pendiente pueden coexistir.",
                DistractorType.FALSE_CONTRADICTION,
            ),
            option("c", "No: el conjunto es consistente.", true, "Controlaste el falso positivo."),
        ),
        correctAnswerId = "c",
        validReasoningRules = listOf(ReasoningRule.CONSISTENCY_CHECK),
        requiredPremiseIds = listOf("p1", "p2", "p3"),
        expectedJustificationClaim = JustificationClaim.SET_IS_CONSISTENT,
        explanation = "Ninguna premisa afirma que R esté confirmada; estar pendiente es compatible.",
        commonErrorTags = listOf(LogicErrorTag.FALSE_CONTRADICTION),
        isIsomorphicProbe = false,
        transferLevel = TransferLevel.TRAINED,
        hints = listOf("Busca dos afirmaciones que no puedan ser verdaderas a la vez."),
    )

    private fun counterexampleItem() = LogicItem(
        id = "logic-e23-chef-01",
        exerciseType = LogicExerciseType.COUNTEREXAMPLE,
        targetSkills = listOf(
            LogicSkill.COUNTEREXAMPLE,
            LogicSkill.PROOF_REFUTATION,
            LogicSkill.CONDITIONAL_REASONING,
        ),
        difficultyLevel = LogicDifficulty.PRACTICED,
        logicalStructureId = "affirming_consequent_counterexample",
        surfaceVariantId = "precise_chef",
        surfaceForm = SurfaceForm.EVERYDAY,
        prompt = "Argumento: todos los chefs del equipo son precisos; Marta es precisa; " +
            "por tanto, Marta es chef. ¿Qué caso lo invalida?",
        premises = listOf(
            LogicPremise("p1", "Todos los chefs del equipo son precisos."),
            LogicPremise("p2", "Marta es precisa."),
        ),
        answerOptions = listOf(
            option(
                "a",
                "Marta es una contable precisa y no es chef.",
                true,
                "Mantiene las premisas verdaderas y hace falsa la conclusión.",
                counterexampleCase = CounterexampleCase(premisesRemainTrue = true, conclusionBecomesFalse = true),
            ),
            option(
                "b",
                "Marta es chef y precisa.",
                false,
                "Es compatible con la conclusión; no la invalida.",
                DistractorType.CONFIRMATION_BIAS,
                CounterexampleCase(premisesRemainTrue = true, conclusionBecomesFalse = false),
            ),
            option(
                "c",
                "Ningún chef es preciso.",
                false,
                "Destruye una premisa en vez de conservarla.",
                DistractorType.MISSING_PREMISE,
                CounterexampleCase(premisesRemainTrue = false, conclusionBecomesFalse = true),
            ),
        ),
        correctAnswerId = "a",
        validReasoningRules = listOf(ReasoningRule.COUNTEREXAMPLE_CONSTRUCTION),
        requiredPremiseIds = listOf("p1", "p2"),
        expectedJustificationClaim = JustificationClaim.COUNTEREXAMPLE_INVALIDATES,
        explanation = "Ser precisa no implica ser chef; puede existir otra persona precisa que no sea chef.",
        commonErrorTags = listOf(LogicErrorTag.AFFIRMING_CONSEQUENT, LogicErrorTag.CONFIRMATION_BIAS),
        isIsomorphicProbe = false,
        transferLevel = TransferLevel.TRAINED,
        hints = listOf("Conserva ambas premisas y busca que la conclusión sea falsa."),
    )

    private fun conditionalIsomorphItem() = LogicItem(
        id = "logic-e3-library-iso-01",
        exerciseType = LogicExerciseType.CONDITIONAL_TEST,
        targetSkills = listOf(
            LogicSkill.CONDITIONAL_REASONING,
            LogicSkill.FUNCTIONAL_TRANSFER,
        ),
        difficultyLevel = LogicDifficulty.DEVELOPING,
        logicalStructureId = CONDITIONAL_REFUTATION_STRUCTURE,
        surfaceVariantId = "library_loan",
        surfaceForm = SurfaceForm.STUDY,
        prompt = "Regla: si un préstamo está vencido, la cuenta queda bloqueada. ¿Qué caso refuta la regla?",
        premises = listOf(
            LogicPremise("p1", "Para refutar la regla debe existir un préstamo vencido sin bloqueo."),
        ),
        answerOptions = listOf(
            option(
                "a",
                "Préstamo vencido y cuenta bloqueada.",
                false,
                "Confirma, pero no refuta.",
                DistractorType.CONFIRMATION_BIAS,
            ),
            option(
                "b",
                "Préstamo vigente y cuenta activa.",
                false,
                "No activa la condición.",
                DistractorType.DENYING_ANTECEDENT,
            ),
            option("c", "Préstamo vencido y cuenta activa.", true, "Misma estructura: P verdadero y Q falso."),
        ),
        correctAnswerId = "c",
        validReasoningRules = listOf(ReasoningRule.CONDITIONAL_FALSIFICATION, ReasoningRule.MODUS_TOLLENS),
        requiredPremiseIds = listOf("p1"),
        expectedJustificationClaim = JustificationClaim.RULE_IS_REFUTED,
        explanation = "El préstamo vencido con cuenta activa contradice el condicional.",
        commonErrorTags = listOf(LogicErrorTag.CONFIRMATION_BIAS, LogicErrorTag.DENYING_ANTECEDENT),
        isIsomorphicProbe = true,
        transferLevel = TransferLevel.T1_ISOMORPHIC,
        hints = listOf("Ignora el tema de biblioteca y representa la regla como P → Q."),
    )

    @Suppress("LongParameterList") // Declarative content factory; each field maps to AnswerOption.
    private fun option(
        id: String,
        text: String,
        correct: Boolean,
        feedback: String,
        distractor: DistractorType? = null,
        counterexampleCase: CounterexampleCase? = null,
    ) = AnswerOption(
        id = id,
        text = text,
        isCorrect = correct,
        distractorType = distractor,
        feedback = feedback,
        counterexampleCase = counterexampleCase,
    )

    private companion object {
        const val CONDITIONAL_REFUTATION_STRUCTURE = "conditional_refutation_p_and_not_q"
    }
}
