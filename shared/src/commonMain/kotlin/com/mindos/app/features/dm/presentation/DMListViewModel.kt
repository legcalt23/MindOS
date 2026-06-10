package com.mindos.app.features.dm.presentation

import com.mindos.app.core.logging.safeDebugLog
import com.mindos.app.features.dm.domain.model.DMChat
import com.mindos.app.features.dm.domain.repository.DMRepository
import com.mindos.app.features.dm.domain.repository.DMRepositoryException
import com.mindos.app.features.dm.domain.repository.DMUser
import com.mindos.app.features.waves.domain.model.WaveRelationshipStatus
import com.mindos.app.features.waves.domain.service.WaveChatAccessResult
import com.mindos.app.features.waves.domain.service.WaveRequestService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DMListStatus {
    data object Loading : DMListStatus
    data object Empty : DMListStatus
    data class Error(val message: String) : DMListStatus
    data object Ready : DMListStatus
}

sealed interface DMListNavigationTarget {
    data class Chat(val chatId: String, val otherUserId: String) : DMListNavigationTarget
}

data class DMListEntryUiModel(
    val key: String,
    val chatId: String?,
    val otherUserId: String,
    val title: String,
    val displayName: String?,
    val username: String?,
    val previewText: String,
    val avatarUrl: String?,
    val unreadCount: Int?,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val lastMessageIsIncomingAndUnread: Boolean = false,
    val lastTimestamp: Long,
    val sortTimestamp: Long,
)

data class DMListUiState(
    val status: DMListStatus = DMListStatus.Loading,
    val items: List<DMListEntryUiModel> = emptyList(),
    val navigationTarget: DMListNavigationTarget? = null,
    val blockedChatAttempt: BlockedWaveChatAttempt? = null,
)

data class BlockedWaveChatAttempt(
    val otherUserId: String,
    val relationshipStatus: WaveRelationshipStatus,
)

