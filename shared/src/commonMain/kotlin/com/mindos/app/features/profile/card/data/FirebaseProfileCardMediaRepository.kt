package com.mindos.app.features.profile.card.data

import com.mindos.app.core.logging.safeErrorLog
import com.mindos.app.core.network.StorageDefaults
import com.mindos.app.core.network.withStorageReadTimeout
import com.mindos.app.core.network.withStorageUploadTimeout
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileCardMediaPolicy
import com.mindos.app.features.profile.card.domain.ProfileCardMediaScope
import com.mindos.app.features.profile.card.domain.repository.ProfileCardMediaRepository
import com.mindos.app.features.social.data.repository.profileVisualStorageData
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.storageMetadata
import kotlinx.coroutines.CancellationException
import kotlin.random.Random

/**
 * Uploads/deletes profile-card media to Firebase Storage, reusing the project's
 * gitlive Storage mechanics. Public media goes to `profile_cards_public/{uid}/media`
 * (authenticated read), private drafts to `profile_cards_private/{uid}/media`
 * (owner-only). Objects are immutable (each upload is a new id); replace = upload new
 * + delete old. Returns metadata only — bytes never touch Firestore.
 */
class FirebaseProfileCardMediaRepository(
    private val storage: FirebaseStorage = Firebase.storage,
    private val auth: FirebaseAuth = Firebase.auth,
) : ProfileCardMediaRepository {

    override suspend fun upload(
        userId: String,
        scope: ProfileCardMediaScope,
        bytes: ByteArray,
        mimeType: String,
    ): ProfileCardMedia {
        check(userId.isNotBlank()) { "Falta el identificador de usuario." }
        check(auth.currentUser?.uid == userId) { "No autenticado como el propietario." }
        ProfileCardMediaPolicy.rejectionReason(mimeType, bytes.size.toLong())?.let { reason -> error(reason) }

        val id = generateMediaId()
        val ext = extensionFor(mimeType)
        val folder = when (scope) {
            ProfileCardMediaScope.PUBLIC -> "profile_cards_public"
            ProfileCardMediaScope.PRIVATE -> "profile_cards_private"
        }
        val storagePath = "$folder/$userId/media/$id.$ext"
        val reference = storage.reference.child(storagePath)

        withStorageUploadTimeout(StorageDefaults.SmallUploadTimeout) {
            reference.putData(
                profileVisualStorageData(bytes),
                storageMetadata { contentType = mimeType },
            )
        }

        val downloadUrl = runCatching {
            withStorageReadTimeout(StorageDefaults.DownloadUrlTimeout) { reference.getDownloadUrl() }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            null // best-effort; render can still resolve from storagePath
        }

        return ProfileCardMedia(
            id = id,
            kind = ProfileCardMediaKind.fromMimeType(mimeType),
            storagePath = storagePath,
            downloadUrl = downloadUrl,
            mimeType = mimeType,
            sizeBytes = bytes.size.toLong(),
            createdAtMillis = SystemEpochClock.nowMillis(),
        )
    }

    override suspend fun delete(userId: String, storagePath: String) {
        if (storagePath.isBlank()) return
        runCatching {
            storage.reference.child(storagePath).delete()
        }.onFailure { error ->
            if (error is CancellationException) throw error
            // Best-effort: an undeleted object is an orphan, not a UX failure.
            safeErrorLog(LogTag, "delete profile card media failed (orphan left): $storagePath", error)
        }
    }

    private fun generateMediaId(): String =
        "m${SystemEpochClock.nowMillis()}_${Random.nextInt(0, ID_SUFFIX_BOUND)}"

    private fun extensionFor(mimeType: String): String = when (mimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "video/mp4" -> "mp4"
        else -> "jpg"
    }

    private companion object {
        const val ID_SUFFIX_BOUND = 1_000_000
        const val LogTag = "ProfileCard"
    }
}
