package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.profile.data.adapter.EpisodicProfileSource
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * DR-P4 · Memoria → EPISODIC_MEMORY adapter for EX3 (episodic / 24h delayed recall). A contribution appears
 * ONLY through the authoritative gate: enough honest discriminating outcomes → VALID-preliminary;
 * no/too-little/gamed → null. The episodic signal informs its OWN dimension (MEMORIA_EPISODICA →
 * EPISODIC_MEMORY), distinct from the working-memory sources (EX1 span + EX4 pairs → WORKING_MEMORY).
 */
class EpisodicProfileSourceTest {

    /** A discriminating delayed outcome: recalled [hits]/8 studied, selected no lures (false-alarm rate 0). */
    private fun discriminating(hits: Int = 6) =
        DeferredRecallOutcome(studiedCount = 8, hits = hits, distractorCount = 8, falseAlarms = 0)

    @Test
    fun noData_yieldsNullContribution_calibrating() = runTest {
        assertNull(EpisodicProfileSource(InMemoryDeferredResultStore()).contribution("u1"))
    }

    @Test
    fun insufficientAccumulation_yieldsNull_neverAFabricatedValue() = runTest {
        val store = InMemoryDeferredResultStore()
        repeat(5) { store.record("u1", discriminating()) } // 5 << the ≥20 gate
        assertNull(EpisodicProfileSource(store).contribution("u1"))
    }

    @Test
    fun sufficientHonestAccumulation_contributesPreliminaryEpisodic() = runTest {
        val store = InMemoryDeferredResultStore()
        repeat(20) { store.record("u1", discriminating(hits = 6)) } // hit rate 0.75, discriminating
        val contribution = assertNotNull(EpisodicProfileSource(store).contribution("u1"))
        assertEquals(CognitiveModuleId.MEMORIA_EPISODICA, contribution.moduleId)
        assertEquals(0.75, assertNotNull(contribution.normalizedScore), absoluteTolerance = 1e-9) // 6/8
        assertTrue(contribution.preliminary, "one accumulated block → preliminary, never ESTABLISHED")
    }

    @Test
    fun selectEverythingHistory_yieldsNull_invalidatedByDiscriminationGate() = runTest {
        val store = InMemoryDeferredResultStore()
        repeat(20) { store.record("u1", DeferredRecallOutcome(studiedCount = 8, hits = 8, distractorCount = 8, falseAlarms = 8)) }
        assertNull(EpisodicProfileSource(store).contribution("u1"))
    }

    @Test
    fun moduleId_isMemoriaEpisodica() {
        assertEquals(
            CognitiveModuleId.MEMORIA_EPISODICA,
            EpisodicProfileSource(InMemoryDeferredResultStore()).moduleId,
        )
    }
}
