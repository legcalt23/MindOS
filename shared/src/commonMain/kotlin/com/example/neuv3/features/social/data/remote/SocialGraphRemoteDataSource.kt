package com.example.neuv3.features.social.data.remote

import com.example.neuv3.features.social.domain.model.BlockRelation
import com.example.neuv3.features.social.domain.model.FollowRelation
import com.example.neuv3.features.social.domain.model.FollowRequest
import com.example.neuv3.features.social.domain.model.PrivacySettings
import com.example.neuv3.features.social.domain.model.UserProfile
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.DomainResult
import com.example.neuv3.features.social.shared.util.Page
import com.example.neuv3.features.social.shared.util.PageRequest

interface SocialGraphRemoteDataSource {
    suspend fun getProfile(userId: UserId): DomainResult<UserProfile>
    suspend fun getPrivacySettings(userId: UserId): DomainResult<PrivacySettings>
    suspend fun createFollow(actorId: UserId, targetId: UserId): DomainResult<FollowRelation>
    suspend fun deleteFollow(actorId: UserId, targetId: UserId): DomainResult<Unit>
    suspend fun createFollowRequest(actorId: UserId, targetId: UserId): DomainResult<FollowRequest>
    suspend fun acceptFollowRequest(actorId: UserId, requestId: RequestId): DomainResult<FollowRequest>
    suspend fun denyFollowRequest(actorId: UserId, requestId: RequestId): DomainResult<FollowRequest>
    suspend fun cancelFollowRequest(actorId: UserId, requestId: RequestId): DomainResult<FollowRequest>
    suspend fun createBlock(actorId: UserId, targetId: UserId): DomainResult<BlockRelation>
    suspend fun deleteBlock(actorId: UserId, targetId: UserId): DomainResult<Unit>
    suspend fun listFollowers(userId: UserId, pageRequest: PageRequest): DomainResult<Page<UserProfile>>
    suspend fun listFollowing(userId: UserId, pageRequest: PageRequest): DomainResult<Page<UserProfile>>
}

