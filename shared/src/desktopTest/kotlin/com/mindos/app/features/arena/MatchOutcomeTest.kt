package com.mindos.app.features.arena

import com.mindos.app.features.arena.rating.engine.BlendingConfig
import com.mindos.app.features.arena.rating.engine.MatchOutcomeCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 03 remainder / P3R.1: Match-outcome path + blending. */
class MatchOutcomeTest {

    private val calc = MatchOutcomeCalculator()

    @Test
    fun equalPlayersExpectHalf() {
        assertEquals(0.5, calc.calculateExpectedMatchOutcome(1500.0, 1500.0, 100.0, 100.0), 1e-9)
    }

    @Test
    fun strongerPlayerExpectsMoreThanHalfAndIsMonotonic() {
        val slightFavorite = calc.calculateExpectedMatchOutcome(1600.0, 1500.0, 50.0, 50.0)
        val bigFavorite = calc.calculateExpectedMatchOutcome(1800.0, 1500.0, 50.0, 50.0)
        assertTrue(slightFavorite > 0.5)
        assertTrue(bigFavorite > slightFavorite)
    }

    @Test
    fun higherUncertaintyPullsExpectationTowardHalf() {
        val confident = calc.calculateExpectedMatchOutcome(1600.0, 1500.0, 50.0, 50.0)
        val uncertain = calc.calculateExpectedMatchOutcome(1600.0, 1500.0, 350.0, 350.0)
        assertTrue(uncertain > 0.5)
        assertTrue(uncertain < confident, "uncertain=$uncertain confident=$confident")
    }

    @Test
    fun matchDeltaIsObservedMinusExpected() {
        assertEquals(0.4, calc.matchOutcomeDelta(1.0, 0.6), 1e-9)
        assertFailsWith<IllegalArgumentException> { calc.matchOutcomeDelta(1.5, 0.6) }
    }

    @Test
    fun blendRespectsWeights() {
        assertEquals(0.2, calc.blend(0.2, 0.8, BlendingConfig.PURE_TASK), 1e-9)
        assertEquals(0.5, calc.blend(0.2, 0.8, BlendingConfig.BALANCED_PVP), 1e-9)
    }

    @Test
    fun blendingWeightsMustSumToOne() {
        assertFailsWith<IllegalArgumentException> { BlendingConfig(taskWeight = 0.7, matchWeight = 0.7) }
    }
}
