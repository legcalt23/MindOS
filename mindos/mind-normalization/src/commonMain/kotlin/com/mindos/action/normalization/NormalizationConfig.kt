package com.mindos.action.normalization

import kotlinx.serialization.Serializable

/**
 * Controls how raw input is normalized before matching/extraction.
 *
 * Design note (ZIP02 §5): normalization is *moderate and locale-aware*, never
 * aggressive. Diacritic folding is OFF by default because stripping accents can
 * create ambiguity in Spanish ("año" vs "ano"). It is opt-in per locale.
 * Free text and entities are NOT normalized identically — see [com.mindos.action.normalization.Token]
 * which carries both a [Token.display] (accent-preserving) and a [Token.normalized] form.
 */
@Serializable
data class NormalizationConfig(
    val localeTag: String = "und",
    val stripControlChars: Boolean = true,
    val applyNfc: Boolean = true,
    val collapseWhitespace: Boolean = true,
    /** When true, the [Token.normalized] form has diacritics folded (á→a, ñ→n). [Token.display] never folds. */
    val foldDiacritics: Boolean = false,
) {
    companion object {
        val Default = NormalizationConfig()
        val English = NormalizationConfig(localeTag = "en")
        /** Spanish defaults: NFC + folding enabled so "café"/"cafe" match for command tokens. */
        val Spanish = NormalizationConfig(localeTag = "es", foldDiacritics = true)
    }
}
