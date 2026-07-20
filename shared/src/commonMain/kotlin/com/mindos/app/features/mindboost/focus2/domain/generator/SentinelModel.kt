package com.mindos.app.features.mindboost.focus2.domain.generator

import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Focus V5 · S4 · tarea (1) — the domain types of Tracer B, FOC2-E01 «Centinela» (rare-target vigilance).
 *
 * **A calibrated note on the construct label.** The exercise measures rare-target detection under a signal/noise
 * criterion — a brief go-only continuous-performance task. Classic sustained-attention *decrement* (the vigilance
 * drop-off Mackworth/CPT paradigms show) needs far longer time-on-task than the ~120-trial block here spans, so
 * this build does not yet demonstrate that decrement, and "sustained attention" is a CANDIDATE construct pending
 * the duration/dose validation deferred to S9/V3, not a demonstrated one. Nothing claims it to a user (nothing is
 * mounted, C0 shows no number); the label is kept honest here so a later slice re-scopes it or earns it with data
 * rather than inheriting an overclaim (`MB-FOCUS-0043`).
 *
 * This is a SECOND mechanic on the SAME engine, and its reason for existing is to prove the engine is not
 * quietly the flanker's engine. So these types deliberately mirror the flanker's ([VectorFilterSpec],
 * [PlannedTrial], [VectorFilterPlan]) in shape while sharing nothing but the neutral pieces: the SDT
 * [ConditionId], the seed, and the [FocusExerciseSpec] contract the engine reads. Where the flanker has four
 * cells of a 2x2 and derives its correct answer from a stimulus orientation, vigilance has one condition, a
 * rare-target signal axis, and a ground truth that IS `isSignal` — there is no separate label to drift.
 *
 * Three design facts are load-bearing and are the confound answers the vigilance paradigm demands:
 *
 *  - **Rare targets.** Signal trials are the minority (the ratified 24/96, a 20% base rate). Sparse targets ARE
 *    the paradigm; a task where the target is common measures reaction, not sustained watchfulness. The
 *    consequence — signal and noise cells of very different sizes — is exactly what `MB-FOCUS-0015` proved the
 *    loglinear correction can bias, which is why the scorer derives its degenerate-responding verdict from
 *    counts and not from the magnitude of d'.
 *  - **Unpredictable ISI.** The gap before each trial is jittered per trial, not fixed. A predictable rhythm is
 *    the vigilance task's most obvious exploit: a person who taps on a metronome, blind to the screen, would
 *    score above chance if targets arrived on a beat. Jitter removes the beat, so a rhythmic responder's taps
 *    fall on targets and non-targets alike and land at chance — which the T-ADV suite then demonstrates rather
 *    than asserts.
 *  - **Optional response window.** Vigilance is go-only: the person taps a target and stays silent otherwise,
 *    so silence is a first-class answer (a correct rejection on a non-target), never an omission. The window
 *    still exists — a tap has to land inside it to count — but not answering is the norm, not a gap.
 */

/**
 * The parameters of a vigilance run. Implements [FocusExerciseSpec] so a sealed descriptor can carry it without
 * knowing it is vigilance; the engine reads only [responseWindowMs] and [totalTrials].
 *
 * [signalTrials] and [noiseTrials] are COUNTS, not a rate, for the same reason the flanker's quota is a count
 * (`MB-FOCUS-0017`): a Bernoulli draw at a 20% rate would let the signal cell fall below the d' floor often
 * enough to matter, and when it did the session would produce no measurement for a reason the participant
 * neither sees nor caused. A constructed quota makes the per-cell floor a guarantee.
 */
