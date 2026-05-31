package com.mindos.action.normalization

import kotlinx.serialization.Serializable

/**
 * Result of running [NormalizationPipeline]. The input is tokenized exactly once;
 * downstream stages (matching, entity extraction) reuse [tokens] without re-splitting.
 *
 * - [text]: canonical normalized text (join of token [Token.normalized]) — the key for
 *   exact/alias indices and static-token matching.
 * - [displayText]: join of token [Token.display] (accent-preserving, lowercased).
 * - [rawText]: join of token [Token.raw] (original case, NFC, control-stripped).
 */
@Serializable
data class NormalizedInput(
    val raw: String,
    val text: String,
    val displayText: String,
    val rawText: String,
    val tokens: List<Token>,
    val config: NormalizationConfig,
) {
    val isBlank: Boolean get() = tokens.isEmpty()
}
