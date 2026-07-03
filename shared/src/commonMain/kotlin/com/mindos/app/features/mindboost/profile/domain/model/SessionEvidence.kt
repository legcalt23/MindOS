package com.mindos.app.features.mindboost.profile.domain.model

/**
 * Coarse, persist-safe cross-session evidence a session-based module exposes to the profile: only the
 * count of VALID sessions and the number of DISTINCT days they span. It carries NO session ids, no
 * timestamps and nothing uploadable (same minimization contract as [ModuleContribution]); the source
 * reduces its raw session list to these two integers via `CalibrationPolicy.summarize`.
 *
 * This is the input the `CalibrationPolicy` (DC-3) needs to cap a dimension's maturity. Trial-accumulation
 * modules (e.g. Memoria) have no session concept and therefore expose `null` — DC-3's cross-session clause
 * does not apply to them (their sufficiency is trial-based via `MinSampleGate`).
 */
data class SessionEvidence(
    val validSessions: Int,
    val distinctDays: Int,
) {
    init {
        require(validSessions >= 0) { "validSessions cannot be negative (was $validSessions)." }
        require(distinctDays in 0..validSessions) {
            "distinctDays must be in 0..validSessions (days=$distinctDays, sessions=$validSessions)."
        }
    }
}
