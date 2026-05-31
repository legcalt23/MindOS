package com.mindos.action.pattern

import com.mindos.action.entity.EntityType
import com.mindos.action.understanding.SlotName

/**
 * Parses a human-authored template into typed [PatternToken]s.
 *
 * Syntax:
 * - static word: `open`, `story`
 * - typed slot:  `{name:typeId}`  e.g. `{target:username}`
 * - greedy slot: `{name:typeId:greedy}` (only meaningful for `free_text`)
 *
 * Returns a typed [Result]; the RegistryCompiler collects failures rather than throwing,
 * so all template errors surface together at build time.
 */
object PatternTemplate {

    sealed interface Result {
        data class Ok(val tokens: List<PatternToken>) : Result
        data class Error(val message: String) : Result
    }

    // Single bounded groups → linear, no catastrophic backtracking.
    private val SLOT = Regex("^\\{([a-zA-Z_][a-zA-Z0-9_]*):([a-z_]+)(?::(greedy))?\\}$")

    fun parse(template: String): Result {
        val trimmed = template.trim()
        if (trimmed.isEmpty()) return Result.Error("template is empty")

        val rawTokens = trimmed.split(' ').filter { it.isNotEmpty() }
        if (rawTokens.isEmpty()) return Result.Error("template has no tokens")

        val tokens = ArrayList<PatternToken>(rawTokens.size)
        for (raw in rawTokens) {
            if (raw.startsWith('{') || raw.endsWith('}')) {
                val m = SLOT.matchEntire(raw)
                    ?: return Result.Error("malformed slot '$raw' (expected {name:type} or {name:type:greedy})")
                val name = m.groupValues[1]
                val typeId = m.groupValues[2]
                val greedy = m.groupValues[3] == "greedy"
                val type = EntityType.byId(typeId)
                    ?: return Result.Error("unknown entity type '$typeId' in slot '$raw'")
                val cardinality = if (greedy) SlotCardinality.Greedy else SlotCardinality.Single
                tokens.add(
                    PatternToken.Slot(
                        ParameterSlot(name = SlotName(name), acceptedType = type, cardinality = cardinality),
                    ),
                )
            } else {
                // Static value kept as-authored; the compiler normalizes it for indexing.
                tokens.add(PatternToken.Static(raw))
            }
        }
        return Result.Ok(tokens)
    }
}
