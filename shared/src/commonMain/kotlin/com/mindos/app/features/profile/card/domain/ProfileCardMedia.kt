package com.mindos.app.features.profile.card.domain

/**
 * A reference to a media binary stored in Firebase Storage. This is METADATA only —
 * the bytes live in Storage (`profile_cards_{private|public}/{uid}/media/{id}.{ext}`),
 * never in Firestore. Pure Kotlin so it round-trips through serialization and tests.
 */
data class ProfileCardMedia(
    val id: String,
    val kind: ProfileCardMediaKind,
    val storagePath: String,
    val downloadUrl: String? = null,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null,
    val createdAtMillis: Long = 0L,
) {
    val isVideo: Boolean get() = kind == ProfileCardMediaKind.VIDEO
}

enum class ProfileCardMediaKind {
    IMAGE,
    GIF,
    VIDEO,
    ;

    companion object {
        /** Classifies a MIME type into a card media kind (defaults to IMAGE). */
        fun fromMimeType(mimeType: String?): ProfileCardMediaKind = when {
            mimeType == null -> IMAGE
            mimeType == "image/gif" -> GIF
            mimeType.startsWith("video/") -> VIDEO
            else -> IMAGE
        }
    }
}

/** Which Storage surface a media binary belongs to. */
enum class ProfileCardMediaScope { PRIVATE, PUBLIC }

/**
 * A canvas item prepared for durable persistence. The model is complete in Ola 1·B;
 * the full canvas upload/restore pipeline is wired in Ola 2. [type] is extensible so
 * "add image" is never a dead-end special case.
 */
data class ProfileCanvasItem(
    val id: Long,
    val type: ProfileCanvasItemType,
    val media: ProfileCardMedia? = null,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val z: Int,
    val locked: Boolean = false,
)

enum class ProfileCanvasItemType { MEDIA, STICKER, TEXT, BADGE, STAT }

/**
 * The full structured profile-card document. Firestore persists this as a single
 * JSON payload string (structured metadata, NO bytes). The same shape is written to
 * the private draft and the public snapshot (auto-publish-on-save in v1).
 */
data class ProfileCardDoc(
    val appearance: ProfileCardAppearance,
    val backgroundMedia: ProfileCardMedia? = null,
    val canvas: List<ProfileCanvasItem> = emptyList(),
)
