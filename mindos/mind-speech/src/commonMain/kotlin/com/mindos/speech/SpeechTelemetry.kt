package com.mindos.speech

enum class SpeechMetric(val key: String) {
    CAPTURE_DURATION_MS("voice.capture.duration_ms"),
    ASR_LATENCY_MS("voice.asr.latency_ms"),
    ASR_RESULT("voice.asr.result"),
    ASR_CONFIDENCE_BUCKET("voice.asr.confidence_bucket"),
    TRANSCRIPT_POLICY_RESULT("voice.transcript_policy.result"),
    ROUTER_LATENCY_MS("voice.router.latency_ms"),
    ROUTER_RESULT("voice.router.result"),
    CONFIRMATION_RESULT("voice.confirmation.result"),
    EXECUTION_LATENCY_MS("voice.execution.latency_ms"),
    TTS_LATENCY_MS("voice.tts.latency_ms"),
    OUTPUT_RESULT("voice.output.result"),
    E2E_LATENCY_MS("voice.e2e.latency_ms"),
}

enum class SpeechTelemetryResult {
    SUCCESS,
    REJECTED,
    TIMEOUT,
    BACKEND_ERROR,
    EMPTY,
    PARTIAL,
    LOW_CONFIDENCE,
    AMBIGUOUS,
    SAFETY_DENIED,
    NEEDS_CONFIRMATION,
    CONFIRMATION_REJECTED,
    NOT_REQUIRED,
    EXECUTED,
    EXECUTION_FAILED,
    DRY_RUN,
    DEGRADED,
    SKIPPED,
}

enum class SpeechConfidenceBucket {
    UNKNOWN,
    BELOW_THRESHOLD,
    LOW,
    MEDIUM,
    HIGH,
}

enum class SpeechTelemetryActionType {
    OPEN_PROFILE,
    OPEN_STORY,
    OPEN_STORY_OF_USER,
    OPEN_CHAT,
    OPEN_MESSAGES,
    OPEN_SETTINGS,
}

class SpeechTelemetryEvent internal constructor(
    val metric: SpeechMetric,
    val requestId: String,
    val sessionId: String?,
    val valueMs: Long? = null,
    val result: SpeechTelemetryResult? = null,
    val confidenceBucket: SpeechConfidenceBucket? = null,
    val backendId: String? = null,
    val localeTag: String? = null,
    val actionType: SpeechTelemetryActionType? = null,
) {
    override fun toString(): String =
        "SpeechTelemetryEvent(metric=$metric, requestId=<redacted>, sessionId=<redacted>, " +
            "valueMs=$valueMs, result=$result, confidenceBucket=$confidenceBucket, " +
            "backendId=$backendId, localeTag=$localeTag, actionType=$actionType)"
}

fun interface SpeechTelemetrySink {
    fun record(event: SpeechTelemetryEvent)
}

object NoOpSpeechTelemetrySink : SpeechTelemetrySink {
    override fun record(event: SpeechTelemetryEvent) = Unit
}

class RecordingSpeechTelemetrySink : SpeechTelemetrySink {
    private val mutableEvents = mutableListOf<SpeechTelemetryEvent>()
    val events: List<SpeechTelemetryEvent> get() = mutableEvents.toList()

    override fun record(event: SpeechTelemetryEvent) {
        mutableEvents += event
    }
}
