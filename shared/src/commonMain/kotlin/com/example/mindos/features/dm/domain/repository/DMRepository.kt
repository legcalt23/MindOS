package com.example.mindos.features.dm.domain.repository

import com.example.mindos.features.dm.domain.model.DMChat
import com.example.mindos.features.dm.domain.model.DMMessage
import com.example.mindos.features.dm.domain.model.DMMessageType
import com.example.mindos.features.dm.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface DMRepository {
    fun getUserChats(userId: String): Flow<List<DMChat>>
    suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat
    fun getChatMessages(chatId: String): Flow<List<DMMessage>>
    suspend fun getUsersByIds(currentUserId: String, userIds: Collection<String>): Map<String, DMUser>

    suspend fun createMessageRecord(
        chatId: String,
        messageId: String,
        senderId: String,
        draft: DMMessageDraft,
        status: MessageStatus,
    )

    suspend fun uploadMessageMedia(chatId: String, messageId: String, draft: DMMessageDraft): MediaUploadResult
    suspend fun updateMessage(chatId: String, messageId: String, updates: Map<String, Any?>)

    suspend fun getAvailableUsers(currentUserId: String): List<DMUser>
}

sealed interface DMRepositoryError {
    val code: String
    val userMessage: String

    data object NotAuthenticated : DMRepositoryError {
        override val code: String = "dm_not_authenticated"
        override val userMessage: String = "Inicia sesion de nuevo para abrir tus mensajes."
    }

    data class PermissionDenied(
        val operation: String,
        val path: String,
        val causeMessage: String? = null,
    ) : DMRepositoryError {
        override val code: String = "dm_permission_denied"
        override val userMessage: String
            get() = when {
                operation.contains("get_or_create") || path.startsWith("dm_chats") ->
                    "No tienes permisos para abrir este chat."
                else ->
                    "No tienes permisos para cargar el listado de usuarios de DM."
            }
    }

    data class DiscoveryMisconfigured(
        val operation: String,
        val path: String,
        val causeMessage: String? = null,
    ) : DMRepositoryError {
        override val code: String = "dm_discovery_misconfigured"
        override val userMessage: String
            get() = when {
                path.startsWith("dm_chats") ->
                    "La configuracion de Firestore para los chats DM no es valida."
                else ->
                    "El discovery de usuarios no esta configurado correctamente."
            }
    }

    data class MissingIndex(
        val operation: String,
        val path: String,
        val causeMessage: String? = null,
    ) : DMRepositoryError {
        override val code: String = "dm_firestore_index_missing"
        override val userMessage: String
            get() = when {
                path.startsWith("dm_chats") ->
                    "La configuracion de Firestore para los chats DM aun no esta lista."
                path.startsWith("profile_visuals") ->
                    "La configuracion de Firestore para descubrir usuarios DM aun no esta lista."
                else ->
                    "La configuracion de Firestore para DM aun no esta lista."
            }
    }

    data class Unexpected(
        val operation: String,
        val path: String? = null,
        val causeMessage: String? = null,
    ) : DMRepositoryError {
        override val code: String = "dm_unexpected_failure"
        override val userMessage: String
            get() = when {
                operation.contains("get_or_create") || path?.startsWith("dm_chats") == true ->
                    "No se pudo abrir el chat. Intentalo de nuevo."
                else ->
                    "No se pudo cargar el listado de usuarios. Intentalo de nuevo."
            }
    }
}

class DMRepositoryException(
    val error: DMRepositoryError,
    cause: Throwable? = null,
) : RuntimeException(error.userMessage, cause)

data class DMUser(
    val userId: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?
)

data class DMMessageDraft(
    val type: DMMessageType,
    val text: String? = null,
    val mediaBytes: ByteArray? = null,
    val thumbnailBytes: ByteArray? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val fileSizeBytes: Long? = null,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
)

data class MediaUploadResult(
    val attachmentUrl: String,
    val thumbnailUrl: String? = null
)
