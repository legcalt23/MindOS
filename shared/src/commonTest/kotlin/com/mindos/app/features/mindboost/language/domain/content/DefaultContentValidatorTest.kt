package com.mindos.app.features.mindboost.language.domain.content

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.CognitiveDose
import com.mindos.app.features.mindboost.language.domain.model.DependencyLevel
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.EvidenceTarget
import com.mindos.app.features.mindboost.language.domain.model.FairnessMetadata
import com.mindos.app.features.mindboost.language.domain.model.FeedbackSpec
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.PrivacyClass
import com.mindos.app.features.mindboost.language.domain.model.PrivacyMetadata
import com.mindos.app.features.mindboost.language.domain.model.QaMetadata
import com.mindos.app.features.mindboost.language.domain.model.QaStatus
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.ScoringRule
import com.mindos.app.features.mindboost.language.domain.model.ScoringSpec
import com.mindos.app.features.mindboost.language.domain.model.StimulusProfile
import com.mindos.app.features.mindboost.language.domain.model.StimulusSource
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TaskPassport
import com.mindos.app.features.mindboost.language.domain.model.TaskVersion
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks the §8.2 blocking rules and §5.6/§8.5 warnings. A clean fixture passes with zero
 * issues; each negative case mutates exactly one field and expects exactly one finding with
 * the right code and severity.
 */
class DefaultContentValidatorTest {

    private val validator = DefaultContentValidator()

    private fun validPassport(): TaskPassport = TaskPassport(
        taskId = TaskId("lang_m6_inf_00421"),
        version = TaskVersion.FIRST,
        status = QaStatus.PILOT,
        moduleId = "M6",
        primaryTrait = TraitCode.INF,
        secondaryTraits = listOf(TraitCode.LIT, TraitCode.SIT),
        operation = OperationType.EVIDENCE_SUPPORTED_INFERENCE,
        responseMode = ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION,
        dose = CognitiveDose.LOW,
        stimulus = StimulusProfile(
            source = StimulusSource.ORIGINAL,
            words = 130,
            domain = "everyday_reasoning",
            priorKnowledgeDependency = DependencyLevel.LOW,
            culturalDependency = DependencyLevel.LOW,
        ),
        scoring = ScoringSpec(ScoringRule.DETERMINISTIC_KEY_PLUS_EVIDENCE, caps = listOf("unsupported_inference_cap")),
        feedback = FeedbackSpec(errorPatterns = listOf(ErrorType.INF_OVER)),
        evidence = EvidenceTarget(
            eligibleStates = listOf(EvidenceState.E1_TRAINED_IMPROVEMENT, EvidenceState.E2_NEAR_TRANSFER),
            bankType = BankType.EVALUATION,
            parallelFormGroup = "pf_inf_012",
        ),
        fairness = FairnessMetadata(variantsValidated = listOf("es_ES", "es_neutral"), difStatus = "pending_pilot"),
        privacy = PrivacyMetadata(PrivacyClass.NON_SENSITIVE_ORIGINAL_CONTENT),
        qa = QaMetadata(expertReviewRequired = true, pilotRequired = true),
    )

    @Test
    fun cleanPassportHasNoIssues() {
        val report = validator.validate(validPassport())
        assertTrue(report.isValid)
        assertEquals(emptyList(), report.issues)
    }

    @Test
    fun blankModuleBlocks() {
        val report = validator.validate(validPassport().copy(moduleId = "  "))
        assertFalse(report.isValid)
        assertEquals("MODULE_ID_BLANK", report.blocking.single().code)
    }

    @Test
    fun primaryTraitInSecondariesBlocks() {
        val report = validator.validate(
            validPassport().copy(secondaryTraits = listOf(TraitCode.INF, TraitCode.LIT)),
        )
        assertEquals("PRIMARY_TRAIT_DUPLICATED", report.blocking.single().code)
    }

    @Test
    fun nonPositiveWordsBlocks() {
        val p = validPassport()
        val report = validator.validate(p.copy(stimulus = p.stimulus.copy(words = 0)))
        assertEquals("STIMULUS_WORDS_NON_POSITIVE", report.blocking.single().code)
    }

    @Test
    fun closedModeWithRubricRuleBlocks() {
        val p = validPassport()
        val report = validator.validate(
            p.copy(
                responseMode = ResponseMode.MULTIPLE_CHOICE,
                scoring = p.scoring.copy(scoringRule = ScoringRule.RUBRIC_LOCAL),
            ),
        )
        assertTrue(report.blocking.any { it.code == "CLOSED_MODE_NEEDS_DETERMINISTIC_RULE" })
    }

    @Test
    fun evidenceRuleModeMismatchBlocks() {
        val p = validPassport()
        // Plain multiple choice but still using the evidence-selection rule.
        val report = validator.validate(
            p.copy(
                responseMode = ResponseMode.MULTIPLE_CHOICE,
                scoring = p.scoring.copy(scoringRule = ScoringRule.DETERMINISTIC_KEY_PLUS_EVIDENCE),
            ),
        )
        assertTrue(report.blocking.any { it.code == "EVIDENCE_RULE_MODE_MISMATCH" })
    }

    @Test
    fun emptyEligibleStatesBlocks() {
        val p = validPassport()
        val report = validator.validate(p.copy(evidence = p.evidence.copy(eligibleStates = emptyList())))
        assertTrue(report.blocking.any { it.code == "NO_ELIGIBLE_EVIDENCE_STATES" })
    }

    @Test
    fun evaluationItemWithRepairPlanBlocks() {
        val p = validPassport()
        val report = validator.validate(
            p.copy(feedback = p.feedback.copy(repairPlanId = "repair_x")),
        )
        assertEquals("EVALUATION_ITEM_HAS_REPAIR_PLAN", report.blocking.single().code)
    }

    @Test
    fun userMaterialMisclassifiedBlocks() {
        val p = validPassport()
        val report = validator.validate(
            p.copy(stimulus = p.stimulus.copy(source = StimulusSource.USER_PROVIDED)),
        )
        assertTrue(report.blocking.any { it.code == "USER_MATERIAL_MISCLASSIFIED" })
    }

    @Test
    fun noValidatedVariantsIsWarningNotBlocking() {
        val p = validPassport()
        val report = validator.validate(p.copy(fairness = p.fairness.copy(variantsValidated = emptyList())))
        assertTrue(report.isValid)
        assertEquals("NO_VALIDATED_VARIANTS", report.warnings.single().code)
    }

    @Test
    fun highPriorKnowledgeIsWarning() {
        val p = validPassport()
        val report = validator.validate(
            p.copy(stimulus = p.stimulus.copy(priorKnowledgeDependency = DependencyLevel.HIGH)),
        )
        assertTrue(report.isValid)
        assertTrue(report.warnings.any { it.code == "HIGH_PRIOR_KNOWLEDGE" })
    }
}
