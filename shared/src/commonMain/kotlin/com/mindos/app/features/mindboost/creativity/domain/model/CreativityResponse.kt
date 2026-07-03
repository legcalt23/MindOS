package com.mindos.app.features.mindboost.creativity.domain.model

import kotlinx.serialization.Serializable

/**
 * A user's local response to a [CreativityPrompt]: the set of ideas they produced.
 *
 * Stored locally only. [elapsedMonotonicMs] is the elapsed time from prompt presentation
 * to submission, measured from a monotonic clock (the anti-cheat timing base); the offline
 * [com.mindos.app.features.mindboost.creativity.domain.validation.CreativityResponseValidator]
 * is the single boundary that judges whether that timing/content is acceptable.
 */
@Serializable
data class CreativityResponse(
    val id: String,
    val promptId: String,
    val ideas: List<String>,
    val elapsedMonotonicMs: Long,
) {
    init {
        require(id.isNotBlank()) { "Response id must not be blank" }
        require(promptId.isNotBlank()) { "Response promptId must not be blank" }
    }

    /** Ideas trimmed of surrounding whitespace with blanks dropped (deterministic). */
    val normalizedIdeas: List<String>
        get() = ideas.map { it.trim() }.filter { it.isNotEmpty() }

    /** Count of distinct non-blank ideas — the fluency proxy used by local rubrics. */
    val distinctIdeaCount: Int
        get() = normalizedIdeas.distinct().size
}
