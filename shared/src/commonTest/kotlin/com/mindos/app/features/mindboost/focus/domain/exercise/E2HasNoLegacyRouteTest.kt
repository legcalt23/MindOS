package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The barrier that keeps E2 off the unversioned path — asserted as a property of the TYPES, not by grepping for
 * a name.
 *
 * ## What this replaces, and why the replacement is a different kind of test
 *
 * `E2LegacySeedCallerTest` scanned production sources for files that both name `E2FiltroGenerator` and contain a
 * `.generate(..., seed)` call. It was green for the whole life of the defect it existed to catch. Two live
 * production routes — baseline (`Mode.CALIBRATION`) and the evaluation probe — drew E2's form from the bare-`Long`
 * overload via `FocusFixedBlockRunner`, which resolves its generator polymorphically as a `TrialGenerator` and
 * therefore never contains the string `E2FiltroGenerator`. The scanner's own test file documented that blind spot
 * and called it hypothetical. It was live.
 *
 * The lesson is that a scanner tests the spelling of the code, and the defect was in its types. So this test
 * asserts the type property directly, and the property it asserts is now enforced by the compiler anyway:
 * `VersionedTrialGenerator` does not extend `TrialGenerator`, so `E2FiltroGenerator` cannot be handed to any
 * caller that wants a `TrialGenerator`. The tests below are defence in depth against someone re-introducing the
 * supertype; the build failing is the actual barrier.
 */
class E2HasNoLegacyRouteTest {

    /**
     * The load-bearing one. Upcast to `Any` first so the compiler will accept the check at all — if
     * `E2FiltroGenerator` ever regains `TrialGenerator` as a supertype, this flips red rather than becoming a
     * warning about an impossible `is`.
     */
    @Test
    fun e2_is_not_a_trial_generator_so_no_bare_long_call_can_dispatch_to_it() {
        val generator: Any = E2FiltroGenerator()
        assertFalse(
            generator is TrialGenerator,
            "E2FiltroGenerator implements TrialGenerator again. That re-opens generate(difficulty, spec, " +
                "seed: Long) on it, which is exactly how E2's baseline and probe forms were silently drawn from " +
                "an unversioned RNG: the caller resolves the interface, so neither @Deprecated nor a source " +
                "scanner can see the call. The versioned interface must not extend the unversioned one.",
        )
    }

    /** The registry cannot express the defect either: E2 is not in the map whose value type permits legacy calls. */
    @Test
    fun e2_is_registered_only_as_versioned() {
        assertTrue(ExerciseId.E2_FILTRO in FocusExerciseGenerators.versioned, "E2 must be engine-routable")
        assertFalse(
            ExerciseId.E2_FILTRO in FocusExerciseGenerators.legacy,
            "E2 is in the legacy registry, which types it as a TrialGenerator and hands it to the bare-Long path.",
        )
    }

    /**
     * Generalises the property to every migrated exercise, so exercise #2 through #13 inherit the barrier rather
     * than each needing a bespoke test remembered at migration time.
     */
    @Test
    fun no_versioned_generator_is_reachable_through_the_legacy_interface() {
        FocusExerciseGenerators.versioned.forEach { (id, generator) ->
            val asAny: Any = generator
            assertFalse(
                asAny is TrialGenerator,
                "$id is versioned but still implements TrialGenerator, so it keeps an unversioned route.",
            )
        }
    }

    /** The two registries partition the catalogue: no exercise is both, none is silently dropped. */
    @Test
    fun the_registries_partition_the_catalogue_with_no_overlap_and_no_loss() {
        val overlap = FocusExerciseGenerators.versioned.keys intersect FocusExerciseGenerators.legacy.keys
        assertTrue(overlap.isEmpty(), "$overlap have BOTH a versioned and a legacy route")
        assertEquals(
            FocusExerciseGenerators.versioned.keys + FocusExerciseGenerators.legacy.keys,
            FocusExerciseGenerators.wiredExerciseIds,
            "wiredExerciseIds must be exactly the union, or the catalogue exposes an id with no runnable path",
        )
    }

    /**
     * The migration ledger, asserted rather than documented. 1 of 13 — if a future change migrates an exercise
     * without updating this, the count is wrong and the test says so, which is the point: the number in a report
     * should not be able to drift from the number in the build.
     */
    @Test
    fun the_migration_ledger_is_one_of_thirteen() {
        assertEquals(1, FocusExerciseGenerators.versioned.size, "migrated exercises")
        assertEquals(12, FocusExerciseGenerators.legacy.size, "unmigrated exercises")
        assertEquals(setOf(ExerciseId.E2_FILTRO), FocusExerciseGenerators.versioned.keys)
    }
}
