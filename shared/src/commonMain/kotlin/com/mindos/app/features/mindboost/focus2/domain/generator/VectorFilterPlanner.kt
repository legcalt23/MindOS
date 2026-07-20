package com.mindos.app.features.mindboost.focus2.domain.generator

import com.mindos.app.features.mindboost.core.runtime.DeterministicRng
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId

/**
 * Focus V5 · S3 · tarea (2) — the FOC2-E07 «Filtro vectorial» plan generator, v2.
 *
 * **Why this does not delegate its draw policy to `E2FiltroGenerator`.** The plan permits reusing the v1
 * flanker "para la GENERACION de estimulos", and the v1 payload shape is indeed reused. Its DRAW POLICY is not,
 * because it is incompatible with the measurement this exercise now performs:
 *
 * ```
 * E2FiltroGenerator.kt:66   val direction = if (random.nextBoolean()) ResponseKey.LEFT else ResponseKey.RIGHT
 * E2FiltroGenerator.kt:67   val congruent = random.nextDouble() >= params.incongruentProportion
 * ```
 *
 * Both are Bernoulli draws, so the per-cell counts are BINOMIAL, not fixed. The ratified E07 design splits 112
 * trials into 2 congruency conditions x 2 signal/noise cells and `MinSampleGate.specFor(DPRIME).nMinPerCell` is
 * 20, leaving a margin of 8 over the expected 28. With independent coin flips a cell falls below 20 often
 * enough to matter, and when it does the scorer correctly returns `Absent(INSUFFICIENT_N)` — so the session
 * produces no measurement at all, for a reason the participant cannot see and did not cause. That is
 * `MB-FOCUS-0017`'s failure mode relocated from the test suite into the runtime.
 *
 * The dossier already requires the fix: "Cuotas balanceadas 50/50 congruente/incongruente y 50/50
 * izquierda/derecha por bloque" and, under `difficulty.parameters`, "proporcion de congruencia (FIJA en
 * measurement)". A quota is CONSTRUCTED and then shuffled; it is not a probability that happens to average out.
 *
 * **Determinism is pinned, not inherited.** The shuffle is written here rather than taken from
 * `List.shuffled(Random)`, and bounded draws come from `nextLong()` rather than `Random.nextInt(bound)`, for
 * the reason recorded on 2026-07-15: the stdlib does not contract those algorithms across Kotlin versions, and
 * a plan that changes shape on a compiler upgrade silently breaks every replay and every golden that depends on
 * it. `DeterministicRng` itself is already pinned splitmix64; this file closes the remaining derived-operation
 * gap so the whole path is stable.
 */
object VectorFilterPlanner {

    /**
     * Bumped on ANY change to the drawn sequence — including a change to the shuffle or to the quota layout.
     * A plan generated under a different version is not comparable and must not be pooled with this one.
     */
    const val GENERATOR_VERSION: Int = 1

    /** The exercise this planner serves. Not an enum member of the v1 `ExerciseId`: Focus v2 has its own ids. */
    const val EXERCISE_ID: String = "foc2-e07-filtro-vectorial"

    /**
     * Builds the trial plan.
     *
     * Post-conditions the caller may rely on, all of them checked below rather than documented and hoped for:
     * every (condition, signal) cell holds exactly [VectorFilterSpec.trialsPerCell] trials, and the total is
     * `4 * trialsPerCell`.
     */
    fun plan(spec: VectorFilterSpec, seed: SessionSeed): VectorFilterPlan {
        val rng = DeterministicRng.forSeed(seed)
        val quota = buildQuota(spec)
        val ordered = DeterministicPlanDraw.shuffle(quota, rng)
        val trials = ordered.mapIndexed { index, cell ->
            PlannedTrial(
                index = index,
                conditionId = cell.condition,
                // "Signal" is the LEFT-pointing centre, by the convention the dossier fixes so that side bias
                // lands in `criterion` instead of being folded into `d'`.
                centrePointsLeft = cell.signal,
                congruent = cell.condition == ConditionId.CONGRUENT,
                flankerCount = spec.flankerCount,
                // The RESOLVED difficulty parameters, recorded per trial. The plan requires "registro por-trial
                // del parametro resuelto": a session-level difficulty label cannot describe a run whose
                // staircase moved, and reconstructing it afterwards from a level index is guesswork.
                separation = spec.separation,
                similarity = spec.similarity,
                responseWindowMs = spec.responseWindowMs,
                interStimulusDelayMs = spec.interStimulusDelayMs,
            )
        }
        check(trials.size == spec.totalTrials) {
            "planner produced ${trials.size} trials, quota demands ${spec.totalTrials}"
        }
        return VectorFilterPlan(
            exerciseId = EXERCISE_ID,
            generatorVersion = GENERATOR_VERSION,
            seed = seed,
            trials = trials,
        )
    }

    /**
     * The 2x2 quota, constructed exactly. This is the whole point of the file: counts are built, not sampled.
     */
    private fun buildQuota(spec: VectorFilterSpec): List<QuotaCell> =
        listOf(ConditionId.CONGRUENT, ConditionId.INCONGRUENT).flatMap { condition ->
            listOf(true, false).flatMap { signal ->
                List(spec.trialsPerCell) { QuotaCell(condition, signal) }
            }
        }

    private data class QuotaCell(val condition: ConditionId, val signal: Boolean)
}
