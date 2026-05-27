package com.example.mindos.features.social.domain.repository

import com.example.mindos.features.social.domain.model.ProfileVisitRecord
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId

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
