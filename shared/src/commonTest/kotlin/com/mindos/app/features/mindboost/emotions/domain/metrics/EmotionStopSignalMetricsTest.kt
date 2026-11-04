@file:Suppress("MagicNumber") // Trial counts, delays and cell floors ARE the contract under test.

package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionTrialClassifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ola 3 · R1 — the stop-signal readings of the fused inhibition exercise.
 *
 * Two failures this suite exists to prevent, both of which look like success from the outside:
 *  - scoring a stop trial by its OPENING frame, which files every failed inhibition as a hit;
 *  - publishing an affective delta from cells too small to hold one, which is a difference between two noisy
 *    estimates wearing the name of an emotional effect.
 */
class EmotionStopSignalMetricsTest {

    private val engine = DefaultEmotionMetricsEngine()

    // Six independent dimensions of one stop-signal trial, each defaulted so a test names only what it varies.
    // Bundling them into a fixture type would add an indirection whose only job is to satisfy the count.
    @Suppress("LongParameterList")
    private fun trial(
        index: Int,
        stop: Boolean,
        responded: Boolean,
        negative: Boolean = false,
        rtMs: Long = 500,
        ssdMs: Long = 250,
    ): ClassifiedEmotionTrial {
        val payload = EmotionStimulusPayload.AffectiveGoNoGo(
            frameOpen = true,
            affectiveWord = if (negative) "tenso" else "calma",
            congruent = if (stop) negative else !negative,
            stopAfterMs = if (stop) ssdMs else null,
            negativeWord = negative,
        )
        val recorded = RecordedEmotionTrial(
            stimulus = EmotionStimulus(
                index = index,
                correctResponseKey = if (stop) EmotionResponseKey.NONE else EmotionResponseKey.PRIMARY,
                payload = payload,
                interStimulusDelayMs = 500,
                responseWindowMs = 1_200,
            ),
            latencyMs = if (responded) rtMs else null,
            responseKey = if (responded) EmotionResponseKey.PRIMARY else EmotionResponseKey.NONE,
        )
        return ClassifiedEmotionTrial(recorded, EmotionTrialClassifier.classify(recorded))
    }

    /** A converged staircase: half the stop trials escape, which is the regime SSRT is estimable in. */
    private fun convergedBlock(
        stopsPerTone: Int,
        goTrials: Int = 48,
        negativeRtMs: Long = 620,
        neutralRtMs: Long = 620,
    ): List<ClassifiedEmotionTrial> {
        var index = 0
        val go = (0 until goTrials).map { i ->
            trial(
                index++,
                stop = false,
                responded = true,
                negative = i % 2 == 0,
                rtMs = if (i % 2 == 0) negativeRtMs else neutralRtMs,
            )
        }
        val stops = listOf(true, false).flatMap { negative ->
            (0 until stopsPerTone).map { i ->
                trial(
                    index = index++,
                    stop = true,
                    responded = i % 2 == 0, // half escape the signal
                    negative = negative,
                    rtMs = if (negative) negativeRtMs else neutralRtMs,
                )
            }
        }
        return go + stops
    }

    @Test
    fun a_responded_stop_trial_is_a_failed_inhibition_not_a_hit() {
        val failed = trial(0, stop = true, responded = true)
        assertEquals(
            com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome.FALSE_ALARM,
            failed.outcome,
            "a stop trial opens its frame; reading the frame alone would score this as a hit",
        )
        val stopped = trial(1, stop = true, responded = false)
        assertEquals(
            com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome.CORRECT_REJECTION,
            stopped.outcome,
        )
    }

