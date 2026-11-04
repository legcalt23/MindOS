package com.mindos.app.features.mindboost.core.timing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * S5 · Paso 5 (F-R3-L) — the versioned shadow config must fail LOUDLY when it is unclassifiable,
 * and must NOT invent invariants across populations.
 *
 * The discriminating halves matter more than the rejections here: (1) the SHIPPED default must
 * construct, or "validated" would just mean "nothing is configurable"; (2) the cross-population
 * comparison the ADR (§R3-18) explicitly rules out as a non-invariant must still construct, or the
 * validator would forbid legitimate configurations while looking rigorous.
 */
class TemporalQualityThresholdsTest {

    @Test
    fun theShippedDefaultConfigIsValid() {
        val defaults = TemporalQualityThresholds.DEFAULT
        assertEquals(TemporalQualityThresholds.VERSION_1, defaults.version)
        assertTrue(defaults.minFramesWindow < defaults.minFramesSession)
        assertTrue(defaults.windowDropCleanMax < defaults.windowDropExcludeMin)
    }

    @Test
    fun aVersionlessConfigIsRejected() {
        // Every classified result carries the version that classified it; a blank version makes the
        // classification unattributable and retroactive re-classification undetectable.
        assertFailsWith<IllegalArgumentException> { TemporalQualityThresholds(version = " ") }
    }

    @Test
    fun aWindowFloorBelowTwoFramesIsRejected() {
        // One frame time yields zero deltas: there is no cadence to classify, so a floor of 1 would
        // let an unclassifiable window through the sample gate.
        assertFailsWith<IllegalArgumentException> { TemporalQualityThresholds(minFramesWindow = 1) }
    }

    @Test
    fun aWindowFloorAtOrAboveTheSessionFloorIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            TemporalQualityThresholds(minFramesWindow = 120, minFramesSession = 120)
        }
    }

    @Test
    fun aCleanCeilingAtOrAboveTheExcludeFloorIsRejected() {
        // Overlap would make one window simultaneously CLEAN and EXCLUDED — the ordering is load-bearing
        // because both quantities are per-window drop fractions of the same population.
        assertFailsWith<IllegalArgumentException> {
            TemporalQualityThresholds(windowDropCleanMax = 0.20, windowDropExcludeMin = 0.15)
        }
        assertFailsWith<IllegalArgumentException> {
            TemporalQualityThresholds(windowDropCleanMax = 0.15, windowDropExcludeMin = 0.15)
        }
    }

    @Test
    fun fractionsOutsideTheUnitIntervalOrNotFiniteAreRejected() {
        assertFailsWith<IllegalArgumentException> { TemporalQualityThresholds(sessionExcludedMax = 1.5) }
        assertFailsWith<IllegalArgumentException> { TemporalQualityThresholds(sessionExcludedMax = -0.1) }
        assertFailsWith<IllegalArgumentException> { TemporalQualityThresholds(cadenceMatchTolerance = Double.NaN) }
        assertFailsWith<IllegalArgumentException> {
            TemporalQualityThresholds(windowDropCleanMax = Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun theCrossPopulationComparisonIsNotTreatedAsAnInvariant() {
        // DISCRIMINATING HALF (ADR §R3-18): `windowDropExcludeMin` is a per-window DROP fraction and
        // `sessionExcludedMax` is a fraction of EXCLUDED WINDOWS — different populations. A validator
        // that ordered them would reject this perfectly coherent config. Its construction is the proof
        // that the validator asserts only same-population orderings.
        val config = TemporalQualityThresholds(windowDropExcludeMin = 0.60, sessionExcludedMax = 0.50)
        assertTrue(config.windowDropExcludeMin > config.sessionExcludedMax)
    }
}
