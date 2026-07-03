@file:Suppress("MagicNumber") // 1.0/0.0 are the VERIFIED / blocked multiplier endpoints.

package com.mindos.app.features.arena.integrity.engine

import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SmurfSuspicionLevel
import com.mindos.app.features.arena.integrity.model.IntegritySignals
import com.mindos.app.features.arena.rating.model.IntegrityGateDecision

/**
 * Phase 05: Offline, deterministic integrity gate builder (integrity_gate_decision_builder.md).
 *
 * Maps fraud signals to an [IntegrityGateDecision]:
 * - collusion              -> INVALIDATED (cancel match, K=0)
 * - boosting / sharing     -> HOLD (blocked pending human review)
 * - otherwise              -> VERIFIED (the only state under which the rating engine applies a delta)
 *
 * Smurf does NOT block the gate; it drives sigma recalibration via [smurfSuspicionFrom]. Lag-switch
 * is a comparability concern ([lagSwitchForcesNotComparable]), not an integrity hold. The decision's
 * reasonCode is coarse (state-level only) so it never reveals detection methods (secrecy).
 */
class IntegrityGateBuilder {

    fun buildIntegrityGate(signals: IntegritySignals): IntegrityGateDecision {
        val state = gateStateFor(signals)
        return IntegrityGateDecision(
            state = state,
            updateMultiplier = if (state == IntegrityStateEnum.VERIFIED) 1.0 else 0.0,
            reasonCode = reasonFor(state),
            sourceEngine = SOURCE_ENGINE,
        )
    }

    /** Severe outcomes can never be final on automation alone (human_review_requirement.md). */
    fun requiresHumanReview(decision: IntegrityGateDecision): Boolean = when (decision.state) {
        IntegrityStateEnum.HOLD,
        IntegrityStateEnum.INVALIDATED,
        IntegrityStateEnum.REVIEW_REQUIRED -> true
        IntegrityStateEnum.OPEN, IntegrityStateEnum.VERIFIED -> false
    }

    /** Smurf suspicion keeps sigma high in the rating engine instead of blocking the gate. */
    fun smurfSuspicionFrom(signals: IntegritySignals): SmurfSuspicionLevel =
        if (signals.smurf) SmurfSuspicionLevel.HIGH else SmurfSuspicionLevel.NONE

    /** Lag-switching invalidates comparability for the session rather than holding the account. */
    fun lagSwitchForcesNotComparable(signals: IntegritySignals): Boolean = signals.lagSwitch

    private fun gateStateFor(signals: IntegritySignals): IntegrityStateEnum = when {
        signals.collusion -> IntegrityStateEnum.INVALIDATED
        signals.boosting || signals.accountSharing -> IntegrityStateEnum.HOLD
        else -> IntegrityStateEnum.VERIFIED
    }

    private fun reasonFor(state: IntegrityStateEnum): String = when (state) {
        IntegrityStateEnum.VERIFIED -> "VERIFIED"
        IntegrityStateEnum.INVALIDATED -> "INTEGRITY_INVALIDATED"
        IntegrityStateEnum.HOLD -> "INTEGRITY_HOLD"
        IntegrityStateEnum.REVIEW_REQUIRED -> "INTEGRITY_REVIEW_REQUIRED"
        IntegrityStateEnum.OPEN -> "INTEGRITY_OPEN"
    }

    companion object {
        private const val SOURCE_ENGINE = "IntegrityGateBuilder"
    }
}
