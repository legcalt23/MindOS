package com.example.mindos.features.social.domain.feed

import com.example.mindos.core.security.SafeInput
import com.example.mindos.features.social.domain.error.InvalidValue
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.MediaContentResolver
import com.example.mindos.features.social.shared.util.MediaContentType

enum class GlobalFeedAttachmentType {
    Image,
    Video,
    Audio,
    File,
}

enum class GlobalFeedPostType {
    PHOTO,
    VIDEO,
    TEXT,
    AUDIO,
}

enum class GlobalFeedTextAlign {
    Start,
    Center,
    End,
}

enum class GlobalFeedTextBackgroundStyle {
    Solid,
    Gradient,
    DarkPremium,
    NeonGlass,
    MinimalLight,
    MinimalDark,
    NeuRebel,
    Executive,
}

data class GlobalFeedTextStyle(
    val fontSizeSp: Float = 28f,
    val fontWeight: Int = 700,
    val italic: Boolean = false,
    val textAlign: GlobalFeedTextAlign = GlobalFeedTextAlign.Start,
    val textColorArgb: Long = 0xFFF4FFF9,
    val lineHeightEm: Float = 1.18f,
    val letterSpacingEm: Float = 0f,
    val paddingDp: Float = 18f,
    val cornerRadiusDp: Float = 24f,
    val borderWidthDp: Float = 1f,
    val borderColorArgb: Long = 0x3328F0C6,
    val shadowAlpha: Float = 0.22f,
    val glowIntensity: Float = 0.45f,
    val backgroundStyle: GlobalFeedTextBackgroundStyle = GlobalFeedTextBackgroundStyle.NeuRebel,
    val backgroundPrimaryArgb: Long = 0xFF071311,
    val backgroundSecondaryArgb: Long = 0xFF102A24,
) {
    init {
        require(fontSizeSp > 0f) { "fontSizeSp must be positive." }
        require(fontWeight in 100..900) { "fontWeight must be between 100 and 900." }
        require(lineHeightEm >= 0.8f) { "lineHeightEm must be at least 0.8." }
        require(paddingDp >= 0f) { "paddingDp cannot be negative." }
        require(cornerRadiusDp >= 0f) { "cornerRadiusDp cannot be negative." }
        require(borderWidthDp >= 0f) { "borderWidthDp cannot be negative." }
        require(shadowAlpha in 0f..1f) { "shadowAlpha must be between 0 and 1." }
        require(glowIntensity in 0f..1.5f) { "glowIntensity must be between 0 and 1.5." }
    }
}

data class GlobalFeedDraftAttachment(
    val id: String,
    val type: GlobalFeedAttachmentType,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val bytes: ByteArray,
    val durationMs: Long? = null,
    val thumbnailBytes: ByteArray? = null,
) {
    init {
        require(SafeInput.isSafeDocumentId(id)) { "Attachment id is invalid." }
        require(fileName.isSafeFeedFileName()) { "Attachment fileName is invalid." }
        require(mimeType.isNotBlank()) { "Attachment mimeType cannot be blank." }
        require(sizeBytes >= 0) { "Attachment sizeBytes cannot be negative." }
        require(durationMs == null || durationMs >= 0L) { "Attachment durationMs cannot be negative." }
    }
}

data class GlobalFeedAttachment(
    val id: String,
    val type: GlobalFeedAttachmentType,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
) {
    val contentType: MediaContentType = MediaContentResolver.resolve(mimeType, url)

    init {
        require(SafeInput.isSafeDocumentId(id)) { "Attachment id is invalid." }
        require(fileName.isSafeFeedFileName()) { "Attachment fileName is invalid." }
        require(mimeType.isNotBlank()) { "Attachment mimeType cannot be blank." }
        require(sizeBytes >= 0) { "Attachment sizeBytes cannot be negative." }
        require(storagePath.isNotBlank()) { "Attachment storagePath cannot be blank." }
        require(url.isNotBlank()) { "Attachment url cannot be blank." }
        require(durationMs == null || durationMs >= 0L) { "Attachment durationMs cannot be negative." }
    }
}

