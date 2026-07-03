@file:Suppress("MagicNumber") // This is the screen's color/shape token sheet; the hex values ARE the data.

package com.mindos.app.features.profile.card.presentation.ui

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import com.mindos.app.features.profile.card.domain.BackgroundPaint
import com.mindos.app.features.profile.card.domain.GradientKind
import com.mindos.app.features.profile.card.domain.ProfileRank

/** Static design tokens for the profile card (design: Perfil.dc.html). */
object ProfileCardTokens {
    val Canvas = Color(0xFF06070B)
    val ScreenInk = Color(0xFF0A0A0C)
    val White = Color(0xFFFFFFFF)
    val TextMuted = Color(0x80FFFFFF)
    val TextFaint = Color(0x59FFFFFF)
    val TextGhost = Color(0x40FFFFFF)
    val HairlineSoft = Color(0x14FFFFFF)
    val Online = Color(0xFF3FBF8F)
    val Positive = Color(0xFF3FBF8F)
    val Negative = Color(0xFFE0556B)
    val Info = Color(0xFF4F7CFF)
    val SurfaceLow = Color(0xFF0C0E16)
    val GoldDeep = Color(0xFF9B7B34)

    fun accent(argb: Long): Color = Color(argb)
}

/** Hexagon used for the general-rank emblem and the rank chips. */
val RankHexagon: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, 0f)
    lineTo(w, h * 0.26f)
    lineTo(w, h * 0.74f)
    lineTo(w * 0.5f, h)
    lineTo(0f, h * 0.74f)
    lineTo(0f, h * 0.26f)
    close()
}

/** Up-pointing chevron used inside the general-rank emblem. */
val ChevronUp: Shape = GenericShape { size, _ ->
    moveTo(0f, size.height)
    lineTo(size.width * 0.5f, 0f)
    lineTo(size.width, size.height)
    close()
}

/** Tier colors for rank emblems. Bronze → Legend, warmer/cooler as the tier climbs. */
fun rankColors(rank: ProfileRank): Pair<Color, Color> = when (rank) {
    ProfileRank.BRONCE -> Color(0xFFCD8B5A) to Color(0xFF7A4A26)
    ProfileRank.PLATA -> Color(0xFFD7DEE8) to Color(0xFF8B97A8)
    ProfileRank.ORO -> Color(0xFFFFD98A) to Color(0xFFB5742A)
    ProfileRank.DIAMANTE -> Color(0xFF9FE8FF) to Color(0xFF3A8FD0)
    ProfileRank.ZAFIRO -> Color(0xFF7FA8FF) to Color(0xFF2C4FB0)
    ProfileRank.RUBI -> Color(0xFFFF8FA8) to Color(0xFFB02C44)
    ProfileRank.MAESTRO -> Color(0xFFC6A3FF) to Color(0xFF6536B5)
    ProfileRank.GRAN_MAESTRO -> Color(0xFFFFB0C6) to Color(0xFF8B2E66)
    ProfileRank.PRODIGIO -> Color(0xFF8AF0D6) to Color(0xFF1E8A5B)
    ProfileRank.LEYENDA -> Color(0xFFFFE6A3) to Color(0xFFB88A2E)
}

/**
 * Builds a Compose [Brush] from a domain [BackgroundPaint]. Uses only linear /
 * vertical gradients and [SolidColor] — NEVER an infinite-radius radial, which
 * fails to paint (Compose radial centers are pixels, not fractions) and lets the
 * host background bleed through.
 */
fun BackgroundPaint.toBrush(): Brush = when (this) {
    is BackgroundPaint.Solid -> SolidColor(Color(argb))
    is BackgroundPaint.Gradient -> {
        val colors = stops.map { Color(it) }
        when (kind) {
            GradientKind.LINEAR_DIAGONAL -> Brush.linearGradient(colors)
            GradientKind.RADIAL_TOP -> Brush.verticalGradient(colors)
        }
    }
    // Image paints are rendered by the caller (needs platform image loading); fall back to ink.
    is BackgroundPaint.Image -> Brush.verticalGradient(
        listOf(ProfileCardTokens.SurfaceLow, ProfileCardTokens.ScreenInk),
    )
}
