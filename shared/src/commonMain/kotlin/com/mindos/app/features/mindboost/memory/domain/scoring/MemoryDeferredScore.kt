package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import com.mindos.app.features.mindboost.core.scoring.pseudoUserId

/**
 * EX3 deferred recall (P3) · Pure: folds an accumulated [DeferredRecordLog] into the universal
 * [ScoreResult]. ONE block is formed from ALL retained outcomes (the adapter sums valid trials across
 * blocks for the ≥20 min-n, so one growing block reaches VALID honestly).
 *
 * `DetectorCoverage.ADEQUATE` is legitimate because the anti-gaming IS the scorer's discrimination gate
 * (a "select-everything" delayed test → INVALIDATED), tested against that synthetic adversary — there is
 * no separate detector, so `antiGamingClean` stays true and the gate inside [DeferredRecallScorer] filters
 * gamers. B-1: the bank integrity is DERIVED from the authority for [bankRef]; the EX3 generator has NO
 * registered procedural contract yet, so the LIVE path is `BANK_CONTAMINATED` (never updates the profile).
 * With a registered bank the state machine then distinguishes honestly: select-all → INVALIDATED, a small
 * sample → INSUFFICIENT_N / CALIBRATING (never a number).
 */
object MemoryDeferredScore {
    const val SCORING_VERSION = ScoringVersionRegistry.MEMORY_DEFERRED

    @Suppress("LongParameterList") // Cohesive session-fold inputs + the authority-derived bankRef (B-1).
    fun scoreOf(
        log: DeferredRecordLog,
        exerciseId: String,
        sessionId: String,
        userId: String,
        timestampMs: Long,
        bankRef: BankRef = BankRef(DeferredRecallProfileAdapter.BANK_ID),
    ): ScoreResult {
        val blocks = listOf(DeferredRecallScorer.scoreBlock(log.outcomes))
        val provenance = ScoreProvenance(
            exerciseId = exerciseId,
            moduleId = ScoreModuleId.EPISODIC_MEMORY,
            sessionId = sessionId,
            pseudoUserId = pseudoUserId(userId),
            timestampMs = timestampMs,
            scoringVersion = SCORING_VERSION,
        )
        return DeferredRecallProfileAdapter.toScoreResult(
            blocks = blocks,
            provenance = provenance,
            detectorCoverage = DetectorCoverage.ADEQUATE,
            bankRef = bankRef,
        )
    }
}
