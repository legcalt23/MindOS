package com.mindos.action.entity

import com.mindos.action.normalization.NormalizedInput

/**
 * Resolves the most-specific type for a single token when the slot accepts more than one
 * type, or for type-less/diagnostic extraction. Deterministic: tries types in a fixed
 * priority order and returns the first that validates.
 *
 * NOTE: the slot-driven matching flow normally knows the exact type, so this is a
 * secondary utility. Sigil-anchored types (mention/hashtag) and Numeric come first
 * because they are the least ambiguous; FreeText is the last resort.
 */
class EntityAmbiguityResolver(private val priority: List<EntityType> = DefaultPriority) {

    fun resolveSingleToken(
        registry: EntityExtractorRegistry,
        input: NormalizedInput,
        index: Int,
    ): EntityExtractionResult {
        for (type in priority) {
            val extractor = registry.forType(type) ?: continue
            val result = extractor.extract(input, index, index + 1)
            if (result is EntityExtractionResult.Extracted) return result
        }
        return EntityExtractionResult.Rejected(
            EntityType.FreeText,
            EntityRejectionReason.ShapeMismatch("no entity type matched token at $index"),
        )
    }

    companion object {
        /** Sigils first (most specific), then numeric, then username, then opaque ids, FreeText last. */
        val DefaultPriority: List<EntityType> = listOf(
            EntityType.Mention,
            EntityType.Hashtag,
            EntityType.Numeric,
            EntityType.Username,
            EntityType.UserId,
            EntityType.ProfileId,
            EntityType.StoryId,
            EntityType.ChatId,
            EntityType.FreeText,
        )
    }
}
