package com.mindos.app.features.mindboost.core.calibration

import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.SampleVerdict
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import kotlin.math.sqrt

/**
 * Ola 1 · N1 — the three-point strip the user actually sees after a response: *poco seguro / medio / muy seguro*.
 *
 * Three levels is not a compromise, it is the whole point: the layer parasitises an existing task and has ~600 ms
 * of the user's attention, so the scale has to be answerable without deliberation. A finer scale would buy
 * resolution the user cannot supply and would slow the host task, which is the failure mode the design explicitly
 * guards against.
 */
enum class ConfidenceLevel { LOW, MEDIUM, HIGH }

/**
 * ONE metacognitive judgment: how sure the user said they were, and whether they were actually right.
 *
 * [chanceLevel] is the probability of being correct by guessing ON THE HOST TASK, and it is REQUIRED — deliberately
 * with no default. It is the parameter that makes this layer transversal: a four-way classification is right 25 % of
 * the time by luck and a two-alternative choice 50 %, so the SAME spoken confidence means a different subjective
 * probability in each. Defaulting it (to 0.5, or to 0.0) would silently pool incomparable judgments and manufacture
 * a calibration bias out of nothing but which exercise the user happened to pick.
 */
data class CalibrationJudgment(
    val level: ConfidenceLevel,
    val wasCorrect: Boolean,
    val chanceLevel: Double,
) {
    init {
        require(chanceLevel > 0.0 && chanceLevel < 1.0) {
            "chanceLevel is the host task's guess rate and must be in (0,1), was $chanceLevel"
        }
    }

    /**
     * The subjective probability of being correct that [level] stands for on this task.
     *
     * The three levels are read as equal-width bins spanning [chanceLevel]..1.0, and each is represented by its
     * MIDPOINT. Two degenerate alternatives are avoided on purpose: mapping the lowest level to 0.0 would claim the
     * user believes they are certainly wrong (nobody answering a four-way item believes they are below 25 %), and
     * mapping the highest to exactly 1.0 would claim absolute certainty and hand a single mistake the maximum
     * possible Brier penalty. Midpoints also mean no arbitrary hand-tuned constant enters the scale: the whole
     * mapping is derived from the host task's chance level.
     */
    fun statedProbability(): Double {
        val width = (1.0 - chanceLevel) / ConfidenceLevel.entries.size
        val binIndex = ConfidenceLevel.entries.indexOf(level)
        return chanceLevel + width * (binIndex + HALF)
    }

    private companion object {
        const val HALF = 0.5
    }
}

/** A two-sided interval around an estimate. Present only where the sample gate demands one. */
data class ConfidenceInterval(val low: Double, val high: Double) {
    /** True when the interval sits entirely on one side of zero, i.e. the sign of the effect is resolved. */
    fun excludesZero(): Boolean = low > 0.0 || high < 0.0
}

/**
 * What the evidence supports saying about the DIRECTION of a user's miscalibration.
 *
 * [NO_DETECTABLE_BIAS] is named for what it is. "Well calibrated" would be a claim the interval cannot license:
 * failing to resolve the sign of a bias is not the same as demonstrating its absence.
 */
enum class CalibrationVerdict { OVERCONFIDENT, UNDERCONFIDENT, NO_DETECTABLE_BIAS }

/** Accuracy actually achieved inside one confidence level — the raw material of the user-facing feedback. */
data class BucketReading(
    val level: ConfidenceLevel,
    val n: Int,
    /**
     * Mean subjective probability this bucket stands for, or null when the user never used this level. Null and
     * not 0.0: a stated probability of zero would mean the user declared themselves certainly wrong, which is a
     * measurement, whereas never having pressed the button is the absence of one.
     */
    val statedProbability: Double?,
    /** Null when this bucket is under-powered: an accuracy from a handful of trials is noise, not a fact. */
    val accuracy: Double?,
)

/**
 * A calibration reading, or an honest statement that there is not one yet.
 *
 * [Absent] carries how far the user is from a reading rather than a number, because the alternative — emitting a
 * provisional Brier — is exactly the unstable estimate the design warns about, and a low Brier reads as "well
 * calibrated" to every downstream consumer.
 */
sealed interface CalibrationReading {

    data class Absent(val nHave: Int, val nNeeded: Int) : CalibrationReading

    data class Present(
        val n: Int,
        /** Mean squared distance between stated probability and outcome. 0 is perfect; LOWER IS BETTER. */
        val brier: Double,
        /** Signed: positive = certainty running ahead of accuracy. Mean(stated) - Mean(correct). */
        val overconfidence: Double,
        val overconfidenceCi: ConfidenceInterval,
        val verdict: CalibrationVerdict,
        /**
         * Does the user's confidence separate their hits from their misses? Mean stated probability on correct
         * answers minus the same on incorrect ones. Null when either cell is empty — a user who got everything
         * right has no resolution to measure, and 0.0 would read as "cannot tell them apart", which is a different
         * and much worse claim.
         */
        val resolution: Double?,
        val buckets: List<BucketReading>,
    ) : CalibrationReading
}

