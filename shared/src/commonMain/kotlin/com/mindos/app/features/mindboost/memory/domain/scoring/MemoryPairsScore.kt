package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import com.mindos.app.features.mindboost.core.scoring.pseudoUserId

/**
 * P1 EX4 (P2b) · Pure: folds an accumulated [PairsRecordLog] into the universal [ScoreResult].
 *
 * ONE block is formed from ALL retained trials (a live session contributes one trial, and the adapter
 * sums valid trials across blocks for the ≥20 min-n — so one growing block reaches VALID honestly, while
 * chunking by session would make 1-trial blocks that can never validate at ≥8/block).
 *
 * `DetectorCoverage.ADEQUATE` is legitimate because EX4's anti-gaming IS the scorer's discrimination
 * gate (a "select-everything" session over-selects lures → INVALIDATED), tested against that synthetic
 * adversary — there is no separate detector to run, so `antiGamingClean` stays true and the gate inside
 * [AssociativePairsScorer] is what filters gamers. Honesty is preserved by the state machine: a
 * select-all session → INVALIDATED, a small sample → INSUFFICIENT_N / CALIBRATING (never a number).
 */
object MemoryPairsScore {
    const val SCORING_VERSION = ScoringVersionRegistry.MEMORY_PAIRS

    fun scoreOf(
        log: PairsRecordLog,
        exerciseId: String,
        sessionId: String,
        userId: String,
        timestampMs: Long,
    ): ScoreResult {
        val blocks = listOf(AssociativePairsScorer.scoreBlock(log.trials))
        val provenance = ScoreProvenance(
            exerciseId = exerciseId,
            moduleId = ScoreModuleId.MEMORY,
            sessionId = sessionId,
            pseudoUserId = pseudoUserId(userId),
            timestampMs = timestampMs,
            scoringVersion = SCORING_VERSION,
        )
        return MemoryPairsProfileAdapter.toScoreResult(
            blocks = blocks,
            provenance = provenance,
            detectorCoverage = DetectorCoverage.ADEQUATE,
        )
    }
}
