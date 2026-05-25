package com.example.neuv3

import android.app.Application
import com.example.neuv3.core.firebase.NEUFirebase

class NEUApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        com.example.neuv3.core.logging.isAndroidDebugLoggingEnabled = isDebuggable
        FirebaseAppCheckInstaller.install()
        NEUFirebase.initialize(applicationContext)
    }
}
