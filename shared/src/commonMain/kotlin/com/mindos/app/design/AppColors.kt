package com.mindos.app.design

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
// This IS the centralized home for color literals (see the file rule above), so
// the ARGB hex values are tokens by definition, not stray magic numbers.
@Suppress("MagicNumber")
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

    /** Composer teal accent. Used for photo mode, action chips, cursor gradients. */
    val ComposerTeal = Color(0xFF28F0C6)

    /** Composer blue accent. Used for video mode, cursor gradients. */
    val ComposerBlue = Color(0xFF2E9DFF)

    /** Composer audio/voice accent. */
    val ComposerAudioCyan = Color(0xFF35C9FF)

    /** Composer text mode accent (warm gold). */
    val ComposerTextGold = Color(0xFFF3C56B)

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

    /** Unread indicator / notification badge. */
    val UnreadBadge = Color(0xFF2196F3)

    /** Online presence indicator. */
    val Online = Color(0xFF20F26B)

    // ── Photo upload loader (Uiverse-inspired) ──────────────────────────
    // Small self-contained badge shown whenever the app is uploading a photo.
    // Keeps the reference loader's own white "card" chrome so it reads on any
    // surface; the surrounding scrim/overlay adapts to the dark theme instead.
    /** Loader card background (the reference's white square). */
    val PhotoUploadLoaderBackground = Color(0xFFFFFFFF)

    /** Loader strong accent (rombo/shadow + swinging dot). */
    val PhotoUploadLoaderPrimary = Color(0xFFFF3D00)

    /** Loader soft accent (front rombo/montaña). */
    val PhotoUploadLoaderSecondary = Color(0xFFFF9371)

    // ── MindOS profile (Perfil.html design) ─────────────────────────────
    /** Profile screen canvas — slightly warmer than the app background. */
    val ProfileCanvas = Color(0xFF0A0A0B)

    /** Neutral near-white text used across the premium profile. */
    val ProfileTextPrimary = Color(0xFFF5F5F4)

    /** Online presence dot on the profile (green). Paired with [Error] for offline. */
    val ProfileOnline = Color(0xFF34D27A)

    // ── MindBoost · Enfoque (focus training) ────────────────────────────
    /** Focus training canvas — deepest, calmest background for the exercise field. */
    val FocusCanvas = Color(0xFF05080A)

    /** First ambient aurora bloom (teal-green). */
    val FocusAuroraA = Color(0xFF0E5E4E)

    /** Second ambient aurora bloom (deep violet). */
    val FocusAuroraB = Color(0xFF1E2A66)

    /** The target signal the user must catch (vivid green). */
    val FocusSignal = Color(0xFF21F0A8)

    /** A lure / non-target the user must ignore (cool violet). */
    val FocusLure = Color(0xFF7C6CFF)

    /** Positive feedback (hit / correct rejection). */
    val FocusHit = Color(0xFF2BF5A6)

    /** Negative feedback (miss / timeout). */
    val FocusMiss = Color(0xFFFFC24D)

    /** Error feedback (false alarm / too early). */
    val FocusError = Color(0xFFFF5E7A)

    /** Muted readout text on the focus surfaces. */
    val FocusReadout = Color(0xFF8FB9AE)

    // ── Text input (AppTextField · Uiverse floating-label design) ───────
    /**
     * Default (unfocused) input border. Dark-theme adaptation of the CSS
     * reference `rgb(200,200,200)` so a neutral rounded outline reads on the
     * near-black app background instead of glaring light-grey.
     */
    val InputBorder = Color(0xFF3A3F45)

    /** Focused / filled input border — lavender, from CSS `rgb(150,150,200)`. */
    val InputBorderFocused = Color(0xFF9696C8)

    /**
     * Floating-label text — muted grey lifted from CSS `rgb(100,100,100)` so it
     * keeps adequate contrast on dark surfaces.
     */
    val InputLabel = Color(0xFF9AA0A6)

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

    // ── Light theme palette (theme-aware layer; see NeuColorScheme) ─────
    // First-pass light values for the new theme-aware component layer. Legacy
    // screens read the dark constants above directly and are unaffected. Verify
    // AA contrast with tooling before enabling light mode app-wide.
    /** Light: app background (warm near-white). */
    val LightBackground = Color(0xFFF6F8F7)

    /** Light: base surface (cards, sheets). */
    val LightSurface = Color(0xFFFFFFFF)

    /** Light: raised surface for subtle layering. */
    val LightSurfaceRaised = Color(0xFFEDF2F0)

    /** Light: primary / high-emphasis foreground (near-black, green cast). */
    val LightOnBackground = Color(0xFF0B1512)

    /** Light: secondary / muted foreground. */
    val LightTextMuted = Color(0xFF56655F)

    /** Light: subtle border / stroke. */
    val LightStroke = Color(0xFFDCE3E0)

    /** Light: stronger border (inputs, focused outlines). */
    val LightStrokeStrong = Color(0xFFBCC6C2)

    /** Light: primary brand accent, darkened from [NeuGreen] for AA on white. */
    val LightPrimary = Color(0xFF0B7A57)

    /** Light: secondary brand accent, darkened from [NeuViolet] for AA on white. */
    val LightSecondary = Color(0xFF6A45E0)

    /** Light: error / destructive accent for light surfaces. */
    val LightError = Color(0xFFD4183D)

    /** Light: on-color for filled accents (primary / secondary / error) — white. */
    val LightOnAccent = Color(0xFFFFFFFF)

    /** Light: disabled foreground. */
    val LightDisabledContent = Color(0xFF9BA6A2)

    /** Light: disabled background / container. */
    val LightDisabledBackground = Color(0xFFE6EAE8)

    // ── Gradients (common brush building blocks) ────────────────────────
    /** Standard vertical dark gradient for list backgrounds. */
    val DarkVerticalGradient = listOf(Background, DeepBlack)

    /** Surface-level gradient for cards and containers. */
    val SurfaceGradient = listOf(Surface, Background)
}
