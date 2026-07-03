@file:Suppress("MagicNumber") // Color-space conversion constants (255, 360, 60, hex masks) ARE the math.

package com.mindos.app.features.profile.card.presentation

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure HSV ↔ ARGB helpers for the color picker. Kept Compose-free and total so
 * they can be unit-tested on the JVM. Alpha is always forced opaque (0xFF).
 */
object ColorMath {

    /** HSV (h in [0,360), s/v in [0,1]) → opaque ARGB packed as 0xFFRRGGBB. */
    fun hsvToArgb(hueDegrees: Float, saturation: Float, value: Float): Long {
        val h = ((hueDegrees % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255).toLong()
        return 0xFF000000L or (r shl 16) or (g shl 8) or b
    }

    /** Opaque ARGB → HSV triple (hue, saturation, value). */
    fun argbToHsv(argb: Long): HsvColor {
        val r = ((argb shr 16) and 0xFF).toFloat() / 255f
        val g = ((argb shr 8) and 0xFF).toFloat() / 255f
        val b = (argb and 0xFF).toFloat() / 255f
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        val hue = (h + 360f) % 360f
        val sat = if (mx == 0f) 0f else d / mx
        return HsvColor(hue, sat, mx)
    }

    /** Uppercase #RRGGBB string for an opaque ARGB long. */
    fun toHex(argb: Long): String {
        fun byteHex(n: Long): String = ((n and 0xFF).toString(16)).padStart(2, '0')
        val r = byteHex(argb shr 16)
        val g = byteHex(argb shr 8)
        val b = byteHex(argb)
        return "#${(r + g + b).uppercase()}"
    }
}

/** Hue (degrees), saturation, value — all in their natural ranges. */
data class HsvColor(val hue: Float, val saturation: Float, val value: Float)
