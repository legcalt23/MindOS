package com.mindos.speech

import com.mindos.command.runtime.CommandDispatchResult
import com.mindos.command.runtime.ResponseText

data class VoicePipelineTimings(
    val captureMs: Long? = null,
    val asrMs: Long? = null,
    val transcriptPolicyMs: Long? = null,
    val gatewayMs: Long? = null,
    val responseTextMs: Long? = null,
    val ttsMs: Long? = null,
    val audioOutputMs: Long? = null,
    val e2eMs: Long,
)

enum class VoiceStage {
    CAPTURE,
    ASR,
    TRANSCRIPT_POLICY,
    ROUTING,
    SAFETY,
    EXECUTION,
    RESPONSE_TEXT,
    TTS,
    AUDIO_OUTPUT,
}

enum class VoiceRejectionReason {
    VOICE_DISABLED,
    CAPTURE_PERMISSION_DENIED,
    CAPTURE_TIMEOUT,
    CAPTURE_BACKEND_ERROR,
    EMPTY_AUDIO,
    AUDIO_TOO_LONG,
    AUDIO_CORRELATION_MISMATCH,
    ASR_TIMEOUT,
    ASR_BACKEND_ERROR,
    TRANSCRIPT_POLICY_ERROR,
    TRANSCRIPT_NOT_FINAL,
    TRANSCRIPT_EMPTY,
    CONFIDENCE_TOO_LOW,
    WAKE_WORD_MISSING,
    TRANSCRIPT_TOO_LONG,
    ROUTING_FAILED,
    ROUTING_TIMEOUT,
    AMBIGUOUS_COMMAND,
    SAFETY_DENIED,
    CONFIRMATION_EXPIRED,
    CONFIRMATION_REJECTED,
    RESPONSE_TEXT_ERROR,
    TTS_TIMEOUT,
    TTS_BACKEND_ERROR,
    TTS_EMPTY_AUDIO,
    AUDIO_OUTPUT_TIMEOUT,
    AUDIO_OUTPUT_ERROR,
}

sealed interface VoiceCommandOutcome {
    val timings: VoicePipelineTimings

    data class Completed(
        val dispatch: CommandDispatchResult,
        val responseText: ResponseText,
        val spoken: Boolean,
        override val timings: VoicePipelineTimings,
    ) : VoiceCommandOutcome

    data class Rejected(
        val reason: VoiceRejectionReason,
        override val timings: VoicePipelineTimings,
    ) : VoiceCommandOutcome

    data class Degraded(
        val dispatch: CommandDispatchResult,
        val responseText: ResponseText,
        val reason: VoiceRejectionReason,
        override val timings: VoicePipelineTimings,
    ) : VoiceCommandOutcome

    data class Failed(
        val stage: VoiceStage,
        val reason: VoiceRejectionReason,
        override val timings: VoicePipelineTimings,
    ) : VoiceCommandOutcome
}
