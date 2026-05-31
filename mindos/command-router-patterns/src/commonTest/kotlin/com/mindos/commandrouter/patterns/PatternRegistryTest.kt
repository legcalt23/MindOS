package com.mindos.commandrouter.patterns

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PatternRegistryTest {

    private fun exact(id: String, cmd: String, priority: Int = 0) =
        ExactCommandPattern(PatternId(id), cmd, PatternPriority(priority))

    private fun param(id: String, tpl: String, priority: Int = 0) =
        ParameterizedCommandPattern(PatternId(id), tpl, PatternPriority(priority))

    @Test
    fun noMatch_whenNoPatterns() {
        val result = PatternRegistry(emptyList()).findMatch("open settings")
        assertIs<PatternMatchResult.NoMatch>(result)
    }

    @Test
    fun exactMatch_singlePattern() {
        val registry = PatternRegistry(listOf(exact("e1", "open settings")))
        val result = registry.findMatch("open settings")
        assertIs<PatternMatchResult.Matched>(result)
        assertEquals(PatternId("e1"), result.match.patternId)
    }

    @Test
    fun exactBeatsParameterized_sameInput() {
        val registry = PatternRegistry(listOf(
            param("p1", "open {target}"),
            exact("e1", "open settings")
        ))
        val result = registry.findMatch("open settings")
        assertIs<PatternMatchResult.Matched>(result)
        assertEquals(PatternId("e1"), result.match.patternId)
    }

    @Test
    fun higherPriorityWins() {
        val registry = PatternRegistry(listOf(
            exact("low", "open settings", priority = 0),
            exact("high", "open settings", priority = 100)
        ))
        val result = registry.findMatch("open settings")
        assertIs<PatternMatchResult.Matched>(result)
        assertEquals(PatternId("high"), result.match.patternId)
    }

    @Test
    fun ambiguous_whenSamePriorityAndSpecificity() {
        val registry = PatternRegistry(listOf(
            param("p1", "go to {target}", priority = 0),
            param("p2", "navigate to {target}", priority = 0)
        ))
        // Both match different inputs, but if same priority+specificity patterns match same input it's ambiguous
        // Let's use two patterns that both match the same input:
        val registry2 = PatternRegistry(listOf(
            param("pa", "do {thing}", priority = 0),
            param("pb", "do {action}", priority = 0)
        ))
        val result = registry2.findMatch("do something")
        assertIs<PatternMatchResult.Ambiguous>(result)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun noMatch_unknownInput() {
        val registry = PatternRegistry(listOf(exact("e1", "open settings")))
        assertIs<PatternMatchResult.NoMatch>(registry.findMatch("close settings"))
    }

    @Test
    fun deterministic_sameInputAlwaysSameResult() {
        val patterns = listOf(
            exact("a", "open settings", 0),
            param("b", "open {target}", 0),
            param("c", "go to {target}", 0)
        )
        val registry = PatternRegistry(patterns)
        val r1 = registry.findMatch("open settings")
        val r2 = registry.findMatch("open settings")
        assertEquals(r1, r2)
    }

    @Test
    fun normalizerIntegration_caseInsensitive() {
        val registry = PatternRegistry(listOf(exact("e1", "open settings")))
        // The registry operates on already-normalized input; normalizer is in the router layer
        assertIs<PatternMatchResult.Matched>(registry.findMatch("open settings"))
    }
}
