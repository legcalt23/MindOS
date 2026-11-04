package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.core.runtime.PlatformSeedSource
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E1VigiaGenerator
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards what the reserved-seed band actually buys: a probe form seed is always inside the band, and no
 * probe id collides with a training exercise.
 *
 * It does NOT guard that a training seed can never equal a probe seed. That guarantee was structural only
 * while the training seed source was the wall clock; production now mints a uniform `Long`, so the ranges
 * can overlap (about 1e-14 per session). See `ProbeSeedSpace` and `MB-FOCUS-0125`.
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
    fun theEpochMillisBand_isDisjointFromTheReservedBand() {
        // A property of the two constants, and only that. These values are chosen by this test, not drawn
        // from the production source, so nothing here is evidence about the seeds the app really mints —
        // see theProductionSeedSourceIsNotConfinedToTheEpochMillisBand for that half.
        val epochScale = listOf(1_700_000_000_000L, 1_710_000_000_000L, 1_720_000_000_000L, 9_999_999_999_999L)
        epochScale.forEach { seed ->
            assertTrue(ProbeSeedSpace.isTrainingSeed(seed))
            assertTrue(!ProbeSeedSpace.isReserved(seed))
        }
    }

    @Test
    fun theProductionSeedSourceIsNotConfinedToTheEpochMillisBand() {
        // The missing half, and the reason it was missing. Every other guard in this file supplies its own
        // epoch-millis-shaped fixture and then asserts a property of that fixture, so all of them stayed
        // green when the producer changed from the wall clock to Random.Default. Sample the producer.
        val minted = List(SEED_SAMPLE) { PlatformSeedSource.mint().value }
        assertTrue(
            minted.any { !ProbeSeedSpace.isTrainingSeed(it) },
            "All $SEED_SAMPLE minted seeds landed in the epoch-millis band. Either the producer went back " +
                "to a wall clock — which is predictable and collides inside one millisecond — or this " +
                "sample is too small: a uniform Long clears the band about half the time.",
        )
        assertTrue(
            minted.any { it < 0L },
            "No minted seed was negative, yet a uniform Long is negative about half the time.",
        )
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

    private companion object {
        /** Either sample assertion would fail spuriously about once in 2^200 runs. */
        const val SEED_SAMPLE = 200
    }
}
