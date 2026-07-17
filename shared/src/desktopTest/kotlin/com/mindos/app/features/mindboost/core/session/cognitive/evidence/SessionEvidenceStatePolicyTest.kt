package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 (§7 canonical-vocabulary alignment, §18 C2.1) · [SessionEvidenceStatePolicy.stateOf]/[eligibilityOf]/
 * [dispositionOf] are total and pure over every [ExclusionReason]; this pins the disposition→state→eligibility→
 * reasons mapping the reopen doc §7/§18 froze. Fuller adversarial coverage of the evidence contract as a whole is
 * test-engineer's subsequent pass.
 */
class SessionEvidenceStatePolicyTest {

    @Test
    fun an_included_session_ceilings_at_provisional() {
        assertEquals(SessionEvidenceState.PROVISIONAL, SessionEvidenceStatePolicy.stateOf(null))
    }

    @Test
    fun purpose_timebase_criticality_and_validity_exclusions_map_to_training_only() {
        listOf(
            ExclusionReason.PURPOSE_EXCLUDED,
            ExclusionReason.TIMEBASE_NON_AUTHORITATIVE,
            ExclusionReason.RT_CRITICALITY_UNKNOWN,
            ExclusionReason.NOT_VALID,
        ).forEach { reason ->
            assertEquals(SessionEvidenceState.TRAINING_ONLY, SessionEvidenceStatePolicy.stateOf(reason))
        }
    }

    @Test
    fun preliminary_and_low_confidence_map_to_provisional() {
        listOf(ExclusionReason.PRELIMINARY, ExclusionReason.LOW_CONFIDENCE).forEach { reason ->
            assertEquals(SessionEvidenceState.PROVISIONAL, SessionEvidenceStatePolicy.stateOf(reason))
        }
    }

    @Test
    fun integrity_and_temporal_exclusions_map_to_invalidated() {
        listOf(
            ExclusionReason.NOT_COMPLETED,
            ExclusionReason.DATA_QUALITY,
            ExclusionReason.TIMING_COMPROMISED,
            ExclusionReason.IDENTITY_CONFLICT,
            ExclusionReason.QUARANTINED_INCONSISTENT_TIME,
            ExclusionReason.FUTURE_TIMESTAMP,
            ExclusionReason.NOT_FOR_BLOCK,
        ).forEach { reason ->
            assertEquals(SessionEvidenceState.INVALIDATED, SessionEvidenceStatePolicy.stateOf(reason))
        }
    }

