package com.mindos.commandrouter.patterns

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExactPatternTest {

    private fun pattern(cmd: String, priority: Int = 0) =
        ExactCommandPattern(PatternId("p"), cmd, PatternPriority(priority))

    @Test
    fun matchesExactInput() {
        val m = pattern("open settings").match("open settings")
        assertEquals(PatternId("p"), m?.patternId)
        assertEquals(emptyMap(), m?.extractedParameters)
    }

    @Test
    fun noMatchOnDifferentInput() {
        assertNull(pattern("open settings").match("open home"))
    }

    @Test
    fun noMatchOnPartialInput() {
        assertNull(pattern("open settings").match("open"))
    }

    @Test
    fun specificityIsAllStatic() {
        val s = pattern("open settings").specificity()
        assertEquals(2, s.staticTokenCount)
        assertEquals(0, s.parameterCount)
    }
}
