@file:Suppress("MagicNumber", "MaxLineLength") // Trial counts and self-report values are the fixtures.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.calibration.ConfidenceLevel
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ola 1 · N1 — the transversal calibration layer, driven through a REAL session.
 *
 * The layer's whole risk is that it looks wired while capturing nothing, so every test here drives an actual
 * session to its terminal and reads what came out, rather than calling the capture function directly.
 */
class EmotionCalibrationCaptureTest {

    private fun graph(repository: LocalEmotionRepository): EmotionGraph {
        // Fake EpochClock: desktopTest's transitive kotlinx-datetime has no runtime Clock.System (documented).
        var nowMs = 1_000L
        return EmotionGraph(
            generators = EmotionExerciseGenerators.registry,
            metricsEngine = DefaultEmotionMetricsEngine(),
            assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
            adaptiveEngine = DefaultEmotionAdaptiveEngine(),
            overloadDetector = DefaultOverloadDetector(),
            pauseGovernor = PauseGovernor(),
            feedbackBuilder = EmotionFeedbackBuilder(),
            repository = repository,
            epochClock = EpochClock { nowMs += 50; nowMs },
            monotonicClock = SystemMonotonicClock(),
        )
    }

    /**
     * Drives a session in SMALL virtual-time steps so the short-lived calibration prompts are actually observed.
     * `advanceUntilIdle` would blow straight through them: both prompts resolve themselves on a timeout, so from
     * an idle-advancing test they are invisible even when they fire.
     *
     * [answerConfidence] and [answerForecast] control whether the user engages with each prompt, which is what
     * lets the same driver produce both the "captured" and the "let it lapse" cases.
     */
    private fun TestScope.drive(
        exercise: EmotionExerciseId,
        answerConfidence: ConfidenceLevel?,
        answerForecast: Int?,
        seed: Long = 7L,
    ): EmotionSessionStateHolder {
        val holder = EmotionSessionStateHolder(graph(LocalEmotionRepository(InMemorySecureKeyValueStore())), exercise, USER, scope = this, seed = seed)
        holder.start()
        runCurrent()
        holder.submitSelfReport(SelfReport(valence = 3, arousal = 3, intensity = 2))

        repeat(MAX_STEPS) {
            when (val state = holder.uiState) {
                is EmotionSessionUiState.ConfidencePrompt -> answerConfidence?.let(holder::submitConfidence)
                is EmotionSessionUiState.BlockForecastPrompt -> answerForecast?.let { holder.submitBlockForecast(it.coerceAtMost(state.blockSize)) }
                is EmotionSessionUiState.MidCheckIn -> holder.submitCheckInIntensity(2)
                is EmotionSessionUiState.MicroPause -> holder.onResumeFromMicroPause()
                is EmotionSessionUiState.SelfReportAfter -> holder.submitSelfReport(SelfReport(3, 3, 2))
                is EmotionSessionUiState.Running -> answerTrial(holder, state)
                else -> Unit
            }
            advanceTimeBy(STEP_MS)
            runCurrent()
        }
        advanceUntilIdle()
        return holder
    }

    /**
     * Answers with the control the field on screen actually offers.
     *
     * This has to dispatch on the field rather than firing every control at once: whichever call lands first wins
     * the trial, so a blanket `onTap()` would answer a classification item with a PRIMARY key and no category —
     * an input a real user cannot produce, and one that silently makes the trial unjudgeable.
     */
    private fun answerTrial(holder: EmotionSessionStateHolder, state: EmotionSessionUiState.Running) {
        when (state.model.field) {
            is EmotionFieldState.GoNoGo -> holder.onTap()
            is EmotionFieldState.TwoAlt -> holder.onLeft()
            is EmotionFieldState.FourCategory -> holder.onCategory(EmotionCategory.THOUGHT)
            // Ola 3 · R2 — the post-error marker takes no input; the trial it belongs to is already answered.
            EmotionFieldState.ErrorMarker -> Unit
        }
    }

