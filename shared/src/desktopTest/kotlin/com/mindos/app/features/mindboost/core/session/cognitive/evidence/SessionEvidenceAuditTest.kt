package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The conservation invariants (P1-12) + the §18 C2.1 state/route re-bucketing: no run is silently lost, double-counted, or mis-classified. */
class SessionEvidenceAuditTest {

    private val versions = PolicyVersionBundle("e", "l", "q", "r", "f")

    // Derives stateCounts/routeEligibilityCounts from the SAME (exclusions + included) via the canonical per-session
    // authority SessionEvidenceStatePolicy.dispositionOf — exactly as the production extractor's evidenceCountsOf
    // does — so every fixture audit is internally conservation-D-consistent. The mapping itself is pinned to EXPLICIT
    // expected values by [derived_stateCounts_and_routeEligibility_match_an_explicit_truth_table], so this derivation
    // is NOT the thing under test here (that is the SessionEvidenceAudit.init conservation), avoiding a tautology.
    private fun derivedCounts(
        exclusions: List<ExclusionCount>,
        includedUniqueCount: Int,
    ): Pair<List<StateCount>, List<RouteEligibilityCount>> {
        val stateTotals = LinkedHashMap<SessionEvidenceState, Int>()
        val eligibilityTotals = LinkedHashMap<EvidenceRouteEligibility, Int>()
        fun bump(reason: ExclusionReason?, amount: Int) {
            val disposition = SessionEvidenceStatePolicy.dispositionOf(reason)
            stateTotals[disposition.state] = (stateTotals[disposition.state] ?: 0) + amount
            eligibilityTotals[disposition.routeEligibility] =
                (eligibilityTotals[disposition.routeEligibility] ?: 0) + amount
        }
        exclusions.forEach { bump(it.reason, it.count) }
        if (includedUniqueCount > 0) bump(null, includedUniqueCount)
        return stateTotals.entries.map { StateCount(it.key, it.value) } to
            eligibilityTotals.entries.map { RouteEligibilityCount(it.key, it.value) }
    }

    private fun audit(
        rawInputCount: Int,
        forBlockCount: Int,
        uniqueIdentityCount: Int,
        duplicateCollapsedCount: Int,
        identityConflictRunCount: Int,
        includedUniqueCount: Int,
        excludedUniqueCount: Int,
        exclusions: List<ExclusionCount>,
        stateCounts: List<StateCount>? = null,
        routeEligibilityCounts: List<RouteEligibilityCount>? = null,
    ): SessionEvidenceAudit {
        val (derivedStates, derivedRoutes) = derivedCounts(exclusions, includedUniqueCount)
        return SessionEvidenceAudit(
            rawInputCount, forBlockCount, uniqueIdentityCount, duplicateCollapsedCount, identityConflictRunCount,
            includedUniqueCount, excludedUniqueCount, exclusions, distinctScaleCount = 1, policyVersions = versions,
            stateCounts = stateCounts ?: derivedStates,
            routeEligibilityCounts = routeEligibilityCounts ?: derivedRoutes,
        )
    }

    @Test
    fun a_conserving_audit_constructs() {
        // 6 raw: 1 not-for-block; 5 for-block = 3 unique + 1 collapsed-dup + 1 conflict-run;
        // 3 unique = 2 included + 1 excluded (PRELIMINARY). exclusions sum = 1 + 1 + 1 = 3.
        audit(
            rawInputCount = 6, forBlockCount = 5,
            uniqueIdentityCount = 3, duplicateCollapsedCount = 1, identityConflictRunCount = 1,
            includedUniqueCount = 2, excludedUniqueCount = 1,
            exclusions = listOf(
                ExclusionCount(ExclusionReason.NOT_FOR_BLOCK, 1),
                ExclusionCount(ExclusionReason.IDENTITY_CONFLICT, 1),
                ExclusionCount(ExclusionReason.PRELIMINARY, 1),
            ),
        )
    }

