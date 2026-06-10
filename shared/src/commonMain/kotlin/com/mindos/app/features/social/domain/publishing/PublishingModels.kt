package com.mindos.app.features.social.domain.publishing

import com.mindos.app.features.social.domain.error.IntegrityViolation
import com.mindos.app.features.social.domain.valueobject.Timestamp
import com.mindos.app.features.social.domain.valueobject.UserId
import com.mindos.app.features.social.shared.util.DomainResult
import kotlin.math.roundToLong

enum class PublisherState {
    Idle,
    EditingDraft,
    Validating,
    UploadingMedia,
    ReadyToPublish,
    PublishingCommandSent,
    AwaitingCommit,
    PublishedAccepted,
    FanoutInProgress,
    FeedVisible,
    PartialDegradation,
    FailedRecoverable,
    FailedTerminal,
}

object PublisherStateMachine {
    private val allowedTransitions = mapOf(
        PublisherState.Idle to setOf(PublisherState.EditingDraft),
        PublisherState.EditingDraft to setOf(PublisherState.Idle, PublisherState.Validating),
        PublisherState.Validating to setOf(
            PublisherState.EditingDraft,
            PublisherState.UploadingMedia,
            PublisherState.ReadyToPublish,
            PublisherState.FailedRecoverable,
            PublisherState.FailedTerminal,
        ),
        PublisherState.UploadingMedia to setOf(
            PublisherState.ReadyToPublish,
            PublisherState.FailedRecoverable,
            PublisherState.FailedTerminal,
        ),
        PublisherState.ReadyToPublish to setOf(PublisherState.PublishingCommandSent, PublisherState.EditingDraft),
        PublisherState.PublishingCommandSent to setOf(PublisherState.AwaitingCommit, PublisherState.FailedRecoverable),
        PublisherState.AwaitingCommit to setOf(
            PublisherState.PublishedAccepted,
            PublisherState.FailedRecoverable,
            PublisherState.FailedTerminal,
        ),
        PublisherState.PublishedAccepted to setOf(
            PublisherState.FanoutInProgress,
            PublisherState.FeedVisible,
            PublisherState.PartialDegradation,
        ),
        PublisherState.FanoutInProgress to setOf(
            PublisherState.FeedVisible,
            PublisherState.PartialDegradation,
            PublisherState.FailedRecoverable,
        ),
        PublisherState.FeedVisible to setOf(PublisherState.Idle, PublisherState.EditingDraft),
        PublisherState.PartialDegradation to setOf(
            PublisherState.FanoutInProgress,
            PublisherState.FeedVisible,
            PublisherState.FailedRecoverable,
            PublisherState.EditingDraft,
        ),
        PublisherState.FailedRecoverable to setOf(
            PublisherState.EditingDraft,
            PublisherState.Validating,
            PublisherState.FanoutInProgress,
        ),
        PublisherState.FailedTerminal to setOf(PublisherState.EditingDraft, PublisherState.Idle),
    )

    fun canTransition(from: PublisherState, to: PublisherState): Boolean =
        from == to || to in allowedTransitions[from].orEmpty()

    fun transition(from: PublisherState, to: PublisherState): DomainResult<PublisherState> =
        if (canTransition(from, to)) {
            DomainResult.success(to)
        } else {
            DomainResult.failure(IntegrityViolation("Invalid publisher state transition $from -> $to."))
        }

    fun acceptsPublishTap(state: PublisherState): Boolean =
        state == PublisherState.Idle ||
            state == PublisherState.EditingDraft ||
            state == PublisherState.FailedRecoverable ||
            state == PublisherState.FailedTerminal ||
            state == PublisherState.PartialDegradation
}

enum class PublicationState {
    Received,
    Committed,
    Outboxed,
    Propagating,
    Published,
    Failed,
    Rejected,
    Deleted,
}

object PublicationStateMachine {
    private val allowedTransitions = mapOf(
        PublicationState.Received to setOf(PublicationState.Committed, PublicationState.Rejected),
        PublicationState.Committed to setOf(PublicationState.Outboxed, PublicationState.Failed, PublicationState.Deleted),
        PublicationState.Outboxed to setOf(PublicationState.Propagating, PublicationState.Failed, PublicationState.Deleted),
        PublicationState.Propagating to setOf(PublicationState.Published, PublicationState.Failed, PublicationState.Outboxed, PublicationState.Deleted),
        PublicationState.Published to setOf(PublicationState.Deleted, PublicationState.Failed),
        PublicationState.Failed to setOf(PublicationState.Outboxed, PublicationState.Rejected, PublicationState.Deleted),
    )

