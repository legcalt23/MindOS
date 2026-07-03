package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.memory.data.InMemoryPairsResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial
import com.mindos.app.features.mindboost.profile.data.adapter.PairsProfileSource
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P3 · Memoria → WORKING_MEMORY adapter for EX4 (associative). A contribution appears ONLY through the
 * authoritative gate: enough honest discriminating trials → VALID-preliminary; no/too-little/gamed → null.
 */
class PairsProfileSourceTest {

    /** A discriminating trial: recalled [hits]/5 targets, selected no lures (false-alarm rate 0). */
    private fun discriminating(hits: Int = 4) =
        PairsTrial(targetCount = 5, hits = hits, distractorCount = 6, falseAlarms = 0)

    @Test
    fun noData_yieldsNullContribution_calibrating() = runTest {
        assertNull(PairsProfileSource(InMemoryPairsResultStore()).contribution("u1"))
    }

    @Test
    fun insufficientAccumulation_yieldsNull_neverAFabricatedValue() = runTest {
        val store = InMemoryPairsResultStore()
        repeat(5) { store.record("u1", discriminating()) } // 5 << the ≥20 gate
        assertNull(PairsProfileSource(store).contribution("u1"))
    }

    @Test
    fun sufficientHonestAccumulation_contributesPreliminaryAssociative() = runTest {
        val store = InMemoryPairsResultStore()
        repeat(20) { store.record("u1", discriminating(hits = 4)) } // hit rate 0.8, discriminating
        val contribution = assertNotNull(PairsProfileSource(store).contribution("u1"))
        assertEquals(CognitiveModuleId.MEMORIA, contribution.moduleId)
        assertEquals(0.8, assertNotNull(contribution.normalizedScore), absoluteTolerance = 1e-9)
        assertTrue(contribution.preliminary, "one accumulated block → preliminary, never ESTABLISHED")
    }

    @Test
    fun selectEverythingSession_yieldsNull_invalidatedByDiscriminationGate() = runTest {
        val store = InMemoryPairsResultStore()
        repeat(20) { store.record("u1", PairsTrial(targetCount = 5, hits = 5, distractorCount = 6, falseAlarms = 6)) }
        assertNull(PairsProfileSource(store).contribution("u1"))
    }

    @Test
    fun moduleId_isMemoria() {
        assertEquals(CognitiveModuleId.MEMORIA, PairsProfileSource(InMemoryPairsResultStore()).moduleId)
    }
}
