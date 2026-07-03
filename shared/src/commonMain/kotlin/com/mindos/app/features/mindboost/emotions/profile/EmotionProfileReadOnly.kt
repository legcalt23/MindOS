package com.mindos.app.features.mindboost.emotions.profile

import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend

/**
 * Read-only local aggregate the FUTURE `MindBoostProfile` can consume. Exposes ONLY coarse, non-PII
 * signals — never raw trials, never the calibration score, never anything that could be uploaded.
 * Dependency direction is one-way: emotions exposes this; the global profile consumes it.
 */
data class EmotionProfileSnapshot(
    val tier: ProfileTier,
    val dataSufficiency: Double,
    val confidence: Double,
    val trend: ProfileTrend,
)

/** Local, read-only access to the emotion regulation snapshot. No upload, no trials, no calibration. */
interface EmotionProfileReadOnly {
    suspend fun snapshot(userId: String): EmotionProfileSnapshot?
}
