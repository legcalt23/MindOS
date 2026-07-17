package com.mindos.app.features.mindboost.profile.domain.calibration

/**
 * SYSTEM-1 6R3 R3-FINAL D1 · The purpose a session was run under, from the profile's point of view. Only a sealed
 * measurement/probe or an explicit calibration/initial-assessment run may feed the live cognitive profile; ALL
 * training (free or recommended), practice, debug, and unknown/absent sessions fail closed. Deliberately NOT the
 * same as `CognitiveSessionEnums.SessionPurpose.mayContributeToProfile()`, which returns `true` for TRAINING_FREE
 * (too permissive for the official/profile route — see D1). A `preliminary=true` label is NOT a substitute for this.
 */
enum class SessionMeasurementPurpose {
    MEASUREMENT,
    CALIBRATION,
    INITIAL_ASSESSMENT,
    TRAINING_FREE,
    TRAINING_RECOMMENDED,
    PRACTICE,
    DEBUG,
    UNKNOWN,
}

/**
 * Pure, fail-closed authority for "may this session feed the live cognitive profile?" (SYSTEM-1 6R3 R3-FINAL D1/D2).
 *
 * A contribution is permitted ONLY when BOTH hold: (a) the purpose is a sealed measurement/probe or an explicit
 * CALIBRATION/INITIAL_ASSESSMENT, and (b) the versioned lineage/scale-key is COMPLETE — because without it,
 * cross-session comparability cannot be established (D2), so no session may be pooled or trusted as an estimate.
 * TRAINING_FREE, TRAINING_RECOMMENDED, PRACTICE, DEBUG, UNKNOWN and absent purpose ALL deny. Never default-eligible;
 * paid-plan/subscription is not an input on this signature, so it can never raise scientific eligibility.
 *
 * Today NO Focus/Logic session model (`SessionResult`, `LogicSession`) transports a purpose or a versioned lineage
 * to its ProfileSource, so those sources pass `UNKNOWN` + `lineageComplete = false` → deny → null → CALIBRATING. The
 * eligible path (a session model that carries a sealed purpose + complete lineage) is the registered purpose+lineage
 * measurement-carrier slice (its own ADR-tracked slice with probe sealing, exposure, integrity, versioning, migration
 * and end-to-end tests) — NOT implemented here.
 */
object ProfileMeasurementEligibility {

    fun measurementEligible(purpose: SessionMeasurementPurpose, lineageComplete: Boolean): Boolean =
        lineageComplete && when (purpose) {
            SessionMeasurementPurpose.MEASUREMENT,
            SessionMeasurementPurpose.CALIBRATION,
            SessionMeasurementPurpose.INITIAL_ASSESSMENT,
            -> true

            SessionMeasurementPurpose.TRAINING_FREE,
            SessionMeasurementPurpose.TRAINING_RECOMMENDED,
            SessionMeasurementPurpose.PRACTICE,
            SessionMeasurementPurpose.DEBUG,
            SessionMeasurementPurpose.UNKNOWN,
            -> false
        }
}
