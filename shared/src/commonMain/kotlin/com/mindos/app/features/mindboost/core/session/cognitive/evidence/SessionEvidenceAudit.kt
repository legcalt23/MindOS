package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 slice 6-R2/6-R3 · Why a received run did NOT qualify for an evidence group. Stable machine names, no
 * PII. Run-level reasons ([NOT_FOR_BLOCK], [IDENTITY_CONFLICT]) count RUNS; the rest count UNIQUE dedup survivors.
 * [IDENTITY_CONFLICT] = two runs shared a sessionId with differing payloads (fail-closed: BOTH dropped).
 * [QUARANTINED_INCONSISTENT_TIME] = the run's own timestamps are impossible (completed after last update) — an
 * intrinsic, clock-independent defect that is NEVER re-admitted as time passes (unlike a merely-future stamp).
 * [RT_CRITICALITY_UNKNOWN] = no [RtCriticalityRegistry] entry exists for the run's (exerciseId, exerciseVersion) —
 * the run's OWN timing sensitivity is unproven, so it is excluded UNCONDITIONALLY (regardless of clock quality);
 * it still trains, it just cannot be evidence (6R3 §18 C4). [TIMEBASE_NON_AUTHORITATIVE] = the run's exercise IS
 * declared timing-critical but the run was not on a frame-locked clock (re-derived from `clockSource`, never a
 * trusted flag) — a NARROWER, more specific gap than [RT_CRITICALITY_UNKNOWN]. [PURPOSE_EXCLUDED] = the run's own
 * [com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose] is never evidence-bearing from this
 * contract today (PRACTICE/DEBUG), or is a full TRAINING_FREE session without the sealed probe-identity fact this
 * contract would need to treat it otherwise (6R3 §7).
 */
enum class ExclusionReason {
    NOT_FOR_BLOCK,
    IDENTITY_CONFLICT,
    QUARANTINED_INCONSISTENT_TIME,
    FUTURE_TIMESTAMP,
    NOT_COMPLETED,
    DATA_QUALITY,
    TIMING_COMPROMISED,
    RT_CRITICALITY_UNKNOWN,
    TIMEBASE_NON_AUTHORITATIVE,
    NOT_VALID,
    PRELIMINARY,
    LOW_CONFIDENCE,
    PURPOSE_EXCLUDED,
}

/** A count of runs excluded for one [ExclusionReason]. A List — never a keyed/dictionary collection — for
 *  stable serialization/equality. */
data class ExclusionCount(val reason: ExclusionReason, val count: Int) {
    init {
        require(count >= 0) { "count must be >= 0 (was $count)." }
    }
}

/**
 * SYSTEM-1 6R3 (§18 C2.1) · A count of sessions carrying one canonical [SessionEvidenceState] — a re-bucketing of
 * the SAME facts already conserved by [SessionEvidenceAudit] (its [ExclusionCount] tally plus the included count),
 * grouped through [SessionEvidenceStatePolicy] instead of by raw [ExclusionReason]. A List, never a keyed
 * collection, for the same stable-serialization reason as [ExclusionCount].
 */
data class StateCount(val state: SessionEvidenceState, val count: Int) {
    init {
        require(count >= 0) { "count must be >= 0 (was $count)." }
    }
}

/**
 * SYSTEM-1 6R3 (§18 C2.1) · A count of sessions carrying one canonical [EvidenceRouteEligibility] — the same
 * re-bucketing as [StateCount], through [SessionEvidenceStatePolicy]'s eligibility mapping instead of its state
 * mapping. A List, never a keyed collection, for the same stable-serialization reason as [ExclusionCount].
 */
data class RouteEligibilityCount(val eligibility: EvidenceRouteEligibility, val count: Int) {
    init {
        require(count >= 0) { "count must be >= 0 (was $count)." }
    }
}

/**
 * SYSTEM-1 6R3 · Every versioned policy that shaped one extraction, bundled so a consumer can detect a rule
 * change (P1-12) instead of trusting one ambiguous string. THREE fields — [evidenceQuantityPolicyVersion],
 * [rankPolicyVersion], and [freshnessPolicyVersion] — pin the version of a per-session evaluative rule this
 * contract retired (the first and third under 6R3 §7; the second — the retired block-readiness-for-ranking rule
 * — under 6R3 §18 C2.3): all three are frozen historical markers, not a live computation, kept only so a
 * previously-extracted [PolicyVersionBundle] stays honestly attributable (§21 lineage/migrations) instead of being
 * silently reinterpreted.
 */
