package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 6R3 (§7 facts-only contract) · The EVIDENCE for ONE exercise scale (one [MeasurementLineage] group).
 * Facts only: versioned identity, a raw session count, a raw distinct-day count, and the raw first→last span. It
 * does NOT produce a capacity estimate, a confidence, a data-sufficiency, a maturity/progress band, a recency
 * verdict, a trend, a raw per-session score list, or any profile/dimension label — those are profile-lane
 * INTERPRETATIONS of these facts and belong to a separate, not-yet-built profile bridge that would import THIS
 * type, never the reverse (enforced by the boundary scanner test).
 *
 * - [observationCount] = ALL quality-valid sessions of this exercise, every profile-eligible purpose (descriptive;
 *   this is the group population, so it includes provisional purposes like INITIAL_ASSESSMENT).
 * - [distinctDayCount] = day spread of that population (approximate UTC day; NOT trusted for rank).
 * - [spanMs] = the raw first→last span across [observationCount] sessions; `null` when fewer than two exist. A
 *   consumer wanting a recency/staleness verdict must build it downstream — this contract ships no such state.
 * - There is deliberately NO confidence / reliability / error field: with no measurement-error model those stay
 *   UNKNOWN, so no such field is offered at all.
 */
data class ExerciseScaleEvidence(
    val lineage: MeasurementLineage,
    val observationCount: Int,
    val distinctDayCount: Int,
    val spanMs: Long?,
) {
    init {
        require(observationCount >= 0) { "observationCount must be >= 0 (was $observationCount)." }
        require(distinctDayCount >= 0) { "distinctDayCount must be >= 0 (was $distinctDayCount)." }
    }
}