class DMListViewModel(
    private val repository: DMRepository,
    private val waveRequestService: WaveRequestService,
    private val currentUserId: String,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(DMListUiState())
    val uiState: StateFlow<DMListUiState> = _uiState.asStateFlow()

    private var chatsJob: Job? = null
    private var directoryJob: Job? = null
    private var participantProfilesJob: Job? = null
    private var cachedChats: List<DMChat> = emptyList()
    private var cachedDirectoryUsers: List<DMUser> = emptyList()
    private var cachedChatUsersById: Map<String, DMUser> = emptyMap()
    private var resolvingChatUserIds: Set<String> = emptySet()
    private var attemptedChatUserIds: Set<String> = emptySet()
    private var hasChatSnapshot = false
    private var hasDirectoryResult = false
    private var chatError: Throwable? = null
    private var directoryError: Throwable? = null

    init {
        loadUsers()
    }

    fun loadUsers() {
        dmVmLog("event=load_start")
        chatsJob?.cancel()
        directoryJob?.cancel()
        participantProfilesJob?.cancel()
        cachedChats = emptyList()
        cachedDirectoryUsers = emptyList()
        cachedChatUsersById = emptyMap()
        resolvingChatUserIds = emptySet()
        attemptedChatUserIds = emptySet()
        hasChatSnapshot = false
        hasDirectoryResult = false
        chatError = null
        directoryError = null
        _uiState.update { it.copy(status = DMListStatus.Loading, items = emptyList(), navigationTarget = null) }

        chatsJob = scope.launch {
            try {
                repository.getUserChats(currentUserId).collect { chats ->
                    cachedChats = chats
                    hasChatSnapshot = true
                    chatError = null
                    refreshChatParticipantProfiles(chats)
                    dmVmLog(
                        "event=chat_snapshot chat_count=${chats.size}"
                    )
                    publishState()
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                chatError = error
                hasChatSnapshot = true
                cachedChats = emptyList()
                refreshChatParticipantProfiles(emptyList())
                dmVmLog(
                    "event=chat_error exception=${error.dmExceptionLog()}"
                )
                publishState()
            }
        }

        directoryJob = scope.launch {
            try {
                val others = repository.getAvailableUsers(currentUserId)
                dmVmLog(
                    "event=directory_result users_count=${others.size}"
                )
                cachedDirectoryUsers = others
                directoryError = null
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                cachedDirectoryUsers = emptyList()
                directoryError = error
                dmVmLog(
                    "event=directory_error exception=${error.dmExceptionLog()}"
                )
            } finally {
                hasDirectoryResult = true
                publishState()
            }
        }
    }

    fun openChat(item: DMListEntryUiModel) {
        scope.launch {
            try {
                item.chatId?.let { chatId ->
                    _uiState.update {
                        it.copy(
                            navigationTarget = DMListNavigationTarget.Chat(chatId, item.otherUserId),
                            blockedChatAttempt = null,
                        )
                    }
                    return@launch
                }

                when (val access = waveRequestService.canStartWaveChat(currentUserId, item.otherUserId)) {
                    WaveChatAccessResult.Allowed -> Unit
                    is WaveChatAccessResult.Denied -> {
                        _uiState.update {
                            it.copy(
                                blockedChatAttempt = BlockedWaveChatAttempt(
                                    otherUserId = item.otherUserId,
                                    relationshipStatus = access.relationshipStatus,
                                ),
                            )
                        }
                        dmVmLog(
                            "event=open_chat_blocked relationship=${access.relationshipStatus::class.simpleName}"
                        )
                        return@launch
                    }
                }

                dmVmLog("event=open_chat_start")
                val chat = repository.getOrCreateChat(currentUserId, item.otherUserId)
                dmVmLog(
                    "event=open_chat_success"
                )
                _uiState.update {
                    it.copy(
                        navigationTarget = DMListNavigationTarget.Chat(chat.id, item.otherUserId),
                        blockedChatAttempt = null,
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                dmVmLog(
                    "event=open_chat_error exception=${error.dmExceptionLog()}"
                )
                _uiState.update {
                    it.copy(
                        status = DMListStatus.Error(error.toDMListMessage("No se pudo abrir el chat."))
                    )
                }
            }
        }
    }

    fun navigationHandled() {
        _uiState.update { it.copy(navigationTarget = null) }
    }

    private fun refreshChatParticipantProfiles(chats: List<DMChat>) {
        val otherUserIds = chats
            .asSequence()
            .mapNotNull { it.otherParticipantId(currentUserId) }
            .toSet()

        cachedChatUsersById = cachedChatUsersById.filterKeys { it in otherUserIds }
        attemptedChatUserIds = attemptedChatUserIds.intersect(otherUserIds)
        participantProfilesJob?.cancel()

        if (otherUserIds.isEmpty()) {
            resolvingChatUserIds = emptySet()
            return
        }

        val idsToResolve = otherUserIds - cachedChatUsersById.keys - attemptedChatUserIds
        resolvingChatUserIds = idsToResolve
        if (idsToResolve.isEmpty()) return

        participantProfilesJob = scope.launch {
            try {
                val resolved = repository.getUsersByIds(currentUserId, idsToResolve)
                if (resolved.isNotEmpty()) {
                    cachedChatUsersById = cachedChatUsersById + resolved
                }
                dmVmLog(
                    "event=chat_profiles_resolved requested_count=${idsToResolve.size} resolved_count=${resolved.size}"
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                dmVmLog(
                    "event=chat_profiles_error requested_count=${idsToResolve.size} exception=${error.dmExceptionLog()}"
                )
            } finally {
                attemptedChatUserIds = attemptedChatUserIds + idsToResolve
                resolvingChatUserIds = emptySet()
                publishState()
            }
        }
    }

    private fun publishState() {
        val items = buildItems(
            chats = cachedChats,
            directoryUsers = cachedDirectoryUsers,
            chatUsersById = cachedChatUsersById,
        )
        val currentChatError = chatError
        val currentDirectoryError = directoryError
        val nextStatus: DMListStatus = when {
            items.isNotEmpty() -> DMListStatus.Ready
            currentChatError != null -> DMListStatus.Error(
                currentChatError.toDMListMessage("No se pudo cargar la lista de chats.")
            )
            currentDirectoryError != null && hasChatSnapshot -> DMListStatus.Error(
                currentDirectoryError.toDMListMessage("No se pudo cargar el listado de usuarios.")
            )
            !hasChatSnapshot || !hasDirectoryResult -> DMListStatus.Loading
            else -> DMListStatus.Empty
        }

        dmVmLog(
            "event=emit_state status=${nextStatus::class.simpleName} items_count=${items.size}"
        )

        _uiState.update { current ->
            current.copy(
                status = nextStatus,
                items = items,
                navigationTarget = if (nextStatus is DMListStatus.Error) null else current.navigationTarget,
            )
        }
    }

    private fun buildItems(
        chats: List<DMChat>,
        directoryUsers: List<DMUser>,
        chatUsersById: Map<String, DMUser>,
    ): List<DMListEntryUiModel> {
        val directoryUsersById = directoryUsers.associateBy { it.userId }
        val usersById = directoryUsersById + chatUsersById

        val chatItems = chats.mapNotNull { chat ->
            val otherUserId = chat.otherParticipantId(currentUserId) ?: return@mapNotNull null
            val user = usersById[otherUserId]
            val username = user?.username?.takeIf { it.isNotBlank() }
            val title = when {
                username != null -> username
                otherUserId in resolvingChatUserIds -> LoadingUserTitle
                otherUserId in attemptedChatUserIds -> UnknownUserTitle
                else -> LoadingUserTitle
            }
            val unreadCount = chat.visualUnreadCount(currentUserId)
            DMListEntryUiModel(
                key = "chat:${chat.id}",
                chatId = chat.id,
                otherUserId = otherUserId,
                title = title,
                displayName = user?.displayName?.takeIf { it.isNotBlank() },
                username = username,
                previewText = chat.lastMessage
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: username?.let { "@$it" }
                    ?: "Pulsa para chatear",
                avatarUrl = user?.avatarUrl?.takeIf { it.isNotBlank() },
                unreadCount = unreadCount,
                isOnline = false,
                isTyping = false,
                lastMessageIsIncomingAndUnread = unreadCount != null && unreadCount > 0,
                lastTimestamp = chat.lastTimestamp,
                sortTimestamp = maxOf(chat.updatedAt, chat.lastTimestamp),
            )
        }.sortedByDescending { it.sortTimestamp }

        val usedUserIds = chatItems.mapTo(mutableSetOf()) { it.otherUserId }
        val directoryItems = directoryUsers
            .asSequence()
            .filterNot { it.userId in usedUserIds }
            .sortedBy { user ->
                user.username
                    ?.lowercase()
                    ?: user.displayName
                        ?.lowercase()
                    ?: UnknownUserTitle.lowercase()
            }
            .map { user ->
                val username = user.username?.takeIf { it.isNotBlank() }
                DMListEntryUiModel(
                    key = "user:${user.userId}",
                    chatId = null,
                    otherUserId = user.userId,
                    title = username ?: GenericUserTitle,
                    displayName = user.displayName?.takeIf { it.isNotBlank() },
                    username = username,
                    previewText = username?.let { "@$it" } ?: "Toca para abrir la conversacion",
                    avatarUrl = user.avatarUrl?.takeIf { it.isNotBlank() },
                    unreadCount = null,
                    isOnline = false,
                    isTyping = false,
                    lastMessageIsIncomingAndUnread = false,
                    lastTimestamp = 0L,
                    sortTimestamp = Long.MIN_VALUE,
                )
            }
            .toList()

        return chatItems + directoryItems
    }
}

private fun DMChat.otherParticipantId(currentUserId: String): String? =
    participants
        .filter { it.isNotBlank() }
        .distinct()
        .takeIf { it.size == 2 && currentUserId in it }
        ?.firstOrNull { it != currentUserId }

private fun DMChat.visualUnreadCount(currentUserId: String): Int? {
    val lastReadAt = lastReadAtByUser[currentUserId] ?: return null
    return if (lastTimestamp > lastReadAt) 1 else 0
}

private fun Throwable.toDMListMessage(fallback: String): String =
    when (this) {
        is DMRepositoryException -> error.userMessage
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }

private fun Throwable.dmExceptionLog(): String =
    this::class.simpleName.orEmpty()

private fun dmVmLog(message: String) {
    safeDebugLog("DMListVm", message)
}

private const val LoadingUserTitle = "Cargando..."
private const val UnknownUserTitle = "Usuario desconocido"
private const val GenericUserTitle = "Usuario"
