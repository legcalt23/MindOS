package com.mindos.app.features.mindboost.core.runtime

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins [DeterministicRng]'s stream to fixed vectors.
 *
 * The vectors were computed from the published splitmix64 algorithm by an independent BigInt reference, not read
 * back out of the Kotlin implementation — a golden test whose expectations are whatever the subject currently
 * prints is a tautology, not a test. `0xE220A8397B1DCDAF` (seed 0, first draw) is splitmix64's own canonical
 * published vector, which anchors the reference itself.
 *
 * If any assertion here fails, historical sessions no longer replay. That is the point: the failure must be a
 * red build, never a quietly different sequence.
 */
class DeterministicRngGoldenTest {

    private fun rng(seed: Long) = DeterministicRng.forSeed(SessionSeed(seed, RngVersion.V1))

    @Test
    fun v1_stream_matches_the_published_splitmix64_vectors() {
        assertEquals(
            listOf(-2152535657050944081L, 7960286522194355700L, 487617019471545679L, -537132696929009172L),
            rng(0L).let { r -> List(4) { r.nextLong() } },
            "seed 0 stream drifted from published splitmix64",
        )
        assertEquals(
            listOf(-4767286540954276203L, 2949826092126892291L, 5139283748462763858L, 6349198060258255764L),
            rng(42L).let { r -> List(4) { r.nextLong() } },
            "seed 42 stream drifted",
        )
        assertEquals(
            listOf(-1956407806741107680L, -1612297016619662647L, 4048727598324417001L, 7862637804313477842L),
            rng(-1L).let { r -> List(4) { r.nextLong() } },
            "seed -1 (all bits set) stream drifted",
        )
    }

    @Test
    fun v1_first_draw_is_the_canonical_splitmix64_anchor() {
        // 0xE220A8397B1DCDAF — the vector splitmix64 is published with. Anchors the reference, not just us.
        assertEquals(-0x1DDF57C684E23251L, rng(0L).nextLong(), "canonical splitmix64 anchor vector drifted")
    }

    @Test
    fun v1_nextBits_matches_the_reference_derivation() {
        assertEquals(
            listOf(-501176263, 1853398634, 113532184),
            rng(0L).let { r -> List(3) { r.nextBits(32) } },
            "nextBits(32) drifted",
        )
    }

    /**
     * The derived operations come from the standard library on top of `nextBits`, so they are the part this
     * codebase cannot pin by construction. These are the calls the generators actually make, so a change in the
     * standard library's derivation must fail HERE rather than silently re-rolling every session.
     *
     * The first version of this test was a **tautology**: it asserted only that the values were in range and
     * that the same seed agreed with itself, pinned no expected value, and would have passed unchanged if the
     * derivation had changed underneath it — while its KDoc claimed the opposite. An adversarial review proved
     * the counterfactual (swap the stdlib's rejection loop for plain modulo: values change, test still passes).
     *
     * The literals below were derived TWICE independently — by a BigInt reference re-implementing the stdlib's
     * documented derivations over the pinned stream, and by the reviewer — and both agree. They were not read
     * back from Kotlin, which would have made the golden a tautology in a different costume.
     *
     * Call order is part of the contract: each call consumes draws, so these values only hold for exactly this
     * sequence (5×nextInt, then 3×nextDouble, then 3×nextLong) on seed 2026.
     */
    @Test
    fun derived_operations_the_generators_call_are_pinned() {
        val r = rng(2026L)
        assertEquals(listOf(6, 4, 8, 3, 7), List(5) { r.nextInt(10) }, "nextInt(10) derivation drifted")
        assertEquals(
            listOf(0.726404219165592, 0.8045466443128232, 0.23006396495012704),
            List(3) { r.nextDouble() },
            "nextDouble() derivation drifted",
        )
        assertEquals(listOf(0L, 676L, 658L), List(3) { r.nextLong(1000L) }, "nextLong(1000) derivation drifted")
    }

    private fun draw(seed: Long, n: Int): List<Long> = rng(seed).let { r -> List(n) { r.nextLong() } }

    @Test
    fun the_same_seed_reopens_the_same_stream() {
        assertContentEquals(draw(7L, 64), draw(7L, 64), "same seed produced two different streams")
    }

    /**
     * Pins the decorrelation, and corrects the record.
     *
     * An earlier draft justified moving off wall-clock seeds partly by claiming that consecutive seeds produce
     * "near-neighbour" sequences. That claim is false — splitmix64 (like XorWow) scrambles the seed into state,
     * so `t` and `t+1` are unrelated streams. This test asserts the true property rather than the invented one.
     * The real wall-clock defects are same-millisecond collisions and predictability, neither of which this
     * test covers.
     *
     * The first version built `a` as `List(16) { rng(SEED).nextLong() }` — constructing a fresh generator inside
     * the lambda, so `a` was one draw repeated sixteen times while `b` (hoisted, one line below) was a real
     * stream. It passed for the wrong reason. Both sides now use [draw].
     */
    @Test
    fun neighbouring_seeds_do_not_produce_neighbouring_streams() {
        val a = draw(1_000_000L, 16)
        val b = draw(1_000_001L, 16)
        assertNotEquals(a.first(), b.first(), "seed n and n+1 produced the same first draw")
        val shared = a.toSet().intersect(b.toSet()).size
        assertEquals(0, shared, "seed n and n+1 shared $shared of 16 draws")
    }

    @Test
    fun nextBits_zero_is_zero_and_out_of_contract_fails_closed() {
        assertEquals(0, rng(1L).nextBits(0), "nextBits(0) must be 0 per the Random contract")
        val tooMany = runCatching { rng(1L).nextBits(33) }
        assertTrue(tooMany.isFailure, "nextBits(33) must fail, was ${tooMany.getOrNull()}")
        val negative = runCatching { rng(1L).nextBits(-1) }
        assertTrue(negative.isFailure, "nextBits(-1) must fail, was ${negative.getOrNull()}")
    }

    @Test
    fun a_seed_carries_the_version_that_gives_it_meaning() {
        val seed = SessionSeed(99L, RngVersion.V1)
        assertEquals(RngVersion.V1, seed.version, "a seed without its version is not reproducible")
        // Every declared version must be openable; a version that cannot be computed must not exist silently.
        RngVersion.entries.forEach { v ->
            val opened = runCatching { DeterministicRng.forSeed(SessionSeed(1L, v)) }
            assertTrue(opened.isSuccess, "RngVersion.$v is declared but cannot be opened")
        }
    }
}
