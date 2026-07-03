package com.mindos.app.features.mindboost.decisiones.domain.scoring

import com.mindos.app.features.mindboost.core.scoring.BankIntegrity
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.scoring.pseudoUserId

/**
 * E2 Reversibilidad (Decisiones · P2) · Pure: folds an accumulated [ReversibilityRecordLog] into the
 * universal [ScoreResult]. ONE block is formed from ALL retained cards (the adapter applies the central
 * ≥20 min-n over valid cards, so one growing block reaches VALID honestly).
 *
 * `DetectorCoverage.ADEQUATE` is legitimate because the anti-gaming IS the scorer's gates — base-rate
 * guessing collapses to chance via balanced accuracy, and a fully-constant responder → INVALIDATED —
 * tested against those synthetic adversaries; there is no separate detector. Provenance carries only a
 * pseudonymous user id, never the raw uid.
 *
 * CONTENT GATE (Fase 0): the live scenario source is degenerate (template description + coin-flip label),
 * so this fold marks the bank `SMALL_UNVALIDATED` → every result is `BANK_CONTAMINATED` (no profile update)
 * regardless of the responses, until a real validated bank exists. The discrimination/anti-gaming gates
 * above remain tested at the adapter boundary ([ReversibilityProfileAdapter] with a `PROCEDURAL` bank) for
 * when that bank lands; here they are superseded by the earlier bank gate.
 */
object ReversibilityScore {
    const val SCORING_VERSION = "e2-reversibility-v1"

    fun scoreOf(
        log: ReversibilityRecordLog,
        exerciseId: String,
        sessionId: String,
        userId: String,
        timestampMs: Long,
    ): ScoreResult {
        val blocks = listOf(ReversibilityScorer.scoreBlock(log.trials))
        val provenance = ScoreProvenance(
            exerciseId = exerciseId,
            moduleId = ScoreModuleId.DECISIONS,
            sessionId = sessionId,
            pseudoUserId = pseudoUserId(userId),
            timestampMs = timestampMs,
            scoringVersion = SCORING_VERSION,
        )
        return ReversibilityProfileAdapter.toScoreResult(
            blocks = blocks,
            provenance = provenance,
            detectorCoverage = DetectorCoverage.ADEQUATE,
            // The live content source (`ReversibilityEngine.generateScenario`) is DEGENERATE: a generic
            // template description ("Reversibility triage level N") with a COIN-FLIP `isReversible` label,
            // so the task is unjudgeable and any discrimination signal would be fabricated. Marking the
            // bank SMALL_UNVALIDATED routes E2 to BANK_CONTAMINATED (no profile update) until a real,
            // validated scenario bank exists — the exercise stays playable as a minigame.
            bankIntegrity = BankIntegrity.SMALL_UNVALIDATED,
        )
    }
}
