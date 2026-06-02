package com.example.mindos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.unit.dp
import com.example.mindos.design.AppColors
import com.example.mindos.design.MindOSTheme
import platform.UIKit.UIViewController

/**
 * iOS entry point. Swift calls `MainViewControllerKt.MainViewController()` to
 * obtain a UIViewController hosting the Compose Multiplatform tree.
 *
 * iOS is intentionally blocked from production use until the Firebase session,
 * App Check, Storage, Functions and release pipeline are wired and validated on
 * macOS. Do not mount [App] here with a preview session: that makes iOS look
 * shippable while exercising fake auth.
 */
@Suppress("FunctionName") // iOS convention: factory functions returning UIViewController are PascalCase.
fun MainViewController(): UIViewController = ComposeUIViewController {
    iosProductionBlockedScreen()
}

@Composable
private fun iosProductionBlockedScreen() {
    MindOSTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "MindOS iOS no esta habilitado para produccion.",
                color = AppColors.OnBackground,
            )
        }
    }
}
