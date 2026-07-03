package com.mindos.app.features.profile.card

import com.mindos.app.features.profile.card.data.InMemoryProfileCardMediaRepository
import com.mindos.app.features.profile.card.data.InMemoryProfileCardRepository
import com.mindos.app.features.profile.card.domain.ProfileCanvasItem
import com.mindos.app.features.profile.card.domain.ProfileCanvasItemType
import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileCardState
import com.mindos.app.features.profile.card.domain.appearance
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
class ProfileCardCanvasPersistenceTest {

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

    @Test
    fun addCanvasMedia_uploadsBinary_andPersistsItemToPrivateAndPublic() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.addCanvasMedia(byteArrayOf(1, 2), "image/jpeg")
        val item = h.state.canvasItems.last()
        assertNotNull(h.media[item.imageRef]?.bytes, "local preview is immediate")

        advanceUntilIdle()
        assertEquals(1, mediaRepo.uploadedPaths.size)
        val priv = cardRepo.load("u1")!!
        assertEquals(1, priv.canvas.size, "only the committed-media item persists (seed placeholders do not)")
        assertEquals(item.id, priv.canvas.first().id)
        assertNotNull(priv.canvas.first().media)
        assertEquals(1, cardRepo.loadPublic("u1")!!.canvas.size, "auto-published to public snapshot")
        coroutineContext.cancelChildren()
    }

    @Test
    fun restore_reconstructsCanvasItemsFromFirestore() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val media = ProfileCardMedia(
            id = "cm1",
            kind = ProfileCardMediaKind.IMAGE,
            storagePath = "profile_cards_public/u1/media/cm1.jpg",
            downloadUrl = "https://test.local/cm1",
        )
        val persisted = ProfileCanvasItem(
            id = 7, type = ProfileCanvasItemType.MEDIA, media = media,
            x = 10f, y = 20f, width = 100f, height = 80f, rotation = 0f, z = 3, locked = false,
        )
        cardRepo.save(
            "u1",
            ProfileCardDoc(appearance = ProfileCardState(username = "x").appearance(), canvas = listOf(persisted)),
        )

        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()
        val items = h.state.canvasItems
        assertEquals(1, items.size, "restored canvas replaces the seed demo items")
        val restored = items.first()
        assertEquals(7L, restored.id)
        assertEquals(10f, restored.x)
        assertEquals(3, restored.z)
        assertEquals("profile_cards_public/u1/media/cm1.jpg", h.media[restored.imageRef]?.storagePath)
        assertNull(h.media[restored.imageRef]?.bytes)
        coroutineContext.cancelChildren()
    }

    @Test
    fun movingItem_persistsNewGeometry() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.addCanvasMedia(byteArrayOf(1), "image/jpeg")
        advanceUntilIdle()
        val item = h.state.canvasItems.last()

        h.moveCanvasItem(item.id, 20f, 33f, 1000f, 1000f)
        advanceUntilIdle()
        val saved = cardRepo.load("u1")!!.canvas.first { it.id == item.id }
        assertEquals(item.x + 20f, saved.x)
        assertEquals(item.y + 33f, saved.y)
        coroutineContext.cancelChildren()
    }

    @Test
    fun removingItem_deletesMedia_andDropsItFromDoc() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository()
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.addCanvasMedia(byteArrayOf(1), "image/jpeg")
        advanceUntilIdle()
        val item = h.state.canvasItems.last()
        val path = mediaRepo.uploadedPaths.single()

        h.removeCanvasItem(item.id)
        advanceUntilIdle()
        assertTrue(h.state.canvasItems.none { it.id == item.id })
        assertTrue(mediaRepo.deletedPaths.contains(path), "media deleted best-effort (no orphan)")
        assertTrue(cardRepo.load("u1")!!.canvas.none { it.id == item.id }, "item dropped from persisted doc")
        coroutineContext.cancelChildren()
    }

    @Test
    fun uploadFailure_keepsPreview_tracksFailure_andRetrySucceeds() = runTest {
        val cardRepo = InMemoryProfileCardRepository()
        val mediaRepo = InMemoryProfileCardMediaRepository().apply { failUploadWith = IllegalStateException("sin red") }
        val h = holder(cardRepo, mediaRepo, this)
        advanceUntilIdle()

        h.addCanvasMedia(byteArrayOf(9), "image/jpeg")
        advanceUntilIdle()
        val item = h.state.canvasItems.last()
        assertTrue(h.canvasUploadFailures.contains(item.id))
        assertNotNull(h.media[item.imageRef]?.bytes, "preview NOT lost on canvas upload failure")
        assertEquals(0, cardRepo.load("u1")?.canvas?.size ?: 0, "failed item not persisted")

        h.retryCanvasMedia(item.id)
        advanceUntilIdle()
        assertTrue(h.canvasUploadFailures.isEmpty())
        assertEquals(1, mediaRepo.uploadedPaths.size)
        assertEquals(1, cardRepo.load("u1")!!.canvas.size)
        coroutineContext.cancelChildren()
    }
}
