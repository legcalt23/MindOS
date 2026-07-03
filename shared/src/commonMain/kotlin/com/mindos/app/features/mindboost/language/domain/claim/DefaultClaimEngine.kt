package com.mindos.app.features.mindboost.language.domain.claim

import com.mindos.app.features.mindboost.language.domain.claim.ClaimType.FUNCTIONAL_TRANSFER
import com.mindos.app.features.mindboost.language.domain.claim.ClaimType.MAINTAINED
import com.mindos.app.features.mindboost.language.domain.claim.ClaimType.NEAR_TRANSFER
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState

/**
 * Default, fully-offline [ClaimEngine]. Pure function of (requested claim, measured state).
 *
 * Invariants it guarantees (the honesty contract):
 *  - A claim is allowed **iff** the measured state reaches its minimum (§5.7).
 *  - A blocked claim always returns the strongest *honest* alternative copy, never silence.
 *  - While calibrating (E0) no improvement claim is ever allowed.
 */
class DefaultClaimEngine : ClaimEngine {

    override fun evaluate(request: ClaimRequest): ClaimDecision {
        val state = request.evidenceState
        val requested = request.requestedClaim
        return if (state.isAtLeast(requested.minimumState)) {
            ClaimDecision.Allowed(
                claim = requested,
                safeCopy = requested.safeCopy,
                caveats = caveatsFor(requested, state),
                evidenceState = state,
            )
        } else {
            ClaimDecision.Blocked(
                requested = requested,
                reason = blockReasonFor(requested, state),
                safeAlternativeCopy = safeAlternative(state),
                evidenceState = state,
            )
        }
    }

    private fun caveatsFor(claim: ClaimType, state: EvidenceState): List<String> = buildList {
        if (claim == NEAR_TRANSFER || claim == ClaimType.TRAINED_IMPROVEMENT) {
            add("Aún no validado como transferencia funcional.")
        }
        if (claim == FUNCTIONAL_TRANSFER && !state.isAtLeast(EvidenceState.E4_MAINTAINED)) {
            add("Aún no validado tras una pausa (retest 7/30 días).")
        }
    }

    private fun blockReasonFor(claim: ClaimType, state: EvidenceState): ClaimBlockReason = when {
        state.isCalibrating -> ClaimBlockReason.STILL_CALIBRATING
        claim == MAINTAINED -> ClaimBlockReason.NO_DELAYED_RETEST
        claim == FUNCTIONAL_TRANSFER && !state.isAtLeast(EvidenceState.E3_FUNCTIONAL_TRANSFER) ->
            ClaimBlockReason.NO_FUNCTIONAL_TRANSFER
        else -> ClaimBlockReason.INSUFFICIENT_EVIDENCE
    }

    private fun safeAlternative(state: EvidenceState): String =
        ClaimType.strongestFor(state)?.safeCopy
            ?: "Estamos calibrando tu perfil; aún no podemos afirmar mejoras."
}
