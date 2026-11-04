@file:Suppress("LongParameterList", "MagicNumber")

package com.mindverse.mindboost.core.session

import com.mindverse.mindboost.core.coach.CoachEngine
import com.mindverse.mindboost.core.coach.CoachSignals
import com.mindverse.mindboost.core.domain.BayesScenario
import com.mindverse.mindboost.core.domain.CognitiveReadinessState
import com.mindverse.mindboost.core.domain.DecisionSession
import com.mindverse.mindboost.core.domain.DifficultyVector
import com.mindverse.mindboost.core.domain.ReversibilityScenario
import com.mindverse.mindboost.core.mdp.bayes.BayesEngine
import com.mindverse.mindboost.core.scoring.ScoringEquations

/** Raw signals captured by the Check-in (CRI / Go-No-go) screen. */
data class CheckInInput(
    val goNoGoAccuracy: Double,
    val reactionTimeCV: Double,
    val subjectiveFatigue: Double,
    val reactionTimeBaselineMs: Long,
)

/**
 * A completed decision round, reduced to the signals scoring needs.
 *
 * Prefer constructing rounds with [DecisionSessionOrchestrator.bayesRound] /
 * [DecisionSessionOrchestrator.reversibilityRound], which derive these signals from a
 * generated scenario plus the user's recorded response.
 */
data class DecisionRound(
    val difficulty: DifficultyVector,
    val objectivesAlignment: Double,
    val bayesianUpdateRatio: Double,
    val biasDetected: Boolean,
    val alternativesScannedRatio: Double,
    val reactionTimeMs: Long,
    val timeLimitMs: Long,
    val isReversible: Boolean,
    /** `null` when this exercise has no spatial grid to observe — see `ScoringEquations.calculateAGRS`. */
    val spatialEntropy: Double?,
    /** `null` when this exercise has no dial/slider whose default the user could leave untouched. */
    val isDialDefault: Boolean?,
)

/** Per-round scoring outcome. */
data class RoundOutcome(
    val icd: Double,
    val ied: Double,
    val agrs: Double,
)

/** Everything produced by assembling one Decisiones session. */
data class DecisionSessionResult(
    val session: DecisionSession,
    val readiness: CognitiveReadinessState,
    val rounds: List<RoundOutcome>,
    val coachMessage: String?,
)

/**
 * Pure, deterministic orchestrator for a single Decisiones session.
 *
 * It sequences check-in -> per-round scoring -> aggregated [DecisionSession] + coach feedback,
 * delegating every formula to [ScoringEquations]/[BayesEngine] and message selection to
 * [CoachEngine]. It owns no persistence, no platform code and no randomness: scenario
 * generation and durable storage stay with the caller (UI / repositories), which keeps this
 * layer fully unit-testable.
 */
class DecisionSessionOrchestrator {
    /** Turns raw Go/No-go signals into the basal [CognitiveReadinessState] (CRI). */
    fun checkIn(input: CheckInInput): CognitiveReadinessState {
        val cri = ScoringEquations.calculateCRI(
            goNoGoAccuracy = input.goNoGoAccuracy,
            reactionTimeCV = input.reactionTimeCV,
            subjectiveFatigue = input.subjectiveFatigue,
        )
        return CognitiveReadinessState(
            goNoGoAccuracy = input.goNoGoAccuracy.coerceIn(0.0, 1.0),
            reactionTimeCV = input.reactionTimeCV.coerceIn(0.0, 1.0),
            subjectiveFatigue = input.subjectiveFatigue.coerceIn(0.0, 1.0),
            criValue = cri,
        )
    }

    /** Builds a Bayes (EX-07/08) round, deriving BUR from the user's posterior estimate. */
    fun bayesRound(
        difficulty: DifficultyVector,
        scenario: BayesScenario,
        userPosterior: Double,
        objectivesAlignment: Double,
        biasDetected: Boolean,
        alternativesScannedRatio: Double,
        reactionTimeMs: Long,
        timeLimitMs: Long,
        spatialEntropy: Double?,
        isDialDefault: Boolean?,
    ): DecisionRound = DecisionRound(
        difficulty = difficulty,
        objectivesAlignment = objectivesAlignment,
        bayesianUpdateRatio = BayesEngine.evaluateBayesianUpdate(scenario, userPosterior),
        biasDetected = biasDetected,
        alternativesScannedRatio = alternativesScannedRatio,
        reactionTimeMs = reactionTimeMs,
        timeLimitMs = timeLimitMs,
        isReversible = false,
        spatialEntropy = spatialEntropy,
        isDialDefault = isDialDefault,
    )

