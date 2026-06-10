package com.mindos.app.features.mindboost.focus.domain.model

/**
 * Paradigm-specific per-trial markers, bundled so [TrialResult] stays lean as more
 * exercises arrive. Every marker is null/absent unless the trial's paradigm defines
 * it; the metrics engine reads only the marker its metric needs.
 *
 * - [congruent]: flanker / Stroop congruency (E2/E5).
 * - [isSwitch]: task-switching — true on a rule-switch trial (E6).
 * - [setSize]: visual-search display size (E7).
 * - [dualTask]: true on dual-condition trials, false on single (E8).
 * - [underPressure]: true on deadline trials, false on calm (E13).
 * - [duringIntrusion]: true while a distractor intruder is present (E11).
 */
data class TrialMarkers(
    val congruent: Boolean? = null,
    val isSwitch: Boolean? = null,
    val setSize: Int? = null,
    val dualTask: Boolean? = null,
    val underPressure: Boolean? = null,
    val duringIntrusion: Boolean? = null,
) {
    companion object {
        val NONE = TrialMarkers()
    }
}
