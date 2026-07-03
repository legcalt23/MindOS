package com.mindos.app.features.mindboost.language

import com.mindos.app.features.mindboost.language.domain.claim.ClaimBlockReason
import com.mindos.app.features.mindboost.language.domain.claim.ClaimDecision
import com.mindos.app.features.mindboost.language.domain.claim.ClaimRequest
import com.mindos.app.features.mindboost.language.domain.claim.ClaimType
import com.mindos.app.features.mindboost.language.domain.claim.DefaultClaimEngine
import com.mindos.app.features.mindboost.language.domain.evidence.DefaultEvidenceEngine
import com.mindos.app.features.mindboost.language.domain.evidence.EvidenceInput
import com.mindos.app.features.mindboost.language.domain.model.AttemptId
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.FatigueState
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.Proportion
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.projection.ProfileProjector
import com.mindos.app.features.mindboost.language.domain.scoring.AnswerKey
import com.mindos.app.features.mindboost.language.domain.scoring.ClosedScoringEngine
import com.mindos.app.features.mindboost.language.domain.scoring.ScoredResponse
import com.mindos.app.features.mindboost.language.domain.scoring.ScoringContext
import com.mindos.app.features.mindboost.language.domain.scoring.ScoringTiming
import com.mindos.app.features.mindboost.language.domain.session.LanguageSessionAction
import com.mindos.app.features.mindboost.language.domain.session.LanguageSessionReducer
import com.mindos.app.features.mindboost.language.domain.session.LanguageSessionState
import com.mindos.app.features.mindboost.language.domain.session.ResponseOutcome
import com.mindos.app.features.mindboost.language.domain.session.SessionPhase
import com.mindos.app.features.mindboost.language.domain.session.SessionPlan
import com.mindos.app.features.mindboost.language.domain.session.TrainingDecision
import com.mindos.app.features.mindboost.language.domain.transfer.DefaultNearTransferVerifier
import com.mindos.app.features.mindboost.language.domain.transfer.NearTransferConfig
import com.mindos.app.features.mindboost.language.domain.transfer.TransferTrial
import com.mindos.app.features.mindboost.language.presentation.GenomeUiBuilder
import com.mindos.app.features.mindboost.language.presentation.ProgressUiBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * End-to-end wiring of the whole module spine with the REAL engines (no fakes), proving the
 * v6 chain holds: Calibrar → Entrenar → (Reparar) → Verificar en material nuevo → Evidencia con
 * gates → Claim honesto → UX sin falsa precisión.
 */
class LanguageCycleEndToEndTest {

    private val scoring = ClosedScoringEngine()
    private val verifier = DefaultNearTransferVerifier()
    private val evidence = DefaultEvidenceEngine()
    private val claims = DefaultClaimEngine()
    private val user = UserIdHash("u1")
    private val key = AnswerKey.SingleChoice("b")

    @Suppress("LongParameterList") // fixture builder; each field maps one promotion signal.
    private fun evidenceInput(
        index: Int,
        scored: ScoredResponse,
        bank: BankType,
        nearTransfer: Boolean = false,
    ) = EvidenceInput(
        packetId = "pk-$index",
        userIdHash = user,
        sourceAttemptId = AttemptId("a-$index"),
        trait = TraitCode.INF,
        bankType = bank,
        operation = OperationType.INFER,
        itemInformation = 1.0,
        transferDistance = 0.3,
        createdAtEpochMs = 1L,
        isStrongUpdateAllowed = scored.isStrongUpdateAllowed,
        evidenceWeight = scored.evidenceWeight,
        gatesApplied = scored.gatesApplied,
        nearTransferDemonstrated = nearTransfer,
    )

