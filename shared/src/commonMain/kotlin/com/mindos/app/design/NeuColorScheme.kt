package com.mindos.app.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme-aware semantic color scheme for NEU's premium component layer.
 *
 * Why this exists: [AppColors] is a set of fixed constants (all dark), read
 * directly by ~800 legacy call-sites, so it cannot become light/dark on its own.
 * This scheme is an ADDITIVE layer: new components read [NeuTheme.colors] and so
 * adapt to light and dark, while legacy screens keep reading the static
 * [AppColors] constants unchanged. Migrating legacy screens onto this layer is a
 * separate, incremental effort.
 *
 * The dark factory reuses the existing [AppColors] values, so today's dark UI is
 * pixel-identical.
 */
@Immutable
@Suppress("LongParameterList") // Token bundle; mirrors Material 3's ColorScheme.
data class NeuColorScheme(
    val isLight: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onBackground: Color,
    val onSurfaceMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val outline: Color,
    val outlineStrong: Color,
    val error: Color,
    val onError: Color,
    val warning: Color,
    val success: Color,
    val info: Color,
    val disabledContent: Color,
    val disabledContainer: Color,
    val scrim: Color,
)

/**
 * Dark scheme — reuses existing [AppColors] values so there is ZERO visual change
 * to today's (dark) UI.
 */
fun neuDarkColorScheme(): NeuColorScheme = NeuColorScheme(
    isLight = false,
    background = AppColors.Background,
    surface = AppColors.Surface,
    surfaceRaised = AppColors.SurfaceRaised,
    onBackground = AppColors.OnBackground,
    onSurfaceMuted = AppColors.TextMuted,
    primary = AppColors.NeuGreen,
    onPrimary = AppColors.OnBright,
    secondary = AppColors.NeuViolet,
    onSecondary = AppColors.OnBackground,
    outline = AppColors.Stroke,
    outlineStrong = AppColors.InputBorder,
    error = AppColors.Error,
    onError = AppColors.OnBackground,
    warning = AppColors.Warning,
    success = AppColors.Success,
    info = AppColors.Info,
    disabledContent = AppColors.Disabled,
    disabledContainer = AppColors.DisabledBackground,
    scrim = AppColors.DeepBlack,
)

/**
 * Light scheme — FIRST-PASS values (see the `Light*` tokens in [AppColors]).
 * Verify AA contrast with tooling before enabling light mode app-wide.
 */
fun neuLightColorScheme(): NeuColorScheme = NeuColorScheme(
    isLight = true,
    background = AppColors.LightBackground,
    surface = AppColors.LightSurface,
    surfaceRaised = AppColors.LightSurfaceRaised,
    onBackground = AppColors.LightOnBackground,
    onSurfaceMuted = AppColors.LightTextMuted,
    primary = AppColors.LightPrimary,
    onPrimary = AppColors.LightOnAccent,
    secondary = AppColors.LightSecondary,
    onSecondary = AppColors.LightOnAccent,
    outline = AppColors.LightStroke,
    outlineStrong = AppColors.LightStrokeStrong,
    error = AppColors.LightError,
    onError = AppColors.LightOnAccent,
    warning = AppColors.Warning,
    success = AppColors.LightPrimary,
    info = AppColors.Info,
    disabledContent = AppColors.LightDisabledContent,
    disabledContainer = AppColors.LightDisabledBackground,
    scrim = AppColors.LightOnBackground,
)

/** Ambient NEU color scheme. Provided by [MindOSTheme]; defaults to dark. */
@Suppress("TopLevelPropertyNaming") // CompositionLocal; the `Local` prefix is a Compose contract.
val LocalNeuColors = staticCompositionLocalOf { neuDarkColorScheme() }

/**
 * Entry point for theme-aware NEU tokens, mirroring the `MaterialTheme` accessor
 * idiom (`NeuTheme.colors.primary`, `NeuTheme.reduceMotion`).
 */
object NeuTheme {

    val colors: NeuColorScheme
        @Composable @ReadOnlyComposable get() = LocalNeuColors.current

    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}
