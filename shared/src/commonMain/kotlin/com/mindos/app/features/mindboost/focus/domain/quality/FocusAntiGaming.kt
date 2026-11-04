package com.mindos.app.features.mindboost.focus.domain.quality

import com.mindos.app.features.mindboost.core.antigaming.AntiGamingFramework
import com.mindos.app.features.mindboost.core.antigaming.DetectorSignal
import com.mindos.app.features.mindboost.core.antigaming.GamingAction
import com.mindos.app.features.mindboost.core.antigaming.GamingVerdict
import com.mindos.app.features.mindboost.core.antigaming.SpeedAccuracyHackDetector
import com.mindos.app.features.mindboost.core.antigaming.TooFastToProcessDetector
import com.mindos.app.features.mindboost.core.antigaming.TrialTelemetry
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.TrialResult
import kotlin.math.max
import kotlin.math.sqrt

/**
 * R15 · Enfoque anti-gaming. Reuses the INFRA-03 framework exactly as `SpeedAntiGaming` and
 * `MemorySpanAntiGaming` already do, and feeds it telemetry DERIVED from the session's own
 * [TrialResult]s — there is no second store, and nothing is passed in that the session does not know.
 *
 * Selected detectors, and why only these three:
 *  - D3 [SpeedAccuracyHackDetector] (INVALIDATE): median RT below the chance floor while accuracy stays at
 *    the blind ceiling. Both inputs are persisted per trial.
 *  - D4 [TooFastToProcessDetector] (WITHHOLD): block anticipation rate above the physiological floor.
 *    `TrialOutcome.ANTICIPATION` is Enfoque's own classification, so this is exact, not an estimate.
 *  - [BlindStrategyCeilingDetector] (INVALIDATE): a block whose accuracy is not statistically above the
 *    BEST BLIND STRATEGY on the schedule it was served carries no discrimination signal. Task-intrinsic and
 *    threshold-free, so it needs no §15 physical calibration.
 *
 * DECLARED CEILING — what is NOT wired, measured rather than assumed:
 *  - D1 (rhythm tapping) needs inter-response intervals, and Enfoque persists only `timestampMs`, whose own
 *    KDoc forbids deriving timing from it (wall-clock, not monotonic). Deriving a cadence from it would be a
 *    timing claim on a clock this model states is not a timing source, so D1 stays out.
 *  - D6 (positional bias) needs the response key, and [TrialResult] does not persist which side was chosen —
 *    the same gap `SpeedAntiGaming` declares for `TrialRecord`.
 *  - The remaining detectors (memorization, item-bank reuse, keyword stuffing, reveal-before-commit,
 *    multi-touch, abandonment) consume signals Enfoque produces nowhere.
 *  - This gate does NOT close `MB-FOCUS-0137`, `0138` or `0141`: one token response in E9, tapping every
 *    trial in E10 and ignoring E8's counter all look like honest play from the outside, and only the
 *    paradigm redesign separates them. Those stay open with their own owners.
 *
 * Coverage is therefore **PARTIAL**, never ADEQUATE, and the value is declared here so no call-site can
 * drift to a stronger claim (`AdequateCoverageInvariantTest` enforces the link to a tested control).
 *
 * Pure and device-free. It NEVER emits user-facing copy and NEVER writes a low score: the only thing a
 * fired detector can do is WITHHOLD the session from measurement.
 */
object FocusAntiGaming {

    /** Below this trial count a rate or a median is not evidence of anything, so the verdict is clean. */
    const val MIN_TRIALS_FOR_DETECTION = 12

    /**
     * This boundary's tested coverage: PARTIAL (see the declared ceiling above). Single source of truth — a
     * consumer asks `FocusAntiGaming.COVERAGE`; it does not write a `DetectorCoverage` literal of its own.
     */
    val COVERAGE = DetectorCoverage.PARTIAL

    /** A schedule with no contrast: every trial in the same class. The chance gates idle instead of firing. */
    internal const val DEGENERATE_CEILING = 1.0

    private val telemetryDetectors = listOf(SpeedAccuracyHackDetector, TooFastToProcessDetector)

    /** The verdict for one session's trials. Fail-closed direction: withhold, never accuse. */
    fun evaluate(trials: List<TrialResult>): GamingVerdict {
        if (trials.size < MIN_TRIALS_FOR_DETECTION) return GamingVerdict(GamingAction.NONE, emptyList())
        val telemetry = telemetryFrom(trials)
        val signals = telemetryDetectors.map { it.evaluate(telemetry) } +
            BlindStrategyCeilingDetector.evaluate(trials)
        return AntiGamingFramework.aggregate(signals)
    }

