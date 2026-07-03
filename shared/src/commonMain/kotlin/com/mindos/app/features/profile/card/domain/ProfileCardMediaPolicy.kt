package com.mindos.app.features.profile.card.domain

/**
 * Production limits for profile-card media, enforced CLIENT-SIDE before upload as
 * defense-in-depth. These mirror `storage.rules` (image/gif ≤ 10 MB), so a bad file
 * fails fast with a clear message instead of wasting an upload that the rules reject.
 * Pure Kotlin so it is unit-testable and shared across platforms.
 */
object ProfileCardMediaPolicy {
    /** 10 MiB — must stay ≤ the limit in `storage.rules` (profile card media create). */
    const val MAX_BYTES: Long = 10L * 1024 * 1024

    /** Content types accepted for v1 (photos + GIF). Mirrors the Storage rule allowlist. */
    val ALLOWED_MIME_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp", "image/gif")

    /** Returns null if the media is acceptable, otherwise a user-facing rejection reason. */
    fun rejectionReason(mimeType: String, sizeBytes: Long): String? = when {
        mimeType !in ALLOWED_MIME_TYPES -> "Formato no permitido. Usa una imagen o GIF (JPG, PNG, WEBP, GIF)."
        sizeBytes <= 0L -> "El archivo está vacío."
        sizeBytes > MAX_BYTES -> "El archivo supera el límite de 10 MB."
        else -> null
    }

    fun isAcceptable(mimeType: String, sizeBytes: Long): Boolean = rejectionReason(mimeType, sizeBytes) == null
}
