package com.mindos.action.understanding

import com.mindos.security.AbuseSubject

/**
 * Per-request options. Pure data — same context + same input ⇒ same result.
 *
 * - [localeTag]: selects locale-specific aliases ("es", "en"); "und" = locale-agnostic.
 * - [explain]: when true, the result carries a [com.mindos.action.observability.MatchTrace].
 *   Off by default to keep the hot path allocation-free.
 * - [maxInputLength]: hard cap guarding against adversarial input (defense in depth).
 * - [requestId]: optional correlation id used for audit/incident reconstruction.
 * - [abuseSubject]: stable user/device/session identifiers for abuse/rate limiting.
 */
data class UnderstandingContext(
    val localeTag: String = "und",
    val explain: Boolean = false,
    val maxInputLength: Int = 512,
    val requestId: String? = null,
    val abuseSubject: AbuseSubject = AbuseSubject.Anonymous,
)

/**
 * The stable contract the rest of the system depends on. Implementations are pure and
 * deterministic; the concrete engine (indices, trie, ranking) lives in `mind-action-matching`
 * and can be swapped without touching callers.
 *
 * Not `suspend`: understanding is CPU-bound and side-effect-free.
 */
interface ActionMatcher {
    fun understand(
        input: String,
        context: UnderstandingContext = UnderstandingContext(),
    ): ActionUnderstandingResult
}
