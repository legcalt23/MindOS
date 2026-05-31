package com.mindos.testkit

/** One template for an intent. `{slot}` placeholders are filled from [CommandGrammar.slots]. */
data class CommandTemplate(val intentId: String, val template: String)

/**
 * A grammar describing how to generate *valid* commands for a registry. Engine-agnostic: the test
 * supplies templates + slot value pools that mirror its real registry, so generated commands have a
 * known expected intent.
 */
data class CommandGrammar(
    val templates: List<CommandTemplate>,
    val slots: Map<String, List<String>>,
)

/** A generated valid command and the intent it should resolve to. */
data class GeneratedCommand(
    val text: String,
    val expectedIntentId: String,
    val slotValues: Map<String, String>,
)

/** Deterministically generates valid command strings from a [CommandGrammar]. */
class CommandGenerator(private val grammar: CommandGrammar) {

    init {
        require(grammar.templates.isNotEmpty()) { "grammar has no templates" }
    }

    fun generate(rng: SeededRng): GeneratedCommand {
        val template = rng.pick(grammar.templates)
        val slotValues = LinkedHashMap<String, String>()
        val text = template.template.split(' ').filter { it.isNotEmpty() }.joinToString(" ") { token ->
            val slotName = slotName(token)
            if (slotName != null) {
                val pool = grammar.slots[slotName] ?: error("no pool for slot '$slotName'")
                val value = rng.pick(pool)
                slotValues[slotName] = value
                value
            } else {
                token
            }
        }
        return GeneratedCommand(text, template.intentId, slotValues)
    }

    private fun slotName(token: String): String? =
        if (token.length > 2 && token.startsWith('{') && token.endsWith('}')) token.substring(1, token.length - 1) else null
}
