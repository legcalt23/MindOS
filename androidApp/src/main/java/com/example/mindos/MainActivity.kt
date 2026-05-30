package com.example.mindos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.mindos.core.session.FirebaseSessionRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() so the system splash window is
        // handed over to the library, which then dismisses it once the first
        // Compose frame is ready (postSplashScreenTheme = Theme.MindOS).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        com.example.mindos.features.social.presentation.media.MediaPreloadManager.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            val sessionRepository = remember {
                FirebaseSessionRepository()
            }

            MindOSApp(
                sessionRepository = sessionRepository,
            )
        }
    }
}
