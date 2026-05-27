package com.example.mindos.features.social.presentation.publisher

import com.example.mindos.features.social.domain.publishing.DraftMediaAttachment
import com.example.mindos.features.social.domain.publishing.IdempotencyKey
import com.example.mindos.features.social.domain.publishing.PostId
import com.example.mindos.features.social.domain.publishing.PostMediaAttachment
import com.example.mindos.features.social.domain.publishing.PostVisibility
import com.example.mindos.features.social.domain.publishing.PublisherState
import com.example.mindos.features.social.domain.publishing.TraceId
import com.example.mindos.features.social.domain.valueobject.RequestId
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId

data class PublisherDraftSnapshot(
    val actorId: UserId,
    val draftId: String,
    val body: String,
    val visibility: PostVisibility,
    val idempotencyKey: IdempotencyKey,
    val traceId: TraceId,
    val requestId: RequestId,
    val state: PublisherState,
    val postId: PostId? = null,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val acceptedAt: Timestamp? = null,
    val pendingMedia: List<DraftMediaAttachment> = emptyList(),
    val uploadedMedia: List<PostMediaAttachment> = emptyList(),
    val lastError: String? = null,
    val attemptCount: Int = 0,
)

interface PublisherLocalDraftStore {
    suspend fun load(actorId: UserId): PublisherDraftSnapshot?
    suspend fun save(snapshot: PublisherDraftSnapshot)
    suspend fun clear(actorId: UserId)
}

class InMemoryPublisherLocalDraftStore : PublisherLocalDraftStore {
    private val drafts = mutableMapOf<UserId, PublisherDraftSnapshot>()

    override suspend fun load(actorId: UserId): PublisherDraftSnapshot? =
        drafts[actorId]

    override suspend fun save(snapshot: PublisherDraftSnapshot) {
        drafts[snapshot.actorId] = snapshot
    }

    override suspend fun clear(actorId: UserId) {
        drafts.remove(actorId)
    }
}

fun PublisherDraftSnapshot.toPersistedString(): String {
    val values = linkedMapOf(
        "schemaVersion" to PublisherDraftPersistence.CurrentSchemaVersion.toString(),
        "actorId" to actorId.value,
        "body" to body,
        "visibility" to visibility.name,
        "idempotencyKey" to idempotencyKey.value,
        "createdAt" to createdAt.epochMillis.toString(),
        "updatedAt" to updatedAt.epochMillis.toString(),
        "pendingMediaCount" to pendingMedia.size.toString(),
    )
    pendingMedia.forEachIndexed { index, media ->
        values["pendingMedia.$index.clientMediaId"] = media.clientMediaId
        values["pendingMedia.$index.localUri"] = media.localUri
        values["pendingMedia.$index.mimeType"] = media.mimeType
        values["pendingMedia.$index.sizeBytes"] = media.sizeBytes.toString()
    }
    return values.entries.joinToString(separator = "\n") { (key, value) ->
        "${key.escapePersistedValue()}=${value.escapePersistedValue()}"
    }
}

fun publisherDraftSnapshotFromPersistedString(raw: String): PublisherDraftSnapshot? {
    val values = parsePersistedValues(raw)
    val schemaVersion = values["schemaVersion"]?.toIntOrNull()
    return if (schemaVersion == null || schemaVersion < PublisherDraftPersistence.CurrentSchemaVersion) {
        publisherDraftSnapshotFromLegacyPersistedString(raw)
    } else {
        publisherDraftSnapshotFromCurrentPersistedValues(values)
    }
}

fun publisherDraftSnapshotFromLegacyPersistedString(raw: String): PublisherDraftSnapshot? {
    val values = parsePersistedValues(raw)
    val actorId = values["actorId"]?.takeIf { it.isNotBlank() }?.let(::UserId) ?: return null
    val draftId = values["draftId"]?.takeIf { it.isNotBlank() } ?: return null
    val visibility = values["visibility"]?.let { name ->
        PostVisibility.entries.firstOrNull { it.name == name }
    } ?: PostVisibility.Public
    val state = values["state"]?.let { name ->
        PublisherState.entries.firstOrNull { it.name == name }
    } ?: PublisherState.EditingDraft

    return PublisherDraftSnapshot(
        actorId = actorId,
        draftId = draftId,
        body = values["body"].orEmpty(),
        visibility = visibility,
        idempotencyKey = IdempotencyKey(values["idempotencyKey"]?.takeIf { it.isNotBlank() } ?: return null),
        traceId = TraceId(values["traceId"]?.takeIf { it.isNotBlank() } ?: return null),
        requestId = RequestId(values["requestId"]?.takeIf { it.isNotBlank() } ?: return null),
        state = state,
        postId = values["postId"]?.takeIf { it.isNotBlank() }?.let(::PostId),
        createdAt = Timestamp(values["createdAt"]?.toLongOrNull() ?: return null),
        updatedAt = Timestamp(values["updatedAt"]?.toLongOrNull() ?: return null),
        acceptedAt = values["acceptedAt"]?.toLongOrNull()?.let(::Timestamp),
        pendingMedia = decodePendingMedia(values),
        uploadedMedia = decodeUploadedMedia(values),
        lastError = values["lastError"]?.takeIf { it.isNotBlank() },
        attemptCount = values["attemptCount"]?.toIntOrNull() ?: 0,
    )
}

