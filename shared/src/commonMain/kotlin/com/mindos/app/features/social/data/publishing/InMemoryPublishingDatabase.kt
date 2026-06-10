package com.mindos.app.features.social.data.publishing

import com.mindos.app.features.social.domain.publishing.AuthorScaleTier
import com.mindos.app.features.social.domain.publishing.CanonicalPost
import com.mindos.app.features.social.domain.publishing.CommandIdempotencyRecord
import com.mindos.app.features.social.domain.publishing.AuthorFeedIndexEntry
import com.mindos.app.features.social.domain.publishing.EventId
import com.mindos.app.features.social.domain.publishing.EventEnvelope
import com.mindos.app.features.social.domain.publishing.EventSubscriberId
import com.mindos.app.features.social.domain.publishing.FanoutDeliveryReport
import com.mindos.app.features.social.domain.publishing.FanoutDistributionDecision
import com.mindos.app.features.social.domain.publishing.FanoutPolicyConfig
import com.mindos.app.features.social.domain.publishing.FanoutPostMetrics
import com.mindos.app.features.social.domain.publishing.FeedCandidate
import com.mindos.app.features.social.domain.publishing.FeedCandidateId
import com.mindos.app.features.social.domain.publishing.PostModerationDecision
import com.mindos.app.features.social.domain.publishing.FeedPostSnapshot
import com.mindos.app.features.social.domain.publishing.IdempotencyKey
import com.mindos.app.features.social.domain.publishing.OutboxMessage
import com.mindos.app.features.social.domain.publishing.OutboxMessageId
import com.mindos.app.features.social.domain.publishing.PostId
import com.mindos.app.features.social.domain.publishing.PublishingEvent
import com.mindos.app.features.social.domain.publishing.PublishingMetric
import com.mindos.app.features.social.domain.publishing.PublishingTraceEvent
import com.mindos.app.features.social.domain.publishing.PublishingTransactionRunner
import com.mindos.app.features.social.domain.publishing.RankedFeedCacheRecord
import com.mindos.app.features.social.domain.valueobject.RelationshipPair
import com.mindos.app.features.social.domain.valueobject.UserId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.mindos.app.features.social.shared.util.DomainResult

class InMemoryPublishingDatabase : PublishingTransactionRunner {
    internal val state = InMemoryPublishingState()
    private val mutex = Mutex()

    override suspend fun <T> read(block: suspend () -> T): T =
        mutex.withLock { block() }

    override suspend fun <T> write(block: suspend () -> T): T =
        mutex.withLock { block() }

    override suspend fun <T> writeResult(block: suspend () -> DomainResult<T>): DomainResult<T> =
        mutex.withLock {
            val snapshot = state.snapshot()
            try {
                when (val result = block()) {
                    is DomainResult.Success -> result
                    is DomainResult.Failure -> {
                        state.restore(snapshot)
                        result
                    }
                }
            } catch (throwable: Throwable) {
                state.restore(snapshot)
                throw throwable
            }
        }
}

internal class InMemoryPublishingState {
    val postsById: MutableMap<PostId, CanonicalPost> = mutableMapOf()
    val idempotencyByKey: MutableMap<IdempotencyKey, CommandIdempotencyRecord> = mutableMapOf()
    val outboxById: MutableMap<OutboxMessageId, OutboxMessage> = mutableMapOf()
    val outboxOrder: MutableList<OutboxMessageId> = mutableListOf()
    val feedCandidatesByViewer: MutableMap<UserId, MutableMap<FeedCandidateId, FeedCandidate>> = mutableMapOf()
    val feedPostSnapshotsById: MutableMap<PostId, FeedPostSnapshot> = mutableMapOf()
    val authorFeedIndexByPostId: MutableMap<PostId, AuthorFeedIndexEntry> = mutableMapOf()
    var fanoutPolicyConfig: FanoutPolicyConfig = FanoutPolicyConfig()
    val fanoutDecisionsByPostId: MutableMap<PostId, FanoutDistributionDecision> = mutableMapOf()
    val fanoutPostMetricsByPostId: MutableMap<PostId, FanoutPostMetrics> = mutableMapOf()
    val fanoutReportsByPostId: MutableMap<PostId, MutableList<FanoutDeliveryReport>> = mutableMapOf()
    val feedMutesByPair: MutableMap<RelationshipPair, com.mindos.app.features.social.domain.valueobject.Timestamp> = mutableMapOf()
    val moderationByPostId: MutableMap<PostId, PostModerationDecision> = mutableMapOf()
    val rankedFeedCacheByViewer: MutableMap<UserId, RankedFeedCacheRecord> = mutableMapOf()
    val authorScaleByUser: MutableMap<UserId, AuthorScaleTier> = mutableMapOf()
    val eventsById: MutableMap<EventId, EventEnvelope> = mutableMapOf()
    val eventJournal: MutableList<EventEnvelope> = mutableListOf()
    val deliveredEventsBySubscriber: MutableSet<EventDeliveryKey> = mutableSetOf()
    val deliveryAttemptsBySubscriber: MutableMap<EventDeliveryKey, Int> = mutableMapOf()
    var nextEventOffset: Long = 1L
    val metrics: MutableList<PublishingMetric> = mutableListOf()
    val traces: MutableList<PublishingTraceEvent> = mutableListOf()

