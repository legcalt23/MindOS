package com.mindos.app.features.mindboost.language.domain.scoring

import com.mindos.app.features.mindboost.language.domain.model.AttemptFlag
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ConfounderAssessment
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceGate
import com.mindos.app.features.mindboost.language.domain.model.FatigueState
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the closed scoring spine: deterministic correctness for the three closed modes, the
 * §5.6 gate logic (any flag → no strong update, weight 0), bank-separation down-weighting,
 * and the unsupported-inference cap on evidence selection.
 */
class ClosedScoringEngineTest {

    private val engine = ClosedScoringEngine()
    private val cleanEval = ScoringContext(bankType = BankType.EVALUATION) // no confounders, fresh

    @Test
    fun singleChoiceCorrectScoresFullAndAllowsUpdate() {
        val key = AnswerKey.SingleChoice(correctOptionId = "b")
        val result = engine.score(key, ResponsePayload.MultipleChoice("b"), ScoringTiming(), cleanEval)
        assertEquals(Proportion.ONE, result.correctness)
        assertTrue(result.errorTypes.isEmpty())
        assertTrue(result.isStrongUpdateAllowed)
        assertEquals(1.0, result.evidenceWeight)
    }

    @Test
    fun singleChoiceWrongSurfacesDiagnosticError() {
        val key = AnswerKey.SingleChoice(
            correctOptionId = "b",
            distractorErrors = mapOf("a" to ErrorType.INF_OVER),
        )
        val result = engine.score(key, ResponsePayload.MultipleChoice("a"), ScoringTiming(), cleanEval)
        assertEquals(Proportion.ZERO, result.correctness)
        assertEquals(listOf(ErrorType.INF_OVER), result.errorTypes)
        // A wrong-but-clean answer is still informative: weight is non-zero (evidence != correctness).
        assertTrue(result.isStrongUpdateAllowed)
    }

    @Test
    fun evidenceSelectionFullySupportedScoresFull() {
        val key = AnswerKey.EvidenceSelection("b", requiredEvidenceIds = setOf("s1", "s2"))
        val response = ResponsePayload.EvidenceSelection("b", listOf("s1", "s2"))
        val result = engine.score(key, response, ScoringTiming(), cleanEval)
        assertEquals(Proportion.ONE, result.correctness)
        assertEquals(Proportion.ONE, result.evidenceContribution)
        assertTrue(result.flags.isEmpty())
    }

    @Test
    fun evidenceSelectionRightOptionButUnsupportedIsCappedAndGated() {
        val key = AnswerKey.EvidenceSelection("b", requiredEvidenceIds = setOf("s1", "s2"))
        val response = ResponsePayload.EvidenceSelection("b", listOf("s1")) // missing s2
        val result = engine.score(key, response, ScoringTiming(), cleanEval)
        assertEquals(0.5, result.correctness.value)
        assertEquals(0.5, result.evidenceContribution.value)
        assertTrue(ErrorType.INF_OVER in result.errorTypes)
        assertTrue(AttemptFlag.UNSUPPORTED_INFERENCE in result.flags)
        assertFalse(result.isStrongUpdateAllowed)
        assertEquals(0.0, result.evidenceWeight)
    }

    @Test
    fun orderingExactScoresFullAndPartialScoresBetween() {
        val key = AnswerKey.Ordering(listOf("a", "b", "c"))
        val exact = engine.score(key, ResponsePayload.Ordering(listOf("a", "b", "c")), ScoringTiming(), cleanEval)
        assertEquals(Proportion.ONE, exact.correctness)

        val partial = engine.score(key, ResponsePayload.Ordering(listOf("a", "c", "b")), ScoringTiming(), cleanEval)
        assertTrue(partial.correctness.value in 0.0..1.0)
        assertTrue(partial.correctness.value < 1.0)
        assertTrue(partial.correctness.value > 0.0)
    }

    @Test
    fun orderingWithWrongItemSetScoresZero() {
        val key = AnswerKey.Ordering(listOf("a", "b", "c"))
        val result = engine.score(key, ResponsePayload.Ordering(listOf("a", "b")), ScoringTiming(), cleanEval)
        assertEquals(Proportion.ZERO, result.correctness)
    }

    @Test
    fun highFatigueBlocksStrongUpdate() {
        val key = AnswerKey.SingleChoice("b")
        val context = cleanEval.copy(fatigue = FatigueState(Proportion(0.9)))
        val result = engine.score(key, ResponsePayload.MultipleChoice("b"), ScoringTiming(), context)
        assertTrue(AttemptFlag.HIGH_FATIGUE in result.flags)
        assertTrue(EvidenceGate.FATIGUE in result.gatesApplied)
        assertFalse(result.isStrongUpdateAllowed)
        assertEquals(0.0, result.evidenceWeight)
    }

    @Test
    fun repeatedItemBlocksStrongUpdateViaNoveltyGate() {
        val key = AnswerKey.SingleChoice("b")
        val context = cleanEval.copy(isRepeatedItem = true)
        val result = engine.score(key, ResponsePayload.MultipleChoice("b"), ScoringTiming(), context)
        assertTrue(AttemptFlag.REPEATED_ITEM in result.flags)
        assertTrue(EvidenceGate.NOVELTY in result.gatesApplied)
        assertFalse(result.isStrongUpdateAllowed)
    }

    @Test
    fun confoundersBlockStrongUpdate() {
        val key = AnswerKey.SingleChoice("b")
        val context = cleanEval.copy(
            confounders = ConfounderAssessment(variantBiasRisk = true),
        )
        val result = engine.score(key, ResponsePayload.MultipleChoice("b"), ScoringTiming(), context)
        assertTrue(AttemptFlag.VARIANT_BIAS_RISK in result.flags)
        assertTrue(EvidenceGate.FAIRNESS in result.gatesApplied)
        assertFalse(result.isStrongUpdateAllowed)
    }

    @Test
    fun trainingBankIsDownWeightedButStillUpdatable() {
        val key = AnswerKey.SingleChoice("b")
        val context = ScoringContext(bankType = BankType.TRAINING)
        val result = engine.score(key, ResponsePayload.MultipleChoice("b"), ScoringTiming(), context)
        assertTrue(EvidenceGate.BANK_SEPARATION in result.gatesApplied)
        assertTrue(result.isStrongUpdateAllowed)
        assertEquals(ClosedScoringEngine.TRAINING_BANK_FACTOR, result.evidenceWeight) // 1.0 * 1.0 * 0.5
    }
}
