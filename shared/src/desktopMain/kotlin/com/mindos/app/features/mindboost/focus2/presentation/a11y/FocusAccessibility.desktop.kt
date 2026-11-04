package com.mindos.app.features.mindboost.focus2.presentation.a11y

import androidx.compose.runtime.Composable
import com.mindos.app.features.mindboost.focus2.domain.runtime.AccessibilityContext

/**
 * Desktop is the preview/test host and exposes no assistive-technology query, so it reports NONE.
 *
 * NONE is the conservative answer, not a convenient one: it claims no assistive technology is active, so a
 * desktop session is never excluded from evidence on the strength of a guess. The participant can still select
 * the sequential modality by hand — which is the point of that being a choice rather than a consequence of
 * detection.
 */
@Composable
actual fun rememberFocusAccessibility(): AccessibilityContext = AccessibilityContext.NONE
