package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankKind
import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.bank.BankTestFixtures
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P1 EX1 (P4) · the pure log → ScoreResult fold. The state-distinction tests inject a REGISTERED + VALID
 * generator bank ([registeredRef]) so a sufficient honest session reaches VALID and gaming/low-processing
 * never do; the PRODUCTION reality (B-1: unregistered → BANK_CONTAMINATED) is pinned separately.
 */
class MemorySpanScoreTest {

    private val registeredRef = BankRef(
        MemorySpanProfileAdapter.BANK_ID,
        registry = listOf(BankTestFixtures.validBank(MemorySpanProfileAdapter.BANK_ID, BankKind.ALGORITHMIC_GENERATOR)),
    )

    private fun score(log: SpanRecordLog) =
        MemorySpanScore.scoreOf(
            log, exerciseId = "EX1_SPAN_COMPLEJO", sessionId = "s1", userId = "u1", timestampMs = 0L, bankRef = registeredRef,
        )

    private fun validTrial(items: Int = 4) =
        SpanTrial(sequenceLength = 6, processingCorrect = true, serialCorrectItems = items)

    /** Varied yes/no judgements → the anti-gaming response-entropy detector stays clean. */
    private fun variedResponses(n: Int) = List(n) { it % 2 }

    @Test
    fun emptyLog_isNotValid_andNeverANumber() {
        val result = score(SpanRecordLog())
        assertNotEquals(ScoreState.VALID, result.state)
        assertNull(result.primarySignal)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun sufficientHonestSession_isValid_withSpanSignal() {
        val log = SpanRecordLog(trials = List(20) { validTrial() }, processingResponses = variedResponses(20))
        val result = score(log)
        assertEquals(ScoreState.VALID, result.state)
        assertTrue(result.updatesProfile)
        assertEquals(0.5, result.primarySignal?.value) // span 4 / MAX_SPAN 8
    }

    @Test
    fun buttonSpammedResponses_isSuspectedGaming_evenWithValidBlock() {
        // Processing answered correctly (block VALID) but every response identical → response-entropy fires.
        val log = SpanRecordLog(trials = List(20) { validTrial() }, processingResponses = List(20) { 1 })
        val result = score(log)
        assertEquals(ScoreState.SUSPECTED_GAMING, result.state)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun lowProcessingAccuracy_isInvalidated() {
        val trials = List(10) { validTrial() } +
            List(10) { SpanTrial(sequenceLength = 6, processingCorrect = false, serialCorrectItems = 4) }
        val result = score(SpanRecordLog(trials = trials, processingResponses = variedResponses(20)))
        assertEquals(ScoreState.INVALIDATED, result.state)
        assertNull(result.primarySignal)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun provenance_neverCarriesTheRawUserId() {
        val result = score(SpanRecordLog(trials = List(20) { validTrial() }, processingResponses = variedResponses(20)))
        assertNotEquals("u1", result.provenance.pseudoUserId)
    }

    @Test
    fun unregisteredBank_liveGate_isBankContaminated_untilBankRegistered_b1() {
        // Production reality (B-1): with no registered procedural contract (the default BankRef resolves
        // against the empty ProfileUpdatingBanks), even a sufficient honest session is BANK_CONTAMINATED.
        val log = SpanRecordLog(trials = List(20) { validTrial() }, processingResponses = variedResponses(20))
        val result = MemorySpanScore.scoreOf(
            log, exerciseId = "EX1_SPAN_COMPLEJO", sessionId = "s1", userId = "u1", timestampMs = 0L,
        )
        assertEquals(ScoreState.BANK_CONTAMINATED, result.state)
        assertFalse(result.updatesProfile)
    }
}
