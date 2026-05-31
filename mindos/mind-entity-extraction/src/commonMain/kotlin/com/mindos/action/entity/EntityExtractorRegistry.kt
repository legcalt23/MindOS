package com.mindos.action.entity

/**
 * Immutable lookup of [EntityExtractor] by [EntityType]. The matching engine asks for the
 * extractor a slot declares; there is no global mutable registry.
 */
class EntityExtractorRegistry(extractors: List<EntityExtractor>) {
    private val byType: Map<EntityType, EntityExtractor> = extractors.associateBy { it.type }

    val supportedTypes: Set<EntityType> get() = byType.keys

    fun forType(type: EntityType): EntityExtractor? = byType[type]

    fun supports(type: EntityType): Boolean = byType.containsKey(type)

    companion object {
        /** The full v1 extractor set. Date/range types are intentionally absent. */
        fun default(): EntityExtractorRegistry = EntityExtractorRegistry(
            listOf(
                UsernameExtractor(),
                MentionExtractor(),
                HashtagExtractor(),
                OpaqueIdExtractor(EntityType.UserId),
                OpaqueIdExtractor(EntityType.ProfileId),
                OpaqueIdExtractor(EntityType.StoryId),
                OpaqueIdExtractor(EntityType.ChatId),
                NumericExtractor(),
                FreeTextExtractor(),
            ),
        )
    }
}
