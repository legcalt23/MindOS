package com.mindos.app.features.mindboost.language.domain.evidence

import com.mindos.app.features.mindboost.language.domain.model.AttemptId
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceGate
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.projection.ProfileProjector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pins the advanced promotion policy (§5.2/§5.5/§10): confounded events never promote, near/
 * functional/maintained states require their respective evidence, and the emitted packet folds
 * through the ProfileProjector to the right state.
 */
class DefaultEvidenceEngineTest {

    private val engine = DefaultEvidenceEngine()

    @Suppress("LongParameterList") // a test fixture builder: each flag exercises one promotion branch.
    private fun input(
        bank: BankType,
        allowed: Boolean = true,
        weight: Double = 1.0,
        prior: EvidenceState = EvidenceState.LOWEST,
        nearTransfer: Boolean = false,
        evaluatorEligible: Boolean = false,
        delayed: Boolean = false,
    ) = EvidenceInput(
        packetId = "pk-1",
        userIdHash = UserIdHash("u1"),
        sourceAttemptId = AttemptId("a1"),
        trait = TraitCode.INF,
        bankType = bank,
        operation = OperationType.INFER,
        itemInformation = 1.0,
        transferDistance = 0.3,
        createdAtEpochMs = 1_700_000_000_000,
        isStrongUpdateAllowed = allowed,
        evidenceWeight = weight,
        gatesApplied = setOf(EvidenceGate.BASE_ITEM_INFORMATION),
        priorState = prior,
        nearTransferDemonstrated = nearTransfer,
        evaluatorEvidenceEligible = evaluatorEligible,
        isDelayedRetest = delayed,
    )

    @Test
    fun confoundedEventNeverPromotes() {
        val decision = engine.evaluate(input(BankType.TRAINING, allowed = false))
        assertNull(decision.packet)
        assertNull(decision.supportsState)
    }

    @Test
    fun cleanTrainingYieldsE1WithWeightAndGates() {
        val decision = engine.evaluate(input(BankType.TRAINING, weight = 0.5))
        val packet = assertNotNull(decision.packet)
        assertEquals(EvidenceState.E1_TRAINED_IMPROVEMENT, packet.supportsState)
        assertEquals(0.5, packet.evidenceWeight)
        assertEquals(setOf(EvidenceGate.BASE_ITEM_INFORMATION), packet.gatesApplied)
    }

    @Test
    fun demonstratedNearTransferYieldsE2() {
        val decision = engine.evaluate(input(BankType.PARALLEL_FORM, nearTransfer = true))
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, decision.supportsState)
    }

    @Test
    fun functionalTransferRequiresEligibleEvaluator() {
        assertEquals(
            EvidenceState.E3_FUNCTIONAL_TRANSFER,
            engine.evaluate(input(BankType.TRANSFER_LAB, evaluatorEligible = true)).supportsState,
        )
        // Transfer Lab task with only a local/stub evaluator (not eligible) is formative → no packet.
        assertNull(engine.evaluate(input(BankType.TRANSFER_LAB, evaluatorEligible = false)).packet)
    }

    @Test
    fun maintenanceRequiresDelayedRetestAfterE3() {
        assertEquals(
            EvidenceState.E4_MAINTAINED,
            engine.evaluate(
                input(BankType.RETENTION, prior = EvidenceState.E3_FUNCTIONAL_TRANSFER, delayed = true),
            ).supportsState,
        )
        // Delayed retest before functional transfer does not maintain anything.
        assertNull(
            engine.evaluate(
                input(BankType.RETENTION, prior = EvidenceState.E2_NEAR_TRANSFER, delayed = true),
            ).packet,
        )
    }

    @Test
    fun nonProgressBanksDoNotPromote() {
        assertNull(engine.evaluate(input(BankType.GOLD_SET)).packet)
        assertNull(engine.evaluate(input(BankType.RED_TEAM)).packet)
    }

    @Test
    fun emittedPacketFoldsThroughProjectorToTheRightState() {
        val packet = assertNotNull(engine.evaluate(input(BankType.PARALLEL_FORM, nearTransfer = true)).packet)
        val profile = ProfileProjector.project(UserIdHash("u1"), listOf(packet), nowEpochMs = 1_700_000_000_001)
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, profile.evidenceStateOf(TraitCode.INF))
    }
}