data class GlobalFeedPost(
    val id: String,
    val userId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val postType: GlobalFeedPostType,
    val text: String?,
    val description: String? = null,
    val textStyle: GlobalFeedTextStyle? = null,
    val attachments: List<GlobalFeedAttachment>,
    val createdAtMillis: Long,
) {
    val hasText: Boolean
        get() = !text.isNullOrBlank()
}

data class CreateGlobalFeedPostRequest(
    val userId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val postType: GlobalFeedPostType,
    val text: String?,
    val description: String? = null,
    val textStyle: GlobalFeedTextStyle? = null,
    val attachments: List<GlobalFeedDraftAttachment>,
)

interface GlobalFeedRepository {
    suspend fun loadGlobalFeed(limit: Int = GlobalFeedPageSize): DomainResult<List<GlobalFeedPost>>
    suspend fun loadPostsByUser(userId: String, limit: Int = GlobalFeedPageSize): DomainResult<List<GlobalFeedPost>>
    suspend fun createPost(request: CreateGlobalFeedPostRequest): DomainResult<GlobalFeedPost>
}

class InMemoryGlobalFeedRepository : GlobalFeedRepository {
    private val posts = mutableListOf<GlobalFeedPost>()
    private var nextPost = 1L

    override suspend fun loadGlobalFeed(limit: Int): DomainResult<List<GlobalFeedPost>> =
        DomainResult.success(
            posts
                .sortedWith(compareByDescending<GlobalFeedPost> { it.createdAtMillis }.thenBy { it.id })
                .take(limit.coerceIn(1, GlobalFeedMaxPageSize)),
        )

    override suspend fun loadPostsByUser(userId: String, limit: Int): DomainResult<List<GlobalFeedPost>> =
        DomainResult.success(
            posts
                .asSequence()
                .filter { it.userId == userId }
                .sortedWith(compareByDescending<GlobalFeedPost> { it.createdAtMillis }.thenBy { it.id })
                .take(limit.coerceIn(1, GlobalFeedMaxPageSize))
                .toList(),
        )

    override suspend fun createPost(request: CreateGlobalFeedPostRequest): DomainResult<GlobalFeedPost> {
        validateGlobalFeedPostDraft(request)?.let { return DomainResult.failure(it) }
        val now = currentTimeMillis()
        val postId = "local_post_${nextPost++}"
        val post = GlobalFeedPost(
            id = postId,
            userId = request.userId,
            authorUsername = request.authorUsername,
            authorDisplayName = request.authorDisplayName,
            postType = request.postType,
            text = request.text.normalizedGlobalFeedText(),
            description = request.description.normalizedGlobalFeedDescription(),
            textStyle = request.textStyle,
            attachments = request.attachments.map { draft ->
                GlobalFeedAttachment(
                    id = draft.id,
                    type = draft.type,
                    fileName = draft.fileName,
                    mimeType = draft.mimeType,
                    sizeBytes = draft.sizeBytes,
                    storagePath = "local-feed/${request.userId}/$postId/${draft.id}",
                    url = "local-feed://${request.userId}/$postId/${draft.id}",
                    durationMs = draft.durationMs,
                )
            },
            createdAtMillis = now,
        )
        posts += post
        return DomainResult.success(post)
    }
}

