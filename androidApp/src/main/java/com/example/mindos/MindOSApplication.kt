package com.example.mindos

import android.app.Application
import com.example.mindos.core.config.DefaultFeatureFlags
import com.example.mindos.core.config.FeatureFlags
import com.example.mindos.core.config.FeatureFlagsHub
import com.example.mindos.core.firebase.MindOSFirebase
import com.example.mindos.core.logging.NonFatalReporter
import com.example.mindos.core.logging.ObservabilityHub
import com.example.mindos.core.logging.ObservabilitySink
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class MindOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        com.example.mindos.core.logging.isAndroidDebugLoggingEnabled = isDebuggable
        FirebaseAppCheckInstaller.install()
        MindOSFirebase.initialize(applicationContext)
        configureCrashlytics(isDebuggable)
        configureRemoteConfig()
    }

    /**
     * Wires Firebase Remote Config as the [FeatureFlags] provider (kill switches).
     * Defaults are seeded synchronously-enough via setDefaultsAsync + a STATIC-source
     * fallback in [RemoteConfigFeatureFlags], so reads are always safe even before the
     * first fetch completes. fetchAndActivate refreshes the cache for subsequent reads.
     */
    private fun configureRemoteConfig() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setDefaultsAsync(
            mapOf(
                DefaultFeatureFlags.KEY_PRIVACY_OVERLAY to DefaultFeatureFlags.privacyOverlayEnabled,
                DefaultFeatureFlags.KEY_WAVES to DefaultFeatureFlags.wavesEnabled,
                DefaultFeatureFlags.KEY_WALLET to DefaultFeatureFlags.walletEnabled,
            ),
        )
        FeatureFlagsHub.current = RemoteConfigFeatureFlags(remoteConfig)
        remoteConfig.fetchAndActivate()
    }

    private fun configureCrashlytics(isDebuggable: Boolean) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        // Disable automatic collection in debug; enable in release.
        crashlytics.isCrashlyticsCollectionEnabled = !isDebuggable
        crashlytics.setCustomKey("app_variant", if (isDebuggable) "debug" else "release")

        // Bridge shared-module non-fatal errors to Crashlytics (legacy handle, kept
        // for backwards compatibility with existing callers of safeErrorLog).
        if (!isDebuggable) {
            NonFatalReporter.handler = { error -> crashlytics.recordException(error) }
        }

        // Structured observability sink — breadcrumbs + custom keys + non-fatals.
        // Always installed (even in debug) so we can verify the wiring is correct
        // locally; only release ships the events because isCrashlyticsCollectionEnabled
        // gates the actual upload.
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

    override fun recordNonFatal(error: Throwable) {
        crashlytics.recordException(error)
    }
}

/**
 * [FeatureFlags] backed by Firebase Remote Config. Reads are live (each property
 * getter queries the activated cache), so a fetched override takes effect on the
 * next read. When a key has no default/remote value yet (source == STATIC), it
 * falls back to the compile-time [DefaultFeatureFlags] so the kill switches are
 * never accidentally "off" during the async startup window.
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

    private fun boolOrDefault(key: String, default: Boolean): Boolean {
        val value = remoteConfig.getValue(key)
        return if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) default else value.asBoolean()
    }
}
