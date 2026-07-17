package com.mindos.app.features.mindboost.core.runtime

import kotlin.random.Random

/**
 * A random stream whose algorithm is ours and therefore stays fixed.
 *
 * `kotlin.random.Random(seed)` returns `XorWowRandom`. It is stable in practice, but the standard library does
 * not contractually guarantee which algorithm a seeded `Random` uses, nor that it will keep using it across
 * Kotlin versions. Everything the measurement product needs from determinism rests on that guarantee existing:
 * a golden test that pins a stimulus sequence, a replay of a past session, and a seed-sweep over a generator all
 * silently become different sequences the day the standard library changes its mind. So the stream is pinned
 * here — splitmix64, ~10 lines, no dependency, identical on every platform and every Kotlin version.
 *
 * It extends [Random] so it is a drop-in for any generator that already accepts one: the derived operations
 * (`nextInt`, `nextDouble`, `nextLong(bound)`) come from the standard library's own derivations on top of
 * [nextBits]. Those derivations are the one part not pinned here, which is exactly why
 * `DeterministicRngGoldenTest` pins the *derived* values the generators actually call rather than only the raw
 * bits: if a derivation ever changes, the test fails loudly instead of the sessions changing quietly.
 *
 * Not cryptographic, and not used for anything that needs to be. It is a reproducibility tool.
 */
class DeterministicRng private constructor(private var state: Long) : Random() {

    /** The raw splitmix64 step: mix the counter, never the state, so every draw advances exactly once. */
    private fun next64(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr SHIFT_1)) * MIX_1
        z = (z xor (z ushr SHIFT_2)) * MIX_2
        return z xor (z ushr SHIFT_3)
    }

    override fun nextBits(bitCount: Int): Int {
        require(bitCount in 0..BITS_PER_INT) { "bitCount must be in 0..$BITS_PER_INT, was $bitCount" }
        if (bitCount == 0) return 0
        // Take the high bits: splitmix64's upper bits are its best-mixed ones.
        return (next64() ushr (BITS_PER_LONG - bitCount)).toInt()
    }

    /** One draw instead of the two the default (`nextInt` twice) would spend, and better mixed. */
    override fun nextLong(): Long = next64()

    companion object {
        // splitmix64's published constants. These ARE the algorithm: changing any of them changes every
        // historical session's sequence, which is what DeterministicRngGoldenTest exists to prevent.
        private const val GOLDEN_GAMMA = -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        private const val MIX_1 = -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        private const val MIX_2 = -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        private const val SHIFT_1 = 30
        private const val SHIFT_2 = 27
        private const val SHIFT_3 = 31
        private const val BITS_PER_INT = 32
        private const val BITS_PER_LONG = 64

        /**
         * Opens the stream a [seed] names.
         *
         * Fails closed on a version this build cannot compute: reinterpreting a seed under the wrong algorithm
         * would not reproduce the session, it would silently generate a *different* one and present it as the
         * original. An unreproducible session must be reported as such, never fabricated.
         */
        fun forSeed(seed: SessionSeed): DeterministicRng = when (seed.version) {
            RngVersion.V1 -> DeterministicRng(seed.value)
        }
    }
}
