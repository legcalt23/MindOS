package com.mindos.app.features.mindboost.memory.domain.metrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryStatisticsTest {

    @Test
    fun median_oddAndEven() {
        assertEquals(2.0, MemoryStatistics.median(listOf(1.0, 3.0, 2.0)))
        assertEquals(2.5, MemoryStatistics.median(listOf(1.0, 2.0, 3.0, 4.0)))
    }

    @Test
    fun median_emptyIsZero() {
        assertEquals(0.0, MemoryStatistics.median(emptyList()))
    }

    @Test
    fun rejectOutliers_dropsExtremeValue() {
        val values = listOf(100.0, 102.0, 98.0, 101.0, 99.0, 5000.0)
        val filtered = MemoryStatistics.rejectOutliers(values)
        assertTrue(5000.0 !in filtered, "The extreme value should be removed.")
        assertTrue(filtered.size == values.size - 1)
    }

    @Test
    fun rejectOutliers_keepsSmallSampleUnchanged() {
        val values = listOf(100.0, 5000.0)
        assertEquals(values, MemoryStatistics.rejectOutliers(values))
    }
}
