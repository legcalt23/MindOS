package com.mindos.app.features.mindboost.decisiones.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.ScoreState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * E2 Reversibilidad (Decisiones · P2) · the pure log → ScoreResult fold. With the discrimination/anti-gaming
 * gates as the tested control (ADEQUATE), a sufficient honest history reaches VALID; constant/small never do.
 */
class ReversibilityScoreTest {

    private fun score(log: ReversibilityRecordLog) =
        ReversibilityScore.scoreOf(log, exerciseId = "E2_REVERSIBILIDAD", sessionId = "s1", userId = "u1", timestampMs = 0L)

    private fun honest(n: Int): List<ReversibilityTrial> {
        val half = n / 2
        return List(half) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) } +
            List(half) { ReversibilityTrial(actuallyReversible = false, selectedReversible = false) }
    }

    private fun constant(n: Int): List<ReversibilityTrial> {
        val half = n / 2
        return List(half) { ReversibilityTrial(actuallyReversible = true, selectedReversible = true) } +
            List(half) { ReversibilityTrial(actuallyReversible = false, selectedReversible = true) } // always "reversible"
    }

    @Test
    fun emptyLog_isNotValid_andNeverANumber() {
        val result = score(ReversibilityRecordLog())
        assertNotEquals(ScoreState.VALID, result.state)
        assertNull(result.primarySignal)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun singleCard_isNotValid_andNeverANumber() {
        val result = score(ReversibilityRecordLog(listOf(ReversibilityTrial(actuallyReversible = true, selectedReversible = true))))
        assertNotEquals(ScoreState.VALID, result.state)
        assertNull(result.primarySignal)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun sufficientHonestHistory_isBlocked_becauseLiveContentBankIsUnvalidated() {
        // Even a perfect, sufficient honest history does NOT feed the profile: the live scenario source is
        // degenerate (template description + coin-flip label), so the fold marks the bank unvalidated and
        // the state machine blocks it (BANK_CONTAMINATED) — never a fabricated REVERSIBILITY_DISCRIMINATION.
        val result = score(ReversibilityRecordLog(honest(24)))
        assertEquals(ScoreState.BANK_CONTAMINATED, result.state)
        assertFalse(result.updatesProfile)
        assertNull(result.primarySignal, "a blocked verdict never carries a number")
    }

    @Test
    fun constantResponder_isAlsoBlocked_byTheContentBankGate() {
        // The bank gate precedes the gaming/invalidation gates, so degenerate content blocks regardless.
        val result = score(ReversibilityRecordLog(constant(24)))
        assertEquals(ScoreState.BANK_CONTAMINATED, result.state)
        assertFalse(result.updatesProfile)
    }

    @Test
    fun provenance_neverCarriesTheRawUserId() {
        val result = score(ReversibilityRecordLog(honest(24)))
        assertNotEquals("u1", result.provenance.pseudoUserId)
    }
}