private fun publisherDraftSnapshotFromCurrentPersistedValues(values: Map<String, String>): PublisherDraftSnapshot? {
    val actorId = values["actorId"]?.takeIf { it.isNotBlank() }?.let(::UserId) ?: return null
    val visibility = values["visibility"]?.let { name ->
        PostVisibility.entries.firstOrNull { it.name == name }
    } ?: PostVisibility.Public
    val idempotencyKey = IdempotencyKey(values["idempotencyKey"]?.takeIf { it.isNotBlank() } ?: return null)
    val createdAt = Timestamp(values["createdAt"]?.toLongOrNull() ?: return null)
    val updatedAt = Timestamp(values["updatedAt"]?.toLongOrNull() ?: return null)
    val syntheticTraceId = TraceId("draft-${idempotencyKey.value}")
    val syntheticRequestId = RequestId("draft-${idempotencyKey.value}")

    return PublisherDraftSnapshot(
        actorId = actorId,
        draftId = "draft_${idempotencyKey.value}",
        body = values["body"].orEmpty(),
        visibility = visibility,
        idempotencyKey = idempotencyKey,
        traceId = syntheticTraceId,
        requestId = syntheticRequestId,
        state = PublisherState.EditingDraft,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pendingMedia = decodePendingMedia(values),
    )
}

private fun parsePersistedValues(raw: String): Map<String, String> =
    raw.lineSequence()
        .mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            line.substring(0, separator).unescapePersistedValue() to
                line.substring(separator + 1).unescapePersistedValue()
        }
        .toMap()

object PublisherDraftPersistence {
    const val CurrentSchemaVersion: Int = 2
}

private fun decodePendingMedia(values: Map<String, String>): List<DraftMediaAttachment> {
    val count = values["pendingMediaCount"]?.toIntOrNull() ?: 0
    return (0 until count).mapNotNull { index ->
        DraftMediaAttachment(
            clientMediaId = values["pendingMedia.$index.clientMediaId"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
            localUri = values["pendingMedia.$index.localUri"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
            mimeType = values["pendingMedia.$index.mimeType"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
            sizeBytes = values["pendingMedia.$index.sizeBytes"]?.toLongOrNull() ?: return@mapNotNull null,
        )
    }
}

private fun decodeUploadedMedia(values: Map<String, String>): List<PostMediaAttachment> {
    val count = values["uploadedMediaCount"]?.toIntOrNull() ?: 0
    return (0 until count).mapNotNull { index ->
        PostMediaAttachment(
            clientMediaId = values["uploadedMedia.$index.clientMediaId"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
            remoteUri = values["uploadedMedia.$index.remoteUri"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
            mimeType = values["uploadedMedia.$index.mimeType"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
            sizeBytes = values["uploadedMedia.$index.sizeBytes"]?.toLongOrNull() ?: return@mapNotNull null,
            uploadedAt = Timestamp(values["uploadedMedia.$index.uploadedAt"]?.toLongOrNull() ?: return@mapNotNull null),
        )
    }
}

private fun String.escapePersistedValue(): String =
    buildString(length) {
        this@escapePersistedValue.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '=' -> append("\\e")
                else -> append(char)
            }
        }
    }

private fun String.unescapePersistedValue(): String =
    buildString(length) {
        var escaping = false
        this@unescapePersistedValue.forEach { char ->
            if (escaping) {
                append(
                    when (char) {
                        'n' -> '\n'
                        'r' -> '\r'
                        'e' -> '='
                        else -> char
                    },
                )
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else {
                append(char)
            }
        }
        if (escaping) append('\\')
    }
