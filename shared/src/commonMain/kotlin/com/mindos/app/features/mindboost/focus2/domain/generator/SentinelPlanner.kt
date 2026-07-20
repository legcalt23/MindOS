package com.mindos.app.features.mindboost.focus2.domain.generator

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.SessionSeed

/**
 * Focus V5 · S4 · tarea (1) — the FOC2-E01 «Centinela» vigilance plan generator, v1.
 *
 * The mechanic differs from the flanker in every way that matters to a participant and in none that matters to
 * the engine. Where [VectorFilterPlanner] builds a 2x2 congruency quota and derives a correct side from a
 * stimulus orientation, this builds a one-condition rare-target quota and lets the signal axis BE the ground
 * truth. What the two share is the discipline: counts are CONSTRUCTED, not sampled, and every drawn value comes
 * from the pinned [DeterministicPlanDraw] so the plan is byte-stable across Kotlin versions.
 *
 * Two properties this generator guarantees, both checked below rather than hoped for:
 *
 *  - The signal/noise cell counts are EXACTLY [SentinelSpec.signalTrials] / [SentinelSpec.noiseTrials]. Sparse
 *    targets are the paradigm, and a sparse target drawn by coin flip would sometimes fall below the d' floor —
 *    the `MB-FOCUS-0017` failure mode. A constructed quota makes the floor a guarantee, not an expectation.
 *  - Every trial carries its OWN jittered inter-stimulus gap. Unpredictable timing is the vigilance task's
 *    core confound answer: it is what makes a rhythmic responder's taps fall on targets and non-targets alike.
 *    The jitter is drawn deterministically, so the unpredictability is real to the participant yet perfectly
 *    reproducible for a replay or a golden.
 */
object SentinelPlanner {

    /** Bumped on ANY change to the drawn sequence — the quota layout, the shuffle, or the ISI jitter draw. */
    const val GENERATOR_VERSION: Int = 1

    /** The exercise this planner serves. Focus v2 has its own ids; not a member of the v1 `ExerciseId`. */
    const val EXERCISE_ID: String = "foc2-e01-centinela"

    /**
     * Builds the vigilance plan.
     *
     * Post-conditions the caller may rely on: the signal cell holds exactly [SentinelSpec.signalTrials] trials
     * and the noise cell exactly [SentinelSpec.noiseTrials], and the total is their sum. The draw order is
     * FIXED — the shuffle consumes its draws first, then each trial consumes one draw for its jitter — so the
     * same seed reproduces the same plan down to every gap.
     */
    fun plan(spec: SentinelSpec, seed: SessionSeed): SentinelPlan {
        val rng = DeterministicRng.forSeed(seed)
        val quota = buildQuota(spec)
        val ordered = DeterministicPlanDraw.shuffle(quota, rng)
        val trials = ordered.mapIndexed { index, isSignal ->
            PlannedVigilanceTrial(
                index = index,
                isSignal = isSignal,
                interStimulusDelayMs = jitteredIsi(spec, rng),
                responseWindowMs = spec.responseWindowMs,
            )
        }
        check(trials.size == spec.totalTrials) {
            "planner produced ${trials.size} trials, quota demands ${spec.totalTrials}"
        }
        return SentinelPlan(
            exerciseId = EXERCISE_ID,
            generatorVersion = GENERATOR_VERSION,
            seed = seed,
            trials = trials,
        )
    }

    /** The signal/noise quota, constructed exactly: [SentinelSpec.signalTrials] `true`s then the `false`s. */
    private fun buildQuota(spec: SentinelSpec): List<Boolean> =
        List(spec.signalTrials) { true } + List(spec.noiseTrials) { false }

    /**
     * A gap of `baseIsiMs + uniform(-jitter, +jitter)`, drawn from the pinned bounded draw.
     *
     * The `require` on [SentinelSpec] guarantees `jitter < baseIsiMs`, so the smallest possible gap
     * (`base - jitter`) is still strictly positive — a trial can never be scheduled before the previous one.
     */
    private fun jitteredIsi(spec: SentinelSpec, rng: DeterministicRng): Long {
        val jitter = spec.isiJitterMs
        if (jitter == 0L) return spec.baseIsiMs
        val span = (2 * jitter + 1).toInt()
        val offset = DeterministicPlanDraw.boundedInt(rng, span) - jitter
        return spec.baseIsiMs + offset
    }
}
