package com.example.mindos.features.social.data.repository

import com.example.mindos.features.social.data.mapper.ProfileVisualConfigDto
import com.example.mindos.features.social.data.mapper.firestoreMapToProfileVisualConfig
import com.example.mindos.features.social.data.mapper.toClientCreateFirestoreMap
import com.example.mindos.features.social.data.mapper.toClientMutableFirestoreUpdateMap
import com.example.mindos.features.social.data.mapper.toDomain
import com.example.mindos.features.social.domain.error.ProfileSaveAppCheckFailed
import com.example.mindos.features.social.domain.error.ProfileSaveException
import com.example.mindos.features.social.domain.error.ProfileSaveError
import com.example.mindos.features.social.domain.error.ProfileSaveNetworkUnavailable
import com.example.mindos.features.social.domain.error.ProfileSaveNotAuthenticated
import com.example.mindos.features.social.domain.error.ProfileSavePermissionDenied
import com.example.mindos.features.social.domain.error.ProfileSaveTimeout
import com.example.mindos.features.social.domain.error.ProfileSaveUnknownRemoteFailure
import com.example.mindos.features.social.domain.error.ProfileSaveValidationError
import com.example.mindos.features.social.domain.model.ProfileMediaMeta
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import com.example.mindos.features.social.domain.model.normalizedProfileMimeType
import com.example.mindos.features.social.domain.repository.ProfileVisualRepository
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.logProfileSave
import com.example.mindos.features.social.shared.util.profileSaveExceptionLog
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.storage.FirebaseStorageException
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.storageMetadata
import kotlinx.coroutines.CancellationException
import kotlin.random.Random

class FirebaseProfileVisualRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val storage: FirebaseStorage = Firebase.storage,
    private val auth: FirebaseAuth = Firebase.auth,
    private val remoteWriteAccessGuard: ProfileRemoteWriteAccessGuard = defaultProfileRemoteWriteAccessGuard(),
) : ProfileVisualRepository {

    override fun currentAuthenticatedUserId(): UserId? =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }?.let(::UserId)

    override suspend fun getConfig(userId: UserId): ProfileVisualConfig? {
        val attemptId = UntrackedAttemptId
        val path = documentPath(userId)
        val snapshot = firebaseOperation(
            attemptId = attemptId,
            phase = "load",
            operation = "firestore_get_profile_visual",
            userId = userId,
            path = path,
        ) {
            document(userId).get()
        }
        if (!snapshot.exists) return null
        return snapshot.toProfileVisualConfig(userId)
    }

    override suspend fun saveConfig(config: ProfileVisualConfig, attemptId: String?) {
        val resolvedAttemptId = attemptId ?: UntrackedAttemptId
        val normalized = config.normalized()
        val path = documentPath(normalized.userId)
        requireOwnedUser(
            userId = normalized.userId,
            attemptId = resolvedAttemptId,
            phase = "commit",
            operation = "firestore_commit_profile_visual",
            path = path,
        )
        remoteWriteAccessGuard.assertCanWrite(
            attemptId = resolvedAttemptId,
            phase = "commit",
            operation = "firestore_commit_profile_visual",
            userId = normalized.userId,
            path = path,
        )
        val reference = document(normalized.userId)
        val privacyReference = firestore.collection(PrivacyCollection).document(normalized.userId.value)
        firebaseOperation(
            attemptId = resolvedAttemptId,
            phase = "commit",
            operation = "firestore_commit_profile_visual",
            userId = normalized.userId,
            path = path,
        ) {
            firestore.runTransaction {
                val snapshot = get(reference)
                val privacySnapshot = get(privacyReference)
                val directoryVisible = privacySnapshot.profileDirectoryVisible()
                if (snapshot.exists) {
                    update(reference, normalized.toClientMutableFirestoreUpdateMap().withDirectoryVisible(directoryVisible))
                } else {
                    set(reference, normalized.toClientCreateFirestoreMap().withDirectoryVisible(directoryVisible))
                }
                if (!privacySnapshot.exists) {
                    set(privacyReference, normalized.toDefaultPrivacyFirestoreMap())
                }
                null
            }
            Unit
        }
    }

    override suspend fun uploadModuleMedia(
        userId: UserId,
        moduleId: ProfileModuleId,
        upload: ProfileMediaUpload,
        uploadedAt: Timestamp,
        attemptId: String?,
    ): ProfileMediaMeta {
        val resolvedAttemptId = attemptId ?: UntrackedAttemptId
        requireOwnedUser(
            userId = userId,
            attemptId = resolvedAttemptId,
            phase = "upload",
            operation = "storage_auth_boundary",
            path = null,
        )
        val normalizedUpload = upload.copy(mimeType = upload.mimeType.normalizedProfileMimeType())
        normalizedUpload.validationError(moduleId)?.let { validationError ->
            throw ProfileSaveException(
                ProfileSaveValidationError(
                    attemptId = resolvedAttemptId,
                    phase = "validation",
                    operation = "validate_profile_media",
                    userId = userId,
                    causeMessage = validationError,
                ),
            )
        }
        remoteWriteAccessGuard.assertCanWrite(
            attemptId = resolvedAttemptId,
            phase = "upload",
            operation = "storage_upload_profile_media",
            userId = userId,
            path = "profile_visuals/${userId.value}/${moduleId.storageFolder}",
        )

        val mediaPath = storagePath(userId, moduleId, normalizedUpload, uploadedAt, resolvedAttemptId)
        val mediaReference = storage.reference.child(mediaPath)

        firebaseOperation(
            attemptId = resolvedAttemptId,
            phase = "upload",
            operation = "storage_upload_profile_media",
            userId = userId,
            path = mediaPath,
        ) {
            mediaReference.putData(
                profileVisualStorageData(normalizedUpload.bytes),
                storageMetadata {
                    contentType = normalizedUpload.mimeType
                    setCustomMetadata("ownerUid", userId.value)
                    setCustomMetadata("visibility", "public")
                },
            )
            Unit
        }
        logProfileSave(
            "event=storage_upload_profile_media_complete save_attempt_id=$resolvedAttemptId " +
                "module=${moduleId.persistedName} mime=${normalizedUpload.mimeType} has_duration=${normalizedUpload.durationMillis != null}"
        )

        val thumbPath = try {
            normalizedUpload.thumbnailBytes?.let { thumbnail ->
                val path = thumbnailPath(userId, moduleId, uploadedAt)
                val thumbnailReference = storage.reference.child(path)

                firebaseOperation(
                    attemptId = resolvedAttemptId,
                    phase = "upload",
                    operation = "storage_upload_profile_thumbnail",
                    userId = userId,
                    path = path,
                ) {
                    thumbnailReference.putData(
                        profileVisualStorageData(thumbnail),
                        storageMetadata {
                            contentType = "image/jpeg"
                            setCustomMetadata("ownerUid", userId.value)
                            setCustomMetadata("visibility", "public")
                        },
                    )
                    Unit
                }

                logProfileSave(
                    "event=storage_upload_profile_thumbnail_complete save_attempt_id=$resolvedAttemptId " +
                        "module=${moduleId.persistedName}"
                )
                path
            }
        } catch (error: Throwable) {
            runCatching { mediaReference.delete() }
            throw error
        }

        return ProfileMediaMeta(
            url = mediaPath,
            storagePath = mediaPath,
            thumbnailUrl = thumbPath,
            thumbnailStoragePath = thumbPath,
            mimeType = normalizedUpload.mimeType,
            durationMillis = normalizedUpload.durationMillis,
            uploadedAt = uploadedAt,
        )
    }

    override suspend fun deleteMedia(userId: UserId, storagePath: String, attemptId: String?) {
        val resolvedAttemptId = attemptId ?: UntrackedAttemptId
        requireOwnedUser(
            userId = userId,
            attemptId = resolvedAttemptId,
            phase = "cleanup",
            operation = "storage_delete_profile_media",
            path = storagePath,
        )
        if (storagePath.isBlank()) return
        if (!isOwnedProfileVisualStoragePath(userId, storagePath)) return
        firebaseOperation(
            attemptId = resolvedAttemptId,
            phase = "cleanup",
            operation = "storage_delete_profile_media",
            userId = userId,
            path = storagePath,
        ) {
            storage.reference.child(storagePath).delete()
            Unit
        }
    }

    private fun document(userId: UserId) =
        firestore.collection(ProfileVisualCollection).document(userId.value)

    private fun documentPath(userId: UserId): String =
        "$ProfileVisualCollection/${userId.value}"

    private fun requireOwnedUser(
        userId: UserId,
        attemptId: String,
        phase: String,
        operation: String,
        path: String?,
    ): UserId {
        val authenticatedUserId = currentAuthenticatedUserId()
            ?: throw ProfileSaveException(
                ProfileSaveNotAuthenticated(
                    attemptId = attemptId,
                    phase = phase,
                    operation = operation,
                    path = path,
                    causeMessage = "FirebaseAuth.currentUser is null.",
                ),
            )
        if (authenticatedUserId != userId) {
            throw ProfileSaveException(
                ProfileSavePermissionDenied(
                    attemptId = attemptId,
                    phase = phase,
                    operation = operation,
                    userId = authenticatedUserId,
                    path = path,
                    causeMessage = "Authenticated uid ${authenticatedUserId.value} does not match target ${userId.value}.",
                ),
            )
        }
        return authenticatedUserId
    }
}

