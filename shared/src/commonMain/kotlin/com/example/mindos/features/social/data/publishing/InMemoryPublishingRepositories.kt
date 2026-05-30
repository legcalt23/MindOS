package com.example.mindos.features.social.data.publishing

import com.example.mindos.features.social.data.local.InMemorySocialGraphDatabase
import com.example.mindos.features.social.domain.publishing.AudienceRepository
import com.example.mindos.features.social.domain.publishing.AuthorFeedIndexEntry
import com.example.mindos.features.social.domain.publishing.AuthorFeedIndexRepository
import com.example.mindos.features.social.domain.publishing.AuthorScaleRepository
import com.example.mindos.features.social.domain.publishing.AuthorScaleTier
import com.example.mindos.features.social.domain.publishing.CanonicalPost
import com.example.mindos.features.social.domain.publishing.CanonicalPostRepository
import com.example.mindos.features.social.domain.publishing.CommandIdempotencyRecord
import com.example.mindos.features.social.domain.publishing.CommandIdempotencyRepository
import com.example.mindos.features.social.domain.publishing.EventId
import com.example.mindos.features.social.domain.publishing.EventAppendResult
import com.example.mindos.features.social.domain.publishing.EventDeliveryMode
import com.example.mindos.features.social.domain.publishing.EventEnvelope
import com.example.mindos.features.social.domain.publishing.EventOffset
import com.example.mindos.features.social.domain.publishing.EventReplayRequest
import com.example.mindos.features.social.domain.publishing.EventSubscriberId
import com.example.mindos.features.social.domain.publishing.FanoutDeliveryReport
import com.example.mindos.features.social.domain.publishing.FanoutDeliveryReportRepository
import com.example.mindos.features.social.domain.publishing.FanoutDistributionDecision
import com.example.mindos.features.social.domain.publishing.FanoutDistributionDecisionRepository
import com.example.mindos.features.social.domain.publishing.FanoutPolicyConfig
import com.example.mindos.features.social.domain.publishing.FanoutPolicyConfigRepository
import com.example.mindos.features.social.domain.publishing.FanoutPostMetrics
import com.example.mindos.features.social.domain.publishing.FanoutPostMetricsRepository
import com.example.mindos.features.social.domain.publishing.FeedCandidate
import com.example.mindos.features.social.domain.publishing.FeedCandidateInboxRepository
import com.example.mindos.features.social.domain.publishing.PostModerationDecision
import com.example.mindos.features.social.domain.publishing.FeedModerationRepository
import com.example.mindos.features.social.domain.publishing.FeedMuteRepository
import com.example.mindos.features.social.domain.publishing.FeedPostSnapshot
import com.example.mindos.features.social.domain.publishing.FeedPostSnapshotRepository
import com.example.mindos.features.social.domain.publishing.IdempotencyKey
import com.example.mindos.features.social.domain.publishing.OutboxMessage
import com.example.mindos.features.social.domain.publishing.OutboxMessageId
import com.example.mindos.features.social.domain.publishing.OutboxRepository
import com.example.mindos.features.social.domain.publishing.OutboxStatus
import com.example.mindos.features.social.domain.publishing.PostId
import com.example.mindos.features.social.domain.publishing.PublicationState
import com.example.mindos.features.social.domain.publishing.PublishingEvent
import com.example.mindos.features.social.domain.publishing.PublishingEventBackbone
import com.example.mindos.features.social.domain.publishing.PublishingEventJournalRepository
import com.example.mindos.features.social.domain.publishing.PublishingEventSubscriber
import com.example.mindos.features.social.domain.publishing.PublishingMetric
import com.example.mindos.features.social.domain.publishing.PublishingMetricsRecorder
import com.example.mindos.features.social.domain.publishing.PublishingTraceEvent
import com.example.mindos.features.social.domain.publishing.PublishingTraceSink
import com.example.mindos.features.social.domain.publishing.RankedFeedCacheRecord
import com.example.mindos.features.social.domain.publishing.RankedFeedCacheRepository
import com.example.mindos.features.social.domain.publishing.StructuredLogEvent
import com.example.mindos.features.social.domain.valueobject.Cursor
import com.example.mindos.features.social.domain.valueobject.RelationshipPair
import com.example.mindos.features.social.domain.valueobject.Timestamp
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.AppClock
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.Page
import com.example.mindos.features.social.shared.util.PageRequest

