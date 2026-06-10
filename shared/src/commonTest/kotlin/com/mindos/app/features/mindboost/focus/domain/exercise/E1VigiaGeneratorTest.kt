package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class E1VigiaGeneratorTest {

    private val generator = E1VigiaGenerator()
    private val spec = BlockSpec(targetTrialCount = 40, mode = Mode.TRAINING)

    @Test
    fun generation_isDeterministicForSameSeed() {
        val a = generator.generate(DifficultyVector.EASIEST, spec, seed = 42)
        val b = generator.generate(DifficultyVector.EASIEST, spec, seed = 42)
        assertEquals(a, b)
    }

    @Test
    fun differentSeeds_produceDifferentSequences() {
        val a = generator.generate(DifficultyVector.EASIEST, spec, seed = 1)
        val b = generator.generate(DifficultyVector.EASIEST, spec, seed = 2)
        assertTrue(a != b)
    }

    @Test
    fun producesRequestedCount_andAtLeastOneSignal() {
        val stimuli = generator.generate(DifficultyVector.EASIEST, spec, seed = 7)
        assertEquals(40, stimuli.size)
        assertTrue(stimuli.any { it.isTarget })
    }

    @Test
    fun higherPerceptualLoad_makesTargetsRarerAndLowersContrast() {
        val easy = generator.generate(DifficultyVector.EASIEST, spec, seed = 9)
        val hard = generator.generate(
            DifficultyVector.EASIEST.adjust(DifficultyAxis.PERCEPTUAL_LOAD, AxisLevel(1.0)),
            spec,
            seed = 9,
        )
        val easyTargets = easy.count { it.isTarget }
        val hardTargets = hard.count { it.isTarget }
        // With load maxed, targets are rarer (lower rarity) on the same seed.
        assertTrue(hardTargets <= easyTargets)
        // And contrast drops.
        val hardContrast = (hard.first().payload as StimulusPayload.Vigilance).contrast
        val easyContrast = (easy.first().payload as StimulusPayload.Vigilance).contrast
        assertTrue(hardContrast < easyContrast)
    }
}
