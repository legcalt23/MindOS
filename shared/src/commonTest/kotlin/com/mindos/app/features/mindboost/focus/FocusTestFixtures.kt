package com.mindos.app.features.mindboost.focus

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricSet
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricUnit
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.ReactionTimeMs
import com.mindos.app.features.mindboost.focus.domain.model.TrialId
import com.mindos.app.features.mindboost.focus.domain.model.TrialMarkers
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult

/** Shared helpers for focus-feature unit tests. */
object FocusTestFixtures {

    @Suppress("LongParameterList") // Test builder mirroring TrialResult's fields.
    fun trial(
        index: Int,
        outcome: TrialOutcome,
        rtMs: Long? = null,
        isTarget: Boolean = outcome == TrialOutcome.HIT || outcome == TrialOutcome.MISS,
        congruent: Boolean? = null,
        requiredStop: Boolean = false,
        stopSignalDelayMs: Long? = null,
        exerciseId: ExerciseId = ExerciseId.E1_VIGIA,
        markers: TrialMarkers = TrialMarkers(congruent = congruent),
    ): TrialResult = TrialResult(
        trialId = TrialId("t$index"),
        exerciseId = exerciseId,
        mode = Mode.TRAINING,
        stimulusIsTarget = isTarget,
        requiredStop = requiredStop,
        outcome = outcome,
        reactionTime = rtMs?.let { ReactionTimeMs(it) },
        difficulty = DifficultyVector.EASIEST,
        timestampMs = 1_000L + index,
        markers = markers,
        stopSignalDelayMs = stopSignalDelayMs,
    )

    /** Builds N consecutive HIT trials with the given RTs. */
    fun hits(rts: List<Long>, startIndex: Int = 0): List<TrialResult> =
        rts.mapIndexed { i, rt -> trial(startIndex + i, TrialOutcome.HIT, rt) }

    fun metricSet(vararg pairs: Pair<MetricKey, Double>): MetricSet =
        pairs.associate { (key, value) ->
            key to MetricValue(key, value, n = 50, confident = true, unit = MetricUnit.RATIO)
        }
}
