package com.mindos.app.features.arena.integrity.review

import kotlinx.serialization.Serializable

/**
 * Phase 02: Auditable Data Model
 * Post Review Case
 * Represents a human review required for a suspected integrity breach in simulation.
 */
@Serializable
data class PostReviewCase(
    val syntheticCaseId: String,
    val syntheticPlayerId: String,
    val creationTimestampMs: Long,
    val resolutionTimestampMs: Long?,
    val resolutionDecision: String, // e.g., FALSE_POSITIVE, CONFIRMED
    val reviewNotes: String
)
