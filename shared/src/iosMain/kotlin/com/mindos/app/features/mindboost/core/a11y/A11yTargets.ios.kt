package com.mindos.app.features.mindboost.core.a11y

import androidx.compose.ui.unit.Dp

/**
 * Apple's Human Interface Guidelines minimum. Deliberately NOT Android's 48 — they are different vendors.
 *
 * NOT VERIFIED: iOS compilation is deferred (no macOS on the build host). This declaration is a move, not new
 * code — it carried the same value in `focus2.presentation.a11y` before.
 */
actual val platformMinimumTarget: Dp = A11yTargets.IOS_MINIMUM
