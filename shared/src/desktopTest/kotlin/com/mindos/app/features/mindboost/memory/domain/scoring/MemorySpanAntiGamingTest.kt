package com.mindos.app.features.mindboost.memory.domain.scoring

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P1 EX1 · the Memoria processing detector: a button-spammer is caught, an honest user is not. */
class MemorySpanAntiGamingTest {

    @Test
    fun buttonSpammer_allSameProcessingResponse_isNotClean() {
        val verdict = MemorySpanAntiGaming.evaluate(List(12) { 1 }) // always "yes"
        assertFalse(verdict.isClean)
    }

    @Test
    fun variedProcessingResponses_isClean() {
        val verdict = MemorySpanAntiGaming.evaluate(listOf(0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0))
        assertTrue(verdict.isClean)
    }

    @Test
    fun tooFewResponses_isClean_insufficientToJudge() {
        val verdict = MemorySpanAntiGaming.evaluate(List(5) { 1 }) // below the detector's minimum
        assertTrue(verdict.isClean)
    }
}
