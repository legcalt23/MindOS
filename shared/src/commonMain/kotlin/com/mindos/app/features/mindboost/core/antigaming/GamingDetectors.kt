@file:Suppress("MagicNumber") // Thresholds are named consts; 0.0/1.0 in pure stats helpers are neutral.

package com.mindos.app.features.mindboost.core.antigaming

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.sqrt

/**
 * INFRA-03 · The 11 pure anti-gaming detectors (D1–D11). Each only counts if a synthetic adversary
 * triggers it AND an honest user does not (see AntiGamingRedTeamTest). D2/D4 mirror signals the existing
 * SsrtCalculator/TrialClassifier already produce (the adapter feeds them in); the rest are new. D6's live
 * wiring into the AGRS `entropyFactor` (replacing the hardcoded `spatialEntropy=1.0` in the decisiones
 * holders) is a per-exercise P1 integration — the detector itself is implemented and tested here.
 *
 * Regla #8: D11 NEVER fires in an emotional/frustration context (abandonment there is a safety signal).
 */

private const val MIN_IRI = 12
private const val MIN_CHOICE = 10
private const val MAX_RHYTHMIC_CV = 0.08
private const val MAX_RT_DIFFICULTY_CORR = 0.10
private const val MIN_WAITING_RESPOND_RATE = 0.25
private const val MAX_SPEED_HACK_ACC_MARGIN = 0.05
private const val MAX_BLOCK_ANTICIPATION = 0.30
private const val MIN_MEMORIZATION_RT_DROP = 0.40
private const val FULL_ACCURACY = 1.0
private const val MAX_MEMORIZATION_ISOMORPH_GAIN = 0.05
private const val MAX_POSITION_ENTROPY_BITS = 0.40
private const val MAX_REEXPOSED_FRACTION = 0.50
private const val MAX_TTR_STUFFING = 0.40
private const val MIN_KEYWORD_DENSITY = 0.30
private const val MIN_REVEAL_FRACTION = 0.50
private const val MAX_MULTITOUCH_FRACTION = 0.15
private const val MIN_ABANDONMENT_CORR = 0.50

private fun idle(detectorId: String) = DetectorSignal(detectorId, false, 0.0, GamingAction.NONE, "OK")

private fun fired(detectorId: String, triggered: Boolean, action: GamingAction, evidence: String): DetectorSignal =
    if (triggered) DetectorSignal(detectorId, true, 1.0, action, evidence) else idle(detectorId)

private fun cv(values: List<Long>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
    return if (mean == 0.0) 0.0 else sqrt(variance) / mean
}

private fun correlation(xs: List<Long>, ys: List<Double>): Double {
    val n = minOf(xs.size, ys.size)
    if (n < 2) return 0.0
    val xMean = xs.take(n).average()
    val yMean = ys.take(n).average()
    var cov = 0.0
    var xVar = 0.0
    var yVar = 0.0
    for (i in 0 until n) {
        val dx = xs[i] - xMean
        val dy = ys[i] - yMean
        cov += dx * dy
        xVar += dx * dx
        yVar += dy * dy
    }
    val denom = sqrt(xVar * yVar)
    return if (denom == 0.0) 0.0 else cov / denom
}

private fun shannonEntropyBits(values: List<Int>): Double {
    if (values.isEmpty()) return 0.0
    val total = values.size.toDouble()
    var entropy = 0.0
    for (count in values.groupingBy { it }.eachCount().values) {
        val p = count / total
        entropy -= p * log2(p)
    }
    return entropy
}

/** D1 · rhythmic tapping: near-constant inter-response intervals AND RT that doesn't track difficulty. */
object RhythmTappingDetector : GamingDetector {
    override val id = "D1_RHYTHM_TAPPING"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        if (telemetry.interResponseIntervalsMs.size < MIN_IRI) return idle(id)
        val corrComputable = telemetry.rtsMs.size >= MIN_IRI && telemetry.rtsMs.size == telemetry.difficulties.size
        val lowCadence = cv(telemetry.interResponseIntervalsMs) < MAX_RHYTHMIC_CV
        val flatCorr = corrComputable &&
            abs(correlation(telemetry.rtsMs, telemetry.difficulties)) < MAX_RT_DIFFICULTY_CORR
        return fired(id, lowCadence && flatCorr, GamingAction.WITHHOLD_PROFILE, "RHYTHMIC_TAP")
    }
}

/** D2 · waiting strategy in stop-signal: response rate below band (wraps the SSRT trustworthy signal). */
object WaitingStrategyDetector : GamingDetector {
    override val id = "D2_WAITING_STRATEGY"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val rate = telemetry.stopRespondRate ?: return idle(id)
        return fired(id, rate < MIN_WAITING_RESPOND_RATE, GamingAction.WITHHOLD_PROFILE, "WAITING_STRATEGY")
    }
}

