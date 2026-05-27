package com.example.mindos.features.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class IntroVisibility {
    ALWAYS,
    FIRST_LAUNCH_ONLY,
    NEVER,
}

@Composable
fun MindOSIntroHost(
    visibility: IntroVisibility = IntroVisibility.ALWAYS,
    hasSeenIntro: Boolean = false,
    onIntroComplete: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val shouldShow = when (visibility) {
        IntroVisibility.ALWAYS -> true
        IntroVisibility.FIRST_LAUNCH_ONLY -> !hasSeenIntro
        IntroVisibility.NEVER -> false
    }

    var introActive by remember { mutableStateOf(shouldShow) }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = introActive,
            exit = fadeOut(animationSpec = tween(400)),
        ) {
            MindOSIntroScreen(
                onIntroFinished = {
                    introActive = false
                    onIntroComplete()
                },
            )
        }
    }
}
