@file:Suppress("MagicNumber") // Confidence levels are named; 0.0/0..1 are bounds.

package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankIntegrityAuthority
import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.scoring.AdapterOutput
import com.mindos.app.features.mindboost.core.scoring.AntiGaming
import com.mindos.app.features.mindboost.core.scoring.DataQuality
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.InvalidationReason
import com.mindos.app.features.mindboost.core.scoring.MetricStability
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.Sampling
import com.mindos.app.features.mindboost.core.scoring.ScoreInputs
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.scoring.ScoreResultFactory
import com.mindos.app.features.mindboost.core.scoring.SecondaryGate
import com.mindos.app.features.mindboost.core.scoring.Signal
import com.mindos.app.features.mindboost.core.scoring.SignalKey
import com.mindos.app.features.mindboost.core.scoring.SignalUnit
import com.mindos.app.features.mindboost.core.scoring.TimebaseQuality
import com.mindos.app.features.mindboost.core.scoring.TimingCriticality
import com.mindos.app.features.mindboost.core.scoring.UncertaintyLabel

/**
 * Shared ScoreResult derivation for false-alarm-corrected RECOGNITION blocks — the single source of truth
 * that EX4 [MemoryPairsProfileAdapter] and EX3 [DeferredRecallProfileAdapter] delegate to. The recognition
 * index ([signalKey] names the per-exercise signal) is the ONLY profile signal (accuracy-based,
 * `RT_IRRELEVANT` — RT never flows to the profile); the bank integrity is DERIVED from
 * [BankIntegrityAuthority] for the caller's [BankRef] (B-1), never a local literal; the session-level min-n
 * comes from the central [MinSampleGate] (`ACCURACY_RT_BASE`).
 *
 * Gaming defense: the anti-gaming IS the scorer's discrimination-validity gate (a "select-everything"
 * session over-selects lures → INVALIDATED), tested against that synthetic adversary — NOT a separate
 * detector. A caller that has run that tested gate passes `ADEQUATE`; before a live path is wired,
 * [detectorCoverage] defaults to `NONE` so even a perfect session caps at `CALIBRATING`. The index is
 * already a 0..1 hit rate, no scaling.
 */
object RecognitionScoreAdapter {

    private const val PRELIMINARY_CONFIDENCE = 0.6
    private const val LOW_CONFIDENCE = 0.4
    private const val MIN_BLOCKS_FOR_STABILITY = 2

    @Suppress("LongParameterList") // Cohesive scoring inputs + authority-derived bankRef (B-1).
    fun toScoreResult(
        blocks: List<RecognitionBlockResult>,
        signalKey: String,
        provenance: ScoreProvenance,
        bankRef: BankRef,
        detectorCoverage: DetectorCoverage = DetectorCoverage.NONE,
        antiGamingClean: Boolean = true,
    ): ScoreResult {
        val validBlocks = blocks.filter { it.state == RecognitionBlockState.VALID }
        val validTrials = validBlocks.sumOf { it.validTrials }
        val sessionIndex = RecognitionDiscriminationScorer.sessionIndex(blocks)
        val onlyInvalidated =
            validBlocks.isEmpty() && blocks.any { it.state == RecognitionBlockState.INVALIDATED }
        val spec = MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE)
        val confidence =
            if (validBlocks.size >= MIN_BLOCKS_FOR_STABILITY) PRELIMINARY_CONFIDENCE else LOW_CONFIDENCE

        val signal = sessionIndex?.let {
            Signal(
                key = SignalKey(signalKey),
                value = it.coerceIn(0.0, 1.0),
                lowerBound = null,
                upperBound = null,
                unit = SignalUnit.RATIO,
                uncertaintyLabel = UncertaintyLabel.MODERATE,
            )
        }
        val inputs = ScoreInputs(
            criticality = TimingCriticality.RT_IRRELEVANT,
            isEmotionalOrFrustration = false,
            completed = true,
            quality = DataQuality(
                deviceTimingQuality = TimebaseQuality.NOT_TIME_CRITICAL,
                criticalFrameJitter = false,
                frameDropFraction = 0.0,
                itemBankIntegrity = BankIntegrityAuthority.integrityFor(bankRef),
                metricStability = MetricStability.STABLE,
                invalidatedBy = if (onlyInvalidated) InvalidationReason.QUALITY_LOW else null,
            ),
            antiGaming = AntiGaming(isClean = antiGamingClean, detectorCoverage = detectorCoverage),
            sampling = Sampling(
                nEffective = validTrials,
                minNRequired = spec.nMinPerCell,
                confidence = confidence,
                crossSessionAggregated = false,
                confidenceInterval = null,
            ),
            primaryComputable = sessionIndex != null,
            secondaryAccuracyGate = SecondaryGate(passes = true),
        )
        return ScoreResultFactory.from(AdapterOutput(inputs, provenance, signal))
    }
}
