package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.ConflictReason
import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import com.mindos.app.features.mindboost.core.sync.RemoteSessionProbe
import com.mindos.app.features.mindboost.core.sync.SyncWriteResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FocusSessionRemoteRepositoryTest {

    private val dto = FocusSessionCloudMapper.toCloudDtoOrNull(focusSession(userId = "user-1"), "user-1", nowMs = 1L)!!
    private val expected = FocusSessionCloudMapper.fingerprintOf(dto)

    private fun repo(source: FakeFocusSessionRemoteDataSource) = FocusSessionRemoteRepository(source)

    @Test
    fun success_isSynced() = runTest {
        val source = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.SUCCESS)
        assertEquals(SyncWriteResult.Synced, repo(source).write(dto, expected))
        assertEquals(0, source.probeCalls, "no probe on a fresh write")
    }

    @Test
    fun alreadyExists_withMatchingRemote_isDuplicate() = runTest {
        val source = FakeFocusSessionRemoteDataSource(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            probeResult = RemoteSessionProbe(exists = true, userId = "user-1", fingerprint = expected),
        )
        assertEquals(SyncWriteResult.AlreadySyncedDuplicate, repo(source).write(dto, expected))
        assertEquals(1, source.probeCalls, "probe required to verify an existing document")
    }

    @Test
    fun alreadyExists_withDivergentFingerprint_isConflict() = runTest {
        val diverged = expected.copy(normalizedScore = 0.9)
        val source = FakeFocusSessionRemoteDataSource(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            probeResult = RemoteSessionProbe(exists = true, userId = "user-1", fingerprint = diverged),
        )
        assertEquals(
            SyncWriteResult.Conflict(ConflictReason.DIFFERENT_FINGERPRINT),
            repo(source).write(dto, expected),
        )
    }

    @Test
    fun alreadyExists_withUnreadableRemote_isUnverifiedConflict_notDuplicate() = runTest {
        val source = FakeFocusSessionRemoteDataSource(
            createOutcome = RawWriteOutcome.ALREADY_EXISTS,
            probeResult = null,
        )
        assertEquals(
            SyncWriteResult.Conflict(ConflictReason.UNVERIFIED_EXISTING),
            repo(source).write(dto, expected),
        )
    }

    @Test
    fun permissionDenied_isNeverSynced() = runTest {
        val source = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.PERMISSION_DENIED)
        val result = repo(source).write(dto, expected)
        assertEquals(SyncWriteResult.PermissionDenied, result)
    }

    @Test
    fun networkError_isRetryableFailure() = runTest {
        val source = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.NETWORK_ERROR)
        assertEquals(SyncWriteResult.NetworkFailure, repo(source).write(dto, expected))
    }

    @Test
    fun invalidArgument_isValidationFailure() = runTest {
        val source = FakeFocusSessionRemoteDataSource(createOutcome = RawWriteOutcome.INVALID_ARGUMENT)
        assertEquals(SyncWriteResult.ValidationFailure, repo(source).write(dto, expected))
    }

    @Test
    fun thrownSdkError_mapsToUnknownFailure_neverPropagates() = runTest {
        val source = FakeFocusSessionRemoteDataSource(createThrows = IllegalStateException("SDK blew up"))
        assertEquals(SyncWriteResult.UnknownFailure, repo(source).write(dto, expected))
    }
}