    /**
     * The discriminating half: the classic go/no-go decided at onset must keep scoring exactly as it did, or
     * the change above would read as "stop trials fixed" while quietly breaking every other go/no-go block.
     */
    @Test
    fun a_frame_closed_at_onset_still_scores_the_way_it_always_did() {
        val payload =
            EmotionStimulusPayload.AffectiveGoNoGo(frameOpen = false, affectiveWord = "calma", congruent = true)
        val recorded = RecordedEmotionTrial(
            stimulus = EmotionStimulus(0, EmotionResponseKey.NONE, payload, 500, 1_200),
            latencyMs = 300,
            responseKey = EmotionResponseKey.PRIMARY,
        )
        assertEquals(
            com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome.FALSE_ALARM,
            EmotionTrialClassifier.classify(recorded),
        )
        assertTrue(payload.requiresWithhold)
    }

    @Test
    fun a_block_without_stop_signals_reports_no_ssrt_at_all() {
        val block = (0 until 12).map { trial(it, stop = false, responded = true) }
        val metrics = engine.blockMetrics(block)
        assertNull(
            metrics[EmotionMetricKey.SSRT],
            "a go/no-go never asked how long the brake takes; an `unavailable` key would claim it tried",
        )
        assertNull(metrics[EmotionMetricKey.AFFECTIVE_SSRT_DELTA])
    }

    @Test
    fun a_converged_staircase_produces_a_confident_ssrt() {
        val metrics = engine.blockMetrics(convergedBlock(stopsPerTone = 12))
        val ssrt = metrics[EmotionMetricKey.SSRT]
        assertTrue(ssrt != null && ssrt.confident, "expected a usable SSRT, got $ssrt")
        assertEquals(EmotionMetricUnit.MILLISECONDS, ssrt.unit)
        assertTrue(ssrt.value > 0.0, "an SSRT at or below zero is a broken estimate, not a fast brake")
    }

    @Test
    fun the_affective_delta_needs_its_smaller_cell_to_clear_the_floor() {
        val below = engine.blockMetrics(convergedBlock(stopsPerTone = DefaultEmotionMetricsEngine.MIN_SSRT_CELL - 1))
        assertFalse(
            below[EmotionMetricKey.AFFECTIVE_SSRT_DELTA]?.confident ?: true,
            "11 stop trials per tone is below the floor; a delta here is the noise of two estimates",
        )

        val atFloor = engine.blockMetrics(
            convergedBlock(
                stopsPerTone = DefaultEmotionMetricsEngine.MIN_SSRT_CELL,
                negativeRtMs = 700,
                neutralRtMs = 600,
            ),
        )
        val delta = atFloor[EmotionMetricKey.AFFECTIVE_SSRT_DELTA]
        assertTrue(delta != null && delta.confident, "exactly at the floor the delta must be reported, got $delta")
        assertTrue(
            delta.value > 0.0,
            "the negative-tone cell stopped more slowly, so the affective cost must read positive (was ${delta.value})",
        )
    }

    /**
     * A one-sided block is the sharp case: plenty of stop trials, all of one tone. The delta is a comparison and
     * one of its two sides does not exist, so it must be withheld — never computed against an empty cell.
     */
    @Test
    fun a_one_sided_block_yields_no_delta_however_many_stop_trials_it_has() {
        var index = 0
        val go = (0 until 48).map { trial(index++, stop = false, responded = true, negative = true) }
        val stops = (0 until 30).map { i ->
            trial(index++, stop = true, responded = i % 2 == 0, negative = true)
        }
        val metrics = engine.blockMetrics(go + stops)
        assertFalse(metrics[EmotionMetricKey.AFFECTIVE_SSRT_DELTA]?.confident ?: true)
    }

    /**
     * Commission is counted over the trials that DEMANDED a withhold. Before R1 that denominator was "frames
     * closed at onset", which in a stop-signal block is empty — the rate would have been a division over nothing
     * reported as flawless inhibition.
     */
    @Test
    fun commission_counts_stop_trials_as_the_withhold_denominator() {
        val metrics = engine.blockMetrics(convergedBlock(stopsPerTone = 12))
        val commission = metrics[EmotionMetricKey.COMMISSION_RATE]
        assertEquals(24, commission?.n, "all 24 stop trials are the denominator")
        assertEquals(0.5, commission?.value, "half escaped the signal, which is where the staircase aims")
    }
}
