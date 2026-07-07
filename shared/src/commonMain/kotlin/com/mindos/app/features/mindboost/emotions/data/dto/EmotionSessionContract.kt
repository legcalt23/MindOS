@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.emotions.data.dto

/**
 * The Emociones session-data contract authority (MC8u-C §3). It documents IN CODE — not only in a report — which PRD
 * session fields are Not Applicable to Emociones BY CHARTER, so a future change to add them is a conscious charter
 * decision, not an accident. A test pins these sets. Everything else in the PRD contract IS persisted explicitly on
 * [EmotionSessionSummaryDto] (identity, counts, accuracy, RT stats, difficulty start/end, quality, versions, block
 * score, calibration eligibility).
 */
object EmotionSessionContract {

    /**
     * Fields Not Applicable BY CHARTER (never faked):
     *  - rawScore / scoreDelta / xpEarned: Emociones has no per-session cognitive score beyond the 0–100 training
     *    performance block score ([normalizedScore]); a raw/delta/XP economy would re-introduce the removed "loaded gun".
     *  - durationSelected / blockId / maxLevelReached: the session is a fixed two-block unit on a single governing axis;
     *    there is no user-selected duration, no per-block id, and difficultyEnd IS the max level reached.
     *  - platform / syncStatus: local-only encrypted storage; no remote sync exists yet (documented P2 debt).
     */
    val NOT_APPLICABLE_BY_CHARTER: Set<String> = setOf(
        "rawScore", "scoreDelta", "xpEarned",
        "durationSelected", "blockId", "maxLevelReached",
        "platform", "syncStatus",
    )

    /** PRD session fields that MUST be persisted explicitly on the summary (a test pins that each is present). */
    val REQUIRED_EXPLICIT_FIELDS: Set<String> = setOf(
        "sessionId", "exerciseId", "durationMs", "difficultyStart", "difficultyEnd",
        "totalAnswers", "correctAnswers", "incorrectAnswers", "omissions", "accuracy",
        "averageReactionTimeMs", "bestReactionTimeMs", "worstReactionTimeMs", "reactionTimeVariability",
        "normalizedScore", "calibrationStatus", "anomalyFlags", "governingConfident",
        "completed", "abandoned", "exerciseVersion", "scoringVersion",
    )
}