class InMemoryCanonicalPostRepository(
    private val database: InMemoryPublishingDatabase,
) : CanonicalPostRepository {
    override suspend fun get(postId: PostId): CanonicalPost? =
        database.state.postsById[postId]

    override suspend fun insert(post: CanonicalPost): Boolean {
        if (database.state.postsById.containsKey(post.id)) return false
        database.state.postsById[post.id] = post
        return true
    }

    override suspend fun update(post: CanonicalPost): Boolean {
        if (!database.state.postsById.containsKey(post.id)) return false
        database.state.postsById[post.id] = post
        return true
    }

    override suspend fun listPublishedByAuthors(
        authorIds: Set<UserId>,
        pageRequest: PageRequest,
    ): Page<CanonicalPost> =
        database.state.postsById.values
            .filter { it.authorId in authorIds && it.state == PublicationState.Published }
            .sortedWith(compareByDescending<CanonicalPost> { it.createdAt.epochMillis }.thenBy { it.id.value })
            .pageByDescendingTimestamp(pageRequest) { it.createdAt.epochMillis to it.id.value }

    override suspend fun listByStates(
        states: Set<PublicationState>,
        pageRequest: PageRequest,
    ): Page<CanonicalPost> =
        database.state.postsById.values
            .filter { it.state in states }
            .sortedWith(compareBy<CanonicalPost> { it.updatedAt.epochMillis }.thenBy { it.id.value })
            .pageByAscendingTimestamp(pageRequest) { it.updatedAt.epochMillis to it.id.value }
}

class InMemoryCommandIdempotencyRepository(
    private val database: InMemoryPublishingDatabase,
) : CommandIdempotencyRepository {
    override suspend fun get(key: IdempotencyKey): CommandIdempotencyRecord? =
        database.state.idempotencyByKey[key]

    override suspend fun put(record: CommandIdempotencyRecord): Boolean {
        if (database.state.idempotencyByKey.containsKey(record.key)) return false
        database.state.idempotencyByKey[record.key] = record
        return true
    }
}

class InMemoryOutboxRepository(
    private val database: InMemoryPublishingDatabase,
) : OutboxRepository {
    override suspend fun append(message: OutboxMessage): Boolean {
        if (database.state.outboxById.containsKey(message.id)) return false
        database.state.outboxById[message.id] = message
        database.state.outboxOrder += message.id
        return true
    }

    override suspend fun claimPending(
        limit: Int,
        now: Timestamp,
        dispatchLeaseTimeoutMillis: Long,
    ): List<OutboxMessage> {
        val claimed = database.state.outboxOrder
            .mapNotNull { database.state.outboxById[it] }
            .filter { it.isClaimable(now, dispatchLeaseTimeoutMillis) }
            .sortedWith(compareBy<OutboxMessage> { it.createdAt.epochMillis }.thenBy { it.id.value })
            .take(limit)
            .map { message ->
                message.copy(
                    status = OutboxStatus.Dispatching,
                    updatedAt = now,
                    attempts = message.attempts + 1,
                    claimedAt = now,
                )
            }

        claimed.forEach { database.state.outboxById[it.id] = it }
        return claimed
    }

    override suspend fun hasUndispatchedForAggregate(postId: PostId): Boolean =
        database.state.outboxById.values.any {
            it.aggregateId == postId && it.status != OutboxStatus.Dispatched
        }

    override suspend fun markDispatched(messageId: OutboxMessageId, at: Timestamp, eventOffset: EventOffset): Boolean =
        update(messageId) {
            it.copy(
                status = OutboxStatus.Dispatched,
                updatedAt = at,
                lastError = null,
                claimedAt = null,
                dispatchedAt = at,
                eventOffset = eventOffset,
            )
        }

    override suspend fun markFailed(messageId: OutboxMessageId, at: Timestamp, error: String, nextAttemptAt: Timestamp): Boolean =
        update(messageId) {
            it.copy(
                status = OutboxStatus.Failed,
                updatedAt = at,
                lastError = error,
                nextAttemptAt = nextAttemptAt,
                claimedAt = null,
            )
        }

    private fun update(messageId: OutboxMessageId, transform: (OutboxMessage) -> OutboxMessage): Boolean {
        val current = database.state.outboxById[messageId] ?: return false
        database.state.outboxById[messageId] = transform(current)
        return true
    }
}

