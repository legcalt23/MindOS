package com.mindos.app.features.arena.session.model

import com.mindos.app.features.arena.rating.model.JitterMs
import com.mindos.app.features.arena.rating.model.LatencyMs
import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Session Environment Snapshot
 */
@Serializable
data class SessionEnvironmentSnapshot(
    val deviceClass: String,
    val inputClass: String,
    val fpsBand: Int,
    val refreshRateBand: Int,
    val screenSizeBand: String,
    val latencyMs: LatencyMs,
    val jitterMs: JitterMs,
    val networkInstability: Double,
    val languageGroup: String,
    val accessibilityClass: String,
    val clientVersion: String,
    val osVersion: String,
    val appBuild: String,
    val timestampMs: Long
)

/**
 * Phase 02: Auditable Data Model
 * Session Result
 * Synthetic ID must be used.
 */
@Serializable
data class SessionResult(
    val syntheticSessionId: String,
    val syntheticPlayerId: String,
    val environmentSnapshot: SessionEnvironmentSnapshot,
    val motorPerformanceRaw: Double, // The raw task outcome
    val timestampMs: Long
)
