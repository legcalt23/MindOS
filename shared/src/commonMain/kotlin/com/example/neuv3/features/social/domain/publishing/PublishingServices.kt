package com.example.neuv3.features.social.domain.publishing

import com.example.neuv3.features.social.domain.model.ProfileVisibility
import com.example.neuv3.features.social.domain.repository.BlockRepository
import com.example.neuv3.features.social.domain.repository.FollowRepository
import com.example.neuv3.features.social.domain.repository.PrivacySettingsRepository
import com.example.neuv3.features.social.domain.repository.UserProfileRepository
import com.example.neuv3.features.social.domain.valueobject.Cursor
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.domain.valueobject.UserId
import com.example.neuv3.features.social.shared.util.AppClock
import com.example.neuv3.features.social.shared.util.DomainResult
import com.example.neuv3.features.social.shared.util.PageRequest
import kotlin.random.Random

data class PublishingDependencies(
    val transactions: PublishingTransactionRunner,
    val posts: CanonicalPostRepository,
    val idempotency: CommandIdempotencyRepository,
    val outbox: OutboxRepository,
    val feedInbox: FeedCandidateInboxRepository,
    val feedPosts: FeedPostSnapshotRepository,
    val authorFeedIndex: AuthorFeedIndexRepository,
    val fanoutConfig: FanoutPolicyConfigRepository,
    val fanoutDecisions: FanoutDistributionDecisionRepository,
    val fanoutPostMetrics: FanoutPostMetricsRepository,
    val fanoutReports: FanoutDeliveryReportRepository,
    val feedMutes: FeedMuteRepository,
    val moderation: FeedModerationRepository,
    val rankedFeedCache: RankedFeedCacheRepository,
    val eventBackbone: PublishingEventBackbone,
    val authorScale: AuthorScaleRepository,
    val audience: AudienceRepository,
    val profiles: UserProfileRepository,
    val privacy: PrivacySettingsRepository,
    val follows: FollowRepository,
    val blocks: BlockRepository,
    val clock: AppClock,
    val postIds: PostIdGenerator,
    val eventIds: EventIdGenerator,
    val outboxIds: OutboxMessageIdGenerator,
    val idempotencyKeys: IdempotencyKeyGenerator,
    val traceContexts: TraceContextFactory,
    val metrics: PublishingMetricsRecorder,
    val traceSink: PublishingTraceSink,
)

class SequentialPostIdGenerator(
    private val prefix: String = "post",
) : PostIdGenerator {
    private var next = 1L

    override fun nextPostId(actorId: UserId, idempotencyKey: IdempotencyKey): PostId =
        PostId("${prefix}_${actorId.value}_${next++}")
}

class StablePostIdGenerator(
    private val prefix: String = "post",
) : PostIdGenerator {
    override fun nextPostId(actorId: UserId, idempotencyKey: IdempotencyKey): PostId {
        val actor = actorId.value.sanitizeIdentifier(maxLength = 32)
        val keyHash = stableHash(idempotencyKey.value)
        return PostId("${prefix}_${actor}_$keyHash")
    }
}

class SequentialPublishingEventIdGenerator(
    private val prefix: String = "event",
) : EventIdGenerator {
    private var next = 1L

    override fun nextEventId(eventName: String): EventId =
        EventId("${prefix}_${eventName.sanitizeEventName()}_${next++}")
}

class SequentialOutboxMessageIdGenerator(
    private val prefix: String = "outbox",
) : OutboxMessageIdGenerator {
    private var next = 1L

    override fun nextOutboxMessageId(postId: PostId, event: PublishingEvent): OutboxMessageId =
        OutboxMessageId("${prefix}_${postId.value}_${event.eventName.sanitizeEventName()}_${next++}")
}

class SequentialIdempotencyKeyGenerator(
    private val prefix: String = "publish",
) : IdempotencyKeyGenerator {
    private var next = 1L

    override fun nextIdempotencyKey(): IdempotencyKey =
        IdempotencyKey("${prefix}_${next++}")
}

