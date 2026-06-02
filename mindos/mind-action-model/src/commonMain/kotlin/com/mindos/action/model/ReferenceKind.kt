package com.mindos.action.model

import kotlinx.serialization.Serializable

/**
 * A kind of reference that must be resolved from a human-facing value to a canonical id
 * before navigation (ZIP03 Resolver Engine). Shared vocabulary across model/validation/
 * resolver/execution so no module reaches into another's internals.
 */
@Serializable
enum class ReferenceKind(val id: String) {
    USERNAME_TO_USER_ID("username_to_user_id"),
    STORY_SLUG_TO_STORY_ID("story_slug_to_story_id"),
    CURRENT_STORY_BY_USER_ID("current_story_by_user_id"),
    CHAT_ALIAS_TO_CHAT_ID("chat_alias_to_chat_id"),
}

/** A concrete (kind, rawValue) pair an action needs resolved. */
@Serializable
data class ActionReference(val kind: ReferenceKind, val rawValue: String)
