package com.example.mindos.design

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized shape/radius tokens for MindOS.
 *
 * ALL corner radius and shape references should use these tokens.
 * Rule: NO bare RoundedCornerShape(N.dp) outside this file.
 */
object AppShapes {

    // ── Radius scale ────────────────────────────────────────────────────
    val RadiusXS: Dp = 4.dp
    val RadiusS: Dp = 8.dp
    val RadiusM: Dp = 12.dp
    val RadiusL: Dp = 16.dp
    val RadiusXL: Dp = 20.dp
    val RadiusXXL: Dp = 28.dp
    val RadiusFull: Dp = 999.dp

    // ── Pre-built shapes ────────────────────────────────────────────────
    val Small: Shape = RoundedCornerShape(RadiusS)
    val Medium: Shape = RoundedCornerShape(RadiusM)
    val Large: Shape = RoundedCornerShape(RadiusL)
    val ExtraLarge: Shape = RoundedCornerShape(RadiusXL)
    val Pill: Shape = RoundedCornerShape(RadiusFull)
    val Circle: Shape = CircleShape

    /** Card shape (default). */
    val Card: Shape = Medium

    /** Button shape. */
    val Button: Shape = Pill

    /** Input field shape. */
    val Input: Shape = Small

    /** Dialog shape. */
    val Dialog: Shape = Large

    /** Bottom sheet shape (top corners only). */
    val BottomSheet: Shape = RoundedCornerShape(
        topStart = RadiusXL,
        topEnd = RadiusXL,
    )

    /** Chip shape. */
    val Chip: Shape = Pill

    /** Badge shape. */
    val Badge: Shape = Pill
}
