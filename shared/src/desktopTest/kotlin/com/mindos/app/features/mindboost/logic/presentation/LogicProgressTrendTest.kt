package com.mindos.app.features.mindboost.logic.presentation

import com.mindos.app.features.mindboost.logic.presentation.state.LogicProgressTrend
import kotlin.test.Test
import kotlin.test.assertEquals

class LogicProgressTrendTest {

    @Test
    fun unknown_withTooFewSessions() {
        assertEquals(LogicProgressTrend.UNKNOWN, logicIcrTrend(listOf(10.0, 20.0, 30.0)))
    }

    @Test
    fun improving_whenRecentSessionsHigher() {
        // Oldest-first: older half ~41, newer half ~61 → +20 ICR points.
        assertEquals(LogicProgressTrend.IMPROVING, logicIcrTrend(listOf(40.0, 42.0, 60.0, 62.0)))
    }

    @Test
    fun declining_whenRecentSessionsLower() {
        assertEquals(LogicProgressTrend.DECLINING, logicIcrTrend(listOf(60.0, 62.0, 40.0, 42.0)))
    }

    @Test
    fun stable_whenWithinEpsilon() {
        assertEquals(LogicProgressTrend.STABLE, logicIcrTrend(listOf(50.0, 51.0, 50.0, 51.0)))
    }
}
