@file:Suppress("MagicNumber", "MaxLineLength") // Fixtures + dense graph wiring under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.emotions.data.local.LocalEmotionRepository
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.DefaultEmotionAdaptiveEngine
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.SeparatorItemFactory
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionExerciseGenerators
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionHoldoutProbe
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Ola 4 · P2 — proves the held-out probe is actually SERVED in a real session, and that serving it does not
 * contaminate the training it exists to evaluate. A holdout that is reserved but never administered proves
 * nothing; a holdout whose trials leak into the training statistics is worse than none, because it inflates the
 * very numbers it was supposed to audit.
 */
class EmotionHoldoutWiringTest {

    // The whole bank: authored seed plus generated extension. Both partitions grew, and a probe item from the
    // generated half is just as held-out as one from the seed.
    private val evalPhrases = SeparatorItemFactory.ALL.filter { it.isEval }.map { it.phrase }.toSet()
    private val trainingPhrases = SeparatorItemFactory.ALL.filter { !it.isEval }.map { it.phrase }.toSet()

    private fun graphWith(repository: LocalEmotionRepository): EmotionGraph {
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

    /** Runs one EM3 session to its terminal, answering nothing (every trial times out). */
    private suspend fun TestScope.runSession(repository: LocalEmotionRepository, seed: Long) {
        val holder = EmotionSessionStateHolder(graphWith(repository), EmotionExerciseId.EM3_SEPARADOR, USER, scope = this, seed = seed)
        holder.start()
        advanceUntilIdle()
        holder.submitSelfReport(SelfReport(valence = 3, arousal = 3, intensity = 2))
        repeat(TRAINING_TRIALS) {
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
    fun a_session_serves_a_probe_and_the_probe_never_enters_the_training_statistics() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())

        runSession(repository, seed = 1L)

        val summary = repository.recentSummaries(USER).single()
        val probe = assertNotNull(summary.holdoutProbe, "the session must have served a held-out probe")

        assertEquals(EmotionHoldoutProbe.PROBE_SIZE, probe.total, "a partial probe must never be recorded")
        assertEquals(probe.total, probe.servedItemIds.size)
        assertTrue(probe.servedItemIds.all { it in evalPhrases }, "the probe must draw only held-out items")
        assertTrue(probe.correct in 0..probe.total)

        // The contamination check. The training statistics are computed over the training blocks ONLY; if probe
        // trials had been appended to them this count would include the probe's trials.
        assertEquals(TRAINING_TRIALS, summary.totalAnswers, "probe trials leaked into the training statistics")

        // ...and the probe must not pollute the training exposure history either, or a later training block would
        // start avoiding held-out items it was never supposed to know about.
        val exposed = repository.recentExposedPhrases(USER, EmotionExerciseId.EM3_SEPARADOR.name)
        assertTrue(exposed.isNotEmpty(), "training exposure must still be recorded")
        assertTrue(exposed.all { it in trainingPhrases }, "an eval phrase reached the training exposure record")
    }

    @Test
    fun a_later_session_probes_on_items_the_first_session_did_not_use() = runTest {
        val repository = LocalEmotionRepository(InMemorySecureKeyValueStore())

        runSession(repository, seed = 1L)
        runSession(repository, seed = 2L)

        val probes = repository.recentSummaries(USER).mapNotNull { it.holdoutProbe }
        assertEquals(2, probes.size, "each session serves its own probe while the holdout lasts")
        val first = probes[0].servedItemIds.toSet()
        val second = probes[1].servedItemIds.toSet()
        assertTrue(first.intersect(second).isEmpty(), "the second probe re-used an item the user had already answered")

        coroutineContext.cancelChildren()
    }

    private companion object {
        const val USER = "u-holdout"

        /**
         * The training trials of one session, with no probe trials added — DERIVED from the session structure
         * instead of pinned as a literal. What this test asserts is probe-vs-training SEPARATION; the block
         * structure is not its subject, so a hard-coded count would turn any future change of that structure into
         * a false failure here and invite someone to "fix" it by retuning the number.
         */
        val TRAINING_TRIALS: Int = emotionBlocksFor(EmotionExerciseId.EM3_SEPARADOR) *
            emotionTrialsPerBlock(EmotionExerciseId.EM3_SEPARADOR, EmotionExerciseCatalog.definition(EmotionExerciseId.EM3_SEPARADOR).governingMetric)
    }
}
