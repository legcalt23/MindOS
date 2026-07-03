package com.mindos.speech

enum class AudioEncoding {
    PCM_SIGNED_16_LE,
}

data class AudioFormat(
    val sampleRateHz: Int,
    val channelCount: Int,
    val encoding: AudioEncoding = AudioEncoding.PCM_SIGNED_16_LE,
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive." }
        require(channelCount > 0) { "channelCount must be positive." }
    }
}

class AudioPayload private constructor(
    private val content: ByteArray,
) {
    val sizeBytes: Int get() = content.size
    val isEmpty: Boolean get() = content.isEmpty()

    fun copyBytes(): ByteArray = content.copyOf()

    override fun equals(other: Any?): Boolean =
        other is AudioPayload && content.contentEquals(other.content)

    override fun hashCode(): Int = content.contentHashCode()

    override fun toString(): String = "AudioPayload(sizeBytes=$sizeBytes)"

    companion object {
        fun fromBytes(bytes: ByteArray): AudioPayload = AudioPayload(bytes.copyOf())
        fun empty(): AudioPayload = AudioPayload(ByteArray(0))
    }
}

data class AudioInput(
    val requestId: String,
    val sessionId: String?,
    val languageTag: String,
    val payload: AudioPayload,
    val format: AudioFormat,
    val durationMs: Long,
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank." }
        require(sessionId == null || sessionId.isNotBlank()) { "sessionId must be null or non-blank." }
        require(languageTag.isNotBlank()) { "languageTag must not be blank." }
        require(durationMs >= 0) { "durationMs cannot be negative." }
    }
}

data class TranscriptResult(
    val text: String,
    val confidence: Float?,
    val languageTag: String,
    val durationMs: Long,
    val backendId: String,
    val isFinal: Boolean,
    val alternatives: List<String> = emptyList(),
) {
    init {
        require(confidence == null || confidence in 0.0f..1.0f) {
            "confidence must be null or between 0 and 1."
        }
        require(languageTag.isNotBlank()) { "languageTag must not be blank." }
        require(durationMs >= 0) { "durationMs cannot be negative." }
        require(backendId.isNotBlank()) { "backendId must not be blank." }
    }
}

data class TtsRequest(
    val text: String,
    val languageTag: String,
    val voiceId: String? = null,
) {
    init {
        require(text.isNotBlank()) { "text must not be blank." }
        require(languageTag.isNotBlank()) { "languageTag must not be blank." }
        require(voiceId == null || voiceId.isNotBlank()) { "voiceId must be null or non-blank." }
    }
}

data class SynthesizedAudio(
    val payload: AudioPayload,
    val format: AudioFormat,
    val durationMs: Long,
    val backendId: String,
) {
    init {
        require(durationMs >= 0) { "durationMs cannot be negative." }
        require(backendId.isNotBlank()) { "backendId must not be blank." }
    }
}
