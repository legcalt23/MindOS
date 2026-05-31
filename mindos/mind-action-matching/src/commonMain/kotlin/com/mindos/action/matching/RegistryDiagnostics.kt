package com.mindos.action.matching

import com.mindos.action.pattern.ActionPattern.ParametricActionPattern
import com.mindos.action.pattern.PatternToken
import com.mindos.action.pattern.SlotCardinality

/** Human-readable dump of a [CompiledRegistry] for debugging (off the hot path). */
object RegistryDiagnostics {

    fun dump(registry: CompiledRegistry): String = buildString {
        appendLine("CompiledRegistry(schema=v${registry.schema.version}, foldDiacritics=${registry.normalizationConfig.foldDiacritics})")
        appendLine("intents (${registry.intentIds.size}): ${registry.intentIds.map { it.value }}")
        appendLine("exact (${registry.exactCount}):")
        registry.exactIndex.entries()
            .toSortedMap()
            .forEach { (phrase, e) -> appendLine("  \"$phrase\" -> ${e.intentId.value} [prio=${e.priority.value}]") }
        appendLine("aliases (${registry.aliasCount}):")
        registry.aliasIndex.entries()
            .toSortedMap()
            .forEach { (key, e) -> appendLine("  \"$key\" -> ${e.intentId.value}") }
        appendLine("parametric (${registry.parametricCount}):")
        registry.parametricEntries.forEach { pe ->
            appendLine("  ${pe.pattern.id.value}: ${describe(pe.pattern)} -> ${pe.pattern.intentId.value} [prio=${pe.pattern.priority.value}]")
        }
    }

    private fun describe(pattern: ParametricActionPattern): String =
        pattern.tokens.joinToString(" ") { token ->
            when (token) {
                is PatternToken.Static -> token.value
                is PatternToken.Slot -> {
                    val s = token.slot
                    val greedy = if (s.cardinality == SlotCardinality.Greedy) ":greedy" else ""
                    "{${s.name.value}:${s.acceptedType.id}$greedy}"
                }
            }
        }
}
