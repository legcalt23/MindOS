package com.mindos.action.pattern

import com.mindos.action.understanding.MindIntentId
import kotlinx.serialization.Serializable

/** Registry schema version. Enables migration checks and id-rename detection over time. */
@Serializable
data class RegistrySchema(val version: Int) {
    companion object {
        val Current = RegistrySchema(1)
    }
}

/** An alias canonicalized **by intent and locale** (not bound to a specific pattern). */
@Serializable
data class AliasSpec(
    val text: String,
    val localeTag: String,
    val intentId: MindIntentId,
)

/**
 * Source declaration of a pattern. Kept un-parsed (templates as strings) so the compiler can
 * parse + validate everything and report all errors at once.
 */
sealed interface PatternDeclaration {
    val patternId: PatternId
    val intentId: MindIntentId
    val priority: PatternPriority

    data class Exact(
        override val patternId: PatternId,
        val phrase: String,
        override val intentId: MindIntentId,
        override val priority: PatternPriority = PatternPriority.Default,
    ) : PatternDeclaration

    data class Parametric(
        override val patternId: PatternId,
        val template: String,
        override val intentId: MindIntentId,
        override val priority: PatternPriority = PatternPriority.Default,
    ) : PatternDeclaration
}

/**
 * The declarative registry input. The RegistryCompiler turns this into a CompiledRegistry,
 * validating schema, conflicts, slot extractors and locale coverage along the way.
 */
data class RegistrySource(
    val schema: RegistrySchema,
    val declaredIntents: List<MindIntentId>,
    val patterns: List<PatternDeclaration>,
    val aliases: List<AliasSpec>,
)
