@file:Suppress("MagicNumber") // This file IS the visual table: its floats are the values, not magic constants.

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.ui.graphics.Color
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.state.FeedbackKind
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState

/**
 * How one [FocusFieldState] is drawn by [FocusStimulusField]. Pure, so the ACHROMATIC half of it can be
 * pinned by a test instead of by eye.
 *
 * [bar] and [innerRing] carry NO defaults on purpose. They are the two channels that make two states tellable
 * apart without colour vision, and a default would let the next state added here render as an unmarked ring
 * that collides with an existing one — silently, the way `TOO_EARLY` collided with `FALSE_ALARM`.
 */
internal data class FieldVisual(
    val color: Color,
    val scale: Float,
    val alpha: Float,
    val durationMs: Int,
    val breathing: Boolean,
    /** Filled centre. */
    val solidCore: Boolean,
    /** A horizontal bar across the disc — the "withhold" mark. */
    val bar: Boolean,
    /** A second, smaller stroked circle inside the focal ring. */
    val innerRing: Boolean,
) {
    /** True when this state is actually drawn HERE; the transparent ones render in their own composable. */
    val drawnHere: Boolean get() = alpha > 0f
}

/**
 * Coarse size, because a raw radius ratio is not a discriminator.
 *
 * Two field states are never on screen at the same time, so there is nothing to compare a radius against; and
 * where one DOES follow another within a trial — the GO disc becoming the STOP disc in E4 — the step animates
 * over 120 ms while the user is racing a stop-signal reaction time. A 10 % radius increment under those
 * conditions is not a signal. Only a band change is treated as usable, and the boundaries are set so that the
 * sizes the table actually uses (0.55 / 0.82 / 1.0 / 1.1 / 1.28) collapse exactly where they are close.
 */
internal enum class SizeBand { SMALL, MEDIUM, LARGE }

internal fun sizeBandOf(scale: Float): SizeBand = when {
    scale < 0.7f -> SizeBand.SMALL
    scale < 1.15f -> SizeBand.MEDIUM
    else -> SizeBand.LARGE
}

/**
 * Everything a user with no colour vision can still use.
 *
 * [FieldVisual.color] is excluded because it is the channel under audit, and [FieldVisual.alpha] because for
 * `Signal`/`Lure` it is a function of the trial's `contrast` — a difficulty parameter — so it is not a stable
 * discriminator between two states. The remaining channels are categorical on purpose: a continuous one lets a
 * pairwise-distinctness test pass on a difference nobody can perceive, which is how the `Stop` / `Signal`
 * collision survived its first guard.
 */
internal data class AchromaticSignature(
    val size: SizeBand,
    val breathing: Boolean,
    val solidCore: Boolean,
    val bar: Boolean,
    val innerRing: Boolean,
)

internal val FieldVisual.achromatic: AchromaticSignature
    get() = AchromaticSignature(
        size = sizeBandOf(scale),
        breathing = breathing,
        solidCore = solidCore,
        bar = bar,
        innerRing = innerRing,
    )

/** Placeholder for a state whose real rendering lives in its own composable; drawn as nothing here. */
private fun notDrawnHere() = FieldVisual(
    color = AppColors.FocusReadout,
    scale = 0.55f,
    alpha = 0f,
    durationMs = 200,
    breathing = false,
    solidCore = false,
    bar = false,
    innerRing = false,
)

internal fun FocusFieldState.toVisual(): FieldVisual = when (this) {
    is FocusFieldState.Idle -> FieldVisual(
        color = AppColors.FocusReadout,
        scale = 0.55f,
        alpha = 0.35f,
        durationMs = 420,
        breathing = true,
        solidCore = false,
        bar = false,
        innerRing = false,
    )

    is FocusFieldState.Signal -> FieldVisual(
        color = AppColors.FocusSignal,
        scale = 1.0f,
        alpha = (0.55f + 0.45f * contrast).coerceIn(0.4f, 1f),
        durationMs = 200,
        breathing = false,
        solidCore = true,
        bar = false,
        innerRing = false,
    )

    is FocusFieldState.Lure -> FieldVisual(
        color = AppColors.FocusLure,
        scale = 0.82f,
        alpha = (0.45f + 0.35f * contrast).coerceIn(0.35f, 0.85f),
        durationMs = 200,
        breathing = false,
        solidCore = false,
        bar = false,
        innerRing = false,
    )

    is FocusFieldState.Stop -> FieldVisual(
        // Stop-signal (E4). The GO signal above is left untouched — its detection latency is the baseline the
        // SSRT is measured against — and the STOP is what gains the achromatic mark: before this, GO and STOP
        // were both solid discs 10 % apart in radius, never on screen together, so the only real difference
        // was green vs red. That is the canonical colour-vision confusion pair, and it sat on the
        // discriminandum of the task, which biased the inhibition score of a whole population downwards.
        color = AppColors.FocusError,
        scale = 1.1f,
        alpha = 1f,
        durationMs = 120,
        breathing = false,
        solidCore = true,
        bar = true,
        innerRing = false,
    )

    is FocusFieldState.Search -> notDrawnHere()

    is FocusFieldState.RuleSwitch -> notDrawnHere()

    is FocusFieldState.Anchor -> notDrawnHere()

    is FocusFieldState.Choice -> notDrawnHere()

    is FocusFieldState.Tracking,
    is FocusFieldState.Stream,
    is FocusFieldState.Dual,
    -> notDrawnHere()

    is FocusFieldState.Feedback -> kind.toVisual()
}

/**
 * The post-response flash. Scale, alpha and duration are IDENTICAL across the five outcomes on purpose:
 * feedback salience feeds post-error adjustment, so changing it would move a metric. The marks add a channel
 * without touching how loud the flash is.
 */
private fun FeedbackKind.toVisual() = FieldVisual(
    color = toColor(),
    scale = 1.28f,
    alpha = 0.9f,
    durationMs = 180,
    breathing = false,
    solidCore = this == FeedbackKind.HIT,
    bar = this == FeedbackKind.FALSE_ALARM || this == FeedbackKind.TOO_EARLY,
    innerRing = this == FeedbackKind.MISS || this == FeedbackKind.TOO_EARLY,
)

/**
 * The colour half. `FALSE_ALARM` and `TOO_EARLY` still SHARE this colour — they are both "you answered when
 * you should not have" — and that is fine now only because [FieldVisual.innerRing] tells them apart; before
 * this file they rendered byte-identically for every user, colour vision or not.
 */
private fun FeedbackKind.toColor(): Color = when (this) {
    FeedbackKind.HIT -> AppColors.FocusHit
    FeedbackKind.CORRECT_REJECTION -> AppColors.FocusReadout
    FeedbackKind.MISS -> AppColors.FocusMiss
    FeedbackKind.FALSE_ALARM -> AppColors.FocusError
    FeedbackKind.TOO_EARLY -> AppColors.FocusError
}
