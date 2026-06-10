package com.mindos.app

import com.mindos.app.core.logging.safeDebugLog
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

internal object BuildVariantFirebaseAppCheckInstaller {
    private const val Tag = "FirebaseAppCheck"

    fun install() {
        FirebaseAppCheck.getInstance().apply {
            installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            setTokenAutoRefreshEnabled(true)
        }
        safeDebugLog(Tag, "Installed Firebase App Check Play Integrity provider.")
    }
}
