package com.mindos.speech.mock

import com.mindos.speech.ASRBackend
import com.mindos.speech.AsrResult
import com.mindos.speech.AudioInput
import com.mindos.speech.BackendFailureReason
import com.mindos.speech.TranscriptResult

sealed interface MockAsrBehavior {
    data class Return(val transcript: TranscriptResult) : MockAsrBehavior
    data class Fail(val reason: BackendFailureReason) : MockAsrBehavior
}

data class ObservedAudioInput(
    val sizeBytes: Int,
    val durationMs: Long,
    val sampleRateHz: Int,
    val channelCount: Int,
)

class MockASRBackend(
    private val behavior: MockAsrBehavior,
) : ASRBackend {
    var invocationCount: Int = 0
        private set

    var lastObservedInput: ObservedAudioInput? = null
        private set

    override suspend fun transcribe(input: AudioInput): AsrResult {
        invocationCount += 1
        lastObservedInput = ObservedAudioInput(
            sizeBytes = input.payload.sizeBytes,
            durationMs = input.durationMs,
            sampleRateHz = input.format.sampleRateHz,
            channelCount = input.format.channelCount,
        )
        return when (behavior) {
            is MockAsrBehavior.Return -> AsrResult.Success(behavior.transcript)
            is MockAsrBehavior.Fail -> AsrResult.Failure(behavior.reason)
        }
    }

    companion object {
        fun success(transcript: TranscriptResult = SpeechFixtures.transcript()) =
            MockASRBackend(MockAsrBehavior.Return(transcript))

        fun lowConfidence(confidence: Float = 0.25f) =
            success(SpeechFixtures.transcript(confidence = confidence))

        fun emptyTranscript() = success(SpeechFixtures.transcript(text = ""))

        fun timeout() = MockASRBackend(MockAsrBehavior.Fail(BackendFailureReason.TIMEOUT))

        fun backendError() = MockASRBackend(MockAsrBehavior.Fail(BackendFailureReason.BACKEND_ERROR))

        fun partial() = success(SpeechFixtures.transcript(isFinal = false))

        fun withAlternatives(alternatives: List<String>) =
            success(SpeechFixtures.transcript(alternatives = alternatives))
    }
}
