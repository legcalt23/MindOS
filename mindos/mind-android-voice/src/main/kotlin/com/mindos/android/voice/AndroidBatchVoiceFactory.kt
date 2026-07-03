package com.mindos.android.voice

import android.content.Context
import com.mindos.app.runtime.MindVoiceRuntimeFactory
import com.mindos.command.runtime.DefaultResponseTextResolver
import com.mindos.command.runtime.TextCommandGateway
import com.mindos.speech.AudioOutputFailureReason
import com.mindos.speech.AudioOutputProvider
import com.mindos.speech.AudioOutputResult
import com.mindos.speech.BackendFailureReason
import com.mindos.speech.BrokerAsrBackend
import com.mindos.speech.DefaultTranscriptPolicy
import com.mindos.speech.DefaultVoiceCommandOrchestrator
import com.mindos.speech.NoOpSpeechTelemetrySink
import com.mindos.speech.SpeechTelemetrySink
import com.mindos.speech.TTSBackend
import com.mindos.speech.TranscriptPolicyConfig
import com.mindos.speech.TtsResult
import com.mindos.speech.VoiceCommandOrchestrator
import com.mindos.speech.VoiceRuntimeConfig

/**
 * Production batch-ASR composition boundary for Android.
 *
 * NVIDIA credentials are intentionally absent: this factory can only create an HTTPS client for
 * the authenticated MindOS speech broker. TTS remains disabled until an equivalent brokered
 * provider contract exists.
 */
class AndroidBatchVoiceFactory(
    context: Context,
    tokenProvider: SpeechBrokerTokenProvider,
    brokerConfig: AndroidSpeechBrokerConfig,
    private val runtimeConfig: VoiceRuntimeConfig,
    private val telemetry: SpeechTelemetrySink = NoOpSpeechTelemetrySink,
) : MindVoiceRuntimeFactory {
    private val applicationContext = context.applicationContext
    private val brokerTransport = AndroidSpeechBrokerTransport(tokenProvider, brokerConfig)

    init {
        require(!runtimeConfig.ttsEnabled) {
            "Android batch voice requires ttsEnabled=false until a brokered TTS provider is configured."
        }
    }

    override fun create(textGateway: TextCommandGateway): VoiceCommandOrchestrator =
        DefaultVoiceCommandOrchestrator(
            speechInput = AndroidSpeechInputProvider(
                permissionChecker = AndroidMicrophonePermissionChecker(applicationContext),
                config = AndroidAudioInputConfig(
                    sampleRateHz = runtimeConfig.audioSampleRateHz,
                    maxDurationMs = runtimeConfig.audioMaxDurationMs,
                    timeoutMs = runtimeConfig.captureTimeoutMs,
                ),
            ),
            asr = BrokerAsrBackend(brokerTransport),
            transcriptPolicy = DefaultTranscriptPolicy(TranscriptPolicyConfig.from(runtimeConfig)),
            gateway = textGateway,
            responseTextResolver = DefaultResponseTextResolver(),
            tts = DisabledTtsBackend,
            audioOutput = DisabledAudioOutputProvider,
            config = runtimeConfig,
            telemetry = telemetry,
        )
}

private object DisabledTtsBackend : TTSBackend {
    override suspend fun synthesize(request: com.mindos.speech.TtsRequest): TtsResult =
        TtsResult.Failure(BackendFailureReason.BACKEND_ERROR)
}

private object DisabledAudioOutputProvider : AudioOutputProvider {
    override suspend fun play(audio: com.mindos.speech.SynthesizedAudio): AudioOutputResult =
        AudioOutputResult.Failure(AudioOutputFailureReason.BACKEND_ERROR)
}
