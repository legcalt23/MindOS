package com.mindos.app.features.mindboost.language.domain.repair

import com.mindos.app.features.mindboost.language.domain.model.ErrorType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the repair MVP (§6.5/§6.6): raw-frequency pattern detection, a complete plan catalog,
 * and outcome branching. Includes the regression that guards against deduplicating errors
 * before counting frequency (CLAUDE.md 2026-06-15).
 */
class DefaultRepairEngineTest {

    private val engine = DefaultRepairEngine()

    @Test
    fun detectPatternCountsRawFrequencyNotDeduplicated() {
        // Two identical impulsive errors MUST be detectable. If frequency were computed over a
        // deduplicated list, the max count would be 1 and the pattern would never trigger.
        val pattern = engine.detectPattern(listOf(ErrorType.INF_OVER, ErrorType.INF_OVER))
        assertTrue(pattern != null)
        assertEquals(ErrorType.INF_OVER, pattern.errorType)
        assertEquals(2, pattern.frequency)
    }

    @Test
    fun singleErrorIsBelowThresholdAndNotAPattern() {
        assertNull(engine.detectPattern(listOf(ErrorType.INF_OVER)))
    }

    @Test
    fun noErrorsYieldsNoPattern() {
        assertNull(engine.detectPattern(emptyList()))
    }

    @Test
    fun dominantPatternIsTheMostFrequentRawError() {
        val pattern = engine.detectPattern(
            listOf(ErrorType.INF_OVER, ErrorType.LIT_DIST, ErrorType.INF_OVER, ErrorType.INF_OVER),
        )
        assertTrue(pattern != null)
        assertEquals(ErrorType.INF_OVER, pattern.errorType)
        assertEquals(3, pattern.frequency)
        assertEquals(RepairSeverity.MEDIUM, pattern.severity)
    }

    @Test
    fun fourOccurrencesAreHighSeverity() {
        val pattern = engine.detectPattern(List(4) { ErrorType.SUM_NO_THESIS })
        assertTrue(pattern != null)
        assertEquals(RepairSeverity.HIGH, pattern.severity)
    }

    @Test
    fun everyErrorTypeHasACompleteRepairPlan() {
        ErrorType.entries.forEach { errorType ->
            val plan = engine.planForError(errorType)
            assertEquals(errorType, plan.errorType)
            assertTrue(plan.explanation.isNotBlank(), "explanation blank for $errorType")
            assertTrue(plan.microLesson.isNotBlank(), "microLesson blank for $errorType")
            assertTrue(plan.isomorphicRetry.isNotBlank(), "isomorphicRetry blank for $errorType")
            assertTrue(plan.verificationCriterion.isNotBlank(), "verification blank for $errorType")
        }
    }

    @Test
    fun planForPatternMatchesPlanForError() {
        val pattern = engine.detectPattern(List(2) { ErrorType.ARG_GAP })!!
        assertEquals(engine.planForError(ErrorType.ARG_GAP), engine.planFor(pattern))
    }

    @Test
    fun outcomeBranchesOnImprovement() {
        val plan = engine.planForError(ErrorType.INF_OVER)
        assertSame(plan.nextIfImprove, engine.resolveOutcome(plan, improved = true).nextAction)
        assertSame(plan.nextIfFail, engine.resolveOutcome(plan, improved = false).nextAction)
    }
}
