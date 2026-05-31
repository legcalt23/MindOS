package com.mindos.action.entity

import com.mindos.action.normalization.NormalizedInput

/**
 * Extracts and validates one entity from a token span of an already-normalized input.
 *
 * Contract:
 * - Operates on the token range `[fromToken, untilToken)` of [input].
 * - Pure and deterministic: same input + span ⇒ same result.
 * - Returns a typed [EntityExtractionResult]; never throws for invalid input.
 * - Precision-first: rejects anything not matching its shape (no false positives).
 */
interface EntityExtractor {
    val type: EntityType

    fun extract(input: NormalizedInput, fromToken: Int, untilToken: Int): EntityExtractionResult
}
