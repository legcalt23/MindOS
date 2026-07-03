package com.mindos.app.features.social.data.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mindos.app.core.logging.safeDebugLog
import com.mindos.app.core.logging.safeErrorLog
import com.mindos.app.core.network.StorageDefaults
import com.mindos.app.core.network.withFunctionsWriteTimeout
import com.mindos.app.core.network.withFirestoreReadTimeout
import com.mindos.app.core.network.withStorageReadTimeout
import com.mindos.app.core.network.withStorageUploadTimeout
import com.mindos.app.features.social.domain.error.PersistenceFailure
import com.mindos.app.features.social.domain.feed.CreateGlobalFeedPostRequest
import com.mindos.app.features.social.domain.feed.GlobalFeedAttachment
import com.mindos.app.features.social.domain.feed.GlobalFeedAttachmentType
import com.mindos.app.features.social.domain.feed.GlobalFeedDraftAttachment
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxPageSize
import com.mindos.app.features.social.domain.feed.GlobalFeedPost
import com.mindos.app.features.social.domain.feed.GlobalFeedPostType
import com.mindos.app.features.social.domain.feed.GlobalFeedRepository
import com.mindos.app.features.social.domain.feed.GlobalFeedTextAlign
import com.mindos.app.features.social.domain.feed.GlobalFeedTextBackgroundStyle
import com.mindos.app.features.social.domain.feed.GlobalFeedTextStyle
import com.mindos.app.features.social.domain.feed.globalFeedAttachmentExtension
import com.mindos.app.features.social.domain.feed.normalizedGlobalFeedDescription
import com.mindos.app.features.social.domain.feed.normalizedGlobalFeedMimeType
import com.mindos.app.features.social.domain.feed.normalizedGlobalFeedText
import com.mindos.app.features.social.domain.feed.validateGlobalFeedPostDraft
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.mindos.app.features.social.shared.util.DomainResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import dev.gitlive.firebase.Firebase as GitLiveFirebase
import dev.gitlive.firebase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@Composable
actual fun rememberGlobalFeedRepository(): GlobalFeedRepository =
    remember { FirebaseGlobalFeedRepository() }

class FirebaseGlobalFeedRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val storage: FirebaseStorage = Firebase.storage,
    private val auth: FirebaseAuth = Firebase.auth,
) : GlobalFeedRepository {

    override suspend fun loadGlobalFeed(limit: Int): DomainResult<List<GlobalFeedPost>> =
        firebaseFeedOperation("load global feed") {
            val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("No hay sesion activa para cargar el feed.")
            val snapshot = firestore
                .collection(FeedsCollection)
                .document(currentUid)
                .collection(FeedPostsCollection)
                .orderBy(CreatedAtField, Query.Direction.DESCENDING)
                .limit(limit.coerceIn(1, GlobalFeedMaxPageSize).toLong())
                .get()
                .let { query -> withFirestoreReadTimeout { query.await() } }

            snapshot.documents
                .mapNotNull { document -> document.data?.toMaterializedFeedPost(document.id) }
        }

    override suspend fun loadPostsByUser(userId: String, limit: Int): DomainResult<List<GlobalFeedPost>> =
        firebaseFeedOperation("load user feed") {
            val boundedLimit = limit.coerceIn(1, GlobalFeedMaxPageSize).toLong()
            val snapshot = firestore
                .collection(UserPostsCollection)
                .document(userId)
                .collection(UserPostsSubcollection)
                .orderBy(CreatedAtField, Query.Direction.DESCENDING)
                .limit(boundedLimit)
                .get()
                .let { query -> withFirestoreReadTimeout { query.await() } }

            snapshot.documents
                .mapNotNull { document -> document.data?.toGlobalFeedPost(document.id) }
                .take(boundedLimit.toInt())
        }

    override suspend fun createPost(request: CreateGlobalFeedPostRequest): DomainResult<GlobalFeedPost> =
        firebaseFeedOperation("create global post") {
            validateGlobalFeedPostDraft(request)?.let { validation ->
                return@firebaseFeedOperation throw IllegalArgumentException(validation.message)
            }
            val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("No hay sesion activa para publicar.")
            require(currentUid == request.userId) { "La sesion activa no coincide con el autor." }

            val now = System.currentTimeMillis()
            val postId = "post_${request.userId.safeStorageSegment(32)}_${now}_${randomSlug()}"
            val uploaded = mutableListOf<GlobalFeedAttachment>()

            try {
                request.attachments.forEach { draft ->
                    uploaded += uploadAttachment(
                        userId = request.userId,
                        postId = postId,
                        attachment = draft,
                    )
                }

                val payload = request.toCallablePayload(postId = postId, attachments = uploaded)
                safeDebugLog(
                    FeedPublishLogTag,
                    "event=feed_publish_prepare post_type=${request.postType.name} " +
                        "attachment_count=${uploaded.size} payload_key_count=${payload.keys.size}",
                )

                withFunctionsWriteTimeout {
                    GitLiveFirebase.functions.httpsCallable(CREATE_POST_CALLABLE).invoke(payload)
                }

                val canonicalSnapshot = firestore.collection(PostsCollection)
                    .document(postId)
                    .get()
                    .let { task -> withFirestoreReadTimeout { task.await() } }
                canonicalSnapshot.data?.toGlobalFeedPost(postId)
                    ?: error("El backend no devolvio el post canonico creado.")
            } catch (error: Throwable) {
                logPublishFailure(
                    request = request,
                    uploaded = uploaded,
                    error = error,
                )
                uploaded.forEach { attachment ->
                    runCatching {
                        withStorageReadTimeout(StorageDefaults.DeleteTimeout) {
                            storage.reference.child(attachment.storagePath).delete().await()
                        }
                    }
                }
                throw error
            }
        }

    private suspend fun uploadAttachment(
        userId: String,
        postId: String,
        attachment: GlobalFeedDraftAttachment,
    ): GlobalFeedAttachment {
        val normalizedMimeType = attachment.mimeType.normalizedGlobalFeedMimeType()
        val extension = globalFeedAttachmentExtension(normalizedMimeType)
            ?: throw IllegalArgumentException("Tipo de adjunto no permitido.")
        val storagePath = "$AttachmentsRoot/$userId/$postId/${attachment.id}.$extension"
        val reference = storage.reference.child(storagePath)
        reference
            .putBytes(
                attachment.bytes,
                StorageMetadata.Builder()
                    .setContentType(normalizedMimeType)
                    .setCustomMetadata("ownerUid", userId)
                    .setCustomMetadata("postId", postId)
                    .setCustomMetadata("visibility", "private")
                    .build(),
            )
            .let { uploadTask ->
                withStorageUploadTimeout(StorageDefaults.LargeUploadTimeout) { uploadTask.await() }
            }
        val thumbnailUrl = attachment.thumbnailBytes
            ?.takeIf { it.isNotEmpty() }
            ?.let { thumbnailBytes ->
                val thumbnailPath = "$AttachmentsRoot/$userId/$postId/${attachment.id}_thumb.jpg"
                val thumbnailReference = storage.reference.child(thumbnailPath)
                thumbnailReference
                    .putBytes(
                        thumbnailBytes,
                        StorageMetadata.Builder()
                            .setContentType("image/jpeg")
                            .setCustomMetadata("ownerUid", userId)
                            .setCustomMetadata("postId", postId)
                            .setCustomMetadata("visibility", "private")
                            .build(),
                    )
                    .let { uploadTask ->
                        withStorageUploadTimeout(StorageDefaults.SmallUploadTimeout) { uploadTask.await() }
                    }
                thumbnailPath
            }

        return GlobalFeedAttachment(
            id = attachment.id,
            type = attachment.type,
            fileName = attachment.fileName,
            mimeType = normalizedMimeType,
            sizeBytes = attachment.sizeBytes,
            storagePath = storagePath,
            url = storagePath,
            thumbnailUrl = thumbnailUrl,
            durationMs = attachment.durationMs,
        )
    }

}

