package com.mindos.app.features.mindboost.language.data.local

import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import com.mindos.app.features.mindboost.language.domain.model.AttemptId
import com.mindos.app.features.mindboost.language.domain.model.BankType
import com.mindos.app.features.mindboost.language.domain.model.ConsentState
import com.mindos.app.features.mindboost.language.domain.model.ConsentType
import com.mindos.app.features.mindboost.language.domain.model.EvidenceGate
import com.mindos.app.features.mindboost.language.domain.model.EvidencePacket
import com.mindos.app.features.mindboost.language.domain.model.EvidenceState
import com.mindos.app.features.mindboost.language.domain.model.LanguageSessionId
import com.mindos.app.features.mindboost.language.domain.model.OperationType
import com.mindos.app.features.mindboost.language.domain.model.RetentionPolicy
import com.mindos.app.features.mindboost.language.domain.model.SessionSummary
import com.mindos.app.features.mindboost.language.domain.model.TaskId
import com.mindos.app.features.mindboost.language.domain.model.TraitCode
import com.mindos.app.features.mindboost.language.domain.model.UserIdHash
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the encrypted local persistence over [InMemorySecureKeyValueStore]: serialization
 * round-trips, profile projection from persisted packets, exposure increments, consent, per-user
 * isolation and granular deletion (§13).
 */
class LocalLanguageRepositoriesTest {

    private val userA = UserIdHash("uA")
    private val userB = UserIdHash("uB")

    private fun packet(user: UserIdHash, state: EvidenceState, id: String) = EvidencePacket(
        packetId = id,
        userIdHash = user,
        sourceAttemptId = AttemptId("att"),
        trait = TraitCode.INF,
        supportsState = state,
        bankType = BankType.EVALUATION,
        operation = OperationType.INFER,
        itemInformation = 1.0,
        evidenceWeight = 1.0,
        gatesApplied = setOf(EvidenceGate.BASE_ITEM_INFORMATION),
        transferDistance = 0.3,
        createdAtEpochMs = 1_700_000_000_000,
    )

    @Test
    fun corruptBlobs_areQuarantined_readsAreEmpty_neverCrash() = runTest {
        // Malformed packet/summary blobs must be skipped on read — never crash, never fabricate evidence.
        val store = InMemorySecureKeyValueStore()
        store.put("pkt:uA::bad", "{ not valid packet json") // keys mirror the repos' prefixes (SEP = "::")
        store.put("sum:uA::bad", "{ garbage summary")
        assertTrue(LocalEvidenceRepository(store).packets(userA).isEmpty())
        assertTrue(LocalSessionSummaryRepository(store).summaries(userA).isEmpty())
    }

    @Test
    fun evidenceAppendThenProfileProjectionReflectsStrongestState() = runTest {
        val repo = LocalEvidenceRepository(InMemorySecureKeyValueStore())
        repo.append(packet(userA, EvidenceState.E1_TRAINED_IMPROVEMENT, "p1"))
        repo.append(packet(userA, EvidenceState.E2_NEAR_TRANSFER, "p2"))

        assertEquals(2, repo.packets(userA).size)
        assertEquals(EvidenceState.E2_NEAR_TRANSFER, repo.loadProfile(userA).evidenceStateOf(TraitCode.INF))
    }

    @Test
    fun evidenceIsIsolatedPerUserAndDeletable() = runTest {
        val repo = LocalEvidenceRepository(InMemorySecureKeyValueStore())
        repo.append(packet(userA, EvidenceState.E1_TRAINED_IMPROVEMENT, "p1"))
        repo.append(packet(userB, EvidenceState.E1_TRAINED_IMPROVEMENT, "p2"))

        assertTrue(repo.packets(userB).single().userIdHash == userB)
        assertEquals(1, repo.deleteUserData(userA))
        assertTrue(repo.packets(userA).isEmpty())
        assertEquals(1, repo.packets(userB).size) // other user untouched
    }

    @Test
    fun summaryRoundTripsAndIsDeletable() = runTest {
        val repo = LocalSessionSummaryRepository(InMemorySecureKeyValueStore())
        val summary = SessionSummary(
            sessionId = LanguageSessionId("s1"),
            userIdHash = userA,
            startedAtEpochMs = 1,
            completedAtEpochMs = 2,
            itemsAttempted = 10,
            correctCount = 7,
            perTraitCorrect = mapOf(TraitCode.INF to 7),
        )
        repo.save(summary)

        assertEquals(summary, repo.get(userA, LanguageSessionId("s1")))
        assertEquals(1, repo.summaries(userA).size)
        assertEquals(1, repo.deleteUserData(userA))
        assertTrue(repo.summaries(userA).isEmpty())
    }

    @Test
    fun exposureRecordPersistsAndIncrements() = runTest {
        val repo = LocalExposureRepository(InMemorySecureKeyValueStore())
        val task = TaskId("t1")
        repo.record(userA, task)
        repo.record(userA, task)

        assertEquals(2, repo.snapshot(userA).timesSeen(task))
        assertEquals(1, repo.deleteUserData(userA))
        assertEquals(0, repo.snapshot(userA).timesSeen(task))
    }

    @Test
    fun consentRoundTripsAbsentReturnsNullAndIsDeletable() = runTest {
        val repo = LocalConsentRepository(InMemorySecureKeyValueStore())
        val consent = ConsentState(
            userIdHash = userA,
            consentType = ConsentType.CLOUD_EVALUATION,
            granted = true,
            retention = RetentionPolicy.ABSTRACTION_ONLY,
            updatedAtEpochMs = 5,
        )
        repo.set(consent)

        assertEquals(consent, repo.get(userA, ConsentType.CLOUD_EVALUATION))
        assertNull(repo.get(userA, ConsentType.AUDIO_CAPTURE)) // no decision → null (treated as denied)
        assertEquals(1, repo.all(userA).size)
        assertEquals(1, repo.deleteUserData(userA))
        assertTrue(repo.all(userA).isEmpty())
    }
}
