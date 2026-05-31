package com.mindos.action.observability

/**
 * Injectable telemetry boundary. The matching engine calls [record]; it never knows
 * the backend. Default is [NoOpMindTelemetrySink] so the hot path costs nothing.
 */
fun interface MindTelemetrySink {
    fun record(event: MindTelemetryEvent)
}

/** Production-safe default: discards everything. */
object NoOpMindTelemetrySink : MindTelemetrySink {
    override fun record(event: MindTelemetryEvent) = Unit
}

/** Test/diagnostic sink that retains events in order. Not for production hot paths. */
class RecordingTelemetrySink : MindTelemetrySink {
    private val _events = ArrayList<MindTelemetryEvent>()
    val events: List<MindTelemetryEvent> get() = _events.toList()
    override fun record(event: MindTelemetryEvent) {
        _events.add(event)
    }

    fun clear() = _events.clear()
}
