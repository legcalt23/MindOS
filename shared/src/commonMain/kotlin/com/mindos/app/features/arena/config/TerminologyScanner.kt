package com.mindos.app.features.arena.config

/**
 * Phase 01 / Task 1: Forbidden-terminology scanner (reusable, pure).
 *
 * This is the in-code counterpart of the Gradle `arenaTerminologyScanner` task. It is kept
 * deliberately generic — the forbidden-term list is supplied by the caller — so that this
 * source file itself contains zero forbidden literals and never trips the scanner that walks
 * the arena source tree.
 *
 * Matching is whole-word (word-boundary anchored) and case-insensitive, so benign substrings
 * (e.g. Spanish "modelo" containing the letters e-l-o) are never false positives.
 */
data class TerminologyHit(val term: String, val index: Int)

object TerminologyScanner {

    /** Returns every whole-word, case-insensitive occurrence of any forbidden term in [content]. */
    fun scan(content: String, forbiddenTerms: Collection<String>): List<TerminologyHit> {
        val hits = mutableListOf<TerminologyHit>()
        for (term in forbiddenTerms) {
            if (term.isBlank()) continue
            val regex = Regex("\\b" + Regex.escape(term) + "\\b", RegexOption.IGNORE_CASE)
            for (match in regex.findAll(content)) {
                hits.add(TerminologyHit(term, match.range.first))
            }
        }
        return hits
    }

    /** True if [content] contains any of [forbiddenTerms] as a whole word. */
    fun containsForbidden(content: String, forbiddenTerms: Collection<String>): Boolean =
        scan(content, forbiddenTerms).isNotEmpty()
}
