package com.mindos.app.design

import androidx.compose.runtime.Composable
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/**
 * iOS reduce-motion via `UIAccessibility.isReduceMotionEnabled`. Read at
 * composition; a mid-session toggle is not observed (acceptable v1).
 *
 * NOT VERIFIED: iOS compilation is deferred (no macOS on the build host).
 */
@Composable
actual fun rememberSystemReduceMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
