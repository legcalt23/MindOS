@file:Suppress("MagicNumber", "MaxLineLength") // Block sizes / window thresholds are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ola 3: generators that reuse the three existing renderers (EM8 go/no-go, EM10 classification, EM11 2-alt). */
class EmotionExtraGeneratorsTest {

    private val difficulty = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = 0.4)
    private val spec = EmotionBlockSpec(targetTrialCount = 20, responseWindowMs = 2000)

    @Test
    fun em8_is_deterministic_and_has_a_no_go() {
        val generator = EM8AntiImpulsoGenerator()
        assertEquals(EmotionExerciseId.EM8_ANTIIMPULSO, generator.exerciseId)
        val a = generator.generate(difficulty, spec, 5L)
        assertEquals(a, generator.generate(difficulty, spec, 5L))
        assertEquals(20, a.size)
        val payloads = a.map { it.payload as EmotionStimulusPayload.AffectiveGoNoGo }
        assertTrue(payloads.count { !it.frameOpen } >= 5, "EM8 must have a meaningfully higher no-go share than EM1")
    }

    @Test
    fun em10_is_classification_under_a_shorter_window() {
        val generator = EM10SepPresionGenerator()
        assertEquals(EmotionExerciseId.EM10_SEP_PRESION, generator.exerciseId)
        val out = generator.generate(difficulty, spec, 9L)
        assertEquals(out, generator.generate(difficulty, spec, 9L))
        assertTrue(out.isNotEmpty())
        out.forEach { stimulus ->
            assertTrue(stimulus.payload is EmotionStimulusPayload.Classification)
            assertTrue(stimulus.responseWindowMs < spec.responseWindowMs, "EM10 must apply time pressure (shorter window)")
        }
    }

    @Test
    fun em11_plants_dense_hard_trials_for_post_error_practice() {
        val generator = EM11ContinuidadGenerator()
        assertEquals(EmotionExerciseId.EM11_FEEDBACK_NEG, generator.exerciseId)
        val out = generator.generate(difficulty, spec, 4L)
        assertEquals(out, generator.generate(difficulty, spec, 4L))
        val hard = out.count { (it.payload as EmotionStimulusPayload.TwoAltPerceptual).harder }
        assertTrue(hard >= 4, "EM11 plants more hard trials than EM2 for denser post-error practice; was $hard")
    }
}