class RandomIdempotencyKeyGenerator(
    private val prefix: String = "publish",
) : IdempotencyKeyGenerator {
    override fun nextIdempotencyKey(): IdempotencyKey =
        IdempotencyKey("${prefix}_${Random.nextLong().toULong().toString(36)}_${Random.nextLong().toULong().toString(36)}")
}

class SequentialTraceContextFactory(
    private val requestPrefix: String = "request",
    private val tracePrefix: String = "trace",
) : TraceContextFactory {
    private var next = 1L

    override fun nextTraceContext(operation: String): TraceContext {
        val current = next++
        val normalized = operation.lowercase().filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "op" }
        return TraceContext(
            traceId = TraceId("${tracePrefix}_${normalized}_$current"),
            requestId = RequestId("${requestPrefix}_${normalized}_$current"),
        )
    }
}

class RandomTraceContextFactory(
    private val requestPrefix: String = "request",
    private val tracePrefix: String = "trace",
) : TraceContextFactory {
    override fun nextTraceContext(operation: String): TraceContext {
        val normalized = operation.lowercase().filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "op" }
        val suffix = "${Random.nextLong().toULong().toString(36)}_${Random.nextLong().toULong().toString(36)}"
        return TraceContext(
            traceId = TraceId("${tracePrefix}_${normalized}_$suffix"),
            requestId = RequestId("${requestPrefix}_${normalized}_$suffix"),
        )
    }
}

class LocalEchoPublishingMediaUploader(
    private val clock: AppClock,
) : PublishingMediaUploader {
    override suspend fun upload(
        actorId: UserId,
        draftId: String,
        idempotencyKey: IdempotencyKey,
        media: List<DraftMediaAttachment>,
        trace: TraceContext,
    ): DomainResult<List<PostMediaAttachment>> {
        val uploadedAt = clock.now()
        return DomainResult.success(
            media.map { attachment ->
                PostMediaAttachment(
                    clientMediaId = attachment.clientMediaId,
                    remoteUri = "local-upload://${actorId.value}/$draftId/${idempotencyKey.value}/${attachment.clientMediaId}",
                    mimeType = attachment.mimeType,
                    sizeBytes = attachment.sizeBytes,
                    uploadedAt = uploadedAt,
                )
            },
        )
    }
}