private fun logPublishFailure(
    request: CreateGlobalFeedPostRequest,
    uploaded: List<GlobalFeedAttachment>,
    error: Throwable,
) {
    val firestoreCode = (error as? FirebaseFirestoreException)?.code?.name ?: "n/a"
    safeErrorLog(
        FeedPublishLogTag,
        "event=feed_publish_failure code=$firestoreCode post_type=${request.postType.name} " +
            "text_present=${!request.text.isNullOrBlank()} attachment_count=${request.attachments.size} " +
            "uploaded_count=${uploaded.size}",
        error,
    )
}

private suspend fun <T> firebaseFeedOperation(
    operation: String,
    block: suspend () -> T,
): DomainResult<T> =
    try {
        DomainResult.success(block())
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        DomainResult.failure(
            PersistenceFailure(
                "No se pudo completar $operation: ${error.message ?: error::class.simpleName.orEmpty()}",
            ),
        )
    }

private fun Map<String, Any>.toGlobalFeedPost(documentId: String): GlobalFeedPost? {
    val id = (this[IdField] as? String)?.takeIf { it.isNotBlank() } ?: documentId
    val userId = this[UserIdField] as? String ?: return null
    val username = this[AuthorUsernameField] as? String ?: userId
    val displayName = this[AuthorDisplayNameField] as? String ?: username
    val postType = (this[PostTypeField] as? String)
        ?.let { raw -> GlobalFeedPostType.entries.firstOrNull { it.name == raw } }
        ?: inferPostType(text = this[TextField] as? String, attachmentsField = this[AttachmentsField] as? List<*>)
    val text = (this[TextField] as? String)?.takeIf { it.isNotBlank() }
    val description = (this[DescriptionField] as? String)?.normalizedGlobalFeedDescription()
    val createdAt = this[CreatedAtField].asMillis() ?: return null
    val attachments = (this[AttachmentsField] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? Map<*, *>)?.toGlobalFeedAttachment() }
    val textStyle = (this[TextStyleField] as? Map<*, *>)?.toGlobalFeedTextStyle()
    return GlobalFeedPost(
        id = id,
        userId = userId,
        authorUsername = username,
        authorDisplayName = displayName,
        postType = postType,
        text = text,
        description = description,
        textStyle = textStyle,
        attachments = attachments,
        createdAtMillis = createdAt,
    )
}

private fun Map<String, Any>.toMaterializedFeedPost(documentId: String): GlobalFeedPost? {
    val postId = (this[PostIdField] as? String)?.takeIf { it.isNotBlank() } ?: documentId
    val nested = this[PostField] as? Map<*, *> ?: return null
    val post = buildMap<String, Any> {
        nested.forEach { (key, value) ->
            if (key is String && value != null) put(key, value)
        }
    }
    return post.toGlobalFeedPost(postId)
}

private fun Map<*, *>.toGlobalFeedAttachment(): GlobalFeedAttachment? {
    val id = this[AttachmentIdField] as? String ?: return null
    val type = (this[AttachmentTypeField] as? String)
        ?.let { raw -> GlobalFeedAttachmentType.entries.firstOrNull { it.name == raw } }
        ?: return null
    val fileName = this[AttachmentFileNameField] as? String ?: return null
    val mimeType = this[AttachmentMimeTypeField] as? String ?: return null
    val sizeBytes = this[AttachmentSizeBytesField].asLong() ?: return null
    val storagePath = this[AttachmentStoragePathField] as? String ?: return null
    val url = this[AttachmentUrlField] as? String ?: return null
    val thumbnailUrl = this[AttachmentThumbnailUrlField] as? String
    val durationMs = this[AttachmentDurationMsField].asLong()
    return GlobalFeedAttachment(
        id = id,
        type = type,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        storagePath = storagePath,
        url = url,
        thumbnailUrl = thumbnailUrl,
        durationMs = durationMs,
    )
}

