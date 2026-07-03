package com.mindos.app.features.mindboost.language.domain.projection

import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.AttemptId
import com.mindos.app.features.mindboost.language.domain.model.EvidenceGate
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the event-sourced projection (§12.3): the profile is a pure replay of the packet stream,
 * promotion is evidence-gated (weight-0 packets never promote), and reliability grows with
 * accumulated weight.
 */
class ProfileProjectorTest {

    private val user = UserIdHash("u1")

    private fun packet(
        trait: TraitCode,
        state: EvidenceState,
        weight: Double,
        bank: BankType = BankType.EVALUATION,
        id: String = "p-${trait.name}-${state.name}-$weight",
    ) = EvidencePacket(
        packetId = id,
        userIdHash = user,
        sourceAttemptId = AttemptId("att"),
        trait = trait,
        supportsState = state,
        bankType = bank,
        operation = OperationType.INFER,
        itemInformation = 1.0,
        evidenceWeight = weight,
        gatesApplied = setOf(EvidenceGate.BASE_ITEM_INFORMATION),
        transferDistance = 0.3,
        createdAtEpochMs = 1_700_000_000_000,
    )

    @Test
    fun emptyStreamYieldsCalibratingProfile() {
        val profile = ProfileProjector.project(user, emptyList(), nowEpochMs = 1)
        assertTrue(profile.traitEstimates.isEmpty())
        assertEquals(EvidenceState.LOWEST, profile.evidenceStateOf(TraitCode.INF))
    }

    @Test
    fun evidenceStateIsTheStrongestGatedPacket() {
        val profile = ProfileProjector.project(
            user,
            listOf(
                packet(TraitCode.INF, EvidenceState.E1_TRAINED_IMPROVEMENT, weight = 1.0),
                packet(TraitCode.INF, EvidenceState.E2_NEAR_TRANSFER, weight = 1.0),
            ),
            nowEpochMs = 1,
        )
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, profile.evidenceStateOf(TraitCode.INF))
    }

    @Test
    fun zeroWeightPacketsNeverPromote() {
        // A packet claims E3 but was gated to weight 0 — it must NOT promote the trait.
        val profile = ProfileProjector.project(
            user,
            listOf(packet(TraitCode.INF, EvidenceState.E3_FUNCTIONAL_TRANSFER, weight = 0.0)),
            nowEpochMs = 1,
        )
        assertEquals(EvidenceState.E0_CALIBRATING, profile.evidenceStateOf(TraitCode.INF))
        assertEquals(0.0, profile.traitEstimates.getValue(TraitCode.INF).reliability)
    }

    @Test
    fun reliabilityGrowsWithAccumulatedWeight() {
        val low = ProfileProjector.project(
            user, listOf(packet(TraitCode.INF, EvidenceState.E1_TRAINED_IMPROVEMENT, weight = 1.0)), 1,
        ).traitEstimates.getValue(TraitCode.INF).reliability
        val high = ProfileProjector.project(
            user,
            List(8) { packet(TraitCode.INF, EvidenceState.E1_TRAINED_IMPROVEMENT, weight = 1.0, id = "p$it") },
            1,
        ).traitEstimates.getValue(TraitCode.INF).reliability
        assertTrue(high > low)
    }

    @Test
    fun trainingBankRaisesPracticeEffectRisk() {
        val profile = ProfileProjector.project(
            user,
            listOf(packet(TraitCode.INF, EvidenceState.E1_TRAINED_IMPROVEMENT, weight = 1.0, bank = BankType.TRAINING)),
            1,
        )
        assertEquals(1.0, profile.traitEstimates.getValue(TraitCode.INF).practiceEffectRisk)
    }

    @Test
    fun projectionIsDeterministicReplay() {
        val packets = listOf(
            packet(TraitCode.INF, EvidenceState.E2_NEAR_TRANSFER, weight = 1.0),
            packet(TraitCode.ARG, EvidenceState.E1_TRAINED_IMPROVEMENT, weight = 2.0),
        )
        assertEquals(
            ProfileProjector.project(user, packets, nowEpochMs = 42),
            ProfileProjector.project(user, packets, nowEpochMs = 42),
        )
    }
}
