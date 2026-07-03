package com.mindos.speech

data class SpeechBrokerTranscript(
    val requestId: String,
    val sessionId: String?,
    val text: String,
    val confidence: Float?,
    val languageTag: String,
    val durationMs: Long,
    val backendId: String,
    val isFinal: Boolean,
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(sessionId == null || sessionId.isNotBlank()) { "sessionId must be null or non-blank." }
        require(confidence == null || confidence in 0.0f..1.0f) {
            "confidence must be null or between 0 and 1."
        }
        require(languageTag.isNotBlank()) { "languageTag must not be blank." }
        require(durationMs >= 0) { "durationMs cannot be negative." }
        require(backendId.isNotBlank()) { "backendId must not be blank." }
    }
}

sealed interface SpeechBrokerResult {
    data class Success(val transcript: SpeechBrokerTranscript) : SpeechBrokerResult
    data object Timeout : SpeechBrokerResult
    data object Rejected : SpeechBrokerResult
    data object Unavailable : SpeechBrokerResult
}

fun interface SpeechBrokerTransport {
    suspend fun transcribe(input: AudioInput): SpeechBrokerResult
}

data class BrokerAsrConfig(
    val requiredSampleRateHz: Int = 16_000,
    val requiredChannelCount: Int = 1,
    val maxAudioDurationMs: Long = 10_000L,
    val maxTranscriptLength: Int = 500,
) {
    init {
        require(requiredSampleRateHz > 0) { "requiredSampleRateHz must be positive." }
        require(requiredChannelCount > 0) { "requiredChannelCount must be positive." }
        require(maxAudioDurationMs > 0) { "maxAudioDurationMs must be positive." }
        require(maxTranscriptLength > 0) { "maxTranscriptLength must be positive." }
    }
}

class BrokerAsrBackend(
    private val transport: SpeechBrokerTransport,
    private val config: BrokerAsrConfig = BrokerAsrConfig(),
) : ASRBackend {
    override suspend fun transcribe(input: AudioInput): AsrResult {
        if (!input.isValidRequest()) {
            return AsrResult.Failure(BackendFailureReason.INVALID_INPUT)
        }

        return when (val result = transport.transcribe(input)) {
            is SpeechBrokerResult.Success -> result.transcript.toAsrResult(input)
            SpeechBrokerResult.Timeout -> AsrResult.Failure(BackendFailureReason.TIMEOUT)
            SpeechBrokerResult.Rejected,
            SpeechBrokerResult.Unavailable,
            -> AsrResult.Failure(BackendFailureReason.BACKEND_ERROR)
        }
    }

    private fun AudioInput.isValidRequest(): Boolean =
        !payload.isEmpty &&
            durationMs in 1..config.maxAudioDurationMs &&
            format.sampleRateHz == config.requiredSampleRateHz &&
            format.channelCount == config.requiredChannelCount &&
            format.encoding == AudioEncoding.PCM_SIGNED_16_LE

    private fun SpeechBrokerTranscript.toAsrResult(input: AudioInput): AsrResult {
        if (
            requestId != input.requestId ||
            sessionId != input.sessionId ||
            languageTag != input.languageTag ||
            text.length > config.maxTranscriptLength
        ) {
            return AsrResult.Failure(BackendFailureReason.BACKEND_ERROR)
        }
        return AsrResult.Success(
            TranscriptResult(
                text = text,
                confidence = confidence,
                languageTag = languageTag,
                durationMs = durationMs,
                backendId = backendId,
                isFinal = isFinal,
            ),
        )
    }
}
