package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * E2 is the first generator on the versioned RNG.
 *
 * `kotlin.random.Random(seed)` returns `XorWowRandom`, whose algorithm the standard library does not guarantee
 * across versions — so a form built on it silently becomes a different form on a Kotlin upgrade, and neither a
 * golden nor a replay survives. E2 now draws from `DeterministicRng`, whose stream is pinned by
 * `DeterministicRngGoldenTest`.
 *
 * Scope, stated honestly: this is **FORM_REPRODUCTION** only — the same identity yields the same potential
 * stimulus sequence. It is NOT `SESSION_REPLAY`, which additionally needs the trials actually presented, the
 * responses, the lifecycle and relative monotonic timings. The seed alone never reproduces a session.
 */
class E2VersionedRngTest {

    private val generator = E2FiltroGenerator()
    private val spec = BlockSpec(targetTrialCount = 40, mode = Mode.TRAINING)
    private val difficulty = DifficultyVector.EASIEST

    private fun seed(value: Long) = SessionSeed(value, RngVersion.V1)

    @Test
    fun the_same_identity_yields_the_same_form() {
        val a = generator.generate(difficulty, spec, seed(4242L))
        val b = generator.generate(difficulty, spec, seed(4242L))
        assertEquals(a, b, "same (difficulty, spec, seed+version) must yield the identical form")
        assertEquals(40, a.size, "the form must be the requested size")
    }

    @Test
    fun a_different_seed_yields_a_different_form() {
        val a = generator.generate(difficulty, spec, seed(1L))
        val b = generator.generate(difficulty, spec, seed(2L))
        assertNotEquals(a, b, "distinct seeds must yield distinct forms")
    }

    /**
     * The discriminating arm: the form must actually come from the PINNED stream, not from the stdlib's.
     * `DeterministicRng` and `kotlin.random.Random` produce different sequences from the same seed, so if E2
     * had silently stayed on the stdlib these would agree.
     */
    @Test
    fun the_form_comes_from_the_pinned_stream_not_the_stdlib_stream() {
        val pinned = generator.generate(difficulty, spec, seed(99L))
        val stdlibDirections = kotlin.random.Random(99L).let { r -> List(40) { r.nextBoolean() } }
        val pinnedDirections = pinned.map { it.correctResponseKey }

        // If E2 were still calling Random(seed), direction i would track stdlibDirections[i] exactly.
        val agreements = pinnedDirections.indices.count { i ->
            val stdlibSaysLeft = stdlibDirections[i]
            val pinnedSaysLeft = pinnedDirections[i] == com.mindos.app.features.mindboost.focus.domain.model
                .ResponseKey.LEFT
            stdlibSaysLeft == pinnedSaysLeft
        }
        assertTrue(
            agreements < 40,
            "E2's form matched the stdlib stream on all 40 trials — it is still drawing from " +
                "kotlin.random.Random, so the versioned RNG is not actually wired in",
        )
    }

    /**
     * Replaces `the_legacy_long_shim_resolves_to_v1`, which asserted that E2's bare-`Long` overload agreed with
     * its versioned one.
     *
     * That overload no longer exists. It was not merely deprecated — it was the live unversioned route by which
     * E2's baseline (`Mode.CALIBRATION`) and probe forms were actually drawn, and it is now impossible to call
     * because `E2FiltroGenerator` is not a `TrialGenerator`. A test that the two paths agree can no longer be
     * written, which is the strongest form the property can take: the second path is gone rather than checked.
     * `E2HasNoLegacyRouteTest` asserts that absence directly.
     */
    @Test
    fun a_versioned_seed_is_the_only_way_to_draw_e2() {
        val form = generator.generate(difficulty, spec, seed(7L))
        assertEquals(spec.targetTrialCount, form.size)
    }
}
