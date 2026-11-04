package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.decisiones.presentation.state.BayesArenaUiState
import com.mindos.app.features.mindboost.decisiones.presentation.state.ReversibilityTriageUiState
import com.mindverse.mindboost.core.domain.BayesScenario
import com.mindverse.mindboost.core.domain.ReversibilityScenario
import com.mindverse.mindboost.core.scoring.ScoringEquations
import com.mindverse.mindboost.core.session.CheckInInput
import com.mindverse.mindboost.core.session.DecisionRound
import com.mindverse.mindboost.core.session.DecisionSessionOrchestrator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * R7 · `MB-CATALOG-0009` — end-to-end reachability of the Decisiones gaming gate.
 *
 * The unit-level applicability rules live in `AgrsApplicabilityTest`; this file asks the question that
 * actually matters: does an adversary playing the REAL holders produce a session the gate rejects?
 * Both live holders used to hardcode `spatialEntropy = 1.0` ("no grid here"), which pinned 30% of the
 * weight at zero and capped any session at 0.7 against a 0.75 threshold — so the answer was no, for
 * every possible adversary, and the "Integridad" axis on the result screen could never fall below 30.
 */
class DecisionesGamingReachabilityTest {

    @Test
    fun aBayesAdversaryWhoNeverTouchesTheDialInvalidatesTheSession() {
        val holder = BayesArenaStateHolder(scenarios = List(ROUNDS) { bayesScenario() })
        holder.start()
        repeat(ROUNDS) {
            holder.submit(userPosterior = 0.5, confidence = 0.8, reactionTimeMs = IMPOSSIBLY_FAST_MS, dialMoved = false)
        }
        val rounds = assertIs<BayesArenaUiState.Finished>(holder.uiState).rounds

        val gamingScore = sessionGamingScore(rounds)
        assertTrue(
            ScoringEquations.isSessionGamingInvalidated(gamingScore),
            "an instant, never-touched-dial session must be rejected; scored $gamingScore",
        )
    }

    @Test
    fun aDeliberateBayesPlayerIsNotFlagged_discriminantHalf() {
        val holder = BayesArenaStateHolder(scenarios = List(ROUNDS) { bayesScenario() })
        holder.start()
        repeat(ROUNDS) {
            holder.submit(userPosterior = 0.5, confidence = 0.8, reactionTimeMs = DELIBERATE_MS, dialMoved = true)
        }
        val rounds = assertIs<BayesArenaUiState.Finished>(holder.uiState).rounds

        assertEquals(0.0, sessionGamingScore(rounds), TOLERANCE)
    }

    @Test
    fun aReversibilityAdversaryTappingFasterThanReadingInvalidatesTheSession() {
        val holder = ReversibilityTriageStateHolder(scenarios = List(ROUNDS) { reversibilityScenario() })
        holder.start()
        repeat(ROUNDS) { holder.commit(selectedReversible = true, reactionTimeMs = IMPOSSIBLY_FAST_MS) }
        val rounds = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState).rounds

        val gamingScore = sessionGamingScore(rounds)
        assertTrue(
            ScoringEquations.isSessionGamingInvalidated(gamingScore),
            "a card task answered under ${IMPOSSIBLY_FAST_MS}ms was not read; scored $gamingScore",
        )
    }

    @Test
    fun aReversibilityPlayerWhoReadsTheCardIsNotFlagged_discriminantHalf() {
        val holder = ReversibilityTriageStateHolder(scenarios = List(ROUNDS) { reversibilityScenario() })
        holder.start()
        repeat(ROUNDS) { holder.commit(selectedReversible = true, reactionTimeMs = DELIBERATE_MS) }
        val rounds = assertIs<ReversibilityTriageUiState.Finished>(holder.uiState).rounds

        assertEquals(0.0, sessionGamingScore(rounds), TOLERANCE)
    }

    @Test
    fun theIntegrityAxisReachesZeroForAnAdversary() {
        // The live consumer of `gamingScore` is the result polygon: `(1 - gamingScore) * 100`. While the
        // ceiling was 0.7 a total gamer still displayed 30/100 "Integridad" — a fabricated floor.
        val holder = BayesArenaStateHolder(scenarios = List(ROUNDS) { bayesScenario() })
        holder.start()
        repeat(ROUNDS) {
            holder.submit(userPosterior = 0.5, confidence = 0.8, reactionTimeMs = IMPOSSIBLY_FAST_MS, dialMoved = false)
        }
        val rounds = assertIs<BayesArenaUiState.Finished>(holder.uiState).rounds

        val integrity = (1.0 - sessionGamingScore(rounds)) * MAX_AXIS
        assertEquals(0.0, integrity, TOLERANCE)
    }

    /** Folds rounds exactly as production does: `assembleSession` averages the per-round AGRS. */
    private fun sessionGamingScore(rounds: List<DecisionRound>): Double =
        DecisionSessionOrchestrator().assembleSession(
            sessionId = "reachability",
            timestampMs = 1L,
            checkIn = CheckInInput(
                goNoGoAccuracy = 0.9,
                reactionTimeCV = 0.15,
                subjectiveFatigue = 0.3,
                reactionTimeBaselineMs = 400L,
            ),
            rounds = rounds,
        ).session.gamingScore

    private fun bayesScenario(): BayesScenario = BayesScenario(
        baseRate = 0.2,
        sensitivity = 0.9,
        falsePositiveRate = 0.05,
        expectedPosterior = 0.5,
    )

    private fun reversibilityScenario(): ReversibilityScenario = ReversibilityScenario(
        id = "r",
        description = "reversible card",
        isReversible = true,
        delayCostRate = 1.0,
        lockedDurationMs = 0L,
    )

    private companion object {
        const val ROUNDS = 5
        const val IMPOSSIBLY_FAST_MS = 100L
        const val DELIBERATE_MS = 3_000L
        const val MAX_AXIS = 100.0
        const val TOLERANCE = 0.0001
    }
}
