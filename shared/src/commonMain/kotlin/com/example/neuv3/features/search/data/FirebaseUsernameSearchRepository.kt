package com.example.neuv3.features.search.data

import com.example.neuv3.features.search.domain.UsernameSearchMaxResults
import com.example.neuv3.features.search.domain.UsernameSearchMinQueryLength
import com.example.neuv3.features.search.domain.UsernameSearchRepository
import com.example.neuv3.features.search.domain.UsernameSearchUser
import com.example.neuv3.features.search.domain.normalizeUsernameSearchQuery
import com.example.neuv3.features.search.domain.usernameMatchScore
import com.example.neuv3.features.social.data.mapper.ProfileVisualConfigDto
import com.example.neuv3.features.social.data.mapper.firestoreMapToProfileVisualConfig
import com.example.neuv3.features.social.data.mapper.toDomain
import com.example.neuv3.features.social.domain.model.ProfileModuleContentType
import com.example.neuv3.features.social.domain.model.ProfileModuleId
import com.example.neuv3.features.social.domain.valueobject.UserId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

class FirebaseUsernameSearchRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : UsernameSearchRepository {

    override suspend fun searchByUsername(
        query: String,
        limit: Int,
    ): List<UsernameSearchUser> {
        val normalizedQuery = normalizeUsernameSearchQuery(query)
        if (normalizedQuery.length < UsernameSearchMinQueryLength) return emptyList()

        val boundedLimit = limit.coerceIn(1, UsernameSearchMaxResults)
        val prefixDocuments = firestore
            .collection(ProfileVisualCollection)
            .where { SearchableField equalTo true }
            .where { DirectoryVisibleField equalTo true }
            .where { UsernameField greaterThanOrEqualTo normalizedQuery }
            .where { UsernameField lessThanOrEqualTo (normalizedQuery + "\uf8ff") }
            .orderBy(UsernameField)
            .limit(boundedLimit)
            .get()
            .documents
        return prefixDocuments
            .distinctBy { it.id }
            .mapNotNull { document ->
                val config = document.toProfileVisualConfig(UserId(document.id)) ?: return@mapNotNull null

                val mainModule = config.module(ProfileModuleId.Main)
                val media = mainModule.media
                val score = usernameMatchScore(normalizedQuery, config.username)
                if (score <= 0f) return@mapNotNull null

                UsernameSearchUser(
                    userId = config.userId.value,
                    username = config.username.removePrefix("@"),
                    displayName = config.displayName,
                    photoStoragePath = config.mainProfilePhotoUrl ?: media?.storagePath,
                    photoThumbnailStoragePath = media?.thumbnailStoragePath,
                    isVideo = mainModule.contentType == ProfileModuleContentType.Video ||
                        media?.mimeType?.startsWith("video/") == true,
                    matchScore = score,
                )
            }
            .distinctBy { it.userId }
            .sortedWith(
                compareByDescending<UsernameSearchUser> { it.matchScore }
                    .thenBy { it.username },
            )
            .take(boundedLimit)
    }
}

private const val ProfileVisualCollection = "profile_visuals"
private const val SearchableField = "searchable"
private const val DirectoryVisibleField = "directoryVisible"
private const val UsernameField = "username"

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
