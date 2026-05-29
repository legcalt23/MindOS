package com.example.mindos

import android.app.Application
import com.example.mindos.core.firebase.MindOSFirebase
import com.example.mindos.core.logging.NonFatalReporter
import com.example.mindos.core.logging.ObservabilityHub
import com.example.mindos.core.logging.ObservabilitySink
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MindOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        com.example.mindos.core.logging.isAndroidDebugLoggingEnabled = isDebuggable
        FirebaseAppCheckInstaller.install()
        MindOSFirebase.initialize(applicationContext)
        configureCrashlytics(isDebuggable)
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
