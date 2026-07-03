package com.mindos.app.features.arena.rating.rollback

import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Rollback Reference
 * References a state to rollback to in case of invalidated sessions.
 */
@Serializable
data class RollbackReference(
    val rollbackId: String,
    val syntheticPlayerId: String,
    val targetTimestampMs: Long,
    val reasonCode: String,
    val executionTimestampMs: Long
)
