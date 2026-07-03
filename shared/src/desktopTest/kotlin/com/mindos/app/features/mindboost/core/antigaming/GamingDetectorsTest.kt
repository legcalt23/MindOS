package com.mindos.app.features.mindboost.core.antigaming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** INFRA-03 · false-positive control + registry completeness (adversary triggers live in the red-team). */
class GamingDetectorsTest {

    @Test
    fun cleanDefaultTelemetry_triggersNoDetector() {
        val clean = TrialTelemetry()
        for (detector in GamingDetectors.all) {
            assertFalse(
                detector.evaluate(clean).triggered,
                "${detector.id} false-positived on clean default telemetry",
            )
        }
    }

    @Test
    fun registry_hasAllElevenDistinctDetectors() {
        assertEquals(11, GamingDetectors.all.size)
        assertEquals(11, GamingDetectors.all.map { it.id }.toSet().size, "detector ids must be distinct")
    }

    @Test
    fun untriggeredSignal_carriesNoneAction() {
        val signal = GamingDetectors.all.first().evaluate(TrialTelemetry())
        assertEquals(GamingAction.NONE, signal.action)
        assertFalse(signal.triggered)
    }
}
