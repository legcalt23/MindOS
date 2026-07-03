package com.mindos.app.features.arena.audit.model

/**
 * Phase 01: Base Audit Log Model
 * Every auditable entity must include these fields.
 * NO real PII or production identifiers are allowed.
 */
interface AuditLogEntry {
    val syntheticId: String
    val syntheticReferenceId: String // player/session/match reference
    val timestampMs: Long
    val configVersion: String
    val decisionState: String
    val reasonCode: String
    val sourceEngine: String
}

/**
 * Example concrete audit log entry for a rating mutation.
 */
data class RatingMutationAuditLog(
    override val syntheticId: String,
    override val syntheticReferenceId: String,
    override val timestampMs: Long,
    override val configVersion: String,
    override val decisionState: String,
    override val reasonCode: String,
    override val sourceEngine: String,
    val previousRatingMu: Double,
    val newRatingMu: Double,
    val previousRatingSigma: Double,
    val newRatingSigma: Double
) : AuditLogEntry
