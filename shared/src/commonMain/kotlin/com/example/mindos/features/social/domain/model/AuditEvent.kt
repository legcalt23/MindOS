package com.example.mindos.features.social.domain.model

import com.example.mindos.features.social.domain.valueobject.AuditEventId
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId

enum class AuditEventType {
    ProfileCreated,
    FollowCreated,
    FollowRemoved,
    FollowRequestCreated,
    FollowRequestAccepted,
    FollowRequestDenied,
    FollowRequestCanceled,
    FollowRequestInvalidated,
    BlockCreated,
    BlockRemoved,
    PrivacyUpdated,
    FollowerRemoved,
}

data class AuditEvent(
    val id: AuditEventId,
    val type: AuditEventType,
    val actorId: UserId,
    val subjectId: UserId,
    val occurredAt: Timestamp,
    val metadata: Map<String, String> = emptyMap(),
)

