package com.mindos.speech

enum class BackendFailureReason {
    TIMEOUT,
    BACKEND_ERROR,
    INVALID_INPUT,
}

sealed interface AsrResult {
    data class Success(val transcript: TranscriptResult) : AsrResult
    data class Failure(val reason: BackendFailureReason) : AsrResult
}

sealed interface TtsResult {
    data class Success(val audio: SynthesizedAudio) : TtsResult
    data class Failure(val reason: BackendFailureReason) : TtsResult
}

enum class CaptureFailureReason {
    PERMISSION_DENIED,
    TIMEOUT,
    EMPTY_AUDIO,
    BACKEND_ERROR,
}

sealed interface SpeechInputResult {
    data class Captured(val audio: AudioInput) : SpeechInputResult
    data class Failure(val reason: CaptureFailureReason) : SpeechInputResult
}

enum class AudioOutputFailureReason {
    TIMEOUT,
    EMPTY_AUDIO,
    BACKEND_ERROR,
}

sealed interface AudioOutputResult {
    data object Played : AudioOutputResult
    data class Failure(val reason: AudioOutputFailureReason) : AudioOutputResult
}

fun interface ASRBackend {
    suspend fun transcribe(input: AudioInput): AsrResult
}

fun interface TTSBackend {
    suspend fun synthesize(request: TtsRequest): TtsResult
}

fun interface SpeechInputProvider {
    suspend fun capture(session: VoiceSession): SpeechInputResult
}

fun interface AudioOutputProvider {
    suspend fun play(audio: SynthesizedAudio): AudioOutputResult
}