private fun OutboxMessage.isClaimable(now: Timestamp, dispatchLeaseTimeoutMillis: Long): Boolean =
    when (status) {
        OutboxStatus.Pending,
        OutboxStatus.Failed -> nextAttemptAt <= now
        OutboxStatus.Dispatching -> {
            val claimed = claimedAt ?: return true
            now.epochMillis - claimed.epochMillis >= dispatchLeaseTimeoutMillis
        }
        OutboxStatus.Dispatched -> false
    }

class InMemoryAuthorScaleRepository(
    private val database: InMemoryPublishingDatabase,
) : AuthorScaleRepository {
    override suspend fun getTier(authorId: UserId): AuthorScaleTier =
        database.state.authorScaleByUser[authorId] ?: AuthorScaleTier.Normal

    override suspend fun setTier(authorId: UserId, tier: AuthorScaleTier) {
        database.state.authorScaleByUser[authorId] = tier
    }
}

class InMemoryFeedCandidateInboxRepository(
    private val database: InMemoryPublishingDatabase,
) : FeedCandidateInboxRepository {
    override suspend fun upsertAll(candidates: List<FeedCandidate>): Int {
        var changed = 0
        for (candidate in candidates) {
            val inbox = database.state.feedCandidatesByViewer.getOrPut(candidate.viewerId) { mutableMapOf() }
            val previous = inbox[candidate.id]
            if (previous == null) {
                inbox[candidate.id] = candidate
                changed++
            } else if (!previous.sameMaterializedCandidate(candidate)) {
                inbox[candidate.id] = candidate
                changed++
            }
        }
        return changed
    }

    override suspend fun listCandidates(viewerId: UserId, pageRequest: PageRequest): Page<FeedCandidate> =
        database.state.feedCandidatesByViewer[viewerId].orEmpty().values
            .sortedWith(compareByDescending<FeedCandidate> { it.postCreatedAt.epochMillis }.thenBy { it.postId.value })
            .pageByDescendingTimestamp(pageRequest) { it.postCreatedAt.epochMillis to it.postId.value }

    override suspend fun listAllCandidates(viewerId: UserId): List<FeedCandidate> =
        database.state.feedCandidatesByViewer[viewerId].orEmpty().values
            .sortedWith(compareByDescending<FeedCandidate> { it.postCreatedAt.epochMillis }.thenBy { it.postId.value })
}

class InMemoryFeedPostSnapshotRepository(
    private val database: InMemoryPublishingDatabase,
) : FeedPostSnapshotRepository {
    override suspend fun upsert(snapshot: FeedPostSnapshot): Boolean {
        val previous = database.state.feedPostSnapshotsById.put(snapshot.postId, snapshot)
        return previous != snapshot
    }

    override suspend fun get(postId: PostId): FeedPostSnapshot? =
        database.state.feedPostSnapshotsById[postId]

    override suspend fun getMany(postIds: Collection<PostId>): Map<PostId, FeedPostSnapshot> =
        postIds.toSet().mapNotNull { postId ->
            database.state.feedPostSnapshotsById[postId]?.let { postId to it }
        }.toMap()
}

