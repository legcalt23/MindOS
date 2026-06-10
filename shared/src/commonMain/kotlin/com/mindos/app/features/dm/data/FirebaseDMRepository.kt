package com.mindos.app.features.dm.data

import com.mindos.app.core.logging.safeDebugLog
import com.mindos.app.core.network.StorageDefaults
import com.mindos.app.core.network.withFirestoreReadTimeout
import com.mindos.app.core.network.withFirestoreWriteTimeout
import com.mindos.app.core.network.withStorageUploadTimeout
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.dm.data.mapper.toDomain
import com.mindos.app.features.dm.data.model.DMChatDto
import com.mindos.app.features.dm.data.model.DMMessageDto
import com.mindos.app.features.dm.domain.model.DMChat
import com.mindos.app.features.dm.domain.model.DMConversationIds
import com.mindos.app.features.dm.domain.model.DMMessage
import com.mindos.app.features.dm.domain.model.DMMessageType
import com.mindos.app.features.dm.domain.model.MessageStatus
import com.mindos.app.features.dm.domain.repository.DMMessageDraft
import com.mindos.app.features.dm.domain.repository.DMRepository
import com.mindos.app.features.dm.domain.repository.DMRepositoryError
import com.mindos.app.features.dm.domain.repository.DMRepositoryException
import com.mindos.app.features.dm.domain.repository.DMUser
import com.mindos.app.features.dm.domain.repository.MediaUploadResult
import com.mindos.app.features.social.data.mapper.ProfileVisualConfigDto
import com.mindos.app.features.social.data.repository.FirebaseProfileVisualRepository
import com.mindos.app.features.social.domain.model.ProfileVisualConfig
import com.mindos.app.features.social.domain.repository.ProfileVisualRepository
import com.mindos.app.features.social.domain.valueobject.UserId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.storageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class FirebaseDMRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val storage: FirebaseStorage = Firebase.storage,
    private val auth: FirebaseAuth = Firebase.auth,
    private val profileVisualRepository: ProfileVisualRepository =
        FirebaseProfileVisualRepository(firestore = firestore, storage = storage, auth = auth),
) : DMRepository {

    override fun getUserChats(userId: String): Flow<List<DMChat>> {
        dmLog(
            "event=dm_chats_query_start operation=firestore_list_dm_chats"
        )
        return firestore.collection(DMChatsCollection)
            .where { ParticipantsField contains userId }
            .orderBy(UpdatedAtField, Direction.DESCENDING)
            // REQUIRED by firestore.rules: the dm_chats `allow list` rule denies any
            // query whose request.query.limit is null or > 50. Without this .limit()
            // the realtime listener is rejected with PERMISSION_DENIED and the chat
            // list never loads. Match the rule cap exactly (most recent 50 chats).
            .limit(ChatsPageLimit)
            .snapshots
            .map { snapshot ->
                val byParticipantKey = linkedMapOf<String, DMChat>()
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.data<DMChatDto>().toDomain(doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.filter { chat ->
                    userId in chat.participants && chat.participants.distinct().size == 2
                }.map { chat ->
                    chat.normalizedForOneToOneDM()
                }.forEach { chat ->
                    val existing = byParticipantKey[chat.participantKey]
                    if (existing == null || chat.shouldReplace(existing)) {
                        byParticipantKey[chat.participantKey] = chat
                    }
                }
                byParticipantKey.values.sortedByDescending { it.updatedAt }.also { chats ->
                    dmLog(
                        "event=dm_chats_query_result success=true count=${chats.size}"
                    )
                    if (chats.isEmpty()) {
                        dmLog("event=dm_empty reason=no_dm_chats_found")
                    }
                }
            }
            .catch { error ->
                if (error is CancellationException) throw error
                dmLog(
                    "event=dm_chats_query_failure exception=${error.dmExceptionLog()}"
                )
                throw error.toDMRepositoryException(
                    operation = "firestore_list_dm_chats",
                    path = DMChatsCollection,
                )
            }
    }

    override suspend fun getOrCreateChat(user1Id: String, user2Id: String): DMChat {
        val operation = "firestore_get_or_create_dm_chat"
        val candidateChatId = DMConversationIds.chatIdOrNull(user1Id, user2Id) ?: "invalid_dm_chat_id"
        val path = "$DMChatsCollection/$candidateChatId"

        return try {
            val authenticatedUserId = requireAuthenticatedUser(user1Id, operation, path)
            val sortedUsers = DMConversationIds.participants(authenticatedUserId, user2Id)
            val deterministicChatId = DMConversationIds.chatId(authenticatedUserId, user2Id)
            val chatRef = firestore.collection(DMChatsCollection).document(deterministicChatId)
            val now = SystemEpochClock.nowMillis()
            var transactionOutcome = "existing"

            dmLog(
                "event=dm_chat_get_or_create_start operation=$operation"
            )

            val chat = withFirestoreWriteTimeout {
                firestore.runTransaction {
                    val snapshot = get(chatRef)
                    if (snapshot.exists) {
                        snapshot.data<DMChatDto>().toDomain(snapshot.id).normalizedForOneToOneDM()
                    } else {
                        transactionOutcome = "created"
                        val dto = DMChatDto(
                            participants = sortedUsers,
                            participantKey = deterministicChatId,
                            lastMessage = null,
                            lastMessageType = null,
                            lastTimestamp = now,
                            updatedAt = now,
                            lastReadAtByUser = emptyMap()
                        )
                        set(chatRef, dto)
                        dto.toDomain(chatRef.id)
                    }
                }
            }
            dmLog(
                "event=dm_chat_get_or_create_result operation=$operation result=$transactionOutcome"
            )
            chat
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            dmLog(
                "event=dm_chat_get_or_create_failure operation=$operation exception=${error.dmExceptionLog()}"
            )
            throw error.toDMRepositoryException(operation, path)
        }
    }

    override fun getChatMessages(chatId: String): Flow<List<DMMessage>> {
        dmLog(
            "event=dm_messages_query_start operation=firestore_list_dm_messages"
        )
        return firestore.collection(DMChatsCollection).document(chatId).collection(MessagesCollection)
            // REQUIRED by firestore.rules: the messages `allow list` rule denies any
            // query whose request.query.limit is null or > 100. We order DESCENDING +
            // limit to fetch the *latest* 100 messages (a null/unbounded query would be
            // rejected with PERMISSION_DENIED), then reverse so the UI still renders them
            // oldest→newest as before.
            .orderBy(CreatedAtField, Direction.DESCENDING)
            .limit(MessagesPageLimit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.data<DMMessageDto>().toDomain(doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.reversed()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                dmLog(
                    "event=dm_messages_query_failure exception=${error.dmExceptionLog()}"
                )
                throw error.toDMRepositoryException(
                    operation = "firestore_list_dm_messages",
                    path = "$DMChatsCollection/$chatId/$MessagesCollection",
                )
            }
    }

    override suspend fun getUsersByIds(
        currentUserId: String,
        userIds: Collection<String>,
    ): Map<String, DMUser> {
        val operation = "firestore_get_dm_profiles"
        val authenticatedUserId = requireAuthenticatedUser(
            currentUserId,
            operation = operation,
            path = ProfileVisualsCollection,
        )
        val requestedIds = userIds
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != authenticatedUserId }
            .distinct()
            .toList()

        if (requestedIds.isEmpty()) return emptyMap()

        dmLog(
            "event=dm_profiles_query_start operation=$operation requested_count=${requestedIds.size}"
        )

        val resolved = linkedMapOf<String, DMUser>()
        requestedIds.forEach { otherUserId ->
            val user = try {
                profileVisualRepository
                    .getConfig(UserId(otherUserId))
                    ?.toDMUser()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                dmLog(
                    "event=dm_profiles_query_failure operation=$operation exception=${error.dmExceptionLog()}"
                )
                null
            }

            if (user != null) {
                resolved[user.userId] = user
                dmLog(
                    "event=dm_profiles_query_result operation=$operation success=true"
                )
            } else {
                dmLog(
                    "event=dm_profiles_query_result operation=$operation success=false reason=profile_unavailable"
                )
            }
        }

        return resolved
    }

    override suspend fun createMessageRecord(
        chatId: String,
        messageId: String,
        senderId: String,
        draft: DMMessageDraft,
        status: MessageStatus
    ) {
        val chatRef = firestore.collection(DMChatsCollection).document(chatId)
        val newMsgRef = chatRef.collection(MessagesCollection).document(messageId)
        val now = SystemEpochClock.nowMillis()

        val dto = DMMessageDto(
            chatId = chatId,
            senderId = senderId,
            type = draft.type.name,
            status = status.name,
            text = draft.text,
            attachmentKind = draft.type.takeIf { it != DMMessageType.Text }?.name?.lowercase(),
            attachmentUrl = null,
            attachmentMimeType = draft.type.takeIf { it != DMMessageType.Text }?.let { uploadMimeType(draft) },
            attachmentName = draft.fileName,
            attachmentSizeBytes = draft.fileSizeBytes,
            thumbnailUrl = null,
            durationMs = draft.durationMs,
            width = draft.width,
            height = draft.height,
            createdAt = now
        )

        val lastMessagePreview = when (draft.type) {
            DMMessageType.Text -> draft.text
            DMMessageType.Image -> "Imagen"
            DMMessageType.Video -> "Video"
            DMMessageType.Audio -> "Audio"
            DMMessageType.File -> "Archivo"
        }

        withFirestoreWriteTimeout {
            firestore.runTransaction {
                val chatSnapshot = get(chatRef)
                require(chatSnapshot.exists) { "DM chat does not exist" }
                val chat = chatSnapshot.data<DMChatDto>()
                require(senderId in chat.participants) { "Sender is not a DM participant" }

                set(newMsgRef, dto)
                update(
                    chatRef,
                    mapOf(
                        LastMessageField to lastMessagePreview,
                        LastMessageTypeField to draft.type.name,
                        LastTimestampField to now,
                        UpdatedAtField to now,
                    )
                )
                null
            }
        }
    }

    override suspend fun uploadMessageMedia(chatId: String, messageId: String, draft: DMMessageDraft): MediaUploadResult {
        val mediaBytes = requireNotNull(draft.mediaBytes) { "Cannot upload empty media bytes" }
        require(mediaBytes.isNotEmpty()) { "Cannot upload empty media bytes" }

        val mimeTypeStr = uploadMimeType(draft)
        val extension = requireNotNull(extensionFromMimeType(mimeTypeStr)) {
            "Unsupported DM media MIME type"
        }
        val generatedFileName = "$messageId.$extension"
        val mediaPath = "dm_media/$chatId/$generatedFileName"
        val authenticatedUserId = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw DMRepositoryException(DMRepositoryError.NotAuthenticated)
        val participants = dmParticipantsFromChatIdOrNull(chatId)
            ?: throw DMRepositoryException(
                DMRepositoryError.PermissionDenied(
                    operation = "storage_upload_dm_media",
                    path = mediaPath,
                    causeMessage = "DM media uploads require a canonical participant-bound chat id.",
                ),
            )
        if (authenticatedUserId !in participants) {
            throw DMRepositoryException(
                DMRepositoryError.PermissionDenied(
                    operation = "storage_upload_dm_media",
                    path = mediaPath,
                    causeMessage = "Authenticated uid $authenticatedUserId is not a chat participant.",
                ),
            )
        }

        val mediaReference = storage.reference.child(mediaPath)

        // Video / large image uploads can legitimately take a long time on slow
        // mobile networks. Use LargeUploadTimeout for any media type that may
        // exceed a few MB; thumbnails fall back to the default short timeout.
        val mediaUploadTimeout = when (draft.type) {
            DMMessageType.Video, DMMessageType.File -> StorageDefaults.LargeUploadTimeout
            else -> StorageDefaults.SmallUploadTimeout
        }
        withStorageUploadTimeout(mediaUploadTimeout) {
            mediaReference.putData(
                dmStorageData(mediaBytes),
                dmMediaStorageMetadata(
                    contentType = mimeTypeStr,
                    chatId = chatId,
                    ownerUid = authenticatedUserId,
                    participants = participants,
                )
            )
        }

        var thumbnailUrl: String? = null
        if (draft.thumbnailBytes != null) {
            val thumbFileName = "thumb_$messageId.jpg"
            val thumbPath = "dm_media/$chatId/$thumbFileName"
            val thumbReference = storage.reference.child(thumbPath)
            withStorageUploadTimeout {
                thumbReference.putData(
                    dmStorageData(draft.thumbnailBytes),
                    dmMediaStorageMetadata(
                        contentType = "image/jpeg",
                        chatId = chatId,
                        ownerUid = authenticatedUserId,
                        participants = participants,
                    )
                )
            }
            thumbnailUrl = thumbPath
        }

        return MediaUploadResult(attachmentUrl = mediaPath, thumbnailUrl = thumbnailUrl)
    }

    override suspend fun updateMessage(chatId: String, messageId: String, updates: Map<String, Any?>) {
        val safeUpdates = updates.filterKeys { it in AllowedMessageUpdateFields }
        require(safeUpdates.size == updates.size) {
            "updateMessage received disallowed fields: ${(updates.keys - AllowedMessageUpdateFields)}"
        }
        withFirestoreWriteTimeout {
            firestore.collection(DMChatsCollection).document(chatId).collection(MessagesCollection).document(messageId)
                .updateFields {
                    safeUpdates.forEach { (field, value) ->
                        field to value
                    }
                }
        }
    }

    override suspend fun getAvailableUsers(currentUserId: String): List<DMUser> {
        val path = ProfileVisualsCollection
        val authenticatedUserId = requireAuthenticatedUser(
            currentUserId,
            operation = "firestore_list_dm_discovery",
            path = path,
        )
        dmLog("event=dm_discovery_start")

        val visualUsers = try {
            queryProfileVisualUsers(authenticatedUserId)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            dmLog(
                "event=dm_discovery_failure exception=${error.dmExceptionLog()}"
            )
            throw error.toDMRepositoryException(
                operation = "firestore_list_dm_profile_visuals",
                path = ProfileVisualsCollection,
            )
        }

        val candidates = visualUsers
        dmLog(
            "event=dm_discovery_filter_start candidate_count=${candidates.size}"
        )
        val filtered = candidates
            .filter { user ->
                val discard = user.userId == authenticatedUserId
                dmLog(
                    "event=dm_discovery_filter_candidate discarded=$discard reason=${if (discard) "current_user" else "kept"}"
                )
                !discard
            }
            .distinctBy { it.userId }

        dmLog(
            "event=dm_discovery_filter_result result_count=${filtered.size}"
        )
        if (filtered.isEmpty()) {
            dmLog(
                "event=dm_empty reason=no_visible_profiles_found"
            )
        }

        return filtered
    }

    private suspend fun queryProfileVisualUsers(currentUserId: String): List<DMUser> {
        dmLog(
            "event=dm_profile_visuals_query_start limit=$AvailableUsersLimit"
        )
        val documents = firestore.collection(ProfileVisualsCollection)
            .where { SearchableField equalTo true }
            .where { DirectoryVisibleField equalTo true }
            .orderBy(UsernameField)
            .limit(AvailableUsersLimit)
            .get()
            .documents
        val users = documents.mapNotNull { doc ->
            doc.toDMUserFromProfileVisual(ProfileVisualsCollection)
        }
        // Filter out the current user: showing the authenticated user inside their
        // own DM discovery list is a UX bug — they cannot DM themselves.
        val visible = users.filter { it.userId != currentUserId }
        dmLog(
            "event=dm_profile_visuals_query_result success=true count=${visible.size} " +
                "self_filtered=${users.size - visible.size}"
        )
        return visible
    }

    private fun DMChat.normalizedForOneToOneDM(): DMChat {
        val normalizedParticipants = participants.distinct().sorted()
        val canonicalKey = DMConversationIds.chatIdForParticipantsOrNull(normalizedParticipants)
        return copy(
            participants = normalizedParticipants,
            participantKey = canonicalKey ?: participantKey.takeIf { it.isNotBlank() } ?: id,
        )
    }

    private fun DMChat.shouldReplace(existing: DMChat): Boolean =
        compareDMChatCandidates(this, existing) > 0

    private fun compareDMChatCandidates(first: DMChat, second: DMChat): Int {
        val firstHasLastMessage = first.lastMessage != null
        val secondHasLastMessage = second.lastMessage != null
        val firstHasHistory = firstHasLastMessage || first.lastTimestamp > 0L
        val secondHasHistory = secondHasLastMessage || second.lastTimestamp > 0L
        return when {
            firstHasLastMessage != secondHasLastMessage -> if (firstHasLastMessage) 1 else -1
            firstHasHistory != secondHasHistory -> if (firstHasHistory) 1 else -1
            first.updatedAt != second.updatedAt -> first.updatedAt.compareTo(second.updatedAt)
            first.id == first.participantKey && second.id != second.participantKey -> 1
            first.id != first.participantKey && second.id == second.participantKey -> -1
            else -> first.id.compareTo(second.id)
        }
    }

    private fun uploadMimeType(draft: DMMessageDraft): String {
        require(draft.type != DMMessageType.File) { "DM file attachments are not supported" }

        val mimeType = draft.mimeType
            ?.lowercase()
            ?.substringBefore(";")
            ?.trim()
            ?.takeIf { extensionFromMimeType(it) != null }
            ?: draft.fileName
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.lowercase()
                ?.let(::mimeTypeFromExtension)
            ?: defaultMimeType(draft.type)

        val extension = draft.fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        require(extension == null || extension in allowedExtensionsFor(mimeType)) {
            "DM attachment extension does not match MIME type"
        }

        return mimeType
    }

    private fun allowedExtensionsFor(mimeType: String): Set<String> =
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

    private fun mimeTypeFromExtension(extension: String): String? =
        when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> null
        }

    private fun extensionFromMimeType(mimeType: String?): String? =
        when (mimeType?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "audio/mp4" -> "m4a"
            "audio/mpeg" -> "mp3"
            "audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave" -> "wav"
            else -> null
        }

    private fun defaultMimeType(type: DMMessageType): String =
        when (type) {
            DMMessageType.Text -> "text/plain"
            DMMessageType.Image -> "image/jpeg"
            DMMessageType.Video -> "video/mp4"
            DMMessageType.Audio -> "audio/mp4"
            DMMessageType.File -> error("DM file attachments are not supported")
        }

    private fun dmMediaStorageMetadata(
        contentType: String,
        chatId: String,
        ownerUid: String,
        participants: List<String>,
    ) = storageMetadata {
        this.contentType = contentType
        setCustomMetadata("ownerUid", ownerUid)
        setCustomMetadata("visibility", "private")
        setCustomMetadata("chatId", chatId)
        setCustomMetadata("participantA", participants[0])
        setCustomMetadata("participantB", participants[1])
    }

    private fun dmParticipantsFromChatIdOrNull(chatId: String): List<String>? {
        val match = CanonicalDMChatIdRegex.matchEntire(chatId) ?: return null
        val firstLength = match.groupValues[1].toIntOrNull() ?: return null
        val first = match.groupValues[2]
        val secondLength = match.groupValues[3].toIntOrNull() ?: return null
        val second = match.groupValues[4]
        if (first.length != firstLength || second.length != secondLength) return null
        return runCatching { DMConversationIds.participants(first, second) }.getOrNull()
    }

    private fun requireAuthenticatedUser(
        currentUserId: String,
        operation: String,
        path: String,
    ): String {
        val requestedUserId = currentUserId.takeIf { it.isNotBlank() }
            ?: throw DMRepositoryException(DMRepositoryError.NotAuthenticated)
        val authenticatedUserId = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw DMRepositoryException(DMRepositoryError.NotAuthenticated)
        if (authenticatedUserId != requestedUserId) {
            throw DMRepositoryException(
                DMRepositoryError.PermissionDenied(
                    operation = operation,
                    path = path,
                    causeMessage = "Authenticated uid $authenticatedUserId does not match route uid $requestedUserId.",
                ),
            )
        }
        return authenticatedUserId
    }

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toDMUserFromProfileVisual(path: String): DMUser? {
        val data = decodeProfileVisualDiscoveryDto(path) ?: return null
        val userId = resolveUserId(path, id, data.userId) ?: return null
        if (!data.searchable) {
            dmLog(
                "event=dm_map_discarded reason=non_searchable"
            )
            return null
        }
        val username = resolvePublicUsername(data.username, userId)
        val displayName = resolvePublicDisplayName(data.displayName, userId)
        dmLog(
            "event=dm_map_success searchable=${data.searchable} has_public_profile_fields=${username != null || displayName != null}"
        )
        return DMUser(
            userId = userId,
            username = username,
            displayName = displayName,
            avatarUrl = data.mainProfilePhotoUrl?.takeIf { it.isNotBlank() },
        )
    }

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.decodeProfileVisualDiscoveryDto(
        path: String,
    ): ProfileVisualConfigDto? =
        try {
            data<ProfileVisualConfigDto>()
        } catch (error: Throwable) {
            dmLog(
                "event=dm_map_discarded reason=data_decode_failed path=$path " +
                    "exception=${error.dmExceptionLog()}"
            )
            null
        }

    private fun resolveUserId(
        path: String,
        documentId: String,
        vararg candidates: String?,
    ): String? {
        val userId = candidates.firstOrNull { !it.isNullOrBlank() } ?: documentId.takeIf { it.isNotBlank() }
        if (userId == null) {
            dmLog("event=dm_map_discarded reason=missing_user_id path=$path")
            return null
        }
        if (userId != documentId) {
            dmLog(
                "event=dm_map_discarded reason=document_id_user_id_mismatch path=$path"
            )
            return null
        }
        return userId
    }

    private fun Throwable.toDMRepositoryException(
        operation: String,
        path: String,
    ): DMRepositoryException = when {
        this is DMRepositoryException -> this
        this is FirebaseFirestoreException -> toDMRepositoryException(operation, path)
        else -> {
            val diagnostic = "${this::class.simpleName.orEmpty()} ${message.orEmpty()}".lowercase()
            val error = when {
                "permission" in diagnostic || "insufficient" in diagnostic || "authorized" in diagnostic ->
                    DMRepositoryError.PermissionDenied(operation, path, message)
                "unauth" in diagnostic || "sesion" in diagnostic || "session" in diagnostic ->
                    DMRepositoryError.NotAuthenticated
                diagnostic.looksLikeMissingIndex() ->
                    DMRepositoryError.MissingIndex(operation, path, message)
                "failed_precondition" in diagnostic || "precondition" in diagnostic ->
                    DMRepositoryError.DiscoveryMisconfigured(operation, path, message)
                else -> DMRepositoryError.Unexpected(operation, path, message)
            }
            if (error is DMRepositoryError.MissingIndex) {
                dmLog(buildMissingIndexLog(operation, path))
            }
            DMRepositoryException(error, this)
        }
    }

    private fun FirebaseFirestoreException.toDMRepositoryException(
        operation: String,
        path: String,
    ): DMRepositoryException {
        val error = when (code) {
            FirestoreExceptionCode.UNAUTHENTICATED -> DMRepositoryError.NotAuthenticated
            FirestoreExceptionCode.PERMISSION_DENIED -> DMRepositoryError.PermissionDenied(operation, path, message)
            FirestoreExceptionCode.FAILED_PRECONDITION -> when {
                message.orEmpty().looksLikeMissingIndex() ->
                    DMRepositoryError.MissingIndex(operation, path, message)
                else ->
                    DMRepositoryError.DiscoveryMisconfigured(operation, path, message)
            }
            else -> DMRepositoryError.Unexpected(operation, path, message)
        }
        if (error is DMRepositoryError.MissingIndex) {
            dmLog(buildMissingIndexLog(operation, path))
        }
        return DMRepositoryException(error, this)
    }

    private fun ProfileVisualConfig.toDMUser(): DMUser {
        val resolvedUserId = userId.value
        val resolvedUsername = resolvePublicUsername(username, resolvedUserId)
        val resolvedDisplayName = resolvePublicDisplayName(displayName, resolvedUserId)
        return DMUser(
            userId = resolvedUserId,
            username = resolvedUsername,
            displayName = resolvedDisplayName,
            avatarUrl = mainProfilePhotoUrl?.takeIf { it.isNotBlank() },
        )
    }

    private fun resolvePublicUsername(
        value: String?,
        userId: String,
    ): String? {
        val normalized = value
            ?.trim()
            ?.removePrefix("@")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() && isSafeUsername(it) }
            ?: return null
        return normalized.takeUnless { it.isTechnicalProfileUsername(userId) }
    }

    private fun resolvePublicDisplayName(
        value: String?,
        userId: String,
    ): String? {
        val normalized = value
            ?.trim()
            ?.take(80)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return normalized.takeUnless { it.isTechnicalProfileDisplayName(userId) }
    }

    private fun String.toSafeUsernameCandidate(): String {
        val normalized = lowercase()
            .map { char -> if (char.isLetterOrDigit() || char == '.' || char == '_') char else '_' }
            .joinToString("")
            .trim('.', '_')
            .replace(Regex("\\.{2,}"), ".")
            .take(30)
        return when {
            isSafeUsername(normalized) -> normalized
            normalized.length in 1..2 -> "user_$normalized".take(30)
            else -> ""
        }
    }

    private fun isSafeUsername(value: String): Boolean =
        value.matches(UsernameRegex) &&
            !value.startsWith(".") &&
            !value.endsWith(".") &&
            !value.contains("..")

    private fun String.isTechnicalProfileUsername(userId: String): Boolean {
        val normalized = trim().removePrefix("@").lowercase()
        return normalized == GenericUsername ||
            normalized == GenericSpanishUsername ||
            normalized == userId.lowercase() ||
            normalized == userId.toSafeUsernameCandidate() ||
            normalized == generatedUsernameFromUserId(userId) ||
            normalized.startsWith(DMConversationIdPrefix)
    }

    private fun String.isTechnicalProfileDisplayName(userId: String): Boolean {
        val normalized = trim()
        if (normalized.isBlank()) return true
        val lowered = normalized.lowercase()
        return lowered == GenericDisplayName.lowercase() ||
            lowered == userId.lowercase() ||
            lowered == userId.toSafeUsernameCandidate() ||
            lowered == generatedUsernameFromUserId(userId) ||
            lowered.startsWith(DMConversationIdPrefix)
    }

    private fun generatedUsernameFromUserId(userId: String): String {
        val uidAlias = userId.lowercase().filter { it.isLetterOrDigit() }.take(24)
        return "user_${uidAlias.ifBlank { "neu" }}".take(30)
    }

    private fun String.looksLikeMissingIndex(): Boolean =
        "query requires an index" in this ||
            ("failed_precondition" in this && "index" in this) ||
            "requires an index" in this

    private fun buildMissingIndexLog(
        operation: String,
        path: String,
    ): String {
        val label = when {
            path.startsWith(DMChatsCollection) ->
                "Firestore index missing for DM query"
            path.startsWith(ProfileVisualsCollection) ->
                "Firestore index missing for profile_visuals discovery query"
            else ->
                "Firestore index missing for DM operation"
        }
        return "event=dm_index_missing operation=$operation label=\"$label\""
    }

    private fun Throwable.dmExceptionLog(): String =
        this::class.simpleName.orEmpty()

    private fun dmLog(message: String) {
        safeDebugLog("FirebaseDMRepository", message)
    }

    private companion object {
        const val DMChatsCollection = "dm_chats"
        const val MessagesCollection = "messages"
        const val ProfileVisualsCollection = "profile_visuals"
        const val AvailableUsersLimit = 12
        // Must stay <= the caps enforced in firestore.rules (dm_chats list <= 50,
        // messages list <= 100). Changing these requires updating the rules too.
        const val ChatsPageLimit = 50
        const val MessagesPageLimit = 100
        const val ParticipantsField = "participants"
        const val LastMessageField = "lastMessage"
        const val LastMessageTypeField = "lastMessageType"
        const val LastTimestampField = "lastTimestamp"
        const val UpdatedAtField = "updatedAt"
        const val CreatedAtField = "createdAt"
        const val SearchableField = "searchable"
        const val DirectoryVisibleField = "directoryVisible"
        const val UsernameField = "username"
        const val GenericDisplayName = "Perfil MindOS"
        const val GenericSpanishUsername = "usuario"
        const val GenericUsername = "user_neu"
        const val DMConversationIdPrefix = "dm1_"
        val UsernameRegex = Regex("^[a-z0-9._]{3,30}$")
        val CanonicalDMChatIdRegex = Regex("^dm1_(\\d+)_([A-Za-z0-9_-]+)_(\\d+)_([A-Za-z0-9_-]+)$")
        val AllowedMessageUpdateFields = setOf("status", "attachmentUrl", "thumbnailUrl")
    }
}
