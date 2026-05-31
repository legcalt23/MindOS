package com.mindos.action.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed entity kinds supported in v1 (ZIP02 §4). Deliberately a sealed hierarchy (not an
 * enum) so future kinds (DateValue, DateRange, Url, Email…) can carry constraints without
 * breaking the model. Date/range/relative-time are **intentionally deferred** to v1.5.
 *
 * [id] is the stable, serialization-safe key used in patterns, telemetry and traces.
 */
@Serializable
sealed interface EntityType {
    val id: String

    @Serializable @SerialName("username")
    data object Username : EntityType { override val id: String = "username" }

    @Serializable @SerialName("mention")
    data object Mention : EntityType { override val id: String = "mention" }

    @Serializable @SerialName("hashtag")
    data object Hashtag : EntityType { override val id: String = "hashtag" }

    @Serializable @SerialName("user_id")
    data object UserId : EntityType { override val id: String = "user_id" }

    @Serializable @SerialName("profile_id")
    data object ProfileId : EntityType { override val id: String = "profile_id" }

    @Serializable @SerialName("story_id")
    data object StoryId : EntityType { override val id: String = "story_id" }

    @Serializable @SerialName("chat_id")
    data object ChatId : EntityType { override val id: String = "chat_id" }

    @Serializable @SerialName("numeric")
    data object Numeric : EntityType { override val id: String = "numeric" }

    @Serializable @SerialName("free_text")
    data object FreeText : EntityType { override val id: String = "free_text" }

    companion object {
        /** All v1 types, stable order. */
        val all: List<EntityType> = listOf(
            Username, Mention, Hashtag, UserId, ProfileId, StoryId, ChatId, Numeric, FreeText,
        )

        fun byId(id: String): EntityType? = all.firstOrNull { it.id == id }
    }
}
