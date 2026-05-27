package com.example.mindos.features.social.data.repository

import com.example.mindos.features.social.domain.model.ProfileVisitRecord
import com.example.mindos.features.social.domain.model.profileVisitDocumentId
import com.example.mindos.features.social.domain.repository.ProfileVisitRepository
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

class FirebaseProfileVisitRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : ProfileVisitRepository {

    override suspend fun getVisit(
        viewerUserId: UserId,
        profileOwnerId: UserId,
    ): ProfileVisitRecord? {
        requireViewerOwnership(viewerUserId)
        val snapshot = document(viewerUserId, profileOwnerId).get()
        if (!snapshot.exists) return null
        return snapshot.toProfileVisitRecord()
    }

    override suspend fun markVisited(
        viewerUserId: UserId,
        profileOwnerId: UserId,
        visitedAt: Timestamp,
    ): ProfileVisitRecord {
        requireViewerOwnership(viewerUserId)
        val reference = document(viewerUserId, profileOwnerId)
        return firestore.runTransaction {
            val snapshot = get(reference)
            val existing = snapshot.toProfileVisitRecord()
            val record = ProfileVisitRecord(
                viewerUserId = viewerUserId,
                profileOwnerId = profileOwnerId,
                hasVisitedProfile = true,
                firstVisitedAt = existing?.firstVisitedAt ?: visitedAt,
                lastVisitedAt = visitedAt,
            )
            if (snapshot.exists) {
                update(reference, record.toMutableFirestoreMap())
            } else {
                set(reference, record.toCreateFirestoreMap())
            }
            record
        }
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

private fun ProfileVisitRecord.toCreateFirestoreMap(): Map<String, Any?> =
    mapOf(
        "viewerUserId" to viewerUserId.value,
        "profileOwnerId" to profileOwnerId.value,
        "hasVisitedProfile" to hasVisitedProfile,
        "firstVisitedAt" to firstVisitedAt.epochMillis,
        "lastVisitedAt" to lastVisitedAt.epochMillis,
    )

private fun ProfileVisitRecord.toMutableFirestoreMap(): Map<String, Any?> =
    mapOf(
        "hasVisitedProfile" to true,
        "lastVisitedAt" to lastVisitedAt.epochMillis,
    )

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
