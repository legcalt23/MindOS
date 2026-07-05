package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult

/**
 * EX3 deferred recall (P3) · Derives the universal [ScoreResult] from delayed-recall blocks by DELEGATING to
 * the shared [RecognitionScoreAdapter] (signal `DELAYED_RECALL_ACCURACY`). That index is the ONLY profile
 * signal (accuracy-based, `RT_IRRELEVANT` — RT never flows to the profile); the bank integrity is DERIVED
 * from the central authority for [BANK_ID] (B-1, unregistered → blocks the profile); the session-level min-n
 * comes from the central `MinSampleGate` (`ACCURACY_RT_BASE`). This is the catalog's HONEST governing metric
 * for EX3 (24h retention without re-study), measured between sessions.
 *
 * Gaming defense: like the EX4 pairs path, the anti-gaming IS the scorer's discrimination-validity gate
 * (a "select-everything" delayed test over-selects lures → INVALIDATED), tested against that synthetic
 * adversary — NOT a separate detector. A caller that has run that tested gate passes `ADEQUATE`; before a
 * live path is wired, [detectorCoverage] defaults to `NONE` so even a perfect session caps at `CALIBRATING`.
 */
object DeferredRecallProfileAdapter {

    /** Bank identity of the EX3 deferred-recall generator (NOT yet registered in `ProfileUpdatingBanks`). */
    const val BANK_ID = "ex3-deferred-recall"

    fun toScoreResult(
        blocks: List<DeferredRecallBlockResult>,
        provenance: ScoreProvenance,
        detectorCoverage: DetectorCoverage = DetectorCoverage.NONE,
        antiGamingClean: Boolean = true,
        bankRef: BankRef = BankRef(BANK_ID),
    ): ScoreResult =
        RecognitionScoreAdapter.toScoreResult(
            blocks = blocks.map {
                RecognitionBlockResult(
                    state = when (it.state) {
                        DeferredRecallBlockState.VALID -> RecognitionBlockState.VALID
                        DeferredRecallBlockState.INVALIDATED -> RecognitionBlockState.INVALIDATED
                        DeferredRecallBlockState.CALIBRATING -> RecognitionBlockState.CALIBRATING
                    },
                    index = it.delayedRecallIndex,
                    falseAlarmRate = it.falseAlarmRate,
                    validTrials = it.validTrials,
                )
            },
            signalKey = "DELAYED_RECALL_ACCURACY",
            provenance = provenance,
            bankRef = bankRef,
            detectorCoverage = detectorCoverage,
            antiGamingClean = antiGamingClean,
        )
}
