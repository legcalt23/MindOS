package com.mindos.app.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for turning the user's stored "favorite color" into a
 * Compose [Color].
 *
 * In MindOS the favorite / accent color is persisted as a packed ARGB [Long]
 * (`ProfileCardAppearance.accentArgb`, mirrored to `profile_cards/{uid}` in
 * Firestore and surfaced on `ProfileCardState.accentArgb`). This file is the ONLY
 * place that should convert that stored value into a [Color] or introduce a
 * fallback — do not scatter `Color(argb)` conversions or new fallback literals
 * across features. Loaders and any other accent-tinted UI resolve their color
 * through the helpers below.
 */
object FavoriteColor {

    /**
     * Absolute last-resort accent, used only when the user has no favorite color
     * AND no themed primary is available (e.g. a pure, non-`@Composable` call
     * site). Matches the Uiverse reference blue that the three-bar loader was
     * ported from. Prefer [rememberFavoriteAccentColor], which falls back to the
     * live theme primary first so the app's brand color wins over this constant.
     */
    // The ARGB hex is a design token by definition (this is its canonical home).
    @Suppress("MagicNumber")
    val LoaderBlue: Color = Color(0xFF076FE5)
}

/**
 * Pure, testable mapper: the stored ARGB [accentArgb] as a [Color], or [fallback]
 * when it is `null` (unknown, not chosen yet, or not loaded). Kept free of Compose
 * runtime so it can be unit-tested and reused off the composition.
 */
fun favoriteAccentColor(
    accentArgb: Long?,
    fallback: Color = FavoriteColor.LoaderBlue,
): Color = accentArgb?.let { Color(it) } ?: fallback

/**
 * Composable convenience used at UI call sites: the user's favorite accent as a
 * [Color], falling back to the current [MaterialTheme] primary when no favorite
 * color is set. This keeps loaders tinted with the REAL user color and degrades
 * to the brand/theme color instead of a hardcoded blue.
 *
 * Not prefixed `remember` on purpose — it memoizes nothing; it just reads the
 * favorite value and the theme primary each composition (both are cheap).
 */
@Composable
fun favoriteAccentColorOrThemePrimary(accentArgb: Long?): Color =
    accentArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

/**
 * Ambient current-user favorite accent as a packed ARGB [Long], or `null` when
 * unknown / not yet provided. Provided once near the app root (see `MindOSApp`);
 * accent-tinted UI reads it via `favoriteAccentColor(LocalFavoriteAccent.current,
 * fallback = <local color>)`, so a loader tints with the REAL user color when it
 * is known and keeps its own fallback color otherwise (no regression until wired).
 */
// CompositionLocal must keep the idiomatic `Local` prefix; it is not a constant.
@Suppress("TopLevelPropertyNaming")
val LocalFavoriteAccent: ProvidableCompositionLocal<Long?> = staticCompositionLocalOf { null }
