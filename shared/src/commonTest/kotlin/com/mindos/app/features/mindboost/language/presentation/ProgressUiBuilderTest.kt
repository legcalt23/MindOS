package com.mindos.app.features.mindboost.language.presentation

import com.mindos.app.features.mindboost.language.domain.adaptation.AdaptationDecision
import com.mindos.app.features.mindboost.language.domain.adaptation.ItemCandidate
import com.mindos.app.features.mindboost.language.domain.adaptation.ScoredCandidate
import com.mindos.app.features.mindboost.language.domain.claim.ClaimRequest
import com.mindos.app.features.mindboost.language.domain.claim.ClaimType
import com.mindos.app.features.mindboost.language.domain.claim.DefaultClaimEngine
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins Next Best Action (§11.4) and the Progress Truth contract (§11.6): the progress headline is
 * always the ClaimEngine's honest verdict — a too-strong request is silently downgraded, never shown.
 */
class ProgressUiBuilderTest {

    private val claimEngine = DefaultClaimEngine()

    private fun decision(selected: ItemCandidate?): AdaptationDecision {
        val ranked = selected
            ?.let { listOf(ScoredCandidate(it, utility = 1.0, components = emptyMap())) }
            ?: emptyList()
        return AdaptationDecision(selected = selected, ranked = ranked, rationale = "Porque toca consolidar.")
    }

    private fun candidate(trait: TraitCode) = ItemCandidate(
        taskId = TaskId("t1"),
        primaryTrait = trait,
        bankType = BankType.EVALUATION,
        transferDistance = 0.3,
        fatigueCost = 0.1,
    )

    @Test
    fun nextBestActionUsesSelectedTraitAndExplicitReasons() {
        val model = ProgressUiBuilder.nextBestAction(
            decision(candidate(TraitCode.INF)),
            sessionTypeLabel = "Core",
            reasons = listOf("Tu literal está estable.", "Sigues sobreinfiriendo."),
            estimatedMinutes = 10,
        )
        assertEquals("Core", model.sessionTypeLabel)
        assertEquals(TraitCode.INF.display, model.focusTrait)
        assertEquals(2, model.reasonLines.size)
        assertEquals(10, model.estimatedMinutes)
    }

    @Test
    fun nextBestActionFallsBackToRationaleWhenNoReasons() {
        val model = ProgressUiBuilder.nextBestAction(decision(candidate(TraitCode.SUM)), "Pulse")
        assertEquals(listOf("Porque toca consolidar."), model.reasonLines)
    }

    @Test
    fun nextBestActionWithoutSelectionShowsPlaceholder() {
        val model = ProgressUiBuilder.nextBestAction(decision(selected = null), "Recovery")
        assertEquals(ProgressUiBuilder.NO_FOCUS, model.focusTrait)
    }

    @Test
    fun allowedClaimShowsItsSafeCopy() {
        val model = ProgressUiBuilder.claimSafeProgress(
            ClaimRequest(ClaimType.TRAINED_IMPROVEMENT, EvidenceState.E1_TRAINED_IMPROVEMENT, TraitCode.INF),
            claimEngine,
        )
        assertEquals("Mejoras en este tipo de ejercicio.", model.headline)
        assertEquals(EvidenceState.E1_TRAINED_IMPROVEMENT.stateLabel, model.evidenceStateLabel)
    }

    @Test
    fun tooStrongClaimIsDowngradedToHonestAlternative() {
        val model = ProgressUiBuilder.claimSafeProgress(
            ClaimRequest(ClaimType.FUNCTIONAL_TRANSFER, EvidenceState.E1_TRAINED_IMPROVEMENT, TraitCode.INF),
            claimEngine,
        )
        // Blocked → the strongest honest copy E1 supports, plus an explanatory caveat.
        assertEquals(ClaimType.TRAINED_IMPROVEMENT.safeCopy, model.headline)
        assertTrue(model.caveats.any { it.contains("tarea aplicada") })
    }

    @Test
    fun calibratingProgressSurfacesUncertainty() {
        val model = ProgressUiBuilder.claimSafeProgress(
            ClaimRequest(ClaimType.TRAINED_IMPROVEMENT, EvidenceState.E0_CALIBRATING, TraitCode.INF),
            claimEngine,
        )
        assertTrue(model.headline.contains("calibrando", ignoreCase = true))
        assertTrue(model.caveats.any { it.contains("calibrando") })
    }
}