class InMemoryProfileVisualRepository : ProfileVisualRepository {
    private val configs = mutableMapOf<UserId, ProfileVisualConfig>()
    private val media = mutableMapOf<String, ByteArray>()
    var authenticatedUserId: UserId? = null

    override fun currentAuthenticatedUserId(): UserId? =
        authenticatedUserId

    override suspend fun getConfig(userId: UserId): ProfileVisualConfig? =
        configs[userId]

    override suspend fun saveConfig(config: ProfileVisualConfig, attemptId: String?) {
        configs[config.userId] = config.normalized()
    }

    override suspend fun uploadModuleMedia(
        userId: UserId,
        moduleId: ProfileModuleId,
        upload: ProfileMediaUpload,
        uploadedAt: Timestamp,
        attemptId: String?,
    ): ProfileMediaMeta {
        upload.validationError(moduleId)?.let { error(it) }

        val mediaPath = storagePath(userId, moduleId, upload, uploadedAt, attemptId)
        media[mediaPath] = upload.bytes

        val thumbPath = upload.thumbnailBytes?.let {
            val path = thumbnailPath(userId, moduleId, uploadedAt)
            media[path] = it
            path
        }

        return ProfileMediaMeta(
            url = "https://example.invalid/$mediaPath",
            storagePath = mediaPath,
            thumbnailUrl = thumbPath?.let { "https://example.invalid/$it" },
            thumbnailStoragePath = thumbPath,
            mimeType = upload.mimeType,
            durationMillis = upload.durationMillis,
            uploadedAt = uploadedAt,
        )
    }

    override suspend fun deleteMedia(userId: UserId, storagePath: String, attemptId: String?) {
        if (!isOwnedProfileVisualStoragePath(userId, storagePath)) return
        media.remove(storagePath)
    }
}

private const val ProfileVisualCollection = "profile_visuals"
private const val PrivacyCollection = "privacy"
private const val DirectoryVisibleField = "directoryVisible"