class FeedEligibilityService(
    private val profiles: UserProfileRepository,
    private val privacy: PrivacySettingsRepository,
    private val follows: FollowRepository,
    private val blocks: BlockRepository,
    private val mutes: FeedMuteRepository,
    private val moderation: FeedModerationRepository,
) {
    suspend fun canReceiveCandidate(viewerId: UserId, post: CanonicalPost): Boolean =
        evaluate(viewerId = viewerId, snapshot = post.toFeedPostSnapshot(), requirePublished = false).accepted

    suspend fun canSeePublishedPost(viewerId: UserId, post: CanonicalPost): Boolean =
        evaluate(viewerId = viewerId, snapshot = post.toFeedPostSnapshot(), requirePublished = true).accepted

    suspend fun canServeCandidate(viewerId: UserId, snapshot: FeedPostSnapshot): Boolean =
        evaluate(viewerId = viewerId, snapshot = snapshot, requirePublished = true).accepted

    suspend fun evaluateServingCandidate(viewerId: UserId, snapshot: FeedPostSnapshot): FeedEligibilityDecision =
        evaluate(viewerId = viewerId, snapshot = snapshot, requirePublished = true)

    private suspend fun evaluate(
        viewerId: UserId,
        snapshot: FeedPostSnapshot,
        requirePublished: Boolean,
    ): FeedEligibilityDecision {
        val account = profiles.getAccount(snapshot.authorId)
        if (account?.canParticipateInSocialGraph != true) {
            return FeedEligibilityDecision.rejected(FeedEligibilityRejectionReason.AuthorUnavailable)
        }
        if (requirePublished && !snapshot.isPublished) {
            return FeedEligibilityDecision.rejected(FeedEligibilityRejectionReason.NotPublished)
        }
        if (!requirePublished && snapshot.publicationState == PublicationState.Rejected) {
            return FeedEligibilityDecision.rejected(FeedEligibilityRejectionReason.NotPublished)
        }
        val modState = moderation.get(snapshot.postId)?.state
        if (modState == PostModerationState.Removed || modState == PostModerationState.Hidden) {
            return FeedEligibilityDecision.rejected(FeedEligibilityRejectionReason.Moderated)
        }
        if (viewerId == snapshot.authorId) return FeedEligibilityDecision.Accepted
        if (blocks.existsAnyDirection(viewerId, snapshot.authorId)) {
            return FeedEligibilityDecision.rejected(FeedEligibilityRejectionReason.Blocked)
        }
        if (mutes.isMuted(viewerId, snapshot.authorId)) {
            return FeedEligibilityDecision.rejected(FeedEligibilityRejectionReason.Muted)
        }
        return when (snapshot.visibility) {
            PostVisibility.Public -> FeedEligibilityDecision.Accepted
            PostVisibility.Followers -> {
                val settings = privacy.get(snapshot.authorId)
                val profileAllowsFollowerSurface = settings?.profileVisibility != ProfileVisibility.Private ||
                    follows.exists(viewerId, snapshot.authorId)
                if (profileAllowsFollowerSurface && follows.exists(viewerId, snapshot.authorId)) {
                    FeedEligibilityDecision.Accepted
                } else {
                    FeedEligibilityDecision.rejected(FeedEligibilityRejectionReason.Privacy)
                }
            }
        }
    }
}

class FanoutDistributionPolicy(
    private val dependencies: PublishingDependencies,
) {
    suspend fun decideForPost(
        post: CanonicalPost,
        trace: TraceContext = post.trace,
        forcedMode: FanoutDistributionMode? = null,
        useExistingDecision: Boolean = forcedMode == null,
    ): FanoutDistributionDecision {
        if (useExistingDecision) {
            dependencies.fanoutDecisions.get(post.id)?.let { return it }
        }

        val config = dependencies.fanoutConfig.get()
        val authorMetrics = FanoutAuthorMetrics(
            followerCount = dependencies.audience.countFollowers(post.authorId),
            authorScaleTier = dependencies.authorScale.getTier(post.authorId),
        )
        val postMetrics = dependencies.fanoutPostMetrics.get(post.id)
        val reason = when {
            forcedMode != null -> FanoutDecisionReason.ForcedMigration
            authorMetrics.authorScaleTier == AuthorScaleTier.HighScale -> FanoutDecisionReason.AuthorHighScaleTier
            postMetrics != null && postMetrics.viralScore >= config.viralScoreThresholdForFanoutOnRead -> {
                FanoutDecisionReason.ViralPost
            }
            authorMetrics.followerCount > config.maxFollowersForFanoutOnWrite -> FanoutDecisionReason.FollowerThresholdExceeded
            else -> FanoutDecisionReason.NormalAuthorBelowThreshold
        }
        val mode = forcedMode ?: when (reason) {
            FanoutDecisionReason.NormalAuthorBelowThreshold -> FanoutDistributionMode.FanoutOnWrite
            FanoutDecisionReason.AuthorHighScaleTier,
            FanoutDecisionReason.FollowerThresholdExceeded,
            FanoutDecisionReason.ViralPost,
            FanoutDecisionReason.ForcedMigration -> FanoutDistributionMode.FanoutOnRead
        }
        val decision = FanoutDistributionDecision(
            postId = post.id,
            authorId = post.authorId,
            mode = mode,
            reason = reason,
            authorMetrics = authorMetrics,
            postMetrics = postMetrics,
            config = config,
            decidedAt = dependencies.clock.now(),
            trace = trace.withPostId(post.id),
        )
        dependencies.fanoutDecisions.upsert(decision)
        dependencies.metrics.increment(
            name = "fanout.decision.selected",
            tags = decision.trace.tags() + decision.metricTags(),
        )
        dependencies.traceSink.record(
            span = "fanout.decision.selected",
            trace = decision.trace,
            attributes = decision.metricTags() + mapOf(
                "followers" to authorMetrics.followerCount.toString(),
                "viralScore" to (postMetrics?.viralScore ?: 0L).toString(),
                "writeFollowerLimit" to config.maxFollowersForFanoutOnWrite.toString(),
                "viralThreshold" to config.viralScoreThresholdForFanoutOnRead.toString(),
            ),
        )
        return decision
    }

    private fun FanoutDistributionDecision.metricTags(): Map<String, String> =
        mapOf(
            "mode" to mode.name,
            "reason" to reason.name,
            "tier" to authorMetrics.authorScaleTier.name,
        )
}

