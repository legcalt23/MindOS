package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankKind
import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.bank.BankTestFixtures
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * EX3 deferred recall (P3) · the pure log → ScoreResult fold. The state-distinction tests inject a REGISTERED
 * + VALID generator bank ([registeredRef]) so a sufficient honest history reaches VALID and select-all never
 * does; the PRODUCTION reality (B-1: unregistered → BANK_CONTAMINATED) is pinned separately.
 */
class MemoryDeferredScoreTest {

    private val registeredRef = BankRef(
        DeferredRecallProfileAdapter.BANK_ID,
        registry = listOf(BankTestFixtures.validBank(DeferredRecallProfileAdapter.BANK_ID, BankKind.ALGORITHMIC_GENERATOR)),
    )

    private fun score(log: DeferredRecordLog) =
        MemoryDeferredScore.scoreOf(
            log, exerciseId = "EX3_LISTA_ESTRATEGIA", sessionId = "s1", userId = "u1", timestampMs = 0L, bankRef = registeredRef,
        )

    private fun discriminating(hits: Int = 6) =
        DeferredRecallOutcome(studiedCount = 8, hits = hits, distractorCount = 8, falseAlarms = 0)

    @Test
    fun emptyLog_isNotValid_andNeverANumber() {
        val result = score(DeferredRecordLog())
        assertNotEquals(ScoreState.VALID, result.state)
        assertNull(result.primarySignal)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun singleOutcome_isNotValid_andNeverANumber() {
        // One honest discriminating outcome is far below BOTH gates (block <8 valid trials AND <20 sample n):
        // it must stay non-VALID with a null signal — never a fabricated number (null != 0).
        val result = score(DeferredRecordLog(outcomes = listOf(discriminating(hits = 6))))
        assertNotEquals(ScoreState.VALID, result.state)
        assertNull(result.primarySignal)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun sufficientHonestHistory_isValid_withDelayedSignal() {
        val result = score(DeferredRecordLog(outcomes = List(20) { discriminating(hits = 6) }))
        assertEquals(ScoreState.VALID, result.state)
        assertTrue(result.updatesProfile)
        assertEquals(0.75, assertNotNull(result.primarySignal?.value), absoluteTolerance = 1e-9) // 6/8
    }

    @Test
    fun selectEverythingHistory_isInvalidated() {
        val outcomes = List(20) { DeferredRecallOutcome(studiedCount = 8, hits = 8, distractorCount = 8, falseAlarms = 8) }
        val result = score(DeferredRecordLog(outcomes = outcomes))
        assertEquals(ScoreState.INVALIDATED, result.state)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun provenance_neverCarriesTheRawUserId() {
        val result = score(DeferredRecordLog(outcomes = List(20) { discriminating() }))
        assertNotEquals("u1", result.provenance.pseudoUserId)
    }

    @Test
    fun unregisteredBank_liveGate_isBankContaminated_untilBankRegistered_b1() {
        val result = MemoryDeferredScore.scoreOf(
            DeferredRecordLog(outcomes = List(20) { discriminating(hits = 6) }),
            exerciseId = "EX3_LISTA_ESTRATEGIA", sessionId = "s1", userId = "u1", timestampMs = 0L,
        )
        assertEquals(ScoreState.BANK_CONTAMINATED, result.state)
        assertFalse(result.updatesProfile)
    }
}