    fun transition(
        post: CanonicalPost,
        target: PublicationState,
        at: Timestamp,
    ): DomainResult<CanonicalPost> {
        if (post.state == target) {
            return DomainResult.success(post.copy(updatedAt = at))
        }
        val allowed = allowedTransitions[post.state].orEmpty()
        return if (target in allowed) {
            DomainResult.success(post.copy(state = target, updatedAt = at, version = post.version + 1))
        } else {
            DomainResult.failure(
                IntegrityViolation("Invalid publication state transition ${post.state} -> $target for ${post.id.value}."),
            )
        }
    }

    fun isReconcileable(state: PublicationState): Boolean =
        state == PublicationState.Outboxed || state == PublicationState.Propagating || state == PublicationState.Failed
}

enum class PostVisibility {
    Public,
    Followers,
}

data class DraftMediaAttachment(
    val clientMediaId: String,
    val localUri: String,
    val mimeType: String,
    val sizeBytes: Long,
) {
    init {
        require(clientMediaId.isNotBlank()) { "clientMediaId cannot be blank." }
        require(localUri.isNotBlank()) { "localUri cannot be blank." }
        require(mimeType.isNotBlank()) { "mimeType cannot be blank." }
        require(sizeBytes >= 0) { "sizeBytes cannot be negative." }
    }
}

data class PostMediaAttachment(
    val clientMediaId: String,
    val remoteUri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val uploadedAt: Timestamp,
) {
    init {
        require(clientMediaId.isNotBlank()) { "clientMediaId cannot be blank." }
        require(remoteUri.isNotBlank()) { "remoteUri cannot be blank." }
        require(mimeType.isNotBlank()) { "mimeType cannot be blank." }
        require(sizeBytes >= 0) { "sizeBytes cannot be negative." }
    }
}

data class CanonicalPost(
    val id: PostId,
    val authorId: UserId,
    val text: PostText,
    val visibility: PostVisibility,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    val state: PublicationState,
    val idempotencyKey: IdempotencyKey,
    val createdAt: Timestamp,
    val updatedAt: Timestamp = createdAt,
    val trace: TraceContext,
    val version: Int = 1,
)

data class PublishPostReceipt(
    override val postId: PostId,
    override val state: PublicationState,
    override val acceptedAt: Timestamp,
    override val trace: TraceContext,
) : PublishingCommandReceipt

sealed interface PublishingCommandReceipt {
    val postId: PostId
    val state: PublicationState
    val acceptedAt: Timestamp
    val trace: TraceContext
}

data class PostMutationReceipt(
    override val postId: PostId,
    override val state: PublicationState,
    override val acceptedAt: Timestamp,
    override val trace: TraceContext,
) : PublishingCommandReceipt

enum class OutboxStatus {
    Pending,
    Dispatching,
    Dispatched,
    Failed,
}

data class OutboxMessage(
    val id: OutboxMessageId,
    val aggregateId: PostId,
    val event: PublishingEvent,
    val status: OutboxStatus,
    val createdAt: Timestamp,
    val updatedAt: Timestamp = createdAt,
    val attempts: Int = 0,
    val lastError: String? = null,
    val nextAttemptAt: Timestamp = createdAt,
    val claimedAt: Timestamp? = null,
    val dispatchedAt: Timestamp? = null,
    val eventOffset: EventOffset? = null,
)

enum class AuthorScaleTier {
    Normal,
    HighScale,
}

enum class CandidatePlane {
    FollowGraph,
    Recommendation,
    Exploration,
}

enum class FanoutDistributionMode {
    FanoutOnWrite,
    FanoutOnRead,
}

enum class CandidateOrigin {
    AuthorSelf,
    FollowGraph,
    Recommendation,
    Exploration,
}

enum class CandidateSource(
    val plane: CandidatePlane,
    val defaultOrigin: CandidateOrigin,
    val defaultDistributionMode: FanoutDistributionMode?,
) {
    FanoutOnWrite(CandidatePlane.FollowGraph, CandidateOrigin.FollowGraph, FanoutDistributionMode.FanoutOnWrite),
    FanoutOnRead(CandidatePlane.FollowGraph, CandidateOrigin.FollowGraph, FanoutDistributionMode.FanoutOnRead),
    Recommendation(CandidatePlane.Recommendation, CandidateOrigin.Recommendation, null),
    Exploration(CandidatePlane.Exploration, CandidateOrigin.Exploration, null),
}

