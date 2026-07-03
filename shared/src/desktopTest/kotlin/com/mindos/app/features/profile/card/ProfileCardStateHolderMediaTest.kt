package com.mindos.app.features.profile.card

import com.mindos.app.features.profile.card.data.InMemoryProfileCardMediaRepository
import com.mindos.app.features.profile.card.data.InMemoryProfileCardRepository
import com.mindos.app.features.profile.card.domain.BackgroundPaint
import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileCardState
import com.mindos.app.features.profile.card.domain.appearance
import com.mindos.app.features.profile.card.presentation.ProfileCardLoadStatus
import com.mindos.app.features.profile.card.presentation.ProfileCardMediaStatus
import com.mindos.app.features.profile.card.presentation.ProfileCardStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileCardStateHolderMediaTest {

    private fun holder(
        cardRepo: InMemoryProfileCardRepository,
        mediaRepo: InMemoryProfileCardMediaRepository,
        scope: CoroutineScope,
    ) = ProfileCardStateHolder(
        initial = ProfileCardState(username = "VXNESA"),
        userId = "u1",
        repository = cardRepo,
        mediaRepository = mediaRepo,
        scope = scope,
    )

    private fun ProfileCardStateHolder.backgroundRef(): String =
        (state.cardBackground as BackgroundPaint.Image).ref

    @Test
    fun setCardImage_showsLocalPreviewImmediately_thenUploadsAndAutoPublishes() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.setCardImage(byteArrayOf(1, 2, 3), "image/jpeg")
        val ref = h.backgroundRef()
        assertNotNull(h.media[ref]?.bytes, "local preview must be visible immediately")
        assertTrue(mediaRepo.uploadedPaths.isEmpty(), "no upload before the coroutine runs")

        advanceUntilIdle()
        assertEquals(1, mediaRepo.uploadedPaths.size)
        assertNotNull(cardRepo.load("u1")?.backgroundMedia, "private draft persisted")
        assertNotNull(cardRepo.loadPublic("u1")?.backgroundMedia, "public snapshot auto-published")
        assertEquals(ProfileCardMediaStatus.Saved, h.backgroundStatus)
        assertNotNull(h.media[ref]?.bytes, "preview bytes preserved after save")
        assertNotNull(h.media[ref]?.storagePath, "remote ref set after upload")
        coroutineContext.cancelChildren()
    }

    @Test
    fun uploadFailure_keepsLocalPreview_andRetrySucceeds() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository().apply { failUploadWith = IllegalStateException("sin conexión") }
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.setCardImage(byteArrayOf(1, 2, 3), "image/jpeg")
        advanceUntilIdle()
        val ref = h.backgroundRef()
        assertTrue(h.backgroundStatus is ProfileCardMediaStatus.UploadFailed)
        assertNotNull(h.media[ref]?.bytes, "preview NOT lost on upload failure")
        assertTrue(h.state.cardBackground is BackgroundPaint.Image)
        assertNull(cardRepo.load("u1"), "nothing persisted on upload failure")

        h.retryBackgroundMedia()
        advanceUntilIdle()
        assertEquals(ProfileCardMediaStatus.Saved, h.backgroundStatus)
        assertEquals(1, mediaRepo.uploadedPaths.size)
        assertNotNull(cardRepo.load("u1")?.backgroundMedia)
        coroutineContext.cancelChildren()
    }

    @Test
    fun saveFailure_keepsUploadedMedia_andRetrySavesWithoutReupload() = runTest {
        val cardRepo = InMemoryProfileCardRepository().apply { failSaveWith = IllegalStateException("firestore caído") }
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.setCardImage(byteArrayOf(1, 2, 3), "image/jpeg")
        advanceUntilIdle()
        assertTrue(h.backgroundStatus is ProfileCardMediaStatus.SaveFailed)
        assertEquals(1, mediaRepo.uploadedPaths.size, "media already uploaded")
        assertNull(cardRepo.load("u1"), "not saved on Firestore failure")

        h.retryBackgroundMedia()
        advanceUntilIdle()
        assertEquals(ProfileCardMediaStatus.Saved, h.backgroundStatus)
        assertEquals(1, mediaRepo.uploadedPaths.size, "retry must NOT re-upload an already uploaded binary")
        assertNotNull(cardRepo.load("u1")?.backgroundMedia)
        coroutineContext.cancelChildren()
    }

    @Test
    fun restore_reconstructsRemoteBackgroundFromFirestore() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val media = ProfileCardMedia(
            id = "m1",
            kind = ProfileCardMediaKind.IMAGE,
            storagePath = "profile_cards_public/u1/media/m1.jpg",
            downloadUrl = "https://test.local/m1",
        )
        cardRepo.save(
            "u1",
            ProfileCardDoc(
                appearance = ProfileCardState(username = "x").appearance().copy(accentArgb = 0xFF445566L),
                backgroundMedia = media,
            ),
        )

        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()
        assertEquals(ProfileCardLoadStatus.Loaded, h.loadStatus)
        assertTrue(h.state.cardBackground is BackgroundPaint.Image)
        val ref = (h.state.cardBackground as BackgroundPaint.Image).ref
        assertEquals("profile_cards_public/u1/media/m1.jpg", h.media[ref]?.storagePath)
        assertEquals("https://test.local/m1", h.media[ref]?.downloadUrl)
        assertNull(h.media[ref]?.bytes, "restored media has no local bytes")
        assertEquals(0xFF445566L, h.state.accentArgb)
        coroutineContext.cancelChildren()
    }

    @Test
    fun replacingBackground_deletesPreviousMediaAfterCommit() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.setCardImage(byteArrayOf(1), "image/jpeg")
        advanceUntilIdle()
        val firstPath = mediaRepo.uploadedPaths.single()

        h.setCardImage(byteArrayOf(2), "image/png")
        advanceUntilIdle()
        assertEquals(2, mediaRepo.uploadedPaths.size)
        assertTrue(mediaRepo.deletedPaths.contains(firstPath), "previous media deleted best-effort after commit")
        coroutineContext.cancelChildren()
    }

    @Test
    fun reset_clearsRefsInPrivateAndPublic_andDeletesMedia() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.setCardImage(byteArrayOf(1), "image/jpeg")
        advanceUntilIdle()
        val path = mediaRepo.uploadedPaths.single()

        h.resetCardBackground()
        advanceUntilIdle()
        assertTrue(h.state.cardBackground !is BackgroundPaint.Image, "reverted to default gradient")
        assertNull(cardRepo.load("u1")?.backgroundMedia, "private ref cleared")
        assertNull(cardRepo.loadPublic("u1")?.backgroundMedia, "public ref cleared")
        assertTrue(mediaRepo.deletedPaths.contains(path), "old media deleted")
        coroutineContext.cancelChildren()
    }

    @Test
    fun discardingAfterSaveFailure_deletesTheUploadedMedia_noOrphan() = runTest {
        val cardRepo = InMemoryProfileCardRepository().apply { failSaveWith = IllegalStateException("firestore") }
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.setCardImage(byteArrayOf(1, 2, 3), "image/jpeg")
        advanceUntilIdle()
        assertTrue(h.backgroundStatus is ProfileCardMediaStatus.SaveFailed)
        val uploadedPath = mediaRepo.uploadedPaths.single()

        h.discardBackgroundMedia()
        advanceUntilIdle()
        assertTrue(mediaRepo.deletedPaths.contains(uploadedPath), "discarded upload must be deleted (no orphan)")
        assertTrue(h.state.cardBackground !is BackgroundPaint.Image, "reverted to default (no previous bg)")
        coroutineContext.cancelChildren()
    }
}
