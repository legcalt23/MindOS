package com.mindos.app.features.mindboost.focus.presentation

import com.mindos.app.features.mindboost.core.lifecycle.AppLifecycleProbe
import com.mindos.app.features.mindboost.core.lifecycle.ForegroundLoss
import com.mindos.app.features.mindboost.core.lifecycle.LifecycleObservation
import com.mindos.app.features.mindboost.core.lifecycle.NotObservableReason
import com.mindos.app.features.mindboost.focus.FocusTestFixtures
import com.mindos.app.features.mindboost.focus.data.InMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricAbsence
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.quality.NotMeasurableReason
import com.mindos.app.features.mindboost.focus.domain.quality.SessionAdmission
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * FOC-P0-03 · The lifecycle probe is wired around the session window and what it saw reaches the PERSISTED
 * session. An observed foreground loss turns every metric into a typed `INTERRUPTED` absence; "watched, nothing
 * happened" and "could not watch" leave the metrics alone and only record the observation; and a holder built
 * without a probe records the honest absence, never `null` and never "clean".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusLifecycleWiringTest {

    private class FakeProbe(private val observation: LifecycleObservation) : AppLifecycleProbe {
        var starts = 0
        var stops = 0
        override fun start() { starts++ }
        override fun stop(): LifecycleObservation { stops++; return observation }
    }

    private fun TestScope.finishedSession(probe: AppLifecycleProbe?): SessionResult {
        val history = InMemorySessionHistoryRepository()
        val graph = FocusTestFixtures.focusGraph(historyRepository = history)
        val holder = if (probe == null) {
            FocusSessionStateHolder(graph = graph, exerciseId = ExerciseId.E3_GATILLO, userId = USER, scope = this)
        } else {
            FocusSessionStateHolder(
                graph = graph,
                exerciseId = ExerciseId.E3_GATILLO,
                userId = USER,
                scope = this,
                lifecycleProbe = probe,
            )
        }
        holder.start(SessionTemplate.MAINTENANCE_5)
        advanceUntilIdle()
        assertIs<FocusSessionUiState.Finished>(holder.uiState)
        val session = history.all(USER).single()
        coroutineContext.cancelChildren()
        return session
    }

    @Test
    fun an_observed_foreground_loss_turns_every_metric_into_a_typed_interrupted_absence() = runTest {
        val loss = ForegroundLoss(lostAtNanos = 1_000L, regainedAtNanos = 5_000L)
        val probe = FakeProbe(LifecycleObservation.Observed(listOf(loss)))

        val session = finishedSession(probe)

        assertEquals(1, probe.starts, "the probe is started exactly once per session")
        assertEquals(1, probe.stops, "the probe is stopped exactly once per session")
        assertEquals(LifecycleObservation.Observed(listOf(loss)), session.lifecycle)
        // FOC-P0-05 · and the gate records the same fact as the session's verdict.
        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.INTERRUPTED), session.admission)
        val block = session.blocks.single()
        val accuracy = assertNotNull(block.metrics[MetricKey.ACCURACY])
        assertEquals(MetricAbsence.INTERRUPTED, accuracy.absence)
        assertEquals(false, accuracy.confident)
        assertEquals(block.trials.size, accuracy.n, "the sample size survives; only the claim is withdrawn")
        val stillConfident = (block.metrics.values + session.sessionMetrics.values).filter { it.confident }
        assertEquals(emptyList(), stillConfident.map { it.key }, "no metric of an interrupted session stays confident")
    }

    @Test
    fun a_watched_session_without_interruptions_keeps_its_metrics_and_records_the_observation() = runTest {
        val session = finishedSession(FakeProbe(LifecycleObservation.Observed(emptyList())))

        assertEquals(LifecycleObservation.Observed(emptyList()), session.lifecycle)
        // FOC-P0-05 · watched and clean, but nobody answered: the gate denies for THAT reason.
        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES), session.admission)
        val accuracy = assertNotNull(session.blocks.single().metrics[MetricKey.ACCURACY])
        // Discriminating half: an unanswered E3 block is absent for its OWN reason (FOC-P0-04), never INTERRUPTED.
        assertEquals(MetricAbsence.NO_RESPONSES, accuracy.absence)
    }

    @Test
    fun a_probe_that_could_not_watch_is_recorded_as_such_and_never_read_as_an_interruption() = runTest {
        val disabled = LifecycleObservation.NotObservable(NotObservableReason.DISABLED_BY_FLAG)

        val session = finishedSession(FakeProbe(disabled))

        assertEquals(disabled, session.lifecycle)
        // FOC-P0-05 · the observed fact (nobody answered) is reported over the unknown (nobody watched).
        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES), session.admission)
        val accuracy = assertNotNull(session.blocks.single().metrics[MetricKey.ACCURACY])
        assertNotEquals(MetricAbsence.INTERRUPTED, accuracy.absence)
    }

    @Test
    fun a_holder_built_without_a_probe_records_the_honest_absence_not_null() = runTest {
        val session = finishedSession(probe = null)

        assertEquals(
            LifecycleObservation.NotObservable(NotObservableReason.NO_PLATFORM_SOURCE),
            session.lifecycle,
            "the default probe is a typed absence: never null, never an observation",
        )
        assertEquals(SessionAdmission.NotMeasurable(NotMeasurableReason.NO_RESPONSES), session.admission)
    }

    private companion object {
        const val USER = "lifecycle-user"
    }
}
