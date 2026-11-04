package com.mindos.app.features.mindboost.logic.presentation

import com.mindos.app.features.mindboost.logic.domain.model.LogicScoringVersion
import com.mindos.app.features.mindboost.logic.presentation.state.LogicProgressTrend
import kotlin.test.Test
import kotlin.test.assertEquals

class LogicProgressTrendTest {

    @Test
    fun unknown_withTooFewSessions() {
        assertEquals(LogicProgressTrend.UNKNOWN, logicIcrTrend(sameInstrument(10.0, 20.0, 30.0)))
    }

    @Test
    fun improving_whenRecentSessionsHigher() {
        // Oldest-first: older half ~41, newer half ~61 → +20 ICR points.
        assertEquals(LogicProgressTrend.IMPROVING, logicIcrTrend(sameInstrument(40.0, 42.0, 60.0, 62.0)))
    }

    @Test
    fun declining_whenRecentSessionsLower() {
        assertEquals(LogicProgressTrend.DECLINING, logicIcrTrend(sameInstrument(60.0, 62.0, 40.0, 42.0)))
    }

    @Test
    fun stable_whenWithinEpsilon() {
        assertEquals(LogicProgressTrend.STABLE, logicIcrTrend(sameInstrument(50.0, 51.0, 50.0, 51.0)))
    }

    @Test
    fun unknown_whenSeriesStraddlesAScoringVersionBoundary() {
        // The user's underlying performance is IDENTICAL in both halves — a real ICR of 41.0 all the
        // way through. The older half was recorded by the pre-fix scorer, which credited a conceptual
        // error 13.5 points more than it should have. Nothing about the person changed; only our
        // instrument did, so no direction may be claimed.
        val inflatedOld = 54.5
        val correctNew = 41.0
        val straddling = listOf(
            LogicIcrPoint(inflatedOld, LogicScoringVersion.LEGACY_PRECEDENCE_BUG),
            LogicIcrPoint(inflatedOld, LogicScoringVersion.LEGACY_PRECEDENCE_BUG),
            LogicIcrPoint(correctNew, LogicScoringVersion.CURRENT),
            LogicIcrPoint(correctNew, LogicScoringVersion.CURRENT),
        )
        assertEquals(LogicProgressTrend.UNKNOWN, logicIcrTrend(straddling))

        // Discriminating half, deliberately built from the SAME numbers: measured end to end by ONE
        // instrument, that series IS a real decline and must still be reported as one. What suppresses
        // the verdict has to be the version boundary, not the magnitude of the drop — without this,
        // returning UNKNOWN unconditionally would satisfy the assertion above.
        assertEquals(
            LogicProgressTrend.DECLINING,
            logicIcrTrend(sameInstrument(inflatedOld, inflatedOld, correctNew, correctNew)),
        )
    }

    private fun sameInstrument(vararg icrs: Double): List<LogicIcrPoint> =
        icrs.map { LogicIcrPoint(it, LogicScoringVersion.CURRENT) }
}
