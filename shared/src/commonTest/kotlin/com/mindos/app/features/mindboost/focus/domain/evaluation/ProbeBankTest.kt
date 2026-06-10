package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ProbeBankTest {

    @Test
    fun reservedSeeds_areDisjointFromTrainingSeedSpace() {
        // Every probe form seed is reserved and never a training seed; the bands never overlap.
        ProbeBank.allReservedSeeds.forEach { seed ->
            assertTrue(ProbeSeedSpace.isReserved(seed), "seed $seed must be reserved")
            assertTrue(!ProbeSeedSpace.isTrainingSeed(seed), "seed $seed must not be a training seed")
        }
        assertTrue(ProbeSeedSpace.RESERVED.last < ProbeSeedSpace.MIN_TRAINING_SEED)
    }

    @Test
    fun probeBank_isDisjointFromTrainingCatalogue() {
        // Probes are keyed by their own ids; they never reference a training ExerciseId.
        // The two catalogues share no identifiers (probe ids are strings, exercise ids are enums),
        // and the probe seeds are reserved — so there is no content overlap with training.
        val trainingExerciseNames = ExerciseCatalog.all.keys.map { it.name }.toSet()
        ProbeBank.all.forEach { probe ->
            assertTrue(probe.isReserved)
            assertTrue(probe.probeId !in trainingExerciseNames)
        }
    }

    @Test
    fun probeDefinition_rejectsNonReservedSeeds() {
        assertFailsWith<IllegalArgumentException> {
            ProbeBank.VIGILANCE_PROBE.copy(reservedForms = listOf(ProbeSeedSpace.MIN_TRAINING_SEED))
        }
    }

    @Test
    fun scheduler_rotatesParallelForms_withoutImmediateReuse() = runTest {
        val repo = InMemoryProbeBankRepository(probes = listOf(ProbeBank.VIGILANCE_PROBE))
        val first = repo.nextProbe("user", week = 1)
        val second = repo.nextProbe("user", week = 1)
        assertEquals(ProbeBank.VIGILANCE_PROBE.probeId, first.probe.probeId)
        // Consecutive administrations use different reserved forms (parallel forms).
        assertNotEquals(first.formSeed, second.formSeed)
        assertTrue(ProbeSeedSpace.isReserved(first.formSeed))
    }

    @Test
    fun scheduler_selectsProbeByWeek() = runTest {
        val repo = InMemoryProbeBankRepository()
        val week1 = repo.nextProbe("user", week = 1)
        val week2 = repo.nextProbe("user", week = 2)
        // Different weeks rotate across capacities.
        assertNotEquals(week1.probe.capacity, week2.probe.capacity)
    }
}
