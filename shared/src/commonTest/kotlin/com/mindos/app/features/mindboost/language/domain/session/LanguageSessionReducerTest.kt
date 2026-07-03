package com.mindos.app.features.mindboost.language.domain.session

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the pure session reducer (§12.4): the request→feedback→next loop, repair branching,
 * completion, and total handling of invalid actions (NoOp, no crash, no state corruption).
 */
class LanguageSessionReducerTest {

    private val plan = SessionPlan(
        listOf(SessionPhase.CALIBRATION, SessionPhase.TRAINING_BASE, SessionPhase.NEAR_TRANSFER),
    )
    private val initial = LanguageSessionState.notStarted(LanguageSessionId("s1"), plan)

    private fun reduce(state: LanguageSessionState, action: LanguageSessionAction) =
        LanguageSessionReducer.reduce(state, action)

    @Test
    fun startPresentsFirstTask() {
        val t = reduce(initial, LanguageSessionAction.Start(plan))
        assertEquals(SessionStatus.AWAITING_RESPONSE, t.state.status)
        assertEquals(SessionPhase.CALIBRATION, t.state.activePhase)
        val decision = assertIs<TrainingDecision.PresentTask>(t.decision)
        assertEquals(0, decision.stepIndex)
        assertEquals(SessionPhase.CALIBRATION, decision.phase)
    }

    @Test
    fun startIsIgnoredOnceRunning() {
        val running = reduce(initial, LanguageSessionAction.Start(plan)).state
        val t = reduce(running, LanguageSessionAction.Start(plan))
        assertIs<TrainingDecision.NoOp>(t.decision)
        assertSame(running, t.state)
    }

    @Test
    fun correctResponseShowsFeedbackWithoutRepair() {
        val running = reduce(initial, LanguageSessionAction.Start(plan)).state
        val t = reduce(running, LanguageSessionAction.SubmitResponse(ResponseOutcome(isCorrect = true)))
        val feedback = assertIs<TrainingDecision.ShowFeedback>(t.decision)
        assertFalse(feedback.repairTriggered)
        assertFalse(t.state.repairPending)
        assertEquals(SessionStatus.AWAITING_FEEDBACK_ACK, t.state.status)
    }

    @Test
    fun errorInRepairablePhaseTriggersRepair() {
        // Advance to TRAINING_BASE (a repairable phase) first.
        var state = reduce(initial, LanguageSessionAction.Start(plan)).state
        state = reduce(state, LanguageSessionAction.SubmitResponse(ResponseOutcome(isCorrect = true))).state
        state = reduce(state, LanguageSessionAction.FeedbackAcknowledged).state
        state = reduce(state, LanguageSessionAction.RequestNext).state
        assertEquals(SessionPhase.TRAINING_BASE, state.activePhase)

        val t = reduce(
            state,
            LanguageSessionAction.SubmitResponse(
                ResponseOutcome(isCorrect = false, errorTypes = listOf(ErrorType.INF_OVER)),
            ),
        )
        val feedback = assertIs<TrainingDecision.ShowFeedback>(t.decision)
        assertTrue(feedback.repairTriggered)
        assertTrue(t.state.repairPending)
    }

    @Test
    fun errorInNonRepairablePhaseDoesNotTriggerRepair() {
        val running = reduce(initial, LanguageSessionAction.Start(plan)).state // CALIBRATION
        val t = reduce(
            running,
            LanguageSessionAction.SubmitResponse(
                ResponseOutcome(isCorrect = false, errorTypes = listOf(ErrorType.INF_OVER)),
            ),
        )
        val feedback = assertIs<TrainingDecision.ShowFeedback>(t.decision)
        assertFalse(feedback.repairTriggered)
    }

    @Test
    fun pendingRepairPresentsIsomorphicRetryBeforeAdvancing() {
        var state = reduce(initial, LanguageSessionAction.Start(plan)).state
        state = reduce(state, LanguageSessionAction.SubmitResponse(ResponseOutcome(true))).state
        state = reduce(state, LanguageSessionAction.FeedbackAcknowledged).state
        state = reduce(state, LanguageSessionAction.RequestNext).state // TRAINING_BASE, index 1
        state = reduce(
            state,
            LanguageSessionAction.SubmitResponse(
                ResponseOutcome(isCorrect = false, errorTypes = listOf(ErrorType.INF_OVER)),
            ),
        ).state
        state = reduce(state, LanguageSessionAction.FeedbackAcknowledged).state

        val t = reduce(state, LanguageSessionAction.RequestNext)
        val decision = assertIs<TrainingDecision.PresentTask>(t.decision)
        assertEquals(SessionPhase.REPAIR_MODE, decision.phase)
        // Repair retries the SAME plan step; it does not advance the index.
        assertEquals(1, decision.stepIndex)
        assertEquals(SessionPhase.REPAIR_MODE, t.state.activePhase)
        assertFalse(t.state.repairPending)
    }

    @Test
    fun requestNextAdvancesThroughThePlanThenCompletes() {
        var state = reduce(initial, LanguageSessionAction.Start(plan)).state
        // Walk all steps with correct answers; the last RequestNext completes the session.
        for (step in 0 until plan.size) {
            state = reduce(state, LanguageSessionAction.SubmitResponse(ResponseOutcome(true))).state
            state = reduce(state, LanguageSessionAction.FeedbackAcknowledged).state
            val t = reduce(state, LanguageSessionAction.RequestNext)
            state = t.state
            if (step == plan.size - 1) {
                assertIs<TrainingDecision.CompleteSession>(t.decision)
            } else {
                assertEquals(step + 1, assertIs<TrainingDecision.PresentTask>(t.decision).stepIndex)
            }
        }
        assertEquals(SessionStatus.COMPLETED, state.status)
    }

    @Test
    fun endCompletesFromMidSession() {
        val running = reduce(initial, LanguageSessionAction.Start(plan)).state
        val t = reduce(running, LanguageSessionAction.End)
        assertIs<TrainingDecision.CompleteSession>(t.decision)
        assertEquals(SessionStatus.COMPLETED, t.state.status)
    }

    @Test
    fun submitBeforeStartIsIgnored() {
        val t = reduce(initial, LanguageSessionAction.SubmitResponse(ResponseOutcome(true)))
        assertIs<TrainingDecision.NoOp>(t.decision)
        assertSame(initial, t.state)
    }
}
