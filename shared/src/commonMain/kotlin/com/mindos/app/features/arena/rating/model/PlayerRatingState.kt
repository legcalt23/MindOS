package com.mindos.app.features.arena.rating.model

import com.mindos.app.features.arena.config.ActivityStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.MinorStateEnum
import com.mindos.app.features.arena.config.VerificationStateEnum
import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Player Default Profile
 */
@Serializable
data class PlayerDefaultProfile(
    val creationTimestampMs: Long,
    val initialRating: Rating,
    val initialSigma: Sigma
)

/**
 * Phase 02: Auditable Data Model
 * Block Rating State
 */
@Serializable
data class BlockRatingState(
    val blockId: String,
    val ratingMu: Rating,
    val ratingSigma: Sigma,
    val lastUpdateTimestampMs: Long
)

/**
 * Phase 02: Auditable Data Model
 * Mode Rating State
 */
@Serializable
data class ModeRatingState(
    val modeId: String,
    val ratingMu: Rating,
    val ratingSigma: Sigma,
    val peakRatingMu: Rating,
    val lastUpdateTimestampMs: Long,
    val matchesPlayed: Int
)

/**
 * Phase 02: Auditable Data Model
 * Core Player Rating State
 */
@Serializable
data class PlayerRatingState(
    val playerIdPseudonymized: String,
    val globalRatingMu: Rating,
    val globalRatingSigma: Sigma,
    val blockRatings: Map<String, BlockRatingState>,
    val modeRatings: Map<String, ModeRatingState>,
    val provisionalState: Boolean,
    val activityState: ActivityStateEnum,
    val verificationState: VerificationStateEnum,
    val integrityState: IntegrityStateEnum,
    val minorState: MinorStateEnum,
    val regionState: String,
    val defaultProfile: PlayerDefaultProfile,
    val lastRatingUpdateTimestampMs: Long,
    val seasonId: String
)
