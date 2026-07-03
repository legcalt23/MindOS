package com.mindos.app.features.mindboost.language.domain.content

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.DependencyLevel
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.ScoringRule
import com.mindos.app.features.mindboost.language.domain.model.StimulusSource
import com.mindos.app.features.mindboost.language.domain.model.TaskPassport
import com.mindos.app.features.mindboost.language.domain.content.ValidationSeverity.BLOCKING
import com.mindos.app.features.mindboost.language.domain.content.ValidationSeverity.WARNING

/**
 * Default, fully-offline [ContentValidator]. Pure function of the passport — no I/O, no
 * platform. Each blocking rule maps to a §8.2 "validación bloqueante"; each warning maps to
 * a §5.6/§8.5 risk that must surface uncertainty but doesn't stop release.
 *
 * Implemented as a set of small `check*` functions that append to one issue list, so the
 * rule set stays readable and individually testable as it grows.
 */
class DefaultContentValidator : ContentValidator {

    override fun validate(passport: TaskPassport): ContentValidationReport {
        val issues = buildList {
            checkModule(passport, this)
            checkTraits(passport, this)
            checkStimulus(passport, this)
            checkScoringConsistency(passport, this)
            checkEvidence(passport, this)
            checkFairness(passport, this)
            checkPrivacy(passport, this)
        }
        return ContentValidationReport(passport.taskId, issues)
    }

    private fun checkModule(p: TaskPassport, out: MutableList<ContentValidationIssue>) {
        if (p.moduleId.isBlank()) {
            out += ContentValidationIssue(
                code = "MODULE_ID_BLANK",
                field = "moduleId",
                message = "Declara el módulo al que pertenece la tarea (p. ej. \"M6\").",
                severity = BLOCKING,
            )
        }
    }

    private fun checkTraits(p: TaskPassport, out: MutableList<ContentValidationIssue>) {
        if (p.primaryTrait in p.secondaryTraits) {
            out += ContentValidationIssue(
                code = "PRIMARY_TRAIT_DUPLICATED",
                field = "secondaryTraits",
                message = "El constructo primario (${p.primaryTrait.code}) no debe repetirse en los secundarios.",
                severity = BLOCKING,
            )
        }
    }

    private fun checkStimulus(p: TaskPassport, out: MutableList<ContentValidationIssue>) {
        if (p.stimulus.words <= 0) {
            out += ContentValidationIssue(
                code = "STIMULUS_WORDS_NON_POSITIVE",
                field = "stimulus.words",
                message = "El estímulo debe declarar un número de palabras positivo (era ${p.stimulus.words}).",
                severity = BLOCKING,
            )
        }
        if (p.stimulus.domain.isBlank()) {
            out += ContentValidationIssue(
                code = "STIMULUS_DOMAIN_BLANK",
                field = "stimulus.domain",
                message = "Declara el dominio del estímulo para controlar confusores temáticos.",
                severity = BLOCKING,
            )
        }
        if (p.stimulus.priorKnowledgeDependency == DependencyLevel.HIGH) {
            out += ContentValidationIssue(
                code = "HIGH_PRIOR_KNOWLEDGE",
                field = "stimulus.priorKnowledgeDependency",
                message = "Alta dependencia de conocimiento previo: las medidas deben marcar riesgo de confusor.",
                severity = WARNING,
            )
        }
    }

    private fun checkScoringConsistency(p: TaskPassport, out: MutableList<ContentValidationIssue>) {
        val closedMode = p.responseMode.isClosed
        val deterministic = p.scoring.scoringRule.isDeterministic
        if (closedMode && !deterministic) {
            out += ContentValidationIssue(
                code = "CLOSED_MODE_NEEDS_DETERMINISTIC_RULE",
                field = "scoring.scoringRule",
                message = "Una respuesta cerrada (${p.responseMode}) exige una regla de scoring determinista.",
                severity = BLOCKING,
            )
        }
        if (!closedMode && deterministic) {
            out += ContentValidationIssue(
                code = "OPEN_MODE_NEEDS_RUBRIC_RULE",
                field = "scoring.scoringRule",
                message = "Una respuesta abierta (${p.responseMode}) no puede puntuarse con una " +
                    "clave determinista; usa rúbrica.",
                severity = BLOCKING,
            )
        }
        val evidenceRule = p.scoring.scoringRule == ScoringRule.DETERMINISTIC_KEY_PLUS_EVIDENCE
        val evidenceMode = p.responseMode == ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION
        if (evidenceRule != evidenceMode) {
            out += ContentValidationIssue(
                code = "EVIDENCE_RULE_MODE_MISMATCH",
                field = "scoring.scoringRule",
                message = "La regla con selección de evidencia y el modo de respuesta con " +
                    "evidencia deben usarse juntos.",
                severity = BLOCKING,
            )
        }
    }

    private fun checkEvidence(p: TaskPassport, out: MutableList<ContentValidationIssue>) {
        if (p.evidence.eligibleStates.isEmpty()) {
            out += ContentValidationIssue(
                code = "NO_ELIGIBLE_EVIDENCE_STATES",
                field = "evidence.eligibleStates",
                message = "Declara al menos un estado de evidencia elegible (E1..E5).",
                severity = BLOCKING,
            )
        }
        // Evaluation items must not double as training (§8.3): an evaluation bank carrying a
        // training repair plan mixes measurement with practice and contaminates evidence.
        if (p.evidence.bankType == BankType.EVALUATION && p.feedback.repairPlanId != null) {
            out += ContentValidationIssue(
                code = "EVALUATION_ITEM_HAS_REPAIR_PLAN",
                field = "feedback.repairPlanId",
                message = "Un ítem del banco de evaluación no debe llevar plan de reparación de entrenamiento.",
                severity = BLOCKING,
            )
        }
    }

    private fun checkFairness(p: TaskPassport, out: MutableList<ContentValidationIssue>) {
        if (p.fairness.difStatus.isBlank()) {
            out += ContentValidationIssue(
                code = "DIF_STATUS_BLANK",
                field = "fairness.difStatus",
                message = "Declara el estado de DIF (p. ej. \"pending_pilot\").",
                severity = BLOCKING,
            )
        }
        if (p.fairness.variantsValidated.isEmpty()) {
            out += ContentValidationIssue(
                code = "NO_VALIDATED_VARIANTS",
                field = "fairness.variantsValidated",
                message = "Sin variantes validadas: la UI debe mostrar copy de incertidumbre por variante.",
                severity = WARNING,
            )
        }
    }

    private fun checkPrivacy(p: TaskPassport, out: MutableList<ContentValidationIssue>) {
        // User-provided material can never be classified as non-sensitive original content.
        if (p.stimulus.source == StimulusSource.USER_PROVIDED && !p.privacy.privacyClass.isUserContent) {
            out += ContentValidationIssue(
                code = "USER_MATERIAL_MISCLASSIFIED",
                field = "privacy.privacyClass",
                message = "El material aportado por el usuario debe clasificarse como contenido " +
                    "de usuario, no como original no sensible.",
                severity = BLOCKING,
            )
        }
    }
}
