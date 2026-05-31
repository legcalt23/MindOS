package com.mindos.action.entity

import kotlinx.serialization.Serializable

/**
 * Deterministic confidence — derived from rule strength, **never probabilistic**.
 *
 * - [EXACT]: a sigil-anchored or exhaustively-validated match (@mention, #hashtag).
 * - [HIGH]: a typed slot whose shape validated cleanly (username, id, numeric).
 * - [MEDIUM]: permissive capture (free text) that passed only minimal checks.
 */
@Serializable
enum class DeterministicConfidence { EXACT, HIGH, MEDIUM }

/**
 * A validated, normalized entity bound to a token span.
 *
 * - [raw]: original-case source text.
 * - [value]: canonical normalized value the app consumes (e.g. "ana" for "@Ana").
 */
@Serializable
data class ExtractedEntity(
    val type: EntityType,
    val raw: String,
    val value: String,
    val startToken: Int,
    val endToken: Int,
    val confidence: DeterministicConfidence,
)
