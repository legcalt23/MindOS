package com.mindos.app.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized spacing tokens for MindOS.
 *
 * ALL padding, margin, gap, and size values should reference these tokens.
 * Rule: NO bare dp literals in UI code unless genuinely one-off and documented.
 *
 * Scale follows a 4dp base grid with key stops at 2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 56, 64.
 */
object AppSpacing {

    // ── Base scale ──────────────────────────────────────────────────────
    val XXS: Dp = 2.dp
    val XS: Dp = 4.dp
    val S: Dp = 8.dp
    val M: Dp = 12.dp
    val L: Dp = 16.dp
    val XL: Dp = 20.dp
    val XXL: Dp = 24.dp
    val XXXL: Dp = 32.dp
    val XXXXL: Dp = 40.dp

    // ── Screen-level ────────────────────────────────────────────────────
    /** Horizontal padding for screen content. */
    val ScreenHorizontal: Dp = 20.dp

    /** Vertical spacing between major screen sections. */
    val SectionGap: Dp = 24.dp

    /** Content padding inside cards. */
    val CardPadding: Dp = 16.dp

    // ── Component-level ─────────────────────────────────────────────────
    /** Minimum touch target size (accessibility). */
    val MinTouchTarget: Dp = 48.dp

    /** Standard icon size. */
    val IconSize: Dp = 24.dp

    /** Small icon size. */
    val IconSizeSmall: Dp = 20.dp

    /** Large icon size (toolbars, emphasis). */
    val IconSizeLarge: Dp = 32.dp

    /** Avatar size (default). */
    val AvatarSize: Dp = 40.dp

    /** Avatar size (small, list items). */
    val AvatarSizeSmall: Dp = 32.dp

    /** Avatar size (large, profile). */
    val AvatarSizeLarge: Dp = 80.dp

    /** Button height. */
    val ButtonHeight: Dp = 48.dp

    /** Input field height. */
    val InputHeight: Dp = 56.dp

    /** Divider thickness. */
    val DividerThickness: Dp = 1.dp

    /** Top bar height. */
    val TopBarHeight: Dp = 56.dp
}
