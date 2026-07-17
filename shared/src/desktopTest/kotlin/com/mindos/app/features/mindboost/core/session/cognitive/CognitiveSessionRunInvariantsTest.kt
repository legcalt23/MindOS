package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.scoring.ScoreState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 1 · Invariants of the sealed [CognitiveSessionRun] and its value types. Every impossible
 * state is rejected at construction (fail-closed), so a dishonest or malformed run can never be sealed.
 */
class CognitiveSessionRunInvariantsTest {

    @Test
    fun `a valid run builds`() {
        val run = CognitiveSessionRunFixtures.validRun()
        assertEquals("sess_0123456789abcdef", run.sessionId)
        assertEquals(SessionCompletionStatus.COMPLETED, run.completionStatus)
    }

    // ---- run-level timestamp / lifecycle invariants (P1-9) ----

    @Test
    fun `startedAt must be a real epoch`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validRun(startedAtEpochMs = 0L)
        }
    }

    @Test
    fun `a terminal status requires completedAt`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validRun(
                completionStatus = SessionCompletionStatus.COMPLETED,
                completedAtEpochMs = null,
            )
        }
    }

    @Test
    fun `INVALID_LIFECYCLE may have a null completedAt`() {
        val run = CognitiveSessionRunFixtures.validRun(
            completionStatus = SessionCompletionStatus.INVALID_LIFECYCLE,
            completedAtEpochMs = null,
            anomalyFlags = listOf("recovery_corrupt"),
        )
        assertEquals(SessionCompletionStatus.INVALID_LIFECYCLE, run.completionStatus)
    }

    @Test
    fun `completedAt before startedAt is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validRun(startedAtEpochMs = 10_000L, completedAtEpochMs = 9_999L)
        }
    }

    @Test
    fun `FINALIZATION_FAILED requires a reason`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validRun(
                completionStatus = SessionCompletionStatus.FINALIZATION_FAILED,
                completedAtEpochMs = 121_000L,
                finalizationFailure = null,
            )
        }
    }

    @Test
    fun `a non-failed status must not carry a finalization reason`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validRun(
                completionStatus = SessionCompletionStatus.COMPLETED,
                finalizationFailure = FinalizationFailureReason.SAVE_FAILED,
            )
        }
    }

    @Test
    fun `negative durationActual is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validRun(durationActualMs = -1L)
        }
    }

    // ---- TrialAggregate ----

    @Test
    fun `completed above planned is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            TrialAggregate(planned = 10, completed = 11, outcomes = listOf(OutcomeCount(TrialOutcomeKind.OMISSION, 11)))
        }
    }

    @Test
    fun `outcomes must sum to completed`() {
        assertFailsWith<IllegalArgumentException> {
            TrialAggregate(
                planned = 10,
                completed = 10,
                outcomes = listOf(OutcomeCount(TrialOutcomeKind.ACTIVE_CORRECT, 9)),
            )
        }
    }

    @Test
    fun `outcomes must not repeat a kind`() {
        assertFailsWith<IllegalArgumentException> {
            TrialAggregate(
                planned = 4,
                completed = 4,
                outcomes = listOf(
                    OutcomeCount(TrialOutcomeKind.ACTIVE_CORRECT, 2),
                    OutcomeCount(TrialOutcomeKind.ACTIVE_CORRECT, 2),
                ),
            )
        }
    }

    @Test
    fun `a negative outcome count is rejected`() {
        assertFailsWith<IllegalArgumentException> { OutcomeCount(TrialOutcomeKind.COMMISSION, -1) }
    }

    // ---- AnswerSummary ----

    @Test
    fun `answer parts must equal total`() {
        assertFailsWith<IllegalArgumentException> {
            AnswerSummary(total = 10, correct = 5, incorrect = 2, omissions = 2)
        }
    }

    @Test
    fun `negative answer counts are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AnswerSummary(total = 1, correct = -1, incorrect = 1, omissions = 1)
        }
    }

    // ---- CognitiveSessionMetrics (P0-6 units) ----

    @Test
    fun `accuracy above one is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validMetrics().copy(accuracy = SessionMetric.Measured(1.5))
        }
    }

    @Test
    fun `a negative reaction time is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validMetrics().copy(reactionTimeMedianMs = SessionMetric.Measured(-1.0))
        }
    }

    @Test
    fun `a negative variability is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validMetrics().copy(reactionTimeVariabilityCv = SessionMetric.Measured(-0.1))
        }
    }

    @Test
    fun `a non-finite measured value is rejected`() {
        assertFailsWith<IllegalArgumentException> { SessionMetric.Measured(Double.NaN) }
    }

    @Test
    fun `unavailable is distinct from a measured zero`() {
        val unavailable: SessionMetric = SessionMetric.Unavailable(MetricUnavailableReason.INSUFFICIENT_SAMPLE)
        val measuredZero: SessionMetric = SessionMetric.Measured(0.0)
        assertTrue(unavailable is SessionMetric.Unavailable)
        assertTrue(measuredZero is SessionMetric.Measured)
        assertTrue(unavailable != measuredZero)
    }

    // ---- CognitiveSessionScore ----

    @Test
    fun `normalized above one is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validScore().copy(normalized = 1.1)
        }
    }

    @Test
    fun `a VALID score must carry a normalized value`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionScore(
                normalized = null,
                category = ScoreState.VALID,
                scoringVersion = "1.0.0",
                preliminary = false,
                lowConfidence = false,
            )
        }
    }

    @Test
    fun `a CALIBRATING score may carry a null normalized`() {
        val score = CognitiveSessionScore(
            normalized = null,
            category = ScoreState.CALIBRATING,
            scoringVersion = "1.0.0",
            preliminary = true,
            lowConfidence = true,
        )
        assertEquals(ScoreState.CALIBRATING, score.category)
    }

    @Test
    fun `a blank scoring version is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.validScore().copy(scoringVersion = " ")
        }
    }

    // ---- TimingIntegritySnapshot + ExerciseSpecificMetricPayload ----

    @Test
    fun `negative timing counters are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveSessionRunFixtures.cleanTiming().copy(pauseResumeCount = -1)
        }
    }

    @Test
    fun `the UNKNOWN timing snapshot never claims CLEAN`() {
        assertEquals(TimingIntegrityStatus.UNKNOWN, TimingIntegritySnapshot.UNKNOWN.integrityStatus)
    }

    @Test
    fun `a blank payload type is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSpecificMetricPayload(type = " ", schemaVersion = 1, payload = CognitiveSessionRunFixtures.sampleJson())
        }
    }

    @Test
    fun `a payload schema version below one is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSpecificMetricPayload(
                type = "x",
                schemaVersion = 0,
                payload = CognitiveSessionRunFixtures.sampleJson(),
            )
        }
    }
}
