package com.mindos.app.features.profile.card

import com.mindos.app.features.profile.card.data.InMemoryProfileCardMediaRepository
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileCardMediaScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProfileCardMediaRepositoryTest {

    private val bytes = byteArrayOf(1, 2, 3, 4)

    @Test
    fun upload_public_returnsPublicScopedMetadata() = runTest {
        val repo = InMemoryProfileCardMediaRepository()
        val media = repo.upload("u1", ProfileCardMediaScope.PUBLIC, bytes, "image/gif")
        assertTrue(media.storagePath.startsWith("profile_cards_public/u1/media/"))
        assertEquals(ProfileCardMediaKind.GIF, media.kind)
        assertEquals(bytes.size.toLong(), media.sizeBytes)
        assertEquals(listOf(media.storagePath), repo.uploadedPaths)
    }

    @Test
    fun upload_private_usesPrivateScope() = runTest {
        val repo = InMemoryProfileCardMediaRepository()
        val media = repo.upload("u1", ProfileCardMediaScope.PRIVATE, bytes, "image/jpeg")
        assertTrue(media.storagePath.startsWith("profile_cards_private/u1/media/"))
        assertEquals(ProfileCardMediaKind.IMAGE, media.kind)
    }

    @Test
    fun delete_recordsPath() = runTest {
        val repo = InMemoryProfileCardMediaRepository()
        repo.delete("u1", "profile_cards_public/u1/media/m0.jpg")
        assertEquals(listOf("profile_cards_public/u1/media/m0.jpg"), repo.deletedPaths)
    }

    @Test
    fun upload_propagatesConfiguredFailure() = runTest {
        val repo = InMemoryProfileCardMediaRepository()
        repo.failUploadWith = IllegalStateException("network")
        assertFailsWith<IllegalStateException> {
            repo.upload("u1", ProfileCardMediaScope.PUBLIC, bytes, "image/jpeg")
        }
    }
}
