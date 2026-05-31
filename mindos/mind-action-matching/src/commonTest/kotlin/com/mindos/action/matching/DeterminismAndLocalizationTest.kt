package com.mindos.action.matching

import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.pattern.registrySource
import com.mindos.action.understanding.ActionUnderstandingResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterminismAndLocalizationTest {

    private fun intentOf(r: ActionUnderstandingResult): String =
        (r as ActionUnderstandingResult.Understood).intent.id.value

    @Test
    fun sameInputProducesIdenticalResult() {
        val m = TestRegistries.matcher()
        val a = m.understand("open ana")
        val b = m.understand("open ana")
        assertEquals(a, b) // structural equality (trace off ⇒ pure data)
    }

    @Test
    fun winnerIsIndependentOfRegistrationOrder() {
        val forward = registrySource {
            exact("p1", "open settings", intent = "open_settings")
            parametric("p2", "open {target:username}", intent = "open_profile")
        }
        val reverse = registrySource {
            parametric("p2", "open {target:username}", intent = "open_profile")
            exact("p1", "open settings", intent = "open_settings")
        }
        assertEquals(
            intentOf(TestRegistries.matcher(forward).understand("open settings")),
            intentOf(TestRegistries.matcher(reverse).understand("open settings")),
        )
    }

    @Test
    fun ambiguityIsSurfacedWhenConflictsAllowed() {
        val source = registrySource {
            parametric("a", "open {x:username}", intent = "open_profile")
            parametric("b", "open {x:profile_id}", intent = "open_other")
        }
        val m = TestRegistries.matcher(source, options = CompileOptions(failOnConflicts = false))
        val r = m.understand("open ana")
        assertTrue(r is ActionUnderstandingResult.Ambiguous, "expected Ambiguous, got $r")
        assertEquals(2, (r as ActionUnderstandingResult.Ambiguous).candidates.size)
    }

    @Test
    fun spanishGoldenFoldsAccentsForMatching() {
        val source = registrySource { exact("p_cafe", "café", intent = "coffee") }
        val m = DefaultActionMatcher(
            RegistryCompiler.compile(source, CompileOptions(normalization = NormalizationConfig.Spanish)).getOrThrow(),
        )
        assertEquals("coffee", intentOf(m.understand("cafe")))
        assertEquals("coffee", intentOf(m.understand("CAFÉ")))
        assertEquals("coffee", intentOf(m.understand("  Café  ")))
    }

    @Test
    fun englishGoldenIsCaseInsensitive() {
        val source = registrySource { exact("p", "open settings", intent = "open_settings") }
        val m = DefaultActionMatcher(RegistryCompiler.compile(source).getOrThrow())
        assertEquals("open_settings", intentOf(m.understand("Open Settings")))
        assertEquals("open_settings", intentOf(m.understand("OPEN   SETTINGS")))
    }
}
