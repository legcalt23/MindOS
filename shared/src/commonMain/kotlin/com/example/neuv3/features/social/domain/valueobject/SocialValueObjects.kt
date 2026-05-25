package com.example.neuv3.features.social.domain.valueobject

import com.example.neuv3.features.social.domain.error.InvalidValue
import com.example.neuv3.features.social.shared.util.DomainResult

@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "UserId cannot be blank." }
    }

    override fun toString(): String = value

    companion object {
        fun from(raw: String): DomainResult<UserId> {
            val normalized = raw.trim()
            return if (normalized.isBlank()) {
                DomainResult.failure(InvalidValue("userId", "blank value"))
            } else {
                DomainResult.success(UserId(normalized))
            }
        }
    }
}

@JvmInline
value class ProfileId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProfileId cannot be blank." }
    }

    companion object {
        fun fromUserId(userId: UserId): ProfileId = ProfileId(userId.value)
    }
}

@JvmInline
value class RequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "RequestId cannot be blank." }
    }
}

@JvmInline
value class AuditEventId(val value: String) {
    init {
        require(value.isNotBlank()) { "AuditEventId cannot be blank." }
    }
}

@JvmInline
value class Handle(val value: String) {
    init {
        require(isValidNormalized(value)) {
            "Handle must contain 3-30 lowercase letters, numbers, dots or underscores."
        }
    }

    override fun toString(): String = "@$value"

    companion object {
        private val Pattern = Regex("^[a-z0-9._]{3,30}$")

        fun from(raw: String): DomainResult<Handle> {
            val normalized = raw.trim().removePrefix("@").lowercase()
            return if (isValidNormalized(normalized)) {
                DomainResult.success(Handle(normalized))
            } else {
                DomainResult.failure(
                    InvalidValue(
                        field = "handle",
                        reason = "must be 3-30 chars using lowercase letters, numbers, dots or underscores",
                    ),
                )
            }
        }

        private fun isValidNormalized(value: String): Boolean =
            Pattern.matches(value) && !value.startsWith(".") && !value.endsWith(".") && !value.contains("..")
    }
}

@JvmInline
value class DisplayName(val value: String) {
    init {
        require(value.isNotBlank() && value.length <= MaxLength) {
            "DisplayName must be 1-$MaxLength characters."
        }
    }

    companion object {
        const val MaxLength: Int = 80

        fun from(raw: String): DomainResult<DisplayName> {
            val normalized = raw.trim()
            return if (normalized.isBlank() || normalized.length > MaxLength) {
                DomainResult.failure(InvalidValue("displayName", "must be 1-$MaxLength characters"))
            } else {
                DomainResult.success(DisplayName(normalized))
            }
        }
    }
}

@JvmInline
value class Bio(val value: String) {
    init {
        require(value.length <= MaxLength) { "Bio must be $MaxLength characters or less." }
    }

    companion object {
        const val MaxLength: Int = 300
        val Empty: Bio = Bio("")

        fun from(raw: String): DomainResult<Bio> {
            val normalized = raw.trim()
            return if (normalized.length > MaxLength) {
                DomainResult.failure(InvalidValue("bio", "must be $MaxLength characters or less"))
            } else {
                DomainResult.success(Bio(normalized))
            }
        }
    }
}

@JvmInline
value class Cursor(val value: String) {
    init {
        require(value.isNotBlank()) { "Cursor cannot be blank." }
    }
}

@JvmInline
value class Timestamp(val epochMillis: Long) : Comparable<Timestamp> {
    init {
        require(epochMillis >= 0) { "Timestamp cannot be negative." }
    }

    override fun compareTo(other: Timestamp): Int =
        epochMillis.compareTo(other.epochMillis)

    companion object {
        val Zero: Timestamp = Timestamp(0)
    }
}

data class RelationshipPair(
    val actorId: UserId,
    val targetId: UserId,
) {
    init {
        require(actorId != targetId) { "RelationshipPair requires two different users." }
    }
}

