package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.core.scoring.BankIntegrity
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.language.data.local.LocalSessionSummaryRepository
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.TraitEstimate
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import com.mindos.app.features.mindboost.language.domain.model.UserLanguageProfile
import com.mindos.app.features.mindboost.language.domain.repository.EvidenceRepository
import com.mindos.app.features.mindboost.language.domain.repository.SessionSummaryRepository
import com.mindos.app.features.mindboost.profile.data.adapter.LanguageEvidenceBlockReason
import com.mindos.app.features.mindboost.profile.data.adapter.LanguageEvidenceDecision
import com.mindos.app.features.mindboost.profile.data.adapter.LanguageProfileSource
import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeEvidence(private val profile: UserLanguageProfile) : EvidenceRepository {
    override suspend fun loadProfile(userIdHash: UserIdHash): UserLanguageProfile = profile
    override suspend fun append(packet: EvidencePacket) = Unit
    override suspend fun packets(userIdHash: UserIdHash): List<EvidencePacket> = emptyList()
    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = 0
}

private class FakeSummaries(private val list: List<SessionSummary>) : SessionSummaryRepository {
    override suspend fun summaries(userIdHash: UserIdHash): List<SessionSummary> = list
    override suspend fun save(summary: SessionSummary) = Unit
    override suspend fun get(userIdHash: UserIdHash, sessionId: LanguageSessionId): SessionSummary? = null
    override suspend fun deleteUserData(userIdHash: UserIdHash): Int = 0
}

private fun traitEstimate(
    trait: TraitCode,
    state: EvidenceState,
    reliability: Double = 0.5,
    practiceRisk: Double = 0.0,
) = TraitEstimate(
    trait = trait,
    thetaMean = 0.0,
    thetaSd = 0.0,
    reliability = reliability,
    evidenceState = state,
    practiceEffectRisk = practiceRisk,
    confounderRisk = 0.0,
    fairnessRisk = 0.0,
    estimatorVersion = "v",
    updatedAtEpochMs = 0L,
)

private fun profileOf(vararg traits: TraitEstimate, updatedAt: Long = 0L) = UserLanguageProfile(
    userIdHash = UserIdHash("anon"),
    traitEstimates = traits.associateBy { it.trait },
    updatedAtEpochMs = updatedAt,
)

private fun summary(id: String, completedAt: Long, items: Int, correct: Int) = SessionSummary(
    sessionId = LanguageSessionId(id),
    userIdHash = UserIdHash("anon"),
    startedAtEpochMs = 0L,
    completedAtEpochMs = completedAt,
    itemsAttempted = items,
    correctCount = correct,
)

class LanguageProfileSourceTest {

    // The math tests declare a VALIDATED bank + real provenance so they exercise the ELIGIBLE path;
    // the integrity gate itself is proven by the *Blocked* / hardening tests below.
    private fun source(profile: UserLanguageProfile, sessions: List<SessionSummary> = emptyList()) =
        LanguageProfileSource(
            FakeEvidence(profile),
            FakeSummaries(sessions),
            contentIntegrity = BankIntegrity.OK,
            provenanceValidated = true,
        )

    @Test
    fun noEvidenceNoSessions_returnsNull() = runTest {
        assertNull(source(profileOf()).contribution("u1"))
    }

    @Test
    fun measuredTraits_useEvidenceLadderRank_notThetaOrAccuracy() = runTest {
        val profile = profileOf(
            traitEstimate(TraitCode.LEX_A, EvidenceState.E1_TRAINED_IMPROVEMENT), // rank 1
            traitEstimate(TraitCode.LEX_D, EvidenceState.E3_FUNCTIONAL_TRANSFER), // rank 3
            updatedAt = 700L,
        )
        val contribution = source(profile).contribution("u1")!!
        assertEquals(CognitiveModuleId.LENGUAJE, contribution.moduleId)
        assertEquals(0.4, contribution.normalizedScore!!, 1e-9) // mean rank 2 / 5
        assertEquals(0.5, contribution.confidence, 1e-9) // reliability 0.5, no practice penalty
        assertEquals(0.2, contribution.dataSufficiency, 1e-9) // breadth 2/5 * 0.5
        assertTrue(contribution.preliminary)
        assertEquals(700L, contribution.updatedAtMs)
    }

    @Test
    fun onlyCalibratingTraits_scoreIsNull_butContributionEmittedWithSessionData() = runTest {
        val profile = profileOf(traitEstimate(TraitCode.LEX_A, EvidenceState.E0_CALIBRATING))
        val contribution = source(profile, listOf(summary("s1", 1L, items = 10, correct = 5))).contribution("u1")!!
        assertNull(contribution.normalizedScore, "no trait cleared calibrating -> CALIBRATING, never a silent 0")
    }

