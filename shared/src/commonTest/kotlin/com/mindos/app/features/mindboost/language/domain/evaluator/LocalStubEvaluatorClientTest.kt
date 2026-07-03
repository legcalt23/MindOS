package com.mindos.app.features.mindboost.language.domain.evaluator

import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.RetentionPolicy
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Pins the §9.7 offline degradation contract: the local stub gives formative feedback only —
 * it NEVER certifies transfer evidence, routes E2+ targets to human review, and never retains
 * content without explicit cloud consent.
 */
class LocalStubEvaluatorClientTest {

    private val client = LocalStubEvaluatorClient()

    private fun request(
        target: EvidenceState,
        consent: Boolean = false,
        local: Int? = null,
    ) = EvaluationRequest(
        taskId = TaskId("t1"),
        rubricId = "rub_memo_v2",
        trait = TraitCode.SUM,
        operation = OperationType.SUMMARIZE,
        responseRef = "local://opaque-ref",
        targetState = target,
        cloudConsentGranted = consent,
        localHeuristicScore = local,
    )

    @Test
    fun neverCertifiesEvidenceEvenWithAHighLocalScore() = runTest {
        val out = client.evaluate(request(EvidenceState.E3_FUNCTIONAL_TRANSFER, local = 100))
        assertFalse(out.evidenceEligible)
        assertFalse(out.rubricScore.evidenceEligible)
        assertEquals(EvaluatorMode.LOCAL, out.mode)
    }

    @Test
    fun nearTransferOrAboveIsRoutedToHumanReview() = runTest {
        assertEquals(
            HumanReviewStatus.PENDING,
            client.evaluate(request(EvidenceState.E2_NEAR_TRANSFER)).humanReviewStatus,
        )
        assertEquals(
            HumanReviewStatus.NOT_REQUIRED,
            client.evaluate(request(EvidenceState.E1_TRAINED_IMPROVEMENT)).humanReviewStatus,
        )
    }

    @Test
    fun retentionDefaultsToNoneWithoutConsent() = runTest {
        assertEquals(
            RetentionPolicy.NONE,
            client.evaluate(request(EvidenceState.E1_TRAINED_IMPROVEMENT, consent = false)).retention,
        )
        assertEquals(
            RetentionPolicy.ABSTRACTION_ONLY,
            client.evaluate(request(EvidenceState.E1_TRAINED_IMPROVEMENT, consent = true)).retention,
        )
    }

    @Test
    fun surfacesLocalHeuristicScoreAndDefaultsWhenAbsent() = runTest {
        assertEquals(
            82,
            client.evaluate(request(EvidenceState.E1_TRAINED_IMPROVEMENT, local = 82)).rubricScore.score0to100,
        )
        assertEquals(
            LocalStubEvaluatorClient.NEUTRAL_SCORE,
            client.evaluate(request(EvidenceState.E1_TRAINED_IMPROVEMENT, local = null)).rubricScore.score0to100,
        )
    }

    @Test
    fun biasSentinelIsNotFlaggedByTheStub() = runTest {
        assertFalse(client.evaluate(request(EvidenceState.E1_TRAINED_IMPROVEMENT)).biasSentinel.flagged)
    }
}
