package com.mindos.speech.mock

import com.mindos.speech.BackendFailureReason
import com.mindos.speech.SynthesizedAudio
import com.mindos.speech.TTSBackend
import com.mindos.speech.TtsRequest
import com.mindos.speech.TtsResult

sealed interface MockTtsBehavior {
    data class Return(val audio: SynthesizedAudio) : MockTtsBehavior
    data class Fail(val reason: BackendFailureReason) : MockTtsBehavior
}

data class ObservedTtsRequest(
    val textLength: Int,
    val languageTag: String,
    val hasVoiceId: Boolean,
)

class MockTTSBackend(
    private val behavior: MockTtsBehavior,
) : TTSBackend {
    var invocationCount: Int = 0
        private set

    var lastObservedRequest: ObservedTtsRequest? = null
        private set

    override suspend fun synthesize(request: TtsRequest): TtsResult {
        invocationCount += 1
        lastObservedRequest = ObservedTtsRequest(
            textLength = request.text.length,
            languageTag = request.languageTag,
            hasVoiceId = request.voiceId != null,
        )
        return when (behavior) {
            is MockTtsBehavior.Return -> TtsResult.Success(behavior.audio)
            is MockTtsBehavior.Fail -> TtsResult.Failure(behavior.reason)
        }
    }

    companion object {
        fun success(audio: SynthesizedAudio = SpeechFixtures.synthesizedAudio()) =
            MockTTSBackend(MockTtsBehavior.Return(audio))

        fun timeout() = MockTTSBackend(MockTtsBehavior.Fail(BackendFailureReason.TIMEOUT))

        fun backendError() = MockTTSBackend(MockTtsBehavior.Fail(BackendFailureReason.BACKEND_ERROR))

        fun emptyAudio() = success(SpeechFixtures.synthesizedAudio(bytes = ByteArray(0), durationMs = 0L))
    }
}
