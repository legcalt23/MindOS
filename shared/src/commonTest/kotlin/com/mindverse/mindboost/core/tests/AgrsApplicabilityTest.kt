package com.mindverse.mindboost.core.tests

import com.mindverse.mindboost.core.scoring.ScoringEquations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * R7 · `MB-CATALOG-0009` — the AGRS terms are not all measurable in every task, and pretending they
 * are is what made the gate unreachable.
 *
 * `spatialEntropy = 1.0` was doing double duty: "measured, maximally diverse" (an honest user) AND
 * "this task has no spatial grid, so there is nothing to measure" (both Decisiones exercises). The
 * second meaning silently pinned that term at zero while it kept consuming 30% of the weight budget,
 * so the worst possible round scored 0.7 against a 0.75 invalidation threshold — no behaviour could
 * cross it. Same sentinel defect as a `0L` timestamp: one value standing for two different facts.
 *
 * The fix models non-applicability as `null` and renormalizes over the terms the task CAN measure.
 * These tests pin both halves: a fully instrumented task keeps its exact numbers, and a partially
 * instrumented one can now reach its own threshold.
 */
class AgrsApplicabilityTest {

    @Test
    fun aFullyInstrumentedTaskKeepsItsExactWeights_discriminantHalf() {
        // Without this half, "renormalize" could quietly inflate every task's gaming risk. A task that
        // measures all three signals must be numerically identical to before: weights sum to 1.0.
        assertEquals(1.0, agrs(rt = 100L, entropy = 0.0, dialDefault = true), TOLERANCE)
        assertEquals(0.03, agrs(rt = 1_000L, entropy = 0.9, dialDefault = false), TOLERANCE)
        assertEquals(0.43, agrs(rt = 100L, entropy = 0.9, dialDefault = false), TOLERANCE)
        assertEquals(0.0, agrs(rt = 1_000L, entropy = 1.0, dialDefault = false), TOLERANCE)
    }

    @Test
    fun aTaskWithoutASpatialGridRenormalizesOntoTheSignalsItCanMeasure() {
        // Bayes Arena: a slider, no grid. Applicable weight = rt 0.4 + dial 0.3 = 0.7.
        assertEquals(1.0, agrs(rt = 100L, entropy = null, dialDefault = true), TOLERANCE)
        assertEquals(0.3 / 0.7, agrs(rt = 1_000L, entropy = null, dialDefault = true), TOLERANCE)
        assertEquals(0.4 / 0.7, agrs(rt = 100L, entropy = null, dialDefault = false), TOLERANCE)
        assertEquals(0.0, agrs(rt = 1_000L, entropy = null, dialDefault = false), TOLERANCE)
    }

    @Test
    fun aTaskWithOnlyAReactionTimeSignalIsHonestlyJustTheReactionTimeGate() {
        // Reversibility triage: no grid and no dial. The gate is weaker — and that is now VISIBLE
        // instead of hidden behind two terms the task never fed.
        assertEquals(1.0, agrs(rt = 100L, entropy = null, dialDefault = null), TOLERANCE)
        assertEquals(0.0, agrs(rt = 1_000L, entropy = null, dialDefault = null), TOLERANCE)
    }

    @Test
    fun theWorstCaseOfEachDecisionesExerciseNowCrossesTheInvalidationThreshold() {
        // The whole point of the finding: before this, NO combination of behaviour could cross it.
        val bayesWorstCase = agrs(rt = 100L, entropy = null, dialDefault = true)
        val reversibilityWorstCase = agrs(rt = 100L, entropy = null, dialDefault = null)

        assertTrue(
            ScoringEquations.isSessionGamingInvalidated(bayesWorstCase),
            "Bayes worst case $bayesWorstCase must cross ${ScoringEquations.AGRS_INVALID_SESSION_THRESHOLD}",
        )
        assertTrue(
            ScoringEquations.isSessionGamingInvalidated(reversibilityWorstCase),
            "Reversibility worst case $reversibilityWorstCase must cross the same threshold",
        )
    }

    @Test
    fun anHonestUserDrawsZeroRiskUnderEveryApplicabilityCombination_discriminantHalf() {
        // Tightening a gate until nobody survives is not a fix. A deliberate user — slow, dial moved,
        // varied placement — must score exactly zero however many signals the task can measure.
        assertEquals(0.0, agrs(rt = 3_000L, entropy = 1.0, dialDefault = false), TOLERANCE)
        assertEquals(0.0, agrs(rt = 3_000L, entropy = null, dialDefault = false), TOLERANCE)
        assertEquals(0.0, agrs(rt = 3_000L, entropy = null, dialDefault = null), TOLERANCE)
        assertFalse(ScoringEquations.isSessionGamingInvalidated(0.0))
    }

    @Test
    fun theThresholdHasExactlyOneDefinition() {
        // The threshold used to be private, so every test and doc that needed it kept its own copy —
        // the `MB-CATALOG-0007` defect one layer down. It is now readable from the equation itself.
        assertEquals(0.75, ScoringEquations.AGRS_INVALID_SESSION_THRESHOLD, TOLERANCE)
        assertFalse(ScoringEquations.isSessionGamingInvalidated(ScoringEquations.AGRS_INVALID_SESSION_THRESHOLD))
        assertTrue(ScoringEquations.isSessionGamingInvalidated(ScoringEquations.AGRS_INVALID_SESSION_THRESHOLD + 0.01))
    }

    private fun agrs(rt: Long, entropy: Double?, dialDefault: Boolean?): Double =
        ScoringEquations.calculateAGRS(
            reactionTimeMs = rt,
            spatialEntropy = entropy,
            isDialDefault = dialDefault,
        )

    private companion object {
        const val TOLERANCE = 0.0001
    }
}
