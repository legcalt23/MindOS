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
        return byKey[key(localeTag, normalized)] ?: byKey[key("und", normalized)]
    }

    private fun key(localeTag: String, normalizedText: String): String = "$localeTag:$normalizedText"
}
