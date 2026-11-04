@file:Suppress("MagicNumber", "MaxLineLength") // Session shape + fixture values are the contract under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionFeedbackBuilder
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.DefaultOverloadDetector
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEventType
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernor
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionSessionAssembler
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.focus.presentation.exercise.SystemMonotonicClock
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ola 3 · R2 — the fused recovery exercise driven end to end through the real holder.
 *
 * The unit tests cover the walk (`AccuracyStaircaseTest`) and the session shape
 * (`EmotionPostErrorReachabilityTest`); what only a driven session can show is that the pieces are actually
 * WIRED: that the level the staircase computed is the level the user is shown, that a wrong answer produces the
 * neutral marker and costs the visible streak, and that the induction ceiling both fires when the design says it
 * must and stays silent when it must not. A ceiling that fires for everyone is not a ceiling — it is a session
 * that always relents — which is why the two ceiling tests below are a pair and neither is worth much alone.
 */
class EmotionRecoverySessionTest {

    private class Harness(scope: TestScope, seed: Long) {
        val store = InMemorySecureKeyValueStore()
        val repository = LocalEmotionRepository(store)
        val holder: EmotionSessionStateHolder

        init {
            // Fake EpochClock: desktopTest's transitive kotlinx-datetime has no runtime Clock.System (documented).
            var nowMs = 1_000L
            val graph = EmotionGraph(
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
            holder = EmotionSessionStateHolder(graph, EmotionExerciseId.EM2_RECUPERACION, "u1", scope = scope, seed = seed)
        }
    }

    /** What the user was actually shown on one two-alternative trial. */
    private data class Shown(val trialIndex: Int, val gap: Float, val streak: Int?)

    /**
     * Everything a driven session is asked about. [markerBeforeStrip] is the guard that keeps the fix to the
     * marker from breaking the calibration layer: the confidence strip asks how sure the user is about the answer
     * they just gave, so a marker that reached the screen FIRST would tell them they were wrong and the judgment
     * would be worthless. Order here is a measurement property, not cosmetics.
     */
    private data class Observed(val shown: List<Shown>, val sawMarker: Boolean, val markerBeforeStrip: Boolean)

    /**
     * Runs a whole session, sampling the UI often enough to see the brief post-error marker, and answering with
     * [answerCorrectly] — `true` picks the larger bar (always right), `false` picks the smaller one (always
     * wrong), `null` answers nothing and lets every trial time out.
     */
    private fun TestScope.runSession(
        harness: Harness,
        answerCorrectly: Boolean?,
    ): Observed {
        val shown = mutableListOf<Shown>()
        var sawMarker = false
        var markerBeforeStrip = false
        var stripSinceTrial = false
        var answered = -1
        harness.holder.start()
        advanceUntilIdle()
        harness.holder.submitSelfReport(SelfReport(valence = 3, arousal = 3, intensity = 2))
        repeat(6_000) {
            when (val state = harness.holder.uiState) {
                is EmotionSessionUiState.Running -> when (val field = state.model.field) {
                    is EmotionFieldState.TwoAlt -> {
                        if (state.model.trialIndex != answered) {
                            shown += Shown(state.model.trialIndex, field.leftMagnitude - field.rightMagnitude, state.model.streak)
                            stripSinceTrial = false
                            if (answerCorrectly != null) {
                                answered = state.model.trialIndex
                                val leftIsCorrect = field.leftMagnitude > field.rightMagnitude
                                if (leftIsCorrect == answerCorrectly) harness.holder.onLeft() else harness.holder.onRight()
                            }
                        }
                    }
                    is EmotionFieldState.ErrorMarker -> {
                        sawMarker = true
                        if (!stripSinceTrial) markerBeforeStrip = true
                    }
                    else -> Unit
                }
                is EmotionSessionUiState.ConfidencePrompt -> stripSinceTrial = true
                is EmotionSessionUiState.MidCheckIn -> harness.holder.submitCheckInIntensity(2)
                is EmotionSessionUiState.MicroPause -> harness.holder.onResumeFromMicroPause()
                is EmotionSessionUiState.SelfReportAfter -> harness.holder.submitSelfReport(SelfReport(3, 3, 2))
                else -> Unit
            }
            advanceTimeBy(100)
        }
        advanceUntilIdle()
        return Observed(shown, sawMarker, markerBeforeStrip)
    }

