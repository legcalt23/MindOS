package com.mindos.app.features.social.domain.repository

import com.mindos.app.features.social.domain.model.ProfileVisitRecord
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId

interface ProfileVisitRepository {
    suspend fun getVisit(
        viewerUserId: UserId,
        profileOwnerId: UserId,
    ): ProfileVisitRecord?

    suspend fun markVisited(
        viewerUserId: UserId,
        profileOwnerId: UserId,
        visitedAt: Timestamp,
    ): ProfileVisitRecord
}
