package com.mindos.app.features.personalidentity.model

/**
 * A point in normalized signature space.
 *
 * Coordinates stay device-independent so the same vector can be previewed at different sizes.
 */
data class SignaturePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = MAX_NORMALIZED_VALUE,
) {
    init {
        require(x.isFinite()) { "SignaturePoint.x must be finite" }
        require(y.isFinite()) { "SignaturePoint.y must be finite" }
        require(pressure.isFinite()) { "SignaturePoint.pressure must be finite" }
    }

    fun clamped(): SignaturePoint = copy(
        x = x.coerceIn(MIN_NORMALIZED_VALUE, MAX_NORMALIZED_VALUE),
        y = y.coerceIn(MIN_NORMALIZED_VALUE, MAX_NORMALIZED_VALUE),
        pressure = pressure.coerceIn(MIN_NORMALIZED_VALUE, MAX_NORMALIZED_VALUE),
    )
}

data class SignatureStroke(
    val points: List<SignaturePoint>,
) {
    val isDrawable: Boolean get() = points.size >= MIN_DRAWABLE_POINT_COUNT

    fun clamped(): SignatureStroke = copy(points = points.map { it.clamped() })
}

data class PersonalSignature(
    val id: String,
    val name: String,
    val strokes: List<SignatureStroke>,
) {
    val isUsable: Boolean get() = strokes.any { it.isDrawable }
}

private const val MIN_NORMALIZED_VALUE = 0f
private const val MAX_NORMALIZED_VALUE = 1f
private const val MIN_DRAWABLE_POINT_COUNT = 2
