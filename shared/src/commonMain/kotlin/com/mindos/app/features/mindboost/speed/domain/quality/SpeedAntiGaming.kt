package com.mindos.app.features.mindboost.speed.domain.quality

import com.mindos.app.features.mindboost.core.antigaming.AntiGamingFramework
import com.mindos.app.features.mindboost.core.antigaming.DetectorSignal
import com.mindos.app.features.mindboost.core.antigaming.GamingAction
import com.mindos.app.features.mindboost.core.antigaming.GamingDetector
import com.mindos.app.features.mindboost.core.antigaming.GamingVerdict
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.antigaming.RhythmTappingDetector
import com.mindos.app.features.mindboost.core.antigaming.SpeedAccuracyHackDetector
import com.mindos.app.features.mindboost.core.antigaming.TooFastToProcessDetector
import com.mindos.app.features.mindboost.core.antigaming.TrialTelemetry
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import kotlin.math.sqrt

/**
 * Velocidad anti-gaming · reuses the INFRA-03 framework, like [com.mindos.app.features.mindboost.memory
 * .domain.scoring.MemorySpanAntiGaming] does for Memoria. It selects the Speed-relevant detectors and feeds
 * them telemetry DERIVED FROM REAL `TrialRecord`s.
 *
 * Selected detectors:
 *  - D1 [RhythmTappingDetector] (WITHHOLD): near-constant inter-response cadence — tapping on a clock. In a
 *    fixed-difficulty MVP-0 block the RT↔difficulty guard is trivially flat (all `difficultyEstimate` equal),
 *    so D1 reduces to the IRI-cadence check; the app's random foreperiod keeps an honest user's IRI variance
 *    well above the threshold.
 *  - D3 [SpeedAccuracyHackDetector] (INVALIDATE): ultra-fast median RT (< 250 ms) at chance accuracy.
 *  - D4 [TooFastToProcessDetector] (WITHHOLD): block anticipation rate above the physiological floor (> 30%).
 *  - [ChanceLevelGuessingDetector] (INVALIDATE): a choice block whose accuracy is not statistically above
 *    chance carries no valid discrimination signal — INVALIDATED regardless of RT. This closes the exploit
 *    band the RT-thresholded D3 leaves open (uniform ~260 ms at chance, or alternating fast/slow holding the
 *    median ≥ 250 ms). Threshold-free (task-intrinsic chance level), so it needs NO §15 physical calibration.
 *  - [PartialFastGuessingDetector] (WITHHOLD): the PARTIAL fast-guesser — fast guesses on a fraction of the
 *    trials plus genuine answers that keep the block above chance overall, dragging the median RT down. It
 *    tests the session's own FASTER HALF against the task-intrinsic chance bound (per-trial correctness),
 *    so it never accuses anyone for merely being fast — only fast-at-coin-flip fires it.
 *
 * Below [MIN_TRIALS_FOR_DETECTION] trials it returns a CLEAN verdict: a rate/median over a tiny sample is not
 * evidence of gaming (small samples are the score machine's `INSUFFICIENT_N` concern, not this one).
 *
 * COVERAGE IS **PARTIAL**, NOT ADEQUATE (an adversarial red-team confirmed the residual gap; the partial
 * fast-guessing band is now narrowed by [PartialFastGuessingDetector], which is task-intrinsic and needs no
 * §15 norm). What still keeps this PARTIAL: (a) a full speed-accuracy-tradeoff analysis (expected accuracy
 * per RT bin against the human choice-RT distribution) requires the §15 physical validation reference, and
 * (b) guessers below the statistical power of the intrinsic bounds (small fast fractions) remain undetected.
 * A future wiring (route Velocidad through `ScoreResultFactory`) must therefore use `DetectorCoverage.PARTIAL`
 * (insufficient for an RT_CRITICAL metric → Velocidad stays CALIBRATING), NEVER `ADEQUATE`.
 *
 * D6 (positional bias) is intentionally NOT included: `TrialRecord` does not persist the LEFT/RIGHT choice,
 * so response-key entropy is not computable for the 2-way exercises without first capturing the chosen side.
 *
 * Pure and device-free; NEVER emits user-facing copy and NEVER writes a low score.
 */
object SpeedAntiGaming {

