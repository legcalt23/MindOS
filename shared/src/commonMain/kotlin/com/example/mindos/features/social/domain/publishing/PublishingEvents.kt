package com.example.mindos.features.social.domain.publishing

import com.example.mindos.features.social.domain.valueobject.RequestId
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId

enum class EventPartitionKind {
    Author,
    Viewer,
    Post,
}

data class EventPartitionKey(
    val kind: EventPartitionKind,
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "EventPartitionKey value cannot be blank." }
    }

    fun tags(): Map<String, String> =
        mapOf(
            "partitionKind" to kind.name,
            "partitionKey" to value,
        )

    companion object {
        fun author(authorId: UserId): EventPartitionKey =
            EventPartitionKey(EventPartitionKind.Author, authorId.value)

        fun viewer(viewerId: UserId): EventPartitionKey =
            EventPartitionKey(EventPartitionKind.Viewer, viewerId.value)

        fun post(postId: PostId): EventPartitionKey =
            EventPartitionKey(EventPartitionKind.Post, postId.value)
    }
}

data class EventReplayPolicy(
    val deduplicationKey: String,
    val replayable: Boolean = true,
) {
    init {
        require(deduplicationKey.isNotBlank()) { "EventReplayPolicy deduplicationKey cannot be blank." }
    }
}

enum class EventDeliveryMode {
    Live,
    Replay,
}

data class EventEnvelope(
    val event: PublishingEvent,
    val offset: EventOffset,
    val partitionKey: EventPartitionKey,
    val storedAt: Timestamp,
    val deliveryMode: EventDeliveryMode = EventDeliveryMode.Live,
    val deliveryRequestId: RequestId = event.trace.requestId,
) {
    fun tags(): Map<String, String> =
        event.tags() + partitionKey.tags() + mapOf(
            "offset" to offset.value.toString(),
            "deliveryMode" to deliveryMode.name,
            "deliveryRequestId" to deliveryRequestId.value,
        )
}

data class EventAppendResult(
    val envelope: EventEnvelope,
    val duplicate: Boolean,
)

data class EventReplayRequest(
    val requestId: RequestId,
    val subscriberId: EventSubscriberId? = null,
    val fromOffset: EventOffset = EventOffset.First,
    val toOffset: EventOffset? = null,
    val limit: Int = 1_000,
    val force: Boolean = false,
) {
    init {
        require(limit in 1..10_000) { "Replay limit must be between 1 and 10000." }
        require(toOffset == null || toOffset >= fromOffset) { "Replay toOffset must be after fromOffset." }
    }
}

sealed interface PublishingEvent {
    val id: EventId
    val occurredAt: Timestamp
    val trace: TraceContext
    val eventName: String
    val partitionKey: EventPartitionKey
    val replayPolicy: EventReplayPolicy

    fun tags(): Map<String, String> =
        trace.tags() + partitionKey.tags() + mapOf(
            "event" to eventName,
            "eventId" to id.value,
            "deduplicationKey" to replayPolicy.deduplicationKey,
        )
}

data class PostCommittedEvent(
    override val id: EventId,
    val postId: PostId,
    val authorId: UserId,
    val visibility: PostVisibility,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "post-created"
    override val partitionKey: EventPartitionKey = EventPartitionKey.author(authorId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${postId.value}")
}

data class PostPublishedEvent(
    override val id: EventId,
    val postId: PostId,
    val authorId: UserId,
    val materializedCandidates: Int,
    val authorScaleTier: AuthorScaleTier,
    val distributionMode: FanoutDistributionMode,
    val decisionReason: FanoutDecisionReason,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "post-published"
    override val partitionKey: EventPartitionKey = EventPartitionKey.post(postId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${postId.value}")
}

data class PostEditedEvent(
    override val id: EventId,
    val postId: PostId,
    val authorId: UserId,
    val canonicalVersion: Int,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "post-edited"
    override val partitionKey: EventPartitionKey = EventPartitionKey.post(postId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${postId.value}:$canonicalVersion")
}

data class PostDeletedEvent(
    override val id: EventId,
    val postId: PostId,
    val authorId: UserId,
    val canonicalVersion: Int,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "post-deleted"
    override val partitionKey: EventPartitionKey = EventPartitionKey.post(postId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${postId.value}:$canonicalVersion")
}

data class ModerationRequestedEvent(
    override val id: EventId,
    val postId: PostId,
    val authorId: UserId,
    val visibility: PostVisibility,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "moderation-requested"
    override val partitionKey: EventPartitionKey = EventPartitionKey.post(postId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${postId.value}")
}

data class ModerationCompletedEvent(
    override val id: EventId,
    val postId: PostId,
    val state: PostModerationState,
    val reason: String? = null,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "moderation-completed"
    override val partitionKey: EventPartitionKey = EventPartitionKey.post(postId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${postId.value}:${state.name}")
}

data class FanoutRequestedEvent(
    override val id: EventId,
    val postId: PostId,
    val authorId: UserId,
    val mode: FanoutDistributionMode,
    val reason: FanoutDecisionReason,
    val followerCount: Int,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "fanout-requested"
    override val partitionKey: EventPartitionKey = EventPartitionKey.author(authorId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${postId.value}:${mode.name}")
}

data class FeedCandidateGeneratedEvent(
    override val id: EventId,
    val candidateId: FeedCandidateId,
    val viewerId: UserId,
    val postId: PostId,
    val authorId: UserId,
    val source: CandidateSource,
    val distributionMode: FanoutDistributionMode?,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "feed-candidate-generated"
    override val partitionKey: EventPartitionKey = EventPartitionKey.viewer(viewerId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy("$eventName:${candidateId.value}")
}

data class PostStateChangedEvent(
    override val id: EventId,
    val postId: PostId,
    val authorId: UserId,
    val from: PublicationState,
    val to: PublicationState,
    val canonicalVersion: Int,
    override val occurredAt: Timestamp,
    override val trace: TraceContext,
) : PublishingEvent {
    override val eventName: String = "post-state-changed"
    override val partitionKey: EventPartitionKey = EventPartitionKey.post(postId)
    override val replayPolicy: EventReplayPolicy = EventReplayPolicy(
        "$eventName:${postId.value}:${from.name}:${to.name}:$canonicalVersion",
    )
}
