package com.example.mindos.features.social.domain.publishing

import kotlinx.serialization.Serializable

enum class MetricName {
    PublishLatency,
    FanoutLag,
    ConsumerLag,
    FeedLatency,
    ModerationLatency,
    PublishSuccess,
    DuplicateSuppression,
    FanoutCompleted,
    FeedReadEmpty,
    StaleFeedRead,
    QueueDepth,
    OutboxBacklog,
    DlqSize,
    SystemBacklogChecked
}

enum class Stage {
    Publishing,
    Moderation,
    Fanout,
    InboxInjection,
    FeedProjection,
    FeedServing,
    OutboxRelay,
    Reconciliation
}

enum class Outcome {
    Started,
    Succeeded,
    Failed,
    Retried,
    Skipped
}

enum class ModerationOutcome {
    Approved,
    Rejected,
    ShadowBlocked,
    Deferred
}

enum class ErrorCategory {
    VALIDATION,
    DEPENDENCY,
    INTERNAL,
    SECURITY,
    INTEGRITY
}

enum class Retryability {
    RETRYABLE,
    NON_RETRYABLE
}

enum class ErrorCode {
    DATABASE_TIMEOUT,
    NETWORK_FAILURE,
    RATE_LIMITED,
    PAYLOAD_TOO_LARGE,
    DEPENDENCY_UNAVAILABLE,
    INTEGRITY_VIOLATION
}

enum class FeedFreshness {
    FRESH,
    WARM,
    STALE
}

// Low cardinality tags for Metrics
abstract class MetricTags {
    abstract fun toMap(): Map<String, String>
}

data class DefaultMetricTags(
    val stage: Stage? = null,
    val outcome: Outcome? = null,
    val errorCategory: ErrorCategory? = null,
    val freshness: FeedFreshness? = null,
    val isDuplicateSuppressed: Boolean? = null,
    val distributionMode: String? = null // FanoutMode enum name
) : MetricTags() {
    override fun toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        stage?.let { map["stage"] = it.name }
        outcome?.let { map["outcome"] = it.name }
        errorCategory?.let { map["error_category"] = it.name }
        freshness?.let { map["freshness"] = it.name }
        isDuplicateSuppressed?.let { map["is_duplicate_suppressed"] = it.toString() }
        distributionMode?.let { map["distribution_mode"] = it }
        return map
    }
}

@Serializable
data class StructuredLogEvent(
    val traceId: String,
    val requestId: String,
    val postId: String? = null,
    val authorId: String? = null,
    val publishAttempt: Int? = null,
    val event: String, // event name descriptor
    val stage: String,
    val result: String? = null,
    val durationMs: Long? = null,
    val errorCategory: String? = null,
    val errorCode: String? = null,
    val retryability: String? = null,
    val attemptNumber: Int? = null,
    val retryScheduled: Boolean? = null,
    val retryExhausted: Boolean? = null,
    val deadLettered: Boolean? = null,
    val moderationDecision: String? = null,
    val generatedCandidates: Int? = null,
    val materializedCandidates: Int? = null,
    val skippedFollowers: Int? = null
)
