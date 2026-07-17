package com.mindos.app.features.mindboost.core.sync

import kotlin.random.Random

/**
 * Canonical, Firestore-path-safe identity of a training session at the sync layer.
 *
 * Distinct from the per-feature `SessionId` value classes (focus/memory) which validate only non-blank: this
 * one ALSO enforces the constraints of a Firestore DOCUMENT id (no whitespace, no control chars, no path
 * separators, not '.'/'..', not the reserved `__*__` pattern, bounded length). Created ONCE per session and
 * reused unchanged across local save, retry and remote write — that reuse is what makes sync idempotent.
 * Phase 1 adopts it at the sync boundary; existing motors keep their current ids until then (Phase 0 audit).
 */
@JvmInline
value class SyncSessionId(val value: String) {
    init {
        require(isValid(value)) { "SyncSessionId is not a valid Firestore document id." }
    }

    companion object {
        /** Bounded well below Firestore's 1500-byte id limit, for readable and path-safe ids. */
        const val MAX_LENGTH = 200

        /** Path separators. Whitespace and control chars are rejected separately in [isValid]. */
        private val FORBIDDEN = setOf('/', '\\')
        private val RESERVED = Regex("__.*__")

        fun isValid(raw: String): Boolean =
            raw.isNotBlank() &&
                raw.length <= MAX_LENGTH &&
                raw.none { it.isWhitespace() || it.isISOControl() || it in FORBIDDEN } &&
                raw != "." &&
                raw != ".." &&
                !RESERVED.matches(raw)

        /** Parse untrusted input (a persisted or remote value) without throwing; null when invalid. */
        fun parseOrNull(raw: String): SyncSessionId? = if (isValid(raw)) SyncSessionId(raw) else null
    }
}

/** Creates a [SyncSessionId] ONCE per session. The result must be persisted and reused, never regenerated. */
interface SyncSessionIdGenerator {
    fun newSessionId(moduleId: String, startedAtMs: Long): SyncSessionId
}

/**
 * Default generator: `"{module}-{startedAtMs}-{suffix}"`, sanitized to a Firestore-safe id (letters, digits,
 * '-' and '_' only; every other character — including whitespace — becomes '_'). The random suffix
 * distinguishes two sessions that start in the same millisecond; because it is generated once and travels
 * with the session, retries and multi-device writes reuse the SAME id. No user id or PII is embedded (the
 * `users/{uid}` document path already scopes by user; the fingerprint carries the user id for verification).
 */
class DefaultSyncSessionIdGenerator(
    private val suffixSource: () -> String = ::randomSuffix,
) : SyncSessionIdGenerator {
    override fun newSessionId(moduleId: String, startedAtMs: Long): SyncSessionId {
        val safeModule = sanitize(moduleId).ifBlank { FALLBACK_MODULE }
        return SyncSessionId("$safeModule-$startedAtMs-${suffixSource()}")
    }

    private fun sanitize(raw: String): String =
        raw.map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }.joinToString("")

    private companion object {
        const val FALLBACK_MODULE = "session"
    }
}

private const val SUFFIX_LENGTH = 8
private const val SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"

private fun randomSuffix(): String =
    (1..SUFFIX_LENGTH).map { SUFFIX_ALPHABET[Random.nextInt(SUFFIX_ALPHABET.length)] }.joinToString("")
