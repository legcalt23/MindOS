package com.mindos.action.observability

import kotlinx.serialization.Serializable

/** Terminal outcome of an action execution, recorded immutably in the audit trail. */
@Serializable
enum class AuditOutcome {
    EXECUTED,
    VALIDATION_REJECTED,
    RESOLUTION_FAILED,
    NAVIGATION_FAILED,
    INPUT_REJECTED,
    RATE_LIMITED,
    IDEMPOTENT_REPLAY,
    REPLAY_REJECTED,
}

/**
 * An immutable audit record (ZIP03 AUDIT_TRAIL): who/what/when/result. PII-free by contract —
 * callers pass stable refs/ids, never raw user text. The audit trail is append-only.
 */
@Serializable
data class AuditEvent(
    val requestId: String,
    val actionType: String,
    val timestamp: Long,
    val outcome: AuditOutcome,
    val errorCode: String? = null,
)

/** Injectable, append-only audit boundary. Default discards. The app forwards to durable storage. */
fun interface AuditSink {
    fun record(event: AuditEvent)
}

object NoOpAuditSink : AuditSink {
    override fun record(event: AuditEvent) = Unit
}

/** Test/diagnostic sink retaining events in order. Not for production. */
class RecordingAuditSink : AuditSink {
    private val _events = ArrayList<AuditEvent>()
    val events: List<AuditEvent> get() = _events.toList()
    override fun record(event: AuditEvent) {
        _events.add(event)
    }
}
