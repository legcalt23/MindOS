@file:Suppress("MagicNumber", "FunctionNaming")

package com.mindos.app.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppSpacing
import com.mindos.app.design.AppStrings
import com.mindos.app.design.FavoriteColor
import com.mindos.app.design.MindOSTheme
import com.mindos.app.design.favoriteAccentColor
import com.mindos.app.design.favoriteAccentColorOrThemePrimary

/*
 * Living gallery for the global loading system. MindOS does not wire Compose
 * `@Preview` into commonMain (see AppTextFieldSamples / PhotoUploadLoader), so
 * these are plain composables you can drop into a dev/debug screen or an IDE
 * interactive run. They double as documentation and cover the required cases:
 *
 *  1. [AppBarsLoaderBlueSample]      loader with the default reference blue
 *  2. [AppBarsLoaderRedSample]       loader with a red favorite color
 *  3. [AppBarsLoaderPurpleSample]    loader with a purple favorite color
 *  3b.[AppBarsLoaderThemePrimarySample] loader falling back to the theme primary
 *  4. [AppLoadingOverlayMessageSample] overlay visible, with a message
 *  5. [AppLoadingOverlayHiddenSample]  overlay not visible (content only)
 *  6. [FullScreenAppLoadingSample]   standalone full-screen loading
 *  7. [AppLoadingDemoScreenSample]   content underneath with blur + scrim active
 *
 * MindOS forces dark mode ([MindOSTheme]), so every sample is already the dark
 * appearance — there is no separate light variant to preview.
 */

/** Two example favorite colors, shown as stored ARGB longs → [favoriteAccentColor]. */
private const val SAMPLE_RED_ARGB = 0xFFE53935L
private const val SAMPLE_PURPLE_ARGB = 0xFF8F5CFFL

/** #1 — Loader tinted with the documented default blue fallback. */
@Composable
internal fun AppBarsLoaderBlueSample() {
    SampleStage {
        AppBarsLoader(color = FavoriteColor.LoaderBlue)
    }
}

/** #2 — Loader tinted with a red favorite color. */
@Composable
internal fun AppBarsLoaderRedSample() {
    SampleStage {
        AppBarsLoader(color = favoriteAccentColor(SAMPLE_RED_ARGB))
    }
}

/** #3 — Loader tinted with a purple favorite color. */
@Composable
internal fun AppBarsLoaderPurpleSample() {
    SampleStage {
        AppBarsLoader(color = favoriteAccentColor(SAMPLE_PURPLE_ARGB))
    }
}

/** #3b — Loader falling back to the theme primary when no favorite color is set. */
@Composable
internal fun AppBarsLoaderThemePrimarySample() {
    SampleStage {
        AppBarsLoader(color = favoriteAccentColorOrThemePrimary(accentArgb = null))
    }
}

/** #4 — Overlay visible over demo content, with a message. */
@Composable
internal fun AppLoadingOverlayMessageSample() {
    MindOSTheme {
        AppLoadingOverlay(
            visible = true,
            color = favoriteAccentColor(SAMPLE_PURPLE_ARGB),
            message = AppStrings.Loading,
            modifier = Modifier.size(280.dp),
        ) {
            DemoContent()
        }
    }
}

/** #5 — Overlay hidden: only the underlying content renders. */
@Composable
internal fun AppLoadingOverlayHiddenSample() {
    MindOSTheme {
        AppLoadingOverlay(
            visible = false,
            color = favoriteAccentColor(SAMPLE_RED_ARGB),
            modifier = Modifier.size(280.dp),
        ) {
            DemoContent()
        }
    }
}

/** #6 — Standalone full-screen loading state. */
@Composable
internal fun FullScreenAppLoadingSample() {
    MindOSTheme {
        FullScreenAppLoading(
            color = favoriteAccentColor(SAMPLE_RED_ARGB),
            modifier = Modifier.size(width = 280.dp, height = 480.dp),
        )
    }
}

/** #7 — Demo screen with real content underneath and blur + scrim active. */
@Composable
internal fun AppLoadingDemoScreenSample() {
    MindOSTheme {
        AppLoadingOverlay(
            visible = true,
            color = favoriteAccentColor(SAMPLE_PURPLE_ARGB),
            message = AppStrings.Loading,
            modifier = Modifier.size(width = 280.dp, height = 480.dp),
        ) {
            DemoContent()
        }
    }
}

/** Dark stage used by the raw-loader samples so the bars read on the app canvas. */
@Composable
private fun SampleStage(content: @Composable () -> Unit) {
    MindOSTheme {
        Box(
            modifier = Modifier
                .background(AppColors.Background)
                .padding(AppSpacing.XXXL),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** Fake screen content the overlays sit on top of. */
@Composable
private fun DemoContent() {
    Column(
        modifier = Modifier
            .background(AppColors.Background)
            .fillMaxWidth()
            .padding(AppSpacing.XXL),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.M),
    ) {
        Text(text = AppStrings.ProfileTitle, color = AppColors.OnBackground)
        repeat(4) { row ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (row % 2 == 0) AppColors.Surface else AppColors.SurfaceRaised),
            )
        }
    }
}
