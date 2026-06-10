package com.mindos.app.features.mindboost.memory.domain.reporting

import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryFitnessCalculatorTest {

    private val calc = MemoryFitnessCalculator()

    @Test
    fun emptyInput_isNotConfidentAndNull() {
        val report = calc.compute(emptyList())
        assertNull(report.overall.value)
        assertFalse(report.overall.confident)
        assertTrue(report.perSubcapacity.isEmpty())
    }

    @Test
    fun reportsPerSubcapacity() {
        val report = calc.compute(
            listOf(
                SubcapacityScore(MemorySubcapacity.WORKING_MEMORY, value = 70.0, n = 30),
                SubcapacityScore(MemorySubcapacity.EPISODIC, value = 50.0, n = 30),
            ),
        )
        assertEquals(2, report.perSubcapacity.size)
        assertEquals(60.0, report.overall.value!!, 1e-9) // mean(70,50)
        assertTrue(report.overall.confident)
    }

    @Test
    fun smallSample_widensBandAndIsProvisional() {
        val report = calc.compute(
            listOf(
                SubcapacityScore(MemorySubcapacity.WORKING_MEMORY, value = 70.0, n = 3),
                SubcapacityScore(MemorySubcapacity.EPISODIC, value = 50.0, n = 3),
            ),
        )
        assertFalse(report.overall.confident)
        val band = report.overall.upperBound!! - report.overall.lowerBound!!
        assertTrue(band > 20.0, "Low-data band should be wide (was $band).")
        assertFalse(report.perSubcapacity.first().confident)
    }

    @Test
    fun singleSubcapacity_notConfidentEvenWithData() {
        // MIN_COMPONENTS = 2: one sub-capacity cannot be a confident overall score.
        val report = calc.compute(
            listOf(SubcapacityScore(MemorySubcapacity.WORKING_MEMORY, value = 80.0, n = 50)),
        )
        assertFalse(report.overall.confident)
    }

    @Test
    fun boundsStayWithinZeroToHundred() {
        val report = calc.compute(
            listOf(
                SubcapacityScore(MemorySubcapacity.WORKING_MEMORY, value = 99.0, n = 3),
                SubcapacityScore(MemorySubcapacity.EPISODIC, value = 98.0, n = 3),
            ),
        )
        assertTrue(report.overall.upperBound!! <= 100.0)
        assertTrue(report.overall.lowerBound!! >= 0.0)
    }
}
