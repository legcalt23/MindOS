package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.random.Random

/** Concrete difficulty parameters E1 derives from the relevant axes of the vector. */
data class E1Params(
    val targetRarity: Double,
    val minIsiMs: Long,
    val maxIsiMs: Long,
    val responseWindowMs: Long,
    val contrast: Double,
    val lureProbability: Double,
)

/**
 * Maps the difficulty vector to E1 (Vigía / PVT) parameters. Reads ONLY the axes
 * declared relevant for E1: TIME_ON_TASK, PERCEPTUAL_LOAD, TEMPORAL_UNCERTAINTY,
 * DISTRACTOR_SALIENCE. Other axes are inert.
 *
 * The literals here are the interpolation endpoints of the difficulty curve (the
 * tuning itself, documented by the surrounding comments), so MagicNumber is suppressed.
 */
@Suppress("MagicNumber")
object E1DifficultyMapping {
    fun map(d: DifficultyVector): E1Params {
        val temporal = d.temporalUncertainty.value
        val perceptual = d.perceptualLoad.value
        val salience = d.distractorSalience.value
        return E1Params(
            // Rarer targets are harder (more vigilance required): 30% → 6%.
            targetRarity = lerp(0.30, 0.06, perceptual),
            // Inter-stimulus interval window widens with temporal uncertainty.
            minIsiMs = lerp(1200.0, 2200.0, temporal).toLong(),
            maxIsiMs = lerp(2600.0, 7000.0, temporal).toLong(),
            // Response window tightens slightly with perceptual load.
            responseWindowMs = lerp(1400.0, 950.0, perceptual).toLong(),
            // Contrast degrades (harder to see) with perceptual load: 1.0 → 0.4.
            contrast = lerp(1.0, 0.4, perceptual),
            // Salient lures appear more often when distractor salience rises.
            lureProbability = lerp(0.0, 0.35, salience),
        )
    }
}

/**
 * E1 Vigía generator. Models a PVT/Mackworth vigilance task: long stretches of
 * "nothing", then a rare, temporally-unpredictable signal the user must catch.
 * Lures (near-signals) must be ignored — responding to one is a false alarm.
 *
 * Deterministic: identical (difficulty, spec, seed) → identical sequence.
 */
class E1VigiaGenerator : TrialGenerator {
    override val exerciseId: ExerciseId = ExerciseId.E1_VIGIA

    override fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus> {
        val params = E1DifficultyMapping.map(difficulty)
        val random = Random(seed)
        val count = spec.targetTrialCount
        val stimuli = ArrayList<Stimulus>(count)

        // Guarantee at least one signal so a block is never all-lure/all-blank.
        val forcedSignalIndex = random.nextInt(count)

        for (index in 0 until count) {
            val isSignal = index == forcedSignalIndex || random.nextDouble() < params.targetRarity
            val isLure = !isSignal && random.nextDouble() < params.lureProbability
            val isi = randomInRange(random, params.minIsiMs, params.maxIsiMs)
            stimuli += Stimulus(
                index = index,
                isTarget = isSignal,
                correctResponseKey = if (isSignal) ResponseKey.PRIMARY else null,
                payload = StimulusPayload.Vigilance(
                    isSignal = isSignal,
                    contrast = params.contrast,
                    lure = isLure,
                ),
                interStimulusDelayMs = isi,
                responseWindowMs = params.responseWindowMs,
            )
        }
        return stimuli
    }

    private fun randomInRange(random: Random, minMs: Long, maxMs: Long): Long {
        if (maxMs <= minMs) return minMs
        return minMs + random.nextLong(maxMs - minMs + 1)
    }
}
