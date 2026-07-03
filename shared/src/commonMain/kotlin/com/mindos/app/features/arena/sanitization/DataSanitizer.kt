package com.mindos.app.features.arena.sanitization

/**
 * Phase 07.5: Output wrapper that can ONLY be produced by [DataSanitizer]. Any code that requires a
 * [SanitizedArenaOutput] is therefore guaranteed the content cleared the sanitization gate — the
 * final boundary before serialization to JSON/CSV/logs.
 */
class SanitizedArenaOutput private constructor(val content: String) {
    internal companion object {
        fun wrap(content: String): SanitizedArenaOutput = SanitizedArenaOutput(content)
    }
}

/**
 * Phase 07.5 / Task 8: The mandatory Data Sanitization Gate.
 *
 * Scrubs any text crossing the Arena boundary:
 * - explicitly-known raw ids -> stable synthetic UUIDv4 (via [SyntheticIdMapper])
 * - emails / IPv4 / long UID-or-device-like tokens -> redacted or synthetic ids
 * - cognitive-profiling phrases ("Low Performance", "High Intelligence", ...) -> redacted
 *
 * Synthetic UUIDv4 ids (which contain dashes) are intentionally preserved — they are the allowed,
 * non-identifying form. Exceptions are made safe via [SafeArenaException].
 */
class DataSanitizer(private val idMapper: SyntheticIdMapper) {

    /** Scrubs [raw]. Any string in [knownRawIds] is mapped to a synthetic id regardless of format. */
    fun scrub(raw: String, knownRawIds: Set<String> = emptySet()): String {
        var out = raw
        for (id in knownRawIds) {
            if (id.isNotEmpty()) out = out.replace(id, idMapper.map(id))
        }
        out = EMAIL.replace(out, REDACTED_EMAIL)
        out = IPV4.replace(out, REDACTED_IP)
        out = LONG_TOKEN.replace(out) { idMapper.map(it.value) }
        for (phrase in PROHIBITED_PHRASES) {
            out = Regex(Regex.escape(phrase), RegexOption.IGNORE_CASE).replace(out, REDACTED_LABEL)
        }
        return out
    }

    /** Scrubs [raw] and returns it wrapped so downstream code knows it passed the gate. */
    fun sanitize(raw: String, knownRawIds: Set<String> = emptySet()): SanitizedArenaOutput =
        SanitizedArenaOutput.wrap(scrub(raw, knownRawIds))

    /** Converts any throwable to a boundary-safe exception (message + cause stripped). */
    fun safeException(throwable: Throwable): SafeArenaException = SafeArenaException.from(throwable)

    companion object {
        private const val REDACTED_EMAIL = "[REDACTED_EMAIL]"
        private const val REDACTED_IP = "[REDACTED_IP]"
        private const val REDACTED_LABEL = "[REDACTED_LABEL]"

        private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        private val IPV4 = Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")

        /** 20+ contiguous alphanumerics: UID/device-token shaped. UUIDv4 (dashed) never matches. */
        private val LONG_TOKEN = Regex("\\b[A-Za-z0-9]{20,}\\b")

        /** Cognitive-profiling phrases that must never be emitted (14_phase_07_5_data_sanitization.md). */
        val PROHIBITED_PHRASES: List<String> = listOf(
            "Low Performance",
            "High Performance",
            "Low Intelligence",
            "High Intelligence",
            "cognitive deficiency",
        )
    }
}