private fun String.sanitizeIdentifier(maxLength: Int): String =
    lowercase()
        .map { char -> if (char.isLetterOrDigit() || char == '_' || char == '-') char else '_' }
        .joinToString("")
        .ifBlank { "unknown" }
        .take(maxLength)

private fun stableHash(value: String): String {
    var hash = 0xcbf29ce484222325UL
    val prime = 0x100000001b3UL
    value.encodeToByteArray().forEach { byte ->
        hash = hash xor byte.toUByte().toULong()
        hash *= prime
    }
    return hash.toString(36)
}

fun deterministicPublishingEventId(eventName: String, vararg parts: String): EventId =
    EventId(
        buildString {
            append(eventName)
            parts.forEach { part ->
                append(":")
                append(part.sanitizeDeterministicEventPart())
            }
        },
    )

private fun String.sanitizeEventName(): String =
    map { char -> if (char.isLetterOrDigit()) char else '_' }
        .joinToString("")
        .ifBlank { "event" }

private fun String.sanitizeDeterministicEventPart(): String =
    map { char -> if (char.isLetterOrDigit() || char == '_' || char == '-' || char == ':') char else '_' }
        .joinToString("")
        .ifBlank { "unknown" }

class CandidateGenerationService(
    private val dependencies: PublishingDependencies,
    private val eligibility: FeedEligibilityService,
) {
    suspend fun generateWriteCandidates(
        post: CanonicalPost,
        decision: FanoutDistributionDecision,
    ): DomainResult<List<FeedCandidate>> {
        val config = dependencies.fanoutConfig.get()
        val now = dependencies.clock.now()
        val viewers = mutableSetOf(post.authorId)

        if (decision.mode == FanoutDistributionMode.FanoutOnWrite) {
            var cursor: Cursor? = null
            do {
                if (viewers.size >= config.maxFollowersForFanoutOnWrite) break
                val page = dependencies.audience.listFollowers(
                    post.authorId,
                    PageRequest(limit = config.graphPageSize, cursor = cursor),
                )
                viewers += page.items
                cursor = page.nextCursor
            } while (cursor != null)
        }

        val candidates = viewers.mapNotNull { viewerId ->
            if (!eligibility.canReceiveCandidate(viewerId, post)) return@mapNotNull null
            FeedCandidate(
                id = FeedCandidateId("${viewerId.value}:${post.id.value}:${decision.mode.idSuffix()}"),
                viewerId = viewerId,
                postId = post.id,
                authorId = post.authorId,
                source = decision.mode.toCandidateSource(),
                origin = if (viewerId == post.authorId) CandidateOrigin.AuthorSelf else CandidateOrigin.FollowGraph,
                distributionMode = decision.mode,
                postCreatedAt = post.createdAt,
                generatedAt = now,
                trace = post.trace,
            )
        }

        dependencies.metrics.increment(
            name = "feed.candidates.generated",
            tags = post.trace.tags() + mapOf(
                "source" to decision.mode.toCandidateSource().name,
                "mode" to decision.mode.name,
                "reason" to decision.reason.name,
                "tier" to decision.authorMetrics.authorScaleTier.name,
            ),
            value = candidates.size.toLong(),
        )
        return DomainResult.success(candidates)
    }

    suspend fun generateReadCandidates(viewerId: UserId, trace: TraceContext): DomainResult<List<FeedCandidate>> {
        val config = dependencies.fanoutConfig.get()
        val followedAuthors = mutableSetOf<UserId>()
        var followingCursor: Cursor? = null
        do {
            val following = dependencies.audience.listFollowing(
                viewerId,
                PageRequest(limit = config.graphPageSize, cursor = followingCursor),
            )
            followedAuthors += following.items
            followingCursor = following.nextCursor
        } while (followingCursor != null)

        if (followedAuthors.isEmpty()) {
            return DomainResult.success(emptyList())
        }

        val entries = mutableListOf<AuthorFeedIndexEntry>()
        var postCursor: Cursor? = null
        var remaining = config.readCandidateLookbackPosts
        do {
            val page = dependencies.authorFeedIndex.listPublishedByAuthors(
                authorIds = followedAuthors,
                pageRequest = PageRequest(limit = remaining.coerceAtMost(200), cursor = postCursor),
            )
            entries += page.items
            remaining -= page.items.size
            postCursor = page.nextCursor
        } while (postCursor != null && remaining > 0)

        val now = dependencies.clock.now()
        val snapshots = dependencies.feedPosts.getMany(entries.map { it.postId })
        val candidates = entries.mapNotNull { entry ->
            val decision = dependencies.fanoutDecisions.get(entry.postId)
            if (decision?.mode != FanoutDistributionMode.FanoutOnRead) return@mapNotNull null
            val snapshot = snapshots[entry.postId] ?: return@mapNotNull null
            if (!eligibility.canServeCandidate(viewerId, snapshot)) return@mapNotNull null
            FeedCandidate(
                id = FeedCandidateId("${viewerId.value}:${entry.postId.value}:${FanoutDistributionMode.FanoutOnRead.idSuffix()}"),
                viewerId = viewerId,
                postId = entry.postId,
                authorId = entry.authorId,
                source = CandidateSource.FanoutOnRead,
                origin = CandidateOrigin.FollowGraph,
                distributionMode = FanoutDistributionMode.FanoutOnRead,
                postCreatedAt = entry.postCreatedAt,
                generatedAt = now,
                trace = trace.withPostId(entry.postId),
            )
        }

        dependencies.metrics.increment(
            name = "feed.candidates.generated",
            tags = trace.tags() + mapOf(
                "source" to CandidateSource.FanoutOnRead.name,
                "mode" to FanoutDistributionMode.FanoutOnRead.name,
            ),
            value = candidates.size.toLong(),
        )
        return DomainResult.success(candidates)
    }

    private fun FanoutDistributionMode.toCandidateSource(): CandidateSource =
        when (this) {
            FanoutDistributionMode.FanoutOnWrite -> CandidateSource.FanoutOnWrite
            FanoutDistributionMode.FanoutOnRead -> CandidateSource.FanoutOnRead
        }

    private fun FanoutDistributionMode.idSuffix(): String =
        when (this) {
            FanoutDistributionMode.FanoutOnWrite -> "write"
            FanoutDistributionMode.FanoutOnRead -> "read"
        }
}

