@file:Suppress("MaxLineLength") // Mapping expressions read best whole.

package com.mindos.app.features.mindboost.emotions.coach

import com.mindos.app.features.mindboost.coach.domain.CoachCalibrationState
import com.mindos.app.features.mindboost.coach.domain.CoachConfidenceLevel
import com.mindos.app.features.mindboost.coach.domain.CoachContribution
import com.mindos.app.features.mindboost.coach.domain.CoachContributionSource
import com.mindos.app.features.mindboost.coach.domain.CoachRecommendation
import com.mindos.app.features.mindboost.coach.domain.CoachRecommendationType
import com.mindos.app.features.mindboost.emotions.data.local.EmotionRepository

/**
 * MC8u-F — adapts Emociones to the app-wide AI Coach contract. It reuses the already-privacy-safe
 * [EmotionCoachSummaryBuilder] (patterns + calibration only) and maps it to a coarse [CoachContribution]. Honoring the
 * charter: NO "you are good/bad at X" — `strengths`/`weaknesses` stay empty; only what was PRACTICED becomes `patterns`,
 * and the block's interpretation limits + non-clinical disclaimer become `cautions`. Returns null until there is data.
 */
class EmotionCoachContributionSource(private val repository: EmotionRepository) : CoachContributionSource {

    override val blockId: String = BLOCK_ID

    override suspend fun contribute(userId: String): CoachContribution? {
        val summaries = repository.recentSummaries(userId)
        if (summaries.isEmpty()) return null
        val summary = EmotionCoachSummaryBuilder.build(summaries, repository.readAggregate(userId))
        return CoachContribution(
            blockId = blockId,
            calibrationState = when {
                summary.calibrated -> CoachCalibrationState.ESTABLISHED
                summary.validSessions > 0 -> CoachCalibrationState.PROVISIONAL
                else -> CoachCalibrationState.CALIBRATING
            },
            confidence = when {
                summary.calibrated -> CoachConfidenceLevel.HIGH
                summary.validSessions >= MEDIUM_CONFIDENCE_SESSIONS -> CoachConfidenceLevel.MEDIUM
                else -> CoachConfidenceLevel.LOW
            },
            patterns = summary.practicedCapacities,
            recommendations = if (!summary.calibrated) {
                listOf(CoachRecommendation(CoachRecommendationType.PRACTICE_MORE, "Completa más sesiones válidas para una lectura estable.", blockId))
            } else {
                emptyList()
            },
            cautions = listOf(summary.interpretationLimits, summary.disclaimer),
        )
    }

    private companion object {
        const val BLOCK_ID = "EMOCIONES"
        const val MEDIUM_CONFIDENCE_SESSIONS = 3
    }
}
