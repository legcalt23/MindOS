package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.ScoreState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * P1 EX4 (P2b) · the pure log → ScoreResult fold on a live path. With the discrimination gate as the
 * tested anti-gaming (ADEQUATE), a sufficient honest session reaches VALID; select-all/small never do.
 */
class MemoryPairsScoreTest {

    private fun score(log: PairsRecordLog) =
        MemoryPairsScore.scoreOf(log, exerciseId = "EX4_PARES_ASOCIADOS", sessionId = "s1", userId = "u1", timestampMs = 0L)

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
}
