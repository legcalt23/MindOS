package com.mindos.app.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Centralized motion easing tokens for MindOS.
 *
 * Pair these with [AppAnimation] durations and the motion-aware
 * [neuAnimationSpec], which collapses animations to an instant snap when the
 * user has requested reduced motion. Curves follow a Material-3-style motion
 * system adapted to NEU's calmer, more cinematic feel.
 *
 * Rule: NO bare CubicBezierEasing(...) outside this file — reference a token.
 */
// The Bezier control points ARE the design tokens here (same rule as AppColors),
// so the float literals are intentional, not stray magic numbers.
@Suppress("MagicNumber")
object AppEasing {

    /** Standard: the default for most on-screen UI transitions. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized: hero / large-surface transitions that need presence. */
    val Emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Decelerate: elements entering the screen (fast → slow). */
    val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** Accelerate: elements leaving the screen (slow → fast). */
    val Accelerate: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Symmetric ease-in-out for pulsing / breathing loops. */
    val Symmetric: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
}
