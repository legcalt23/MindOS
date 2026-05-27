package com.example.mindos.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized elevation/shadow tokens for MindOS.
 *
 * On a dark theme, elevation is less visually prominent, but we maintain
 * a consistent scale for layering/z-order semantics.
 */
object AppElevation {

    /** No elevation (flat). */
    val None: Dp = 0.dp

    /** Subtle elevation (cards, list items). */
    val Low: Dp = 2.dp

    /** Medium elevation (floating action, dropdowns). */
    val Medium: Dp = 4.dp

    /** High elevation (dialogs, bottom sheets). */
    val High: Dp = 8.dp

    /** Highest elevation (modals, overlays). */
    val Highest: Dp = 16.dp
}
