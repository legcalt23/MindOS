@file:Suppress("MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.speed.data.persistence.InMemoryExerciseTelemetryStore
import com.mindos.app.features.mindboost.speed.data.persistence.goNoGoSummaryForSession
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoConfig
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoCategory
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoFeedbackKind
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoSignal
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTrialType
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** MC9-D · E8 — the event-driven session state machine: honest per-trial resolution, scoring, and persistence. */
class GoNoGoSessionStateHolderTest {

    private val fixedClock = EpochClock { 1_000L }

    // scheduleBlock 8 + minNoGo 2, trialCount 16 ⇒ deterministic counts: exactly 12 GO + 4 NO-GO.
    private fun holder(scope: kotlinx.coroutines.CoroutineScope, store: InMemoryExerciseTelemetryStore) =
        GoNoGoSessionStateHolder(
            scope = scope,
            userId = "u1",
            telemetryStore = store,
            config = GoNoGoConfig(),
            clock = fixedClock,
            trialCount = 16,
        )

    /** Drives one trial: reveal the stimulus, then respond per [respond] (true = tap), then advance. */
    private fun playTrial(h: GoNoGoSessionStateHolder, respond: (GoNoGoTrialType) -> Boolean) {
        h.onStimulusShown()
        val type = h.state.value.currentStimulus!!.trialType
        if (respond(type)) h.onStimulusTap(300L) else h.onResponseWindowElapsed()
        h.beginNextTrial()
    }

    @Test
    fun a_perfectly_played_run_finishes_with_strong_control_and_persists() = runTest {
        val store = InMemoryExerciseTelemetryStore()
        val h = holder(this, store)
        h.startSession()
        // Respond to GO, withhold on NO-GO: every trial correct.
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()

        val state = h.state.value
        assertEquals(GoNoGoPhase.Finished, state.phase)
        val score = assertNotNull(state.score)
        assertTrue(score.index != null && score.index!! > 0.9, "perfect play ⇒ high index: ${score.index}")
        assertEquals(GoNoGoCategory.STRONG_CONTROL, score.category)
        assertEquals(GoNoGoFeedbackKind.STRONG_CONTROL_GOOD_SPEED, state.feedback)
        assertTrue(state.summaryPersisted)
        val persisted = store.goNoGoSummaryForSession("u1", assertNotNull(state.sessionId))
        assertNotNull(persisted, "the run summary is persisted historically")
        assertEquals(16, persisted.totalTrials)
        coroutineContext.cancelChildren()
    }

    @Test
    fun tapping_every_trial_scores_low_and_flags_commissions() = runTest {
        val store = InMemoryExerciseTelemetryStore()
        val h = holder(this, store)
        h.startSession()
        repeat(16) { playTrial(h) { true } } // tap-all: GO correct, but every NO-GO is a commission
        advanceUntilIdle()

        val score = assertNotNull(h.state.value.score)
        assertTrue(score.index != null && score.index!! < 0.35, "tap-all must score low: ${score.index}")
        assertEquals(GoNoGoCategory.IMPULSIVE_COMMISSION, score.category)
        assertEquals(GoNoGoFeedbackKind.HIGH_COMMISSION, h.state.value.feedback)
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_foreperiod_tap_resolves_the_trial_as_an_anticipation() = runTest {
        val store = InMemoryExerciseTelemetryStore()
        val h = holder(this, store)
        h.startSession()
        h.onPrematureTap()
        val outcome = assertNotNull(h.state.value.lastOutcome)
        assertTrue(outcome.anticipation)
        assertEquals(GoNoGoPhase.Feedback, h.state.value.phase)
        coroutineContext.cancelChildren()
    }

    @Test
    fun no_go_resolution_flows_end_to_end_into_the_persisted_summary() = runTest {
        // Withholding on every NO-GO ⇒ the persisted summary shows perfect NO-GO accuracy and zero commissions.
        val store = InMemoryExerciseTelemetryStore()
        val h = holder(this, store)
        h.startSession()
        repeat(16) { playTrial(h) { type -> type == GoNoGoTrialType.GO } }
        advanceUntilIdle()
        val sessionId = assertNotNull(h.state.value.sessionId)
        val summary = assertNotNull(store.goNoGoSummaryForSession("u1", sessionId))
        assertEquals(4, summary.noGoTrials, "16 trials with an 8-block quota of 2 ⇒ exactly 4 NO-GO")
        assertEquals(1.0, (summary.noGoAccuracy as GoNoGoSignal.Measured).value, 1e-9)
        assertEquals(0.0, (summary.commissionRate as GoNoGoSignal.Measured).value, 1e-9)
        coroutineContext.cancelChildren()
    }
}
