package com.example.neuv3.features.dm.domain.usecase

import com.example.neuv3.core.security.SafeInput
import com.example.neuv3.features.dm.domain.model.DMMessageType
import com.example.neuv3.features.dm.domain.model.MessageStatus
import com.example.neuv3.features.dm.domain.repository.DMMessageDraft
import com.example.neuv3.features.dm.domain.repository.DMRepository
import kotlin.random.Random

class SendDMMessageUseCase(
    private val repository: DMRepository
) {
    suspend operator fun invoke(chatId: String, senderId: String, draft: DMMessageDraft) {
        require(isSafeDMChatId(chatId)) { "Chat ID is invalid" }
        SafeInput.requireSafeDocumentId(senderId, "senderId")
        val safeDraft = validateDraft(draft)

        val uniqueMessageId = generateMessageId()

        repository.createMessageRecord(
            chatId = chatId,
            messageId = uniqueMessageId,
            senderId = senderId,
            draft = safeDraft,
            status = MessageStatus.Sending,
        )

        if (safeDraft.type != DMMessageType.Text && safeDraft.mediaBytes != null) {
            try {
                val uploadResult = repository.uploadMessageMedia(chatId, uniqueMessageId, safeDraft)
                val updates = mutableMapOf<String, Any?>(
                    "attachmentUrl" to uploadResult.attachmentUrl,
                    "status" to MessageStatus.Sent.name
                )
                if (uploadResult.thumbnailUrl != null) {
                    updates["thumbnailUrl"] = uploadResult.thumbnailUrl
                }

                repository.updateMessage(chatId, uniqueMessageId, updates)
            } catch (e: Exception) {
                repository.updateMessage(chatId, uniqueMessageId, mapOf("status" to MessageStatus.Failed.name))
                throw e
            }
        } else {
            repository.updateMessage(chatId, uniqueMessageId, mapOf("status" to MessageStatus.Sent.name))
        }
    }

    private fun generateMessageId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..20).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun validateDraft(draft: DMMessageDraft): DMMessageDraft {
        val safeDraft = draft.copy(
            text = draft.text?.let { SafeInput.normalizeMultilineHumanText(it, MaxTextLength) },
            fileName = draft.fileName?.let { SafeInput.normalizeSingleLineHumanText(it, MaxFileNameLength) },
            mimeType = draft.mimeType?.lowercase()?.substringBefore(";")?.trim(),
        )
        draft.text?.let { text ->
            require(SafeInput.isSafeMultilineHumanText(text, MaxTextLength)) { "DM text contains invalid characters" }
        }
        safeDraft.fileName?.let { fileName ->
            require(fileName.isSafeDMFileName()) { "DM file name is invalid" }
        }
        safeDraft.mimeType?.let { mimeType ->
            require(mimeType.isNotBlank() && mimeType.length <= MaxMimeTypeLength) { "DM MIME type is invalid" }
        }
        when (safeDraft.type) {
            DMMessageType.Text -> require(!safeDraft.text.isNullOrBlank()) { "Text messages must not be blank" }
            DMMessageType.Image,
            DMMessageType.Video,
            DMMessageType.Audio,
            DMMessageType.File -> {
                require(safeDraft.mediaBytes?.isNotEmpty() == true) { "Media messages must include bytes" }
                require(safeDraft.type != DMMessageType.File) { "DM file attachments are not supported" }
                validateSafeAttachmentType(safeDraft)
                val sizeBytes = safeDraft.fileSizeBytes ?: safeDraft.mediaBytes.size.toLong()
                require(sizeBytes > 0L && sizeBytes <= maxAttachmentBytes(safeDraft)) {
                    "DM attachment size is invalid"
                }
            }
        }
        return safeDraft
    }

    private fun isSafeDMChatId(value: String): Boolean =
        value.length in 15..280 &&
            !SafeInput.hasBlockedControlChars(value) &&
            value.matches(Regex("^dm1_\\d+_[A-Za-z0-9_-]+_\\d+_[A-Za-z0-9_-]+$"))

    private fun String.isSafeDMFileName(): Boolean =
        isNotBlank() &&
            length <= MaxFileNameLength &&
            !contains("..") &&
            none { it == '/' || it == '\\' } &&
            !SafeInput.hasBlockedControlChars(this)

    private fun validateSafeAttachmentType(draft: DMMessageDraft) {
        val normalizedMime = draft.mimeType?.lowercase()?.substringBefore(";")?.trim()
        require(normalizedMime != null && normalizedMime in allowedMimeTypesFor(draft.type)) {
            "DM attachment MIME type is not supported"
        }

        val extension = draft.fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        if (extension != null) {
            require(extension in allowedExtensionsFor(normalizedMime)) {
                "DM attachment extension does not match MIME type"
            }
        }
    }

    private fun maxAttachmentBytes(draft: DMMessageDraft): Long =
        when (draft.type) {
            DMMessageType.Text -> 0L
            DMMessageType.Image -> MaxImageBytes
            DMMessageType.Video -> MaxVideoBytes
            DMMessageType.Audio -> MaxAudioBytes
            DMMessageType.File -> 0L
        }

    private fun allowedMimeTypesFor(type: DMMessageType): Set<String> =
        when (type) {
            DMMessageType.Image -> ImageMimeTypes
            DMMessageType.Video -> VideoMimeTypes
            DMMessageType.Audio -> AudioMimeTypes
            DMMessageType.Text,
            DMMessageType.File -> emptySet()
        }

    private fun allowedExtensionsFor(mimeType: String?): Set<String> =
        when (mimeType) {
            "image/jpeg" -> setOf("jpg", "jpeg")
            "image/png" -> setOf("png")
            "image/webp" -> setOf("webp")
            "video/mp4" -> setOf("mp4")
            "video/webm" -> setOf("webm")
            "audio/mp4" -> setOf("m4a")
            "audio/mpeg" -> setOf("mp3")
            "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" -> setOf("wav")
            else -> emptySet()
        }

    private companion object {
        const val MaxTextLength = 4_000
        const val MaxFileNameLength = 180
        const val MaxMimeTypeLength = 128
        const val MaxImageBytes = 10_485_760L
        const val MaxVideoBytes = 52_428_800L
        const val MaxAudioBytes = 26_214_400L
        val ImageMimeTypes = setOf("image/jpeg", "image/png", "image/webp")
        val VideoMimeTypes = setOf("video/mp4", "video/webm")
        val AudioMimeTypes = setOf("audio/mp4", "audio/mpeg", "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave")
    }
}
