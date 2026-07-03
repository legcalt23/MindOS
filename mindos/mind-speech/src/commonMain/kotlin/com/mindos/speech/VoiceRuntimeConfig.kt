package com.mindos.speech

data class VoiceRuntimeConfig(
    val enabled: Boolean = false,
    val killSwitch: Boolean = false,
    val captureTimeoutMs: Long = 12_000L,
    val asrTimeoutMs: Long = 12_000L,
    val routingTimeoutMs: Long = 2_000L,
    val ttsTimeoutMs: Long = 8_000L,
    val outputTimeoutMs: Long = 10_000L,
    val ttsEnabled: Boolean = true,
    val defaultLanguageTag: String = "es-US",
    val confidenceThreshold: Float = 0.80f,
    val acceptNullConfidence: Boolean = true,
    val audioSampleRateHz: Int = 16_000,
    val audioMaxDurationMs: Long = 10_000L,
    val requireWakeWord: Boolean = false,
    val wakeWords: Set<String> = setOf("MindVerse", "MindOS"),
    val logTranscripts: Boolean = false,
    val dryRun: Boolean = false,
    val streamingEnabled: Boolean = false,
) {
    val canRun: Boolean get() = enabled && !killSwitch

    init {
        require(captureTimeoutMs > 0) { "captureTimeoutMs must be positive." }
        require(asrTimeoutMs > 0) { "asrTimeoutMs must be positive." }
        require(routingTimeoutMs > 0) { "routingTimeoutMs must be positive." }
        require(ttsTimeoutMs > 0) { "ttsTimeoutMs must be positive." }
        require(outputTimeoutMs > 0) { "outputTimeoutMs must be positive." }
        require(defaultLanguageTag.isNotBlank()) { "defaultLanguageTag must not be blank." }
        require(confidenceThreshold in 0.0f..1.0f) { "confidenceThreshold must be between 0 and 1." }
        require(audioSampleRateHz > 0) { "audioSampleRateHz must be positive." }
        require(audioMaxDurationMs > 0) { "audioMaxDurationMs must be positive." }
        require(wakeWords.all { it.isNotBlank() }) { "wakeWords must not contain blanks." }
        require(!requireWakeWord || wakeWords.isNotEmpty()) {
            "wakeWords must not be empty when requireWakeWord is true."
        }
        require(!logTranscripts) { "Raw transcript logging is prohibited in the speech core." }
        require(!streamingEnabled) { "Streaming is outside the supported voice foundation phase." }
    }
}
