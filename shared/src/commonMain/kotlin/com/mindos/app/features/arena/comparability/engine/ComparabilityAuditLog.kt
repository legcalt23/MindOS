package com.mindos.app.features.arena.comparability.engine

import com.mindos.app.features.arena.audit.model.AuditLogEntry

/**
 * Phase 04: Auditable record of a single ComparabilityDecision, capturing every per-dimension reason
 * code (comparability_audit_log.md). Identifiers are synthetic only; no PII.
 */
@Suppress("LongParameterList") // Auditable record: each dimension tier is a required, distinct field.
data class ComparabilityAuditLog(
    override val syntheticId: String,
    override val syntheticReferenceId: String,
    override val timestampMs: Long,
    override val configVersion: String,
    override val decisionState: String,
    override val reasonCode: String,
    override val sourceEngine: String,
    val latencyTier: String,
    val jitterTier: String,
    val networkTier: String,
    val accessibilityTier: String,
) : AuditLogEntry