    fun snapshot(): InMemoryPublishingSnapshot =
        InMemoryPublishingSnapshot(
            postsById = postsById.toMutableMap(),
            idempotencyByKey = idempotencyByKey.toMutableMap(),
            outboxById = outboxById.toMutableMap(),
            outboxOrder = outboxOrder.toMutableList(),
            feedCandidatesByViewer = feedCandidatesByViewer
                .mapValues { (_, candidates) -> candidates.toMutableMap() }
                .toMutableMap(),
            feedPostSnapshotsById = feedPostSnapshotsById.toMutableMap(),
            authorFeedIndexByPostId = authorFeedIndexByPostId.toMutableMap(),
            fanoutPolicyConfig = fanoutPolicyConfig,
            fanoutDecisionsByPostId = fanoutDecisionsByPostId.toMutableMap(),
            fanoutPostMetricsByPostId = fanoutPostMetricsByPostId.toMutableMap(),
            fanoutReportsByPostId = fanoutReportsByPostId
                .mapValues { (_, reports) -> reports.toMutableList() }
                .toMutableMap(),
            feedMutesByPair = feedMutesByPair.toMutableMap(),
            moderationByPostId = moderationByPostId.toMutableMap(),
            rankedFeedCacheByViewer = rankedFeedCacheByViewer.toMutableMap(),
            authorScaleByUser = authorScaleByUser.toMutableMap(),
            eventsById = eventsById.toMutableMap(),
            eventJournal = eventJournal.toMutableList(),
            deliveredEventsBySubscriber = deliveredEventsBySubscriber.toMutableSet(),
            deliveryAttemptsBySubscriber = deliveryAttemptsBySubscriber.toMutableMap(),
            nextEventOffset = nextEventOffset,
            metrics = metrics.toMutableList(),
            traces = traces.toMutableList(),
        )

    fun restore(snapshot: InMemoryPublishingSnapshot) {
        postsById.replaceWith(snapshot.postsById)
        idempotencyByKey.replaceWith(snapshot.idempotencyByKey)
        outboxById.replaceWith(snapshot.outboxById)
        outboxOrder.replaceWith(snapshot.outboxOrder)
        feedCandidatesByViewer.replaceWith(snapshot.feedCandidatesByViewer)
        feedPostSnapshotsById.replaceWith(snapshot.feedPostSnapshotsById)
        authorFeedIndexByPostId.replaceWith(snapshot.authorFeedIndexByPostId)
        fanoutPolicyConfig = snapshot.fanoutPolicyConfig
        fanoutDecisionsByPostId.replaceWith(snapshot.fanoutDecisionsByPostId)
        fanoutPostMetricsByPostId.replaceWith(snapshot.fanoutPostMetricsByPostId)
        fanoutReportsByPostId.replaceWith(snapshot.fanoutReportsByPostId)
        feedMutesByPair.replaceWith(snapshot.feedMutesByPair)
        moderationByPostId.replaceWith(snapshot.moderationByPostId)
        rankedFeedCacheByViewer.replaceWith(snapshot.rankedFeedCacheByViewer)
        authorScaleByUser.replaceWith(snapshot.authorScaleByUser)
        eventsById.replaceWith(snapshot.eventsById)
        eventJournal.replaceWith(snapshot.eventJournal)
        deliveredEventsBySubscriber.replaceWith(snapshot.deliveredEventsBySubscriber)
        deliveryAttemptsBySubscriber.replaceWith(snapshot.deliveryAttemptsBySubscriber)
        nextEventOffset = snapshot.nextEventOffset
        metrics.replaceWith(snapshot.metrics)
        traces.replaceWith(snapshot.traces)
    }
}

internal data class EventDeliveryKey(
    val subscriberId: EventSubscriberId,
    val eventId: EventId,
)

internal data class InMemoryPublishingSnapshot(
    val postsById: MutableMap<PostId, CanonicalPost>,
    val idempotencyByKey: MutableMap<com.mindos.app.features.social.domain.publishing.IdempotencyKey, CommandIdempotencyRecord>,
    val outboxById: MutableMap<OutboxMessageId, OutboxMessage>,
    val outboxOrder: MutableList<OutboxMessageId>,
    val feedCandidatesByViewer: MutableMap<UserId, MutableMap<FeedCandidateId, FeedCandidate>>,
    val feedPostSnapshotsById: MutableMap<PostId, FeedPostSnapshot>,
    val authorFeedIndexByPostId: MutableMap<PostId, AuthorFeedIndexEntry>,
    val fanoutPolicyConfig: FanoutPolicyConfig,
    val fanoutDecisionsByPostId: MutableMap<PostId, FanoutDistributionDecision>,
    val fanoutPostMetricsByPostId: MutableMap<PostId, FanoutPostMetrics>,
    val fanoutReportsByPostId: MutableMap<PostId, MutableList<FanoutDeliveryReport>>,
    val feedMutesByPair: MutableMap<RelationshipPair, com.mindos.app.features.social.domain.valueobject.Timestamp>,
    val moderationByPostId: MutableMap<PostId, PostModerationDecision>,
    val rankedFeedCacheByViewer: MutableMap<UserId, RankedFeedCacheRecord>,
    val authorScaleByUser: MutableMap<UserId, AuthorScaleTier>,
    val eventsById: MutableMap<EventId, EventEnvelope>,
    val eventJournal: MutableList<EventEnvelope>,
    val deliveredEventsBySubscriber: MutableSet<EventDeliveryKey>,
    val deliveryAttemptsBySubscriber: MutableMap<EventDeliveryKey, Int>,
    val nextEventOffset: Long,
    val metrics: MutableList<PublishingMetric>,
    val traces: MutableList<PublishingTraceEvent>,
)

private fun <K, V> MutableMap<K, V>.replaceWith(values: Map<K, V>) {
    clear()
    putAll(values)
}

private fun <T> MutableList<T>.replaceWith(values: List<T>) {
    clear()
    addAll(values)
}

private fun <T> MutableSet<T>.replaceWith(values: Set<T>) {
    clear()
    addAll(values)
}
