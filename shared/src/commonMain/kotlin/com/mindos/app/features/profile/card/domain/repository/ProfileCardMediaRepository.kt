package com.mindos.app.features.profile.card.domain.repository

import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaScope

/**
 * Uploads/deletes profile-card media BINARIES to Firebase Storage. Firestore only
 * ever stores the returned [ProfileCardMedia] metadata (paths/urls), never bytes.
 */
interface ProfileCardMediaRepository {
    /**
     * Uploads [bytes] to the [scope] Storage surface for [userId] and returns its
     * durable metadata (id, storagePath, downloadUrl, …). Throws on failure.
     */
    suspend fun upload(
        userId: String,
        scope: ProfileCardMediaScope,
        bytes: ByteArray,
        mimeType: String,
    ): ProfileCardMedia

    /** Best-effort delete of a previously uploaded object. Never throws to the caller. */
    suspend fun delete(userId: String, storagePath: String)
}
