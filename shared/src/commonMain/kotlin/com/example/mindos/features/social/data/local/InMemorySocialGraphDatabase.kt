package com.example.mindos.features.social.data.local

import com.example.mindos.features.social.domain.model.AuditEvent
import com.example.mindos.features.social.domain.model.BlockRelation
import com.example.mindos.features.social.domain.model.FollowRelation
import com.example.mindos.features.social.domain.model.FollowRequest
import com.example.mindos.features.social.domain.model.PrivacySettings
import com.example.mindos.features.social.domain.model.ProfileCounters
import com.example.mindos.features.social.domain.model.UserAccount
import com.example.mindos.features.social.domain.model.UserProfile
import com.example.mindos.features.social.domain.repository.SocialGraphTransactionRunner
import com.example.mindos.features.social.domain.valueobject.RelationshipPair
import com.example.mindos.features.social.domain.valueobject.RequestId
import com.example.mindos.features.social.domain.valueobject.UserId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemorySocialGraphDatabase : SocialGraphTransactionRunner {
    internal val state = InMemorySocialGraphState()
    private val mutex = Mutex()

    override suspend fun <T> read(block: suspend () -> T): T =
        mutex.withLock { block() }

    override suspend fun <T> write(block: suspend () -> T): T =
        mutex.withLock { block() }
}

internal class InMemorySocialGraphState {
    val accountsById: MutableMap<UserId, UserAccount> = mutableMapOf()
    val profilesByUserId: MutableMap<UserId, UserProfile> = mutableMapOf()
    val profileUserIdByHandle: MutableMap<String, UserId> = mutableMapOf()
    val privacyByUserId: MutableMap<UserId, PrivacySettings> = mutableMapOf()
    val followsByPair: MutableMap<RelationshipPair, FollowRelation> = mutableMapOf()
    val requestsById: MutableMap<RequestId, FollowRequest> = mutableMapOf()
    val pendingRequestIdByPair: MutableMap<RelationshipPair, RequestId> = mutableMapOf()
    val blocksByPair: MutableMap<RelationshipPair, BlockRelation> = mutableMapOf()
    val countersByUserId: MutableMap<UserId, ProfileCounters> = mutableMapOf()
    val auditEvents: MutableList<AuditEvent> = mutableListOf()
}