class InMemoryAuthorFeedIndexRepository(
    private val database: InMemoryPublishingDatabase,
) : AuthorFeedIndexRepository {
    override suspend fun upsert(entry: AuthorFeedIndexEntry): Boolean {
        val previous = database.state.authorFeedIndexByPostId.put(entry.postId, entry)
        return previous != entry
    }

    override suspend fun listPublishedByAuthors(
        authorIds: Set<UserId>,
        pageRequest: PageRequest,
    ): Page<AuthorFeedIndexEntry> =
        database.state.authorFeedIndexByPostId.values
            .filter { it.authorId in authorIds }
            .sortedWith(compareByDescending<AuthorFeedIndexEntry> { it.postCreatedAt.epochMillis }.thenBy { it.postId.value })
            .pageByDescendingTimestamp(pageRequest) { it.postCreatedAt.epochMillis to it.postId.value }
}

class InMemoryFanoutPolicyConfigRepository(
    private val database: InMemoryPublishingDatabase,
) : FanoutPolicyConfigRepository {
    override suspend fun get(): FanoutPolicyConfig =
        database.state.fanoutPolicyConfig

    override suspend fun set(config: FanoutPolicyConfig) {
        database.state.fanoutPolicyConfig = config
    }
}

class InMemoryFanoutDistributionDecisionRepository(
    private val database: InMemoryPublishingDatabase,
) : FanoutDistributionDecisionRepository {
    override suspend fun get(postId: PostId): FanoutDistributionDecision? =
        database.state.fanoutDecisionsByPostId[postId]

    override suspend fun upsert(decision: FanoutDistributionDecision): Boolean {
        val previous = database.state.fanoutDecisionsByPostId.put(decision.postId, decision)
        return previous != decision
    }
}

class InMemoryFanoutPostMetricsRepository(
    private val database: InMemoryPublishingDatabase,
) : FanoutPostMetricsRepository {
    override suspend fun get(postId: PostId): FanoutPostMetrics? =
        database.state.fanoutPostMetricsByPostId[postId]

    override suspend fun upsert(metrics: FanoutPostMetrics): Boolean {
        val previous = database.state.fanoutPostMetricsByPostId.put(metrics.postId, metrics)
        return previous != metrics
    }
}

class InMemoryFanoutDeliveryReportRepository(
    private val database: InMemoryPublishingDatabase,
) : FanoutDeliveryReportRepository {
    override suspend fun append(report: FanoutDeliveryReport) {
        database.state.fanoutReportsByPostId.getOrPut(report.postId) { mutableListOf() } += report
    }

    override suspend fun latest(postId: PostId): FanoutDeliveryReport? =
        database.state.fanoutReportsByPostId[postId]?.maxByOrNull { it.attempt }

    override suspend fun listByPost(postId: PostId): List<FanoutDeliveryReport> =
        database.state.fanoutReportsByPostId[postId].orEmpty().sortedBy { it.attempt }
}

class InMemoryFeedMuteRepository(
    private val database: InMemoryPublishingDatabase,
) : FeedMuteRepository {
    override suspend fun isMuted(viewerId: UserId, authorId: UserId): Boolean =
        viewerId != authorId && database.state.feedMutesByPair.containsKey(RelationshipPair(viewerId, authorId))

    override suspend fun mute(viewerId: UserId, authorId: UserId, at: Timestamp): Boolean {
        if (viewerId == authorId) return false
        val previous = database.state.feedMutesByPair.put(RelationshipPair(viewerId, authorId), at)
        return previous == null
    }

    override suspend fun unmute(viewerId: UserId, authorId: UserId): Boolean =
        database.state.feedMutesByPair.remove(RelationshipPair(viewerId, authorId)) != null
}

class InMemoryFeedModerationRepository(
    private val database: InMemoryPublishingDatabase,
) : FeedModerationRepository {
    override suspend fun get(postId: PostId): PostModerationDecision? =
        database.state.moderationByPostId[postId]

    override suspend fun upsert(decision: PostModerationDecision): Boolean {
        val previous = database.state.moderationByPostId.put(decision.postId, decision)
        return previous != decision
    }
}