private fun Map<*, *>.toGlobalFeedTextStyle(): GlobalFeedTextStyle? {
    val backgroundStyle = (this[TextBackgroundStyleField] as? String)
        ?.let { raw -> GlobalFeedTextBackgroundStyle.entries.firstOrNull { it.name == raw } }
        ?: return null
    val textAlign = (this[TextAlignField] as? String)
        ?.let { raw -> GlobalFeedTextAlign.entries.firstOrNull { it.name == raw } }
        ?: GlobalFeedTextAlign.Start
    return GlobalFeedTextStyle(
        fontSizeSp = this[TextFontSizeField].asFloat() ?: 28f,
        fontWeight = this[TextFontWeightField].asInt() ?: 700,
        italic = this[TextItalicField] as? Boolean ?: false,
        textAlign = textAlign,
        textColorArgb = this[TextColorField].asLong() ?: 0xFFF4FFF9,
        lineHeightEm = this[TextLineHeightField].asFloat() ?: 1.18f,
        letterSpacingEm = this[TextLetterSpacingField].asFloat() ?: 0f,
        paddingDp = this[TextPaddingField].asFloat() ?: 18f,
        cornerRadiusDp = this[TextCornerRadiusField].asFloat() ?: 24f,
        borderWidthDp = this[TextBorderWidthField].asFloat() ?: 1f,
        borderColorArgb = this[TextBorderColorField].asLong() ?: 0x3328F0C6,
        shadowAlpha = this[TextShadowAlphaField].asFloat() ?: 0.22f,
        glowIntensity = this[TextGlowField].asFloat() ?: 0.45f,
        backgroundStyle = backgroundStyle,
        backgroundPrimaryArgb = this[TextBackgroundPrimaryField].asLong() ?: 0xFF071311,
        backgroundSecondaryArgb = this[TextBackgroundSecondaryField].asLong() ?: 0xFF102A24,
    )
}

private fun CreateGlobalFeedPostRequest.toCallablePayload(
    postId: String,
    attachments: List<GlobalFeedAttachment>,
): Map<String, Any?> =
    linkedMapOf(
        "postId" to postId,
        PostTypeField to postType.name,
        TextField to text.normalizedGlobalFeedText(),
        DescriptionField to description.normalizedGlobalFeedDescription(),
        TextStyleField to textStyle?.toFirestoreMap(),
        AttachmentsField to attachments.map { it.toFirestoreMap() },
    )

private fun GlobalFeedAttachment.toFirestoreMap(): Map<String, Any> =
    linkedMapOf<String, Any>(
        AttachmentIdField to id,
        AttachmentTypeField to type.name,
        AttachmentFileNameField to fileName,
        AttachmentMimeTypeField to mimeType,
        AttachmentSizeBytesField to sizeBytes,
        AttachmentStoragePathField to storagePath,
        AttachmentUrlField to url,
    ).apply {
        thumbnailUrl?.let { put(AttachmentThumbnailUrlField, it) }
        durationMs?.let { put(AttachmentDurationMsField, it) }
    }

private fun GlobalFeedTextStyle.toFirestoreMap(): Map<String, Any?> =
    linkedMapOf(
        TextFontSizeField to fontSizeSp,
        TextFontWeightField to fontWeight,
        TextItalicField to italic,
        TextAlignField to textAlign.name,
        TextColorField to textColorArgb,
        TextLineHeightField to lineHeightEm,
        TextLetterSpacingField to letterSpacingEm,
        TextPaddingField to paddingDp,
        TextCornerRadiusField to cornerRadiusDp,
        TextBorderWidthField to borderWidthDp,
        TextBorderColorField to borderColorArgb,
        TextShadowAlphaField to shadowAlpha,
        TextGlowField to glowIntensity,
        TextBackgroundStyleField to backgroundStyle.name,
        TextBackgroundPrimaryField to backgroundPrimaryArgb,
        TextBackgroundSecondaryField to backgroundSecondaryArgb,
    )

