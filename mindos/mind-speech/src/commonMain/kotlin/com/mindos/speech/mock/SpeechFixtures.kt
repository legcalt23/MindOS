package com.mindos.speech.mock

import com.mindos.speech.AudioFormat
import com.mindos.speech.AudioInput
import com.mindos.speech.AudioPayload
import com.mindos.speech.SynthesizedAudio
import com.mindos.speech.TranscriptResult

object SpeechFixtures {
    val Mono16Khz = AudioFormat(sampleRateHz = 16_000, channelCount = 1)

    fun audioInput(
        bytes: ByteArray = byteArrayOf(1, 2, 3, 4),
        durationMs: Long = 250L,
        requestId: String = "voice-req",
        sessionId: String? = "voice-session",
        languageTag: String = "es-US",
    ): AudioInput = AudioInput(
        requestId = requestId,
        sessionId = sessionId,
        languageTag = languageTag,
        payload = AudioPayload.fromBytes(bytes),
        format = Mono16Khz,
        durationMs = durationMs,
    )

    fun transcript(
        text: String = "abre ajustes",
        confidence: Float? = 0.95f,
        languageTag: String = "es-US",
        isFinal: Boolean = true,
        alternatives: List<String> = emptyList(),
    ): TranscriptResult = TranscriptResult(
        text = text,
        confidence = confidence,
        languageTag = languageTag,
        durationMs = 250L,
        backendId = "mock-asr",
        isFinal = isFinal,
        alternatives = alternatives,
    )

    fun synthesizedAudio(
        bytes: ByteArray = byteArrayOf(5, 6, 7, 8),
        durationMs: Long = 300L,
    ): SynthesizedAudio = SynthesizedAudio(
        payload = AudioPayload.fromBytes(bytes),
        format = Mono16Khz,
        durationMs = durationMs,
        backendId = "mock-tts",
    )
}