    /**
     * Telemetry from the trials themselves. `accuracy` uses the SAME rule the metrics engine publishes
     * (correct over non-anticipation trials), so the detector judges the number the session actually
     * reports and not a second, privately-derived one.
     */
    internal fun telemetryFrom(trials: List<TrialResult>): TrialTelemetry {
        val valid = trials.filter { it.outcome != TrialOutcome.ANTICIPATION }
        val correct = trials.count {
            it.outcome == TrialOutcome.HIT || it.outcome == TrialOutcome.CORRECT_REJECTION
        }
        val anticipations = trials.size - valid.size
        return TrialTelemetry(
            rtsMs = trials.mapNotNull { it.reactionTime?.value },
            accuracy = if (valid.isEmpty()) 0.0 else correct.toDouble() / valid.size,
            chanceAccuracy = blindCeiling(trials),
            medianRtMs = median(valid.mapNotNull { it.reactionTime?.value }),
            anticipationRateInBlock = if (trials.isEmpty()) 0.0 else anticipations.toDouble() / trials.size,
        )
    }

    /**
     * The accuracy the best BLIND strategy reaches on this schedule. In a detection task the two blind
     * strategies are "respond to everything" (right on every target) and "respond to nothing" (right on
     * every non-target), so the ceiling is `max(p, 1 - p)` over the served target rate.
     *
     * Returns [DEGENERATE_CEILING] when the schedule serves only targets or only non-targets: there is then
     * no discrimination to be had and no honest bound to test against, so the chance-based gates must idle
     * rather than fire on everyone. That is exactly E10's shape (`MB-FOCUS-0138`), and reading it as a real
     * ceiling of 1.0 would accuse every player of a defect that belongs to the exercise.
     */
    internal fun blindCeiling(trials: List<TrialResult>): Double {
        if (trials.isEmpty()) return DEGENERATE_CEILING
        val targetRate = trials.count { it.stimulusIsTarget }.toDouble() / trials.size
        val ceiling = max(targetRate, 1.0 - targetRate)
        return if (ceiling >= DEGENERATE_CEILING) DEGENERATE_CEILING else ceiling
    }

    private fun median(values: List<Long>): Long? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}

/**
 * R15 · Enfoque discrimination gate. A block whose accuracy is not statistically above the accuracy a BLIND
 * strategy would reach on the same schedule carries no discrimination signal, so it cannot be read as a
 * measure of the person however fast or slow the responses were.
 *
 * The bound is the one-sided upper limit of the blind binomial around [FocusAntiGaming.blindCeiling] — a
 * task-intrinsic quantity read off the served schedule, NOT a population norm, so it needs no §15 physical
 * calibration.
 *
 * Fail-closed toward NOT accusing, in three separate ways: it idles on a degenerate schedule (only targets
 * or only non-targets — there is nothing to discriminate, and the defect there is the exercise's), it idles
 * below [MIN_DISCRIMINATION_TRIALS] (a small sample is the score machine's `INSUFFICIENT_N` concern, not
 * this one), and it never looks at speed, so being fast is never by itself evidence of anything.
 *
 * DECLARED CONSEQUENCE, stated rather than hidden: an honest session that genuinely lands at the blind
 * ceiling is withheld too. That is deliberate and it is not a judgement of the person — a block that does
 * not separate targets from non-targets carries no discrimination signal whoever produced it, so reading it
 * as a measure would be the fabrication. The session is withheld, no low score is written, and the copy says
 * what WE could not conclude, never what the user did.
 */
object BlindStrategyCeilingDetector {
    const val ID = "FOCUS_BLIND_STRATEGY_CEILING"

    /** One-sided ~95% upper bound of the blind binomial (mirrors the Velocidad chance gate). */
    private const val CHANCE_Z = 1.64
    private const val MIN_DISCRIMINATION_TRIALS = 12

    fun evaluate(trials: List<TrialResult>): DetectorSignal {
        val ceiling = FocusAntiGaming.blindCeiling(trials)
        val engaged = ceiling > 0.0 && ceiling < FocusAntiGaming.DEGENERATE_CEILING &&
            trials.size >= MIN_DISCRIMINATION_TRIALS
        return if (engaged && accuracyOf(trials) <= upperBound(ceiling, trials.size)) {
            DetectorSignal(ID, true, 1.0, GamingAction.INVALIDATE, "NOT_ABOVE_BLIND_CEILING")
        } else {
            DetectorSignal(ID, false, 0.0, GamingAction.NONE, "OK")
        }
    }

    private fun accuracyOf(trials: List<TrialResult>): Double =
        FocusAntiGaming.telemetryFrom(trials).accuracy

    private fun upperBound(ceiling: Double, n: Int): Double =
        ceiling + CHANCE_Z * sqrt(ceiling * (1.0 - ceiling) / n)
}
