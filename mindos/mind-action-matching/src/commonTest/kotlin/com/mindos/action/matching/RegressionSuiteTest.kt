package com.mindos.action.matching

import com.mindos.action.normalization.NormalizationConfig
import com.mindos.action.pattern.registrySource
import com.mindos.action.understanding.ActionUnderstandingResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Permanent regression suite (ZIP11 REGRESSION_SUITE): **every production bug becomes a permanent
 * case here and is never removed.** Each case pins a behaviour the engine must keep forever. Add new
 * cases with a stable id + a short note describing the bug they guard against.
 */
class RegressionSuiteTest {

    private data class RegressionCase(val id: String, val note: String, val check: () -> Unit)

    private val zeroWidth = Char(0x200B).toString()

    private val cases: List<RegressionCase> = listOf(
        RegressionCase("REG-001", "invalid entity must not become a false positive") {
            assertTrue(Reliability.matcher.understand("open \$\$\$") is ActionUnderstandingResult.NoMatch)
        },
        RegressionCase("REG-002", "exact phrase outranks parametric of equal/lower priority") {
            val r = Reliability.matcher.understand("open settings")
            assertEquals("open_settings", (r as ActionUnderstandingResult.Understood).intent.id.value)
        },
        RegressionCase("REG-003", "zero-width/format char injection is handled safely (security guard rejects spoofing; never a wrong match)") {
            val r = Reliability.matcher.understand("open set${zeroWidth}tings")
            assertTrue(
                r is ActionUnderstandingResult.Rejected || r is ActionUnderstandingResult.NoMatch,
                "zero-width injection must not resolve to a spurious intent (got $r)",
            )
        },
        RegressionCase("REG-004", "same input is fully deterministic") {
            assertEquals(Reliability.matcher.understand("open ana"), Reliability.matcher.understand("open ana"))
        },
        RegressionCase("REG-005", "case + extra spacing are normalized away") {
            val r = Reliability.matcher.understand("   OPEN    SETTINGS  ")
            assertEquals("open_settings", (r as ActionUnderstandingResult.Understood).intent.id.value)
        },
        RegressionCase("REG-006", "Spanish diacritic folding matches accented/unaccented forms") {
            val es = DefaultActionMatcher(
                RegistryCompiler.compile(
                    registrySource { exact("p_cafe", "café", intent = "coffee") },
                    CompileOptions(normalization = NormalizationConfig.Spanish),
                ).getOrThrow(),
            )
            assertEquals("coffee", (es.understand("cafe") as ActionUnderstandingResult.Understood).intent.id.value)
            assertEquals("coffee", (es.understand("CAFÉ") as ActionUnderstandingResult.Understood).intent.id.value)
        },
    )

    @Test
    fun allRegressionCasesHold() {
        val failures = ArrayList<String>()
        for (case in cases) {
            try {
                case.check()
            } catch (e: Throwable) {
                failures += "${case.id} (${case.note}): ${e.message}"
            }
        }
        assertTrue(failures.isEmpty(), "regression failures:\n" + failures.joinToString("\n"))
    }
}
