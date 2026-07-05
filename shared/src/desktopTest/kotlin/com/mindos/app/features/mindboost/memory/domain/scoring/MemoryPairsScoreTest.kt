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
 * P1 EX4 (P2b) · the pure log → ScoreResult fold. The state-distinction tests inject a REGISTERED + VALID
 * generator bank ([registeredRef]) so a sufficient honest session reaches VALID and select-all never does;
 * the PRODUCTION reality (B-1: unregistered → BANK_CONTAMINATED) is pinned separately.
 */
class MemoryPairsScoreTest {

    private val registeredRef = BankRef(
        MemoryPairsProfileAdapter.BANK_ID,
        registry = listOf(BankTestFixtures.validBank(MemoryPairsProfileAdapter.BANK_ID, BankKind.ALGORITHMIC_GENERATOR)),
    )

    private fun score(log: PairsRecordLog) =
        MemoryPairsScore.scoreOf(
            log, exerciseId = "EX4_PARES_ASOCIADOS", sessionId = "s1", userId = "u1", timestampMs = 0L, bankRef = registeredRef,
        )

    /** A discriminating trial: recalled [hits]/5 targets, selected no lures (false-alarm rate 0). */
    private fun discriminating(hits: Int = 4) =
        PairsTrial(targetCount = 5, hits = hits, distractorCount = 6, falseAlarms = 0)

    @Test
    fun emptyLog_isNotValid_andNeverANumber() {
        val result = score(PairsRecordLog())
        assertNotEquals(ScoreState.VALID, result.state)
        assertNull(result.primarySignal)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun sufficientHonestSession_isValid_withAssociativeSignal() {
        val log = PairsRecordLog(trials = List(20) { discriminating(hits = 4) })
        val result = score(log)
        assertEquals(ScoreState.VALID, result.state)
        assertTrue(result.updatesProfile)
        assertEquals(0.8, assertNotNull(result.primarySignal?.value), absoluteTolerance = 1e-9) // hit rate 4/5
    }

    @Test
    fun selectEverythingSession_isInvalidated() {
        val log = PairsRecordLog(trials = List(20) { PairsTrial(targetCount = 5, hits = 5, distractorCount = 6, falseAlarms = 6) })
        val result = score(log)
        assertEquals(ScoreState.INVALIDATED, result.state)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun provenance_neverCarriesTheRawUserId() {
        val result = score(PairsRecordLog(trials = List(20) { discriminating() }))
        assertNotEquals("u1", result.provenance.pseudoUserId)
    }

    @Test
    fun unregisteredBank_liveGate_isBankContaminated_untilBankRegistered_b1() {
        val log = PairsRecordLog(trials = List(20) { discriminating(hits = 4) })
        val result = MemoryPairsScore.scoreOf(log, exerciseId = "EX4_PARES_ASOCIADOS", sessionId = "s1", userId = "u1", timestampMs = 0L)
        assertEquals(ScoreState.BANK_CONTAMINATED, result.state)
        assertFalse(result.updatesProfile)
    }
}
