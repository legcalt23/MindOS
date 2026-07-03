package com.mindos.app.features.arena.rating.model

import com.mindos.app.features.arena.config.ComparabilityStateEnum
import com.mindos.app.features.arena.config.ConfidenceStateEnum
import com.mindos.app.features.arena.config.IntegrityStateEnum
import com.mindos.app.features.arena.config.SessionValidityStatusEnum
import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Comparability Decision
 */
@Serializable
data class ComparabilityDecision(
    val state: ComparabilityStateEnum,
    val updateMultiplier: Double,
    val reasonCode: String,
    val sourceEngine: String
)

/**
 * Phase 02: Auditable Data Model
 * Measurement Confidence Decision
 */
@Serializable
data class MeasurementConfidenceDecision(
    val state: ConfidenceStateEnum,
    val updateMultiplier: Double,
    val reasonCode: String,
    val sourceEngine: String,
    val sem: SEM,
    val confidenceInterval: ConfidenceInterval
)

/**
 * Phase 02: Auditable Data Model
 * Integrity Gate Decision
 */
@Serializable
data class IntegrityGateDecision(
    val state: IntegrityStateEnum,
    val updateMultiplier: Double,
    val reasonCode: String,
    val sourceEngine: String
)

/**
 * Phase 02: Auditable Data Model
 * Session Validity Decision
 */
@Serializable
data class SessionValidityDecision(
    val status: SessionValidityStatusEnum,
    val updateMultiplier: Double,
    val reasonCode: String,
    val sourceEngine: String
)