    /** Below this trial count a rate/median is not evidence of gaming, so the verdict is clean. */
    const val MIN_TRIALS_FOR_DETECTION = 12

    /**
     * This boundary's tested coverage: PARTIAL (it catches blatant gaming but misses subtle fast-guessing —
     * see the class note). A caller routing Velocidad to the profile MUST use this, NEVER `ADEQUATE`, until a
     * §15-calibrated detector exists. Single source of truth so the source can't drift to a stronger claim.
     */
    val COVERAGE = DetectorCoverage.PARTIAL

    private val detectors = listOf(
        RhythmTappingDetector,
        SpeedAccuracyHackDetector,
        TooFastToProcessDetector,
        ChanceLevelGuessingDetector,
    )

    /**
     * [chanceAccuracy] is 0.5 for the 2-way choice exercises. For single-tap E1 (where any response is
     * "correct") no chance logic is meaningful: callers pass `accuracy = 1.0` AND `chanceAccuracy = 0.0`
     * ("no chance concept"), which structurally idles every chance-based gate (D3, the chance gate and the
     * partial fast-guessing gate) while the task-agnostic cadence/anticipation detectors stay live.
     */
    fun evaluate(trials: List<TrialRecord>, accuracy: Double, chanceAccuracy: Double): GamingVerdict {
        if (trials.size < MIN_TRIALS_FOR_DETECTION) return GamingVerdict(GamingAction.NONE, emptyList())
        val telemetry = telemetryFrom(trials, accuracy, chanceAccuracy)
        val signals = detectors.map { it.evaluate(telemetry) } +
            PartialFastGuessingDetector.evaluate(trials, chanceAccuracy)
        return AntiGamingFramework.aggregate(signals)
    }

    private fun telemetryFrom(trials: List<TrialRecord>, accuracy: Double, chanceAccuracy: Double): TrialTelemetry {
        val ordered = trials.sortedBy { it.trialIndex }
        val rtsMs = ordered.map { it.rtNanos / NANOS_PER_MS }
        val iris = ordered.zipWithNext { a, b -> (b.responseUptimeNanos - a.responseUptimeNanos) / NANOS_PER_MS }
        val difficulties = ordered.map { it.difficultyEstimate }
        val anticipations = ordered.count { it.isAnticipation }
        val anticipationRate = if (ordered.isEmpty()) 0.0 else anticipations.toDouble() / ordered.size
        val medianRtMs = medianMs(ordered.filterNot { it.isAnticipation }.map { it.rtNanos / NANOS_PER_MS })
        return TrialTelemetry(
            interResponseIntervalsMs = iris,
            rtsMs = rtsMs,
            difficulties = difficulties,
            accuracy = accuracy,
            chanceAccuracy = chanceAccuracy,
            medianRtMs = medianRtMs,
            anticipationRateInBlock = anticipationRate,
        )
    }

    private fun medianMs(values: List<Long>): Long? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    private const val NANOS_PER_MS = 1_000_000L
}

/**
 * Velocidad fast-guessing gate · closes the exploit band the general [SpeedAccuracyHackDetector] (D3) leaves
 * open. D3 INVALIDATEs only when the median RT is ALSO below the chance floor (250 ms), so uniform ~260 ms
 * guessing — or alternating fast/slow that holds the median ≥ 250 ms — stays at chance yet escapes it.
 *
 * This detector is THRESHOLD-FREE on RT: a choice block whose accuracy is not statistically above chance
 * carries no valid discrimination signal, so it is INVALIDATED however fast (or slow) the responses are. The
 * "consistent with chance" bound is the one-sided upper limit of the chance binomial (using the task-intrinsic
 * chance level, 0.5 for a 2-way task), NOT a population RT norm — so it needs NO §15 physical calibration. It
 * engages only for genuine choice tasks (chanceAccuracy in (0,1)); single-tap E1 (any response correct →
 * accuracy 1.0) is always above chance and never flagged. A fast but ACCURATE reactor (§15 P1-humano: ~177 ms
 * at high accuracy) beats chance comfortably, so speed alone never trips it (no false positive on honest play).
 */
object ChanceLevelGuessingDetector : GamingDetector {
    override val id = "SPEED_CHANCE_GUESSING"

