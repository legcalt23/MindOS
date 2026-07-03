package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.adaptation.AdaptationDecision
import com.mindos.app.features.mindboost.language.domain.claim.ClaimBlockReason
import com.mindos.app.features.mindboost.language.domain.claim.ClaimDecision
import com.mindos.app.features.mindboost.language.domain.claim.ClaimEngine
import com.mindos.app.features.mindboost.language.domain.claim.ClaimRequest
import com.mindos.app.features.mindboost.language.presentation.state.ClaimSafeProgressUiModel
import com.mindos.app.features.mindboost.language.presentation.state.NextBestActionUiModel

/**
 * Pure builder for Next Best Action and Claim-safe Progress (v6 §11.4/§11.6).
 *
 * The progress headline is ALWAYS the ClaimEngine's verdict: an allowed claim shows its safe copy, a
 * blocked claim shows the strongest honest alternative. The UI can ask for any tier and never gets a
 * false statement back — that is the whole point of "Progress Truth".
 */
object ProgressUiBuilder {

    /** Builds the recommended-action card from an adaptation decision and the caller's reasons. */
    fun nextBestAction(
        decision: AdaptationDecision,
        sessionTypeLabel: String,
        reasons: List<String> = emptyList(),
        estimatedMinutes: Int? = null,
    ): NextBestActionUiModel = NextBestActionUiModel(
        sessionTypeLabel = sessionTypeLabel,
        focusTrait = decision.selected?.primaryTrait?.display ?: NO_FOCUS,
        reasonLines = reasons.ifEmpty { listOf(decision.rationale) },
        estimatedMinutes = estimatedMinutes,
    )

    /** Routes a requested claim through the [engine] and exposes only honest copy. */
    fun claimSafeProgress(request: ClaimRequest, engine: ClaimEngine): ClaimSafeProgressUiModel =
        when (val decision = engine.evaluate(request)) {
            is ClaimDecision.Allowed -> ClaimSafeProgressUiModel(
                headline = decision.safeCopy,
                caveats = decision.caveats,
                evidenceStateLabel = decision.evidenceState.stateLabel,
            )
            is ClaimDecision.Blocked -> ClaimSafeProgressUiModel(
                headline = decision.safeAlternativeCopy,
                caveats = listOf(blockedCaveat(decision.reason)),
                evidenceStateLabel = decision.evidenceState.stateLabel,
            )
        }

    private fun blockedCaveat(reason: ClaimBlockReason): String = when (reason) {
        ClaimBlockReason.STILL_CALIBRATING -> "Tu perfil aún se está calibrando."
        ClaimBlockReason.INSUFFICIENT_EVIDENCE -> "Aún falta evidencia para esa afirmación."
        ClaimBlockReason.NO_FUNCTIONAL_TRANSFER -> "Aún no demostrado en una tarea aplicada."
        ClaimBlockReason.NO_DELAYED_RETEST -> "Aún no validado tras una pausa."
    }

    const val NO_FOCUS = "—"
}
