package com.mindos.app.features.social.domain.model

import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId

data class ProfileVisitRecord(
    val viewerUserId: UserId,
    val profileOwnerId: UserId,
    val hasVisitedProfile: Boolean = true,
    val firstVisitedAt: Timestamp,
    val lastVisitedAt: Timestamp,
) {
    init {
        require(hasVisitedProfile) { "Profile visit records must represent a visited profile." }
        require(lastVisitedAt >= firstVisitedAt) { "The latest visit timestamp cannot precede the first visit." }
    }
}

fun profileVisitDocumentId(
    viewerUserId: UserId,
    profileOwnerId: UserId,
): String = "${viewerUserId.value}_${profileOwnerId.value}"
