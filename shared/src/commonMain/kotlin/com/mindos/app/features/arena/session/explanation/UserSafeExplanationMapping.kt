package com.mindos.app.features.arena.session.explanation

/**
 * Phase 04: Internal reason-code categories used to drive user-facing copy. These collapse the many
 * raw reason codes produced by the comparability / measurement / rating layers into a small set.
 */
enum class InternalReasonCode {
    INTEGRITY_NOT_VERIFIED,
    LEDGER_NOT_APPROVED_CURRENT,
    NOT_COMPARABLE,
    PARTIALLY_COMPARABLE,
    INSUFFICIENT_MEASUREMENT_CONFIDENCE,
    SESSION_INVALID,
    SUFFICIENT,
    UNKNOWN;

    companion object {
        private val MEASUREMENT_INSUFFICIENT = setOf(
            "SEM_ABOVE_MAX",
            "RELIABILITY_BELOW_MIN",
            "SAMPLE_SIZE_BELOW_MIN",
        )

        /** Maps any raw reason code from any layer into a safe internal category. */
        fun fromRaw(raw: String): InternalReasonCode {
            val token = raw.trim().uppercase()
            return entries.firstOrNull { token == it.name } ?: when {
                token.startsWith("NOT_COMPARABLE") -> NOT_COMPARABLE
                token.startsWith("PARTIALLY_COMPARABLE") -> PARTIALLY_COMPARABLE
                token.startsWith("COMPARABLE") -> SUFFICIENT
                token in MEASUREMENT_INSUFFICIENT -> INSUFFICIENT_MEASUREMENT_CONFIDENCE
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Phase 04: Maps internal reason codes to user-safe copy (user_safe_explanation_mapping.md).
 *
 * The copy deliberately reveals NO thresholds (no numbers), NO algorithm names, and NO anti-cheat
 * signals — it uses neutral, plain product language only. This is the only sanctioned surface
 * for turning an internal decision into something a user could see.
 */
object UserSafeExplanationMapper {

    fun explain(rawReasonCode: String): String = copyFor(InternalReasonCode.fromRaw(rawReasonCode))

    fun copyFor(code: InternalReasonCode): String = when (code) {
        InternalReasonCode.INTEGRITY_NOT_VERIFIED ->
            "This session couldn't be confirmed, so it didn't change your profile."
        InternalReasonCode.LEDGER_NOT_APPROVED_CURRENT ->
            "This exercise is being refreshed, so this session wasn't counted yet."
        InternalReasonCode.NOT_COMPARABLE ->
            "Your session conditions weren't standard, so it didn't affect your calibration."
        InternalReasonCode.PARTIALLY_COMPARABLE ->
            "Some session conditions varied, so this session counted only partially."
        InternalReasonCode.INSUFFICIENT_MEASUREMENT_CONFIDENCE ->
            "A little more practice is needed before we update your profile."
        InternalReasonCode.SESSION_INVALID ->
            "This session ended early, so it wasn't counted."
        InternalReasonCode.SUFFICIENT ->
            "Your session was counted toward your profile."
        InternalReasonCode.UNKNOWN ->
            "This session didn't update your profile."
    }
}
