package com.mindos.action.normalization

/** A single pure text transformation. Steps are composed by [NormalizationPipeline]. */
fun interface NormalizationStep {
    fun apply(input: String): String
}

/** Trim leading/trailing whitespace. */
internal object TrimStep : NormalizationStep {
    override fun apply(input: String): String = input.trim()
}

/** Locale-invariant case folding (Kotlin's no-arg lowercase is Unicode-invariant). */
internal object CaseFoldStep : NormalizationStep {
    override fun apply(input: String): String = input.lowercase()
}

/** Unicode NFC composition. */
internal object NfcStep : NormalizationStep {
    override fun apply(input: String): String = nfcNormalize(input)
}

/** Fold diacritics (á→a, ñ→n). Applied only to the [Token.normalized] form. */
internal object DiacriticFoldStep : NormalizationStep {
    override fun apply(input: String): String = foldDiacritics(input)
}

/**
 * Remove Unicode control/format characters (zero-width spaces, joiners, BOM, etc.)
 * while preserving real whitespace. Char-scan, zero regex — safe on the hot path.
 */
internal object ControlCharStep : NormalizationStep {
    override fun apply(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
                sb.append(ch)
                continue
            }
            val cat = ch.category
            if (cat == CharCategory.CONTROL || cat == CharCategory.FORMAT) continue
            sb.append(ch)
        }
        return sb.toString()
    }
}

/**
 * Collapse any run of whitespace to a single ASCII space and trim ends.
 * Char-scan, zero regex — replaces the per-request `Regex("\\s+")` blocker (P1).
 */
internal object WhitespaceCollapseStep : NormalizationStep {
    override fun apply(input: String): String {
        val sb = StringBuilder(input.length)
        var pendingSpace = false
        for (ch in input) {
            if (ch.isWhitespace()) {
                pendingSpace = sb.isNotEmpty()
            } else {
                if (pendingSpace) sb.append(' ')
                pendingSpace = false
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
