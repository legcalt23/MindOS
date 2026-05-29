package com.example.mindos.features.social.data.repository

import com.example.mindos.core.network.FirestoreDefaults
import com.example.mindos.core.network.withFirestoreReadTimeout
import com.example.mindos.core.network.withFirestoreWriteTimeout
import com.example.mindos.features.social.data.mapper.ProfileVisualConfigDto
import com.example.mindos.features.social.data.mapper.firestoreMapToProfileVisualConfig
import com.example.mindos.features.social.data.mapper.toDomain
import com.example.mindos.features.social.domain.error.ActionNotAuthorized
import com.example.mindos.features.social.domain.error.PersistenceFailure
import com.example.mindos.features.social.domain.error.ProfileNotFound
import com.example.mindos.features.social.domain.error.SelfFollowNotAllowed
import com.example.mindos.features.social.domain.model.InAppNotification
import com.example.mindos.features.social.domain.model.InAppNotificationType
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.repository.InAppNotificationRepository
import com.example.mindos.features.social.domain.repository.ObservationRepository
import com.example.mindos.features.social.domain.repository.ObserveUserResult
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.DomainResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException

class FirebaseSocialInteractionRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : ObservationRepository, InAppNotificationRepository {

    override suspend fun observe(
        observerUserId: UserId,
        observedUserId: UserId,
    ): DomainResult<ObserveUserResult> {
        if (observerUserId == observedUserId) return DomainResult.failure(SelfFollowNotAllowed)
        requireAuthenticatedActor(observerUserId)?.let { return it }

        return firebaseResult {
            val relationId = observationId(observerUserId, observedUserId)
            val observationRef = firestore.collection(ObservationsCollection).document(relationId)
            val observedProfileRef = firestore.collection(ProfileVisualCollection).document(observedUserId.value)
            val actorProfileRef = firestore.collection(ProfileVisualCollection).document(observerUserId.value)
            val notificationRef = firestore.collection(NotificationsCollection).document(relationId)
            val now = System.currentTimeMillis()

            val outcome = withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {
                firestore.runTransaction {
                    val existing = get(observationRef)
                    if (existing.exists) return@runTransaction ObserveTransactionOutcome.AlreadyObserving

                    val observedProfile = get(observedProfileRef)
                    if (!observedProfile.exists) return@runTransaction ObserveTransactionOutcome.TargetMissing

                    val actorProfile = get(actorProfileRef)
                    val actor = actorProfile
                        .takeIf { actorProfile.exists }
                        ?.let { snapshot -> actorSummary(observerUserId, snapshot) }
                        ?: ActorNotificationSummary(
                            username = fallbackUsername(observerUserId.value),
                            displayName = observerUserId.value.take(18),
                            mainPhotoUrl = null,
                            photoStoragePath = null,
                            photoThumbnailStoragePath = null,
                        )

                    val currentCount = observedProfile.toProfileVisualConfig(observedUserId)?.observersCount
                        ?: observedProfile.dataMap().int(ObserversCountField)
                        ?: 0
                    set(
                        observationRef,
                        mapOf(
                            "id" to relationId,
                            "observerUserId" to observerUserId.value,
                            "observedUserId" to observedUserId.value,
                            "createdAt" to now,
                        ),
                    )
                    update(
                        observedProfileRef,
                        mapOf(ObserversCountField to currentCount + 1),
                    )
                    set(
                        notificationRef,
                        mapOf(
                            "notificationId" to relationId,
                            "recipientUserId" to observedUserId.value,
                            "actorUserId" to observerUserId.value,
                            "actorUsername" to actor.username,
                            "actorDisplayName" to actor.displayName,
                            "actorMainPhotoUrl" to actor.mainPhotoUrl,
                            "actorPhotoStoragePath" to actor.photoStoragePath,
                            "actorPhotoThumbnailStoragePath" to actor.photoThumbnailStoragePath,
                            "type" to InAppNotificationType.Observed.name,
                            "timestamp" to now,
                            "targetUserId" to observedUserId.value,
                            "isRead" to false,
                        ),
                    )
                    ObserveTransactionOutcome.Created
                }
            }

            when (outcome) {
                ObserveTransactionOutcome.Created -> DomainResult.success(ObserveUserResult.Created)
                ObserveTransactionOutcome.AlreadyObserving -> DomainResult.success(ObserveUserResult.AlreadyObserving)
                ObserveTransactionOutcome.TargetMissing -> DomainResult.failure(ProfileNotFound(observedUserId))
            }
        }
    }

    override suspend fun isObserving(
        observerUserId: UserId,
        observedUserId: UserId,
    ): DomainResult<Boolean> {
        if (observerUserId == observedUserId) return DomainResult.success(false)
        requireAuthenticatedViewer(observerUserId)?.let { return it }

        return firebaseResult {
            val snapshot = withFirestoreReadTimeout {
                firestore
                    .collection(ObservationsCollection)
                    .document(observationId(observerUserId, observedUserId))
                    .get()
            }
            DomainResult.success(snapshot.exists)
        }
    }

    override suspend fun observersCount(userId: UserId): DomainResult<Int?> =
        firebaseResult {
            val snapshot = withFirestoreReadTimeout {
                firestore
                    .collection(ProfileVisualCollection)
                    .document(userId.value)
                    .get()
            }
            DomainResult.success(snapshot.dataMap().int(ObserversCountField))
        }