@Serializable
@SerialName("focus2.SentinelSpec")
data class SentinelSpec(
    /** The rare TARGET count. At the measurement tier this must clear `MinSampleGate`'s d' floor on its own. */
    val signalTrials: Int = DEFAULT_SIGNAL_TRIALS,
    /** The frequent NON-TARGET count. Must also clear the floor, so a false-alarm rate is estimable. */
    val noiseTrials: Int = DEFAULT_NOISE_TRIALS,
    /** The centre of the inter-stimulus gap. The jitter below is added around it, per trial. */
    val baseIsiMs: Long = DEFAULT_BASE_ISI_MS,
    /**
     * Half-width of the uniform jitter applied to each gap. Strictly less than [baseIsiMs] so a drawn gap can
     * never go non-positive. This is the unpredictability that defeats a rhythmic responder.
     */
    val isiJitterMs: Long = DEFAULT_ISI_JITTER_MS,
    /** Response deadline. Generous, because — as in the flanker — latency never governs the score here. */
    override val responseWindowMs: Long = DEFAULT_RESPONSE_WINDOW_MS,
) : FocusExerciseSpec {
    init {
        require(signalTrials > 0) { "signalTrials must be positive, was $signalTrials" }
        require(noiseTrials > 0) { "noiseTrials must be positive, was $noiseTrials" }
        require(baseIsiMs > 0) { "baseIsiMs must be positive, was $baseIsiMs" }
        require(isiJitterMs in 0 until baseIsiMs) {
            "isiJitterMs must be in [0, baseIsiMs) so a jittered gap stays positive, was $isiJitterMs"
        }
        require(responseWindowMs > 0) { "responseWindowMs must be positive, was $responseWindowMs" }
    }

    /** Signal plus noise. There is one condition, so this is the whole run. */
    override val totalTrials: Int get() = signalTrials + noiseTrials

    companion object {
        /**
         * 24 targets + 96 non-targets = 120 trials at a 20% base rate — the vigilance stream the S2 simulation
         * ratified (`SyntheticTrialStreams.E01_TRIALS`). 24 >= `MinSampleGate.specFor(DPRIME).nMinPerCell` (20)
         * and 96 clears it comfortably, so both the hit rate and the false-alarm rate are estimable with margin.
         */
        const val DEFAULT_SIGNAL_TRIALS: Int = 24
        const val DEFAULT_NOISE_TRIALS: Int = 96
        const val DEFAULT_BASE_ISI_MS: Long = 1400L
        const val DEFAULT_ISI_JITTER_MS: Long = 600L

        /** Matches the 1500 ms window the S2 vigilance simulation used, so the sim and the runtime agree. */
        const val DEFAULT_RESPONSE_WINDOW_MS: Long = 1500L
    }
}

/**
 * One planned vigilance trial.
 *
 * Note what is absent that the flanker's [PlannedTrial] has: a derived correct answer. In vigilance the correct
 * behaviour IS the signal axis — tap a target, withhold on a non-target — so [isSignal] is the ground truth and
 * there is nothing to derive and nothing to drift. The coin-flip-label defect the flanker's KDoc warns about
 * cannot occur here because there is no label.
 */
data class PlannedVigilanceTrial(
    val index: Int,
    /** True for a rare TARGET (respond), false for a frequent NON-TARGET (withhold). The SDT signal axis. */
    val isSignal: Boolean,
    /**
     * The RESOLVED inter-stimulus gap for this trial, jittered. Recorded per trial because a session-level ISI
     * cannot describe a run whose gaps were deliberately made unpredictable — the same per-trial-resolved rule
     * the flanker keeps for its difficulty parameters.
     */
    val interStimulusDelayMs: Long,
    val responseWindowMs: Long,
) {
    /** Every trial belongs to the single vigilance condition. */
    val conditionId: ConditionId get() = ConditionId.VIGILANCE
}

/**
 * A generated vigilance plan and everything needed to reproduce it.
 *
 * [seed] travels WITH its `RngVersion` (a bare `Long` loses the algorithm that gives it meaning, learning
 * 2026-07-15); [generatorVersion] travels with it at the layer above.
 */
data class SentinelPlan(
    val exerciseId: String,
    val generatorVersion: Int,
    val seed: SessionSeed,
    val trials: List<PlannedVigilanceTrial>,
) {
    /** Counts per signal/noise cell — the property the planner exists to guarantee. */
    fun cellCounts(): Map<Boolean, Int> = trials.groupingBy { it.isSignal }.eachCount()
}
