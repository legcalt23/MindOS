package com.mindos.app

import android.app.Application
import android.content.pm.ApplicationInfo
import com.mindos.app.core.config.DefaultFeatureFlags
import com.mindos.app.core.config.FeatureFlags
import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.core.firebase.MindOSFirebase
import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.core.logging.ObservabilitySink
import com.mindos.app.core.logging.NonFatalTelemetryEvent
import com.mindos.app.core.logging.SanitizedNonFatalException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class MindOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        com.mindos.app.core.logging.isAndroidDebugLoggingEnabled = isDebuggable
        FirebaseAppCheckInstaller.install()
        MindOSFirebase.initialize(applicationContext)
        configureCrashlytics(isDebuggable)
        configureRemoteConfig()
    }

    /**
     * Wires Firebase Remote Config as the [FeatureFlags] provider. Defaults are
     * available before the first fetch completes, so kill switches are safe to
     * read during startup.
     */
    private fun configureRemoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setDefaultsAsync(
            mapOf(
                DefaultFeatureFlags.KEY_PRIVACY_OVERLAY to DefaultFeatureFlags.privacyOverlayEnabled,
                DefaultFeatureFlags.KEY_WAVES to DefaultFeatureFlags.wavesEnabled,
                DefaultFeatureFlags.KEY_WALLET to DefaultFeatureFlags.walletEnabled,
                DefaultFeatureFlags.KEY_VOICE_COMMANDS to DefaultFeatureFlags.voiceCommandsEnabled,
                DefaultFeatureFlags.KEY_VOICE_CLOUD_SPEECH to DefaultFeatureFlags.voiceCloudSpeechEnabled,
                DefaultFeatureFlags.KEY_MINDBOOST_SYNC_FIRESTORE to
                    DefaultFeatureFlags.mindboostSyncFirestoreEnabled,
                DefaultFeatureFlags.KEY_MINDBOOST_GLOBAL_SCORE to
                    DefaultFeatureFlags.mindboostGlobalScoreEnabled,
            ),
        )
        FeatureFlagsHub.current = RemoteConfigFeatureFlags(remoteConfig)
        remoteConfig.fetchAndActivate()
    }

    private fun configureCrashlytics(isDebuggable: Boolean) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = !isDebuggable
        crashlytics.setCustomKey("app_variant", if (isDebuggable) "debug" else "release")
        ObservabilityHub.sink = CrashlyticsObservabilitySink(crashlytics)
    }
}

/**
 * Forwards [ObservabilitySink] calls to Crashlytics. Lives in `:androidApp` so
 * `:shared` does not need a direct dependency on the Crashlytics SDK.
 */
private class CrashlyticsObservabilitySink(
    private val crashlytics: FirebaseCrashlytics,
) : ObservabilitySink {
    override fun breadcrumb(message: String) {
        crashlytics.log(message)
    }

    override fun customKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun customKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    override fun customKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    override fun recordNonFatal(event: NonFatalTelemetryEvent) {
        crashlytics.recordException(SanitizedNonFatalException(event))
    }
}

/**
 * [FeatureFlags] backed by Firebase Remote Config. Reads are live from the
 * activated cache. If a value is still static, fall back to the compiled default.
 */
private class RemoteConfigFeatureFlags(
    private val remoteConfig: FirebaseRemoteConfig,
) : FeatureFlags {
    override val privacyOverlayEnabled: Boolean
        get() = boolOrDefault(DefaultFeatureFlags.KEY_PRIVACY_OVERLAY, DefaultFeatureFlags.privacyOverlayEnabled)

    override val wavesEnabled: Boolean
        get() = boolOrDefault(DefaultFeatureFlags.KEY_WAVES, DefaultFeatureFlags.wavesEnabled)

    override val walletEnabled: Boolean
        get() = boolOrDefault(DefaultFeatureFlags.KEY_WALLET, DefaultFeatureFlags.walletEnabled)

    override val voiceCommandsEnabled: Boolean
        get() = boolOrDefault(DefaultFeatureFlags.KEY_VOICE_COMMANDS, DefaultFeatureFlags.voiceCommandsEnabled)

    override val voiceCloudSpeechEnabled: Boolean
        get() = boolOrDefault(DefaultFeatureFlags.KEY_VOICE_CLOUD_SPEECH, DefaultFeatureFlags.voiceCloudSpeechEnabled)

    override val arenaRatingEngineEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_ARENA_RATING_ENGINE,
            DefaultFeatureFlags.arenaRatingEngineEnabled,
        )

    override val arenaShadowRankedEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_ARENA_SHADOW_RANKED,
            DefaultFeatureFlags.arenaShadowRankedEnabled,
        )

    override val arenaMentalSpeedRankedEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_ARENA_MENTAL_SPEED_RANKED,
            DefaultFeatureFlags.arenaMentalSpeedRankedEnabled,
        )

    override val arenaMatchmakingEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_ARENA_MATCHMAKING,
            DefaultFeatureFlags.arenaMatchmakingEnabled,
        )

    override val arenaTrialsEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_ARENA_TRIALS,
            DefaultFeatureFlags.arenaTrialsEnabled,
        )

    override val mindboostSyncFirestoreEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_MINDBOOST_SYNC_FIRESTORE,
            DefaultFeatureFlags.mindboostSyncFirestoreEnabled,
        )

    override val focusScoreSurfacesEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_FOCUS_SCORE_SURFACES,
            DefaultFeatureFlags.focusScoreSurfacesEnabled,
        )

    override val mindboostGlobalScoreEnabled: Boolean
        get() = boolOrDefault(
            DefaultFeatureFlags.KEY_MINDBOOST_GLOBAL_SCORE,
            DefaultFeatureFlags.mindboostGlobalScoreEnabled,
        )

    private fun boolOrDefault(key: String, default: Boolean): Boolean {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) default else value.asBoolean()
    }
}
