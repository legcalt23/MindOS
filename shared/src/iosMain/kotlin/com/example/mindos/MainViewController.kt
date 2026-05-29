package com.example.mindos

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point. Swift calls `MainViewControllerKt.MainViewController()` to
 * obtain a UIViewController hosting the Compose Multiplatform tree.
 *
 * Wired identically to the desktop `App()` preview: anonymous session via
 * [com.example.mindos.core.session.PreviewSessionRepository]. The real iOS
 * Firebase wiring lives in a follow-up wave once the Xcode project compiles
 * against the shared framework on macOS and Firebase pods are linked.
 */
@Suppress("FunctionName") // iOS convention: factory functions returning UIViewController are PascalCase.
fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
