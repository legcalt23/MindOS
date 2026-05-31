package com.mindos.testkit

/**
 * Deterministic, reproducible pseudo-random generator (64-bit LCG). The entire ZIP04 test platform
 * is seeded so any fuzz/property failure can be replayed exactly from its seed — no hidden
 * randomness, consistent with the project's determinism principle.
 */
class SeededRng(seed: Long) {
    private var state: Long = seed xor MULTIPLIER

    fun nextLong(): Long {
        state = state * MULTIPLIER + INCREMENT
        return state
    }

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be > 0" }
        val bits = (nextLong() ushr 33).toInt() and Int.MAX_VALUE
        return bits % bound
    }

    /** Uniform in [min, max). */
    fun nextIntInRange(min: Int, max: Int): Int {
        require(max > min) { "max must be > min" }
        return min + nextInt(max - min)
    }

    fun nextBoolean(): Boolean = nextInt(2) == 0

    /** Uniform in [0.0, 1.0). */
    fun nextDouble(): Double = (nextLong() ushr 11).toDouble() / (1L shl 53).toDouble()

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    /** Derive an independent child stream (deterministic given this stream + salt). */
    fun fork(salt: Long): SeededRng = SeededRng(nextLong() xor salt)

    private companion object {
        const val MULTIPLIER = 6364136223846793005L
        const val INCREMENT = 1442695040888963407L
    }
}
