package com.mindos.app.features.mindboost.core.sync

/**
 * Minimal, privacy-preserving fingerprint of a session, used for idempotency and conflict detection.
 *
 * Contains ONLY allowlisted summary fields — NEVER raw trials or per-item content. [normalizedScore] is
 * nullable to preserve `null != 0` (a calibrating session has no score; a zero score is a distinct value).
 * Equality of two fingerprints IS the "same session" test used by the sync classifier.
 *
 * NOTE: [fingerprint] is an IDEMPOTENCY / conflict-detection key, NOT a cryptographic or anti-tamper
 * guarantee. FNV-1a is a fast, deterministic, non-cryptographic hash — do not rely on it for security.
 */
data class SessionFingerprint(
    val userId: String,
    val sessionId: String,
    val moduleId: String,
    val exerciseId: String?,
    val startedAtMs: Long,
    val completedAtMs: Long,
    val durationMs: Long,
    val scoringVersion: String,
    val normalizedScore: Double?,
) {
    /** Deterministic, cross-platform canonical string: fixed field order, explicit null tokens. */
    fun canonical(): String = listOf(
        userId,
        sessionId,
        moduleId,
        exerciseId ?: NULL_TOKEN,
        startedAtMs.toString(),
        completedAtMs.toString(),
        durationMs.toString(),
        scoringVersion,
        normalizedScore?.toString() ?: NULL_TOKEN,
    ).joinToString(FIELD_SEPARATOR)

    /** A short, stable, cross-platform hash of [canonical] (FNV-1a — an idempotency key, NOT a security hash). */
    fun fingerprint(): String = fnv1a(canonical())

    companion object {
        // Printable token that no real field value equals, so an absent value stays distinct from the literal
        // strings "null" and "". Printable ASCII only — no NUL byte in source, and readable in canonical()/logs.
        private const val NULL_TOKEN = "<NULL>"
        private const val FIELD_SEPARATOR = "|"

        // FNV-1a over 64-bit Long (wrapping multiply == mod 2^64). Deterministic across JVM/Native/JS.
        private const val FNV_OFFSET = -3750763034362895579L // 14695981039346656037 unsigned
        private const val FNV_PRIME = 1099511628211L
        private const val HASH_RADIX = 16

        private fun fnv1a(text: String): String {
            var hash = FNV_OFFSET
            for (char in text) {
                hash = hash xor char.code.toLong()
                hash *= FNV_PRIME
            }
            return hash.toString(HASH_RADIX)
        }
    }
}