    @Test
    fun derived_stateCounts_and_routeEligibility_match_an_explicit_truth_table() {
        // §18 C2.1 truth-table with EXPLICIT expected values (not re-derived from the mapping under test):
        // 2 INCLUDED (null → PROVISIONAL / PROVISIONAL_PROFILE_ELIGIBLE) + 1 PRELIMINARY (→ PROVISIONAL /
        // PROVISIONAL_PROFILE_ELIGIBLE) + 1 NOT_FOR_BLOCK (→ INVALIDATED / TRAINING_ONLY) + 1 IDENTITY_CONFLICT
        // (→ INVALIDATED / TRAINING_ONLY). Also proves conservation D holds: 2+3 = 5 = raw 6 − collapsed 1.
        val (states, routes) = derivedCounts(
            exclusions = listOf(
                ExclusionCount(ExclusionReason.NOT_FOR_BLOCK, 1),
                ExclusionCount(ExclusionReason.IDENTITY_CONFLICT, 1),
                ExclusionCount(ExclusionReason.PRELIMINARY, 1),
            ),
            includedUniqueCount = 2,
        )
        assertEquals(
            mapOf(SessionEvidenceState.INVALIDATED to 2, SessionEvidenceState.PROVISIONAL to 3),
            states.associate { it.state to it.count },
        )
        assertEquals(
            mapOf(
                EvidenceRouteEligibility.TRAINING_ONLY to 2,
                EvidenceRouteEligibility.PROVISIONAL_PROFILE_ELIGIBLE to 3,
            ),
            routes.associate { it.eligibility to it.count },
        )
    }

    @Test
    fun conservation_A_partition_of_for_block_runs_is_enforced() {
        assertFailsWith<IllegalArgumentException> {
            audit(6, 5, 3, 1, 1, 2, 1, listOf(ExclusionCount(ExclusionReason.NOT_FOR_BLOCK, 1))).let { }
        }
    }

    @Test
    fun conservation_B_partition_of_unique_survivors_is_enforced() {
        assertFailsWith<IllegalArgumentException> {
            // unique 3 != included 2 + excluded 2
            audit(
                rawInputCount = 5, forBlockCount = 5,
                uniqueIdentityCount = 3, duplicateCollapsedCount = 2, identityConflictRunCount = 0,
                includedUniqueCount = 2, excludedUniqueCount = 2, exclusions = emptyList(),
            )
        }
    }

    @Test
    fun conservation_C_exclusion_tally_must_account_for_every_drop() {
        assertFailsWith<IllegalArgumentException> {
            // tally 0 != not-for-block(1) + conflict(0) + excluded(1)
            audit(
                rawInputCount = 5, forBlockCount = 4,
                uniqueIdentityCount = 4, duplicateCollapsedCount = 0, identityConflictRunCount = 0,
                includedUniqueCount = 3, excludedUniqueCount = 1, exclusions = emptyList(),
            )
        }
    }

    @Test
    fun conservation_D_stateCounts_must_sum_to_raw_minus_collapsed_duplicates() {
        // Explicit BAD stateCounts (sum 4 ≠ dispositioned 5 = raw 6 − collapsed 1) must be rejected by init even
        // though A/B/C all hold — proves init enforces the §18 C2.1 disposition conservation, not just the helper.
        assertFailsWith<IllegalArgumentException> {
            audit(
                rawInputCount = 6, forBlockCount = 5,
                uniqueIdentityCount = 3, duplicateCollapsedCount = 1, identityConflictRunCount = 1,
                includedUniqueCount = 2, excludedUniqueCount = 1,
                exclusions = listOf(
                    ExclusionCount(ExclusionReason.NOT_FOR_BLOCK, 1),
                    ExclusionCount(ExclusionReason.IDENTITY_CONFLICT, 1),
                    ExclusionCount(ExclusionReason.PRELIMINARY, 1),
                ),
                stateCounts = listOf(StateCount(SessionEvidenceState.PROVISIONAL, 4)),
                routeEligibilityCounts = listOf(
                    RouteEligibilityCount(EvidenceRouteEligibility.PROVISIONAL_PROFILE_ELIGIBLE, 5),
                ),
            )
        }
    }

    @Test
    fun a_repeated_reason_is_rejected() {
        assertFailsWith<IllegalArgumentException> {
            audit(
                rawInputCount = 2, forBlockCount = 2,
                uniqueIdentityCount = 2, duplicateCollapsedCount = 0, identityConflictRunCount = 0,
                includedUniqueCount = 0, excludedUniqueCount = 2,
                exclusions = listOf(
                    ExclusionCount(ExclusionReason.PRELIMINARY, 1),
                    ExclusionCount(ExclusionReason.PRELIMINARY, 1),
                ),
            )
        }
    }
}
