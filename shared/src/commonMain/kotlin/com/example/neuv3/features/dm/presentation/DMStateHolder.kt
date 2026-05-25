package com.example.neuv3.features.dm.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.neuv3.features.dm.domain.model.DMChat
import com.example.neuv3.features.dm.domain.model.DMMessage
import com.example.neuv3.features.dm.domain.model.DMMessageType
import com.example.neuv3.features.dm.domain.repository.DMMessageDraft
import com.example.neuv3.features.dm.domain.repository.DMRepository
import com.example.neuv3.features.dm.domain.repository.DMUser
import com.example.neuv3.features.dm.domain.usecase.GetDMConversationsUseCase
import com.example.neuv3.features.dm.domain.usecase.GetDMMessagesUseCase
import com.example.neuv3.features.dm.domain.usecase.GetOrCreateDMChatUseCase
import com.example.neuv3.features.dm.domain.usecase.SendDMMessageUseCase
import com.example.neuv3.features.waves.domain.service.WaveChatAccessResult
import com.example.neuv3.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DMStateHolder(
    private val repository: DMRepository,
    private val waveRequestService: WaveRequestService,
    private val scope: CoroutineScope,
) {
    private val getDMConversationsUseCase = GetDMConversationsUseCase(repository)
    private val getOrCreateDMChatUseCase = GetOrCreateDMChatUseCase(repository)
    private val getDMMessagesUseCase = GetDMMessagesUseCase(repository)
    private val sendDMMessageUseCase = SendDMMessageUseCase(repository)

    private val _chats = MutableStateFlow<List<DMChat>>(emptyList())
    val chats: StateFlow<List<DMChat>> = _chats.asStateFlow()

    private val _availableUsers = MutableStateFlow<List<DMUser>>(emptyList())
    val availableUsers: StateFlow<List<DMUser>> = _availableUsers.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<DMMessage>>(emptyList())
    val currentMessages: StateFlow<List<DMMessage>> = _currentMessages.asStateFlow()

    private val _resolvedChatId = MutableStateFlow<String?>(null)
    val resolvedChatId: StateFlow<String?> = _resolvedChatId.asStateFlow()

    private val _chatAccess = MutableStateFlow<WaveChatAccessResult?>(null)
    val chatAccess: StateFlow<WaveChatAccessResult?> = _chatAccess.asStateFlow()

    private var chatsJob: Job? = null
    private var availableUsersJob: Job? = null
    private var resolveJob: Job? = null
    private var messagesJob: Job? = null

    fun loadChats(userId: String) {
        chatsJob?.cancel()
        chatsJob = scope.launch {
            getDMConversationsUseCase(userId).collect {
                _chats.value = it
            }
        }
        loadAvailableUsers(userId)
    }

    fun loadAvailableUsers(currentUserId: String) {
        availableUsersJob?.cancel()
        availableUsersJob = scope.launch {
            try {
                val directoryUsers = repository.getAvailableUsers(currentUserId)
                _availableUsers.value = mergeUsers(directoryUsers, _availableUsers.value)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
            }
        }
    }

    suspend fun openOrCreateChat(user1Id: String, user2Id: String): String {
        when (waveRequestService.canStartWaveChat(user1Id, user2Id)) {
            WaveChatAccessResult.Allowed -> Unit
            is WaveChatAccessResult.Denied -> error("Wave chat is not authorized for users $user1Id and $user2Id")
        }
        val chat = getOrCreateDMChatUseCase(user1Id, user2Id)
        return chat.id
    }

    fun resolveAndLoadChat(myUserId: String, peerUserId: String?, inputChatId: String?) {
        resolveJob?.cancel()
        messagesJob?.cancel()
        loadAvailableUsers(myUserId)
        _resolvedChatId.value = null
        _currentMessages.value = emptyList()
        _chatAccess.value = null
        resolveJob = scope.launch {
            val normalizedPeerUserId = peerUserId
                ?.takeIf { it.isNotBlank() && it != "null" }
            normalizedPeerUserId?.let { resolveUsersByIds(myUserId, setOf(it)) }

            if (normalizedPeerUserId != null) {
                when (val access = waveRequestService.canStartWaveChat(myUserId, normalizedPeerUserId)) {
                    WaveChatAccessResult.Allowed -> _chatAccess.value = access
                    is WaveChatAccessResult.Denied -> {
                        _chatAccess.value = access
                        return@launch
                    }
                }
            }

            val finalChatId = when {
                inputChatId != null && inputChatId != "null" -> inputChatId
                normalizedPeerUserId != null -> openOrCreateChat(myUserId, normalizedPeerUserId)
                else -> null
            }
            if (finalChatId != null) {
                _resolvedChatId.value = finalChatId
                loadChatMessages(finalChatId)
            }
        }
    }

    fun loadChatMessages(chatId: String) {
        messagesJob?.cancel()
        messagesJob = scope.launch {
            getDMMessagesUseCase(chatId).collect {
                _currentMessages.value = it
            }
        }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        file: com.example.neuv3.core.media.PlatformFile,
        type: DMMessageType
    ) {
        sendMessage(
            chatId = chatId,
            senderId = senderId,
            type = type,
            mediaBytes = file.bytes,
            thumbnailBytes = file.thumbnailBytes,
            mimeType = file.mimeType,
            fileName = file.name,
            fileSizeBytes = file.size,
            durationMs = file.durationMs
        )
    }

    fun sendMessage(
        chatId: String, 
        senderId: String, 
        type: DMMessageType, 
        text: String? = null, 
        mediaBytes: ByteArray? = null, 
        thumbnailBytes: ByteArray? = null,
        mimeType: String? = null, 
        fileName: String? = null, 
        fileSizeBytes: Long? = null,
        durationMs: Long? = null,
        width: Int? = null,
        height: Int? = null,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        scope.launch {
            val result = runCatching {
                sendDMMessageUseCase(
                    chatId = chatId,
                    senderId = senderId,
                    draft = DMMessageDraft(
                        type = type,
                        text = text,
                        mediaBytes = mediaBytes,
                        thumbnailBytes = thumbnailBytes,
                        mimeType = mimeType,
                        fileName = fileName,
                        fileSizeBytes = fileSizeBytes,
                        durationMs = durationMs,
                        width = width,
                        height = height
                    )
                )
            }
            onComplete(result)
        }
    }

    private suspend fun resolveUsersByIds(
        currentUserId: String,
        userIds: Set<String>,
    ) {
        if (userIds.isEmpty()) return
        try {
            val resolvedUsers = repository.getUsersByIds(currentUserId, userIds)
            if (resolvedUsers.isNotEmpty()) {
                _availableUsers.value = mergeUsers(resolvedUsers.values, _availableUsers.value)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
        }
    }
}

@Composable
fun rememberDMStateHolder(
    repository: DMRepository,
    waveRequestService: WaveRequestService,
): DMStateHolder {
    val scope = rememberCoroutineScope()
    return remember(repository, waveRequestService, scope) {
        DMStateHolder(repository, waveRequestService, scope)
    }
}

private fun mergeUsers(
    primary: Iterable<DMUser>,
    secondary: Iterable<DMUser>,
): List<DMUser> {
    val merged = linkedMapOf<String, DMUser>()
    secondary.forEach { user -> merged[user.userId] = user }
    primary.forEach { user -> merged[user.userId] = user }
    return merged.values.toList()
}
