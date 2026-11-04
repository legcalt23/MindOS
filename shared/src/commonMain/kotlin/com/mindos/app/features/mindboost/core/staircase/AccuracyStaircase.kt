package com.mindos.app.features.mindboost.core.staircase

/**
 * Online weighted up/down staircase that holds a task at a TARGET ACCURACY (Kaernbach 1991).
 *
 * [level] is a difficulty parameter expressed as EASINESS — higher is easier — so the rule is: after a correct
 * answer the level drops by [stepDown]; after a wrong one it rises by [stepUp]. The two steps are not free
 * parameters: the process is stationary where the expected move is zero, `p * stepDown == (1 - p) * stepUp`, so
 * [stepUp] is DERIVED as `stepDown * target / (1 - target)`. The convergence point is therefore the target itself
 * rather than a number somebody tuned until it looked right.
 *
 * ## Why the update lags one trial
 *
 * A plain staircase eases the task immediately after every error, which means the trial right after an error is
 * systematically easier than the trial right after a correct one. For a task whose PRIMARY reading is post-error
 * accuracy that is fatal: the exercise would report a recovery it manufactured with its own difficulty knob, and
 * it would report it in the flattering direction. So [onOutcome] applies the PREVIOUS outcome and holds the
 * current one back. The level a trial is presented at is then a function of every outcome up to two trials ago,
 * and carries no information about the outcome immediately before it — post-error and post-correct trials become
 * difficulty-matched in expectation. The fixed point is unchanged; only the variance of the walk grows slightly.
 *
 * The target can be moved MID-SESSION with [retargetTo]. That exists for one purpose: the safety response of a
 * paradigm whose error rate is fixed on purpose. Lowering the target is a smaller induction, and it is applied to
 * the process rather than to any single trial, so no measured trial is ever hand-picked to be easy.
 */
class AccuracyStaircase(
    targetAccuracy: Double,
    private val stepDown: Double,
    initialLevel: Double,
    private val minLevel: Double,
    private val maxLevel: Double,
) {
    init {
        require(targetAccuracy > 0.0 && targetAccuracy < 1.0) { "targetAccuracy must be in (0,1): $targetAccuracy" }
        require(stepDown > 0.0) { "stepDown must be positive: $stepDown" }
        require(minLevel < maxLevel) { "minLevel must be below maxLevel: $minLevel..$maxLevel" }
    }

    /** The accuracy this staircase currently converges on. */
    var targetAccuracy: Double = targetAccuracy
        private set

    /** Current difficulty as EASINESS; the runtime stamps this on the trial it is about to present. */
    var currentLevel: Double = initialLevel.coerceIn(minLevel, maxLevel)
        private set

    /** The outcome held back by the one-trial lag; null before the first response of the session. */
    private var deferred: Boolean? = null

    /** The step applied after a WRONG answer, derived from the target so the fixed point is the target. */
    val stepUp: Double get() = stepDown * targetAccuracy / (1.0 - targetAccuracy)

    /** Records [correct] and applies the outcome held back from the previous trial. */
    fun onOutcome(correct: Boolean) {
        deferred?.let { previous ->
            currentLevel = if (previous) {
                (currentLevel - stepDown).coerceAtLeast(minLevel)
            } else {
                (currentLevel + stepUp).coerceAtMost(maxLevel)
            }
        }
        deferred = correct
    }

    /**
     * Moves the convergence point (safety only — see the class KDoc). The pending outcome is kept: the lag is a
     * measurement property of the walk, not of the target it is walking towards.
     */
    fun retargetTo(target: Double) {
        require(target > 0.0 && target < 1.0) { "targetAccuracy must be in (0,1): $target" }
        targetAccuracy = target
    }
}