    override suspend fun listForRecipient(
        recipientUserId: UserId,
        limit: Int,
    ): DomainResult<List<InAppNotification>> {
        requireAuthenticatedViewer(recipientUserId)?.let { return it }

        return firebaseResult {
            val snapshot = withFirestoreReadTimeout {
                firestore
                    .collection(NotificationsCollection)
                    .where { "recipientUserId" equalTo recipientUserId.value }
                    .limit(limit.coerceIn(1, 100).toLong())
                    .get()
            }
            val notifications = snapshot.documents
                .mapNotNull { document ->
                    document.dataMap().toInAppNotification(fallbackId = document.id)
                }
                .sortedByDescending { it.timestamp.epochMillis }
            DomainResult.success(notifications)
        }
    }

    private fun requireAuthenticatedActor(userId: UserId): DomainResult.Failure? {
        val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return DomainResult.Failure(ActionNotAuthorized("Tu sesion no esta lista."))
        return if (currentUid == userId.value) {
            null
        } else {
            DomainResult.Failure(ActionNotAuthorized("La sesion activa no coincide con el observador."))
        }
    }

    private fun requireAuthenticatedViewer(userId: UserId): DomainResult.Failure? {
        val currentUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: return DomainResult.Failure(ActionNotAuthorized("Tu sesion no esta lista."))
        return if (currentUid == userId.value) {
            null
        } else {
            DomainResult.Failure(ActionNotAuthorized("No puedes leer datos privados de otro usuario."))
        }
    }

    private fun actorSummary(userId: UserId, snapshot: DocumentSnapshot): ActorNotificationSummary {
        val config = snapshot.toProfileVisualConfig(userId)
        val mainMedia = config?.module(ProfileModuleId.Main)?.media
        val username = config?.username?.removePrefix("@")?.takeIf { it.isNotBlank() } ?: fallbackUsername(userId.value)
        return ActorNotificationSummary(
            username = username,
            displayName = config?.displayName?.takeIf { it.isNotBlank() } ?: username,
            mainPhotoUrl = config?.mainProfilePhotoUrl,
            photoStoragePath = config?.mainProfilePhotoUrl ?: mainMedia?.storagePath,
            photoThumbnailStoragePath = mainMedia?.thumbnailStoragePath,
        )
    }
}

private enum class ObserveTransactionOutcome {
    Created,
    AlreadyObserving,
    TargetMissing,
}

private data class ActorNotificationSummary(
    val username: String,
    val displayName: String,
    val mainPhotoUrl: String?,
    val photoStoragePath: String?,
    val photoThumbnailStoragePath: String?,
)

private suspend fun <T> firebaseResult(block: suspend () -> DomainResult<T>): DomainResult<T> =
    try {
        block()
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        DomainResult.failure(
            PersistenceFailure(error.message ?: "No se pudo sincronizar con Firebase."),
        )
    }

private fun observationId(observerUserId: UserId, observedUserId: UserId): String =
    "${observerUserId.value}_${observedUserId.value}"

private fun DocumentSnapshot.dataMap(): Map<String, Any?> =
    if (!exists) {
        emptyMap()
    } else {
        runCatching { data<Map<String, Any?>>() }.getOrElse { emptyMap() }
    }

private fun DocumentSnapshot.toProfileVisualConfig(userId: UserId) =
    runCatching {
        data<ProfileVisualConfigDto>().toDomain(fallbackUserId = userId)
    }.getOrElse {
        firestoreMapToProfileVisualConfig(dataMap(), fallbackUserId = userId)
    }

private fun fallbackUsername(raw: String): String {
    val normalized = raw
        .lowercase()
        .filter { it.isLetterOrDigit() || it == '.' || it == '_' }
        .trim('.')
        .replace(Regex("\\.{2,}"), ".")
        .take(30)
    return normalized.takeIf {
        it.length >= 3 && !it.startsWith(".") && !it.endsWith(".") && !it.contains("..")
    } ?: "user_${raw.lowercase().filter { it.isLetterOrDigit() }.take(18).ifBlank { "neu" }}".take(30)
}

private fun Map<String, Any?>.toInAppNotification(fallbackId: String): InAppNotification? {
    val recipient = string("recipientUserId")?.takeIf { it.isNotBlank() }?.let(::UserId) ?: return null
    val actor = string("actorUserId")?.takeIf { it.isNotBlank() }?.let(::UserId) ?: return null
    val timestamp = long("timestamp") ?: return null
    val type = string("type")
        ?.let { raw -> InAppNotificationType.entries.firstOrNull { it.name == raw } }
        ?: InAppNotificationType.Observed
    return InAppNotification(
        notificationId = string("notificationId")?.takeIf { it.isNotBlank() } ?: fallbackId,
        recipientUserId = recipient,
        actorUserId = actor,
        actorUsername = string("actorUsername")?.takeIf { it.isNotBlank() } ?: actor.value.take(18),
        actorDisplayName = string("actorDisplayName")?.takeIf { it.isNotBlank() } ?: actor.value.take(18),
        actorMainPhotoUrl = string("actorMainPhotoUrl"),
        actorPhotoStoragePath = string("actorPhotoStoragePath"),
        actorPhotoThumbnailStoragePath = string("actorPhotoThumbnailStoragePath"),
        type = type,
        timestamp = Timestamp(timestamp),
        targetUserId = string("targetUserId")?.takeIf { it.isNotBlank() }?.let(::UserId),
        isRead = bool("isRead") ?: false,
    )
}

private fun Map<String, Any?>.string(key: String): String? =
    this[key] as? String

private fun Map<String, Any?>.bool(key: String): Boolean? =
    this[key] as? Boolean

private fun Map<String, Any?>.int(key: String): Int? =
    long(key)?.toInt()

private fun Map<String, Any?>.long(key: String): Long? =
    when (val value = this[key]) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

private const val ProfileVisualCollection = "profile_visuals"
private const val ObservationsCollection = "observations"
private const val NotificationsCollection = "notifications"
private const val ObserversCountField = "observersCount"
