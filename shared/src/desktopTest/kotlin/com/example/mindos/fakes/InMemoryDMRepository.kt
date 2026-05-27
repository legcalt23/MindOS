package com.example.mindos.fakes

import com.example.mindos.features.dm.domain.model.DMChat
import com.example.mindos.features.dm.domain.model.DMMessage
import com.example.mindos.features.dm.domain.model.DMMessageType
import com.example.mindos.features.dm.domain.model.MessageStatus
import com.example.mindos.features.dm.domain.repository.DMMessageDraft
import com.example.mindos.features.dm.domain.repository.DMRepository
import com.example.mindos.features.dm.domain.repository.DMUser
import com.example.mindos.features.dm.domain.repository.MediaUploadResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Stateful in-memory DMRepository for desktop UI tests that need to drive DM
 * flows without Firebase. State is held in MutableStateFlow so observers
 * (getUserChats, getChatMessages) receive updates after createMessageRecord.
 *
 * Limitations (sufficient for smoke / interaction tests, NOT for behaviour
 * tests that mirror Firestore semantics):
 *   - participantKey is derived as participants.sorted().joinToString("_") instead of
 *     the canonical "dm1_<lenA>_<idA>_<lenB>_<idB>" used by FirebaseDMRepository.
 *   - uploadMessageMedia and updateMessage are no-ops — sufficient for tests that
 *     only exercise text messages.
 *   - No authorization checks: any caller can write any chat/message.
 *
 * Replace with a higher-fidelity fake when a test needs Firestore parity.
 */
internal class InMemoryDMRepository(
    private val knownUsers: Map<String, DMUser> = emptyMap(),
) : DMRepository {

    private val chats = MutableStateFlow<List<DMChat>>(emptyList())
    private val messagesByChatId = mutableMapOf<String, MutableStateFlow<List<DMMessage>>>()
    private var idCounter = 0L

    override fun getUserChats(userId: String): Flow<List<DMChat>> =
        chats.map { all -> all.filter { userId in it.participants } }

    override suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat {
        val participants = listOf(user1Id, user2Id).sorted()
        val key = participants.joinToString("_")
        chats.value.firstOrNull { it.participantKey == key }?.let { return it }
        val chat = DMChat(
            id = key,
            participants = participants,
            participantKey = key,
            lastMessage = null,
            lastMessageType = null,
            lastTimestamp = 0L,
            updatedAt = 0L,
            lastReadAtByUser = emptyMap(),
        )
        chats.value = chats.value + chat
        return chat
    }

    override fun getChatMessages(chatId: String): Flow<List<DMMessage>> =
        messagesFlow(chatId).asStateFlow()

    override suspend fun getUsersByIds(
        currentUserId: String,
        userIds: Collection<String>,
    ): Map<String, DMUser> =
        userIds
            .asSequence()
            .filter { it != currentUserId }
            .mapNotNull { id -> knownUsers[id]?.let { id to it } }
            .toMap()

    override suspend fun createMessageRecord(
        chatId: String,
        messageId: String,
        senderId: String,
        draft: DMMessageDraft,
        status: MessageStatus,
    ) {
        val now = ++idCounter
        val message = DMMessage(
            id = messageId,
            chatId = chatId,
            senderId = senderId,
            type = draft.type,
            status = status,
            text = draft.text,
            attachmentKind = null,
            attachmentUrl = null,
            attachmentMimeType = draft.mimeType,
            attachmentName = draft.fileName,
            attachmentSizeBytes = draft.fileSizeBytes,
            thumbnailUrl = null,
            durationMs = draft.durationMs,
            width = draft.width,
            height = draft.height,
            createdAt = now,
        )
        val flow = messagesFlow(chatId)
        flow.value = flow.value + message
        val preview = when (draft.type) {
            DMMessageType.Text -> draft.text
            DMMessageType.Image -> "Imagen"
            DMMessageType.Video -> "Video"
            DMMessageType.Audio -> "Audio"
            DMMessageType.File -> "Archivo"
        }
        chats.value = chats.value.map { chat ->
            if (chat.id == chatId) {
                chat.copy(
                    lastMessage = preview,
                    lastMessageType = draft.type,
                    lastTimestamp = now,
                    updatedAt = now,
                )
            } else {
                chat
            }
        }
    }

    override suspend fun uploadMessageMedia(
        chatId: String,
        messageId: String,
        draft: DMMessageDraft,
    ): MediaUploadResult = MediaUploadResult(attachmentUrl = "test://media/$chatId/$messageId")

    override suspend fun updateMessage(
        chatId: String,
        messageId: String,
        updates: Map<String, Any?>,
    ) {
        // Intentionally a no-op: smoke tests don't exercise message edits.
    }

    override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> =
        knownUsers.values.filter { it.userId != currentUserId }

    private fun messagesFlow(chatId: String): MutableStateFlow<List<DMMessage>> =
        messagesByChatId.getOrPut(chatId) { MutableStateFlow(emptyList()) }
}
