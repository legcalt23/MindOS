@file:Suppress("MagicNumber", "MaxLineLength") // Fixtures + dense graph wiring under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.AdaptiveAction
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveDecision
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveInput
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ola 0 · S1 — proves the STEP-UP RULE IS REACHED at least once in a real session.
 *
 * [DefaultEmotionAdaptiveEngine] evaluates an ordered rule list in which `holdForHysteresis` precedes `stepUp`, so
 * the step-up rule is only ever evaluated once `blocksSinceChange` has reached
 * [DefaultEmotionAdaptiveEngine.COOLDOWN_AFTER_DECISIONS]. In the two-block session that shipped before this wave
 * that counter could only ever reach 0 and then 1: difficulty could FALL but never RISE, for every exercise, no
 * matter how well the user performed. The adaptive engine was not broken — it was never asked the question.
 *
 * The subject here is that reachability, not any one exercise's mastery, so the test records the counter the engine
 * was ACTUALLY handed on each decision instead of asserting a difficulty outcome. That keeps it honest across the
 * rest of this wave: which exercises can go on to PROVE mastery is a separate defect (the governing metric each
 * zone reads, and whether a block is long enough to make it confident) with its own tests.
 *
 * Mutation-checked: with the session back at two blocks the observed sequence is `[0, 1]` and both assertions fail.
 */
class EmotionHysteresisReachabilityTest {

    /**
     * Records the counter the engine was handed, then always HOLDs. Holding is what isolates the subject: the real
     * engine's verdict depends on metrics that differ per exercise, whereas the hysteresis counter is advanced by
     * the SESSION, and a fixed HOLD is precisely the path the counter is supposed to accumulate along.
     */
    private class HoldingRecorder : EmotionAdaptiveEngine {
        val seenBlocksSinceChange = mutableListOf<Int>()

        override fun decide(input: EmotionAdaptiveInput): EmotionAdaptiveDecision {
            seenBlocksSinceChange += input.blocksSinceChange
            return EmotionAdaptiveDecision(AdaptiveAction.HOLD, input.currentDifficulty, input.intensity)
        }
    }

    private fun graphWith(engine: EmotionAdaptiveEngine): EmotionGraph {
        // Fake EpochClock: desktopTest's transitive kotlinx-datetime has no runtime Clock.System (documented).
        var nowMs = 1_000L
        return EmotionGraph(
            generators = EmotionExerciseGenerators.registry,
            metricsEngine = DefaultEmotionMetricsEngine(),
            assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine()),
            adaptiveEngine = engine,
            overloadDetector = DefaultOverloadDetector(),
            pauseGovernor = PauseGovernor(),
            feedbackBuilder = EmotionFeedbackBuilder(),
            repository = LocalEmotionRepository(InMemorySecureKeyValueStore()),
            epochClock = EpochClock { nowMs += 50; nowMs },
            monotonicClock = SystemMonotonicClock(),
        )
    }

    /** Runs one EM2 session to its terminal. EM2 uses no content bank, so no held-out probe is served. */
    private suspend fun TestScope.runSession(graph: EmotionGraph) {
        val holder = EmotionSessionStateHolder(graph, EmotionExerciseId.EM2_RECUPERACION, USER, scope = this, seed = 7L)
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(valence = 3, arousal = 3, intensity = 2))
        repeat(emotionBlocksFor(EmotionExerciseId.EM2_RECUPERACION) * emotionTrialsPerBlock(EmotionExerciseId.EM2_RECUPERACION, EmotionExerciseCatalog.definition(EmotionExerciseId.EM2_RECUPERACION).governingMetric)) {
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
    fun a_session_reaches_the_hysteresis_threshold_so_the_step_up_rule_is_evaluated() = runTest {
        val recorder = HoldingRecorder()

        runSession(graphWith(recorder))

        assertEquals(
            EmotionSessionStateHolder.BLOCKS,
            recorder.seenBlocksSinceChange.size,
            "the engine must be asked once per block",
        )
        // The real assertion, expressed against the engine's own threshold rather than a second copy of the number:
        // if the counter never reaches it, `holdForHysteresis` short-circuits every decision and no amount of
        // mastery can ever raise difficulty.
        val reached = recorder.seenBlocksSinceChange.max()
        assertTrue(
            reached >= DefaultEmotionAdaptiveEngine.COOLDOWN_AFTER_DECISIONS,
            "the step-up rule is unreachable: the hysteresis counter peaked at $reached, below the threshold " +
                "${DefaultEmotionAdaptiveEngine.COOLDOWN_AFTER_DECISIONS}, so difficulty can only ever fall",
        )

        coroutineContext.cancelChildren()
    }

    @Test
    fun the_counter_advances_one_block_at_a_time_while_the_engine_holds() = runTest {
        val recorder = HoldingRecorder()

        runSession(graphWith(recorder))

        // Pins the accumulation itself. A counter that jumped straight to the threshold would satisfy the test
        // above while destroying the anti-oscillation guarantee the threshold exists to provide.
        assertEquals(
            List(EmotionSessionStateHolder.BLOCKS) { it },
            recorder.seenBlocksSinceChange,
            "the hysteresis counter must advance exactly one block per held decision",
        )

        coroutineContext.cancelChildren()
    }

    private companion object {
        const val USER = "u-hysteresis"
    }
}
