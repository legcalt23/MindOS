package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 6R3 (§7) · The Scientific Measurement Charter §7 canonical evidence state for ONE session's disposition
 * inside a block's extraction. An INCLUDED session ceilings at [PROVISIONAL] — the Exercise Quality Standard §3.2
 * per-execution `evidenceEligibility` ceiling for the same disposition is `PROVISIONAL_PROFILE_ELIGIBLE`. This
 * contract has no blueprint/comparability/calibration bridge, so [OFFICIAL_ELIGIBLE] stays structurally
 * unreachable from here — [SessionEvidenceStatePolicy] never constructs it.
 */
enum class SessionEvidenceState { TRAINING_ONLY, PROVISIONAL, OFFICIAL_ELIGIBLE, INVALIDATED }

/**
 * SYSTEM-1 6R3 (Exercise Quality Standard §3.2) · WHICH ROUTE one session's disposition may feed, kept as its OWN
 * enum — never folded into [SessionEvidenceState] — because "how much accumulated evidence a version has"
 * ([SessionEvidenceState]/`validationStatus`) and "what one execution under these conditions may feed"
 * (`evidenceEligibility`) are orthogonal per the standard's own text: a version can be mature while ONE session
 * still stays offline/repeated/technically-compromised and therefore non-official. [PROFILE_ELIGIBLE] and
 * [RANK_ELIGIBLE] never auto-derive from one another (§3.2: "RANK_ELIGIBLE requiere un gate adicional") and both
 * stay structurally unreachable from this evidence-only contract — [SessionEvidenceStatePolicy] never constructs
 * them; only a future, separate calibration/ranking bridge could.
 */
enum class EvidenceRouteEligibility { TRAINING_ONLY, PROVISIONAL_PROFILE_ELIGIBLE, PROFILE_ELIGIBLE, RANK_ELIGIBLE }

/**
 * SYSTEM-1 6R3 · The FULL canonical disposition of ONE session inside a block's extraction: its
 * [SessionEvidenceState], its [EvidenceRouteEligibility], and the [reasons] that produced them. [reasons] is a
 * List (never a single value) to mirror the run model's own rejection-reason shape
 * ([com.mindos.app.features.mindboost.core.session.cognitive.EligibilityTrace.rejectionReasons]); today's
 * extractor only ever surfaces the single FIRST-in-cascade [ExclusionReason] (so [reasons] holds 0 or 1 entries:
 * empty when the session qualified/included, one otherwise) — a future extractor able to detect several
 * SIMULTANEOUS reasons could grow this list without changing the shape.
 */
data class SessionEvidenceDisposition(
    val state: SessionEvidenceState,
    val routeEligibility: EvidenceRouteEligibility,
    val reasons: List<ExclusionReason>,
)

/**
 * SYSTEM-1 6R3 (§7 canonical-vocabulary alignment + Exercise Quality Standard §3.2) · Maps ONE session's
 * [ExclusionReason] disposition — `null` meaning it qualified and is INCLUDED in an [ExerciseScaleEvidence] group
 * — to its canonical [SessionEvidenceState] and [EvidenceRouteEligibility]. Both mappings are pure and TOTAL over
 * every [ExclusionReason] (no subscription/payment-plan input exists on either signature — a paid plan can never
 * raise evidence eligibility, because the policy has nothing plan-shaped to read).
 * [ExclusionReason.NOT_FOR_BLOCK] never actually reaches this function (a run outside this block never receives a
 * per-survivor disposition at all) but is mapped conservatively so the `when` stays exhaustive without an `else`.
 */
object SessionEvidenceStatePolicy {

    /** Bump when the disposition→state/eligibility mapping changes. */
    const val VERSION: String = "session-evidence-state-v2"

    fun stateOf(exclusionReason: ExclusionReason?): SessionEvidenceState = when (exclusionReason) {
        null -> SessionEvidenceState.PROVISIONAL
        ExclusionReason.PURPOSE_EXCLUDED,
        ExclusionReason.TIMEBASE_NON_AUTHORITATIVE,
        ExclusionReason.RT_CRITICALITY_UNKNOWN,
        ExclusionReason.NOT_VALID,
        -> SessionEvidenceState.TRAINING_ONLY
        ExclusionReason.PRELIMINARY,
        ExclusionReason.LOW_CONFIDENCE,
        -> SessionEvidenceState.PROVISIONAL
        ExclusionReason.NOT_COMPLETED,
        ExclusionReason.DATA_QUALITY,
        ExclusionReason.TIMING_COMPROMISED,
        ExclusionReason.IDENTITY_CONFLICT,
        ExclusionReason.QUARANTINED_INCONSISTENT_TIME,
        ExclusionReason.FUTURE_TIMESTAMP,
        ExclusionReason.NOT_FOR_BLOCK,
        -> SessionEvidenceState.INVALIDATED
    }

    /**
     * The per-execution route ceiling (Exercise Quality Standard §3.2). A qualifying/included session, and one
     * excluded only for PRELIMINARY/LOW_CONFIDENCE, both ceiling at the provisional-profile eligibility — still
     * evidence, just softer confidence. Every other exclusion floors at [EvidenceRouteEligibility.TRAINING_ONLY]:
     * an INVALIDATED session (integrity/identity/temporal defect) is not merely lower-confidence evidence, it is
     * NOT evidence at all for any profile/rank route.
     */
    fun eligibilityOf(exclusionReason: ExclusionReason?): EvidenceRouteEligibility = when (exclusionReason) {
        null,
        ExclusionReason.PRELIMINARY,
        ExclusionReason.LOW_CONFIDENCE,
        -> EvidenceRouteEligibility.PROVISIONAL_PROFILE_ELIGIBLE
        ExclusionReason.PURPOSE_EXCLUDED,
        ExclusionReason.TIMEBASE_NON_AUTHORITATIVE,
        ExclusionReason.RT_CRITICALITY_UNKNOWN,
        ExclusionReason.NOT_VALID,
        ExclusionReason.NOT_COMPLETED,
        ExclusionReason.DATA_QUALITY,
        ExclusionReason.TIMING_COMPROMISED,
        ExclusionReason.IDENTITY_CONFLICT,
        ExclusionReason.QUARANTINED_INCONSISTENT_TIME,
        ExclusionReason.FUTURE_TIMESTAMP,
        ExclusionReason.NOT_FOR_BLOCK,
        -> EvidenceRouteEligibility.TRAINING_ONLY
    }

    /** The full [SessionEvidenceDisposition] for one session's [exclusionReason] (`null` = qualified/included). */
    fun dispositionOf(exclusionReason: ExclusionReason?): SessionEvidenceDisposition = SessionEvidenceDisposition(
        state = stateOf(exclusionReason),
        routeEligibility = eligibilityOf(exclusionReason),
        reasons = listOfNotNull(exclusionReason),
    )
}
