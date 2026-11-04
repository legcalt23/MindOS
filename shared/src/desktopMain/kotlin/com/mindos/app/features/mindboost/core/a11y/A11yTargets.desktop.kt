package com.mindos.app.features.mindboost.core.a11y

import androidx.compose.ui.unit.Dp

/** Desktop has no touch guidance of its own, so it takes the larger of the two mobile minimums. */
actual val platformMinimumTarget: Dp = A11yTargets.DESKTOP_MINIMUM
