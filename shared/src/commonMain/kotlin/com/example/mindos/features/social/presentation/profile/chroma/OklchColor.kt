package com.example.mindos.features.social.presentation.profile.chroma

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * OKLCH Color Space model (Oklab polar form).
 * L: Lightness (0f..1f)
 * C: Chroma (0f..~0.323f for sRGB, but can be higher depending on gamut)
 * H: Hue (0f..360f)
 */
data class Oklch(
    val l: Float,
    val c: Float,
    val h: Float
) {
    fun toColor(): Color {
        val hRad = h * (PI / 180.0)
        val a = c * cos(hRad).toFloat()
        val b = c * sin(hRad).toFloat()
        // Compose Color maps v0=red, v1=green, v2=blue
        // For Oklab standard, v0=L, v1=a, v2=b
        return Color(l, a, b, 1f, ColorSpaces.Oklab)
    }

    companion object {
        fun fromColor(color: Color): Oklch {
            val oklab = color.convert(ColorSpaces.Oklab)
            val l = oklab.red // L is packed in red
            val a = oklab.green // a is packed in green
            val b = oklab.blue // b is packed in blue
            
            val c = sqrt(a * a + b * b)
            var h = atan2(b, a) * (180.0 / PI).toFloat()
            if (h < 0f) h += 360f
            
            return Oklch(l, c, h)
        }
    }
}
