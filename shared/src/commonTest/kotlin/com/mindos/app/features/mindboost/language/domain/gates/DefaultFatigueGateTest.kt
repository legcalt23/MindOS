package com.mindos.app.features.mindboost.language.domain.gates

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins fatigue assessment (§4.4): fresh sessions are low, a long/erroring session with rising
 * latency crosses the high threshold, and the rising-latency marker is detected robustly.
 */
class DefaultFatigueGateTest {

    private val gate = DefaultFatigueGate()

    @Test
    fun freshSessionIsLowFatigue() {
        val state = gate.assess(FatigueSignals())
        assertFalse(state.isHigh)
        assertFalse(state.risingLatency)
    }

    @Test
    fun longErroringSessionWithRisingLatencyIsHighFatigue() {
        val state = gate.assess(
            FatigueSignals(
                recentLatenciesMs = listOf(300, 310, 320, 500, 520, 540),
                itemsCompleted = 40,
                sessionElapsedMs = 1_200_000,
                recentErrorRate = 0.5,
            ),
        )
        assertTrue(state.isHigh)
        assertTrue(state.risingLatency)
    }

    @Test
    fun risingLatencyDetectedOnlyWhenLastWindowIsSlower() {
        val rising = gate.assess(FatigueSignals(recentLatenciesMs = listOf(300, 310, 320, 500, 520, 540)))
        val flat = gate.assess(FatigueSignals(recentLatenciesMs = List(6) { 400L }))
        assertTrue(rising.risingLatency)
        assertFalse(flat.risingLatency)
    }

    @Test
    fun tooFewSamplesNeverReportRisingLatency() {
        val state = gate.assess(FatigueSignals(recentLatenciesMs = listOf(300, 900)))
        assertFalse(state.risingLatency)
    }
}
