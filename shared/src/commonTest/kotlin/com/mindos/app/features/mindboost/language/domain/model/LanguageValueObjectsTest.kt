package com.mindos.app.features.mindboost.language.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Value objects guard structural invariants so the rest of the domain can assume validity.
 * These tests pin the boundaries (blank ids, out-of-range proportions, negative latency).
 */
class LanguageValueObjectsTest {

    @Test
    fun blankIdentitiesAreRejected() {
        assertFailsWith<IllegalArgumentException> { LanguageSessionId(" ") }
        assertFailsWith<IllegalArgumentException> { TaskId("") }
        assertFailsWith<IllegalArgumentException> { AttemptId("") }
        assertFailsWith<IllegalArgumentException> { UserIdHash("") }
    }

    @Test
    fun taskVersionIsOneBased() {
        assertFailsWith<IllegalArgumentException> { TaskVersion(0) }
        assertEquals(1, TaskVersion.FIRST.value)
    }

    @Test
    fun proportionStaysInRange() {
        assertFailsWith<IllegalArgumentException> { Proportion(-0.01) }
        assertFailsWith<IllegalArgumentException> { Proportion(1.01) }
        assertEquals(0, Proportion.ZERO.percent)
        assertEquals(100, Proportion.ONE.percent)
        assertEquals(50, Proportion(0.5).percent)
    }

    @Test
    fun proportionClampedNeverThrows() {
        assertEquals(Proportion.ONE, Proportion.clamped(2.0))
        assertEquals(Proportion.ZERO, Proportion.clamped(-5.0))
    }

    @Test
    fun latencyCannotBeNegative() {
        assertFailsWith<IllegalArgumentException> { LatencyMs(-1) }
        assertTrue(LatencyMs.ZERO.value == 0L)
    }

    @Test
    fun cognitiveDoseRejectsOutOfRangeAxis() {
        assertFailsWith<IllegalArgumentException> {
            CognitiveDose.LOW.copy(novelty = 1.5)
        }
        // A valid dose exposes exactly its ten named axes.
        assertEquals(10, CognitiveDose.LOW.axes().size)
    }
}
