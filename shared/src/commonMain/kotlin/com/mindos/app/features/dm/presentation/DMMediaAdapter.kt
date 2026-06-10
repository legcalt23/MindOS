package com.mindos.app.features.dm.presentation

import com.mindos.app.features.dm.domain.model.DMMessage
import com.mindos.app.features.dm.domain.model.DMMessageType
import com.mindos.app.features.social.domain.model.AppMediaContent
import com.mindos.app.features.social.shared.util.MediaContentResolver
import com.mindos.app.features.social.shared.util.MediaContentType

internal fun DMMessage.toAppMediaContent(): AppMediaContent? {
    val storagePath = attachmentUrl?.takeIf { it.isNotBlank() } ?: return null
    val mimeType = attachmentMimeType?.takeIf { it.isNotBlank() }
        ?: MediaContentResolver.resolve(null, storagePath).fallbackMimeType(type)

    return AppMediaContent(
        url = storagePath,
        storagePath = storagePath,
        fileName = attachmentName?.takeIf { it.isNotBlank() } ?: defaultAttachmentName(type, mimeType),
        mimeType = mimeType,
        sizeBytes = attachmentSizeBytes ?: 0L,
        thumbnailUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
    )
}

internal fun DMMessage.resolvedMediaContentType(): MediaContentType =
    MediaContentResolver.resolve(
        mimeType = attachmentMimeType,
        url = attachmentUrl ?: thumbnailUrl ?: attachmentName,
    )

private fun MediaContentType.fallbackMimeType(messageType: DMMessageType): String =
    when (this) {
        MediaContentType.Image -> "image/jpeg"
        MediaContentType.Video -> "video/mp4"
        MediaContentType.Audio -> "audio/mp4"
        MediaContentType.Pdf -> "application/pdf"
        MediaContentType.Text -> "text/plain"
        MediaContentType.Unsupported,
        MediaContentType.Unknown -> messageType.defaultMimeType()
    }

private fun DMMessageType.defaultMimeType(): String =
    when (this) {
        DMMessageType.Text -> "text/plain"
        DMMessageType.Image -> "image/jpeg"
        DMMessageType.Video -> "video/mp4"
        DMMessageType.Audio -> "audio/mp4"
        DMMessageType.File -> "application/octet-stream"
    }

private fun defaultAttachmentName(type: DMMessageType, mimeType: String): String =
    when (type) {
        DMMessageType.Text -> "mensaje.txt"
        DMMessageType.Image -> "imagen.jpg"
        DMMessageType.Video -> "video.mp4"
        DMMessageType.Audio -> "audio.m4a"
        DMMessageType.File -> when (MediaContentResolver.resolve(mimeType, null)) {
            MediaContentType.Pdf -> "archivo.pdf"
            MediaContentType.Text -> "archivo.txt"
            else -> "archivo"
        }
    }
