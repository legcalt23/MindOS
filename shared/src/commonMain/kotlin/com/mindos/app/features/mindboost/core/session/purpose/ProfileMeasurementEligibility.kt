package com.mindos.app.features.mindboost.core.session.purpose

/**
 * Focus V5 · S1 · ADR-FOCUS-PURPOSE (decided 2026-07-20, Decision Log §20.2) — the SINGLE authority on the purpose
 * a session ran under, from the measurement point of view.
 *
 * Until S1 the repo had TWO authorities that disagreed: this enum (TRAINING_FREE denied) and
 * `CognitiveSessionEnums.SessionPurpose.mayContributeToProfile()` (TRAINING_FREE allowed). One of them was wrong on
 * every call. The ADR collapsed them onto this one and RETIRED the rival derivation.
 *
 * SCOPE OF THAT CLAIM, stated narrowly on purpose. This is the sole authority on the PURPOSE axis within the
 * cognitive-session lane. It is not the only gate a session passes, and two things deliberately fall outside it:
 *  - The sibling `*ProfileSource` adapters gate on OTHER axes entirely (bank integrity, timebase authority,
 *    detector coverage). They are separate gates on separate questions, not rivals to this one.
 *  - `SessionEligibility.profileEligible` is PERSISTED. A stored row is a PROJECTION of a past evaluation, never
 *    a verdict to re-read as if it were current: a row sealed under an older rule keeps the older answer forever.
 *    Re-derive from the purpose; do not trust the Boolean. The evidence layer already reaches the same
 *    conclusion independently, which is why it re-derives every eligibility fact instead of believing the
 *    stored flags.
 *
 * It lives in `core` — not in `profile` — precisely so every lane can consult it without inverting the dependency
 * direction (`core` must not depend on a feature). A caller that cannot reach this type must NOT re-derive the
 * answer locally, and must NOT be handed a pre-computed Boolean either: an eligibility flag decided elsewhere is
 * forgeable by drift, which is the exact failure this ADR closes.
 *
 * [TUTORIAL] and [FAMILIARIZATION] are the members S1 added so a warm-up run can be recorded as what it is instead
 * of being flattened into [PRACTICE]. That distinction is not cosmetic: washout analysis needs to tell a first-
 * exposure run apart from ordinary practice, and a lossy label cannot be un-lost afterwards. [RESEARCH_ONLY] marks
 * data gathered under a study protocol, which never feeds a live profile no matter how clean it is.
 *
 * [UNKNOWN] is reserved for "not sealed, absent, or changed mid-session" and always resolves to
 * [ProfileEligibilityDisposition.Indeterminate] — it is never a way to say "not applicable".
 */
enum class SessionMeasurementPurpose {
    MEASUREMENT,
    CALIBRATION,
    INITIAL_ASSESSMENT,
    TRAINING_FREE,
    TRAINING_RECOMMENDED,
    PRACTICE,
    TUTORIAL,
    FAMILIARIZATION,
    RESEARCH_ONLY,
    DEBUG,
    UNKNOWN,
    ;

    /**
     * True when this purpose may count toward calibration's valid sessions (still gated by duration/quality).
     *
     * An ALLOWLIST, deliberately: a member added later defaults to INELIGIBLE. The predicate this replaced was a
     * denylist (`!= PRACTICE && != DEBUG`), which meant every future member silently defaulted to eligible — the
     * fail-OPEN shape ADR-FOCUS-PURPOSE retired.
     */
    fun mayContributeToCalibration(): Boolean =
        this == TRAINING_RECOMMENDED || this == TRAINING_FREE || this == CALIBRATION

    /** True when this purpose may count toward the OFFICIAL ranking. INITIAL_ASSESSMENT is provisional-only. */
    fun mayContributeToRanking(): Boolean =
        this == TRAINING_RECOMMENDED || this == TRAINING_FREE || this == CALIBRATION

