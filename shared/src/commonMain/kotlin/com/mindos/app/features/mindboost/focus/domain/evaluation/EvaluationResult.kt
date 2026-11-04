package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot

/** Pre/post change of a single metric, with its (possibly gated) RCI verdict. */
data class MetricChange(
    val metric: MetricKey,
    val pre: Double,
    val post: Double,
    val rci: RciResult,
) {
    val rawDelta: Double get() = post - pre
}

/**
 * The result of comparing a retest against the baseline. The strong, honest claim
 * — "a reliable change" — only holds when at least one metric has [RciResult.Value]
 * with `reliable == true`. Everything else is a raw delta shown with caution.
 */
data class EvaluationResult(
    val userId: String,
    val capturedAtMs: Long,
    val changes: List<MetricChange>,
    /**
     * Metrics the retest DID compute and that were withheld because their sample did not reach the
     * ratified floor — never metrics that simply have no floor (`measurementSufficient == null`), and
     * never metrics the retest did not produce at all. Keeping the value out of [changes] is the R8 gate
     * and is unchanged; this records the separate fact that it was kept out, so downstream can tell "we
     * could not measure this" from "this probe does not measure that". Only the second is a fact about
     * the user; presenting the first as the second is what the charter forbids.
     */
    val withheldForSample: List<MetricKey> = emptyList(),
) {
    val hasReliableChange: Boolean
        get() = changes.any { (it.rci as? RciResult.Value)?.reliable == true }
}

/**
 * Compares a retest's metrics against a [BaselineSnapshot]. RCI is computed only for metrics that have BOTH a
 * baseline value, a MEASUREMENT-SUFFICIENT retest value, AND a reliability estimate — otherwise the change is
 * recorded with [RciResult.NotEnoughData] (the gate, 06_ §5).
 *
 * The retest gate is `measurementSufficient`, not `confident`. A reliable change is a claim about the user's
 * capacity, so it must rest on a sample the central `MinSampleGate` ratifies (≥20/cell), never on the 3..6-trial
 * in-session floor that exists to steer difficulty. Under `confident` a congruency effect measured on three
 * trials per condition could have supported one.
 *
 * **Declared limit — the PRE side is not gated.** [BaselineSnapshot.perMetric] persists values only
 * (`Map<String, Double>`), so a `pre` may rest on a sample the central gate would refuse while its `post`
 * cannot. This paragraph used to give two reasons for leaving that alone; both were measured and both are
 * FALSE, so they are corrected here rather than carried.
 *
 * The sample fact is NOT unrecoverable. `DefaultMetricsEngine.blockMetrics` stamps `measurementSufficient`
 * on every value before the block leaves the engine, and `DefaultCalibrationEngine` then DISCARDS it when
 * it maps each entry down to a bare `Double`. It is thrown away here, not missing.
 *
 * Carrying it is NOT "a schema change with a migration". `FocusJson` sets `ignoreUnknownKeys` and
 * `encodeDefaults`, so an additive optional field on the DTO reads correctly in both directions with NO
 * version bump — the in-repo precedent is `SessionSummaryDto.lifecycle` and `.admission`, added exactly
 * that way. A bump would be actively harmful: `FocusSchema.CURRENT` is SHARED with the profile and the
 * session summaries, and `readVersioned` quarantines — deletes — any key whose stored version is newer,
 * so one downgrade would take the baseline, the profile and every session record with it.
 *
 * What does hold is that tightening the producer is not equivalent: `perMetric` has two other live
 * consumers — `ArchetypeInference`, a control-loop input reached from `FocusSessionStateHolder`, and the
 * count `FocusBaselineStateHolder` renders to the user — so the fix is a NEW field, never a narrower
 * filter.
 *
 * And the gap is armed, not firing. On the probe the protocol serves, ACCURACY is the only metric that can
 * clear the retest gate, and its own cell is the trials; an admitted block cannot be short of them, because
 * the anticipation cap and `BlindStrategyCeilingDetector` reject a block long before its correct trials
 * approach the floor. It arms the day the probe is lengthened (`MB-FOCUS-0130`), which
 * `ProbeMeasurementReachabilityTest` already turns RED on. Legacy snapshots carry no sample fact at all,
 * so whoever adds the field decides what an unknown means there — denying it drops those users' comparison,
 * which is a product act and is registered as one instead of being settled here.
 */
object EvaluationComparator {

    fun compare(
        userId: String,
        baseline: BaselineSnapshot,
        retestMetrics: MetricSet,
        reliabilities: Map<MetricKey, ReliabilityEstimate>,
        capturedAtMs: Long,
    ): EvaluationResult {
        val changes = baseline.perMetric.mapNotNull { (metric, pre) ->
            // `measurementSufficient == true` explicitly: `null` (no ratified floor for this metric) must
            // deny, not pass. A `!= false` here would let every ungoverned metric through by default —
            // the permissive default that turns a gate into decoration.
            val retest = retestMetrics[metric]?.takeIf { it.measurementSufficient == true }
                ?: return@mapNotNull null
            val rci = reliabilities[metric]
                ?.let { ReliableChangeIndex.compute(pre = pre, post = retest.value, reliability = it) }
                ?: RciResult.NotEnoughData
            MetricChange(metric = metric, pre = pre, post = retest.value, rci = rci)
        }
        // Computed separately rather than collected inside the fold above, so the claim path stays
        // byte-identical: this cannot change which metrics reach `changes` or whether a change is
        // reliable. `== false` and not `!= true`: `null` is "no ratified floor, the question does not
        // apply", which is not a withheld measurement and must not be reported as one.
        val withheld = baseline.perMetric.keys.filter { retestMetrics[it]?.measurementSufficient == false }
        return EvaluationResult(
            userId = userId,
            capturedAtMs = capturedAtMs,
            changes = changes,
            withheldForSample = withheld,
        )
    }
}
