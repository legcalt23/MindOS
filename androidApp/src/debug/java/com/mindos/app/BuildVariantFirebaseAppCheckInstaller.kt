package com.mindos.app

import com.mindos.app.core.logging.safeDebugLog
import com.mindos.app.core.logging.safeErrorLog
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

internal object BuildVariantFirebaseAppCheckInstaller {
    private const val Tag = "FirebaseAppCheck"

    fun install() {
        FirebaseAppCheck.getInstance().apply {
            installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            setTokenAutoRefreshEnabled(true)
        }
        safeDebugLog(Tag, "Installed Firebase App Check debug provider.")
        FirebaseAppCheck.getInstance()
            .getAppCheckToken(false)
            .addOnSuccessListener {
                safeDebugLog(Tag, "Firebase App Check debug token request completed for this development build.")
            }
            .addOnFailureListener { error ->
                safeErrorLog(
                    Tag,
                    "Firebase App Check debug token could not be acquired yet. " +
                        "Verifica que el dispositivo de desarrollo este registrado en App Check.",
                    error,
                )
            }
    }
}
