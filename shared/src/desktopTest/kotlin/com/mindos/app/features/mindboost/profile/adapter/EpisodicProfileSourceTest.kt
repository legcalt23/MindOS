package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredResultStore
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.profile.data.adapter.EpisodicProfileSource
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * DR-P4 · Memoria → EPISODIC_MEMORY adapter for EX3 (episodic / 24h delayed recall). A contribution appears
 * ONLY through the authoritative gate: no/too-little/gamed → null. The episodic signal informs its OWN
 * dimension (MEMORIA_EPISODICA → EPISODIC_MEMORY). B-1: the EX3 generator has NO registered procedural
 * contract, so even sufficient honest data is BANK_CONTAMINATED → null in production (re-promotion deferred).
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
    fun sufficientHonestAccumulation_isBankContaminated_untilBankRegistered_b1() = runTest {
        // B-1: no registered procedural contract for the EX3 generator → even ≥20 discriminating outcomes
        // yield BANK_CONTAMINATED → null contribution → EPISODIC_MEMORY stays CALIBRATING in production.
        val store = InMemoryDeferredResultStore()
        repeat(20) { store.record("u1", discriminating(hits = 6)) } // hit rate 0.75, discriminating
        assertNull(EpisodicProfileSource(store).contribution("u1"))
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
