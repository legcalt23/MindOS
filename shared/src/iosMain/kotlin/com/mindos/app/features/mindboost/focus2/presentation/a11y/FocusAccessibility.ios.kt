package com.mindos.app.features.mindboost.focus2.presentation.a11y

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.mindos.app.features.mindboost.focus2.domain.runtime.AccessibilityContext
import platform.UIKit.UIAccessibilityIsSwitchControlRunning
import platform.UIKit.UIAccessibilityIsVoiceOverRunning

/** Apple's Human Interface Guidelines minimum. Deliberately NOT Android's 48 — they are different vendors. */
actual val platformMinimumTarget: Dp = A11yTargets.IOS_MINIMUM

/**
 * iOS answers both questions directly, unlike Android: `UIAccessibilityIsVoiceOverRunning` and
 * `UIAccessibilityIsSwitchControlRunning` are first-class queries, so neither reading is a heuristic here.
 *
 * Read at composition and sealed into the descriptor; a mid-session toggle is not observed, because a session
 * whose accessibility context changed halfway through would have two halves that mean different things.
 *
 * NOT VERIFIED: iOS compilation is deferred (no macOS on the build host).
 */
@Composable
actual fun rememberFocusAccessibility(): AccessibilityContext = AccessibilityContext(
    screenReaderActive = UIAccessibilityIsVoiceOverRunning(),
    switchAccessActive = UIAccessibilityIsSwitchControlRunning(),
)
