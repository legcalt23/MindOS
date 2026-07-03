package com.mindos.app.features.personalidentity.model

import kotlin.math.sqrt

/**
 * Removes very dense consecutive points while preserving the visible shape and final point.
 */
fun SignatureStroke.simplified(
    minDistance: Float = DEFAULT_MIN_POINT_DISTANCE,
): SignatureStroke {
    if (points.size <= MIN_SIMPLIFICATION_POINT_COUNT) return clamped()

    val safeMinDistance = minDistance.coerceAtLeast(0f)
    val result = mutableListOf<SignaturePoint>()
    var last: SignaturePoint? = null

    for (point in points.map { it.clamped() }) {
        val previous = last
        if (previous == null || previous.distanceTo(point) >= safeMinDistance) {
            result += point
            last = point
        }
    }

    val finalPoint = points.last().clamped()
    if (result.lastOrNull() != finalPoint) result += finalPoint

    return SignatureStroke(result)
}

fun PersonalSignature.normalizedAndSimplified(): PersonalSignature = copy(
    strokes = strokes
        .map { it.simplified() }
        .filter { it.isDrawable },
)

private fun SignaturePoint.distanceTo(other: SignaturePoint): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

private const val DEFAULT_MIN_POINT_DISTANCE = 0.004f
private const val MIN_SIMPLIFICATION_POINT_COUNT = 2
