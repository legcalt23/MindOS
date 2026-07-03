@file:Suppress("MagicNumber", "MaxLineLength") // Fixtures + dense graph wiring under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmotionSessionStateHolderTest {

    @Test
    fun field_mapping_is_honest_per_paradigm() {
        val goNoGo = emotionFieldFor(
            EmotionStimulus(0, EmotionResponseKey.PRIMARY, EmotionStimulusPayload.AffectiveGoNoGo(true, "espera", true), 0, 1000),
        )
        assertTrue(goNoGo is EmotionFieldState.GoNoGo && goNoGo.frameOpen)

        val left = emotionFieldFor(
            EmotionStimulus(0, EmotionResponseKey.LEFT, EmotionStimulusPayload.TwoAltPerceptual(EmotionResponseKey.LEFT, 0.5, false), 0, 1000),
        ) as EmotionFieldState.TwoAlt
        assertTrue(left.leftMagnitude > left.rightMagnitude, "the larger bar must be on the correct (LEFT) side")

        val right = emotionFieldFor(
            EmotionStimulus(0, EmotionResponseKey.RIGHT, EmotionStimulusPayload.TwoAltPerceptual(EmotionResponseKey.RIGHT, 0.5, false), 0, 1000),
        ) as EmotionFieldState.TwoAlt
        assertTrue(right.rightMagnitude > right.leftMagnitude)

        val classify = emotionFieldFor(
            EmotionStimulus(0, null, EmotionStimulusPayload.Classification("Hay tres opciones", EmotionCategory.FACT, false), 0, 1000),
        )
        assertEquals("Hay tres opciones", (classify as EmotionFieldState.FourCategory).phrase)
    }

    @Test
    fun session_runs_end_to_end_to_result_and_persists() = runTest {
        val store = InMemorySecureKeyValueStore()
        val repository = LocalEmotionRepository(store)
        // Inject a fake EpochClock: desktopTest's transitive kotlinx-datetime 0.7.1 has no runtime
        // Clock.System, so SystemEpochClock would crash here (documented). Production keeps the real one.
        var nowMs = 1_000L
        val testClock = EpochClock { nowMs += 50; nowMs }
        val graph = EmotionGraph(
            generators = EmotionExerciseGenerators.registry,
            metricsEngine = DefaultEmotionMetricsEngine(),
            assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
            adaptiveEngine = DefaultEmotionAdaptiveEngine(),
            overloadDetector = DefaultOverloadDetector(),
            pauseGovernor = PauseGovernor(),
            feedbackBuilder = EmotionFeedbackBuilder(),
            repository = repository,
            epochClock = testClock,
            monotonicClock = SystemMonotonicClock(),
        )
        val holder = EmotionSessionStateHolder(graph, EmotionExerciseId.EM1_PAUSA, "u1", scope = this, seed = 1L)

        holder.start()
        advanceUntilIdle()
        assertTrue(holder.uiState is EmotionSessionUiState.SelfReportBefore)
        holder.submitSelfReport(SelfReport(valence = 3, arousal = 3, intensity = 2))

        // Drive through any proxy-armed safety check-in / micro-pause to the after-report, then finish.
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

        assertTrue(holder.uiState is EmotionSessionUiState.Result, "expected Result, got ${holder.uiState}")
        assertTrue(repository.recentSummaries("u1").isNotEmpty(), "a session summary must be persisted")

        coroutineContext.cancelChildren()
    }
}
