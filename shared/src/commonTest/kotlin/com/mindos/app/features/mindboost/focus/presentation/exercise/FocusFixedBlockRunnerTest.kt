package com.mindos.app.features.mindboost.focus.presentation.exercise

import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The shared fixed-difficulty engine must (1) refuse TRAINING (no adaptation here), (2) record every
 * trial with an honest monotonic RT, and (3) classify tap targets/lures correctly so a baseline/probe
 * block can be assembled. Virtual time (runTest) skips the real delays.
 */
class FocusFixedBlockRunnerTest {

    private class FakeMonotonicClock(var nanos: Long = 0) : MonotonicClock {
        override fun nowNanos(): Long = nanos
        fun advanceMs(ms: Long) { nanos += ms * 1_000_000L }
    }

    /** Emits [signals] tap targets then [lures] non-targets (E1-like Vigilance payload). */
    private class FakeTapGenerator(private val signals: Int, private val lures: Int) : TrialGenerator {
        override val exerciseId = ExerciseId.E1_VIGIA
        override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> =
            (0 until spec.targetTrialCount).map { i ->
                val isTarget = i < signals
                Stimulus(
                    index = i,
                    isTarget = isTarget,
                    correctResponseKey = if (isTarget) ResponseKey.PRIMARY else null,
                    payload = StimulusPayload.Vigilance(isSignal = isTarget, contrast = 1.0, lure = !isTarget),
                    interStimulusDelayMs = 10,
                    responseWindowMs = 1_000,
                )
            }.also { require(spec.targetTrialCount == signals + lures) }
    }

    private val runner = FocusFixedBlockRunner(FakeMonotonicClock())

    @Test
    fun trainingMode_isRejected() = runTest {
        assertFailsWith<IllegalArgumentException> {
            runner.run(
                generator = FakeTapGenerator(1, 0),
                difficulty = DifficultyVector.EASIEST,
                trialCount = 1,
                seed = 1L,
                mode = Mode.TRAINING,
                thresholds = IndividualThresholds(),
                awaitResponse = { null },
                onProgress = {},
            )
        }
    }

    @Test
    fun noResponse_recordsEveryTrial_withNoHits() = runTest {
        val result = runner.run(
            generator = FakeTapGenerator(signals = 4, lures = 2),
            difficulty = DifficultyVector.EASIEST,
            trialCount = 6,
            seed = 1L,
            mode = Mode.EVALUATION,
            thresholds = IndividualThresholds(),
            awaitResponse = { null },
            onProgress = {},
        )
        assertEquals(6, result.recorded.size)
        assertTrue(result.recorded.all { it.responseLatencyMs == null })
        assertEquals(0, result.counters.hits)
    }

    @Test
    fun fastResponses_hitTargets_andFalseAlarmOnLures() = runTest {
        val clock = FakeMonotonicClock()
        val runner = FocusFixedBlockRunner(clock)
        val result = runner.run(
            generator = FakeTapGenerator(signals = 3, lures = 2),
            difficulty = DifficultyVector.EASIEST,
            trialCount = 5,
            seed = 1L,
            mode = Mode.CALIBRATION,
            thresholds = IndividualThresholds(),
            // Respond 200 ms after each onset (within the 1 s window).
            awaitResponse = { clock.advanceMs(200); FocusResponseEvent(clock.nowNanos(), ResponseKey.PRIMARY) },
            onProgress = {},
        )
        assertEquals(3, result.counters.hits, "Three targets responded within window → 3 hits.")
        assertEquals(2, result.counters.falseAlarms, "Two lures responded to → 2 false alarms.")
        assertTrue(result.recorded.all { it.responseLatencyMs == 200L }, "Monotonic RT must be 200 ms.")
    }
}