    @Test
    fun cleanNearTransferCyclePromotesToE2AndUnlocksAnHonestClaim() {
        // Score three correct answers on unseen, parallel-form material (no confounds).
        val ctx = ScoringContext(bankType = BankType.PARALLEL_FORM)
        val scored = (0 until 3).map {
            scoring.score(key, ResponsePayload.MultipleChoice("b"), ScoringTiming(), ctx)
        }
        scored.forEach { assertTrue(it.isStrongUpdateAllowed) }

        // Near transfer is verified across the unseen trials.
        val trials = scored.map { TransferTrial(BankType.PARALLEL_FORM, it.correctness, it.isStrongUpdateAllowed) }
        val verdict = verifier.verify(TraitCode.INF, trials, NearTransferConfig())
        assertTrue(verdict.demonstrated)

        // Each scored event becomes an E2 evidence packet, folded into the profile.
        val packets = scored.mapIndexed { i, s ->
            assertNotNull(evidence.evaluate(evidenceInput(i, s, BankType.PARALLEL_FORM, nearTransfer = true)).packet)
        }
        val profile = ProfileProjector.project(user, packets, nowEpochMs = 2L)
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, profile.evidenceStateOf(TraitCode.INF))

        // The claim engine now allows exactly the near-transfer claim, and the UI shows its safe copy.
        val request = ClaimRequest(ClaimType.NEAR_TRANSFER, profile.evidenceStateOf(TraitCode.INF), TraitCode.INF)
        assertIs<ClaimDecision.Allowed>(claims.evaluate(request))
        assertEquals(
            "Tu rendimiento mejora en material nuevo similar.",
            ProgressUiBuilder.claimSafeProgress(request, claims).headline,
        )

        // And the cockpit reflects E2 with no fabricated numeric range (placeholder θ).
        val cockpitItem = GenomeUiBuilder.evidenceCockpit(profile).items.first { it.trait == TraitCode.INF }
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, cockpitItem.evidenceState)
        assertNull(cockpitItem.probableRange)
    }

    @Test
    fun confoundedAttemptNeverPromotesAndTheClaimStaysCalibrating() {
        // A correct answer under high fatigue must not move the profile.
        val ctx = ScoringContext(bankType = BankType.EVALUATION, fatigue = FatigueState(Proportion(0.9)))
        val scored = scoring.score(key, ResponsePayload.MultipleChoice("b"), ScoringTiming(), ctx)
        assertFalse(scored.isStrongUpdateAllowed)

        assertNull(evidence.evaluate(evidenceInput(0, scored, BankType.EVALUATION)).packet)

        // With no promotable packet, the profile stays calibrating and the claim is blocked.
        val profile = ProfileProjector.project(user, emptyList(), nowEpochMs = 1L)
        assertEquals(EvidenceState.E0_CALIBRATING, profile.evidenceStateOf(TraitCode.INF))
        val blocked = assertIs<ClaimDecision.Blocked>(
            claims.evaluate(
                ClaimRequest(ClaimType.TRAINED_IMPROVEMENT, profile.evidenceStateOf(TraitCode.INF), TraitCode.INF),
            ),
        )
        assertSame(ClaimBlockReason.STILL_CALIBRATING, blocked.reason)
    }

    @Test
    fun sessionReducerDrivesTheLoopAndBranchesIntoRepairOnError() {
        val plan = SessionPlan(listOf(SessionPhase.TRAINING_BASE))
        val start = LanguageSessionReducer.reduce(
            LanguageSessionState.notStarted(LanguageSessionId("s1"), plan),
            LanguageSessionAction.Start(plan),
        )
        val submitted = LanguageSessionReducer.reduce(
            start.state,
            LanguageSessionAction.SubmitResponse(
                ResponseOutcome(isCorrect = false, errorTypes = listOf(ErrorType.INF_OVER)),
            ),
        )
        assertTrue(assertIs<TrainingDecision.ShowFeedback>(submitted.decision).repairTriggered)

        val acked = LanguageSessionReducer.reduce(submitted.state, LanguageSessionAction.FeedbackAcknowledged)
        val next = LanguageSessionReducer.reduce(acked.state, LanguageSessionAction.RequestNext)
        assertEquals(SessionPhase.REPAIR_MODE, assertIs<TrainingDecision.PresentTask>(next.decision).phase)
    }
}
