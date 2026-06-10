package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E1VigiaGenerator
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the anti-contamination invariant end-to-end: probe content (reserved
 * seeds) can never be produced by the training seed source, and the probe bank is
 * disjoint from the training catalogue. A regression here would mean a probe could
 * leak into training and invalidate transfer claims.
 */
class ProbeContaminationTest {

    @Test
    fun probeReservedSeeds_neverFallInTrainingSeedSpace() {
        ProbeBank.allReservedSeeds.forEach { seed ->
            assertTrue(ProbeSeedSpace.isReserved(seed))
            assertTrue(!ProbeSeedSpace.isTrainingSeed(seed))
        }
        assertTrue(ProbeSeedSpace.RESERVED.last < ProbeSeedSpace.MIN_TRAINING_SEED)
    }

    @Test
    fun plausibleTrainingSeeds_areNeverReserved() {
        // Training seeds are epoch-millis-scale; sample a range and confirm none is reserved.
        val sampleEpochSeeds = listOf(1_700_000_000_000L, 1_710_000_000_000L, 1_720_000_000_000L, 9_999_999_999_999L)
        sampleEpochSeeds.forEach { seed ->
            assertTrue(ProbeSeedSpace.isTrainingSeed(seed))
            assertTrue(!ProbeSeedSpace.isReserved(seed))
        }
    }

    @Test
    fun probeBankIds_doNotCollideWithTrainingExercises() {
        val trainingNames = ExerciseCatalog.all.keys.map { it.name }.toSet()
        ProbeBank.all.forEach { probe ->
            assertTrue(probe.isReserved)
            assertTrue(probe.probeId !in trainingNames)
        }
    }

    @Test
    fun trainingGenerationUsesItsOwnSeed_notAReservedOne() {
        // A training block generated with a normal seed is independent of the probe bank.
        val trainingSeed = 1_700_000_000_123L
        assertTrue(ProbeSeedSpace.isTrainingSeed(trainingSeed))
        val stimuli = E1VigiaGenerator().generate(
            DifficultyVector.EASIEST,
            BlockSpec(targetTrialCount = 20, mode = Mode.TRAINING),
            seed = trainingSeed,
        )
        assertTrue(stimuli.isNotEmpty())
    }
}
