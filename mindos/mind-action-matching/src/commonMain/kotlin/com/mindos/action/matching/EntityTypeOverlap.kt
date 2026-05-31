package com.mindos.action.matching

import com.mindos.action.entity.EntityType

/**
 * Whether two slot types can match a common token — used only by the RegistryCompiler for
 * build-time conflict detection. Conservative and explicit:
 *
 * - FreeText overlaps everything (captures any token).
 * - All alphanumeric id-like types overlap each other (a token like "ana" or "123" is a
 *   valid username, opaque id, and—if digits—numeric).
 * - Mention (@) and Hashtag (#) are sigil-anchored and overlap only themselves.
 */
internal object EntityTypeOverlap {

    private val ALPHANUMERIC: Set<EntityType> = setOf(
        EntityType.Username,
        EntityType.UserId,
        EntityType.ProfileId,
        EntityType.StoryId,
        EntityType.ChatId,
        EntityType.Numeric,
    )

    fun overlap(a: EntityType, b: EntityType): Boolean {
        if (a == b) return true
        if (a == EntityType.FreeText || b == EntityType.FreeText) return true
        return a in ALPHANUMERIC && b in ALPHANUMERIC
    }
}
