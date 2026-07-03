package com.mindos.core

data class AliasMatch(
    val actionType: String,
    val localeTag: String,
)

class AliasMatcherImpl(
    private val aliases: List<RouterAliasSpec>,
    private val normalizer: NormalizationPipelineImpl = NormalizationPipelineImpl(),
) {
    private val byKey: Map<String, AliasMatch> = aliases.associate { alias ->
        key(alias.localeTag, normalizer.normalize(alias.text).text) to AliasMatch(alias.actionType, alias.localeTag)
    }

    fun match(input: String, localeTag: String): AliasMatch? {
        val normalized = normalizer.normalize(input).text
        return localeFallbacks(localeTag)
            .firstNotNullOfOrNull { candidate -> byKey[key(candidate, normalized)] }
    }

    private fun key(localeTag: String, normalizedText: String): String =
        "${localeTag.lowercase()}:$normalizedText"

    private fun localeFallbacks(localeTag: String): List<String> {
        val normalized = localeTag.lowercase()
        return listOf(normalized, normalized.substringBefore('-'), "und").distinct()
    }
}
