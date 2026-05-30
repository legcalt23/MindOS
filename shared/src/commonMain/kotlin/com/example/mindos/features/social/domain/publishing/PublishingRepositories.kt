package com.example.mindos.features.social.domain.publishing

import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.Page
import com.example.mindos.features.social.shared.util.PageRequest

interface PublishingTransactionRunner {
    suspend fun <T> read(block: suspend () -> T): T
    suspend fun <T> write(block: suspend () -> T): T
    suspend fun <T> writeResult(block: suspend () -> DomainResult<T>): DomainResult<T>
}

interface CanonicalPostRepository {
    suspend fun get(postId: PostId): CanonicalPost?
    suspend fun insert(post: CanonicalPost): Boolean
    suspend fun update(post: CanonicalPost): Boolean
    suspend fun listPublishedByAuthors(authorIds: Set<UserId>, pageRequest: PageRequest): Page<CanonicalPost>
    suspend fun listByStates(states: Set<PublicationState>, pageRequest: PageRequest): Page<CanonicalPost>
}

data class CommandIdempotencyRecord(
    val key: IdempotencyKey,
    val commandName: String,
    val actorId: UserId,
    val receipt: PublishingCommandReceipt,
    val createdAt: Timestamp,
    val trace: TraceContext,
)

interface CommandIdempotencyRepository {
    suspend fun get(key: IdempotencyKey): CommandIdempotencyRecord?
    suspend fun put(record: CommandIdempotencyRecord): Boolean
}

interface OutboxRepository {
    suspend fun append(message: OutboxMessage): Boolean
    suspend fun claimPending(
        limit: Int,
        now: Timestamp,
        dispatchLeaseTimeoutMillis: Long,
    ): List<OutboxMessage>
    suspend fun hasUndispatchedForAggregate(postId: PostId): Boolean
    suspend fun markDispatched(messageId: OutboxMessageId, at: Timestamp, eventOffset: EventOffset): Boolean
    suspend fun markFailed(messageId: OutboxMessageId, at: Timestamp, error: String, nextAttemptAt: Timestamp): Boolean
}

interface AuthorScaleRepository {
    suspend fun getTier(authorId: UserId): AuthorScaleTier
    suspend fun setTier(authorId: UserId, tier: AuthorScaleTier)
}

interface AudienceRepository {
    suspend fun listFollowers(authorId: UserId, pageRequest: PageRequest): Page<UserId>
    suspend fun listFollowing(viewerId: UserId, pageRequest: PageRequest): Page<UserId>
    suspend fun follows(followerId: UserId, followeeId: UserId): Boolean
    suspend fun countFollowers(authorId: UserId): Int
    suspend fun countFollowing(viewerId: UserId): Int
}

interface FeedCandidateInboxRepository {
    suspend fun upsertAll(candidates: List<FeedCandidate>): Int
    suspend fun listCandidates(viewerId: UserId, pageRequest: PageRequest): Page<FeedCandidate>
    suspend fun listAllCandidates(viewerId: UserId): List<FeedCandidate>
}

interface FeedPostSnapshotRepository {
    suspend fun upsert(snapshot: FeedPostSnapshot): Boolean
    suspend fun get(postId: PostId): FeedPostSnapshot?
    suspend fun getMany(postIds: Collection<PostId>): Map<PostId, FeedPostSnapshot>
}

interface AuthorFeedIndexRepository {
    suspend fun upsert(entry: AuthorFeedIndexEntry): Boolean
    suspend fun listPublishedByAuthors(authorIds: Set<UserId>, pageRequest: PageRequest): Page<AuthorFeedIndexEntry>
}

interface FanoutPolicyConfigRepository {
    suspend fun get(): FanoutPolicyConfig
    suspend fun set(config: FanoutPolicyConfig)
}

interface FanoutDistributionDecisionRepository {
    suspend fun get(postId: PostId): FanoutDistributionDecision?
    suspend fun upsert(decision: FanoutDistributionDecision): Boolean
}

interface FanoutPostMetricsRepository {
    suspend fun get(postId: PostId): FanoutPostMetrics?
    suspend fun upsert(metrics: FanoutPostMetrics): Boolean
}

interface FanoutDeliveryReportRepository {
    suspend fun append(report: FanoutDeliveryReport)
    suspend fun latest(postId: PostId): FanoutDeliveryReport?
    suspend fun listByPost(postId: PostId): List<FanoutDeliveryReport>
}

interface FeedMuteRepository {
    suspend fun isMuted(viewerId: UserId, authorId: UserId): Boolean
    suspend fun mute(viewerId: UserId, authorId: UserId, at: Timestamp): Boolean
    suspend fun unmute(viewerId: UserId, authorId: UserId): Boolean
}

interface FeedModerationRepository {
    suspend fun get(postId: PostId): PostModerationDecision?
    suspend fun upsert(decision: PostModerationDecision): Boolean
}

interface RankedFeedCacheRepository {
    suspend fun get(viewerId: UserId): RankedFeedCacheRecord?
    suspend fun put(record: RankedFeedCacheRecord)
    suspend fun invalidate(viewerId: UserId)

    suspend fun invalidateAll(viewerIds: Iterable<UserId>) {
        viewerIds.toSet().forEach { invalidate(it) }
    }
}

interface PublishingEventJournalRepository {
    suspend fun append(event: PublishingEvent): EventAppendResult
    suspend fun get(eventId: EventId): EventEnvelope?
    suspend fun list(fromOffset: EventOffset, toOffset: EventOffset?, limit: Int): List<EventEnvelope>
}

interface PublishingEventSubscriber {
    val subscriberId: EventSubscriberId
    suspend fun handle(envelope: EventEnvelope): DomainResult<Unit>
}

interface PublishingEventBackbone {
    suspend fun publish(event: PublishingEvent): DomainResult<EventEnvelope>
    suspend fun replay(request: EventReplayRequest): DomainResult<Int>
}

data class PublishingMetric(
    val name: String,
    val tags: Map<String, String>,
    val value: Long,
    val recordedAt: Timestamp,
)

interface PublishingMetricsRecorder {
    suspend fun increment(name: MetricName, tags: MetricTags, value: Long = 1)
    suspend fun increment(name: String, tags: Map<String, String>, value: Long = 1)
    suspend fun recordDistribution(name: MetricName, durationMillis: Long, tags: MetricTags)
    suspend fun recordDistribution(name: String, durationMillis: Long, tags: Map<String, String>)
}

data class PublishingTraceEvent(
    val event: StructuredLogEvent,
    val occurredAt: Timestamp,
)

interface PublishingTraceSink {
    suspend fun record(event: StructuredLogEvent)
    suspend fun record(span: String, trace: TraceContext, attributes: Map<String, String> = emptyMap())
}

fun interface PostIdGenerator {
    fun nextPostId(actorId: UserId, idempotencyKey: IdempotencyKey): PostId
}

fun interface EventIdGenerator {
    fun nextEventId(eventName: String): EventId
}

fun interface OutboxMessageIdGenerator {
    fun nextOutboxMessageId(postId: PostId, event: PublishingEvent): OutboxMessageId
}

fun interface IdempotencyKeyGenerator {
    fun nextIdempotencyKey(): IdempotencyKey
}

fun interface TraceContextFactory {
    fun nextTraceContext(operation: String): TraceContext
}

fun interface PublishingMediaUploader {
    suspend fun upload(
        actorId: UserId,
        draftId: String,
        idempotencyKey: IdempotencyKey,
        media: List<DraftMediaAttachment>,
        trace: TraceContext,
    ): DomainResult<List<PostMediaAttachment>>
}