data class PolicyVersionBundle(
    val evidenceRuleVersion: String,
    val lineagePolicyVersion: String,
    val evidenceQuantityPolicyVersion: String,
    val rankPolicyVersion: String,
    val freshnessPolicyVersion: String,
)

/**
 * SYSTEM-1 slice 6-R2/6-R3 · A MINIMIZED-PERSONAL (NOT anonymous) audit of ONE extraction. It carries NO session
 * ids and NO raw scores, but it DOES carry coarse counts and — because a downstream projection must still
 * minimize — it is classified cognitive/personal data: the [exclusions] breakdown includes measurement-integrity
 * / anti-gaming reasons that must NOT be surfaced to the user (mechanism leak) and must NOT be logged/exported at
 * this granularity without a safe projection (P2-01, W-precondition). The [init] block enforces the conservation
 * invariants so no run is silently lost or double-counted (P1-12); [stateCounts]/[routeEligibilityCounts] (§18
 * C2.1) are FACTS re-bucketed from the SAME already-conserved data (never a second, independently-derived source
 * of truth), so Conservation D below ties their totals back to [rawInputCount]/[duplicateCollapsedCount].
 */
@Suppress("LongParameterList") // A cohesive audit record; every field is one distinct, minimized fact.
data class SessionEvidenceAudit(
    val rawInputCount: Int,
    val forBlockCount: Int,
    val uniqueIdentityCount: Int,
    val duplicateCollapsedCount: Int,
    val identityConflictRunCount: Int,
    val includedUniqueCount: Int,
    val excludedUniqueCount: Int,
    val exclusions: List<ExclusionCount>,
    val distinctScaleCount: Int,
    val policyVersions: PolicyVersionBundle,
    val stateCounts: List<StateCount>,
    val routeEligibilityCounts: List<RouteEligibilityCount>,
) {
    init {
        require(rawInputCount >= 0 && forBlockCount >= 0) { "counts must be >= 0." }
        require(forBlockCount <= rawInputCount) { "forBlockCount ($forBlockCount) must be <= rawInputCount." }
        // Conservation A: every for-block run is a dedup survivor, a collapsed identical duplicate, or a conflict.
        require(forBlockCount == uniqueIdentityCount + duplicateCollapsedCount + identityConflictRunCount) {
            "for-block runs must partition into unique + collapsed-duplicate + conflict."
        }
        // Conservation B: every unique dedup survivor is either included in a scale group or excluded.
        require(uniqueIdentityCount == includedUniqueCount + excludedUniqueCount) {
            "unique survivors must partition into included + excluded."
        }
        // Conservation C: the exclusions breakdown must exactly account for the run-level + unique-level drops.
        val notForBlock = rawInputCount - forBlockCount
        val tallied = exclusions.sumOf { it.count }
        require(tallied == notForBlock + identityConflictRunCount + excludedUniqueCount) {
            "exclusions ($tallied) must sum to not-for-block + conflict + excluded-unique."
        }
        require(exclusions.map { it.reason }.toSet().size == exclusions.size) { "exclusions must not repeat a reason." }
        // Conservation D (§18 C2.1): every raw run except a collapsed-identical duplicate receives EXACTLY one
        // canonical disposition (a duplicate is the SAME logical session as its surviving twin, never double
        // counted). Both re-bucketings (by state, by route eligibility) total the SAME figure.
        val dispositioned = rawInputCount - duplicateCollapsedCount
        val stateTotal = stateCounts.sumOf { it.count }
        require(stateTotal == dispositioned) {
            "stateCounts ($stateTotal) must sum to raw runs minus collapsed duplicates ($dispositioned)."
        }
        require(stateCounts.map { it.state }.toSet().size == stateCounts.size) {
            "stateCounts must not repeat a state."
        }
        require(routeEligibilityCounts.sumOf { it.count } == dispositioned) {
            "routeEligibilityCounts (${routeEligibilityCounts.sumOf { it.count }}) must sum to raw runs minus " +
                "collapsed duplicates ($dispositioned)."
        }
        require(routeEligibilityCounts.map { it.eligibility }.toSet().size == routeEligibilityCounts.size) {
            "routeEligibilityCounts must not repeat an eligibility."
        }
    }
}
