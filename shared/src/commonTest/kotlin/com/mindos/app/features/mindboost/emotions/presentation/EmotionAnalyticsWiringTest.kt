@file:Suppress("MagicNumber", "MaxLineLength") // Fixtures + dense graph wiring under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalytics
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalyticsEvent
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/** MC8u-B+ §I: analytics events actually fire from the LIVE session holder (integration, not just a unit contract),
 * and a throwing analytics sink can never break, freeze or alter the session. */
class EmotionAnalyticsWiringTest {

    private class Recording : EmotionAnalytics {
        val events = mutableListOf<EmotionAnalyticsEvent>()
        override fun log(event: EmotionAnalyticsEvent) { events += event }
    }

    private class Throwing : EmotionAnalytics {
        override fun log(event: EmotionAnalyticsEvent): Unit = error("analytics down")
    }

    private fun graph(analytics: EmotionAnalytics, repository: LocalEmotionRepository): EmotionGraph {
        var nowMs = 1_000L
        val clock = EpochClock { nowMs += 50; nowMs }
        return EmotionGraph(
            generators = EmotionExerciseGenerators.registry,
            metricsEngine = DefaultEmotionMetricsEngine(),
            assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
            adaptiveEngine = DefaultEmotionAdaptiveEngine(),
            overloadDetector = DefaultOverloadDetector(),
            pauseGovernor = PauseGovernor(),
            feedbackBuilder = EmotionFeedbackBuilder(),
            repository = repository,
            epochClock = clock,
            monotonicClock = SystemMonotonicClock(),
            analytics = analytics,
        )
    }

    private fun TestScope.driveToResult(holder: EmotionSessionStateHolder) {
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(valence = 3, arousal = 3, intensity = 2))
        repeat(12) {
            advanceUntilIdle()
            when (holder.uiState) {
                is EmotionSessionUiState.MidCheckIn -> holder.submitCheckInIntensity(2)
                is EmotionSessionUiState.MicroPause -> holder.onResumeFromMicroPause()
                is EmotionSessionUiState.SelfReportAfter -> holder.submitSelfReport(SelfReport(3, 3, 2))
                else -> Unit
            }
        }
        advanceUntilIdle()
    }

    @Test
    fun session_emits_started_and_terminal_analytics() = runTest {
        val analytics = Recording()
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = EmotionSessionStateHolder(graph(analytics, repo), EmotionExerciseId.EM1_PAUSA, "u1", scope = this, seed = 1L)
        driveToResult(holder)

        assertTrue(analytics.events.any { it is EmotionAnalyticsEvent.ExerciseStarted }, "ExerciseStarted must fire")
        assertTrue(
            analytics.events.any { it is EmotionAnalyticsEvent.SessionCompleted || it is EmotionAnalyticsEvent.SessionAbandoned },
            "a terminal analytics event must fire; got ${analytics.events}",
        )
        coroutineContext.cancelChildren()
    }

    @Test
    fun throwing_analytics_sink_does_not_break_the_session() = runTest {
        val repo = LocalEmotionRepository(InMemorySecureKeyValueStore())
        val holder = EmotionSessionStateHolder(graph(Throwing(), repo), EmotionExerciseId.EM1_PAUSA, "u1", scope = this, seed = 1L)
        driveToResult(holder)

        assertTrue(holder.uiState is EmotionSessionUiState.Result, "a failing analytics sink must not break the session; got ${holder.uiState}")
        assertTrue(repo.recentSummaries("u1").isNotEmpty(), "the session summary must still persist")
        coroutineContext.cancelChildren()
    }
}
