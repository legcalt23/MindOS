package com.mindos.app.features.mindboost.core.copy

/** A whole-word, accent- and case-insensitive match of a forbidden term in scanned copy. */
data class CopySafetyHit(val term: String, val index: Int)

/**
 * INFRA-05 · Pure forbidden-copy matcher shared by ALL MindBoost modules. In-code counterpart of the
 * Gradle `mindboostCopySafetyScanner` task (Phase 3): same matching rules, parity-tested, so the build
 * gate and the runtime compliance tests agree.
 *
 * Matching (mirrors [com.mindos.app.features.mindboost.emotions.domain.safety.EmotionTerminologyScanner]):
 *  - whole-word (`\b`-anchored), so benign substrings never false-positive;
 *  - case-insensitive AND Spanish-accent-insensitive (small pure-Kotlin fold map, because
 *    `java.text.Normalizer` is JVM-only and unusable in `commonMain`);
 *  - deliberately hyphen-/negation-crossing: `\b` treats `-` as a boundary, so a forbidden root inside
 *    "non-clinical" / "anti-fake-news" is still caught (invariant #c — zero claims even to negate them).
 *
 * This file intentionally contains ZERO forbidden literals (the term list comes from [ForbiddenLexicon]
 * or the caller), so it never trips a scan of its own source tree.
 */
object MindBoostCopySafetyScanner {

    private val ACCENT_FOLD: Map<Char, Char> = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
    )

    /**
     * Lower-case then strip the Spanish diacritics (1:1, index-preserving).
     *
     * CANONICAL, and public on purpose: `MB-EMO-0032` folded the AI coach's safety gate onto this
     * function instead of leaving it a fourth hand-rolled copy, so a runtime gate that classifies
     * crisis/self-harm requests now depends on this table. Do not narrow its visibility, and do not
     * REMOVE a pair without reading `AICoachSafetyPolicyTest` first — five of the seven are
     * load-bearing there for real Spanish spellings, two of them on the crisis path. Adding a pair is
     * safe in that direction (more folding matches more, which for a blocking gate is fail-closed),
     * but it must still be mirrored into the two Gradle-side folds, which cannot import this code;
     * `CopySafetyLexiconParityTest` is what fails when it is not.
     */
    fun fold(raw: String): String {
        val lowered = raw.lowercase()
        return buildString(lowered.length) {
            for (ch in lowered) append(ACCENT_FOLD[ch] ?: ch)
        }
    }

    /** Every whole-word occurrence of any [forbiddenTerms] in [content] (folded comparison). */
    fun scan(content: String, forbiddenTerms: Collection<String>): List<CopySafetyHit> {
        val foldedContent = fold(content)
        val hits = mutableListOf<CopySafetyHit>()
        for (term in forbiddenTerms) {
            if (term.isBlank()) continue
            val regex = Regex("\\b" + Regex.escape(fold(term)) + "\\b")
            for (match in regex.findAll(foldedContent)) {
                hits.add(CopySafetyHit(term, match.range.first))
            }
        }
        return hits
    }

    /** Every whole-word occurrence of any [ForbiddenLexicon.ALL] term in [content]. */
    fun scanAll(content: String): List<CopySafetyHit> = scan(content, ForbiddenLexicon.ALL)

    /** True if [content] contains any of [forbiddenTerms] as a whole word. */
    fun containsForbidden(content: String, forbiddenTerms: Collection<String>): Boolean =
        scan(content, forbiddenTerms).isNotEmpty()

    /** True if [content] contains any [ForbiddenLexicon.ALL] term as a whole word. */
    fun containsForbidden(content: String): Boolean = scanAll(content).isNotEmpty()
}
