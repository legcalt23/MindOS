package com.mindos.app.features.mindboost.language.domain.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The domain entities cross the persistence (SecureKeyValueStore) and sync boundaries as
 * JSON, so round-trip stability is part of the contract. These tests prove the key
 * aggregates — including the sealed [ResponsePayload] polymorphism and the enum-keyed
 * profile map — survive encode → decode unchanged.
 */
class LanguageDomainSerializationTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun attemptWithEvidenceSelectionRoundTrips() {
        val attempt = Attempt(
            attemptId = AttemptId("att-1"),
            sessionId = LanguageSessionId("sess-1"),
            taskId = TaskId("lang_m6_inf_00421"),
            taskVersion = TaskVersion.FIRST,
            bankType = BankType.EVALUATION,
            primaryTrait = TraitCode.INF,
            operation = OperationType.EVIDENCE_SUPPORTED_INFERENCE,
            responseMode = ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION,
            response = ResponsePayload.EvidenceSelection(
                selectedOptionId = "b",
                selectedEvidenceIds = listOf("s2", "s4"),
            ),
            correctness = Proportion.ONE,
            latencyMs = LatencyMs(4200),
            confidence = Proportion(0.75),
            errorTypes = listOf(ErrorType.INF_OVER),
            flags = setOf(AttemptFlag.REPEATED_ITEM),
            evidenceWeight = 0.0,
            createdAtEpochMs = 1_700_000_000_000,
        )

        val restored = json.decodeFromString<Attempt>(json.encodeToString(attempt))

        assertEquals(attempt, restored)
        // The sealed payload must keep its concrete subtype across the boundary.
        assertEquals(
            ResponsePayload.EvidenceSelection("b", listOf("s2", "s4")),
            restored.response,
        )
    }

    @Test
    fun taskPassportRoundTrips() {
        val passport = TaskPassport(
            taskId = TaskId("lang_m6_inf_00421"),
            version = TaskVersion.FIRST,
            status = QaStatus.PILOT,
            moduleId = "M6",
            primaryTrait = TraitCode.INF,
            secondaryTraits = listOf(TraitCode.LIT, TraitCode.SIT, TraitCode.META),
            operation = OperationType.EVIDENCE_SUPPORTED_INFERENCE,
            responseMode = ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION,
            dose = CognitiveDose.LOW,
            stimulus = StimulusProfile(
                source = StimulusSource.ORIGINAL,
                words = 130,
                domain = "everyday_reasoning",
                priorKnowledgeDependency = DependencyLevel.LOW,
                culturalDependency = DependencyLevel.LOW,
            ),
            scoring = ScoringSpec(
                scoringRule = ScoringRule.DETERMINISTIC_KEY_PLUS_EVIDENCE,
                caps = listOf("unsupported_inference_cap"),
            ),
            feedback = FeedbackSpec(
                errorPatterns = listOf(ErrorType.INF_OVER, ErrorType.INF_UNDER),
                repairPlanId = "repair_evidence_minimum_v2",
            ),
            evidence = EvidenceTarget(
                eligibleStates = listOf(EvidenceState.E1_TRAINED_IMPROVEMENT, EvidenceState.E2_NEAR_TRANSFER),
                bankType = BankType.EVALUATION,
                parallelFormGroup = "pf_inf_012",
            ),
            fairness = FairnessMetadata(
                variantsValidated = listOf("es_ES", "es_MX", "es_neutral"),
                difStatus = "pending_pilot",
            ),
            privacy = PrivacyMetadata(privacyClass = PrivacyClass.NON_SENSITIVE_ORIGINAL_CONTENT),
            qa = QaMetadata(expertReviewRequired = true, pilotRequired = true),
        )

        val restored = json.decodeFromString<TaskPassport>(json.encodeToString(passport))
        assertEquals(passport, restored)
    }

    @Test
    fun userProfileWithEnumKeyedMapRoundTrips() {
        val estimate = TraitEstimate(
            trait = TraitCode.INF,
            thetaMean = 0.4,
            thetaSd = 0.3,
            reliability = 0.62,
            evidenceState = EvidenceState.E2_NEAR_TRANSFER,
            practiceEffectRisk = 0.1,
            confounderRisk = 0.2,
            fairnessRisk = 0.05,
            estimatorVersion = "v6-stub-1",
            updatedAtEpochMs = 1_700_000_000_000,
        )
        val profile = UserLanguageProfile(
            userIdHash = UserIdHash("hash-abc"),
            traitEstimates = mapOf(TraitCode.INF to estimate),
            updatedAtEpochMs = 1_700_000_000_000,
        )

        val restored = json.decodeFromString<UserLanguageProfile>(json.encodeToString(profile))

        assertEquals(profile, restored)
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, restored.evidenceStateOf(TraitCode.INF))
        assertEquals(EvidenceState.LOWEST, restored.evidenceStateOf(TraitCode.ARG))
    }

    @Test
    fun evidencePacketAndPosteriorRoundTrip() {
        val packet = EvidencePacket(
            packetId = "pk-1",
            userIdHash = UserIdHash("hash-abc"),
            sourceAttemptId = AttemptId("att-1"),
            trait = TraitCode.INF,
            supportsState = EvidenceState.E2_NEAR_TRANSFER,
            bankType = BankType.PARALLEL_FORM,
            operation = OperationType.INFER,
            itemInformation = 0.8,
            evidenceWeight = 0.55,
            gatesApplied = setOf(EvidenceGate.NOVELTY, EvidenceGate.BANK_SEPARATION),
            transferDistance = 0.3,
            createdAtEpochMs = 1_700_000_000_000,
        )
        val posterior = AbilityPosterior(
            trait = TraitCode.INF,
            thetaMean = 0.4,
            thetaSd = 0.3,
            credibleInterval90Low = -0.1,
            credibleInterval90High = 0.9,
            reliability = 0.62,
            evidenceState = EvidenceState.E2_NEAR_TRANSFER,
            nearTransferEvidence = 0.5,
            functionalTransferEvidence = 0.0,
            maintenanceEvidence = 0.0,
            practiceEffectRisk = 0.1,
            confounderRisk = 0.2,
            fairnessRisk = 0.05,
            estimatorVersion = "v6-stub-1",
            lastUpdatedAtEpochMs = 1_700_000_000_000,
        )

        assertEquals(packet, json.decodeFromString<EvidencePacket>(json.encodeToString(packet)))
        assertEquals(posterior, json.decodeFromString<AbilityPosterior>(json.encodeToString(posterior)))
    }
}