fun validateGlobalFeedPostDraft(request: CreateGlobalFeedPostRequest): InvalidValue? {
    val text = request.text.normalizedGlobalFeedText().orEmpty()
    val descriptionLength = request.description.globalFeedCodePointLength()
    return when {
        !SafeInput.isSafeDocumentId(request.userId) -> InvalidValue("userId", "invalid id")
        !SafeInput.isSafeUsername(request.authorUsername) -> InvalidValue("authorUsername", "invalid username")
        !SafeInput.isSafeSingleLineHumanText(request.authorDisplayName, 80, minLength = 1) -> InvalidValue("authorDisplayName", "invalid display name")
        request.text != null && !SafeInput.isSafeMultilineHumanText(request.text, GlobalFeedMaxTextLength) -> InvalidValue("text", "text contains invalid characters")
        request.description != null && !SafeInput.isSafeMultilineHumanText(request.description, GlobalFeedMaxDescriptionLength) -> InvalidValue("description", "description contains invalid characters")
        text.length > GlobalFeedMaxTextLength -> InvalidValue("text", "text exceeds $GlobalFeedMaxTextLength characters")
        descriptionLength > GlobalFeedMaxDescriptionLength -> InvalidValue("description", "description exceeds $GlobalFeedMaxDescriptionLength characters")
        request.postType == GlobalFeedPostType.TEXT && text.isBlank() -> InvalidValue("text", "text content required")
        request.postType != GlobalFeedPostType.TEXT && text.isNotBlank() -> InvalidValue("text", "text only supported for text posts")
        request.postType == GlobalFeedPostType.PHOTO && request.attachments.count { it.type == GlobalFeedAttachmentType.Image } != 1 ->
            InvalidValue("attachments", "photo posts require exactly one image")
        request.postType == GlobalFeedPostType.PHOTO && request.attachments.any { it.type != GlobalFeedAttachmentType.Image } ->
            InvalidValue("attachments", "photo posts only allow images")
        request.postType == GlobalFeedPostType.VIDEO && request.attachments.count { it.type == GlobalFeedAttachmentType.Video } != 1 ->
            InvalidValue("attachments", "video posts require exactly one video")
        request.postType == GlobalFeedPostType.VIDEO && request.attachments.any { it.type != GlobalFeedAttachmentType.Video } ->
            InvalidValue("attachments", "video posts only allow videos")
        request.postType == GlobalFeedPostType.AUDIO && request.attachments.none { it.type == GlobalFeedAttachmentType.Audio } ->
            InvalidValue("attachments", "audio posts require at least one audio clip")
        request.postType == GlobalFeedPostType.AUDIO && request.attachments.any { it.type != GlobalFeedAttachmentType.Audio } ->
            InvalidValue("attachments", "audio posts only allow audio clips")
        request.postType == GlobalFeedPostType.AUDIO && request.attachments.size > GlobalFeedMaxAudioClips ->
            InvalidValue("attachments", "audio posts allow up to $GlobalFeedMaxAudioClips clips")
        request.postType == GlobalFeedPostType.TEXT && request.attachments.isNotEmpty() ->
            InvalidValue("attachments", "text posts do not allow attachments")
        request.postType != GlobalFeedPostType.TEXT && request.attachments.isEmpty() ->
            InvalidValue("attachments", "media content required")
        request.attachments.size > GlobalFeedMaxAttachments -> InvalidValue("attachments", "must contain $GlobalFeedMaxAttachments items or less")
        request.attachments.sumOf { it.sizeBytes } > GlobalFeedMaxAttachmentBytes -> InvalidValue("attachments", "selected files are too large")
        request.attachments.any { !SafeInput.isSafeDocumentId(it.id) } -> InvalidValue("attachments", "invalid attachment id")
        request.attachments.any { !it.fileName.isSafeFeedFileName() } -> InvalidValue("attachments", "invalid file name")
        request.attachments.any { it.bytes.isEmpty() || it.sizeBytes <= 0L } -> InvalidValue("attachments", "empty file")
        request.attachments.any { !it.hasAllowedMimeType() } -> InvalidValue("attachments", "unsupported file type")
        request.attachments.any { it.type == GlobalFeedAttachmentType.Audio && (it.durationMs ?: 0L) <= 0L } ->
            InvalidValue("attachments", "audio clip duration is invalid")
        request.attachments.any { it.bytes.size.toLong() > it.maxAllowedBytes() || it.sizeBytes > it.maxAllowedBytes() } ->
            InvalidValue("attachments", "file is too large")
        else -> null
    }
}

fun GlobalFeedDraftAttachment.normalizedMimeType(): String =
    mimeType.normalizedGlobalFeedMimeType()

fun String.normalizedGlobalFeedMimeType(): String =
    lowercase()
        .substringBefore(";")
        .trim()
        .let { normalized ->
            if (normalized in GlobalFeedWavMimeTypeAliases) "audio/wav" else normalized
        }

fun globalFeedAttachmentExtension(mimeType: String): String? =
    when (mimeType.normalizedGlobalFeedMimeType()) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "audio/mp4" -> "m4a"
        "audio/aac" -> "aac"
        "audio/mpeg" -> "mp3"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        "application/json" -> "json"
        "audio/wav" -> "wav"
        else -> null
    }

