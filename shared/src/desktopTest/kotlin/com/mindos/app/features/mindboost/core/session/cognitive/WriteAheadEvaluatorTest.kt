package com.mindos.app.features.mindboost.core.session.cognitive

import com.mindos.app.features.mindboost.core.session.SessionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 3 · The pure write-ahead evaluator: lifecycle evidence → eligibility + distinct anomaly flags.
 * Only a present, id-matching, FINALIZED, time-coherent record yields eligible=true; everything else is
 * fail-closed with its own flag.
 */
class WriteAheadEvaluatorTest {

    private fun evidence(
        present: Boolean = true,
        status: SessionStatus? = SessionStatus.FINALIZED,
        matches: Boolean = true,
        startedAt: Long? = 1_000L,
        finalizedAt: Long? = 5_000L,
        reason: WriteAheadFailureReason? = null,
    ) = WriteAheadEvidence(present, status, matches, startedAt, finalizedAt, reason)

    private fun evaluate(
        e: WriteAheadEvidence,
        runStatus: SessionCompletionStatus = SessionCompletionStatus.COMPLETED,
        runStarted: Long = 1_000L,
        runCompleted: Long? = 5_000L,
    ) = WriteAheadEvaluator.evaluate(e, runStatus, runStarted, runCompleted)

    @Test
    fun `finalized matching coherent is eligible with no flags`() {
        val v = evaluate(evidence())
        assertTrue(v.eligible)
        assertTrue(v.anomalyFlags.isEmpty())
    }

    @Test
    fun `missing record is ineligible with write_ahead_missing`() {
        val v = evaluate(evidence(present = false, status = null, reason = null))
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.MISSING), v.anomalyFlags)
    }

    @Test
    fun `unreadable record is ineligible with write_ahead_unreadable`() {
        val v = evaluate(evidence(present = false, status = null, reason = WriteAheadFailureReason.UNREADABLE))
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.UNREADABLE), v.anomalyFlags)
    }

    @Test
    fun `read failure is ineligible with write_ahead_read_failed and not conflated with missing`() {
        val v = evaluate(evidence(present = false, status = null, reason = WriteAheadFailureReason.READ_FAILED))
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.READ_FAILED), v.anomalyFlags)
    }

    @Test
    fun `abandoned record is ineligible with write_ahead_abandoned`() {
        val v = evaluate(evidence(status = SessionStatus.ABANDONED))
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.ABANDONED), v.anomalyFlags)
    }

    @Test
    fun `started record under a completed run is ineligible with write_ahead_not_finalized`() {
        val v = evaluate(evidence(status = SessionStatus.STARTED), runStatus = SessionCompletionStatus.COMPLETED)
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.NOT_FINALIZED), v.anomalyFlags)
    }

    @Test
    fun `checkpoint record under a completed run is ineligible with write_ahead_not_finalized`() {
        val v = evaluate(evidence(status = SessionStatus.CHECKPOINT), runStatus = SessionCompletionStatus.COMPLETED)
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.NOT_FINALIZED), v.anomalyFlags)
    }

    @Test
    fun `started record under a non-completed run is ineligible without a false-inconsistency flag`() {
        val v = evaluate(evidence(status = SessionStatus.STARTED), runStatus = SessionCompletionStatus.ABANDONED)
        assertFalse(v.eligible)
        assertTrue(v.anomalyFlags.isEmpty())
    }

    @Test
    fun `session id mismatch is ineligible with session_id_mismatch`() {
        val v = evaluate(evidence(matches = false))
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.SESSION_ID_MISMATCH), v.anomalyFlags)
    }

    @Test
    fun `record finalized before it started is incoherent`() {
        val v = evaluate(evidence(startedAt = 5_000L, finalizedAt = 1_000L), runStarted = 5_000L, runCompleted = 6_000L)
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.TIMING_INCOHERENT), v.anomalyFlags)
    }

    @Test
    fun `run completed before the session started is incoherent`() {
        val v = evaluate(
            evidence(startedAt = 10_000L, finalizedAt = 20_000L),
            runStarted = 4_000L,
            runCompleted = 5_000L,
        )
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.TIMING_INCOHERENT), v.anomalyFlags)
    }

    @Test
    fun `run started after the session finalized is incoherent`() {
        val v = evaluate(evidence(startedAt = 1_000L, finalizedAt = 5_000L), runStarted = 6_000L, runCompleted = 7_000L)
        assertFalse(v.eligible)
        assertEquals(listOf(WriteAheadAnomalies.TIMING_INCOHERENT), v.anomalyFlags)
    }

    @Test
    fun `an ineligible write-ahead verdict forces the eligibility policy to deny everything`() {
        val v = evaluate(evidence(present = false, status = null, reason = null))
        val result = SessionEligibilityPolicy.evaluate(
            CognitiveSessionRunFixtures.validInputs(writeAheadPresent = v.eligible),
        ).eligibility
        assertFalse(result.profileEligible)
        assertFalse(result.calibrationEligible)
        assertFalse(result.rankingEligible)
    }

    @Test
    fun `an eligible write-ahead verdict lets an otherwise-clean session pass the policy`() {
        val v = evaluate(evidence())
        val result = SessionEligibilityPolicy.evaluate(
            CognitiveSessionRunFixtures.validInputs(writeAheadPresent = v.eligible),
        ).eligibility
        assertTrue(result.profileEligible)
        assertTrue(result.calibrationEligible)
    }
}
