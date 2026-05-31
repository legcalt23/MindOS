package com.example.mindos.features.profile

import com.example.mindos.features.profile.data.InMemoryMindOSProfileRepository
import com.example.mindos.features.profile.data.InMemoryProfileObserverRepository
import com.example.mindos.features.profile.data.InMemoryProfilePresenceRepository
import com.example.mindos.features.profile.domain.media.decodeImagePixelSize
import com.example.mindos.features.profile.presentation.MindOSProfileStateHolder
import com.example.mindos.features.profile.presentation.state.MindOSProfileUiState
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MindOSProfileIconValidationTest {

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return output.toByteArray()
    }

    private fun upload(bytes: ByteArray) =
        ProfileMediaUpload(kind = ProfileMediaUploadKind.Photo, bytes = bytes, mimeType = "image/png")

    @Test
    fun decodeReadsDimensions() {
        val size = decodeImagePixelSize(pngBytes(20, 10))
        assertNotNull(size)
        assertEquals(20, size.width)
        assertEquals(10, size.height)
        assertFalse(size.isSquare)
        assertTrue(decodeImagePixelSize(pngBytes(16, 16))!!.isSquare)
    }

    @Test
    fun nonSquareIconRejectedAndSquareAccepted() = runTest {
        val holder = MindOSProfileStateHolder(
            profileRepository = InMemoryMindOSProfileRepository(),
            observerRepository = InMemoryProfileObserverRepository(),
            presenceRepository = InMemoryProfilePresenceRepository(),
            scope = this,
        )
        holder.load(viewerId = "u1", ownerId = "u1", username = "currolf", isOwnProfile = true)
        advanceUntilIdle()
        holder.openLinkEditor(0)

        holder.onLinkIconPicked(upload(pngBytes(32, 16)))
        val rejected = assertIs<MindOSProfileUiState.Loaded>(holder.uiState)
        assertNotNull(rejected.linkEditor?.validationError)

        holder.onLinkIconPicked(upload(pngBytes(24, 24)))
        val accepted = assertIs<MindOSProfileUiState.Loaded>(holder.uiState)
        assertNull(accepted.linkEditor?.validationError)
        assertNotNull(accepted.linkEditor?.pickedIconBytes)
        coroutineContext.cancelChildren()
    }
}
