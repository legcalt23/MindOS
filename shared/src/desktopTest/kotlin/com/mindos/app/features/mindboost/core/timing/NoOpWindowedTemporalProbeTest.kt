package com.mindos.app.features.mindboost.core.timing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * ADR-FOCUS-TEMPORAL-QUALITY §17 [R2-HIST] criterio 7 — a no-op probe reports its fallback capability as a
 * first-class fact and never yields an observation, so combined with [TemporalClassPolicy] it can
 * never classify above T0 nor produce a CLEAN window.
 */
class NoOpWindowedTemporalProbeTest {

    @Test
    fun noOpProbe_reportsFallbackCapability_andNeverObserves() {
        val probe = NoOpWindowedTemporalProbe
        probe.start()
        probe.markPresent()
        assertNull(probe.drainWindow(), "a no-op probe must never fabricate an observation")
        probe.stop()
        assertEquals(ProbeCapability.NONE_MONOTONIC_FALLBACK, probe.capability)
        assertEquals(ClockDomain.NONE_MONOTONIC_FALLBACK, probe.clockDomain)
    }

    @Test
    fun noOpProbe_throughPolicy_isT0_andNoObservation() {
        val probe = NoOpWindowedTemporalProbe
        val clazz = TemporalClassPolicy.sessionClass(
            capability = probe.capability,
            clockDomain = probe.clockDomain,
            sampledFramesSession = 10_000,
        )
        assertEquals(TemporalClass.T0_NON_AUTHORITATIVE, clazz)
        assertEquals(
            TemporalWindowVerdict.NoObservation,
            TemporalClassPolicy.windowIntegrity(probe.drainWindow()),
        )
    }
}
