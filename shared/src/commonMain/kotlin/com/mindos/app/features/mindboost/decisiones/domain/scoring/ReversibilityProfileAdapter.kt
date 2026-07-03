@file:Suppress("MagicNumber") // Confidence levels are named; 0.0/0..1 are bounds.

package com.mindos.app.features.mindboost.decisiones.domain.scoring

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
 * E2 Reversibilidad (Decisiones · P1) · Derives the universal [ScoreResult] from reversibility-triage
 * blocks. The REVERSIBILITY_DISCRIMINATION index (balanced accuracy, 0..1) is the ONLY profile signal —
 * accuracy-based, `RT_IRRELEVANT`, so the card's reaction time / deliberation lock never flows to the
 * profile; [bankIntegrity] is caller-supplied (default `PROCEDURAL` for a real generated bank), so the
 * production fold can mark a still-degenerate/unvalidated content source as `SMALL_UNVALIDATED` to keep it
 * out of the profile; the session-level min-n comes from the central [MinSampleGate] (`ACCURACY_RT_BASE`).
 * This is the catalog's HONEST governing metric for E2: how well the user tells reversible (Type-2) from
 * irreversible (Type-1) decisions apart.
 *
 * Gaming defense: like the EX4 pairs / EX3 delayed paths, the anti-gaming IS the scorer's gates (a
 * base-rate guesser is collapsed to chance by the balanced index; a fully-constant responder → INVALIDATED),
 * tested against those synthetic adversaries — NOT a separate detector. A caller that has run that tested
 * gate passes `ADEQUATE`; before a live path is wired, [detectorCoverage] defaults to `NONE` so even a
 * perfect session caps at `CALIBRATING`. The index is already a 0..1 balanced accuracy, no scaling.
 */
object ReversibilityProfileAdapter {

    private const val PRELIMINARY_CONFIDENCE = 0.6
    private const val LOW_CONFIDENCE = 0.4
    private const val MIN_BLOCKS_FOR_STABILITY = 2

    fun toScoreResult(
        blocks: List<ReversibilityBlockResult>,
        provenance: ScoreProvenance,
        detectorCoverage: DetectorCoverage = DetectorCoverage.NONE,
        antiGamingClean: Boolean = true,
        bankIntegrity: BankIntegrity = BankIntegrity.PROCEDURAL,
    ): ScoreResult {
        val validBlocks = blocks.filter { it.state == ReversibilityBlockState.VALID }
        val validTrials = validBlocks.sumOf { it.validTrials }
        val sessionIndex = ReversibilityScorer.sessionIndex(blocks)
        val onlyInvalidated =
            validBlocks.isEmpty() && blocks.any { it.state == ReversibilityBlockState.INVALIDATED }
        val spec = MinSampleGate.specFor(ScoreMetricId.ACCURACY_RT_BASE)
        val confidence =
            if (validBlocks.size >= MIN_BLOCKS_FOR_STABILITY) PRELIMINARY_CONFIDENCE else LOW_CONFIDENCE

        val signal = sessionIndex?.let {
            Signal(
                key = SignalKey("REVERSIBILITY_DISCRIMINATION"),
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
                itemBankIntegrity = bankIntegrity,
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