class InMemoryRankedFeedCacheRepository(
    private val database: InMemoryPublishingDatabase,
) : RankedFeedCacheRepository {
    override suspend fun get(viewerId: UserId): RankedFeedCacheRecord? =
        database.state.rankedFeedCacheByViewer[viewerId]

    override suspend fun put(record: RankedFeedCacheRecord) {
        database.state.rankedFeedCacheByViewer[record.viewerId] = record
    }

    override suspend fun invalidate(viewerId: UserId) {
        database.state.rankedFeedCacheByViewer.remove(viewerId)
    }
}

class InMemoryPublishingEventJournalRepository(
    private val database: InMemoryPublishingDatabase,
    private val clock: AppClock,
) : PublishingEventJournalRepository {
    override suspend fun append(event: PublishingEvent): EventAppendResult {
        database.state.eventsById[event.id]?.let { existing ->
            return EventAppendResult(envelope = existing, duplicate = true)
        }
        val envelope = EventEnvelope(
            event = event,
            offset = EventOffset(database.state.nextEventOffset++),
            partitionKey = event.partitionKey,
            storedAt = clock.now(),
        )
        database.state.eventsById[event.id] = envelope
        database.state.eventJournal += envelope
        return EventAppendResult(envelope = envelope, duplicate = false)
    }

    override suspend fun get(eventId: EventId): EventEnvelope? =
        database.state.eventsById[eventId]

    override suspend fun list(fromOffset: EventOffset, toOffset: EventOffset?, limit: Int): List<EventEnvelope> =
        database.state.eventJournal
            .filter { envelope ->
                envelope.offset >= fromOffset && (toOffset == null || envelope.offset <= toOffset)
            }
            .sortedBy { it.offset.value }
            .take(limit)
}

class InMemoryPublishingEventBackbone(
    private val database: InMemoryPublishingDatabase,
    private val journal: PublishingEventJournalRepository,
    private val metrics: PublishingMetricsRecorder,
    private val clock: AppClock,
) : PublishingEventBackbone {
    private val subscribers: MutableList<PublishingEventSubscriber> = mutableListOf()

    fun register(subscriber: PublishingEventSubscriber) {
        subscribers.removeAll { it.subscriberId == subscriber.subscriberId }
        subscribers += subscriber
    }

    override suspend fun publish(event: PublishingEvent): DomainResult<EventEnvelope> {
        val append = journal.append(event)
        val envelope = append.envelope
        if (append.duplicate) {
            metrics.increment("event.backbone.duplicate", envelope.tags())
        } else {
            metrics.increment("event.backbone.published", envelope.tags())
        }
        return when (val delivered = deliver(envelope, subscribers, force = false)) {
            is DomainResult.Success -> DomainResult.success(envelope)
            is DomainResult.Failure -> delivered
        }
    }

    override suspend fun replay(request: EventReplayRequest): DomainResult<Int> {
        var delivered = 0
        val targetSubscribers = request.subscriberId?.let { subscriberId ->
            subscribers.filter { it.subscriberId == subscriberId }
        } ?: subscribers
        val envelopes = journal.list(
            fromOffset = request.fromOffset,
            toOffset = request.toOffset,
            limit = request.limit,
        )
        for (envelope in envelopes) {
            if (!envelope.event.replayPolicy.replayable) continue
            val replayEnvelope = envelope.copy(
                deliveryMode = EventDeliveryMode.Replay,
                deliveryRequestId = request.requestId,
            )
            when (val result = deliver(replayEnvelope, targetSubscribers, force = request.force)) {
                is DomainResult.Success -> delivered += result.value
                is DomainResult.Failure -> return result
            }
        }
        metrics.increment(
            "event.backbone.replay.completed",
            mapOf(
                "requestId" to request.requestId.value,
                "fromOffset" to request.fromOffset.value.toString(),
                "toOffset" to request.toOffset?.value?.toString().orEmpty(),
                "force" to request.force.toString(),
            ),
            delivered.toLong(),
        )
        return DomainResult.success(delivered)
    }

    private suspend fun deliver(
        envelope: EventEnvelope,
        targetSubscribers: List<PublishingEventSubscriber>,
        force: Boolean,
    ): DomainResult<Int> {
        var delivered = 0
        for (subscriber in targetSubscribers) {
            val key = EventDeliveryKey(subscriber.subscriberId, envelope.event.id)
            if (!force && key in database.state.deliveredEventsBySubscriber) {
                metrics.increment(
                    "event.consumer.duplicate_skipped",
                    envelope.tags() + mapOf("subscriberId" to subscriber.subscriberId.value),
                )
                continue
            }
            val attempt = database.state.deliveryAttemptsBySubscriber.getOrDefault(key, 0) + 1
            database.state.deliveryAttemptsBySubscriber[key] = attempt
            when (val result = subscriber.handle(envelope)) {
                is DomainResult.Success -> {
                    database.state.deliveredEventsBySubscriber += key
                    metrics.increment(
                        "event.consumer.delivered",
                        envelope.tags() + mapOf(
                            "subscriberId" to subscriber.subscriberId.value,
                            "attempt" to attempt.toString(),
                            "deliveredAt" to clock.now().epochMillis.toString(),
                        ),
                    )
                    delivered++
                }
                is DomainResult.Failure -> {
                    metrics.increment(
                        "event.consumer.failed",
                        envelope.tags() + mapOf(
                            "subscriberId" to subscriber.subscriberId.value,
                            "attempt" to attempt.toString(),
                        ),
                    )
                    return result
                }
            }
        }
        return DomainResult.success(delivered)
    }
}