data class FeedCandidate(
    val id: FeedCandidateId,
    val viewerId: UserId,
    val postId: PostId,
    val authorId: UserId,
    val source: CandidateSource,
    val origin: CandidateOrigin = source.defaultOrigin,
    val distributionMode: FanoutDistributionMode? = source.defaultDistributionMode,
    val postCreatedAt: Timestamp,
    val generatedAt: Timestamp,
    val trace: TraceContext,
)

data class AuthorFeedIndexEntry(
    val postId: PostId,
    val authorId: UserId,
    val visibility: PostVisibility,
    val postCreatedAt: Timestamp,
    val indexedAt: Timestamp,
    val trace: TraceContext,
)

data class FanoutPolicyConfig(
    val maxFollowersForFanoutOnWrite: Int = 5_000,
    val viralScoreThresholdForFanoutOnRead: Long = 50_000,
    val graphPageSize: Int = 200,
    val readCandidateLookbackPosts: Int = 200,
) {
    init {
        require(maxFollowersForFanoutOnWrite >= 0) { "maxFollowersForFanoutOnWrite cannot be negative." }
        require(viralScoreThresholdForFanoutOnRead >= 0) { "viralScoreThresholdForFanoutOnRead cannot be negative." }
        require(graphPageSize in 1..200) { "graphPageSize must be between 1 and 200." }
        require(readCandidateLookbackPosts in 1..5_000) { "readCandidateLookbackPosts must be between 1 and 5000." }
    }
}

data class FanoutAuthorMetrics(
    val followerCount: Int,
    val authorScaleTier: AuthorScaleTier,
) {
    init {
        require(followerCount >= 0) { "followerCount cannot be negative." }
    }
}

data class FanoutPostMetrics(
    val postId: PostId,
    val impressionCount: Long = 0,
    val engagementCount: Long = 0,
    val viralScore: Long = engagementCount * 10 + impressionCount,
    val updatedAt: Timestamp,
) {
    init {
        require(impressionCount >= 0) { "impressionCount cannot be negative." }
        require(engagementCount >= 0) { "engagementCount cannot be negative." }
        require(viralScore >= 0) { "viralScore cannot be negative." }
    }
}

enum class FanoutDecisionReason {
    NormalAuthorBelowThreshold,
    AuthorHighScaleTier,
    FollowerThresholdExceeded,
    ViralPost,
    ForcedMigration,
}

data class FanoutDistributionDecision(
    val postId: PostId,
    val authorId: UserId,
    val mode: FanoutDistributionMode,
    val reason: FanoutDecisionReason,
    val authorMetrics: FanoutAuthorMetrics,
    val postMetrics: FanoutPostMetrics?,
    val config: FanoutPolicyConfig,
    val decidedAt: Timestamp,
    val trace: TraceContext,
)

enum class FanoutDeliveryStatus {
    Succeeded,
    Failed,
}

data class FanoutDeliveryReport(
    val postId: PostId,
    val authorId: UserId,
    val mode: FanoutDistributionMode,
    val reason: FanoutDecisionReason,
    val status: FanoutDeliveryStatus,
    val generatedCandidates: Int,
    val materializedCandidates: Int,
    val skippedFollowers: Int,
    val attempt: Int,
    val startedAt: Timestamp,
    val finishedAt: Timestamp,
    val trace: TraceContext,
    val errorMessage: String? = null,
) {
    init {
        require(generatedCandidates >= 0) { "generatedCandidates cannot be negative." }
        require(materializedCandidates >= 0) { "materializedCandidates cannot be negative." }
        require(skippedFollowers >= 0) { "skippedFollowers cannot be negative." }
        require(attempt >= 1) { "attempt must be positive." }
    }
}

enum class PostModerationState {
    Draft,
    Publishing,
    PublishedPendingReview,
    PublishedLimitedDistribution,
    PublishedFullDistribution,
    Demoted,
    Hidden,
    Removed,
}

data class PostModerationDecision(
    val postId: PostId,
    val state: PostModerationState,
    val reason: String? = null,
    val ruleVersion: String = "inline_v1",
    val decidedAt: Timestamp,
)

data class FeedPostSnapshot(
    val postId: PostId,
    val authorId: UserId,
    val text: PostText,
    val visibility: PostVisibility,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    val publicationState: PublicationState,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val trace: TraceContext,
    val canonicalVersion: Int,
) {
    val isPublished: Boolean
        get() = publicationState == PublicationState.Published
}

