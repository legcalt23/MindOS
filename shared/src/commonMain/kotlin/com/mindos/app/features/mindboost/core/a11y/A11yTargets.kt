package com.mindos.app.features.mindboost.core.a11y

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The minimum size of anything a user has to hit, per platform.
 *
 * **Minimum target sizes are NATIVE values, not one aspirational number.** Android's guidance is 48dp and
 * Apple's HIG is 44pt. They are different numbers from different vendors, and picking the smaller one "because
 * it's the stricter-looking constant" ships an Android build that fails its own accessibility scanner, while
 * picking a single 44 "universal" number is the anti-pattern the Focus V5 plan names as P1-16. So the table
 * below records both and the platform picks its own.
 *
 * This lives in `core` because it is a platform constant, not a feature's concept. It was written for the
 * Focus V5 tracer lane and sat there while the lane the hub actually mounts — Focus V1 — had no source for the
 * number at all, so its controls used a literal 36.dp. A constant with one owner and two lanes that need it
 * belongs where both can read it; duplicating it into the second lane would have made two owners of one fact.
 */
object A11yTargets {

    /** Android: Material accessibility guidance, enforced by the Accessibility Scanner. */
    val ANDROID_MINIMUM: Dp = 48.dp

    /** iOS: Human Interface Guidelines. A point and a dp are both 1/160 inch at baseline density. */
    val IOS_MINIMUM: Dp = 44.dp

    /** Desktop has no touch guidance of its own; it follows the larger of the two, which is never wrong. */
    val DESKTOP_MINIMUM: Dp = ANDROID_MINIMUM
}

/** This platform's minimum interactive target. Declared per platform; never a shared compromise value. */
expect val platformMinimumTarget: Dp
