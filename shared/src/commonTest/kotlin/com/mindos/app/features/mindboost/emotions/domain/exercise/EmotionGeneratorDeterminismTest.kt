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

    @Test
    fun em1_guarantees_nogo_and_both_congruency_cells() {
        val out = EM1PausaGenerator().generate(difficulty, spec, seed = 7L)
        assertEquals(20, out.size)
        val payloads = out.map { it.payload as EmotionStimulusPayload.AffectiveGoNoGo }
        assertTrue(payloads.count { !it.frameOpen } >= 1, "needs ≥1 no-go")
        assertTrue(payloads.count { it.frameOpen && it.congruent } >= 2, "needs ≥2 congruent GO")
        assertTrue(payloads.count { it.frameOpen && !it.congruent } >= 2, "needs ≥2 incongruent GO")
        out.forEach {
            val p = it.payload as EmotionStimulusPayload.AffectiveGoNoGo
            val expected = if (p.frameOpen) EmotionResponseKey.PRIMARY else EmotionResponseKey.NONE
            assertEquals(expected, it.correctResponseKey)
        }
    }

    @Test
    fun em2_plants_at_least_three_hard_trials_and_directional_answers() {
        val out = EM2RecuperacionGenerator().generate(difficulty, spec, seed = 3L)
        assertEquals(20, out.size)
        val payloads = out.map { it.payload as EmotionStimulusPayload.TwoAltPerceptual }
        assertTrue(payloads.count { it.harder } >= 3, "needs ≥3 planted hard trials for measurable recovery")
        out.forEach {
            val p = it.payload as EmotionStimulusPayload.TwoAltPerceptual
            assertTrue(p.correct.isDirectional)
            assertEquals(p.correct, it.correctResponseKey)
        }
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
