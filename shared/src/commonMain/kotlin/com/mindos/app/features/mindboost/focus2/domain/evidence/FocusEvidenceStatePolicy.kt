package com.mindos.app.features.mindboost.focus2.domain.evidence

import com.mindos.app.features.mindboost.core.timing.InputTimestampPrecision
import com.mindos.app.features.mindboost.core.timing.TemporalQualityThresholds

/**
 * Focus2-LOCAL session-evidence vocabulary (S5 · ADR-FOCUS-TEMPORAL-QUALITY §8 [R2-HIST]).
 *
 * The contract's closed decision fixes these four NAMES as the session-evidence states. This is
 * deliberately NOT the cognitive lane's enum of the same shape: that package sits behind a mechanical
 * consumer-sweep barrier (no commonMain file outside it may even NAME its public types until an
 * explicit, reviewed wiring decision — the barrier this slice must not weaken). A desktopTest pins
 * name-parity between the two vocabularies so they cannot drift silently; only a future reviewed
 * bridge may collapse them into one type.
 */
enum class FocusEvidenceState { TRAINING_ONLY, PROVISIONAL, OFFICIAL_ELIGIBLE, INVALIDATED }

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §8 [R2-HIST] — the SINGLE derivation point composing focus2's existing
 * gates with the temporal axis into the session-evidence vocabulary. SHADOW and UNMOUNTED in S5:
 * nothing visible consumes this yet.
 *
 * Two deliberate properties, both pinned by tests:
 *  - [FocusEvidenceState.OFFICIAL_ELIGIBLE] is INCONSTRUCTIBLE — no input combination returns it
 *    (closed rule: nothing RT-critical enters official evidence in v1 of v2; PROVISIONAL is the ceiling).
 *  - `NotCaptured` temporal state does NOT demote the session: accuracy evidence does not require
 *    temporal capture (a probe-less platform still trains and still yields provisional accuracy);
 *    the temporal axis demotes only through the session BACKSTOP (excluded fraction over ceiling).
 *
 * Deviation from the entry-gate's sketched 5-input signature, declared: `rtModalityAdmissible` moved
 * to [rtShadowAdmissible] — modality never demotes the SESSION (accuracy is modality-fair, charter
 * §14); it gates only the RT lane, alongside precision and the temporal verdict.
 */
object FocusEvidenceStatePolicy {

    fun evidenceOf(
        writeAheadAdmissible: Boolean,
        doseComplete: Boolean,
        scorerInvalidated: Boolean,
        temporal: FocusTemporalEligibility.TemporalEvaluation?,
        thresholds: TemporalQualityThresholds = TemporalQualityThresholds.DEFAULT,
    ): FocusEvidenceState = when {
        scorerInvalidated -> FocusEvidenceState.INVALIDATED
        !writeAheadAdmissible || !doseComplete -> FocusEvidenceState.TRAINING_ONLY
        backstopExceeded(temporal, thresholds) -> FocusEvidenceState.TRAINING_ONLY
        else -> FocusEvidenceState.PROVISIONAL
    }

    /**
     * A ceiling can only be EXCEEDED by a fraction that was actually derived.
     *
     * This used to read `(temporal?.excludedFraction ?: 0.0) > max`, and that constant was doing two
     * different jobs under one name. It stood for "no evaluation reached us" AND for "an evaluation
     * reached us and could derive no fraction" — and it entered a `>` comparison, which is how a value
     * that means "does not apply" quietly becomes a measurement that means "nothing was excluded". The
     * RT lane reads the SAME null as fully excluded (`FULLY_EXCLUDED = 1.0`); that asymmetry is correct
     * and is the reason it must be WRITTEN rather than left to a sentinel: the RT lane cannot say
     * anything without temporal capture, while accuracy does not need it at all (a probe-less or
     * cadence-less platform still trains, and accuracy is modality-fair, charter §14). Neither absence
     * is evidence of a bad session, so neither demotes — stated as a branch, not inferred from a zero.
     */
    private fun backstopExceeded(
        temporal: FocusTemporalEligibility.TemporalEvaluation?,
        thresholds: TemporalQualityThresholds,
    ): Boolean {
        val fraction = temporal?.excludedFraction
        return fraction != null && fraction > thresholds.sessionExcludedMax
    }

    /** Whether this session's RTs may enter the SHADOW ledger (never official in v1). */
    fun rtShadowAdmissible(
        rtModalityAdmissible: Boolean,
        inputPrecision: InputTimestampPrecision,
        temporal: FocusTemporalEligibility.TemporalEvaluation?,
    ): Boolean = rtModalityAdmissible &&
        inputPrecision == InputTimestampPrecision.NANOS_NATIVE &&
        temporal?.rtShadowVerdict == FocusTemporalEligibility.Verdict.Eligible
}
