package com.mindos.app.features.mindboost.focus2.domain.generator

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng

/**
 * Focus V5 · S4 · tarea (1) — the pinned draw operations both plan generators share.
 *
 * Promoted OUT of [VectorFilterPlanner] when Tracer B needed the identical Fisher-Yates and bounded draw. The
 * reason it is shared rather than copied is the one this package states everywhere it pins a sequence: a pinned
 * determinism algorithm duplicated is a sequence with two versions, and the copy that drifts on a Kotlin upgrade
 * is always the one nobody re-reads. One implementation, exercised by BOTH planners' golden tests, is the only
 * form in which "the plan is byte-stable across compiler versions" can stay true.
 *
 * Both operations derive from `DeterministicRng.nextLong()` alone — the single operation this repo has pinned
 * with published golden vectors (learning 2026-07-15) — and never from `Random.nextInt(bound)` or
 * `List.shuffled(Random)`, whose algorithms the stdlib does not contract across versions.
 */
internal object DeterministicPlanDraw {

    /**
     * Fisher-Yates, written out so the permutation is a property of THIS repo and survives a stdlib change.
     *
     * Draws each swap index from [boundedInt], so the whole permutation depends only on the pinned
     * `nextLong()`.
     */
    fun <T> shuffle(source: List<T>, rng: DeterministicRng): List<T> {
        val out = source.toMutableList()
        for (i in out.lastIndex downTo 1) {
            val j = boundedInt(rng, i + 1)
            val tmp = out[i]
            out[i] = out[j]
            out[j] = tmp
        }
        return out
    }

    /**
     * Uniform integer in `[0, bound)` derived only from `nextLong()`, by rejection sampling.
     *
     * A plain `% bound` biases the low values whenever `bound` does not divide the range, and a biased draw
     * would put a detectable structure into whatever it orders. Rejection sampling removes that bias at the
     * cost of an occasional redraw.
     */
    fun boundedInt(rng: DeterministicRng, bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        val limit = Long.MAX_VALUE - (Long.MAX_VALUE % bound) - 1
        var draw: Long
        do {
            draw = rng.nextLong() and Long.MAX_VALUE
        } while (draw > limit)
        return (draw % bound).toInt()
    }
}
