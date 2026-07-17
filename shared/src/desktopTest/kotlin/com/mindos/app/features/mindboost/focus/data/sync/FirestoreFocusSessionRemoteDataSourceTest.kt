package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.ConflictReason
import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import com.mindos.app.features.mindboost.core.sync.SyncWriteResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * End-to-end classification of the Firestore adapter via a FAKE gateway (the real gitlive gateway is
 * emulator-gated). Exercises FirestoreFocusSessionRemoteDataSource composed with FocusSessionRemoteRepository.
 */
class FirestoreFocusSessionRemoteDataSourceTest {

    private val dto =
        FocusSessionCloudMapper.toCloudDtoOrNull(focusSession(userId = "user-1"), "user-1", nowMs = 1L)!!
    private val expected = FocusSessionCloudMapper.fingerprintOf(dto)

    private fun repo(gateway: FakeFocusSessionFirestoreGateway) =
        FocusSessionRemoteRepository(FirestoreFocusSessionRemoteDataSource(gateway))

    @Test
    fun createSuccess_isSynced_withoutProbe() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(createOutcome = RawWriteOutcome.SUCCESS)
        assertEquals(SyncWriteResult.Synced, repo(gateway).write(dto, expected))
        assertEquals(0, gateway.readCalls, "no probe on a fresh write")
    }

    @Test
    fun alreadyExists_sameFingerprint_isDuplicate() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            read = FirestoreSessionRead.Found("user-1", expected),
        )
        assertEquals(SyncWriteResult.AlreadySyncedDuplicate, repo(gateway).write(dto, expected))
        assertEquals(1, gateway.readCalls, "an existing document must be verified before accepting it")
    }

    @Test
    fun alreadyExists_differentFingerprint_isConflict() = runTest {
        val diverged = expected.copy(normalizedScore = 0.9)
        val gateway = FakeFocusSessionFirestoreGateway(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            read = FirestoreSessionRead.Found("user-1", diverged),
        )
        assertEquals(
            SyncWriteResult.Conflict(ConflictReason.DIFFERENT_FINGERPRINT),
            repo(gateway).write(dto, expected),
        )
    }

    @Test
    fun alreadyExists_differentUser_isConflict() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            read = FirestoreSessionRead.Found("intruder", expected.copy(userId = "intruder")),
        )
        assertEquals(SyncWriteResult.Conflict(ConflictReason.DIFFERENT_USER), repo(gateway).write(dto, expected))
    }

    @Test
    fun alreadyExists_unreadableRemote_isUnverifiedConflict() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            read = FirestoreSessionRead.Unreadable,
        )
        assertEquals(
            SyncWriteResult.Conflict(ConflictReason.UNVERIFIED_EXISTING),
            repo(gateway).write(dto, expected),
        )
    }

    @Test
    fun alreadyExists_notFoundOnRead_isUnverifiedConflict() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            read = FirestoreSessionRead.NotFound,
        )
        assertEquals(
            SyncWriteResult.Conflict(ConflictReason.UNVERIFIED_EXISTING),
            repo(gateway).write(dto, expected),
        )
    }

    @Test
    fun permissionDenied_isNeverSynced() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(createOutcome = RawWriteOutcome.PERMISSION_DENIED)
        assertEquals(SyncWriteResult.PermissionDenied, repo(gateway).write(dto, expected))
    }

    @Test
    fun network_isRetryableFailure() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(createOutcome = RawWriteOutcome.NETWORK_ERROR)
        assertEquals(SyncWriteResult.NetworkFailure, repo(gateway).write(dto, expected))
    }

    @Test
    fun invalidArgument_isValidationFailure() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(createOutcome = RawWriteOutcome.INVALID_ARGUMENT)
        assertEquals(SyncWriteResult.ValidationFailure, repo(gateway).write(dto, expected))
    }

    @Test
    fun unknown_isUnknownFailure() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(createOutcome = RawWriteOutcome.UNKNOWN)
        assertEquals(SyncWriteResult.UnknownFailure, repo(gateway).write(dto, expected))
    }

    @Test
    fun thrownGatewayError_mapsToUnknownFailure_neverPropagates() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(createThrows = IllegalStateException("boom"))
        assertEquals(SyncWriteResult.UnknownFailure, repo(gateway).write(dto, expected))
    }

    @Test
    fun payloadCarriesNoRawTrials() = runTest {
        val gateway = FakeFocusSessionFirestoreGateway(createOutcome = RawWriteOutcome.SUCCESS)
        repo(gateway).write(dto, expected)
        val json = Json.encodeToString(gateway.created.single())
        assertFalse(json.contains("reactionTime"))
        assertFalse(json.contains("trials"))
        assertFalse(json.contains("blocks"))
        assertFalse(json.contains("subjectiveState"))
    }
}
