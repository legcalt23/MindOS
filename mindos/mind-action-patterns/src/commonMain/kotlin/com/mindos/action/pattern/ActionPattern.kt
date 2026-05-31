package com.mindos.action.pattern

import com.mindos.action.entity.EntityType
import com.mindos.action.understanding.MindIntentId

/** A token in a parsed parametric pattern: a fixed word or a typed slot. */
sealed interface PatternToken {
    data class Static(val value: String) : PatternToken
    data class Slot(val slot: ParameterSlot) : PatternToken
}

/**
 * The **compiled, typed** pattern form (post-parse). Produced by the RegistryCompiler from a
 * [PatternDeclaration]. The understanding engine matches against these, never against raw text.
 */
sealed interface ActionPattern {
    val id: PatternId
    val intentId: MindIntentId
    val priority: PatternPriority

    /** A literal phrase. Matched via the O(1) exact index. */
    data class ExactActionPattern(
        override val id: PatternId,
        val phrase: String,
        override val intentId: MindIntentId,
        override val priority: PatternPriority = PatternPriority.Default,
    ) : ActionPattern

    /** A sequence of static tokens + typed slots. Matched via the token trie. */
    data class ParametricActionPattern(
        override val id: PatternId,
        val tokens: List<PatternToken>,
        override val intentId: MindIntentId,
        override val priority: PatternPriority = PatternPriority.Default,
    ) : ActionPattern {
        val slots: List<ParameterSlot> = tokens.mapNotNull { (it as? PatternToken.Slot)?.slot }
        val staticTokenCount: Int = tokens.count { it is PatternToken.Static }
        val freeTextSlotCount: Int = slots.count { it.acceptedType == EntityType.FreeText }
        val typedSlotCount: Int = slots.size - freeTextSlotCount
    }
}
