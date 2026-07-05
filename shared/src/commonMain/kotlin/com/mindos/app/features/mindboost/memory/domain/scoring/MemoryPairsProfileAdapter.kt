package com.mindos.app.features.mindboost.memory.domain.scoring

import com.mindos.app.features.mindboost.core.bank.BankRef
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult

/**
 * P1 EX4 · Derives the universal [ScoreResult] from paired-associates blocks by DELEGATING to the shared
 * [RecognitionScoreAdapter] (signal `ASSOCIATIVE_INDEX`). The associative index is the ONLY profile signal
 * (accuracy-based, `RT_IRRELEVANT` — RT never flows to the profile); the bank integrity is DERIVED from the
 * central authority for [BANK_ID] (B-1, unregistered → blocks the profile); the session-level min-n comes
 * from the central `MinSampleGate` (`ACCURACY_RT_BASE`).
 *
 * Gaming defense: EX4's anti-gaming IS the scorer's discrimination-validity gate (a "select-everything"
 * session over-selects lures → INVALIDATED), tested against that synthetic adversary — NOT a separate
 * entropy detector. A caller that has run that tested gate passes `ADEQUATE`; before a live path is wired,
 * [detectorCoverage] defaults to `NONE` so even a perfect session caps at `CALIBRATING`.
 */
object MemoryPairsProfileAdapter {

    /** Bank identity of the EX4 associative-pairs generator (NOT yet registered in `ProfileUpdatingBanks`). */
    const val BANK_ID = "ex4-associative-pairs"

    fun toScoreResult(
        blocks: List<PairsBlockResult>,
        provenance: ScoreProvenance,
        detectorCoverage: DetectorCoverage = DetectorCoverage.NONE,
        antiGamingClean: Boolean = true,
        bankRef: BankRef = BankRef(BANK_ID),
    ): ScoreResult =
        RecognitionScoreAdapter.toScoreResult(
            blocks = blocks.map {
                RecognitionBlockResult(
                    state = when (it.state) {
                        PairsBlockState.VALID -> RecognitionBlockState.VALID
                        PairsBlockState.INVALIDATED -> RecognitionBlockState.INVALIDATED
                        PairsBlockState.CALIBRATING -> RecognitionBlockState.CALIBRATING
                    },
                    index = it.associativeIndex,
                    falseAlarmRate = it.falseAlarmRate,
                    validTrials = it.validTrials,
                )
            },
            signalKey = "ASSOCIATIVE_INDEX",
            provenance = provenance,
            bankRef = bankRef,
            detectorCoverage = detectorCoverage,
            antiGamingClean = antiGamingClean,
        )
}