    /**
     * §18 C2.1 exhaustive truth table: condition (every [ExclusionReason] plus the `null`/included case) →
     * canonical [SessionEvidenceState] → [EvidenceRouteEligibility] → [SessionEvidenceDisposition.reasons]. One
     * row per reachable condition; nothing here ever produces OFFICIAL_ELIGIBLE / PROFILE_ELIGIBLE / RANK_ELIGIBLE
     * (asserted separately below) because this contract cannot construct them.
     */
    @Test
    fun the_full_condition_to_state_to_eligibility_truth_table_is_exhaustive_and_exact() {
        val table = listOf(
            Triple(null, SessionEvidenceState.PROVISIONAL, EvidenceRouteEligibility.PROVISIONAL_PROFILE_ELIGIBLE),
            Triple(
                ExclusionReason.PRELIMINARY,
                SessionEvidenceState.PROVISIONAL,
                EvidenceRouteEligibility.PROVISIONAL_PROFILE_ELIGIBLE,
            ),
            Triple(
                ExclusionReason.LOW_CONFIDENCE,
                SessionEvidenceState.PROVISIONAL,
                EvidenceRouteEligibility.PROVISIONAL_PROFILE_ELIGIBLE,
            ),
            Triple(
                ExclusionReason.PURPOSE_EXCLUDED,
                SessionEvidenceState.TRAINING_ONLY,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.TIMEBASE_NON_AUTHORITATIVE,
                SessionEvidenceState.TRAINING_ONLY,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.RT_CRITICALITY_UNKNOWN,
                SessionEvidenceState.TRAINING_ONLY,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.NOT_VALID,
                SessionEvidenceState.TRAINING_ONLY,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.NOT_COMPLETED,
                SessionEvidenceState.INVALIDATED,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.DATA_QUALITY,
                SessionEvidenceState.INVALIDATED,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.TIMING_COMPROMISED,
                SessionEvidenceState.INVALIDATED,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.IDENTITY_CONFLICT,
                SessionEvidenceState.INVALIDATED,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.QUARANTINED_INCONSISTENT_TIME,
                SessionEvidenceState.INVALIDATED,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.FUTURE_TIMESTAMP,
                SessionEvidenceState.INVALIDATED,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
            Triple(
                ExclusionReason.NOT_FOR_BLOCK,
                SessionEvidenceState.INVALIDATED,
                EvidenceRouteEligibility.TRAINING_ONLY,
            ),
        )
        // Every ExclusionReason plus the null/included case is covered exactly once — an exhaustiveness proof
        // independent of the production `when`, so an added ExclusionReason with no table row fails this test.
        assertEquals(ExclusionReason.entries.size + 1, table.size, "the table must cover every condition exactly once")
        table.forEach { (reason, expectedState, expectedEligibility) ->
            val disposition = SessionEvidenceStatePolicy.dispositionOf(reason)
            val label = reason?.name ?: "null(included)"
            assertEquals(expectedState, disposition.state, "state for $label")
            assertEquals(expectedEligibility, disposition.routeEligibility, "eligibility for $label")
            assertEquals(expectedState, SessionEvidenceStatePolicy.stateOf(reason), "stateOf for $label")
            val eligibilityOfResult = SessionEvidenceStatePolicy.eligibilityOf(reason)
            assertEquals(expectedEligibility, eligibilityOfResult, "eligibilityOf for $label")
            assertEquals(listOfNotNull(reason), disposition.reasons, "reasons for $label")
        }
    }

    @Test
    fun official_profile_and_rank_eligible_are_never_produced_by_the_policy() {
        val allConditions = listOf<ExclusionReason?>(null) + ExclusionReason.entries
        allConditions.forEach { reason ->
            val disposition = SessionEvidenceStatePolicy.dispositionOf(reason)
            assertTrue(
                disposition.state != SessionEvidenceState.OFFICIAL_ELIGIBLE,
                "OFFICIAL_ELIGIBLE must stay structurally unreachable (condition: $reason)",
            )
            assertTrue(
                disposition.routeEligibility != EvidenceRouteEligibility.PROFILE_ELIGIBLE &&
                    disposition.routeEligibility != EvidenceRouteEligibility.RANK_ELIGIBLE,
                "PROFILE_ELIGIBLE/RANK_ELIGIBLE must stay structurally unreachable (condition: $reason)",
            )
        }
    }

    /**
     * §18 C4 structural guard: eligibility must never be coupled to a payment plan — proven two ways. (1) the
     * policy's own `dispositionOf` signature takes ONLY an [ExclusionReason]?, nothing plan-shaped. (2) the
     * underlying run model's OWN declared fields carry no plan/subscription field for a future policy to read.
     */
    @Test
    fun eligibility_is_never_coupled_to_a_subscription_plan_structurally() {
        val dispositionOfMethod = SessionEvidenceStatePolicy::class.java.declaredMethods
            .single { it.name == "dispositionOf" }
        assertEquals(
            listOf(ExclusionReason::class.java),
            dispositionOfMethod.parameterTypes.toList(),
            "dispositionOf must take only an ExclusionReason — no plan/subscription input exists to read",
        )
        val runFields = com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun::class.java
            .declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .filterNot { it.contains("$") }
            .toSet()
        val expectedRunFields = setOf(
            "sessionId", "blockId", "exerciseId", "exerciseVersion", "scoringVersion", "sessionPurpose",
            "startedAtEpochMs", "completedAtEpochMs", "durationSelectedMs", "durationActualMs", "completionStatus",
            "finalizationFailure", "difficulty", "trials", "answerSummary", "metrics", "exerciseMetrics", "score",
            "eligibility", "eligibilityTrace", "calibrationEffect", "anomalyFlags", "platform", "createdAtEpochMs",
            "updatedAtEpochMs",
        )
        assertEquals(
            expectedRunFields,
            runFields,
            "a plan/subscription field appearing here would let eligibility read one; golden field list must match",
        )
    }
}