private fun ProfileVisualConfig.toDefaultPrivacyFirestoreMap(): Map<String, Any?> =
    mapOf(
        "userId" to userId.value,
        "profileVisibility" to "Public",
        "followersVisibility" to "Everyone",
        "followingVisibility" to "Everyone",
        "messagePermission" to "Mutuals",
        "allowMentionsFrom" to "Followers",
        "discoverable" to true,
        "showActivityStatus" to false,
        "updatedAt" to updatedAt.epochMillis,
    )

private fun Map<String, Any?>.withDirectoryVisible(directoryVisible: Boolean): Map<String, Any?> =
    this + (DirectoryVisibleField to directoryVisible)

private fun DocumentSnapshot.profileDirectoryVisible(): Boolean =
    dataMap()["profileVisibility"] != "Private"

private fun storagePath(
    userId: UserId,
    moduleId: ProfileModuleId,
    upload: ProfileMediaUpload,
    uploadedAt: Timestamp,
    attemptId: String?,
): String {
    val extension = upload.extension()
    val slug = storageSlug(attemptId)
    return "profile_visuals/${userId.value}/${moduleId.storageFolder}/${uploadedAt.epochMillis}_${slug}.$extension"
}

private fun thumbnailPath(
    userId: UserId,
    moduleId: ProfileModuleId,
    uploadedAt: Timestamp,
): String =
    "profile_visuals/${userId.value}/${moduleId.storageFolder}/${uploadedAt.epochMillis}_thumb.jpg"

private fun ProfileMediaUpload.extension(): String =
    when (mimeType.normalizedProfileMimeType()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg" -> "jpg"
        "video/quicktime" -> "mov"
        "video/mp4" -> "mp4"
        else -> if (kind == ProfileMediaUploadKind.Photo) "jpg" else "mp4"
    }

private fun isOwnedProfileVisualStoragePath(userId: UserId, storagePath: String): Boolean {
    val segments = storagePath.split("/")
    if (segments.size != 4) return false
    if (segments.any { it.isBlank() || it == "." || it == ".." }) return false
    if (segments[0] != ProfileVisualCollection) return false
    if (segments[1] != userId.value) return false
    if (segments[2] !in ProfileModuleId.entries.map { it.storageFolder }) return false
    return segments[3].none { it == '/' || it == '\\' }
}

private fun DocumentSnapshot.dataMap(): Map<String, Any?> =
    if (!exists) {
        emptyMap()
    } else {
        runCatching { data<Map<String, Any?>>() }.getOrElse { emptyMap() }
    }

private fun DocumentSnapshot.toProfileVisualConfig(userId: UserId): ProfileVisualConfig? =
    runCatching {
        data<ProfileVisualConfigDto>().toDomain(fallbackUserId = userId)
    }.getOrElse {
        firestoreMapToProfileVisualConfig(dataMap(), fallbackUserId = userId)
    }

private suspend fun <T> firebaseOperation(
    attemptId: String,
    phase: String,
    operation: String,
    userId: UserId,
    path: String?,
    block: suspend () -> T,
): T {
    logProfileSave(
        "event=phase_start save_attempt_id=$attemptId phase=$phase operation=$operation"
    )
    return try {
        val result = block()
        logProfileSave(
            "event=phase_success save_attempt_id=$attemptId phase=$phase operation=$operation"
        )
        result
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        val saveError = error.toProfileSaveError(
            attemptId = attemptId,
            phase = phase,
            operation = operation,
            userId = userId,
            path = path,
        )
        logProfileSave(
            "event=phase_failure save_attempt_id=$attemptId phase=$phase operation=$operation " +
                "error_code=${saveError.code} exception=${error.profileSaveExceptionLog()}"
        )
        throw ProfileSaveException(saveError, error)
    }
}

private fun Throwable.toProfileSaveError(
    attemptId: String,
    phase: String,
    operation: String,
    userId: UserId,
    path: String?,
): ProfileSaveError = when {
    this is ProfileSaveException -> saveError
    this is FirebaseFirestoreException -> toProfileSaveError(attemptId, phase, operation, userId, path)
    this is FirebaseStorageException -> toGenericProfileSaveError(attemptId, phase, operation, userId, path)
    else -> toGenericProfileSaveError(attemptId, phase, operation, userId, path)
}