fun GlobalFeedDraftAttachment.hasAllowedMimeType(): Boolean {
    val normalized = normalizedMimeType()
    return when (type) {
        GlobalFeedAttachmentType.Image -> normalized in AllowedGlobalFeedImageMimeTypes
        GlobalFeedAttachmentType.Video -> normalized in AllowedGlobalFeedVideoMimeTypes
        GlobalFeedAttachmentType.Audio -> normalized in AllowedGlobalFeedAudioMimeTypes
        GlobalFeedAttachmentType.File -> normalized in AllowedGlobalFeedFileMimeTypes
    }
}

fun GlobalFeedDraftAttachment.maxAllowedBytes(): Long =
    when (normalizedMimeType()) {
        in AllowedGlobalFeedImageMimeTypes -> GlobalFeedMaxImageBytes
        in AllowedGlobalFeedVideoMimeTypes -> GlobalFeedMaxVideoBytes
        in AllowedGlobalFeedAudioMimeTypes -> GlobalFeedMaxAudioBytes
        "application/pdf" -> GlobalFeedMaxPdfBytes
        "text/plain", "application/json" -> GlobalFeedMaxTextFileBytes
        "audio/wav" -> GlobalFeedMaxAudioBytes
        else -> 0L
    }

private fun String.isSafeFeedFileName(): Boolean =
    isNotBlank() &&
        length <= 180 &&
        !contains("..") &&
        none { it == '/' || it == '\\' } &&
        !SafeInput.hasBlockedControlChars(this)

const val GlobalFeedPageSize: Int = 50
const val GlobalFeedMaxPageSize: Int = 100
const val GlobalFeedMaxTextLength: Int = 2_000
const val GlobalFeedMaxAttachments: Int = 6
const val GlobalFeedMaxAttachmentBytes: Long = 96L * 1024L * 1024L
const val GlobalFeedMaxImageBytes: Long = 10L * 1024L * 1024L
const val GlobalFeedMaxVideoBytes: Long = 75L * 1024L * 1024L
const val GlobalFeedMaxAudioBytes: Long = 75L * 1024L * 1024L
const val GlobalFeedMaxAudioClips: Int = 5
const val GlobalFeedMaxPdfBytes: Long = 32L * 1024L * 1024L
const val GlobalFeedMaxTextFileBytes: Long = 2L * 1024L * 1024L

val AllowedGlobalFeedImageMimeTypes: Set<String> =
    setOf("image/jpeg", "image/png", "image/webp", "image/gif")

val AllowedGlobalFeedVideoMimeTypes: Set<String> =
    setOf("video/mp4", "video/webm")

val AllowedGlobalFeedAudioMimeTypes: Set<String> =
    setOf("audio/mp4", "audio/aac", "audio/mpeg", "audio/wav")

val AllowedGlobalFeedFileMimeTypes: Set<String> =
    setOf("application/pdf", "text/plain", "application/json", "audio/wav")

private val GlobalFeedWavMimeTypeAliases: Set<String> =
    setOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave")

internal expect fun currentTimeMillis(): Long

const val GlobalFeedMaxDescriptionLength: Int = 1_000

fun String?.globalFeedCodePointLength(): Int {
    val value = this ?: return 0
    var count = 0
    var index = 0
    while (index < value.length) {
        val current = value[index]
        index += if (current.isHighSurrogate() && index + 1 < value.length && value[index + 1].isLowSurrogate()) 2 else 1
        count += 1
    }
    return count
}

fun String.limitGlobalFeedDescription(maxCodePoints: Int = GlobalFeedMaxDescriptionLength): String {
    if (maxCodePoints <= 0 || isEmpty()) return ""
    var index = 0
    var count = 0
    while (index < length && count < maxCodePoints) {
        val current = this[index]
        index += if (current.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()) 2 else 1
        count += 1
    }
    return if (index >= length) this else substring(0, index)
}

fun String?.normalizedGlobalFeedDescription(): String? =
    this
        ?.let { SafeInput.normalizeMultilineHumanText(it, GlobalFeedMaxDescriptionLength) }
        ?.takeIf { value -> value.isNotBlank() }

fun String?.normalizedGlobalFeedText(): String? =
    this
        ?.let { SafeInput.normalizeMultilineHumanText(it, GlobalFeedMaxTextLength) }
        ?.takeIf { value -> value.isNotBlank() }
