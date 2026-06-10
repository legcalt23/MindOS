package com.mindos.app.features.dm.domain.usecase

import com.mindos.app.features.dm.domain.model.DMChat
import com.mindos.app.features.dm.domain.model.DMMessage
import com.mindos.app.features.dm.domain.model.DMMessageType
import com.mindos.app.features.dm.domain.model.MessageStatus
import com.mindos.app.features.dm.domain.repository.DMMessageDraft
import com.mindos.app.features.dm.domain.repository.DMRepository
import com.mindos.app.features.dm.domain.repository.DMUser
import com.mindos.app.features.dm.domain.repository.MediaUploadResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SendDMMessageUseCaseTest {

    @Test
    fun textMessageCreatesRecordAndMarksSent() = runTest {
        val repository = FakeDMRepository()
        val useCase = SendDMMessageUseCase(repository)

        useCase(
            chatId = "dm1_5_alice_3_bob",
            senderId = "alice",
            draft = DMMessageDraft(type = DMMessageType.Text, text = "hola"),
        )

        assertEquals(MessageStatus.Sending, repository.created.single().status)
        assertEquals(mapOf("status" to MessageStatus.Sent.name), repository.updates.single().fields)
        assertTrue(repository.uploads.isEmpty())
    }

    @Test
    fun videoMessageUploadsMediaAndStoresThumbnail() = runTest {
        val repository = FakeDMRepository(
            uploadResult = MediaUploadResult(
                attachmentUrl = "dm_media/dm1_5_alice_3_bob/message.mp4",
                thumbnailUrl = "dm_media/dm1_5_alice_3_bob/thumb_message.jpg",
            )
        )
        val useCase = SendDMMessageUseCase(repository)

        useCase(
            chatId = "dm1_5_alice_3_bob",
            senderId = "alice",
            draft = DMMessageDraft(
                type = DMMessageType.Video,
                mediaBytes = byteArrayOf(1, 2, 3),
                thumbnailBytes = byteArrayOf(4, 5),
                mimeType = "video/mp4",
                fileName = "clip.mp4",
                fileSizeBytes = 3L,
            ),
        )

        assertEquals(MessageStatus.Sending, repository.created.single().status)
        assertEquals(DMMessageType.Video, repository.uploads.single().draft.type)
        assertEquals(MessageStatus.Sent.name, repository.updates.single().fields["status"])
        assertEquals(repository.uploadResult.attachmentUrl, repository.updates.single().fields["attachmentUrl"])
        assertEquals(repository.uploadResult.thumbnailUrl, repository.updates.single().fields["thumbnailUrl"])
    }

    @Test
    fun uploadFailureMarksMessageFailed() = runTest {
        val repository = FakeDMRepository(uploadFailure = IllegalStateException("storage failed"))
        val useCase = SendDMMessageUseCase(repository)

        assertFailsWith<IllegalStateException> {
            useCase(
                chatId = "dm1_5_alice_3_bob",
                senderId = "alice",
                draft = DMMessageDraft(
                    type = DMMessageType.Image,
                    mediaBytes = byteArrayOf(1),
                    mimeType = "image/jpeg",
                    fileName = "photo.jpg",
                    fileSizeBytes = 1L,
                ),
            )
        }

        assertEquals(MessageStatus.Failed.name, repository.updates.single().fields["status"])
    }

    @Test
    fun oversizedAudioIsRejectedBeforeWriting() = runTest {
        val repository = FakeDMRepository()
        val useCase = SendDMMessageUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            useCase(
                chatId = "dm1_5_alice_3_bob",
                senderId = "alice",
                draft = DMMessageDraft(
                    type = DMMessageType.Audio,
                    mediaBytes = byteArrayOf(1),
                    mimeType = "audio/mp4",
                    fileName = "audio.m4a",
                    fileSizeBytes = 26_214_401L,
                ),
            )
        }

        assertTrue(repository.created.isEmpty())
        assertTrue(repository.updates.isEmpty())
        assertTrue(repository.uploads.isEmpty())
    }

    @Test
    fun genericFileAttachmentIsRejectedBeforeWriting() = runTest {
        val repository = FakeDMRepository()
        val useCase = SendDMMessageUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            useCase(
                chatId = "dm1_5_alice_3_bob",
                senderId = "alice",
                draft = DMMessageDraft(
                    type = DMMessageType.File,
                    mediaBytes = byteArrayOf(1),
                    mimeType = "application/octet-stream",
                    fileName = "payload.bin",
                    fileSizeBytes = 1L,
                ),
            )
        }

        assertTrue(repository.created.isEmpty())
        assertTrue(repository.updates.isEmpty())
        assertTrue(repository.uploads.isEmpty())
    }

    @Test
    fun mismatchedAttachmentExtensionIsRejectedBeforeWriting() = runTest {
        val repository = FakeDMRepository()
        val useCase = SendDMMessageUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            useCase(
                chatId = "dm1_5_alice_3_bob",
                senderId = "alice",
                draft = DMMessageDraft(
                    type = DMMessageType.Image,
                    mediaBytes = byteArrayOf(1),
                    mimeType = "image/jpeg",
                    fileName = "payload.exe",
                    fileSizeBytes = 1L,
                ),
            )
        }

        assertTrue(repository.created.isEmpty())
        assertTrue(repository.updates.isEmpty())
        assertTrue(repository.uploads.isEmpty())
    }

    @Test
    fun sqlPayloadTextIsStoredAsTextButControlCharactersAreRejected() = runTest {
        val repository = FakeDMRepository()
        val useCase = SendDMMessageUseCase(repository)

        useCase(
            chatId = "dm1_5_alice_3_bob",
            senderId = "alice",
            draft = DMMessageDraft(type = DMMessageType.Text, text = "' OR '1'='1"),
        )

        assertEquals("' OR '1'='1", repository.created.single().draft.text)

        assertFailsWith<IllegalArgumentException> {
            useCase(
                chatId = "dm1_5_alice_3_bob",
                senderId = "alice",
                draft = DMMessageDraft(type = DMMessageType.Text, text = "hola\u0000mundo"),
            )
        }
    }

    @Test
    fun unsafeChatIdIsRejectedBeforeWriting() = runTest {
        val repository = FakeDMRepository()
        val useCase = SendDMMessageUseCase(repository)

        assertFailsWith<IllegalArgumentException> {
            useCase(
                chatId = "../../dm1_5_alice_3_bob",
                senderId = "alice",
                draft = DMMessageDraft(type = DMMessageType.Text, text = "hola"),
            )
        }

        assertTrue(repository.created.isEmpty())
    }

    private class FakeDMRepository(
        val uploadResult: MediaUploadResult = MediaUploadResult("dm_media/chat/message.jpg"),
        private val uploadFailure: Throwable? = null,
    ) : DMRepository {
        val created = mutableListOf<CreatedMessage>()
        val uploads = mutableListOf<UploadedMessage>()
        val updates = mutableListOf<UpdatedMessage>()

        override fun getUserChats(userId: String): Flow<List<DMChat>> = emptyFlow()

        override suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat =
            error("Not used")

        override fun getChatMessages(chatId: String): Flow<List<DMMessage>> = emptyFlow()

        override suspend fun getUsersByIds(
            currentUserId: String,
            userIds: Collection<String>,
        ): Map<String, DMUser> = emptyMap()

        override suspend fun createMessageRecord(
            chatId: String,
            messageId: String,
            senderId: String,
            draft: DMMessageDraft,
            status: MessageStatus,
        ) {
            created += CreatedMessage(chatId, messageId, senderId, draft, status)
        }

        override suspend fun uploadMessageMedia(
            chatId: String,
            messageId: String,
            draft: DMMessageDraft,
        ): MediaUploadResult {
            uploadFailure?.let { throw it }
            uploads += UploadedMessage(chatId, messageId, draft)
            return uploadResult
        }

        override suspend fun updateMessage(chatId: String, messageId: String, updates: Map<String, Any?>) {
            this.updates += UpdatedMessage(chatId, messageId, updates)
        }

        override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> = emptyList()
    }

    private data class CreatedMessage(
        val chatId: String,
        val messageId: String,
        val senderId: String,
        val draft: DMMessageDraft,
        val status: MessageStatus,
    )

    private data class UploadedMessage(
        val chatId: String,
        val messageId: String,
        val draft: DMMessageDraft,
    )

    private data class UpdatedMessage(
        val chatId: String,
        val messageId: String,
        val fields: Map<String, Any?>,
    )
}