class CandidateInjectionPipeline(
    private val dependencies: PublishingDependencies,
    private val candidateGeneration: CandidateGenerationService,
    private val fanoutPolicy: FanoutDistributionPolicy,
) {
    suspend fun indexPostSnapshot(post: CanonicalPost): Boolean {
        val snapshot = post.toFeedPostSnapshot()
        val changed = dependencies.feedPosts.upsert(snapshot)
        if (snapshot.isPublished) {
            dependencies.authorFeedIndex.upsert(snapshot.toAuthorFeedIndexEntry(dependencies.clock.now()))
        }
        return changed
    }

    suspend fun injectWriteCandidates(post: CanonicalPost): DomainResult<FanoutDeliveryReport> {
        val startedAt = dependencies.clock.now()
        val decision = fanoutPolicy.decideForPost(post)
        when (val published = dependencies.eventBackbone.publish(
            FanoutRequestedEvent(
                id = deterministicPublishingEventId("fanout-requested", post.id.value, decision.mode.name),
                postId = post.id,
                authorId = post.authorId,
                mode = decision.mode,
                reason = decision.reason,
                followerCount = decision.authorMetrics.followerCount,
                occurredAt = startedAt,
                trace = decision.trace,
            ),
        )) {
            is DomainResult.Success -> Unit
            is DomainResult.Failure -> return published
        }
        val candidates = when (val generated = candidateGeneration.generateWriteCandidates(post, decision)) {
            is DomainResult.Success -> generated.value
            is DomainResult.Failure -> {
                val report = recordFanoutReport(
                    post = post,
                    decision = decision,
                    status = FanoutDeliveryStatus.Failed,
                    generatedCandidates = 0,
                    materializedCandidates = 0,
                    startedAt = startedAt,
                    errorMessage = generated.error.message,
                )
                val durationMs = dependencies.clock.now().epochMillis - startedAt.epochMillis
                dependencies.metrics.recordDistribution(
                    MetricName.FanoutLag,
                    durationMs,
                    DefaultMetricTags(stage = Stage.Fanout, outcome = Outcome.Failed, distributionMode = decision.mode.name)
                )
                dependencies.traceSink.record(
                    StructuredLogEvent(
                        traceId = report.trace.traceId.value,
                        requestId = report.trace.requestId.value,
                        postId = post.id.value,
                        authorId = post.authorId.value,
                        event = "fanout_completed",
                        stage = Stage.Fanout.name,
                        result = Outcome.Failed.name,
                        errorCategory = ErrorCategory.INTERNAL.name,
                        durationMs = durationMs
                    )
                )
                return generated
            }
        }
        val materialized = dependencies.feedInbox.upsertAll(candidates)
        for (candidate in candidates) {
            when (val published = dependencies.eventBackbone.publish(candidate.toGeneratedEvent(dependencies.clock.now()))) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> return published
            }
        }
        dependencies.rankedFeedCache.invalidateAll(candidates.map { it.viewerId })
        dependencies.metrics.increment(
            name = "feed.candidate_inbox.injected",
            tags = post.trace.tags() + mapOf(
                "source" to decision.mode.toCandidateSource().name,
                "mode" to decision.mode.name,
                "reason" to decision.reason.name,
            ),
            value = materialized.toLong(),
        )
        val report = recordFanoutReport(
            post = post,
            decision = decision,
            status = FanoutDeliveryStatus.Succeeded,
            generatedCandidates = candidates.size,
            materializedCandidates = materialized,
            startedAt = startedAt,
        )
        val durationMs = dependencies.clock.now().epochMillis - startedAt.epochMillis
        dependencies.metrics.recordDistribution(
            MetricName.FanoutLag,
            durationMs,
            DefaultMetricTags(stage = Stage.Fanout, outcome = Outcome.Succeeded, distributionMode = decision.mode.name)
        )
        dependencies.traceSink.record(
            StructuredLogEvent(
                traceId = report.trace.traceId.value,
                requestId = report.trace.requestId.value,
                postId = post.id.value,
                authorId = post.authorId.value,
                event = "fanout_completed",
                stage = Stage.Fanout.name,
                result = Outcome.Succeeded.name,
                durationMs = durationMs,
                generatedCandidates = candidates.size,
                materializedCandidates = materialized
            )
        )
        return DomainResult.success(report)
    }

    suspend fun injectReadCandidates(viewerId: UserId, trace: TraceContext): DomainResult<Int> {
        val candidates = when (val generated = candidateGeneration.generateReadCandidates(viewerId, trace)) {
            is DomainResult.Success -> generated.value
            is DomainResult.Failure -> return generated
        }
        if (candidates.isEmpty()) return DomainResult.success(0)
        val materialized = dependencies.feedInbox.upsertAll(candidates)
        for (candidate in candidates) {
            when (val published = dependencies.eventBackbone.publish(candidate.toGeneratedEvent(dependencies.clock.now()))) {
                is DomainResult.Success -> Unit
                is DomainResult.Failure -> return published
            }
        }
        if (materialized > 0) {
            dependencies.rankedFeedCache.invalidate(viewerId)
        }
        dependencies.metrics.increment(
            name = "feed.candidate_inbox.injected",
            tags = trace.tags() + mapOf("source" to CandidateSource.FanoutOnRead.name),
            value = materialized.toLong(),
        )
        return DomainResult.success(materialized)
    }

    private suspend fun recordFanoutReport(
        post: CanonicalPost,
        decision: FanoutDistributionDecision,
        status: FanoutDeliveryStatus,
        generatedCandidates: Int,
        materializedCandidates: Int,
        startedAt: com.example.neuv3.features.social.domain.valueobject.Timestamp,
        errorMessage: String? = null,
    ): FanoutDeliveryReport {
        val latest = dependencies.fanoutReports.latest(post.id)
        val skippedFollowers = when (decision.mode) {
            FanoutDistributionMode.FanoutOnRead -> decision.authorMetrics.followerCount
            FanoutDistributionMode.FanoutOnWrite -> {
                val followerCandidates = generatedCandidates - 1
                (decision.authorMetrics.followerCount - followerCandidates).coerceAtLeast(0)
            }
        }
        val report = FanoutDeliveryReport(
            postId = post.id,
            authorId = post.authorId,
            mode = decision.mode,
            reason = decision.reason,
            status = status,
            generatedCandidates = generatedCandidates,
            materializedCandidates = materializedCandidates,
            skippedFollowers = skippedFollowers,
            attempt = (latest?.attempt ?: 0) + 1,
            startedAt = startedAt,
            finishedAt = dependencies.clock.now(),
            trace = decision.trace,
            errorMessage = errorMessage,
        )
        dependencies.fanoutReports.append(report)
        val outcome = if (status == FanoutDeliveryStatus.Succeeded) Outcome.Succeeded else Outcome.Failed
        dependencies.metrics.increment(
            MetricName.FanoutCompleted,
            DefaultMetricTags(stage = Stage.Fanout, outcome = outcome, distributionMode = decision.mode.name),
            value = materializedCandidates.toLong(),
        )
        return report
    }

    private fun FanoutDeliveryReport.metricTags(): Map<String, String> =
        mapOf(
            "mode" to mode.name,
            "reason" to reason.name,
            "status" to status.name,
            "attempt" to attempt.toString(),
        )

    private fun FanoutDistributionMode.toCandidateSource(): CandidateSource =
        when (this) {
            FanoutDistributionMode.FanoutOnWrite -> CandidateSource.FanoutOnWrite
            FanoutDistributionMode.FanoutOnRead -> CandidateSource.FanoutOnRead
        }

    private fun FeedCandidate.toGeneratedEvent(
        occurredAt: com.example.neuv3.features.social.domain.valueobject.Timestamp,
    ): FeedCandidateGeneratedEvent =
        FeedCandidateGeneratedEvent(
            id = deterministicPublishingEventId("feed-candidate-generated", id.value),
            candidateId = id,
            viewerId = viewerId,
            postId = postId,
            authorId = authorId,
            source = source,
            distributionMode = distributionMode,
            occurredAt = occurredAt,
            trace = trace,
        )
}

