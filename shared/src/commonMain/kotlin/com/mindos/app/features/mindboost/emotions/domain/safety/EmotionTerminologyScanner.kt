package com.mindos.app.features.mindboost.emotions.domain.safety

/** A whole-word, accent- and case-insensitive match of a forbidden term in scanned content. */
data class TerminologyHit(val term: String, val index: Int)

/**
 * Pure forbidden-terminology matcher for the emotions module. This is the in-code counterpart of the
 * Gradle `emotionsTerminologyScanner` task (same matching rules, parity-tested), so the build gate and
 * the runtime compliance test agree.
 *
 * Decision I/Q4c: matching is **whole-word, case-insensitive AND accent-insensitive**, and deliberately
 * hyphen-crossing (a forbidden root inside a hyphenated compound is still caught — `\b` treats `-` as a
 * boundary). Because `java.text.Normalizer` is JVM-only and unusable in `commonMain`, accent folding is a
 * small pure-Kotlin map covering the Spanish diacritics that appear in the forbidden list.
 *
 * This file intentionally contains ZERO forbidden literals (the term list is supplied by the caller /
 * the Gradle task), so the scanner never trips when it walks its own source tree.
 */
object EmotionTerminologyScanner {

    private val ACCENT_FOLD: Map<Char, Char> = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
    )

    /** Lower-case then strip the Spanish diacritics (1:1, index-preserving). */
    fun fold(raw: String): String {
        val lowered = raw.lowercase()
        return buildString(lowered.length) {
            for (ch in lowered) append(ACCENT_FOLD[ch] ?: ch)
        }
    }

    /** Every whole-word occurrence of any forbidden term in [content] (folded comparison). */
    fun scan(content: String, forbiddenTerms: Collection<String>): List<TerminologyHit> {
        val foldedContent = fold(content)
        val hits = mutableListOf<TerminologyHit>()
        for (term in forbiddenTerms) {
            if (term.isBlank()) continue
            val regex = Regex("\\b" + Regex.escape(fold(term)) + "\\b")
            for (match in regex.findAll(foldedContent)) {
                hits.add(TerminologyHit(term, match.range.first))
            }
        }
        return hits
    }

    /** True if [content] contains any of [forbiddenTerms] as a whole word. */
    fun containsForbidden(content: String, forbiddenTerms: Collection<String>): Boolean =
        scan(content, forbiddenTerms).isNotEmpty()
}
