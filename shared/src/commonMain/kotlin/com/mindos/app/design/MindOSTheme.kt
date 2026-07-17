@file:Suppress(
    // MindOSTheme is a @Composable, conventionally PascalCase; this repo's Detekt
    // does not exempt @Composable, so the suppression is applied once at file scope.
    "FunctionNaming",
    // The private top-level color/typography tokens are PascalCase by design-system
    // convention (they are token bundles, not runtime constants).
    "TopLevelPropertyNaming",
)

package com.mindos.app.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val NeuDarkColors = darkColorScheme(
    primary = AppColors.NeuGreen,
    onPrimary = AppColors.OnBright,
    secondary = AppColors.NeuViolet,
    onSecondary = AppColors.DeepBlack,
    background = AppColors.Background,
    onBackground = AppColors.OnBackground,
    surface = AppColors.Surface,
    onSurface = AppColors.OnBackground,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.OnSurfaceVariant,
    error = AppColors.Error,
    onError = AppColors.OnBackground,
)

private val NeuLightColors = lightColorScheme(
    primary = AppColors.LightPrimary,
    onPrimary = AppColors.LightOnAccent,
    secondary = AppColors.LightSecondary,
    onSecondary = AppColors.LightOnAccent,
    background = AppColors.LightBackground,
    onBackground = AppColors.LightOnBackground,
    surface = AppColors.LightSurface,
    onSurface = AppColors.LightOnBackground,
    surfaceVariant = AppColors.LightSurfaceRaised,
    onSurfaceVariant = AppColors.LightTextMuted,
    error = AppColors.LightError,
    onError = AppColors.LightOnAccent,
)

private val NeuTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
)

/**
 * Root MindOS theme.
 *
 * Provides the Material 3 [MaterialTheme] plus the NEU theme-aware layer
 * ([LocalNeuColors]) and the reduced-motion signal ([LocalReduceMotion]).
 *
 * [darkTheme] defaults to `true` to preserve today's always-dark app: legacy
 * screens read the static (dark) [AppColors] constants and are not yet
 * light-aware, so flipping the app default to `isSystemInDarkTheme()` is a
 * follow-up gated on migrating those screens onto [NeuTheme]. New components read
 * [NeuTheme.colors] and already render correctly in both schemes — pass
 * `darkTheme = false` in a preview/sample to see the light variant.
 */
@Composable
fun MindOSTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val neuColors = if (darkTheme) neuDarkColorScheme() else neuLightColorScheme()
    val materialScheme = if (darkTheme) NeuDarkColors else NeuLightColors
    CompositionLocalProvider(
        LocalNeuColors provides neuColors,
        LocalReduceMotion provides rememberSystemReduceMotion(),
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = NeuTypography,
            content = content,
        )
    }
}
