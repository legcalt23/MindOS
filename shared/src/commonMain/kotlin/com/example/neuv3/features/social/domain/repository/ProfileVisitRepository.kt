package com.example.neuv3.features.social.domain.repository

import com.example.neuv3.features.social.domain.model.ProfileVisitRecord
import com.example.neuv3.features.social.domain.valueobject.Timestamp
import com.example.neuv3.features.social.domain.valueobject.UserId

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
