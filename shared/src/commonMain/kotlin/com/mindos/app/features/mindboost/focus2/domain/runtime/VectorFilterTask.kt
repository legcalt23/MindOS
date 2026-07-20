package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.TaskSpec

/**
 * Focus V5 · S3 — the classification rules of FOC2-E07.
 *
 * This file used to also hold an ASSEMBLER that joined the plan against a stored list of per-trial responses.
 * It is gone, and its absence is the point: `ADR-FOCUS-DATA` option (i) forbids persisting raw per-trial data,
 * so there is no stored stream left to join. Trials are classified the instant they resolve and only the counts
 * survive (`FocusRuntimeSnapshot.tallies`). Deleting the join was the fix — keeping it would have meant keeping
 * the data it joined.
 */
object VectorFilterTask {

    /**
     * The signal/noise axis is the SIDE the centre element points to, not "is there a target" — a flanker has a
     * target on every trial, so a hit rate built on target-presence would read a stream with no noise trials at
     * all. Choosing the side as the axis is what makes a side bias show up as the criterion `c` rather than
     * hiding inside accuracy.
     *
     * [TaskSpec.primaryCondition] is INCONGRUENT because that is where the interference the exercise is about
     * actually lives; the congruent condition is the comparison, not the finding. Note this is a LEVEL within a
     * condition, never a between-condition subtraction — difference scores were retired as governing metrics.
     */
    val SPEC = TaskSpec(
        taskId = "foc2-e07-filtro-vectorial",
        conditions = listOf(ConditionId.CONGRUENT, ConditionId.INCONGRUENT),
        yesResponse = Response.LEFT,
        noResponse = Response.RIGHT,
        chanceLevel = CHANCE_TWO_ALTERNATIVE,
        primaryCondition = ConditionId.INCONGRUENT,
    )

    private const val CHANCE_TWO_ALTERNATIVE = 0.5
}