    companion object {
        /**
         * Deserialization boundary, fail-closed: a value this build does not recognise — a row written by a NEWER
         * build, or a corrupted name — becomes [UNKNOWN], which denies. It never throws and never guesses a
         * neighbouring member. `null` (absent field) is likewise [UNKNOWN], because "absent" and "unreadable" are
         * both "not sealed", and neither is evidence of anything.
         */
        fun fromWire(raw: String?): SessionMeasurementPurpose =
            entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

/** Why a purpose could not be resolved to a definite yes/no. Each value is a FACT about the record, never a guess. */
enum class IndeterminateReason {
    /** No purpose was sealed before the first trial, or the persisted value was absent/unrecognised. */
    PURPOSE_NOT_SEALED,

    /**
     * The purpose allows measurement, but the versioned lineage is incomplete, so comparability cannot be
     * established between sessions.
     */
    LINEAGE_INCOMPLETE,

    /** The purpose changed after the session had begun; the run is kept as practice but is not evidence. */
    PURPOSE_CHANGED_MID_SESSION,
}

/**
 * The three-way answer required by ADR-FOCUS-PURPOSE. A Boolean cannot express this: "this purpose is not allowed"
 * and "we cannot tell what this purpose was" are different facts with different consequences, and collapsing them
 * hides the second one — which is the one that signals a broken sealing path rather than an ordinary training run.
 *
 * All three are non-eligible for the profile. Only [Eligible] permits a contribution.
 */
sealed interface ProfileEligibilityDisposition {

    /** The purpose is a sealed measurement/probe or an explicit calibration run, AND the lineage is complete. */
    data object Eligible : ProfileEligibilityDisposition

    /** A definite NO: this purpose is known and is structurally excluded from the profile. */
    data class NotEligibleByPurpose(val purpose: SessionMeasurementPurpose) : ProfileEligibilityDisposition

    /** A definite "cannot tell": deny, and surface the reason rather than pretending it was a plain training run. */
    data class Indeterminate(val reason: IndeterminateReason) : ProfileEligibilityDisposition
}

/**
 * Pure, fail-closed authority for "may this session feed the live cognitive profile?" (SYSTEM-1 6R3 R3-FINAL D1/D2,
 * re-sealed by ADR-FOCUS-PURPOSE).
 *
 * A contribution is permitted ONLY when BOTH hold: (a) the purpose is a sealed measurement/probe or an explicit
 * CALIBRATION/INITIAL_ASSESSMENT, and (b) the versioned lineage/scale-key is COMPLETE — because without it,
 * cross-session comparability cannot be established (D2), so no session may be pooled or trusted as an estimate.
 * Every other purpose denies. Never default-eligible; paid-plan/subscription is not an input on this signature, so
 * it can never raise scientific eligibility.
 */
object ProfileMeasurementEligibility {

    /**
     * The full three-way verdict. Prefer this over [measurementEligible] wherever the reason can be surfaced: a
     * denial that says WHY is the difference between diagnosing a broken sealing path and shrugging at a `false`.
     */
    fun disposition(
        purpose: SessionMeasurementPurpose,
        lineageComplete: Boolean,
    ): ProfileEligibilityDisposition = when (purpose) {
        SessionMeasurementPurpose.UNKNOWN ->
            ProfileEligibilityDisposition.Indeterminate(IndeterminateReason.PURPOSE_NOT_SEALED)

        SessionMeasurementPurpose.MEASUREMENT,
        SessionMeasurementPurpose.CALIBRATION,
        SessionMeasurementPurpose.INITIAL_ASSESSMENT,
        ->
            if (lineageComplete) {
                ProfileEligibilityDisposition.Eligible
            } else {
                ProfileEligibilityDisposition.Indeterminate(IndeterminateReason.LINEAGE_INCOMPLETE)
            }

        SessionMeasurementPurpose.TRAINING_FREE,
        SessionMeasurementPurpose.TRAINING_RECOMMENDED,
        SessionMeasurementPurpose.PRACTICE,
        SessionMeasurementPurpose.TUTORIAL,
        SessionMeasurementPurpose.FAMILIARIZATION,
        SessionMeasurementPurpose.RESEARCH_ONLY,
        SessionMeasurementPurpose.DEBUG,
        -> ProfileEligibilityDisposition.NotEligibleByPurpose(purpose)
    }

    /** Boolean projection of [disposition] for call sites that only branch on yes/no. Eligible, or nothing. */
    fun measurementEligible(
        purpose: SessionMeasurementPurpose,
        lineageComplete: Boolean,
    ): Boolean = disposition(purpose, lineageComplete) is ProfileEligibilityDisposition.Eligible
}