private fun Any?.asMillis(): Long? =
    when (this) {
        is Long -> this
        is Int -> toLong()
        is Double -> toLong()
        is Timestamp -> toDate().time
        else -> null
    }

private fun Any?.asLong(): Long? =
    when (this) {
        is Long -> this
        is Int -> toLong()
        is Double -> toLong()
        else -> null
    }

private fun Any?.asFloat(): Float? =
    when (this) {
        is Float -> this
        is Double -> toFloat()
        is Long -> toFloat()
        is Int -> toFloat()
        else -> null
    }

private fun Any?.asInt(): Int? =
    when (this) {
        is Int -> this
        is Long -> toInt()
        is Double -> toInt()
        else -> null
    }

private fun inferPostType(text: String?, attachmentsField: List<*>?): GlobalFeedPostType {
    val attachmentType = attachmentsField
        ?.firstNotNullOfOrNull { (it as? Map<*, *>)?.get(AttachmentTypeField) as? String }
    return when {
        !text.isNullOrBlank() -> GlobalFeedPostType.TEXT
        attachmentType == GlobalFeedAttachmentType.Image.name -> GlobalFeedPostType.PHOTO
        attachmentType == GlobalFeedAttachmentType.Video.name -> GlobalFeedPostType.VIDEO
        attachmentType == GlobalFeedAttachmentType.Audio.name -> GlobalFeedPostType.AUDIO
        else -> GlobalFeedPostType.TEXT
    }
}

private fun String.safeStorageSegment(maxLength: Int): String =
    filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        .ifBlank { "neu" }
        .take(maxLength)

private fun randomSlug(): String =
    Random.nextLong().toULong().toString(36) + Random.nextLong().toULong().toString(36)

private const val PostsCollection = "posts"
private const val UserPostsCollection = "user_posts"
private const val UserPostsSubcollection = "posts"
private const val FeedsCollection = "feeds"
private const val FeedPostsCollection = "posts"
private const val PostIdField = "postId"
private const val PostField = "post"
private const val AttachmentsRoot = "post_attachments"
private const val IdField = "id"
private const val UserIdField = "userId"
private const val AuthorUsernameField = "authorUsername"
private const val AuthorDisplayNameField = "authorDisplayName"
private const val PostTypeField = "postType"
private const val TextField = "text"
private const val DescriptionField = "description"
private const val TextStyleField = "textStyle"
private const val AttachmentsField = "attachments"
private const val CreatedAtField = "createdAt"
private const val AttachmentIdField = "id"
private const val AttachmentTypeField = "type"
private const val AttachmentFileNameField = "fileName"
private const val AttachmentMimeTypeField = "mimeType"
private const val AttachmentSizeBytesField = "sizeBytes"
private const val AttachmentStoragePathField = "storagePath"
private const val AttachmentUrlField = "url"
private const val AttachmentThumbnailUrlField = "thumbnailUrl"
private const val AttachmentDurationMsField = "durationMs"
private const val TextFontSizeField = "fontSizeSp"
private const val TextFontWeightField = "fontWeight"
private const val TextItalicField = "italic"
private const val TextAlignField = "textAlign"
private const val TextColorField = "textColorArgb"
private const val TextLineHeightField = "lineHeightEm"
private const val TextLetterSpacingField = "letterSpacingEm"
private const val TextPaddingField = "paddingDp"
private const val TextCornerRadiusField = "cornerRadiusDp"
private const val TextBorderWidthField = "borderWidthDp"
private const val TextBorderColorField = "borderColorArgb"
private const val TextShadowAlphaField = "shadowAlpha"
private const val TextGlowField = "glowIntensity"
private const val TextBackgroundStyleField = "backgroundStyle"
private const val TextBackgroundPrimaryField = "backgroundPrimaryArgb"
private const val TextBackgroundSecondaryField = "backgroundSecondaryArgb"
private const val FeedPublishLogTag = "FirebaseGlobalFeedRepo"
private const val CREATE_POST_CALLABLE = "createPost"
