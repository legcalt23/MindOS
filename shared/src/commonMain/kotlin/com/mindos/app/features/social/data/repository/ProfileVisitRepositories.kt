package com.mindos.app.features.social.data.repository

import com.mindos.app.core.network.withFirestoreReadTimeout
import com.mindos.app.core.network.withFunctionsWriteTimeout
import com.mindos.app.features.social.domain.model.ProfileVisitRecord
import com.mindos.app.features.social.domain.model.profileVisitDocumentId
import com.mindos.app.features.social.domain.repository.ProfileVisitRepository
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.FirebaseFunctions
import dev.gitlive.firebase.functions.functions

class FirebaseProfileVisitRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val functions: FirebaseFunctions = Firebase.functions,
) : ProfileVisitRepository {

    override suspend fun getVisit(
        viewerUserId: UserId,
        profileOwnerId: UserId,
    ): ProfileVisitRecord? {
        requireViewerOwnership(viewerUserId)
        val snapshot = withFirestoreReadTimeout { document(viewerUserId, profileOwnerId).get() }
        if (!snapshot.exists) return null
        return snapshot.toProfileVisitRecord()
    }

    override suspend fun markVisited(
        viewerUserId: UserId,
        profileOwnerId: UserId,
        visitedAt: Timestamp,
    ): ProfileVisitRecord {
        requireViewerOwnership(viewerUserId)
        withFunctionsWriteTimeout {
            functions.httpsCallable("recordProfileVisit")
                .invoke(mapOf("profileOwnerId" to profileOwnerId.value))
        }
        return getVisit(viewerUserId, profileOwnerId)
            ?: ProfileVisitRecord(
                viewerUserId = viewerUserId,
                profileOwnerId = profileOwnerId,
                hasVisitedProfile = true,
                firstVisitedAt = visitedAt,
                lastVisitedAt = visitedAt,
            )
    }

    private fun document(
        viewerUserId: UserId,
        profileOwnerId: UserId,
    ) = firestore.collection(ProfileVisitsCollection)
        .document(profileVisitDocumentId(viewerUserId, profileOwnerId))

    private fun requireViewerOwnership(viewerUserId: UserId) {
        val authenticated = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        require(authenticated == viewerUserId.value) {
            "Authenticated uid must match the viewer visit owner."
        }
    }
}

class InMemoryProfileVisitRepository : ProfileVisitRepository {
    private val visits = mutableMapOf<String, ProfileVisitRecord>()

    override suspend fun getVisit(
        viewerUserId: UserId,
        profileOwnerId: UserId,
    ): ProfileVisitRecord? = visits[profileVisitDocumentId(viewerUserId, profileOwnerId)]

    override suspend fun markVisited(
        viewerUserId: UserId,
        profileOwnerId: UserId,
        visitedAt: Timestamp,
    ): ProfileVisitRecord {
        val documentId = profileVisitDocumentId(viewerUserId, profileOwnerId)
        val existing = visits[documentId]
        return ProfileVisitRecord(
            viewerUserId = viewerUserId,
            profileOwnerId = profileOwnerId,
            firstVisitedAt = existing?.firstVisitedAt ?: visitedAt,
            lastVisitedAt = visitedAt,
        ).also { visits[documentId] = it }
    }
}

private const val ProfileVisitsCollection = "profile_visits"

private fun DocumentSnapshot.toProfileVisitRecord(): ProfileVisitRecord? {
    if (!exists) return null
    val map = runCatching { data<Map<String, Any?>>() }.getOrDefault(emptyMap())
    val viewer = (map["viewerUserId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val owner = (map["profileOwnerId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    val hasVisited = map["hasVisitedProfile"] as? Boolean ?: false
    val firstVisitedAt = map.long("firstVisitedAt") ?: return null
    val lastVisitedAt = map.long("lastVisitedAt") ?: firstVisitedAt
    return ProfileVisitRecord(
        viewerUserId = UserId(viewer),
        profileOwnerId = UserId(owner),
        hasVisitedProfile = hasVisited,
        firstVisitedAt = Timestamp(firstVisitedAt.coerceAtLeast(0L)),
        lastVisitedAt = Timestamp(lastVisitedAt.coerceAtLeast(firstVisitedAt)),
    )
}

private fun Map<String, Any?>.long(key: String): Long? =
    when (val value = this[key]) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
