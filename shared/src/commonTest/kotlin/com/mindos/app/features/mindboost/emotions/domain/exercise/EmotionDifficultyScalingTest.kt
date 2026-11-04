@file:Suppress("MagicNumber", "MaxLineLength") // Difficulty levels / block sizes are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MC8u-B §2: separator difficulty must change the ACTUAL trials (item ambiguity, inter-stimulus gap), never a
 * decorative number, and must never introduce clinical/high-intensity content (it only re-selects the clean,
 * pre-verified bank). Cross-generator invariants pin that every wired generator honors difficulty, keeps a valid trial
 * contract, and stays deterministic for a fixed seed.
 *
 * Ola 3 · R3 · P2 removed the response window from that list: it is now the one thing the ambiguity axis may NOT
 * touch, because R3 makes time pressure a mode of its own.
 */
class EmotionDifficultyScalingTest {

    private val low = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = 0.0)
    private val high = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet(), start = 1.0)
    private val spec = EmotionBlockSpec(targetTrialCount = 20, responseWindowMs = 2000)

    // The verified bank is the authored seed PLUS the generated extension. Its mild-intensity guarantee is held
    // by SeparatorItemFactoryTest, which scans every phrase against the clinical lexicon; what this fixture pins
    // is the other half — that a generator only ever serves phrases that came from that verified bank.
    private val ambiguityByPhrase: Map<String, Int> =
        SeparatorItemFactory.ALL.associate { it.phrase to it.ambiguity }
    private val cleanBankPhrases: Set<String> = SeparatorItemFactory.ALL.map { it.phrase }.toSet()

    private fun ambiguousCount(stimuli: List<EmotionStimulus>): Int =
        stimuli.count { ambiguityByPhrase[(it.payload as EmotionStimulusPayload.Classification).phrase] == 1 }

    private fun phrasesAreFromCleanBank(stimuli: List<EmotionStimulus>): Boolean =
        stimuli.isNotEmpty() && stimuli.all { (it.payload as EmotionStimulusPayload.Classification).phrase in cleanBankPhrases }

    // ---- EM3 ---------------------------------------------------------------------------------------------------

    /**
     * Ola 3 · R3 · P2 inverted the window half of this test. It used to assert that a higher `CATEGORY_AMBIGUITY`
     * tightens the response window; R3 makes time pressure a MODE, so the ambiguity axis must leave the calm window
     * alone and the same assertion now runs the other way. The reset-gap half is unchanged — the axis still shortens
     * recovery time, which is what keeps this test from being satisfied by a generator that ignores difficulty.
     */
    @Test
    fun em3_difficulty_changes_trial_complexity_without_touching_the_calm_window() {
        val gen = EM3SeparadorGenerator()
        val easy = gen.generate(low, spec, seed = 1L)
        val hard = gen.generate(high, spec, seed = 1L)
        assertEquals(
            spec.responseWindowMs,
            hard.first().responseWindowMs,
            "difficulty moved the calm window, so pressure is a knob again instead of a mode",
        )
        assertEquals(easy.first().responseWindowMs, hard.first().responseWindowMs)
        assertTrue(hard.first().interStimulusDelayMs < easy.first().interStimulusDelayMs, "higher difficulty shortens the reset gap")
    }

    /**
     * Ola 3 · R3 · P3 — the pressure window scales with the phrase, and the whole point is that it is never a
     * constant. A single number would make the block's difficulty depend on which items a user happened to draw:
     * over this bank the shortest phrase would get almost twice the spare reading time of the longest.
     */
    @Test
    fun the_pressure_window_is_reading_time_per_word_and_never_one_number() {
        val windows = SeparatorItemFactory.ALL.associate { it.phrase to SeparatorDifficulty.pressureWindowMs(it.phrase) }

        assertTrue(windows.values.toSet().size > 1, "every phrase got the same pressure window — that is a constant")
        SeparatorItemFactory.ALL.forEach { item ->
            val words = item.phrase.trim().split(" ").size
            assertEquals(
                SeparatorDifficulty.PRESSURE_BASE_MS + SeparatorDifficulty.PRESSURE_PER_WORD_MS * words,
                windows.getValue(item.phrase),
                "'${item.phrase}' ($words words)",
            )
        }
        // Pressure that is not shorter than calm is not pressure. `spec.responseWindowMs` is the calm window here.
        assertTrue(
            windows.values.max() < spec.responseWindowMs,
            "the longest phrase's pressure window (${windows.values.max()}) is not below the calm window",
        )
    }

    /**
     * The reason the calm window may not vary with difficulty, stated as the fact it rests on: the window is one
     * number for phrases of very different lengths, so any tightening spends a different share of a short item's
     * reading time than of a long one's. Measured over the bank, items run 2 to 7 words.
     */
    @Test
    fun em3_serves_one_window_for_phrases_of_every_length() {
        val stimuli = EM3SeparadorGenerator().generate(high, spec, seed = 5L)
        val words = stimuli.map { (it.payload as EmotionStimulusPayload.Classification).phrase.trim().split(" ").size }

        assertTrue(words.toSet().size > 1, "the block served one phrase length, so this test compares nothing")
        assertEquals(
            setOf(spec.responseWindowMs),
            stimuli.map { it.responseWindowMs }.toSet(),
            "phrases of ${words.min()}..${words.max()} words were served different windows",
        )
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

    // ---- Cross-generator ---------------------------------------------------------------------------------------

    /**
     * Every implemented generator must let the difficulty vector reach the user SOMEWHERE.
     *
     * Ola 3 · R2 added the second place it can land. A generator whose difficulty is resolved online hands the
     * axis to its staircase instead of to the trials it emits, so its `generate` output is legitimately identical
     * at both ends of the axis — and demanding otherwise would push a designer to bake a second, redundant
     * difficulty into the stimuli that the staircase would then immediately walk away from. What must never
     * happen is the axis reaching NEITHER, which is what this asserts.
     */
    @Test
    fun implemented_generators_use_difficulty_parameter() {
        EmotionExerciseGenerators.registry.values.forEach { gen ->
            val variesStimuli = gen.generate(low, spec, seed = 11L) != gen.generate(high, spec, seed = 11L)
            val variesStaircase = (gen as? EmotionAccuracyStaircaseGenerator)?.let {
                it.staircase(low).targetAccuracy != it.staircase(high).targetAccuracy
            } ?: false
            assertTrue(
                variesStimuli || variesStaircase,
                "${gen.exerciseId} ignores difficulty (same trials AND same staircase at level 0 and level 1)",
            )
        }
    }

    /**
     * The discriminating half of the rule above: it pins WHERE R2's difficulty went, so the generic assertion
     * cannot be satisfied by a future edit that quietly drops the axis from one place and adds it to the other.
     * A higher `POST_ERROR_LOAD` means a lower target accuracy — more errors, and therefore longer error runs,
     * which is the only way a staircase-driven task can lengthen an error chain without engineering one.
     */
    @Test
    fun em2_carries_its_difficulty_in_the_staircase_and_not_in_the_trials() {
        val gen = EM2RecuperacionGenerator()
        assertEquals(
            gen.generate(low, spec, seed = 11L),
            gen.generate(high, spec, seed = 11L),
            "EM2 must not bake a second difficulty into the trials the staircase overwrites",
        )
        assertTrue(
            gen.staircase(high).targetAccuracy < gen.staircase(low).targetAccuracy,
            "raising POST_ERROR_LOAD must lower the target accuracy, i.e. ask for more errors",
        )
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

    // MC8u-C §7 mitigation: DIFFERENT seeds (per-session) yield a DIFFERENT item selection, so repeated sessions do not
    // replay the same trials — reducing memorization/false-familiarity across sessions (a formal exposure/repetition
    // guard with cross-session history remains a P2 refinement).
    @Test
    fun different_seeds_yield_different_item_selection_across_sessions() {
        listOf(EM3SeparadorGenerator()).forEach { gen ->
            val phrasesA = gen.generate(low, spec, seed = 100L).map { (it.payload as EmotionStimulusPayload.Classification).phrase }
            val phrasesB = gen.generate(low, spec, seed = 200L).map { (it.payload as EmotionStimulusPayload.Classification).phrase }
            assertTrue(phrasesA != phrasesB, "${gen.exerciseId} draws the same items regardless of session seed (memorization risk)")
        }
    }
}
