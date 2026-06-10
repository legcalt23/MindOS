package com.mindos.app.core.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.app
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize

object MindOSFirebase {
    private var defaultApp: FirebaseApp? = null

    val app: FirebaseApp
        get() = defaultApp ?: Firebase.app.also { defaultApp = it }

    fun initialize(context: Any? = null): FirebaseApp {
        defaultApp?.let { return it }

        Firebase.apps(context).firstOrNull()?.let { initializedApp ->
            defaultApp = initializedApp
            return initializedApp
        }

        return (Firebase.initialize(context) ?: Firebase.app).also { initializedApp ->
            defaultApp = initializedApp
        }
    }
}