/**
 * Ola 1 · N1 — the pure calibration kernel. Lives in `core/` and not in emotions on purpose: the audit's design
 * calls the layer transversal, and metacognitive calibration is equally the currency of Decisiones (deciding well
 * IS calibration), Lógica (its ICR scorer already models confidence) and Memoria (judgments of learning).
 * Depending on a feature from here would make that impossible.
 *
 * SAMPLE FLOOR — reconciled authority. The audit design specifies n >= 30 judgments; [MinSampleGate], which
 * declares itself the single source of truth for per-metric minimum samples, specifies 40 for
 * [ScoreMetricId.BRIER] and additionally requires a confidence interval. The gate wins: it is the canonical table,
 * it is the stricter of the two, and hardcoding a local 30 next to a central table that says 40 is precisely the
 * duplicated-threshold failure this repo has been bitten by before. So the floor is READ from the gate, never
 * restated here, and the interval requirement is honoured by refusing to state a direction the interval does not
 * resolve.
 */
object CalibrationKernel {

    /** 95 % two-sided normal quantile. */
    private const val Z_95 = 1.96

    /** Minimum judgments before a single confidence bucket's accuracy may be reported as a fact. */
    private val bucketFloor: Int
        get() = MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE).nMinPerCell

    /** How many judgments a reading needs, read from the canonical table rather than restated. */
    val judgmentsRequired: Int
        get() = MinSampleGate.specFor(ScoreMetricId.BRIER).nMinPerCell

    /**
     * Reads [judgments] — which may span several sessions and several host exercises, since the gate marks Brier
     * as cross-session and every judgment carries its own chance level — into a calibration reading.
     */
    fun read(judgments: List<CalibrationJudgment>): CalibrationReading {
        val verdict = MinSampleGate.evaluate(ScoreMetricId.BRIER, judgments.size, cellsPresent = true)
        return when (verdict) {
            is SampleVerdict.InsufficientData -> CalibrationReading.Absent(verdict.nHave, verdict.nNeeded)
            is SampleVerdict.Sufficient -> present(judgments)
        }
    }

    private fun present(judgments: List<CalibrationJudgment>): CalibrationReading.Present {
        val stated = judgments.map { it.statedProbability() }
        val outcomes = judgments.map { if (it.wasCorrect) 1.0 else 0.0 }
        val deltas = stated.zip(outcomes) { p, o -> p - o }
        val ci = meanInterval(deltas)
        return CalibrationReading.Present(
            n = judgments.size,
            brier = deltas.sumOf { it * it } / judgments.size,
            overconfidence = deltas.average(),
            overconfidenceCi = ci,
            verdict = verdictOf(deltas.average(), ci),
            resolution = resolutionOf(judgments),
            buckets = bucketsOf(judgments),
        )
    }

    /**
     * The direction is stated only when the interval resolves it. This is what honouring
     * `MinSampleGate.requiresConfidenceInterval` means in practice: a point estimate of +0.04 is not evidence of
     * overconfidence, and calling it that would put a claim in front of the user that the data does not carry.
     */
    private fun verdictOf(mean: Double, ci: ConfidenceInterval): CalibrationVerdict = when {
        !ci.excludesZero() -> CalibrationVerdict.NO_DETECTABLE_BIAS
        mean > 0.0 -> CalibrationVerdict.OVERCONFIDENT
        else -> CalibrationVerdict.UNDERCONFIDENT
    }

    /** Mean of [values] with its 95 % interval. Requires n >= 2, which the sample gate already guarantees. */
    private fun meanInterval(values: List<Double>): ConfidenceInterval {
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        val standardError = sqrt(variance / values.size)
        return ConfidenceInterval(mean - Z_95 * standardError, mean + Z_95 * standardError)
    }

    private fun resolutionOf(judgments: List<CalibrationJudgment>): Double? {
        val (hits, misses) = judgments.partition { it.wasCorrect }
        return if (hits.isEmpty() || misses.isEmpty()) {
            null
        } else {
            hits.map { it.statedProbability() }.average() - misses.map { it.statedProbability() }.average()
        }
    }

    private fun bucketsOf(judgments: List<CalibrationJudgment>): List<BucketReading> {
        val floor = bucketFloor
        return ConfidenceLevel.entries.map { level ->
            val inLevel = judgments.filter { it.level == level }
            BucketReading(
                level = level,
                n = inLevel.size,
                statedProbability = if (inLevel.isEmpty()) null else inLevel.map { it.statedProbability() }.average(),
                accuracy = if (inLevel.size >= floor) {
                    inLevel.count { it.wasCorrect }.toDouble() / inLevel.size
                } else {
                    null
                },
            )
        }
    }
}
