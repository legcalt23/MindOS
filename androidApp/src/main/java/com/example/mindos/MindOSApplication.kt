package com.example.mindos

import android.app.Application
import com.example.mindos.core.firebase.MindOSFirebase

class MindOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        com.example.mindos.core.logging.isAndroidDebugLoggingEnabled = isDebuggable
        FirebaseAppCheckInstaller.install()
        MindOSFirebase.initialize(applicationContext)
    }
}
