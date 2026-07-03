package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.decisiones.data.InMemoryReversibilityResultStore
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.profile.data.adapter.DecisionsProfileSource
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * P3c · Decisiones → DECISION_MAKING adapter for E2 Reversibilidad. A contribution appears ONLY through the
 * authoritative gate: enough honest discriminating cards → VALID-preliminary; no/too-little/gamed → null.
 */
class DecisionsProfileSourceTest {

    /** [n] balanced cards (n/2 reversible + n/2 irreversible), all classified correctly → balanced acc 1.0. */
    private fun honest(n: Int): List<ReversibilityTrial> {
        val half = n / 2
        return List(half) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) } +
            List(half) { ReversibilityTrial(actuallyReversible = false, selectedReversible = false) }
    }

    /** A constant responder (always "reversible") on both classes → INVALIDATED. */
    private fun constant(n: Int): List<ReversibilityTrial> {
        val half = n / 2
        return List(half) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) } +
            List(half) { ReversibilityTrial(actuallyReversible = false, selectedReversible = true) }
    }

    @Test
    fun noData_yieldsNullContribution_calibrating() = runTest {
        assertNull(DecisionsProfileSource(InMemoryReversibilityResultStore()).contribution("u1"))
    }

    @Test
    fun insufficientAccumulation_yieldsNull_neverAFabricatedValue() = runTest {
        val store = InMemoryReversibilityResultStore()
        store.record("u1", honest(10)) // 10 < the ≥20 gate
        assertNull(DecisionsProfileSource(store).contribution("u1"))
    }

    @Test
    fun sufficientHonestAccumulation_stillYieldsNull_whileContentBankIsUnvalidated() = runTest {
        // Even 24 balanced correct cards do NOT contribute: the live E2 scenario content is degenerate
        // (template description + coin-flip label), so the fold marks the bank unvalidated → BANK_CONTAMINATED
        // → no DECISION_MAKING update (never a fabricated number) until a real validated bank exists.
        val store = InMemoryReversibilityResultStore()
        store.record("u1", honest(24))
        assertNull(DecisionsProfileSource(store).contribution("u1"))
    }

    @Test
    fun constantResponderHistory_yieldsNull_invalidatedByTheGate() = runTest {
        val store = InMemoryReversibilityResultStore()
        store.record("u1", constant(24))
        assertNull(DecisionsProfileSource(store).contribution("u1"))
    }

    @Test
    fun moduleId_isDecisiones() {
        assertEquals(
            CognitiveModuleId.DECISIONES,
            DecisionsProfileSource(InMemoryReversibilityResultStore()).moduleId,
        )
    }
}
