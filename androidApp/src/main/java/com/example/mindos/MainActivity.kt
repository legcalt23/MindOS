package com.example.mindos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.mindos.core.session.FirebaseSessionRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
