@file:Suppress("MagicNumber", "MaxLineLength") // Difficulty levels / block sizes are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * MC8u-B §2: EM3/EM10 difficulty must change the ACTUAL trials (item ambiguity, response window, inter-stimulus gap),
 * never a decorative number, and must never introduce clinical/high-intensity content (it only re-selects the clean,
 * pre-verified bank). Cross-generator invariants pin that every wired generator honors difficulty, keeps a valid trial
 * contract, and stays deterministic for a fixed seed.
 */
class EmotionDifficultyScalingTest {

    private val low = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = 0.0)
    private val high = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = 1.0)
    private val spec = EmotionBlockSpec(targetTrialCount = 20, responseWindowMs = 2000)

    private val ambiguityByPhrase: Map<String, Int> =
        EmotionContent.SEPARATOR_ITEMS.associate { it.phrase to it.ambiguity }
    private val cleanBankPhrases: Set<String> = EmotionContent.SEPARATOR_ITEMS.map { it.phrase }.toSet()

    private fun ambiguousCount(stimuli: List<EmotionStimulus>): Int =
        stimuli.count { ambiguityByPhrase[(it.payload as EmotionStimulusPayload.Classification).phrase] == 1 }

    private fun phrasesAreFromCleanBank(stimuli: List<EmotionStimulus>): Boolean =
        stimuli.isNotEmpty() && stimuli.all { (it.payload as EmotionStimulusPayload.Classification).phrase in cleanBankPhrases }

    // ---- EM3 ---------------------------------------------------------------------------------------------------

    @Test
    fun em3_difficulty_changes_trial_complexity() {
        val gen = EM3SeparadorGenerator()
        val easy = gen.generate(low, spec, seed = 1L)
        val hard = gen.generate(high, spec, seed = 1L)
        assertTrue(hard.first().responseWindowMs < easy.first().responseWindowMs, "higher difficulty tightens the window")
        assertTrue(hard.first().interStimulusDelayMs < easy.first().interStimulusDelayMs, "higher difficulty shortens the reset gap")
    }

    @Test
    fun em3_higher_difficulty_is_not_identical_to_lower() {
        val gen = EM3SeparadorGenerator()
        assertTrue(gen.generate(low, spec, seed = 2L) != gen.generate(high, spec, seed = 2L))
    }

    @Test
    fun em3_high_difficulty_adds_more_categories_or_ambiguity() {
        val gen = EM3SeparadorGenerator()
        assertTrue(ambiguousCount(gen.generate(high, spec, seed = 3L)) > ambiguousCount(gen.generate(low, spec, seed = 3L)))
    }

    @Test
    fun em3_difficulty_never_uses_clinical_or_high_intensity_content() {
        assertTrue(phrasesAreFromCleanBank(EM3SeparadorGenerator().generate(high, spec, seed = 4L)))
    }

    // ---- EM10 --------------------------------------------------------------------------------------------------

    @Test
    fun em10_difficulty_changes_pressure_or_ambiguity() {
        val gen = EM10SepPresionGenerator()
        val easy = gen.generate(low, spec, seed = 5L)
        val hard = gen.generate(high, spec, seed = 5L)
        assertTrue(hard.first().responseWindowMs < easy.first().responseWindowMs, "higher pressure tightens the window further")
    }

    @Test
    fun em10_higher_difficulty_is_not_identical_to_lower() {
        val gen = EM10SepPresionGenerator()
        assertTrue(gen.generate(low, spec, seed = 6L) != gen.generate(high, spec, seed = 6L))
    }

    @Test
    fun em10_high_difficulty_adds_contextual_complexity() {
        val gen = EM10SepPresionGenerator()
        assertTrue(ambiguousCount(gen.generate(high, spec, seed = 7L)) > ambiguousCount(gen.generate(low, spec, seed = 7L)))
    }

    @Test
    fun em10_difficulty_never_uses_clinical_or_high_intensity_content() {
        assertTrue(phrasesAreFromCleanBank(EM10SepPresionGenerator().generate(high, spec, seed = 8L)))
    }

    // ---- Cross-generator ---------------------------------------------------------------------------------------

    @Test
    fun implemented_generators_use_difficulty_parameter() {
        EmotionExerciseGenerators.registry.values.forEach { gen ->
            val easy = gen.generate(low, spec, seed = 11L)
            val hard = gen.generate(high, spec, seed = 11L)
            assertTrue(easy != hard, "${gen.exerciseId} ignores difficulty (same output at level 0 and level 1)")
        }
    }

    @Test
    fun difficulty_scaling_preserves_valid_trial_contract() {
        EmotionExerciseGenerators.registry.values.forEach { gen ->
            val stimuli = gen.generate(high, spec, seed = 12L)
            assertTrue(stimuli.isNotEmpty(), "${gen.exerciseId} produced no trials")
            assertTrue(stimuli.all { it.responseWindowMs > 0 && it.interStimulusDelayMs >= 0 }, "${gen.exerciseId} broke the trial contract")
        }
    }

    @Test
    fun difficulty_scaling_is_deterministic_for_seed_if_seeded() {
        EmotionExerciseGenerators.registry.values.forEach { gen ->
            assertTrue(gen.generate(high, spec, seed = 13L) == gen.generate(high, spec, seed = 13L), "${gen.exerciseId} is not deterministic")
        }
    }
}
