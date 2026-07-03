package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import com.mindos.app.features.mindboost.core.scoring.pseudoUserId

/**
 * P1 EX1 (P4) · Pure: folds an accumulated [SpanRecordLog] into the universal [ScoreResult].
 *
 * ONE span block is formed from ALL retained trials. A live session contributes one trial, and the
 * adapter sums valid trials ACROSS blocks for the ≥20 min-n — so a single growing block reaches VALID
 * honestly, whereas chunking by session would make 1-trial blocks that can never validate (≥8/block),
 * leaving EX1 permanently unreachable.
 *
 * Anti-gaming runs the REAL Memoria detector ([MemorySpanAntiGaming], tested against synthetic
 * adversaries), so [DetectorCoverage.ADEQUATE] is legitimate here — this is the boundary at which EX1
 * can finally reach VALID on a live path. Honesty is preserved by the state machine: a button-spammer
 * → SUSPECTED_GAMING, a low-processing block → INVALIDATED, a small sample → INSUFFICIENT_N /
 * CALIBRATING (never a number).
 *
 * The span/validity gate reads [SpanRecordLog.trials] (one summary per session) while anti-gaming reads
 * [SpanRecordLog.processingResponses] (a 0/1 window spanning fewer sessions, since each session emits
 * several responses). This decoupling is intentional and safe: a persistent gamer is low-entropy in BOTH
 * windows, and reaching the ≥20-valid-trial gate necessarily appends ≥40 responses, so the detector's
 * minimum-sample floor is never idle while a VALID-eligible block exists — the "detector idle but VALID"
 * path cannot occur.
 */
object MemorySpanScore {
    const val SCORING_VERSION = ScoringVersionRegistry.MEMORY_SPAN

    fun scoreOf(
        log: SpanRecordLog,
        exerciseId: String,
        sessionId: String,
        userId: String,
        timestampMs: Long,
    ): ScoreResult {
        val blocks = listOf(ComplexSpanScorer.scoreBlock(log.trials))
        val clean = MemorySpanAntiGaming.evaluate(log.processingResponses).isClean
        val provenance = ScoreProvenance(
            exerciseId = exerciseId,
            moduleId = ScoreModuleId.MEMORY,
            sessionId = sessionId,
            pseudoUserId = pseudoUserId(userId),
            timestampMs = timestampMs,
            scoringVersion = SCORING_VERSION,
        )
        return MemorySpanProfileAdapter.toScoreResult(
            blocks = blocks,
            provenance = provenance,
            detectorCoverage = DetectorCoverage.ADEQUATE,
            antiGamingClean = clean,
        )
    }
}