fun CanonicalPost.toFeedPostSnapshot(): FeedPostSnapshot =
    FeedPostSnapshot(
        postId = id,
        authorId = authorId,
        text = text,
        visibility = visibility,
        mediaAttachments = mediaAttachments,
        publicationState = state,
        createdAt = createdAt,
        updatedAt = updatedAt,
        trace = trace,
        canonicalVersion = version,
    )

fun FeedPostSnapshot.toAuthorFeedIndexEntry(indexedAt: Timestamp): AuthorFeedIndexEntry =
    AuthorFeedIndexEntry(
        postId = postId,
        authorId = authorId,
        visibility = visibility,
        postCreatedAt = createdAt,
        indexedAt = indexedAt,
        trace = trace,
    )

enum class FeedEligibilityRejectionReason {
    AuthorUnavailable,
    NotPublished,
    Privacy,
    Blocked,
    Muted,
    Moderated,
    Duplicate,
    Fatigue,
}

data class FeedEligibilityDecision(
    val accepted: Boolean,
    val rejectionReason: FeedEligibilityRejectionReason? = null,
) {
    companion object {
        val Accepted = FeedEligibilityDecision(accepted = true)

        fun rejected(reason: FeedEligibilityRejectionReason): FeedEligibilityDecision =
            FeedEligibilityDecision(accepted = false, rejectionReason = reason)
    }
}

data class FeedCandidateFeatures(
    val viewerFollowsAuthor: Boolean,
    val viewerIsAuthor: Boolean,
    val authorScaleTier: AuthorScaleTier,
    val sourcePlane: CandidatePlane,
    val sourceWeight: Double,
    val hasMedia: Boolean,
    val textLength: Int,
    val ageMillis: Long,
    val authorFatiguePenalty: Double = 0.0,
    val moderationPenalty: Double = 0.0,
)

data class EnrichedFeedCandidate(
    val candidate: FeedCandidate,
    val postSnapshot: FeedPostSnapshot,
    val features: FeedCandidateFeatures,
)

data class RankedFeedCandidate(
    val candidate: FeedCandidate,
    val score: Double,
    val rankedAt: Timestamp,
    val features: FeedCandidateFeatures? = null,
    val modelVersion: String = "heuristic.feed.v1",
    val scoreKey: Long = score.toStableFeedScoreKey(),
)

data class FeedItem(
    val postId: PostId,
    val authorId: UserId,
    val text: PostText,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    val score: Double,
    val source: CandidateSource,
    val origin: CandidateOrigin = source.defaultOrigin,
    val distributionMode: FanoutDistributionMode? = source.defaultDistributionMode,
    val sourcePlane: CandidatePlane = source.plane,
    val createdAt: Timestamp,
    val trace: TraceContext,
    val rankingModelVersion: String = "heuristic.feed.v1",
    val scoreKey: Long = score.toStableFeedScoreKey(),
    val servedFromCache: Boolean = false,
)

data class RankedFeedCacheRecord(
    val viewerId: UserId,
    val entries: List<RankedFeedCandidate>,
    val generatedAt: Timestamp,
    val rankingModelVersion: String,
    val candidateWindowSize: Int,
)

data class PendingLocalFeedItem(
    val postId: PostId,
    val authorId: UserId,
    val text: PostText,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    val acceptedAt: Timestamp,
    val trace: TraceContext,
)

enum class MergedFeedItemState {
    LocalPending,
    RemoteConfirmed,
}

data class MergedFeedItem(
    val postId: PostId,
    val authorId: UserId,
    val text: PostText,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    val createdAt: Timestamp,
    val state: MergedFeedItemState,
    val remote: FeedItem? = null,
    val local: PendingLocalFeedItem? = null,
)

data class ReconciliationReport(
    val dispatchedOutboxMessages: Int,
    val repairedOutboxMessages: Int,
    val stillPendingPosts: Int,
)

const val FeedScorePrecision: Double = 1_000_000.0

fun Double.toStableFeedScoreKey(): Long {
    val normalized = when {
        isNaN() -> 0.0
        this > Long.MAX_VALUE / FeedScorePrecision -> Long.MAX_VALUE / FeedScorePrecision
        this < Long.MIN_VALUE / FeedScorePrecision -> Long.MIN_VALUE / FeedScorePrecision
        else -> this
    }
    return (normalized * FeedScorePrecision).roundToLong()
}