private fun FirebaseFirestoreException.toProfileSaveError(
    attemptId: String,
    phase: String,
    operation: String,
    userId: UserId,
    path: String?,
): ProfileSaveError {
    if (message.orEmpty().looksLikeAppCheckFailure()) {
        return ProfileSaveAppCheckFailed(
            attemptId = attemptId,
            phase = phase,
            operation = operation,
            userId = userId,
            path = path,
            causeMessage = message,
        )
    }
    return when (code) {
        FirestoreExceptionCode.UNAUTHENTICATED -> ProfileSaveNotAuthenticated(
            attemptId = attemptId,
            phase = phase,
            operation = operation,
            path = path,
            causeMessage = message,
        )
        FirestoreExceptionCode.PERMISSION_DENIED -> ProfileSavePermissionDenied(
            attemptId = attemptId,
            phase = phase,
            operation = operation,
            userId = userId,
            path = path,
            causeMessage = message,
        )
        FirestoreExceptionCode.UNAVAILABLE -> ProfileSaveNetworkUnavailable(
            attemptId = attemptId,
            phase = phase,
            operation = operation,
            userId = userId,
            path = path,
            causeMessage = message,
        )
        FirestoreExceptionCode.DEADLINE_EXCEEDED -> ProfileSaveTimeout(
            attemptId = attemptId,
            phase = phase,
            operation = operation,
            userId = userId,
            path = path,
            causeMessage = message,
        )
        else -> toGenericProfileSaveError(attemptId, phase, operation, userId, path)
    }
}

private fun Throwable.toGenericProfileSaveError(
    attemptId: String,
    phase: String,
    operation: String,
    userId: UserId,
    path: String?,
): ProfileSaveError {
    val diagnostic = "${this::class.simpleName.orEmpty()} ${message.orEmpty()}".lowercase()
    return when {
        diagnostic.looksLikeAppCheckFailure() ->
            ProfileSaveAppCheckFailed(attemptId, phase, operation, userId, path, message)
        "permission" in diagnostic || "insufficient" in diagnostic || "authorized" in diagnostic ->
            ProfileSavePermissionDenied(attemptId, phase, operation, userId, path, message)
        "unauth" in diagnostic || "sesion" in diagnostic || "session" in diagnostic ->
            ProfileSaveNotAuthenticated(attemptId, phase, operation, path, message)
        "network" in diagnostic || "unavailable" in diagnostic ->
            ProfileSaveNetworkUnavailable(attemptId, phase, operation, userId, path, message)
        "timeout" in diagnostic || "deadline" in diagnostic || "timed out" in diagnostic ->
            ProfileSaveTimeout(attemptId, phase, operation, userId, path, message)
        else -> ProfileSaveUnknownRemoteFailure(
            attemptId = attemptId,
            phase = phase,
            operation = operation,
            userId = userId,
            path = path,
            causeMessage = message,
        )
    }
}

private fun storageSlug(attemptId: String?): String {
    val deterministic = attemptId
        ?.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        ?.take(48)
        ?.takeIf { it.isNotBlank() && it != UntrackedAttemptId }
    return deterministic ?: randomStorageSlug()
}

private fun randomStorageSlug(): String =
    buildString(capacity = 16) {
        repeat(16) {
            append(StorageObjectIdAlphabet[Random.nextInt(StorageObjectIdAlphabet.length)])
        }
    }

private const val StorageObjectIdAlphabet =
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-"

private const val UntrackedAttemptId = "profile_save_untracked"

private fun String.looksLikeAppCheckFailure(): Boolean =
    "app check" in this ||
        "attestation" in this ||
        "play integrity" in this ||
        "placeholder token" in this ||
        "too many attempts" in this
