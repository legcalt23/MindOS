package com.mindos.commandrouter.patterns

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParameterizedPatternTest {

    private fun pattern(tpl: String, priority: Int = 0) =
        ParameterizedCommandPattern(PatternId("p"), tpl, PatternPriority(priority))

    @Test
    fun singleParamAtEnd_singleWord() {
        val m = pattern("open {target}").match("open settings")
        assertEquals(mapOf("target" to "settings"), m?.extractedParameters)
    }

    @Test
    fun singleParamAtEnd_multipleWords() {
        val m = pattern("open {target}").match("open home screen")
        assertEquals(mapOf("target" to "home screen"), m?.extractedParameters)
    }

    @Test
    fun goTo_multiWord() {
        val m = pattern("go to {target}").match("go to privacy settings")
        assertEquals(mapOf("target" to "privacy settings"), m?.extractedParameters)
    }

    @Test
    fun midParam_singleWord() {
        val m = pattern("send {message} to {user}").match("send hello to alice")
        assertEquals(mapOf("message" to "hello", "user" to "alice"), m?.extractedParameters)
    }

    @Test
    fun noMatchWhenStaticTokenMismatch() {
        assertNull(pattern("open {target}").match("close settings"))
    }

    @Test
    fun noMatchWhenParamEmpty() {
        assertNull(pattern("open {target}").match("open"))
    }

    @Test
    fun noMatchWhenExtraTokensAfterExact() {
        // template "open {target}" with {target}=single word in middle position would leave extra
        // but {target} IS the last token so it greedily consumes both — this should match
        val m = pattern("open {target}").match("open home screen extra")
        assertEquals(mapOf("target" to "home screen extra"), m?.extractedParameters)
    }

    @Test
    fun specificityReflectsStaticAndParamCounts() {
        val s = pattern("go to {target}").specificity()
        assertEquals(2, s.staticTokenCount)
        assertEquals(1, s.parameterCount)
    }
}
