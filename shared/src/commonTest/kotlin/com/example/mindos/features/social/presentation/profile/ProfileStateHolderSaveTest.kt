package com.example.mindos.features.social.presentation.profile

import com.example.mindos.features.social.data.local.InMemorySocialGraphComponent
import com.example.mindos.features.social.domain.error.ProfileSaveAppCheckFailed
import com.example.mindos.features.social.domain.error.ProfileSaveException
import com.example.mindos.features.social.domain.model.ProfileCoverMediaMaxBytes
import com.example.mindos.features.social.domain.error.ProfileSavePermissionDenied
import com.example.mindos.features.social.domain.model.ProfileMediaMeta
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import com.example.mindos.features.social.domain.repository.ProfileVisualRepository
import com.example.mindos.features.social.domain.valueobject.Bio
import com.example.mindos.features.social.domain.valueobject.DisplayName
import com.example.mindos.features.social.domain.valueobject.Handle
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.IncrementingAppClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileStateHolderSaveTest {
    @Test
    fun singleTapSuccessCommitsOnceAndClosesEditor() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.updateDisplayName("Owner Updated")
        holder.saveEdit()

        assertEquals(1, repo.saveCalls)
        assertEquals(0, repo.uploadCalls)
        assertEquals("Owner Updated", holder.uiState.visualConfig?.displayName)
        assertFalse(holder.uiState.editor.isEditing)
        assertEquals("Perfil visual guardado.", holder.uiState.editor.feedbackMessage)
        assertNull(holder.uiState.editor.errorMessage)
        assertNull(holder.uiState.editor.lastSaveErrorCode)
    }

    @Test
    fun usernameChangeCommitsAndReloadsFromRepository() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.updateUsername("@Curro_2")
        holder.saveEdit()

        assertEquals(1, repo.saveCalls)
        assertEquals("curro_2", repo.config?.username)
        assertEquals("curro_2", holder.uiState.visualConfig?.username)

        holder.load(viewerId = Owner, ownerId = Owner)

        assertEquals("curro_2", holder.uiState.visualConfig?.username)
        assertFalse(holder.uiState.editor.isEditing)
    }

    @Test
    fun userphraseChangeCommitsAndReloadsFromRepository() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.updateUserphrase("Frase publica real")
        holder.saveEdit()

        assertEquals(1, repo.saveCalls)
        assertEquals("Frase publica real", repo.config?.userphrase)
        assertEquals("Frase publica real", holder.uiState.visualConfig?.userphrase)
    }

    @Test
    fun inlineSaveKeepsEditModeAfterCommittingUserphrase() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.updateUserphrase("Frase inline")
        holder.saveEdit(keepEditing = true)

        assertEquals(1, repo.saveCalls)
        assertTrue(holder.uiState.editor.isEditing)
        assertEquals("Frase inline", holder.uiState.editor.draft?.userphrase)
        assertEquals("Frase inline", holder.uiState.visualConfig?.userphrase)
    }

    @Test
    fun userphraseWithEmojiFailsValidationBeforeRemoteWrite() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.updateUserphrase("Frase con emoji 😀")
        holder.saveEdit()

        assertEquals(0, repo.saveCalls)
        assertEquals("profile_save_validation_error", holder.uiState.editor.lastSaveErrorCode)
        assertNotNull(holder.uiState.editor.errorMessage)
        assertEquals("", repo.config?.userphrase)
    }

    @Test
    fun userphraseLongerThanOneHundredCharsIsTrimmedBeforeRemoteWrite() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.updateUserphrase("a".repeat(101))
        holder.saveEdit()

        assertEquals(1, repo.saveCalls)
        assertEquals("a".repeat(100), repo.config?.userphrase)
        assertEquals("a".repeat(100), holder.uiState.visualConfig?.userphrase)
    }

    @Test
    fun rapidRepeatedTapsRunOneRemoteSaveOnly() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        repo.saveGate = CompletableDeferred()
        val holder = loadedHolder(repo)
        holder.updateDisplayName("Owner Updated")

        val first = launch { holder.saveEdit() }
        runCurrent()
        val second = launch { holder.saveEdit() }
        val third = launch { holder.saveEdit() }
        runCurrent()

        assertTrue(holder.uiState.editor.isSaving)
        assertEquals(1, repo.saveCalls)
        assertEquals(1, repo.maxConcurrentSaves)

        repo.saveGate?.complete(Unit)
        first.join()
        second.join()
        third.join()
        advanceUntilIdle()

        assertEquals(1, repo.saveCalls)
        assertEquals("Owner Updated", holder.uiState.visualConfig?.displayName)
        assertFalse(holder.uiState.editor.isEditing)
    }

    @Test
    fun authUnavailableFailsBeforeAnyRemoteWrite() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)
        holder.updateDisplayName("Owner Updated")
        repo.authenticatedUserId = null

        holder.saveEdit()

        assertEquals(0, repo.uploadCalls)
        assertEquals(0, repo.saveCalls)
        assertEquals("profile_save_not_authenticated", holder.uiState.editor.lastSaveErrorCode)
        assertTrue(holder.uiState.editor.isEditing)
    }

    @Test
    fun permissionDeniedIsMappedToDomainError() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        repo.saveFailure = permissionDenied()
        val holder = loadedHolder(repo)
        holder.updateDisplayName("Owner Updated")

        holder.saveEdit()

        assertEquals(1, repo.saveCalls)
        assertEquals("profile_save_permission_denied", holder.uiState.editor.lastSaveErrorCode)
        assertNull(holder.uiState.editor.feedbackMessage)
        assertNotNull(holder.uiState.editor.errorMessage)
    }

    @Test
    fun appCheckFailureIsExposedWithDedicatedErrorCode() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        repo.saveFailure = appCheckFailed()
        val holder = loadedHolder(repo)
        holder.updateDisplayName("Owner Updated")

        holder.saveEdit()

        assertEquals(1, repo.saveCalls)
        assertEquals("profile_save_app_check_failed", holder.uiState.editor.lastSaveErrorCode)
        assertNull(holder.uiState.editor.feedbackMessage)
        assertNotNull(holder.uiState.editor.errorMessage)
    }

    @Test
    fun storageFailureBeforeCommitDoesNotWriteFirestore() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        repo.uploadFailure = permissionDenied(operation = "storage_upload_profile_media")
        val holder = loadedHolder(repo)

        holder.setModuleMedia(ProfileModuleId.Main, photoUpload())
        holder.saveEdit()

        assertEquals(1, repo.uploadCalls)
        assertEquals(0, repo.saveCalls)
        assertEquals("profile_save_permission_denied", holder.uiState.editor.lastSaveErrorCode)
        assertTrue(holder.uiState.editor.isEditing)
    }

    @Test
    fun zeroDurationVideoIsRejectedBeforeAnyUploadOrCommit() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.setModuleMedia(ProfileModuleId.Main, videoUpload(duration = 0L))

        assertNotNull(holder.uiState.editor.errorMessage)
        assertTrue(holder.uiState.editor.pendingMedia.isEmpty())

        holder.saveEdit()

        assertEquals(0, repo.uploadCalls)
        assertEquals(0, repo.saveCalls)
        assertEquals("No hay cambios pendientes.", holder.uiState.editor.feedbackMessage)
        assertNull(holder.uiState.editor.errorMessage)
        assertNull(holder.uiState.editor.lastSaveErrorCode)
    }

    @Test
    fun coverPhotoLargerThanFifteenMbIsRejectedBeforeAnyUploadOrCommit() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.setModuleMedia(
            ProfileModuleId.Main,
            ProfileMediaUpload(
                kind = ProfileMediaUploadKind.Photo,
                bytes = ByteArray((ProfileCoverMediaMaxBytes + 1L).toInt()),
                mimeType = "image/jpeg",
            ),
        )

        assertEquals("La portada no puede superar 15 MB.", holder.uiState.editor.errorMessage)
        assertTrue(holder.uiState.editor.pendingMedia.isEmpty())

        holder.saveEdit()

        assertEquals(0, repo.uploadCalls)
        assertEquals(0, repo.saveCalls)
    }

    @Test
    fun coverVideoLargerThanFifteenMbIsRejectedBeforeAnyUploadOrCommit() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.setModuleMedia(
            ProfileModuleId.Main,
            ProfileMediaUpload(
                kind = ProfileMediaUploadKind.Video,
                bytes = ByteArray((ProfileCoverMediaMaxBytes + 1L).toInt()),
                mimeType = "video/mp4",
                durationMillis = 2_000L,
            ),
        )

        assertEquals("La portada no puede superar 15 MB.", holder.uiState.editor.errorMessage)
        assertTrue(holder.uiState.editor.pendingMedia.isEmpty())

        holder.saveEdit()

        assertEquals(0, repo.uploadCalls)
        assertEquals(0, repo.saveCalls)
    }

    @Test
    fun firestoreFailureAfterUploadCompensatesAndReportsPartialFailure() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        repo.saveFailure = permissionDenied(operation = "firestore_commit_profile_visual")
        val holder = loadedHolder(repo)

        holder.setModuleMedia(ProfileModuleId.Main, photoUpload())
        holder.saveEdit()

        assertEquals(1, repo.uploadCalls)
        assertEquals(1, repo.saveCalls)
        assertEquals(listOf("profile_visuals/owner/main/1_media.jpg"), repo.deletedPaths)
        assertEquals("profile_save_partial_remote_failure", holder.uiState.editor.lastSaveErrorCode)
        assertTrue(holder.uiState.editor.isEditing)
    }

    @Test
    fun noChangesDoesNotWrite() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        val holder = loadedHolder(repo)

        holder.saveEdit()

        assertEquals(0, repo.uploadCalls)
        assertEquals(0, repo.saveCalls)
        assertEquals("No hay cambios pendientes.", holder.uiState.editor.feedbackMessage)
        assertNull(holder.uiState.editor.lastSaveErrorCode)
        assertTrue(holder.uiState.editor.isEditing)
    }

    @Test
    fun nonCriticalCleanupFailureDoesNotFlipSuccessfulSaveToError() = runTest {
        val initial = defaultConfig().withSavedMedia(
            moduleId = ProfileModuleId.Main,
            media = ProfileMediaMeta(
                url = "https://example.invalid/profile_visuals/owner/main/1_old.jpg",
                storagePath = "profile_visuals/owner/main/1_old.jpg",
                mimeType = "image/jpeg",
                uploadedAt = Timestamp(1L),
            ),
        )
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = initial)
        repo.deleteFailure = IllegalStateException("cleanup denied")
        val holder = loadedHolder(repo)

        holder.clearModule(ProfileModuleId.Main)
        holder.saveEdit()

        assertEquals(1, repo.saveCalls)
        assertEquals(listOf("profile_visuals/owner/main/1_old.jpg"), repo.deletedPaths)
        assertEquals("Perfil visual guardado.", holder.uiState.editor.feedbackMessage)
        assertNull(holder.uiState.editor.errorMessage)
        assertNull(holder.uiState.editor.lastSaveErrorCode)
    }

    @Test
    fun aSaveAttemptNeverEmitsSuccessAndFailureTogether() = runTest {
        val repo = RecordingProfileVisualRepository(authenticatedUserId = Owner, config = defaultConfig())
        repo.saveFailure = IllegalStateException("remote unavailable")
        val holder = loadedHolder(repo)
        holder.updateDisplayName("Owner Updated")

        holder.saveEdit()

        assertNull(holder.uiState.editor.feedbackMessage)
        assertNotNull(holder.uiState.editor.errorMessage)
        assertEquals("profile_save_unknown_remote_failure", holder.uiState.editor.lastSaveErrorCode)
    }

    private suspend fun TestScope.loadedHolder(repo: RecordingProfileVisualRepository): ProfileStateHolder {
        val graph = InMemorySocialGraphComponent.create(clock = IncrementingAppClock(startEpochMillis = 100L))
        graph.seedUserIfMissing(
            userId = Owner,
            handle = Handle("owner"),
            displayName = DisplayName("Owner"),
            bio = Bio("Perfil visual MindOS."),
        )
        val holder = ProfileStateHolder(
            getProfile = graph.useCases.getProfile,
            followUser = graph.useCases.followUser,
            unfollowUser = graph.useCases.unfollowUser,
            blockUser = graph.useCases.blockUser,
            unblockUser = graph.useCases.unblockUser,
            profileVisualRepository = repo,
            clock = IncrementingAppClock(startEpochMillis = 1L),
        )
        holder.load(viewerId = Owner, ownerId = Owner)
        holder.enterEditMode()
        return holder
    }

    private class RecordingProfileVisualRepository(
        var authenticatedUserId: UserId?,
        var config: ProfileVisualConfig?,
    ) : ProfileVisualRepository {
        var saveCalls = 0
        var uploadCalls = 0
        var activeSaves = 0
        var maxConcurrentSaves = 0
        var saveFailure: Throwable? = null
        var uploadFailure: Throwable? = null
        var deleteFailure: Throwable? = null
        var saveGate: CompletableDeferred<Unit>? = null
        val deletedPaths = mutableListOf<String>()

        override fun currentAuthenticatedUserId(): UserId? =
            authenticatedUserId

        override suspend fun getConfig(userId: UserId): ProfileVisualConfig? =
            config

        override suspend fun saveConfig(config: ProfileVisualConfig, attemptId: String?) {
            saveCalls += 1
            activeSaves += 1
            maxConcurrentSaves = maxOf(maxConcurrentSaves, activeSaves)
            try {
                saveGate?.await()
                saveFailure?.let { throw it }
                this.config = config.normalized()
            } finally {
                activeSaves -= 1
            }
        }

        override suspend fun uploadModuleMedia(
            userId: UserId,
            moduleId: ProfileModuleId,
            upload: ProfileMediaUpload,
            uploadedAt: Timestamp,
            attemptId: String?,
        ): ProfileMediaMeta {
            uploadCalls += 1
            uploadFailure?.let { throw it }
            val storagePath = "profile_visuals/${userId.value}/${moduleId.storageFolder}/${uploadCalls}_media.jpg"
            return ProfileMediaMeta(
                url = "https://example.invalid/$storagePath",
                storagePath = storagePath,
                mimeType = upload.mimeType,
                durationMillis = upload.durationMillis,
                uploadedAt = uploadedAt,
            )
        }

        override suspend fun deleteMedia(userId: UserId, storagePath: String, attemptId: String?) {
            deletedPaths += storagePath
            deleteFailure?.let { throw it }
        }
    }

    private fun defaultConfig(): ProfileVisualConfig =
        ProfileVisualConfig.createDefault(
            userId = Owner,
            displayName = "Owner",
            username = "owner",
            observersCount = 0,
            now = Timestamp(1L),
        )

    private fun photoUpload(): ProfileMediaUpload =
        ProfileMediaUpload(
            kind = ProfileMediaUploadKind.Photo,
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "image/jpeg",
        )

    private fun videoUpload(duration: Long): ProfileMediaUpload =
        ProfileMediaUpload(
            kind = ProfileMediaUploadKind.Video,
            bytes = byteArrayOf(1, 2, 3),
            mimeType = "video/mp4",
            durationMillis = duration,
        )

    private fun permissionDenied(
        operation: String = "firestore_commit_profile_visual",
    ): ProfileSaveException =
        ProfileSaveException(
            ProfileSavePermissionDenied(
                attemptId = "fake_attempt",
                phase = "remote",
                operation = operation,
                userId = Owner,
                path = "profile_visuals/owner",
                causeMessage = "Missing or insufficient permissions.",
            ),
        )

    private fun appCheckFailed(
        operation: String = "firestore_commit_profile_visual",
    ): ProfileSaveException =
        ProfileSaveException(
            ProfileSaveAppCheckFailed(
                attemptId = "fake_attempt",
                phase = "remote",
                operation = operation,
                userId = Owner,
                path = "profile_visuals/owner",
                causeMessage = "App Check no pudo emitir un token.",
            ),
        )

    private companion object {
        val Owner = UserId("owner")
    }
}
