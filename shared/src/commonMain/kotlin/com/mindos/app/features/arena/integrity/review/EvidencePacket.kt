package com.mindos.app.features.arena.integrity.review

import com.mindos.app.features.arena.integrity.model.IntegritySignals

/**
 * Phase 05: Pseudonymized evidence packet handed to Ops for human review (evidence_packet_format.md).
 *
 * Carries only synthetic references and aggregate scalars — a synthetic case/player id, a synthetic
 * session-replay reference, input entropy, geo-velocity, and the triggered integrity signals. There
 * are deliberately NO raw device ids, IPs, emails, or other PII fields.
 */
data class EvidencePacket(
    val syntheticCaseId: String,
    val syntheticPlayerId: String,
    val sessionReplayReference: String,
    val inputEntropy: Double,
    val geoVelocityKmh: Double,
    val signals: IntegritySignals,
    val createdTimestampMs: Long,
) {
    init {
        require(syntheticCaseId.isNotBlank()) { "syntheticCaseId must not be blank" }
        require(syntheticPlayerId.isNotBlank()) { "syntheticPlayerId must not be blank" }
        require(sessionReplayReference.isNotBlank()) { "sessionReplayReference must not be blank" }
        require(inputEntropy >= 0.0 && !inputEntropy.isNaN()) { "inputEntropy must be >= 0" }
        require(geoVelocityKmh >= 0.0 && !geoVelocityKmh.isNaN()) { "geoVelocityKmh must be >= 0" }
        require(createdTimestampMs >= 0) { "createdTimestampMs must be >= 0" }
    }
}
