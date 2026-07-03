package com.mindos.app.features.mindboost.core.scoring

import com.mindos.app.features.mindboost.core.scoring.ClaimLevel.NONE
import com.mindos.app.features.mindboost.core.scoring.ClaimLevel.PRACTICE_SIGNAL
import com.mindos.app.features.mindboost.core.scoring.ClaimLevel.TASK_PERFORMANCE
import com.mindos.app.features.mindboost.core.scoring.ScoreState.ABANDONED_SAFE
import com.mindos.app.features.mindboost.core.scoring.ScoreState.BANK_CONTAMINATED
import com.mindos.app.features.mindboost.core.scoring.ScoreState.CALIBRATING
import com.mindos.app.features.mindboost.core.scoring.ScoreState.DEVICE_UNRELIABLE
import com.mindos.app.features.mindboost.core.scoring.ScoreState.INSUFFICIENT_N
import com.mindos.app.features.mindboost.core.scoring.ScoreState.INVALIDATED
import com.mindos.app.features.mindboost.core.scoring.ScoreState.SUSPECTED_GAMING
import com.mindos.app.features.mindboost.core.scoring.ScoreState.VALID
import com.mindos.app.features.mindboost.core.scoring.TimingCriticality.RT_CRITICAL
import com.mindos.app.features.mindboost.core.scoring.TimingCriticality.RT_IRRELEVANT

/** Secondary accuracy gate (e.g. fast RT with chance accuracy is not useful speed). */
data class SecondaryGate(val passes: Boolean)

/** Everything the state machine needs to decide a verdict. */
@Suppress("LongParameterList") // Input bundle for a pure decision function.
data class ScoreInputs(
    val criticality: TimingCriticality,
    val isEmotionalOrFrustration: Boolean,
    val completed: Boolean,
    val quality: DataQuality,
    val antiGaming: AntiGaming,
    val sampling: Sampling,
    val primaryComputable: Boolean,
    val secondaryAccuracyGate: SecondaryGate,
)

/** What an adapter hands to [ScoreResultFactory]: inputs + the would-be signal + record metadata. */
data class AdapterOutput(
    val inputs: ScoreInputs,
    val provenance: ScoreProvenance,
    val primarySignal: Signal?,
    val secondarySignals: List<Signal> = emptyList(),
    val copySafetyFlags: Set<CopySafetyFlag> = emptySet(),
)

/**
 * INFRA-01 · Pure, testable honesty state machine. The FIRST firing gate fixes the state; safety gates
 * (abandonment → timebase → jank → bank → gaming → invalidation) precede evidence gates (computable →
 * n → detector-coverage → secondary-accuracy → stability), which precede `VALID`.
 *
 * Gates are an ordered table of `(state, claim, predicate)`; `evaluate` returns the first match's
 * no-update verdict, else `VALID`. This keeps every function at a single return (Detekt `ReturnCount`)
 * with no long early-return cascade.
 */
object ScoreStateMachine {

    data class Verdict(
        val state: ScoreState,
        val updatesProfile: Boolean,
        val preliminary: Boolean,
        val claimLevel: ClaimLevel,
    )

    private data class Gate(val state: ScoreState, val claim: ClaimLevel, val fires: (ScoreInputs) -> Boolean)

    private const val ROBUST_CONFIDENCE = 0.7
    private const val MAX_FRAME_DROP = 0.10

    fun evaluate(inputs: ScoreInputs): Verdict =
        gates.firstOrNull { gate -> gate.fires(inputs) }
            ?.let { gate -> noUpdate(gate.state, gate.claim) }
            ?: validVerdict(inputs)

    private val gates: List<Gate> = listOf(
        Gate(ABANDONED_SAFE, NONE) { !it.completed && it.isEmotionalOrFrustration },
        Gate(DEVICE_UNRELIABLE, PRACTICE_SIGNAL) { it.criticality == RT_CRITICAL && !timebaseTrusted(it.quality) },
        Gate(DEVICE_UNRELIABLE, PRACTICE_SIGNAL) { it.criticality != RT_IRRELEVANT && jankExcessive(it.quality) },
        Gate(BANK_CONTAMINATED, PRACTICE_SIGNAL) { bankContaminated(it.quality.itemBankIntegrity) },
        Gate(SUSPECTED_GAMING, NONE) { !it.antiGaming.isClean },
        Gate(INVALIDATED, NONE) { it.quality.invalidatedBy != null },
        Gate(CALIBRATING, PRACTICE_SIGNAL) { !it.primaryComputable },
        Gate(INSUFFICIENT_N, PRACTICE_SIGNAL) { !it.sampling.meetsMinN },
        Gate(CALIBRATING, PRACTICE_SIGNAL) { !it.antiGaming.detectorCoverage.adequateFor(it.criticality) },
        Gate(CALIBRATING, PRACTICE_SIGNAL) { it.criticality == RT_CRITICAL && !it.secondaryAccuracyGate.passes },
        Gate(CALIBRATING, TASK_PERFORMANCE) { it.quality.metricStability == MetricStability.UNSTABLE },
    )

    private fun validVerdict(inputs: ScoreInputs): Verdict {
        val robust = inputs.sampling.confidence >= ROBUST_CONFIDENCE && inputs.sampling.crossSessionAggregated
        return Verdict(
            state = VALID,
            updatesProfile = true,
            preliminary = !robust,
            claimLevel = if (robust) ClaimLevel.WITHIN_TASK_TREND else TASK_PERFORMANCE,
        )
    }

    private fun noUpdate(state: ScoreState, claim: ClaimLevel) = Verdict(state, false, true, claim)
    private fun timebaseTrusted(q: DataQuality) = q.deviceTimingQuality == TimebaseQuality.AUTHORITATIVE
    private fun jankExcessive(q: DataQuality) = q.criticalFrameJitter || q.frameDropFraction > MAX_FRAME_DROP
    // A bank that is contaminated, exhausted, OR small/unvalidated cannot back a profile signal
    // (the charter invariant "small/unvalidated bank blocks profile" enforced at runtime, not only by
    // INFRA-04's build-time BankValidator). SMALL_UNVALIDATED is how a degenerate/unvalidated content
    // source (e.g. a placeholder generator) honestly stays out of the profile until a real bank exists.
    private fun bankContaminated(b: BankIntegrity) =
        b == BankIntegrity.CONTAMINATED ||
            b == BankIntegrity.HOLDOUT_EXHAUSTED ||
            b == BankIntegrity.SMALL_UNVALIDATED
}

/**
 * The ONLY construction path for [ScoreResult]. The state machine is the authority; this wrapper seals
 * the invariant that `VALID` requires both `meetsMinN` and a present signal.
 */
object ScoreResultFactory {
    fun from(output: AdapterOutput): ScoreResult {
        val verdict = ScoreStateMachine.evaluate(output.inputs)
        check(
            verdict.state != VALID ||
                (output.inputs.sampling.meetsMinN && output.primarySignal != null),
        ) { "VALID with n<nMin or without a signal is impossible by construction." }
        return ScoreResult(
            provenance = output.provenance,
            state = verdict.state,
            primarySignal = if (verdict.state == VALID) output.primarySignal else null,
            secondarySignals = output.secondarySignals,
            sampling = output.inputs.sampling,
            updatesProfile = verdict.updatesProfile,
            preliminary = verdict.preliminary,
            dataQuality = output.inputs.quality,
            claimLevel = verdict.claimLevel,
            copySafetyFlags = output.copySafetyFlags,
        )
    }
}
