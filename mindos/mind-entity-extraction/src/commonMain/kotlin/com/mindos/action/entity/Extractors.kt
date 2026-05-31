package com.mindos.action.entity

import com.mindos.action.entity.EntityExtractionResult.Extracted
import com.mindos.action.entity.EntityExtractionResult.Rejected
import com.mindos.action.normalization.NormalizedInput
import com.mindos.action.normalization.Token

/** Base for entities that occupy exactly one token. */
internal abstract class SingleTokenExtractor : EntityExtractor {
    final override fun extract(input: NormalizedInput, fromToken: Int, untilToken: Int): EntityExtractionResult {
        val count = untilToken - fromToken
        if (count != 1) {
            return Rejected(type, EntityRejectionReason.WrongTokenCount("expected 1 token, got $count"))
        }
        return extractToken(input.tokens[fromToken], fromToken)
    }

    protected abstract fun extractToken(token: Token, index: Int): EntityExtractionResult

    protected fun ok(
        token: Token,
        index: Int,
        value: String,
        confidence: DeterministicConfidence,
    ): Extracted = Extracted(
        ExtractedEntity(
            type = type,
            raw = token.raw,
            value = value,
            startToken = index,
            endToken = index + 1,
            confidence = confidence,
        ),
    )
}

/** Usernames: optional leading '@', 2–30 chars [a-z0-9_.], no edge dot. Case-folded value. */
internal class UsernameExtractor : SingleTokenExtractor() {
    override val type: EntityType = EntityType.Username

    override fun extractToken(token: Token, index: Int): EntityExtractionResult {
        val hadSigil = token.normalized.startsWith('@')
        val body = if (hadSigil) token.normalized.substring(1) else token.normalized
        if (body.isEmpty()) return Rejected(type, EntityRejectionReason.Empty("empty username"))
        if (body.length < 2) return Rejected(type, EntityRejectionReason.TooShort("username '<2' chars"))
        if (body.length > 30) return Rejected(type, EntityRejectionReason.TooLong("username '>30' chars"))
        if (body.startsWith('.') || body.endsWith('.')) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("username has edge dot"))
        }
        if (!EntityShapes.USERNAME.matches(body)) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("'$body' is not a valid username"))
        }
        val confidence = if (hadSigil) DeterministicConfidence.EXACT else DeterministicConfidence.HIGH
        return ok(token, index, body, confidence)
    }
}

/** Mentions: MUST start with '@'; body validated as a username. Value is the bare username. */
internal class MentionExtractor : SingleTokenExtractor() {
    override val type: EntityType = EntityType.Mention

    override fun extractToken(token: Token, index: Int): EntityExtractionResult {
        if (!token.normalized.startsWith('@')) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("mention must start with '@'"))
        }
        val body = token.normalized.substring(1)
        if (body.isEmpty()) return Rejected(type, EntityRejectionReason.Empty("empty mention"))
        if (body.length < 2) return Rejected(type, EntityRejectionReason.TooShort("mention '<2' chars"))
        if (body.length > 30) return Rejected(type, EntityRejectionReason.TooLong("mention '>30' chars"))
        if (body.startsWith('.') || body.endsWith('.') || !EntityShapes.USERNAME.matches(body)) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("'$body' is not a valid mention body"))
        }
        return ok(token, index, body, DeterministicConfidence.EXACT)
    }
}

/** Hashtags: MUST start with '#'; body [a-z0-9_]{1,100}. Value is the bare tag. */
internal class HashtagExtractor : SingleTokenExtractor() {
    override val type: EntityType = EntityType.Hashtag

    override fun extractToken(token: Token, index: Int): EntityExtractionResult {
        if (!token.normalized.startsWith('#')) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("hashtag must start with '#'"))
        }
        val body = token.normalized.substring(1)
        if (body.isEmpty()) return Rejected(type, EntityRejectionReason.Empty("empty hashtag"))
        if (body.length > 100) return Rejected(type, EntityRejectionReason.TooLong("hashtag '>100' chars"))
        if (!EntityShapes.HASHTAG_BODY.matches(body)) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("'$body' is not a valid hashtag body"))
        }
        return ok(token, index, body, DeterministicConfidence.EXACT)
    }
}

/**
 * Opaque, **case-sensitive** ids (UserId/ProfileId/StoryId/ChatId). Uses [Token.raw] to
 * preserve original case (Firestore ids are case-sensitive). The slot declares which id
 * type applies; the shape is shared, so collisions between id-typed slots at the same
 * position are caught by the RegistryCompiler, not guessed at runtime.
 */
internal class OpaqueIdExtractor(
    override val type: EntityType,
    private val minLength: Int = 3,
) : SingleTokenExtractor() {

    override fun extractToken(token: Token, index: Int): EntityExtractionResult {
        val v = token.raw
        if (v.isEmpty()) return Rejected(type, EntityRejectionReason.Empty("empty id"))
        if (v.length < minLength) return Rejected(type, EntityRejectionReason.TooShort("id '<$minLength' chars"))
        if (v.length > 128) return Rejected(type, EntityRejectionReason.TooLong("id '>128' chars"))
        if (!EntityShapes.OPAQUE_ID.matches(v)) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("'$v' is not a valid id"))
        }
        return ok(token, index, v, DeterministicConfidence.HIGH)
    }
}

/** Signed integers up to 19 digits. Uses [Token.raw] (digits are case-irrelevant). */
internal class NumericExtractor : SingleTokenExtractor() {
    override val type: EntityType = EntityType.Numeric

    override fun extractToken(token: Token, index: Int): EntityExtractionResult {
        val v = token.raw
        if (v.isEmpty()) return Rejected(type, EntityRejectionReason.Empty("empty numeric"))
        if (!EntityShapes.NUMERIC.matches(v)) {
            return Rejected(type, EntityRejectionReason.ShapeMismatch("'$v' is not numeric"))
        }
        return ok(token, index, v, DeterministicConfidence.HIGH)
    }
}

/**
 * Greedy free text: captures one or more tokens verbatim using [Token.raw] (preserves
 * case AND accents — "café" stays "café"). The lowest-specificity slot; only rejects when
 * the span is empty. Always MEDIUM confidence.
 */
internal class FreeTextExtractor : EntityExtractor {
    override val type: EntityType = EntityType.FreeText

    override fun extract(input: NormalizedInput, fromToken: Int, untilToken: Int): EntityExtractionResult {
        if (untilToken - fromToken < 1) {
            return EntityExtractionResult.Rejected(type, EntityRejectionReason.Empty("free text needs ≥1 token"))
        }
        val value = buildString {
            for (i in fromToken until untilToken) {
                if (i > fromToken) append(' ')
                append(input.tokens[i].raw)
            }
        }
        return EntityExtractionResult.Extracted(
            ExtractedEntity(
                type = type,
                raw = value,
                value = value,
                startToken = fromToken,
                endToken = untilToken,
                confidence = DeterministicConfidence.MEDIUM,
            ),
        )
    }
}
