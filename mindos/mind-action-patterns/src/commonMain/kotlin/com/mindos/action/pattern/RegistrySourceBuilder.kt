package com.mindos.action.pattern

import com.mindos.action.understanding.MindIntentId

/**
 * Fluent, deterministic authoring DSL for a [RegistrySource]. Declaration order is preserved
 * (used as the final stable ranking tie-break — never a lexicographic id compare).
 *
 * Example:
 * ```
 * val source = registrySource {
 *     exact("p_settings_en", "open settings", intent = "open_settings")
 *     alias("ajustes", locale = "es", intent = "open_settings")
 *     parametric("p_open_profile", "open {target:username}", intent = "open_profile")
 * }
 * ```
 */
class RegistrySourceBuilder(private val schema: RegistrySchema = RegistrySchema.Current) {
    private val intents = LinkedHashSet<MindIntentId>()
    private val patterns = ArrayList<PatternDeclaration>()
    private val aliases = ArrayList<AliasSpec>()

    fun exact(
        patternId: String,
        phrase: String,
        intent: String,
        priority: Int = PatternPriority.Default.value,
    ): RegistrySourceBuilder {
        val iid = MindIntentId(intent)
        intents.add(iid)
        patterns.add(PatternDeclaration.Exact(PatternId(patternId), phrase, iid, PatternPriority(priority)))
        return this
    }

    fun parametric(
        patternId: String,
        template: String,
        intent: String,
        priority: Int = PatternPriority.Default.value,
    ): RegistrySourceBuilder {
        val iid = MindIntentId(intent)
        intents.add(iid)
        patterns.add(PatternDeclaration.Parametric(PatternId(patternId), template, iid, PatternPriority(priority)))
        return this
    }

    fun alias(text: String, locale: String, intent: String): RegistrySourceBuilder {
        val iid = MindIntentId(intent)
        intents.add(iid)
        aliases.add(AliasSpec(text, locale, iid))
        return this
    }

    fun build(): RegistrySource = RegistrySource(
        schema = schema,
        declaredIntents = intents.toList(),
        patterns = patterns.toList(),
        aliases = aliases.toList(),
    )
}

/** Entry point for the authoring DSL. */
fun registrySource(
    schema: RegistrySchema = RegistrySchema.Current,
    block: RegistrySourceBuilder.() -> Unit,
): RegistrySource = RegistrySourceBuilder(schema).apply(block).build()
