package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.core.lifecycle.AppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.ForegroundLoss
import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.core.lifecycle.NotObservableReason
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemoryFocusProfileRepository
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveDecision
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveEngine
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveInput
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveResult
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricAbsence
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.domain.session.FocusReport
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.honestResponseTo
import com.mindos.app.features.mindboost.focus.presentation.exercise.MonotonicClock
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * FOC-P0-05 · The gate is wired where the session becomes publishable: nothing downstream of
 * `FocusSessionStateHolder.finishSession` (history, adaptation, profile evolution, personalised score, report)
 * may consume a session the gate did not admit. Drives a REAL E3 session through the holder with a stepping
 * monotonic clock so every trial is answered (or none is), and an adaptive engine that ALWAYS steps up — the
 * live engine holds today for its own reason (FOC-P0-02), so only an always-moving engine can show that
 * "difficulty holds" comes from the gate and not from the engine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusQualityGateWiringTest {

    private class FakeProbe(private val observation: LifecycleObservation) : AppLifecycleProbe {
        override fun start() = Unit
        override fun stop(): LifecycleObservation = observation
    }

    private class SteppingClock : MonotonicClock {
        var nanos = 0L
        override fun nowNanos(): Long = nanos
    }

    private object AlwaysStepUp : AdaptiveEngine {
        override fun decide(input: AdaptiveInput): AdaptiveResult = AdaptiveResult(
            decision = AdaptiveDecision.StepUp(DifficultyAxis.PERCEPTUAL_LOAD),
            nextDifficulty = input.current.stepUp(DifficultyAxis.PERCEPTUAL_LOAD, STEP),
            rationale = "test engine: always steps up",
        )
    }

    private class Run(val session: SessionResult, val profile: UserCognitiveProfile, val report: FocusReport) {
        val confidentKeys: List<MetricKey>
            get() = (session.blocks.flatMap { it.metrics.values } + session.sessionMetrics.values)
                .filter { it.confident }
                .map { it.key }
        val accuracy get() = assertNotNull(session.blocks.single().metrics[MetricKey.ACCURACY])
        val difficulty get() = assertNotNull(profile.startingDifficulty[ExerciseId.E3_GATILLO])
    }

    private val watched = LifecycleObservation.Observed(emptyList())
    private val unwatched = LifecycleObservation.NotObservable(NotObservableReason.DISABLED_BY_FLAG)
    private val interrupted = LifecycleObservation.Observed(listOf(ForegroundLoss(1_000L, 2_000L)))

    /**
     * [respond] false plays nothing at all; true PLAYS THE TASK — it answers a go signal and withholds on a
     * no-go, read off the same field the user sees. R15: the earlier version tapped on every tick regardless of
     * what was on screen, which in a go/no-go lane IS the blind "respond to everything" strategy, so
     * `FocusAntiGaming` withholds it. A fixture that plays blind cannot stand in for the honest half.
     */
    private suspend fun TestScope.play(
        lifecycle: LifecycleObservation,
        respond: Boolean,
        scoreSurfaces: Boolean = false,
    ): Run {
        val history = InMemorySessionHistoryRepository()
        val profiles = InMemoryFocusProfileRepository()
        val clock = SteppingClock()
        val graph = FocusTestFixtures.focusGraph(
            monotonicClock = clock,
            profileRepository = profiles,
            historyRepository = history,
        ).copy(adaptiveEngine = AlwaysStepUp)
        val holder = FocusSessionStateHolder(
            graph = graph,
            exerciseId = ExerciseId.E3_GATILLO,
            userId = USER,
            scope = this,
            lifecycleProbe = FakeProbe(lifecycle),
            scoreSurfacesEnabled = { scoreSurfaces },
        )
        holder.start(SessionTemplate.MAINTENANCE_5)
        var guard = 0
        while (holder.uiState !is FocusSessionUiState.Finished && guard++ < MAX_TICKS) {
            runCurrent()
            val field = (holder.uiState as? FocusSessionUiState.Running)?.model?.field
            if (respond && field != null && honestResponseTo(field) != null) {
                clock.nanos += TAP_RT_NANOS
                holder.onTap()
            }
            advanceTimeBy(TICK_MS)
        }
        val finished = assertIs<FocusSessionUiState.Finished>(holder.uiState, "did not finish in $MAX_TICKS ticks")
        val run = Run(history.all(USER).single(), assertNotNull(profiles.load(USER)), finished.report)
        coroutineContext.cancelChildren()
        return run
    }

    @Test
    fun an_unwatched_session_is_recorded_as_not_measurable_and_moves_nothing() = runTest {
        val run = play(unwatched, respond = true)

        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.LIFECYCLE_NOT_OBSERVED), run.session.admission)
        assertEquals(unwatched, run.session.lifecycle)
        assertEquals(emptyList(), run.confidentKeys, "no confident metric may survive a denied session")
        assertEquals(MetricAbsence.NOT_ADMITTED, run.accuracy.absence)
        assertTrue(run.accuracy.n > 0, "the sample size is kept, only the claim is withdrawn")
        // The engine would have stepped up; the gate never consulted it.
        assertEquals(DifficultyVector.EASIEST, run.difficulty)
        assertEquals(1, run.profile.totalSessions, "the session happened: usage is a fact, not a claim")
        assertNull(run.report.personalizedScore)
        assertTrue(run.report.honestNote.startsWith(NOT_MEASURABLE_PREFIX), run.report.honestNote)
        assertFalse("demasiado corta" in run.report.honestNote, run.report.honestNote)
    }

    @Test
    fun an_honest_watched_answered_session_is_admitted_and_everything_downstream_moves() = runTest {
        val run = play(watched, respond = true)

        assertEquals(SessionAdmission.Admitted, run.session.admission)
        assertTrue(run.accuracy.confident)
        assertNotEquals(DifficultyVector.EASIEST, run.difficulty, "discriminating half: the engine's step-up lands")
        assertEquals(1, run.profile.totalSessions)
        assertFalse(run.report.honestNote.startsWith(NOT_MEASURABLE_PREFIX), run.report.honestNote)
    }

    @Test
    fun a_watched_session_nobody_answered_is_not_measurable_for_that_reason() = runTest {
        val run = play(watched, respond = false)

        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES), run.session.admission)
        assertEquals(emptyList(), run.confidentKeys)
        assertEquals(DifficultyVector.EASIEST, run.difficulty)
        assertTrue(run.report.honestNote.startsWith(NOT_MEASURABLE_PREFIX), run.report.honestNote)
    }

    @Test
    fun an_observed_interruption_is_not_measurable_and_keeps_the_more_specific_absence() = runTest {
        val run = play(interrupted, respond = true)

        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED), run.session.admission)
        assertEquals(emptyList(), run.confidentKeys)
        assertEquals(MetricAbsence.INTERRUPTED, run.accuracy.absence, "FOC-P0-03's reason is not overwritten")
        assertEquals(DifficultyVector.EASIEST, run.difficulty)
    }

    @Test
    fun the_gate_not_the_containment_flag_is_what_keeps_the_score_dark() = runTest {
        // Score surfaces ON: the only thing standing between a denied session and a score is the gate.
        val denied = play(unwatched, respond = true, scoreSurfaces = true)
        assertNull(denied.report.personalizedScore, "a denied session never reaches the personalised scorer")

        val admitted = play(watched, respond = true, scoreSurfaces = true)
        assertNotNull(admitted.report.personalizedScore, "discriminating half: an admitted session is scored")
    }

    private companion object {
        const val USER = "user-p005"
        const val MAX_TICKS = 200_000
        const val TICK_MS = 50L
        const val TAP_RT_NANOS = 300_000_000L
        const val STEP = 1.0
        const val NOT_MEASURABLE_PREFIX = "Esta sesión no cuenta para tu progreso: "
    }
}