    private suspend fun safetyTypes(harness: Harness): List<String> =
        harness.repository.recentSummaries("u1").flatMap { summary -> summary.safetyEvents.map { it.type } }

    @Test
    fun the_level_the_user_sees_is_the_live_staircase_level_and_not_the_generators_placeholder() = runTest {
        val harness = Harness(this, seed = 4L)
        val shown = runSession(harness, answerCorrectly = true).shown

        assertTrue(shown.size > 40, "the session presented only ${shown.size} trials; the drive loop is too short")
        val gaps = shown.map { kotlin.math.abs(it.gap) }.distinct()
        assertTrue(
            gaps.size > 1,
            "every trial was presented at the same difficulty ($gaps) — the staircase computed a level that " +
                "never reached the user, which is the placeholder being shown instead of the stamp",
        )
        // A user who never errs is walked toward a harder task, never an easier one.
        assertTrue(
            kotlin.math.abs(shown.last().gap) < kotlin.math.abs(shown.first().gap),
            "a session answered perfectly ended no harder than it started (${shown.first().gap} -> ${shown.last().gap})",
        )
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_wrong_answer_shows_the_neutral_marker_and_resets_the_visible_streak() = runTest {
        val harness = Harness(this, seed = 5L)
        val observed = runSession(harness, answerCorrectly = false)

        assertTrue(
            observed.sawMarker,
            "no post-error marker was ever shown across ${observed.shown.size} wrong answers — the marker is" +
                " being suppressed by whatever holds the screen between the response and this point",
        )
        assertTrue(
            !observed.markerBeforeStrip,
            "the marker reached the screen before the confidence strip: the user is told they were wrong and" +
                " is then asked how sure they were, which makes the calibration judgment meaningless",
        )
        assertTrue(observed.shown.all { it.streak == 0 }, "the streak survived a wrong answer: ${observed.shown.map { it.streak }.distinct()}")
        coroutineContext.cancelChildren()
    }

    @Test
    fun a_perfect_session_builds_the_streak_and_never_relents() = runTest {
        val harness = Harness(this, seed = 6L)
        val observed = runSession(harness, answerCorrectly = true)
        val shown = observed.shown

        assertTrue(!observed.sawMarker, "the marker appeared in a session with no wrong answers")
        assertEquals(0, shown.first().streak ?: -1, "the streak must start at zero rather than at nothing")
        assertTrue(shown.last().streak!! > shown.first().streak!!, "correct answers did not build the visible streak")
        assertTrue(
            SafetyEventType.REDUCE_INTENSITY.name !in safetyTypes(harness),
            "the induction ceiling fired in a session with no errors at all — a ceiling that fires for " +
                "everyone is a session that always relents, and the reading goes with it",
        )
        coroutineContext.cancelChildren()
    }

    @Test
    fun three_errors_in_a_row_lower_the_induction_exactly_once() = runTest {
        val harness = Harness(this, seed = 7L)
        runSession(harness, answerCorrectly = false)

        val relief = safetyTypes(harness).count { it == SafetyEventType.REDUCE_INTENSITY.name }
        assertEquals(
            1,
            relief,
            "the ceiling fired $relief times across a session of nothing but errors; it must fire (the error " +
                "rate is held on purpose, so frustration needs a stated maximum) and it must fire ONCE, or it " +
                "ratchets the induction down to nothing and takes the measurement with it",
        )
        coroutineContext.cancelChildren()
    }
}