class InMemoryPublishingMetricsRecorder(
    private val database: InMemoryPublishingDatabase,
    private val clock: AppClock,
) : PublishingMetricsRecorder {
    override suspend fun increment(name: String, tags: Map<String, String>, value: Long) {
        database.state.metrics += PublishingMetric(
            name = name,
            tags = tags,
            value = value,
            recordedAt = clock.now(),
        )
    }

    override suspend fun increment(name: com.example.mindos.features.social.domain.publishing.MetricName, tags: com.example.mindos.features.social.domain.publishing.MetricTags, value: Long) {
        database.state.metrics += PublishingMetric(
            name = name.name,
            tags = tags.toMap(),
            value = value,
            recordedAt = clock.now(),
        )
    }

    override suspend fun recordDistribution(name: com.example.mindos.features.social.domain.publishing.MetricName, durationMillis: Long, tags: com.example.mindos.features.social.domain.publishing.MetricTags) {
        database.state.metrics += PublishingMetric(
            name = name.name + ".distribution",
            tags = tags.toMap(),
            value = durationMillis,
            recordedAt = clock.now(),
        )
    }

    override suspend fun recordDistribution(name: String, durationMillis: Long, tags: Map<String, String>) {
        database.state.metrics += PublishingMetric(
            name = "$name.distribution",
            tags = tags,
            value = durationMillis,
            recordedAt = clock.now(),
        )
    }

    fun snapshot(): List<PublishingMetric> =
        database.state.metrics.toList()
}

class InMemoryPublishingTraceSink(
    private val database: InMemoryPublishingDatabase,
    private val clock: AppClock,
) : PublishingTraceSink {
    override suspend fun record(event: com.example.mindos.features.social.domain.publishing.StructuredLogEvent) {
        database.state.traces += PublishingTraceEvent(
            event = event,
            occurredAt = clock.now(),
        )
    }

    override suspend fun record(span: String, trace: com.example.mindos.features.social.domain.publishing.TraceContext, attributes: Map<String, String>) {
        database.state.traces += PublishingTraceEvent(
            event = StructuredLogEvent(
                traceId = trace.traceId.value,
                requestId = trace.requestId.value,
                postId = trace.postId?.value,
                event = span,
                stage = attributes["stage"] ?: span,
                result = attributes["result"],
            ),
            occurredAt = clock.now(),
        )
    }

    fun snapshot(): List<PublishingTraceEvent> =
        database.state.traces.toList()
}

