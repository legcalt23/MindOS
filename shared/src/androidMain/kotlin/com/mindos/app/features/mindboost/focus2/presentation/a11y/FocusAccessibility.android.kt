package com.mindos.app.features.mindboost.focus2.presentation.a11y

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mindos.app.features.mindboost.focus2.domain.runtime.AccessibilityContext

/**
 * Reads the active assistive technologies from `AccessibilityManager`.
 *
 * `isTouchExplorationEnabled` is the reliable signal for a screen reader: TalkBack turns it on, and it is the
 * flag that actually changes how a person reaches a target.
 *
 * Switch access is a HEURISTIC and is documented as one. Android exposes no "switch access is running" query,
 * so the best available inference is "an accessibility service is enabled that is not touch exploration". It
 * is wrong in both directions: a false positive (an autofill service, a screen filter) relabels an honest touch
 * session into a modality it cannot be pooled with, and a false negative leaves a switch user with touch-only
 * targets — the structural inequity REDTEAM #34 names.
 *
 * **DECLARED GAP, not a design:** the correct answer is a manual modality choice offered before the seal, so
 * that accessibility never depends on a heuristic guessing right. It does not exist yet. Until it does,
 * detection is the ONLY selector, and that is a limitation of this build rather than an intended behaviour.
 * iOS does not share the problem — `UIAccessibilityIsSwitchControlRunning` is a real query there.
 */
@Composable
actual fun rememberFocusAccessibility(): AccessibilityContext {
    val context = LocalContext.current
    return remember(context) { read(context) }
}

private fun read(context: Context): AccessibilityContext {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return AccessibilityContext.NONE
    val screenReader = manager.isTouchExplorationEnabled
    val otherServiceRunning = manager.isEnabled &&
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).isNotEmpty()
    return AccessibilityContext(
        screenReaderActive = screenReader,
        switchAccessActive = otherServiceRunning && !screenReader,
    )
}
