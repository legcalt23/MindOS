package com.example.neuv3.features.social.domain.publishing

import com.example.neuv3.features.social.domain.error.InvalidValue
import com.example.neuv3.features.social.domain.valueobject.RequestId
import com.example.neuv3.features.social.shared.util.DomainResult

@JvmInline
value class PostId(val value: String) {
    init {
        require(value.isNotBlank()) { "PostId cannot be blank." }
    }
}

@JvmInline
value class IdempotencyKey(val value: String) {
    init {
        require(value.isNotBlank()) { "IdempotencyKey cannot be blank." }
    }
}

@JvmInline
value class TraceId(val value: String) {
    init {
        require(value.isNotBlank()) { "TraceId cannot be blank." }
    }
}

@JvmInline
value class EventId(val value: String) {
    init {
        require(value.isNotBlank()) { "EventId cannot be blank." }
    }
}

@JvmInline
value class EventOffset(val value: Long) : Comparable<EventOffset> {
    init {
        require(value > 0) { "EventOffset must be positive." }
    }

    override fun compareTo(other: EventOffset): Int =
        value.compareTo(other.value)

    companion object {
        val First: EventOffset = EventOffset(1)
    }
}

@JvmInline
value class EventSubscriberId(val value: String) {
    init {
        require(value.isNotBlank()) { "EventSubscriberId cannot be blank." }
    }
}

@JvmInline
value class OutboxMessageId(val value: String) {
    init {
        require(value.isNotBlank()) { "OutboxMessageId cannot be blank." }
    }
}

@JvmInline
value class FeedCandidateId(val value: String) {
    init {
        require(value.isNotBlank()) { "FeedCandidateId cannot be blank." }
    }
}

@JvmInline
value class PostText private constructor(val value: String) {
    init {
        require(value.isNotBlank() && value.length <= MaxLength) {
            "PostText must be 1-$MaxLength characters."
        }
    }

    companion object {
        const val MaxLength: Int = 2_000

        fun from(raw: String): DomainResult<PostText> {
            val normalized = raw.trim()
            return when {
                normalized.isBlank() -> DomainResult.failure(InvalidValue("postText", "blank value"))
                normalized.length > MaxLength -> DomainResult.failure(
                    InvalidValue("postText", "must be $MaxLength characters or less"),
                )
                else -> DomainResult.success(PostText(normalized))
            }
        }
    }
}

data class TraceContext(
    val traceId: TraceId,
    val requestId: RequestId,
    val postId: PostId? = null,
    val idempotencyKey: IdempotencyKey? = null,
) {
    fun withPostId(postId: PostId): TraceContext =
        copy(postId = postId)

    fun withIdempotencyKey(idempotencyKey: IdempotencyKey): TraceContext =
        copy(idempotencyKey = idempotencyKey)

    fun tags(): Map<String, String> =
        buildMap {
            put("traceId", traceId.value)
            put("requestId", requestId.value)
            idempotencyKey?.let { put("idempotencyKey", it.value) }
            postId?.let { put("postId", it.value) }
        }
}
