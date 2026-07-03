@file:Suppress("MagicNumber") // Normalization ceiling + confidence levels are named; 0.0/0..1 are bounds.

package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.AdapterOutput
import com.mindos.app.features.mindboost.core.scoring.AntiGaming
import com.mindos.app.features.mindboost.core.scoring.BankIntegrity
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
 * P1 EX1 (V1_01 §9/§12) · Derives the universal [ScoreResult] from complex-span blocks. The span is the
 * ONLY profile signal (accuracy-based, `RT_IRRELEVANT` — RT never flows to the profile); the bank is
 * `PROCEDURAL`; the session-level min-n comes from the central [MinSampleGate].
 *
 * Honest cap (V1_01 §5): with no Memoria anti-gaming detector wired yet, [detectorCoverage] defaults to
 * `NONE`, so even a perfect session resolves to `CALIBRATING` (never `VALID`). When a detector is wired,
 * the caller passes `ADEQUATE` and a sufficient session reaches `VALID`. The min-n table uses
 * `ACCURACY_RT_BASE` (≥20 valid trials) as the central authority; the per-block ≥8 lives in the scorer.
 */
object MemorySpanProfileAdapter {

    private const val MAX_SPAN = 8.0
    private const val PRELIMINARY_CONFIDENCE = 0.6
    private const val LOW_CONFIDENCE = 0.4
    private const val MIN_BLOCKS_FOR_STABILITY = 2

    fun toScoreResult(
        blocks: List<SpanBlockResult>,
        provenance: ScoreProvenance,
        detectorCoverage: DetectorCoverage = DetectorCoverage.NONE,
        antiGamingClean: Boolean = true,
    ): ScoreResult {
        val validBlocks = blocks.filter { it.state == SpanBlockState.VALID }
        val validTrials = validBlocks.sumOf { it.validTrials }
        val sessionSpan = ComplexSpanScorer.sessionSpan(blocks)
        val onlyInvalidated = validBlocks.isEmpty() && blocks.any { it.state == SpanBlockState.INVALIDATED }
        val spec = MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE)
        val confidence =
            if (validBlocks.size >= MIN_BLOCKS_FOR_STABILITY) PRELIMINARY_CONFIDENCE else LOW_CONFIDENCE

        val signal = sessionSpan?.let {
            Signal(
                key = SignalKey("WORKING_MEMORY_SPAN"),
                value = (it / MAX_SPAN).coerceIn(0.0, 1.0),
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
                itemBankIntegrity = BankIntegrity.PROCEDURAL,
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
            primaryComputable = sessionSpan != null,
            secondaryAccuracyGate = SecondaryGate(passes = true),
        )
        return ScoreResultFactory.from(AdapterOutput(inputs, provenance, signal))
    }
}
