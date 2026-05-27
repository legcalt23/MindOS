package com.example.mindos.features.dm.presentation

import com.example.mindos.features.dm.domain.model.DMMessage
import com.example.mindos.features.dm.domain.model.DMMessageType
import com.example.mindos.features.dm.domain.model.MessageStatus
import com.example.mindos.features.social.shared.util.MediaContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DMMediaAdapterTest {

    @Test
    fun videoMessageMapsToReusableMediaContentWithThumbnail() {
        val message = message(
            type = DMMessageType.Video,
            attachmentUrl = "dm_media/chat/video.mp4",
            attachmentMimeType = "video/mp4",
            attachmentName = "clip.mp4",
            attachmentSizeBytes = 1_024L,
            thumbnailUrl = "dm_media/chat/thumb.jpg",
        )

        val content = message.toAppMediaContent()

        requireNotNull(content)
        assertEquals("dm_media/chat/video.mp4", content.storagePath)
        assertEquals("dm_media/chat/video.mp4", content.url)
        assertEquals("clip.mp4", content.fileName)
        assertEquals("video/mp4", content.mimeType)
        assertEquals("dm_media/chat/thumb.jpg", content.thumbnailUrl)
        assertEquals(MediaContentType.Video, message.resolvedMediaContentType())
    }

    @Test
    fun fileMessageKeepsPdfTypeForViewer() {
        val message = message(
            type = DMMessageType.File,
            attachmentUrl = "dm_media/chat/report.pdf",
            attachmentMimeType = "application/pdf",
            attachmentName = "report.pdf",
        )

        val content = message.toAppMediaContent()

        requireNotNull(content)
        assertEquals("application/pdf", content.mimeType)
        assertEquals(MediaContentType.Pdf, message.resolvedMediaContentType())
    }

    @Test
    fun messageWithoutAttachmentDoesNotOpenViewer() {
        val message = message(type = DMMessageType.File)

        assertNull(message.toAppMediaContent())
    }

    private fun message(
        type: DMMessageType,
        attachmentUrl: String? = null,
        attachmentMimeType: String? = null,
        attachmentName: String? = null,
        attachmentSizeBytes: Long? = null,
        thumbnailUrl: String? = null,
    ): DMMessage =
        DMMessage(
            id = "message-id",
            chatId = "chat-id",
            senderId = "sender-id",
            type = type,
            status = MessageStatus.Sent,
            text = null,
            attachmentKind = type.name.lowercase(),
            attachmentUrl = attachmentUrl,
            attachmentMimeType = attachmentMimeType,
            attachmentName = attachmentName,
            attachmentSizeBytes = attachmentSizeBytes,
            thumbnailUrl = thumbnailUrl,
            durationMs = null,
            width = null,
            height = null,
            createdAt = 0L,
        )
}
