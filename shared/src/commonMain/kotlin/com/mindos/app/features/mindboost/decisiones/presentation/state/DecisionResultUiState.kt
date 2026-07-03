package com.mindos.app.features.mindboost.decisiones.presentation.state

/** One axis of the cognitive polygon. [value] is a 0..100 performance reading. */
data class PolygonAxis(
    val label: String,
    val value: Double,
)

/**
 * Result of one assembled Decisiones session.
 *
 * Only honestly-computable single-session axes are surfaced. PHD is intentionally absent: its
 * calibration term needs prediction-vs-outcome data this pipeline does not collect, so showing it
 * would fabricate a number.
 */
data class DecisionResultUiState(
    val axes: List<PolygonAxis>,
    val complexity: Double,
    val coachMessage: String?,
)
