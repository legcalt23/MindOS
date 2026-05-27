package com.example.mindos.design

import androidx.compose.ui.graphics.Color

/**
 * Centralized color palette for MindOS.
 *
 * ALL color references across the app should come from either:
 * 1. MaterialTheme.colorScheme (via MindOSTheme) for standard Material3 slots
 * 2. AppColors for brand-specific / semantic colors not covered by Material3
 *
 * Rule: NO Color(0xFF...) literals outside this file and MindOSTheme.kt.
 *
 * Naming convention: grouped by semantic purpose, not by visual appearance.
 */
object AppColors {

    // ── Brand palette ───────────────────────────────────────────────────
    /** Signature neon green. Used for primary actions, accents, success states. */
    val NeuGreen = Color(0xFF13ECA4)

    /** Softer green variant. Used for edit buttons, secondary green accents. */
    val NeuGreenSoft = Color(0xFF17E6AD)

    /** Deep purple/violet. Used for secondary actions, badges, highlights. */
    val NeuViolet = Color(0xFF8F5CFF)

    /** Warm gold. Used for premium indicators, awards, special elements. */
    val NeuGold = Color(0xFFE7E0CF)

    /** Target/highlight gold. Used for search results, radar highlights. */
    val TargetGold = Color(0xFFE9FF9B)

    /** Neon cyan. Used for edit mode glow, focus rings, special accents. */
    val NeuCyan = Color(0xFF00E5FF)

    /** Softer cyan for focused input borders, cursor. */
    val CyanFocus = Color(0xFF35DBFF)

    /** Ice blue for labels on dark dialogs. */
    val IceBlue = Color(0xFF9BC8E6)

    // ── Neutral palette ─────────────────────────────────────────────────
    /** True black for deepest backgrounds. */
    val DeepBlack = Color(0xFF020504)

    /** App background. */
    val Background = Color(0xFF050505)

    /** Elevated surface (cards, sheets). */
    val Surface = Color(0xFF0B0B0B)

    /** Subtle raised surface for cards and phrases. */
    val SurfaceRaised = Color(0xFF171717)

    /** Dark surface for control backgrounds (segments, panels). */
    val SurfaceDark = Color(0xFF101214)

    /** Surface variant for subtle differentiation. */
    val SurfaceVariant = Color(0xFF101815)

    /** Dark button/chip surface. */
    val SurfaceButton = Color(0xFF151515)

    /** Dialog / overlay background. */
    val DialogBackground = Color(0xFF0A0D13)

    /** Dialog input field background. */
    val DialogField = Color(0xFF111725)

    /** Primary dialog action chip background. */
    val DialogActionPrimary = Color(0xFF111A24)

    /** Secondary dialog action chip background. */
    val DialogActionSecondary = Color(0xFF0E121B)

    /** Primary text / high-emphasis foreground. */
    val OnBackground = Color(0xFFF4FFF9)

    /** High-emphasis light text. */
    val TextPrimary = Color(0xFFEAEAEA)

    /** Slightly softer high-emphasis text. */
    val TextPrimarySoft = Color(0xFFE6E6E6)

    /** Secondary text / medium-emphasis foreground. */
    val Mist = Color(0xFFA7CFC1)

    /** Muted text for secondary labels. */
    val TextMuted = Color(0xFFB8B8B8)

    /** Dark foreground on bright surfaces (buttons, selected segments). */
    val OnBright = Color(0xFF06130F)

    /** Accent text on surface variant. */
    val OnSurfaceVariant = Color(0xFF9BFFE0)

    /** Light text for selected segments, near-white with green cast. */
    val TextSelected = Color(0xFFE1FFF8)

    // ── Border / Stroke colors ──────────────────────────────────────────
    /** Subtle border for cards, containers. */
    val Stroke = Color(0xFF252525)

    /** Border for control segments (slightly blue-tinted). */
    val StrokeSegment = Color(0xFF262A2E)

    /** Border for dialogs (blue-tinted). */
    val StrokeDialog = Color(0xFF26344A)

    /** Attachment menu background. */
    val AttachmentMenuBackground = Color(0xFF1A1D1C)

    // ── Semantic colors ─────────────────────────────────────────────────
    /** Error / destructive actions. */
    val Error = Color(0xFFFF4D6A)

    /** Softer error for inline profile messages. */
    val ErrorSoft = Color(0xFFFF7D7D)

    /** Recording / danger indicator. */
    val RecordingRed = Color(0xFFFF5A5A)

    /** Soft error for audio capture failure. */
    val ErrorWarmSoft = Color(0xFFFFB4A9)

    /** Warning / attention required. */
    val Warning = Color(0xFFFFB74D)

    /** Success / confirmation. */
    val Success = NeuGreen

    /** Info / neutral informational. */
    val Info = Color(0xFF64B5F6)

    // ── State colors ────────────────────────────────────────────────────
    /** Disabled foreground. */
    val Disabled = Color(0xFF4A4A4A)

    /** Disabled background / surface. */
    val DisabledBackground = Color(0xFF1A1A1A)

    /** Pressed / active overlay alpha. */
    const val PressedAlpha = 0.12f

    /** Focused overlay alpha. */
    const val FocusedAlpha = 0.08f

    /** Overlay background alpha for scrims, sheets. */
    const val ScrimAlpha = 0.6f

    // ── Gradients (common brush building blocks) ────────────────────────
    /** Standard vertical dark gradient for list backgrounds. */
    val DarkVerticalGradient = listOf(Background, DeepBlack)

    /** Surface-level gradient for cards and containers. */
    val SurfaceGradient = listOf(Surface, Background)
}
