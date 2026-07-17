package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.ClockSource
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SYSTEM-1 6R3-P1-06/P1-07 · Fills the two gaps [CognitiveSessionEvidenceExtractorTest] does not close: it proves
 * the FULL disposition->state PIPELINE — a run's own raw facts drive the extractor to an [ExclusionReason], which
 * [SessionEvidenceStatePolicy] then maps to its canonical [SessionEvidenceState] — for the two RT-criticality and
 * TRAINING_FREE gates specifically, not just the pure policy mapping in isolation
 * ([SessionEvidenceStatePolicyTest] already pins that mapping directly with a literal reason). The registry-miss
 * fail-closed matrix itself (null/RT_SECONDARY/RT_CRITICAL all require an authoritative timebase; only an explicit
 * RT_IRRELEVANT waives it) is already fully covered by
 * `rt_criticality_policy_requires_the_authoritative_timebase_unless_explicitly_declared_irrelevant` and
 * `the_registry_declares_no_exercise_today` in the reworked extractor test — not duplicated here.
 *
 * NOT expressible against the current API (report note): [RtCriticalityRegistry]'s `declared` map is a hardcoded
 * `private val = emptyMap()` with no test-injection seam, so a run-level scenario where the registry actually
 * RETURNS an explicit `RT_IRRELEVANT` for some (exerciseId, exerciseVersion) cannot be constructed through
 * [CognitiveSessionEvidenceExtractor] today — only the pure policy function
 * ([RtCriticalityRegistry.requiresAuthoritativeTimebase]) can be exercised for that branch, which the existing
 * extractor test already does directly.
 */
class CognitiveSessionEvidenceRtCriticalityTrainingFreeTest {

    private companion object {
        val NOW = EvaluationAsOf.authoritative(Long.MAX_VALUE / 2)
    }

    private fun extract(
        runs: List<CognitiveSessionRun>,
        registry: RtCriticalityRegistry = RtCriticalityRegistry(),
    ): CognitiveSessionEvidence {
        val batch = SubjectScopedSessionBatch.of("subject", runs)
        return CognitiveSessionEvidenceExtractor.extractEvidence(ScoreModuleId.SPEED, batch, NOW, registry)
    }

    @Test
    fun a_registry_miss_on_a_non_frame_clock_excludes_the_run_whose_canonical_state_is_training_only() {
        val nonFrameTiming = CognitiveSessionRunFixtures.cleanTiming()
            .copy(clockSource = ClockSource.WALL_CLOCK_DERIVED)
        val run = CognitiveSessionRunFixtures.validRun(
            eligibility = CognitiveSessionRunFixtures.validEligibility().copy(timingIntegrity = nonFrameTiming),
        )
        val r = extract(listOf(run))
        val reason = r.audit.exclusions.single().reason
        val message = "an undeclared exercise is UNKNOWN, fail-closed"
        assertEquals(ExclusionReason.RT_CRITICALITY_UNKNOWN, reason, message)
        assertEquals(SessionEvidenceState.TRAINING_ONLY, SessionEvidenceStatePolicy.stateOf(reason))
    }

    @Test
    fun a_full_training_free_session_is_purpose_excluded_whose_canonical_state_is_training_only() {
        val run = CognitiveSessionRunFixtures.validRun(sessionPurpose = SessionPurpose.TRAINING_FREE)
        val r = extract(listOf(run), EvidenceTestRegistries.PERMISSIVE)
        val reason = r.audit.exclusions.single().reason
        assertEquals(
            ExclusionReason.PURPOSE_EXCLUDED,
            reason,
            "full TRAINING_FREE has no sealed probe-identity/within-dose fact on the run today (6R3-P1-06)",
        )
        assertEquals(SessionEvidenceState.TRAINING_ONLY, SessionEvidenceStatePolicy.stateOf(reason))
    }
}
