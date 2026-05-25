package com.example.neuv3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.neuv3.core.session.FirebaseSessionRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.neuv3.features.social.presentation.media.MediaPreloadManager.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            val sessionRepository = remember {
                FirebaseSessionRepository()
            }

            NEUApp(
                sessionRepository = sessionRepository,
            )
        }
    }
}
