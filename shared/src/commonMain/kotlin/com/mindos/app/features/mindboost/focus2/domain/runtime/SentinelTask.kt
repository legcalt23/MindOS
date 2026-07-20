package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import com.mindos.app.features.mindboost.focus2.domain.scoring.TaskSpec

/**
 * Focus V5 · S4 · tarea (2) — the classification rule of FOC2-E01 «Centinela», and the whole of the "SUSTAIN
 * scorer" this slice owes.
 *
 * There is no new scorer here, and that absence is the point of Tracer B. `DescriptiveScorer` was built in S3
 * to be paradigm-neutral: it computes accuracy, d', criterion and their uncertainty PER CONDITION, driven
 * entirely by a [TaskSpec]'s yes/no mapping. Vigilance is a different mechanic — one condition, a go-only
 * response, rare targets — but it is not a different MEASUREMENT: hits, false alarms, misses, correct
 * rejections, omissions and anticipations are the same six outcomes, and d' / criterion are the same two
 * statistics. So the sustained-attention scorer is exactly the neutral scorer plus THIS spec, and a second
 * scorer would be a formula with two versions, the one that drifts always being the copy nobody re-reads.
 *
 * What the spec encodes, and why each choice is the honest one for vigilance:
 *
 *  - **yes = TAP, no = NONE.** The task is go-only: a person taps a target and stays silent otherwise. So the
 *    absence of a response IS the "no", and silence on a non-target is a correct rejection — NOT an omission.
 *    Not responding is the norm in this paradigm, and collapsing silence into omission would invent errors out
 *    of the intended behaviour.
 *  - **chanceLevel = null.** A go-only task has no well-defined guessing probability: the chance of a "correct"
 *    non-response depends on how willing the person is to respond at all, which is precisely what criterion
 *    measures. Fabricating a chance level would invent a denominator, so the chance-corrected accuracy branch
 *    is structurally disabled and the vigilance verdict rests on d' and its interval, where it belongs.
 *  - **one condition.** There is no congruency axis to contrast; the finding is the level of sustained
 *    discrimination itself, a within-condition d', never a between-condition subtraction.
 *
 * The two escape outcomes the plan names for the SUSTAIN feedback — omissions and anticipations — are already
 * counted by `SdtCell` and surfaced on every `ConditionResult`. And latency never governs: it touches the
 * primary path only as the anticipation FLOOR, never as a measure, so "sin RT en feedback" holds by the same
 * structural rule the flanker keeps.
 */
object SentinelTask {

    /**
     * Deliberately identical in shape to `SyntheticTrialStreams.E01_SPEC`, the vigilance spec the S2 simulation
     * ratified against adversarial profiles. That spec proved the FORMULA on a 24/96 stream before any engine
     * existed; this is the same spec promoted to the place the runtime and the run-planner read it from, so the
     * S2 oracle and the live tracer classify by one rule, not two.
     */
    val SPEC = TaskSpec(
        taskId = "foc2-e01-centinela",
        conditions = listOf(ConditionId.VIGILANCE),
        yesResponse = Response.TAP,
        noResponse = Response.NONE,
        chanceLevel = null,
        primaryCondition = ConditionId.VIGILANCE,
    )
}
