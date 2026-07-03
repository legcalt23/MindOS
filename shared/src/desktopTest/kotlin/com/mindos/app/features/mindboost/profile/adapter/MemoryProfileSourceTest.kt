package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.memory.data.InMemorySpanResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanTrial
import com.mindos.app.features.mindboost.profile.data.adapter.MemoryProfileSource
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P5 · Memoria → WORKING_MEMORY adapter. A contribution appears ONLY through the authoritative gate:
 * enough honest valid trials → VALID-preliminary; no/too-little/gamed data → null (CALIBRATING).
 */
class MemoryProfileSourceTest {

    private fun validTrial() = SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = 4)

    @Test
    fun noData_yieldsNullContribution_calibrating() = runTest {
        assertNull(MemoryProfileSource(InMemorySpanResultStore()).contribution("u1"))
    }

    @Test
    fun insufficientAccumulation_yieldsNull_neverAFabricatedValue() = runTest {
        val store = InMemorySpanResultStore()
        repeat(5) { store.record("u1", validTrial(), listOf(0, 1)) } // 5 valid trials << the ≥20 gate
        assertNull(MemoryProfileSource(store).contribution("u1"))
    }

    @Test
    fun sufficientHonestAccumulation_contributesPreliminaryWorkingMemory() = runTest {
        val store = InMemorySpanResultStore()
        repeat(20) { i -> store.record("u1", validTrial(), listOf(i % 2)) } // 20 valid + varied responses
        val contribution = assertNotNull(MemoryProfileSource(store).contribution("u1"))
        assertEquals(CognitiveModuleId.MEMORIA, contribution.moduleId)
        assertEquals(0.5, contribution.normalizedScore) // span 4 / MAX_SPAN 8
        assertTrue(contribution.preliminary, "one accumulated block → preliminary, never ESTABLISHED")
    }

    @Test
    fun buttonSpammedProcessing_yieldsNull_gatedByAntiGaming() = runTest {
        val store = InMemorySpanResultStore()
        repeat(20) { store.record("u1", validTrial(), listOf(1)) } // always "yes" → low entropy → gamed
        assertNull(MemoryProfileSource(store).contribution("u1"))
    }

    @Test
    fun moduleId_isMemoria() {
        assertEquals(CognitiveModuleId.MEMORIA, MemoryProfileSource(InMemorySpanResultStore()).moduleId)
    }
}