    private const val CHANCE_Z = 1.64 // one-sided ~95% upper bound of the chance binomial
    private const val MIN_CHOICE_TRIALS = 12

    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val chance = telemetry.chanceAccuracy
        val n = telemetry.rtsMs.size
        if (chance <= 0.0 || chance >= 1.0 || n < MIN_CHOICE_TRIALS) return idle()
        val chanceUpperBound = chance + CHANCE_Z * sqrt(chance * (1.0 - chance) / n)
        val notAboveChance = telemetry.accuracy <= chanceUpperBound
        return if (notAboveChance) {
            DetectorSignal(id, true, 1.0, GamingAction.INVALIDATE, "CHANCE_LEVEL_GUESSING")
        } else {
            idle()
        }
    }

    private fun idle() = DetectorSignal(id, false, 0.0, GamingAction.NONE, "OK")
}

/**
 * Velocidad partial fast-guessing gate · narrows the residual band the whole-block detectors leave open: a
 * player who GUESSES a fraction of the trials fast and answers the rest genuinely stays above chance overall
 * (so [ChanceLevelGuessingDetector] is idle) with a median RT dragged toward fast (so D3 is idle) — inflating
 * measured processing speed.
 *
 * Signature it fires on (BOTH must hold, per-trial correctness from real [TrialRecord]s):
 *  1. the session's own FASTER HALF (non-anticipation trials sorted by RT) has accuracy statistically
 *     CONSISTENT WITH CHANCE (one-sided upper bound of the chance binomial — the same task-intrinsic
 *     machinery as [ChanceLevelGuessingDetector], NO §15 population norm), AND
 *  2. the block overall is statistically ABOVE chance (the fully-at-chance case is
 *     [ChanceLevelGuessingDetector]'s INVALIDATE, not this detector's).
 *
 * Never accuses for speed alone: a fast AND accurate half beats the chance bound comfortably, and an honest
 * speed-accuracy tradeoff (fast half slightly less accurate but well above coin-flip) stays clean. Task-aware
 * and fail-closed toward NOT accusing: it engages only for genuine choice tasks (chanceAccuracy in (0,1) —
 * simple-RT E1 never enters), and only with [MIN_FAST_TRIALS]+ trials in the fast half (below that the sample
 * carries no evidence → idle, never an accusation). Action is WITHHOLD_PROFILE (suspicious mixed signal), not
 * INVALIDATE.
 */
object PartialFastGuessingDetector {
    const val ID = "SPEED_PARTIAL_FAST_GUESSING"

    private const val CHANCE_Z = 1.64 // one-sided ~95% bound of the chance binomial (mirrors the chance gate)
    private const val MIN_FAST_TRIALS = 10 // below this the fast-half accuracy is not evidence — stay idle

    fun evaluate(trials: List<TrialRecord>, chanceAccuracy: Double): DetectorSignal {
        val scored = trials.filterNot { it.isAnticipation }.sortedBy { it.rtNanos }
        val fast = scored.take(scored.size / 2)
        return when {
            chanceAccuracy <= 0.0 || chanceAccuracy >= 1.0 -> idle() // not a choice task → never accuse
            fast.size < MIN_FAST_TRIALS -> idle() // insufficient sample → inconclusive, never contaminating
            firesOn(fast, scored, chanceAccuracy) ->
                DetectorSignal(ID, true, 1.0, GamingAction.WITHHOLD_PROFILE, "PARTIAL_FAST_GUESSING")
            else -> idle()
        }
    }

    /** Fast half consistent with chance while the whole block is above chance — the partial-guess signature. */
    private fun firesOn(fast: List<TrialRecord>, scored: List<TrialRecord>, chance: Double): Boolean {
        val fastAtChance = accuracyOf(fast) <= chanceUpperBound(chance, fast.size)
        val overallAboveChance = accuracyOf(scored) > chanceUpperBound(chance, scored.size)
        return fastAtChance && overallAboveChance
    }

    private fun accuracyOf(trials: List<TrialRecord>): Double =
        trials.count { it.isCorrect }.toDouble() / trials.size

    private fun chanceUpperBound(chance: Double, n: Int): Double =
        chance + CHANCE_Z * sqrt(chance * (1.0 - chance) / n)

    private fun idle() = DetectorSignal(ID, false, 0.0, GamingAction.NONE, "OK")
}
