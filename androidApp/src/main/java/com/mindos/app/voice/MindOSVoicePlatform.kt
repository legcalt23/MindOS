package com.mindos.app.voice

import android.content.Context
import com.mindos.android.voice.AndroidBatchVoiceFactory
import com.mindos.android.voice.AndroidSpeechBrokerConfig
import com.mindos.app.BuildConfig
import com.mindos.app.core.config.FeatureFlags
import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.runtime.MindVoiceRuntimeFactory
import com.mindos.speech.VoiceRuntimeConfig

object MindOSVoicePlatform {
    fun isEnabled(flags: FeatureFlags = FeatureFlagsHub.current): Boolean =
        flags.voiceCommandsEnabled && flags.voiceCloudSpeechEnabled

    fun createBatchFactory(
        context: Context,
        flags: FeatureFlags = FeatureFlagsHub.current,
    ): MindVoiceRuntimeFactory = AndroidBatchVoiceFactory(
        context = context,
        tokenProvider = FirebaseSpeechBrokerTokenProvider(),
        brokerConfig = AndroidSpeechBrokerConfig(BuildConfig.MINDOS_SPEECH_BROKER_URL),
        runtimeConfig = VoiceRuntimeConfig(
            enabled = isEnabled(flags),
            killSwitch = !isEnabled(flags),
            ttsEnabled = false,
        ),
    )
}