class InMemorySocialAudienceRepository(
    private val socialDatabase: InMemorySocialGraphDatabase,
) : AudienceRepository {
    override suspend fun listFollowers(authorId: UserId, pageRequest: PageRequest): Page<UserId> =
        socialDatabase.state.followsByPair.values
            .filter { it.followeeId == authorId }
            .sortedWith(compareBy({ it.createdAt.epochMillis }, { it.followerId.value }))
            .pageByAscendingTimestamp(pageRequest) { it.createdAt.epochMillis to it.followerId.value }
            .let { page -> Page(items = page.items.map { it.followerId }, nextCursor = page.nextCursor) }

    override suspend fun listFollowing(viewerId: UserId, pageRequest: PageRequest): Page<UserId> =
        socialDatabase.state.followsByPair.values
            .filter { it.followerId == viewerId }
            .sortedWith(compareBy({ it.createdAt.epochMillis }, { it.followeeId.value }))
            .pageByAscendingTimestamp(pageRequest) { it.createdAt.epochMillis to it.followeeId.value }
            .let { page -> Page(items = page.items.map { it.followeeId }, nextCursor = page.nextCursor) }

    override suspend fun follows(followerId: UserId, followeeId: UserId): Boolean =
        followerId != followeeId && socialDatabase.state.followsByPair.containsKey(RelationshipPair(followerId, followeeId))

    override suspend fun countFollowers(authorId: UserId): Int =
        socialDatabase.state.followsByPair.values.count { it.followeeId == authorId }

    override suspend fun countFollowing(viewerId: UserId): Int =
        socialDatabase.state.followsByPair.values.count { it.followerId == viewerId }
}

private data class StableCursor(
    val millis: Long,
    val id: String,
)

private fun FeedCandidate.sameMaterializedCandidate(other: FeedCandidate): Boolean =
    viewerId == other.viewerId &&
        postId == other.postId &&
        authorId == other.authorId &&
        source == other.source &&
        origin == other.origin &&
        distributionMode == other.distributionMode &&
        postCreatedAt == other.postCreatedAt

private fun <T> List<T>.pageByDescendingTimestamp(
    pageRequest: PageRequest,
    key: (T) -> Pair<Long, String>,
): Page<T> {
    val cursor = pageRequest.cursor?.decodeStableCursor()
    val filtered = cursor?.let { current ->
        dropWhile { item ->
            val (millis, id) = key(item)
            millis > current.millis || (millis == current.millis && id <= current.id)
        }
    } ?: this
    val items = filtered.take(pageRequest.limit)
    val nextCursor = if (filtered.size > pageRequest.limit) items.lastOrNull()?.encodeCursor(key) else null
    return Page(items = items, nextCursor = nextCursor)
}

private fun <T> List<T>.pageByAscendingTimestamp(
    pageRequest: PageRequest,
    key: (T) -> Pair<Long, String>,
): Page<T> {
    val cursor = pageRequest.cursor?.decodeStableCursor()
    val filtered = cursor?.let { current ->
        dropWhile { item ->
            val (millis, id) = key(item)
            millis < current.millis || (millis == current.millis && id <= current.id)
        }
    } ?: this
    val items = filtered.take(pageRequest.limit)
    val nextCursor = if (filtered.size > pageRequest.limit) items.lastOrNull()?.encodeCursor(key) else null
    return Page(items = items, nextCursor = nextCursor)
}

private fun <T> T.encodeCursor(key: (T) -> Pair<Long, String>): Cursor {
    val (millis, id) = key(this)
    return Cursor("$millis|$id")
}

private fun Cursor.decodeStableCursor(): StableCursor? {
    val parts = value.split("|")
    if (parts.size != 2) return null
    return StableCursor(
        millis = parts[0].toLongOrNull() ?: return null,
        id = parts[1].takeIf { it.isNotBlank() } ?: return null,
    )
}