/** D3 · speed-accuracy hack: ultra-fast median RT with near-chance accuracy. */
object SpeedAccuracyHackDetector : GamingDetector {
    override val id = "D3_SPEED_ACCURACY_HACK"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val median = telemetry.medianRtMs ?: return idle(id)
        val tooFast = median < telemetry.chanceFloorRtMs
        val chanceAcc = telemetry.accuracy <= telemetry.chanceAccuracy + MAX_SPEED_HACK_ACC_MARGIN
        return fired(id, tooFast && chanceAcc, GamingAction.INVALIDATE, "SPEED_ACCURACY_HACK")
    }
}

/** D4 · too fast to process: block anticipation rate above floor (wraps the anticipation classifier). */
object TooFastToProcessDetector : GamingDetector {
    override val id = "D4_TOO_FAST"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val triggered = telemetry.anticipationRateInBlock > MAX_BLOCK_ANTICIPATION
        return fired(id, triggered, GamingAction.WITHHOLD_PROFILE, "TOO_FAST_BLOCK")
    }
}

/** D5 · memorization: RT collapses and accuracy saturates on repeats with no isomorph transfer. */
object MemorizationDetector : GamingDetector {
    override val id = "D5_MEMORIZATION"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val triggered = telemetry.repeatedItemRtDropFraction > MIN_MEMORIZATION_RT_DROP &&
            telemetry.repeatedItemAccuracy >= FULL_ACCURACY &&
            telemetry.isomorphGain < MAX_MEMORIZATION_ISOMORPH_GAIN
        return fired(id, triggered, GamingAction.WITHHOLD_PROFILE, "MEMORIZATION")
    }
}

/** D6 · positional bias: response-key entropy collapses (e.g. "always b"). */
object PositionBiasDetector : GamingDetector {
    override val id = "D6_POSITION_BIAS"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        if (telemetry.responseKeys.size < MIN_CHOICE) return idle(id)
        val lowEntropy = shannonEntropyBits(telemetry.responseKeys) < MAX_POSITION_ENTROPY_BITS
        return fired(id, lowEntropy, GamingAction.INVALIDATE, "POSITION_BIAS")
    }
}

/** D7 · repeated-bank exploit: most of the session is already-served items. */
object RepeatedBankExploitDetector : GamingDetector {
    override val id = "D7_BANK_EXPLOIT"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val triggered = telemetry.reexposedFraction > MAX_REEXPOSED_FRACTION
        return fired(id, triggered, GamingAction.WITHHOLD_PROFILE, "BANK_EXPLOIT")
    }
}

/** D8 · keyword stuffing: low type-token ratio with high keyword density. */
object KeywordStuffingDetector : GamingDetector {
    override val id = "D8_KEYWORD_STUFFING"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val triggered = telemetry.typeTokenRatio < MAX_TTR_STUFFING && telemetry.keywordDensity > MIN_KEYWORD_DENSITY
        return fired(id, triggered, GamingAction.INVALIDATE, "KEYWORD_STUFFING")
    }
}

/** D9 · low-effort reveal: revealed the answer before committing on most trials. */
object LowEffortRevealDetector : GamingDetector {
    override val id = "D9_LOW_EFFORT_REVEAL"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val triggered = telemetry.revealedBeforeCommitFraction > MIN_REVEAL_FRACTION
        return fired(id, triggered, GamingAction.WITHHOLD_PROFILE, "LOW_EFFORT_REVEAL")
    }
}

/** D10 · motor contamination: multi-touch bursts. A QUALITY signal (reclassify), never a fraud penalty. */
object MotorContaminationDetector : GamingDetector {
    override val id = "D10_MOTOR_CONTAMINATION"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        val triggered = telemetry.multiTouchFraction > MAX_MULTITOUCH_FRACTION
        return fired(id, triggered, GamingAction.WITHHOLD_PROFILE, "MOTOR_CONTAMINATION")
    }
}

/** D11 · abandonment manipulation: quitting covaries with difficulty — but NEVER in emotional contexts. */
object AbandonmentManipulationDetector : GamingDetector {
    override val id = "D11_ABANDONMENT_GAMING"
    override fun evaluate(telemetry: TrialTelemetry): DetectorSignal {
        if (telemetry.isEmotionalOrFrustration) return idle(id) // Regla #8: safety signal, never penalized.
        val triggered = telemetry.difficultyAbandonmentCorrelation > MIN_ABANDONMENT_CORR
        return fired(id, triggered, GamingAction.WITHHOLD_PROFILE, "ABANDONMENT_GAMING")
    }
}

/** Canonical registry of the 11 detectors (the framework + red-team suite iterate this). */
object GamingDetectors {
    val all: List<GamingDetector> = listOf(
        RhythmTappingDetector,
        WaitingStrategyDetector,
        SpeedAccuracyHackDetector,
        TooFastToProcessDetector,
        MemorizationDetector,
        PositionBiasDetector,
        RepeatedBankExploitDetector,
        KeywordStuffingDetector,
        LowEffortRevealDetector,
        MotorContaminationDetector,
        AbandonmentManipulationDetector,
    )
}
