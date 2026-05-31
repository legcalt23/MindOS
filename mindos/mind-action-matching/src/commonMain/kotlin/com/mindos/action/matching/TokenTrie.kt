package com.mindos.action.matching

import com.mindos.action.normalization.Token
import com.mindos.action.pattern.ActionPattern.ParametricActionPattern
import com.mindos.action.pattern.ParameterSlot
import com.mindos.action.pattern.PatternToken
import com.mindos.action.pattern.SlotCardinality

/** A parametric pattern plus its stable declaration order. */
internal data class ParametricEntry(
    val pattern: ParametricActionPattern,
    val declarationOrder: Int,
)

/** One structural match: a terminal pattern and the token span bound to each slot, in order. */
internal data class TrieMatch(
    val entry: ParametricEntry,
    val slotSpans: List<IntRange>,
)

private class TrieNode {
    val staticChildren = HashMap<String, TrieNode>()
    val slotBranches = ArrayList<SlotBranch>()
    val terminals = ArrayList<ParametricEntry>()
}

private class SlotBranch(val slot: ParameterSlot, val child: TrieNode)

/**
 * Static-token trie with typed slot branches. Patterns sharing a prefix share nodes, so the
 * candidate set walked per request is proportional to input length (O(k)), not the registry
 * size (O(N)). Slot branches are keyed by (type, cardinality); slot *names* come from the
 * terminal pattern, so spans align positionally with `pattern.slots`.
 */
internal class TokenTrie {
    private val root = TrieNode()

    fun insert(entry: ParametricEntry, normalizeStatic: (String) -> String) {
        var node = root
        for (token in entry.pattern.tokens) {
            node = when (token) {
                is PatternToken.Static -> node.staticChildren.getOrPut(normalizeStatic(token.value)) { TrieNode() }
                is PatternToken.Slot -> {
                    val slot = token.slot
                    val existing = node.slotBranches.firstOrNull {
                        it.slot.acceptedType == slot.acceptedType && it.slot.cardinality == slot.cardinality
                    }
                    existing?.child ?: TrieNode().also { node.slotBranches.add(SlotBranch(slot, it)) }
                }
            }
        }
        node.terminals.add(entry)
    }

    /** All structural matches for the input tokens. Pure; no entity validation here. */
    fun walk(tokens: List<Token>): List<TrieMatch> {
        val out = ArrayList<TrieMatch>()
        walkFrom(root, tokens, 0, emptyList(), out)
        return out
    }

    private fun walkFrom(
        node: TrieNode,
        tokens: List<Token>,
        index: Int,
        spans: List<IntRange>,
        out: MutableList<TrieMatch>,
    ) {
        if (index == tokens.size) {
            for (terminal in node.terminals) {
                if (spans.size == terminal.pattern.slots.size) out.add(TrieMatch(terminal, spans))
            }
            return
        }
        // Static descent.
        node.staticChildren[tokens[index].normalized]?.let {
            walkFrom(it, tokens, index + 1, spans, out)
        }
        // Slot descents.
        for (branch in node.slotBranches) {
            when (branch.slot.cardinality) {
                SlotCardinality.Single ->
                    walkFrom(branch.child, tokens, index + 1, spans + listOf(index..index), out)
                SlotCardinality.Greedy ->
                    walkFrom(branch.child, tokens, tokens.size, spans + listOf(index until tokens.size), out)
            }
        }
    }
}
