package com.mindos.app.design

import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the current UI should honor a reduced-motion preference. Provided by
 * [MindOSTheme] from the platform accessibility setting; defaults to `false`
 * (full motion) so previews/tests without the theme still animate.
 *
 * Read it through [NeuTheme.reduceMotion]; drive animations through
 * [neuAnimationSpec] so honoring the preference is automatic.
 */
@Suppress("TopLevelPropertyNaming") // CompositionLocal; the `Local` prefix is a Compose contract.
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * The platform's "reduce motion" accessibility signal:
 *  - Android: the "Remove animations" setting (forces animator scale to 0).
 *  - iOS: `UIAccessibility.isReduceMotionEnabled`.
 *  - Desktop: no standard signal → always `false`.
 *
 * Read once per composition; a mid-session toggle is not observed (acceptable v1).
 */
@Composable
expect fun rememberSystemReduceMotion(): Boolean

/**
 * Motion-aware animation spec: a normal [tween] that collapses to an instant
 * [snap] (no animation) when [LocalReduceMotion] is active. Use it wherever a
 * component animates state (color, float, offset) so reduced motion is honored
 * without per-call branching.
 */
@Composable
fun <T> neuAnimationSpec(
    durationMillis: Int = AppAnimation.DurationMedium,
    delayMillis: Int = 0,
    easing: Easing = AppEasing.Standard,
): DurationBasedAnimationSpec<T> =
    if (LocalReduceMotion.current) {
        snap()
    } else {
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)
    }
