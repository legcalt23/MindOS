package com.mindos.app.features.mindboost.memory.domain.exercise

import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DifficultyDecodeTest {

    @Test
    fun lerp_endpointsAndMidpoint() {
        assertEquals(2.0, DifficultyDecode.lerp(2.0, 8.0, 0.0))
        assertEquals(8.0, DifficultyDecode.lerp(2.0, 8.0, 1.0))
        assertEquals(5.0, DifficultyDecode.lerp(2.0, 8.0, 0.5))
    }

    @Test
    fun decodeCount_mapsLevelIntoRange() {
        assertEquals(2, DifficultyDecode.decodeCount(AxisLevel.MIN, 2, 8))
        assertEquals(8, DifficultyDecode.decodeCount(AxisLevel.MAX, 2, 8))
        assertEquals(5, DifficultyDecode.decodeCount(AxisLevel(0.5), 2, 8))
    }

    @Test
    fun decodeCount_staysInBounds() {
        val c = DifficultyDecode.decodeCount(AxisLevel(0.99), 4, 30)
        assertEquals(true, c in 4..30)
    }

    @Test
    fun decodeMillis_mapsLevelIntoRange() {
        assertEquals(0L, DifficultyDecode.decodeMillis(AxisLevel.MIN, 0L, 300_000L))
        assertEquals(300_000L, DifficultyDecode.decodeMillis(AxisLevel.MAX, 0L, 300_000L))
    }

    @Test
    fun decodeCount_rejectsInvertedRange() {
        assertFailsWith<IllegalArgumentException> { DifficultyDecode.decodeCount(AxisLevel.MIN, 8, 2) }
    }
}