    /**
     * Builds a Reversibility (EX-19) round. Triage correctness drives objective alignment;
     * triage has no probabilistic update, so BUR is neutral.
     */
    fun reversibilityRound(
        difficulty: DifficultyVector,
        scenario: ReversibilityScenario,
        selectedReversible: Boolean,
        alternativesScannedRatio: Double,
        reactionTimeMs: Long,
        timeLimitMs: Long,
        spatialEntropy: Double?,
        isDialDefault: Boolean?,
    ): DecisionRound {
        val classifiedCorrectly = selectedReversible == scenario.isReversible
        return DecisionRound(
            difficulty = difficulty,
            objectivesAlignment = if (classifiedCorrectly) CORRECT_ALIGNMENT else INCORRECT_ALIGNMENT,
            bayesianUpdateRatio = NEUTRAL_UPDATE_RATIO,
            biasDetected = false,
            alternativesScannedRatio = alternativesScannedRatio,
            reactionTimeMs = reactionTimeMs,
            timeLimitMs = timeLimitMs,
            isReversible = scenario.isReversible,
            spatialEntropy = spatialEntropy,
            isDialDefault = isDialDefault,
        )
    }

    /**
     * Scores every round and folds them into one [DecisionSession] with check-in-derived
     * fatigue (FRS), session gaming risk (AGRS) and a coach message when a profile triggers.
     * An empty round list yields a zeroed session and no coach message.
     */
    fun assembleSession(
        sessionId: String,
        timestampMs: Long,
        checkIn: CheckInInput,
        rounds: List<DecisionRound>,
    ): DecisionSessionResult {
        require(sessionId.isNotBlank()) { "sessionId must not be blank." }

        val readiness = checkIn(checkIn)
        val outcomes = rounds.map(::scoreRound)
        val icd = outcomes.map { it.icd }.meanOrZero()
        val ied = outcomes.map { it.ied }.meanOrZero()
        val cde = rounds.map(::complexityFor).meanOrZero()
        val gamingScore = outcomes.map { it.agrs }.meanOrZero()
        val meanReactionMs = rounds.map { it.reactionTimeMs }.meanOrZero().toLong()
        val fatigueScore = ScoringEquations.calculateFRS(
            reactionTimeActualMs = meanReactionMs,
            reactionTimeMediaMs = checkIn.reactionTimeBaselineMs,
            goNoGoErrorRate = (1.0 - checkIn.goNoGoAccuracy).coerceIn(0.0, 1.0),
        )

        val session = DecisionSession(
            sessionId = sessionId,
            timestampMs = timestampMs,
            icd = icd,
            ied = ied,
            cde = cde,
            fatigueScore = fatigueScore,
            gamingScore = gamingScore,
        )
        val coachMessage = CoachEngine.render(
            CoachSignals(
                reactionTimeMs = meanReactionMs,
                icd = icd,
                ied = ied,
                agrs = gamingScore,
                frs = fatigueScore,
                reactionTimeCv = readiness.reactionTimeCV,
            ),
        )

        return DecisionSessionResult(
            session = session,
            readiness = readiness,
            rounds = outcomes,
            coachMessage = coachMessage,
        )
    }

    private fun scoreRound(round: DecisionRound): RoundOutcome {
        val icd = ScoringEquations.calculateICD(
            objectivesAlignment = round.objectivesAlignment,
            bur = round.bayesianUpdateRatio,
            biasDetected = round.biasDetected,
            alternativesScannedRatio = round.alternativesScannedRatio,
        )
        val ied = ScoringEquations.calculateIED(
            icd = icd,
            reactionTimeMs = round.reactionTimeMs,
            timeLimitMs = round.timeLimitMs,
            isReversible = round.isReversible,
        )
        val agrs = ScoringEquations.calculateAGRS(
            reactionTimeMs = round.reactionTimeMs,
            spatialEntropy = round.spatialEntropy,
            isDialDefault = round.isDialDefault,
        )
        return RoundOutcome(icd = icd, ied = ied, agrs = agrs)
    }

    private fun complexityFor(round: DecisionRound): Double {
        val difficulty = round.difficulty
        return ScoringEquations.calculateCDE(
            nVar = difficulty.numberOfVariables,
            hProb = difficulty.ambiguityLevel,
            hMax = MAX_NORMALIZED_ENTROPY,
            timePressure = difficulty.timePressure,
            memoryLoad = difficulty.memoryLoad,
            reversibility = (1.0 - difficulty.irreversibility).coerceIn(0.0, 1.0),
        )
    }

    private fun List<Number>.meanOrZero(): Double =
        if (isEmpty()) 0.0 else sumOf { it.toDouble() } / size

    private companion object {
        const val CORRECT_ALIGNMENT = 1.0
        const val INCORRECT_ALIGNMENT = 0.0
        const val NEUTRAL_UPDATE_RATIO = 1.0
        const val MAX_NORMALIZED_ENTROPY = 1.0
    }
}