    @Test
    fun improvingAccuracyOverTime_yieldsImprovingTrend() = runTest {
        val sessions = listOf(
            summary("s1", 1L, items = 10, correct = 4),
            summary("s2", 2L, items = 10, correct = 4),
            summary("s3", 3L, items = 10, correct = 8),
            summary("s4", 4L, items = 10, correct = 8),
        )
        val contribution = source(profileOf(), sessions).contribution("u1")!!
        assertNull(contribution.normalizedScore) // no measured traits
        assertEquals(ProfileTrend.IMPROVING, contribution.trend)
    }

    // --- LANGUAGE_EVIDENCE_INTEGRITY_HARDENING (2026-07-03) ---

    @Test
    fun honestDefaults_blockDemoBank_evenWithStrongEvidence() = runTest {
        // Defaults mirror Lenguaje today: SMALL_UNVALIDATED demo bank + stub provenance.
        val evidence = FakeEvidence(profileOf(traitEstimate(TraitCode.LEX_D, EvidenceState.E3_FUNCTIONAL_TRANSFER)))
        val gated = LanguageProfileSource(evidence, FakeSummaries(emptyList()))
        assertNull(gated.contribution("u1"), "a demo bank must never feed the official profile")
        val decision = assertIs<LanguageEvidenceDecision.Blocked>(gated.evaluate("u1"))
        assertEquals(LanguageEvidenceBlockReason.SMALL_UNVALIDATED_BANK, decision.reason)
    }

    @Test
    fun validBankButStubProvenance_isBlockedByProvenance() = runTest {
        val evidence = FakeEvidence(profileOf(traitEstimate(TraitCode.LEX_D, EvidenceState.E3_FUNCTIONAL_TRANSFER)))
        val gated = LanguageProfileSource(
            evidence,
            FakeSummaries(emptyList()),
            contentIntegrity = BankIntegrity.OK,
            provenanceValidated = false,
        )
        assertNull(gated.contribution("u1"))
        val decision = assertIs<LanguageEvidenceDecision.Blocked>(gated.evaluate("u1"))
        assertEquals(LanguageEvidenceBlockReason.STUB_PROVENANCE, decision.reason)
    }

    @Test
    fun validBankAndProvenanceButNoEvidence_isInsufficient() = runTest {
        val gated = LanguageProfileSource(
            FakeEvidence(profileOf()),
            FakeSummaries(emptyList()),
            contentIntegrity = BankIntegrity.OK,
            provenanceValidated = true,
        )
        assertNull(gated.contribution("u1"))
        val decision = assertIs<LanguageEvidenceDecision.Blocked>(gated.evaluate("u1"))
        assertEquals(LanguageEvidenceBlockReason.INSUFFICIENT_VALID_EVIDENCE, decision.reason)
    }

    @Test
    fun fiveSavedDemoSessions_stayBlocked_andOutOfGlobalScore() = runTest {
        // A REAL encrypted repo genuinely holding 5 sessions (proves persistence is not the blocker).
        val store = InMemorySecureKeyValueStore()
        val summaryRepo = LocalSessionSummaryRepository(store)
        val hash = UserIdHash("u1")
        repeat(5) { i ->
            summaryRepo.save(
                SessionSummary(
                    sessionId = LanguageSessionId("s$i"),
                    userIdHash = hash,
                    startedAtEpochMs = 0L,
                    completedAtEpochMs = (i + 1).toLong(),
                    itemsAttempted = 10,
                    correctCount = 9,
                ),
            )
        }
        assertEquals(5, summaryRepo.summaries(hash).size, "repo genuinely persisted 5 sessions")

        // Strong evidence present too (a trait cleared E3), as if scored from the demo bank.
        val evidence = FakeEvidence(profileOf(traitEstimate(TraitCode.LEX_D, EvidenceState.E3_FUNCTIONAL_TRANSFER)))
        val gated = LanguageProfileSource(evidence, summaryRepo) // honest defaults: demo bank + stub provenance

        assertNull(gated.contribution("u1"), "5 saved sessions + E3 evidence must NOT unlock VERBAL from a demo bank")
        val decision = assertIs<LanguageEvidenceDecision.Blocked>(gated.evaluate("u1"))
        assertEquals(LanguageEvidenceBlockReason.SMALL_UNVALIDATED_BANK, decision.reason)

        // Aggregator: VERBAL stays CALIBRATING and Lenguaje never reaches the Global/Overall Score.
        val profile = MindBoostProfileAggregator()
            .aggregate("u1", listOfNotNull(gated.contribution("u1")), nowMs = 100L)
        val verbal = profile.dimensions.getValue(CognitiveDimension.VERBAL)
        assertEquals(DimensionStatus.CALIBRATING, verbal.status, "VERBAL stays CALIBRATING")
        assertNull(verbal.value, "VERBAL is CALIBRATING, never a silent number")
        assertNull(profile.overall?.compositeIndex, "no established dimension -> no Global Score; Lenguaje excluded")
    }
}
