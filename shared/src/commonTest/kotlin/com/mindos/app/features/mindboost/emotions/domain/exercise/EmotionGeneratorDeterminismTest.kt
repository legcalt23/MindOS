@file:Suppress("MagicNumber") // Block sizes / cell thresholds are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins generator determinism (same seed → same trials) and the structural guarantees each exercise's
 * metrics depend on (EM1 has both congruent and incongruent GO cells + a no-go; EM2 plants ≥3 hard
 * trials; EM3 draws disjoint training items).
 */
class EmotionGeneratorDeterminismTest {

    private val difficulty = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = 0.4)
    private val spec = EmotionBlockSpec(targetTrialCount = 20, responseWindowMs = 1500)

    @Test
    fun all_generators_are_deterministic_for_a_seed() {
        listOf(EM1PausaGenerator(), EM2RecuperacionGenerator(), EM3SeparadorGenerator()).forEach { gen ->
            assertEquals(
                gen.generate(difficulty, spec, seed = 42L),
                gen.generate(difficulty, spec, seed = 42L),
                "${gen.exerciseId} must be deterministic for a fixed seed.",
            )
        }
    }

    /**
     * Ola 3 · R1 turned this exercise from a no-go into a STOP SIGNAL, so the old guarantee ("at least one frame
     * closed at onset") is not a weaker version of the new one — it is the opposite of it. Every trial must now
     * open its frame, because a trial whose frame is shut from the start never starts the response the brake
     * exists to catch, and an SSRT estimated over such trials would not be an SSRT.
     *
     * The delay itself is deliberately absent here: the generator declares WHICH trials stop, the staircase
     * decides WHEN, and a generator that pre-baked a delay would have quietly taken the adaptation offline.
     */
    @Test
    fun em1_opens_every_frame_and_marks_the_stop_trials_by_their_expected_answer() {
        val out = EM1PausaGenerator().generate(difficulty, spec, seed = 7L)
        assertEquals(20, out.size)
        val payloads = out.map { it.payload as EmotionStimulusPayload.AffectiveGoNoGo }
        assertTrue(payloads.all { it.frameOpen }, "a stop trial must START as a go, or there is no brake to time")
        assertTrue(payloads.all { it.stopAfterMs == null }, "the delay belongs to the staircase, not the generator")

        val stops = out.count { it.correctResponseKey == EmotionResponseKey.NONE }
        assertTrue(stops >= 1, "needs ≥1 stop trial")
        assertEquals(20 - stops, out.count { it.correctResponseKey == EmotionResponseKey.PRIMARY })
    }

    /**
     * The affective contrast is a difference between two SSRT estimates, so BOTH tone cells have to exist on the
     * stop trials. A generator that defaulted the tone would leave one cell empty and the delta would be
     * withheld forever — fail-closed, and therefore silent: the reading would simply never appear, and nothing
     * would say it was a design defect rather than a short session.
     */
    @Test
    fun em1_fills_both_tone_cells_of_the_affective_contrast() {
        val stops = EM1PausaGenerator().generate(difficulty, spec, seed = 7L)
            .filter { it.correctResponseKey == EmotionResponseKey.NONE }
            .map { it.payload as EmotionStimulusPayload.AffectiveGoNoGo }
        assertTrue(stops.any { it.negativeWord }, "no negative-tone stop trials: the affective cell is empty")
        assertTrue(stops.any { !it.negativeWord }, "no neutral-tone stop trials: the comparison cell is empty")
    }

    /**
     * Ola 3 · R2 replaced "plants ≥3 hard trials" with its opposite, and the inversion is the point.
     *
     * Planting made SOME trials error-likely and left it to the user whether any errors actually happened, so the
     * strongest users produced no post-error pairs at all and were shown an `unavailable` reading. The staircase
     * holds the error rate at a target instead, which is why the generator now emits ONE uniform difficulty that
     * the runtime overwrites per trial — a planted mix would be a second difficulty fighting the first.
     */
    @Test
    fun em2_emits_one_uniform_difficulty_for_the_staircase_to_overwrite() {
        val out = EM2RecuperacionGenerator().generate(difficulty, spec, seed = 3L)
        assertEquals(20, out.size)
        val payloads = out.map { it.payload as EmotionStimulusPayload.TwoAltPerceptual }
        assertEquals(
            1,
            payloads.map { it.discriminability }.toSet().size,
            "the generator must not plant a difficulty mix",
        )
        out.forEach {
            val p = it.payload as EmotionStimulusPayload.TwoAltPerceptual
            assertTrue(p.correct.isDirectional)
            assertEquals(p.correct, it.correctResponseKey)
        }
    }

    /**
     * The discriminating half: the correct SIDE must still be drawn from the seeded RNG, so it cannot be read off
     * the trial index. Uniform difficulty is not a licence for a uniform answer.
     */
    @Test
    fun em2_still_varies_the_correct_side_across_the_block() {
        val out = EM2RecuperacionGenerator().generate(difficulty, spec, seed = 3L)
        assertEquals(2, out.map { it.correctResponseKey }.toSet().size, "both sides must appear in a 20-trial block")
    }

    @Test
    fun em3_draws_disjoint_training_items_only() {
        val spec3 = EmotionBlockSpec(targetTrialCount = 12, responseWindowMs = 4000)
        val out = EM3SeparadorGenerator().generate(difficulty, spec3, seed = 9L)
        assertEquals(12, out.size)
        val phrases = out.map { (it.payload as EmotionStimulusPayload.Classification).phrase }
        assertEquals(phrases.size, phrases.toSet().size, "no duplicate phrases within a draw")
        out.forEach {
            val p = it.payload as EmotionStimulusPayload.Classification
            assertTrue(!p.isEval, "sessions must draw training items only")
            assertNull(it.correctResponseKey, "classification truth lives in the payload, not a response key")
        }
    }
}