interface FeedFeatureEnrichmentPlane {
    suspend fun enrich(
        viewerId: UserId,
        candidates: List<FeedCandidate>,
        snapshotsByPostId: Map<PostId, FeedPostSnapshot>,
        trace: TraceContext,
    ): List<EnrichedFeedCandidate>
}

class DefaultFeedFeatureEnrichmentPlane(
    private val dependencies: PublishingDependencies,
) : FeedFeatureEnrichmentPlane {
    override suspend fun enrich(
        viewerId: UserId,
        candidates: List<FeedCandidate>,
        snapshotsByPostId: Map<PostId, FeedPostSnapshot>,
        trace: TraceContext,
    ): List<EnrichedFeedCandidate> {
        val now = dependencies.clock.now()
        val authorIds = candidates.map { it.authorId }.toSet()
        val followsByAuthor = authorIds.associateWith { authorId ->
            viewerId != authorId && dependencies.follows.exists(viewerId, authorId)
        }
        val tierByAuthor = authorIds.associateWith { authorId ->
            dependencies.authorScale.getTier(authorId)
        }
        val moderationByPost = candidates.associate { candidate ->
            candidate.postId to dependencies.moderation.get(candidate.postId)
        }

        val enriched = candidates.mapNotNull { candidate ->
            val snapshot = snapshotsByPostId[candidate.postId] ?: return@mapNotNull null
            val ageMillis = (now.epochMillis - snapshot.createdAt.epochMillis).coerceAtLeast(0)
            EnrichedFeedCandidate(
                candidate = candidate,
                postSnapshot = snapshot,
                features = FeedCandidateFeatures(
                    viewerFollowsAuthor = followsByAuthor[candidate.authorId] == true,
                    viewerIsAuthor = viewerId == candidate.authorId,
                    authorScaleTier = tierByAuthor[candidate.authorId] ?: AuthorScaleTier.Normal,
                    sourcePlane = candidate.source.plane,
                    sourceWeight = candidate.source.baseSourceWeight(),
                    hasMedia = snapshot.mediaAttachments.isNotEmpty(),
                    textLength = snapshot.text.value.length,
                    ageMillis = ageMillis,
                    moderationPenalty = when (moderationByPost[candidate.postId]?.state) {
                        PostModerationState.PublishedLimitedDistribution -> LimitedModerationPenalty
                        PostModerationState.PublishedPendingReview -> -50.0
                        PostModerationState.Demoted -> -500.0
                        else -> 0.0
                    },
                ),
            )
        }
        dependencies.metrics.increment("feed.features.enriched", trace.tags(), enriched.size.toLong())
        return enriched
    }

    private fun CandidateSource.baseSourceWeight(): Double =
        when (this) {
            CandidateSource.FanoutOnWrite -> 140.0
            CandidateSource.FanoutOnRead -> 110.0
            CandidateSource.Recommendation -> 70.0
            CandidateSource.Exploration -> 45.0
        }

    private companion object {
        const val LimitedModerationPenalty = -250.0
    }
}

interface FeedRankingPlane {
    suspend fun rank(
        viewerId: UserId,
        candidates: List<EnrichedFeedCandidate>,
        trace: TraceContext,
    ): List<RankedFeedCandidate>
}

class DefaultFeedRankingPlane(
    private val rankingEngine: com.example.neuv3.features.social.domain.ranking.HomeFeedRankingEngine,
    private val clock: AppClock,
    private val metrics: PublishingMetricsRecorder,
) : FeedRankingPlane {
    override suspend fun rank(
        viewerId: UserId,
        candidates: List<EnrichedFeedCandidate>,
        trace: TraceContext,
    ): List<RankedFeedCandidate> {
        val now = clock.now()
        val ranked = rankingEngine.rankCandidates(candidates, now)
        metrics.increment("feed.ranking.ranked", trace.tags(), ranked.size.toLong())
        return ranked
    }
}
