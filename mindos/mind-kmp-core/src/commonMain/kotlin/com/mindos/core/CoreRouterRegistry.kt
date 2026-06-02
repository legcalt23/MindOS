package com.mindos.core

import com.mindos.action.pattern.registrySource

enum class PatternKind {
    EXACT,
    PARAMETRIC,
}

data class RouterPatternSpec(
    val id: String,
    val text: String,
    val actionType: String,
    val kind: PatternKind,
    val priority: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "pattern id must not be blank." }
        require(text.isNotBlank()) { "pattern text must not be blank." }
        require(actionType.isNotBlank()) { "actionType must not be blank." }
    }
}

data class RouterAliasSpec(
    val text: String,
    val localeTag: String,
    val actionType: String,
) {
    init {
        require(text.isNotBlank()) { "alias text must not be blank." }
        require(localeTag.isNotBlank()) { "localeTag must not be blank." }
        require(actionType.isNotBlank()) { "actionType must not be blank." }
    }
}

data class CoreRouterRegistry(
    val patterns: List<RouterPatternSpec>,
    val aliases: List<RouterAliasSpec> = emptyList(),
) {
    init {
        require(patterns.isNotEmpty() || aliases.isNotEmpty()) { "registry must contain at least one pattern or alias." }
    }

    internal fun toRegistrySource() = registrySource {
        patterns.forEach { pattern ->
            when (pattern.kind) {
                PatternKind.EXACT -> exact(pattern.id, pattern.text, pattern.actionType, pattern.priority)
                PatternKind.PARAMETRIC -> parametric(pattern.id, pattern.text, pattern.actionType, pattern.priority)
            }
        }
        aliases.forEach { alias ->
            alias(alias.text, alias.localeTag, alias.actionType)
        }
    }

    companion object {
        fun default(): CoreRouterRegistry = CoreActionRegistry.default().routerRegistry
    }
}
