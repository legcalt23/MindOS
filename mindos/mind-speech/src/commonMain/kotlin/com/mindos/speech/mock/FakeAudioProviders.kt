package com.mindos.speech.mock

import com.mindos.speech.AudioOutputFailureReason
import com.mindos.speech.AudioOutputProvider
import com.mindos.speech.AudioOutputResult
import com.mindos.speech.CaptureFailureReason
import com.mindos.speech.SpeechInputProvider
import com.mindos.speech.SpeechInputResult
import com.mindos.speech.SynthesizedAudio
import com.mindos.speech.VoiceSession

class FakeSpeechInputProvider(
    private val result: SpeechInputResult,
) : SpeechInputProvider {
    var invocationCount: Int = 0
        private set

    override suspend fun capture(session: VoiceSession): SpeechInputResult {
        invocationCount += 1
        return when (result) {
            is SpeechInputResult.Captured -> SpeechInputResult.Captured(
                result.audio.copy(
                    requestId = session.requestId,
                    sessionId = session.sessionId,
                    languageTag = session.localeTag,
                ),
            )
            is SpeechInputResult.Failure -> result
        }
    }

    companion object {
        fun captured() = FakeSpeechInputProvider(SpeechInputResult.Captured(SpeechFixtures.audioInput()))
        fun permissionDenied() =
            FakeSpeechInputProvider(SpeechInputResult.Failure(CaptureFailureReason.PERMISSION_DENIED))
        fun timeout() = FakeSpeechInputProvider(SpeechInputResult.Failure(CaptureFailureReason.TIMEOUT))
        fun emptyAudio() =
            FakeSpeechInputProvider(
                SpeechInputResult.Captured(SpeechFixtures.audioInput(bytes = ByteArray(0), durationMs = 0L)),
            )
        fun backendError() =
            FakeSpeechInputProvider(SpeechInputResult.Failure(CaptureFailureReason.BACKEND_ERROR))
    }
}

enum class FakeAudioOutputBehavior {
    PLAY,
    TIMEOUT,
    BACKEND_ERROR,
}

class FakeAudioOutputProvider(
    private val behavior: FakeAudioOutputBehavior = FakeAudioOutputBehavior.PLAY,
) : AudioOutputProvider {
    var invocationCount: Int = 0
        private set

    var lastObservedSizeBytes: Int? = null
        private set

    override suspend fun play(audio: SynthesizedAudio): AudioOutputResult {
        invocationCount += 1
        lastObservedSizeBytes = audio.payload.sizeBytes
        if (audio.payload.isEmpty) {
            return AudioOutputResult.Failure(AudioOutputFailureReason.EMPTY_AUDIO)
        }
        return when (behavior) {
            FakeAudioOutputBehavior.PLAY -> AudioOutputResult.Played
            FakeAudioOutputBehavior.TIMEOUT -> AudioOutputResult.Failure(AudioOutputFailureReason.TIMEOUT)
            FakeAudioOutputBehavior.BACKEND_ERROR ->
                AudioOutputResult.Failure(AudioOutputFailureReason.BACKEND_ERROR)
        }
    }
}
