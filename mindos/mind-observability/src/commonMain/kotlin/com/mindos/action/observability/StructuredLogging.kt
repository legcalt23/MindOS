package com.mindos.action.observability

import kotlinx.serialization.Serializable

@Serializable
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
}

@Serializable
data class StructuredLogEvent(
    val requestId: String,
    val sessionId: String,
    val timestampMillis: Long,
    val action: String,
    val outcome: String,
    val latencyMs: Long,
    val level: LogLevel = LogLevel.INFO,
    val errorCode: String? = null,
    val attributes: Map<String, String> = emptyMap(),
) {
    init {
        require(requestId.isNotBlank()) { "StructuredLogEvent.requestId must not be blank." }
        require(sessionId.isNotBlank()) { "StructuredLogEvent.sessionId must not be blank." }
        require(action.isNotBlank()) { "StructuredLogEvent.action must not be blank." }
        require(outcome.isNotBlank()) { "StructuredLogEvent.outcome must not be blank." }
        require(latencyMs >= 0L) { "StructuredLogEvent.latencyMs cannot be negative." }
        attributes.forEach { (key, value) ->
            require(key.isSafeTelemetryKey()) { "Log attribute '$key' may contain sensitive data." }
            require(value.length <= MaxLogValueLength) { "Log attribute '$key' is too long." }
        }
    }

    companion object {
        const val UnassignedRequestId = "unassigned"
        const val AnonymousSessionId = "anonymous"
    }
}

fun interface StructuredLogSink {
    fun record(event: StructuredLogEvent)
}

object NoOpStructuredLogSink : StructuredLogSink {
    override fun record(event: StructuredLogEvent) = Unit
}

class RecordingStructuredLogSink : StructuredLogSink {
    private val _events = ArrayList<StructuredLogEvent>()
    val events: List<StructuredLogEvent> get() = _events.toList()

    override fun record(event: StructuredLogEvent) {
        _events += event
    }

    fun clear() = _events.clear()
}

internal const val MaxLogValueLength = 256