    @Test
    fun a_classification_session_captures_the_confidence_judgments_the_user_makes() {
        runTest {
            val holder = drive(EmotionExerciseId.EM3_SEPARADOR, answerConfidence = ConfidenceLevel.HIGH, answerForecast = 5)
            assertTrue(
                holder.sessionCalibration.isNotEmpty(),
                "a four-way classification session must be able to carry confidence judgments; captured none",
            )
            assertTrue(
                holder.sessionCalibration.all { it.chanceLevel == 0.25 },
                "every judgment from a four-way task must carry that task's chance level, got ${holder.sessionCalibration.map { it.chanceLevel }.distinct()}",
            )
            assertTrue(holder.sessionCalibration.all { it.level == ConfidenceLevel.HIGH })
        }
    }

    @Test
    fun a_go_no_go_session_never_offers_the_strip_at_all() {
        runTest {
            // The discriminating half of the paradigm gate. The user here answers EVERY prompt they are shown, so
            // an empty result can only mean the strip was never offered — which is the intended behaviour: the
            // correct answer to a no-go trial is withholding, and there is no choice to rate.
            val holder = drive(EmotionExerciseId.EM1_PAUSA, answerConfidence = ConfidenceLevel.HIGH, answerForecast = 5)
            assertEquals(
                emptyList(),
                holder.sessionCalibration,
                "go/no-go must not carry confidence judgments; capturing them would rate a non-response",
            )
            assertEquals(
                emptyList(),
                holder.sessionBlockForecasts,
                "and a host that cannot carry judgments must not be asked to forecast either",
            )
        }
    }

    @Test
    fun a_strip_the_user_ignores_records_nothing_rather_than_a_low_confidence_guess() {
        runTest {
            val holder = drive(EmotionExerciseId.EM3_SEPARADOR, answerConfidence = null, answerForecast = 5)
            assertEquals(
                emptyList(),
                holder.sessionCalibration,
                "silence is not a judgment: an ignored strip must leave no trace",
            )
        }
    }

    @Test
    fun the_block_forecast_is_asked_and_scored_against_what_actually_happened() {
        runTest {
            val holder = drive(EmotionExerciseId.EM3_SEPARADOR, answerConfidence = ConfidenceLevel.MEDIUM, answerForecast = 4)
            assertTrue(holder.sessionBlockForecasts.isNotEmpty(), "the forecast prompt never fired")
            holder.sessionBlockForecasts.forEach { outcome ->
                assertEquals(4, outcome.prediction.predictedCorrect)
                assertTrue(
                    outcome.actualCorrect in 0..outcome.prediction.blockSize,
                    "a scored block must fit its own size, got ${outcome.actualCorrect} of ${outcome.prediction.blockSize}",
                )
            }
        }
    }

    @Test
    fun a_forecast_the_user_ignores_is_not_recorded_as_a_forecast_of_zero() {
        runTest {
            val holder = drive(EmotionExerciseId.EM3_SEPARADOR, answerConfidence = ConfidenceLevel.LOW, answerForecast = null)
            assertEquals(
                emptyList(),
                holder.sessionBlockForecasts,
                "predicting nothing is not predicting zero — a zero would be scored as wild pessimism",
            )
        }
    }

    @Test
    fun exiting_during_a_calibration_prompt_still_ends_the_session() {
        runTest {
            // Safety: the layer must never become the reason a user cannot leave. The forecast prompt in
            // particular holds the screen far longer than a trial does.
            val holder = EmotionSessionStateHolder(graph(LocalEmotionRepository(InMemorySecureKeyValueStore())), EmotionExerciseId.EM3_SEPARADOR, USER, scope = this, seed = 7L)
            holder.start()
            runCurrent()
            holder.submitSelfReport(SelfReport(3, 3, 2))
            repeat(MAX_STEPS) {
                if (holder.uiState is EmotionSessionUiState.BlockForecastPrompt) return@repeat
                advanceTimeBy(STEP_MS)
                runCurrent()
            }
            assertTrue(
                holder.uiState is EmotionSessionUiState.BlockForecastPrompt,
                "expected the forecast prompt to be reachable, got ${holder.uiState}",
            )
            holder.onExit()
            advanceUntilIdle()
            assertTrue(
                holder.uiState is EmotionSessionUiState.Exited || holder.uiState is EmotionSessionUiState.SaveFailed,
                "exit during a calibration prompt must still terminate the session, got ${holder.uiState}",
            )
        }
    }

    private companion object {
        const val USER = "u-calibration"
        const val STEP_MS = 100L

        /** Generous: a full three-block session plus every prompt, stepped 100 ms at a time. */
        const val MAX_STEPS = 4_000
    }
}
