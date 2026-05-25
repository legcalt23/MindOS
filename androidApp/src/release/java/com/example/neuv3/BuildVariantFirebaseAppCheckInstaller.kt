package com.example.neuv3

import com.example.neuv3.core.logging.safeDebugLog
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
