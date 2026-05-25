package com.example.neuv3.features.dm.data.mapper

import com.example.neuv3.features.dm.data.model.DMChatDto
import com.example.neuv3.features.dm.data.model.DMMessageDto
import com.example.neuv3.features.dm.domain.model.DMMessageType
import com.example.neuv3.features.dm.domain.model.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DMMapperTest {

    @Test
    fun chatDtoKeepsConversationIdentityAndParticipants() {
        val chat = DMChatDto(
            participants = listOf("user-one", "user_two"),
            participantKey = "dm1_8_user-one_8_user_two",
            lastMessage = "Video",
            lastMessageType = "Video",
            lastTimestamp = 20L,
            updatedAt = 30L,
            lastReadAtByUser = mapOf("user-one" to 10L),
        ).toDomain("dm1_8_user-one_8_user_two")

        assertEquals("dm1_8_user-one_8_user_two", chat.id)
        assertEquals(listOf("user-one", "user_two"), chat.participants)
        assertEquals(DMMessageType.Video, chat.lastMessageType)
        assertEquals(10L, chat.lastReadAtByUser["user-one"])
    }

    @Test
    fun messageDtoKeepsTextFileVideoAndAudioPayloadFields() {
        val cases = listOf(
            DMMessageDto(
                chatId = "chat-id",
                senderId = "sender-id",
                type = "Text",
                status = "Sent",
                text = "hola",
                createdAt = 1L,
            ) to DMMessageType.Text,
            DMMessageDto(
                chatId = "chat-id",
                senderId = "sender-id",
                type = "File",
                status = "Sent",
                attachmentKind = "file",
                attachmentUrl = "dm_media/chat-id/file.pdf",
                attachmentMimeType = "application/pdf",
                attachmentName = "file.pdf",
                attachmentSizeBytes = 2_048L,
                createdAt = 2L,
            ) to DMMessageType.File,
            DMMessageDto(
                chatId = "chat-id",
                senderId = "sender-id",
                type = "Video",
                status = "Sending",
                attachmentKind = "video",
                attachmentUrl = "dm_media/chat-id/video.mp4",
                attachmentMimeType = "video/mp4",
                attachmentName = "video.mp4",
                attachmentSizeBytes = 4_096L,
                thumbnailUrl = "dm_media/chat-id/thumb.jpg",
                durationMs = 3_000L,
                width = 1280,
                height = 720,
                createdAt = 3L,
            ) to DMMessageType.Video,
            DMMessageDto(
                chatId = "chat-id",
                senderId = "sender-id",
                type = "Audio",
                status = "Read",
                attachmentKind = "audio",
                attachmentUrl = "dm_media/chat-id/audio.m4a",
                attachmentMimeType = "audio/mp4",
                attachmentName = "audio.m4a",
                attachmentSizeBytes = 1_024L,
                durationMs = 5_000L,
                createdAt = 4L,
            ) to DMMessageType.Audio,
        )

        cases.forEachIndexed { index, (dto, expectedType) ->
            val message = dto.toDomain("message-$index")

            assertEquals("message-$index", message.id)
            assertEquals(expectedType, message.type)
            assertEquals(dto.status.let { runCatching { MessageStatus.valueOf(it) }.getOrDefault(MessageStatus.Sent) }, message.status)
            assertEquals(dto.text, message.text)
            assertEquals(dto.attachmentUrl, message.attachmentUrl)
            assertEquals(dto.attachmentMimeType, message.attachmentMimeType)
            assertEquals(dto.attachmentName, message.attachmentName)
            assertEquals(dto.attachmentSizeBytes, message.attachmentSizeBytes)
            assertEquals(dto.thumbnailUrl, message.thumbnailUrl)
            assertEquals(dto.durationMs, message.durationMs)
            assertEquals(dto.width, message.width)
            assertEquals(dto.height, message.height)
            assertEquals(dto.createdAt, message.createdAt)
        }
    }
}
