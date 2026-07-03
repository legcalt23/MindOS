package com.mindos.app.features.profile.card.data

import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileCardMediaScope
import com.mindos.app.features.profile.card.domain.repository.ProfileCardMediaRepository

/**
 * Non-persistent media repository for tests. Records uploaded/deleted storage paths
 * and can simulate an upload failure, so the state-holder media flow is fully testable.
 */
class InMemoryProfileCardMediaRepository : ProfileCardMediaRepository {
    val uploadedPaths = mutableListOf<String>()
    val deletedPaths = mutableListOf<String>()

    /** When non-null, the next [upload] throws this (then it is cleared). */
    var failUploadWith: Throwable? = null

    private var counter = 0

    override suspend fun upload(
        userId: String,
        scope: ProfileCardMediaScope,
        bytes: ByteArray,
        mimeType: String,
    ): ProfileCardMedia {
        failUploadWith?.let { error ->
            failUploadWith = null
            throw error
        }
        val id = "m${counter++}"
        val folder = when (scope) {
            ProfileCardMediaScope.PUBLIC -> "profile_cards_public"
            ProfileCardMediaScope.PRIVATE -> "profile_cards_private"
        }
        val storagePath = "$folder/$userId/media/$id.jpg"
        uploadedPaths += storagePath
        return ProfileCardMedia(
            id = id,
            kind = ProfileCardMediaKind.fromMimeType(mimeType),
            storagePath = storagePath,
            downloadUrl = "https://test.local/$storagePath",
            mimeType = mimeType,
            sizeBytes = bytes.size.toLong(),
        )
    }

    override suspend fun delete(userId: String, storagePath: String) {
        deletedPaths += storagePath
    }
}
