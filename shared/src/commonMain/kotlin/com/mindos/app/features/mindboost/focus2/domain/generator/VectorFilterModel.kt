package com.mindos.app.features.mindboost.focus2.domain.generator

import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Focus V5 · S3 · tarea (1) — the domain types of the Tracer A plan.
 *
 * These are deliberately NOT the v1 `Stimulus` / `StimulusPayload`. That model carries thirteen payload
 * variants and an `isTarget` flag whose meaning differs per paradigm, and `TrialResult.stimulusIsTarget` is a
 * constant `true` for every flanker trial — so anything computing a hit rate against it would read a stream
 * with no noise trials at all. Focus v2 needs a signal/noise axis that MEANS something, so it declares its own.
 */

/**
 * The parameters of one planned block. Everything the generator needs, and nothing it does not.
 *
 * [trialsPerCell] is the load-bearing field: it is a COUNT, not a proportion. See [VectorFilterPlanner] for why
 * a proportion is unsafe here.
 */
@Serializable
@SerialName("focus2.VectorFilterSpec")
data class VectorFilterSpec(
    /** Trials in each of the four (congruency x signal) cells. Must clear the d' floor with margin. */
    val trialsPerCell: Int = DEFAULT_TRIALS_PER_CELL,
    /** How many flankers surround the centre element. A difficulty axis, fixed within a block. */
    val flankerCount: Int = DEFAULT_FLANKER_COUNT,
    /** Target-to-flanker separation, normalised. Higher is easier. A resolved value, recorded per trial. */
    val separation: Double = DEFAULT_SEPARATION,
    /** Target-to-flanker similarity, normalised. Higher is harder. A resolved value, recorded per trial. */
    val similarity: Double = DEFAULT_SIMILARITY,
    /**
     * Response deadline. Generous by design: the dossier lists "Contaminacion de Velocidad" as the first
     * confound and answers it with a deadline that does not punish slowness, because RT never governs here.
     */
    override val responseWindowMs: Long = DEFAULT_RESPONSE_WINDOW_MS,
    val interStimulusDelayMs: Long = DEFAULT_ISI_MS,
) : FocusExerciseSpec {
    init {
        require(trialsPerCell > 0) { "trialsPerCell must be positive, was $trialsPerCell" }
        require(flankerCount > 0) { "flankerCount must be positive, was $flankerCount" }
        require(separation in 0.0..1.0) { "separation must be in [0,1], was $separation" }
        require(similarity in 0.0..1.0) { "similarity must be in [0,1], was $similarity" }
        require(responseWindowMs > 0) { "responseWindowMs must be positive, was $responseWindowMs" }
        require(interStimulusDelayMs >= 0) { "interStimulusDelayMs must be >= 0, was $interStimulusDelayMs" }
    }

    /** Four cells: CONGRUENT/INCONGRUENT x signal/noise. */
    override val totalTrials: Int get() = trialsPerCell * CELL_COUNT

    companion object {
        const val CELL_COUNT: Int = 4

        /**
         * 28 per cell = 112 trials, the count the S2 simulation ratified. It clears
         * `MinSampleGate.specFor(DPRIME).nMinPerCell` (20) with a margin of 8 — and because the quota is
         * constructed rather than sampled, that margin is a guarantee and not an expectation.
         */
        const val DEFAULT_TRIALS_PER_CELL: Int = 28
        const val DEFAULT_FLANKER_COUNT: Int = 4
        const val DEFAULT_SEPARATION: Double = 0.5
        const val DEFAULT_SIMILARITY: Double = 0.5
        const val DEFAULT_RESPONSE_WINDOW_MS: Long = 1200L
        const val DEFAULT_ISI_MS: Long = 600L
    }
}

/**
 * One planned trial.
 *
 * Note what is NOT here: the correct answer. It is [correctResponse], DERIVED from [centrePointsLeft], because
 * the dossier's `solutionValidator` requires exactly that — "la direccion correcta se DERIVA de la orientacion
 * del elemento central generado; nunca se almacena como etiqueta independiente ni aleatoria". A stored label
 * can drift out of agreement with the thing it labels; a derived one cannot. This is the same defect class as
 * the 2026-06-30 finding where a generator drew its correct answer from a coin flip independent of the stimulus.
 */
data class PlannedTrial(
    val index: Int,
    val conditionId: ConditionId,
    /** The centre element points LEFT. By the ratified convention this is also the SIGNAL of the SDT model. */
    val centrePointsLeft: Boolean,
    /** Flankers point the same way as the centre. Redundant with [conditionId] by construction; see [init]. */
    val congruent: Boolean,
    val flankerCount: Int,
    val separation: Double,
    val similarity: Double,
    val responseWindowMs: Long,
    val interStimulusDelayMs: Long,
) {
    init {
        // The condition IS the congruency. Keeping both fields makes the render code readable, but they must
        // never disagree — a trial labelled INCONGRUENT that renders congruent flankers would be scored under
        // the wrong condition and no test of the scorer could ever see it.
        require(congruent == (conditionId == ConditionId.CONGRUENT)) {
            "congruent=$congruent contradicts conditionId=$conditionId at trial $index"
        }
    }

    /** The SDT signal axis: LEFT-pointing centre is "signal". */
    val isSignal: Boolean get() = centrePointsLeft

    /** Derived, never stored. */
    val correctResponse: Response get() = if (centrePointsLeft) Response.LEFT else Response.RIGHT
}

/**
 * A generated plan, and everything needed to reproduce it.
 *
 * [seed] travels WITH its `RngVersion` because a bare `Long` loses the algorithm that gives it meaning
 * (learning 2026-07-15). [generatorVersion] travels with it for the same reason at the layer above.
 */
data class VectorFilterPlan(
    val exerciseId: String,
    val generatorVersion: Int,
    val seed: SessionSeed,
    val trials: List<PlannedTrial>,
) {
    /** Counts per (condition, signal) cell — the property the planner exists to guarantee. */
    fun cellCounts(): Map<Pair<ConditionId, Boolean>, Int> =
        trials.groupingBy { it.conditionId to it.isSignal }.eachCount()
}
