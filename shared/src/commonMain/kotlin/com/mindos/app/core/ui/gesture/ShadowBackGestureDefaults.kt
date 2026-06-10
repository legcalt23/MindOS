package com.mindos.app.core.ui.gesture

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Default configuration tokens for [ShadowBackGestureHandler].
 *
 * All visual constants are centralised here so tuning the effect
 * never requires touching gesture or rendering logic.
 */
object ShadowBackGestureDefaults {

    /** Width of the left-edge hotzone that activates the gesture. */
    val edgeWidth = 28.dp

    /** Drag distance required to confirm the back navigation. */
    val triggerDistance = 108.dp

    /** Base shadow colour — the cloud body. */
    val shadowColor: Color = Color(0xFF000000)

    /**
     * Accent colour for the dense inner nucleus.
     * Deep indigo/purple — "dark energy" aesthetic.
     */
    val accentColor: Color = Color(0xFF120920)

    /**
     * Secondary accent for the glow fringe.
     * Muted purple-violet highlight.
     */
    val glowColor: Color = Color(0xFF3A1060)

    /**
     * Maximum fraction of screen width the cloud expands to at progress = 1.
     */
    const val maxShadowFraction: Float = 0.72f

    /**
     * Number of sample points that define the organic right-edge of the cloud.
     *
     * More points → smoother but more expensive path construction.
     * 28 is a good balance for 60/120 Hz.
     */
    const val EDGE_SAMPLE_COUNT: Int = 28

    /**
     * Precomputed normalised sine wave table used for the right-edge of the
     * cloud silhouette. Values are in [-1, 1].
     *
     * This is computed once at class-loading time and shared across all frames,
     * avoiding any per-frame allocation or Random usage.
     *
     * The wave uses three superimposed frequencies to produce an irregular but
     * deterministic organic shape.
     */
    val organicEdgeTable: FloatArray = FloatArray(EDGE_SAMPLE_COUNT) { i ->
        val t = i / (EDGE_SAMPLE_COUNT - 1).toFloat()
        val angle = t * 2.0 * PI
        val wave = sin(angle * 1.7 + 0.4) * 0.52 +
                   sin(angle * 3.1 + 1.2) * 0.28 +
                   sin(angle * 5.3 + 2.1) * 0.20
        wave.toFloat()
    }

    /**
     * Pre-baked control points for the 20 internal anime-style strokes.
     * Each row is [startXFrac, startYFrac, cp1XFrac, cp1YFrac, cp2XFrac, cp2YFrac, endXFrac, endYFrac]
     * as fractions of (shadowWidth, screenHeight).
     *
     * These are hand-tuned to look like energetic smoke/brushwork, matching the sketch.
     */
    val internalStrokeTable: Array<FloatArray> = arrayOf(
        // Format: sx, sy, cp1x, cp1y, cp2x, cp2y, ex, ey
        floatArrayOf(0.02f, 0.10f, 0.30f, 0.08f, 0.55f, 0.14f, 0.68f, 0.11f),
        floatArrayOf(0.03f, 0.20f, 0.22f, 0.18f, 0.48f, 0.24f, 0.62f, 0.22f),
        floatArrayOf(0.04f, 0.32f, 0.18f, 0.30f, 0.44f, 0.36f, 0.56f, 0.34f),
        floatArrayOf(0.02f, 0.42f, 0.26f, 0.40f, 0.50f, 0.45f, 0.65f, 0.43f),
        floatArrayOf(0.05f, 0.52f, 0.20f, 0.50f, 0.46f, 0.56f, 0.60f, 0.54f),
        floatArrayOf(0.03f, 0.62f, 0.24f, 0.60f, 0.50f, 0.65f, 0.64f, 0.63f),
        floatArrayOf(0.04f, 0.72f, 0.22f, 0.70f, 0.48f, 0.75f, 0.58f, 0.73f),
        floatArrayOf(0.02f, 0.82f, 0.28f, 0.80f, 0.52f, 0.85f, 0.66f, 0.83f),
        floatArrayOf(0.06f, 0.92f, 0.32f, 0.90f, 0.56f, 0.94f, 0.70f, 0.92f),
        floatArrayOf(0.08f, 0.06f, 0.36f, 0.04f, 0.60f, 0.09f, 0.72f, 0.07f),
        floatArrayOf(0.10f, 0.16f, 0.28f, 0.14f, 0.52f, 0.19f, 0.64f, 0.17f),
        floatArrayOf(0.09f, 0.27f, 0.24f, 0.25f, 0.48f, 0.30f, 0.58f, 0.28f),
        floatArrayOf(0.07f, 0.38f, 0.30f, 0.36f, 0.54f, 0.41f, 0.66f, 0.39f),
        floatArrayOf(0.11f, 0.48f, 0.26f, 0.46f, 0.50f, 0.51f, 0.62f, 0.49f),
        floatArrayOf(0.08f, 0.58f, 0.34f, 0.56f, 0.58f, 0.61f, 0.70f, 0.59f),
        floatArrayOf(0.06f, 0.68f, 0.20f, 0.66f, 0.44f, 0.71f, 0.54f, 0.69f),
        floatArrayOf(0.12f, 0.78f, 0.36f, 0.76f, 0.60f, 0.81f, 0.72f, 0.79f),
        floatArrayOf(0.05f, 0.87f, 0.22f, 0.85f, 0.46f, 0.89f, 0.56f, 0.87f),
        floatArrayOf(0.09f, 0.96f, 0.30f, 0.94f, 0.54f, 0.97f, 0.64f, 0.95f),
        floatArrayOf(0.14f, 0.02f, 0.40f, 0.01f, 0.64f, 0.05f, 0.74f, 0.03f),
    )
}
