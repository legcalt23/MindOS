package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.data.InMemoryProbeBankRepository
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun probeRelease_givesBackExactlyTheLastBurn_andNeverMore() = runTest {
        val probeId = ProbeBank.VIGILANCE_PROBE.probeId
        val repo = InMemoryProbeBankRepository(probes = listOf(ProbeBank.VIGILANCE_PROBE))

        // Released before the user saw it: the SAME form comes back, so an abort costs no supply.
        val burned = repo.nextProbe("user", week = 1)
        repo.releaseProbe("user", probeId)
        assertEquals(burned.formSeed, repo.nextProbe("user", week = 1).formSeed)

        // ...but exactly ONE burn: a release must not rewind into a form already administered.
        val second = repo.nextProbe("user", week = 1)
        assertNotEquals(burned.formSeed, second.formSeed)
        repo.releaseProbe("user", probeId)
        assertEquals(second.formSeed, repo.nextProbe("user", week = 1).formSeed)

        // A release with no burn behind it is a floor, not a reset: the cursor cannot go negative and
        // hand back a form the user has already seen on a later wrap.
        val fresh = InMemoryProbeBankRepository(probes = listOf(ProbeBank.VIGILANCE_PROBE))
        fresh.releaseProbe("user", probeId)
        assertEquals(ProbeBank.VIGILANCE_PROBE.reservedForms.first(), fresh.nextProbe("user", week = 1).formSeed)
    }

    @Test
    fun scheduler_selectsProbeByWeek() = runTest {
        val repo = InMemoryProbeBankRepository()
        val week1 = repo.nextProbe("user", week = 1)
        val week2 = repo.nextProbe("user", week = 2)
        // Different weeks rotate across capacities.
        assertNotEquals(week1.probe.capacity, week2.probe.capacity)
    }

    @Test
    fun scheduler_declaresTheWrap_whenTheReservedSupplyComesRoundAgain() = runTest {
        val probe = ProbeBank.VIGILANCE_PROBE
        val repo = InMemoryProbeBankRepository(probes = listOf(probe))

        // Every reserved form gets a first showing, and none of them may be reported as a repeat.
        val served = List(probe.reservedForms.size) { repo.nextProbe("user", week = 1) }
        assertTrue(served.none { it.repeatsEarlierForm }, "a first showing must never be reported as a repeat")
        assertEquals(probe.reservedForms.size, served.map { it.formSeed }.toSet().size)

        // The supply is finite, so the next administration comes round to the FIRST form again.
        val wrapped = repo.nextProbe("user", week = 1)
        assertTrue(wrapped.repeatsEarlierForm, "the wrap must be declared, not silent")
        // Discriminating half: the flag is not decorative — it names a form the user has really been given.
        assertEquals(served.first().formSeed, wrapped.formSeed)
    }

    /**
     * T4 of the adversarial audit (MB-FOCUS-0122, R13). The reliability gate opens for the administration
     * whose prior count reaches MIN_ADMINISTRATIONS — administration number MIN + 1, drawing form index
     * MIN — so it is derived from BOTH authorities here: the bank's supply and the estimator's threshold.
     * With the two in phase (supply == MIN) that administration wraps to the baseline's own form, and the
     * first retest that could carry a reliable-change claim is the one on repeated material. Editing either
     * number alone turns this red; that is the point.
     *
     * DECLARED CEILING: the arithmetic here runs on the form-burn cursor, and production opens the gate on a
     * DIFFERENT counter — prior administrations counts only rows admitted as measurable, while every pick
     * burns a form. A withheld run therefore spends supply without moving the gate, so off the all-admitted
     * path the first gated retest CAN still land on repeated material. That stays honest, but by the other
     * half of R13 (the freshness gate withholds) rather than by this supply arithmetic. See MB-FOCUS-0134.
     */
    @Test
    fun theFirstGatedRetestNeverLandsOnTheBaselineForm() = runTest {
        val gateOpensAt = ProbeReliabilityEstimator.MIN_ADMINISTRATIONS + 1
        assertTrue(ProbeBank.all.isNotEmpty(), "No probe: nothing was checked.")
        ProbeBank.all.forEach { probe ->
            val repo = InMemoryProbeBankRepository(probes = listOf(probe))
            val served = List(gateOpensAt) { repo.nextProbe("user", week = 1) }
            val baselineForm = served.first().formSeed
            val firstGated = served.last()
            assertNotEquals(
                baselineForm,
                firstGated.formSeed,
                "${probe.probeId}: the first retest the reliability gate admits (administration $gateOpensAt) " +
                    "must not re-serve the baseline form; supply ${probe.reservedForms.size} vs " +
                    "MIN_ADMINISTRATIONS ${ProbeReliabilityEstimator.MIN_ADMINISTRATIONS}.",
            )
            assertFalse(
                firstGated.repeatsEarlierForm,
                "${probe.probeId}: the first gated retest must be fresh material",
            )

            // Discriminating half: the supply is still finite. Once every form has been given, the next
            // administration wraps and says so — the scheduler is not declaring everything fresh.
            repeat(probe.reservedForms.size - gateOpensAt) {
                assertFalse(repo.nextProbe("user", week = 1).repeatsEarlierForm)
            }
            assertTrue(
                repo.nextProbe("user", week = 1).repeatsEarlierForm,
                "${probe.probeId}: the wrap must still be declared",
            )
        }
    }

    @Test
    fun aReleasedFormWasNeverAdministered_soItDoesNotCountTowardTheWrap() = runTest {
        val probe = ProbeBank.VIGILANCE_PROBE
        val repo = InMemoryProbeBankRepository(probes = listOf(probe))

        // Burned and given back: the user never saw it, so it must not push them toward a wrap.
        repo.nextProbe("user", week = 1)
        repo.releaseProbe("user", probe.probeId)

        val served = List(probe.reservedForms.size) { repo.nextProbe("user", week = 1) }
        assertTrue(served.none { it.repeatsEarlierForm }, "a released burn is not a form the user was given")
        assertTrue(repo.nextProbe("user", week = 1).repeatsEarlierForm)
    }
}
