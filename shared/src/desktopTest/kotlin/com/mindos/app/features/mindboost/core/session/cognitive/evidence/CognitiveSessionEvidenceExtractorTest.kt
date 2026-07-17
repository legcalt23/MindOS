package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.core.session.cognitive.ClockSource
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import com.mindos.app.features.mindboost.core.session.cognitive.SessionCompletionStatus
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import com.mindos.app.features.mindboost.core.session.cognitive.TimingIntegrityStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 · The PURE, NOT-WIRED [CognitiveSessionEvidenceExtractor]. These pin the evidence-only contract:
 * re-derived quality (never a trusted flag), per-exercise scale groups (never a block estimate), raw counts only
 * (never a maturity/progress band or a raw per-session score list), fail-closed RT-criticality, honest absence,
 * deterministic order-independence, dedup/quarantine/future handling, and conservation. As of 6R3 §18 C2.3 this
 * contract ships NO block-readiness-for-ranking vocabulary at all — the dedicated RT-criticality-gate tests below
 * use the PRODUCTION registry default on purpose (they ARE about that gate); every other test uses
 * [EvidenceTestRegistries.PERMISSIVE] so [CognitiveSessionRunFixtures.validRun]'s default (unregistered) exercise
 * identity does not fail closed for a reason unrelated to what it tests.
 */
class CognitiveSessionEvidenceExtractorTest {

    private companion object {
        const val DAY = 86_400_000L
        val NOW = EvaluationAsOf.authoritative(Long.MAX_VALUE / 2)
    }

    private fun extract(
        block: ScoreModuleId,
        runs: List<CognitiveSessionRun>,
        asOf: EvaluationAsOf = NOW,
        registry: RtCriticalityRegistry = EvidenceTestRegistries.PERMISSIVE,
    ): CognitiveSessionEvidence = CognitiveSessionEvidenceExtractor.extractEvidence(
        block,
        SubjectScopedSessionBatch.of("subject", runs),
        asOf,
        registry,
    )

    private fun CognitiveSessionEvidence.countOf(reason: ExclusionReason): Int =
        audit.exclusions.firstOrNull { it.reason == reason }?.count ?: 0

    private fun validScore() = CognitiveSessionRunFixtures.validScore()
    private fun validEligibility() = CognitiveSessionRunFixtures.validEligibility()
    private fun cleanTiming() = CognitiveSessionRunFixtures.cleanTiming()

    /** N valid CALIBRATION runs (a purpose this contract still counts), one per DISTINCT day, same scale key. */
    private fun validRunsAcrossDays(
        count: Int,
        block: ScoreModuleId = ScoreModuleId.SPEED,
        normalized: Double = 0.8,
    ): List<CognitiveSessionRun> = (0 until count).map { i ->
        val at = DAY * (i + 1) + 121_000L
        CognitiveSessionRunFixtures.validRun(
            sessionId = "sess_$i",
            blockId = block,
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY * (i + 1),
            completedAtEpochMs = at,
            createdAtEpochMs = at,
            updatedAtEpochMs = at,
            score = validScore().copy(normalized = normalized),
        )
    }

    // ───────────────────────── Absence ─────────────────────────

    @Test
    fun an_empty_batch_yields_no_groups_and_a_conserving_audit() {
        val r = extract(ScoreModuleId.SPEED, emptyList())
        assertTrue(r.scaleGroups.isEmpty(), "absence is absence, never a fabricated group")
        assertEquals(0, r.audit.rawInputCount)
        assertEquals(0, r.audit.includedUniqueCount)
    }

    // ───────────────────────── Re-derived quality gating ─────────────────────────

    @Test
    fun abandoned_data_quality_timing_non_valid_preliminary_low_confidence_all_excluded() {
        val abandoned = CognitiveSessionRunFixtures.validRun(completionStatus = SessionCompletionStatus.ABANDONED)
        assertEquals(1, extract(ScoreModuleId.SPEED, listOf(abandoned)).countOf(ExclusionReason.NOT_COMPLETED))

        val dq = CognitiveSessionRunFixtures.validRun(eligibility = validEligibility().copy(dataQualityCompromised = true))
        assertEquals(1, extract(ScoreModuleId.SPEED, listOf(dq)).countOf(ExclusionReason.DATA_QUALITY))

        val timing = validEligibility().copy(
            timingIntegrity = cleanTiming().copy(integrityStatus = TimingIntegrityStatus.COMPROMISED),
        )
        assertEquals(
            1,
            extract(ScoreModuleId.SPEED, listOf(CognitiveSessionRunFixtures.validRun(eligibility = timing)))
                .countOf(ExclusionReason.TIMING_COMPROMISED),
        )

        val nonValid = CognitiveSessionRunFixtures.validRun(score = validScore().copy(category = ScoreState.CALIBRATING))
        assertEquals(1, extract(ScoreModuleId.SPEED, listOf(nonValid)).countOf(ExclusionReason.NOT_VALID))

        val prelim = CognitiveSessionRunFixtures.validRun(score = validScore().copy(preliminary = true))
        assertEquals(1, extract(ScoreModuleId.SPEED, listOf(prelim)).countOf(ExclusionReason.PRELIMINARY))

        val lowConf = CognitiveSessionRunFixtures.validRun(score = validScore().copy(lowConfidence = true))
        assertEquals(1, extract(ScoreModuleId.SPEED, listOf(lowConf)).countOf(ExclusionReason.LOW_CONFIDENCE))
    }

    @Test
    fun practice_debug_and_full_training_free_are_purpose_excluded() {
        // 6R3-P1-06: full TRAINING_FREE has no sealed probe-identity/within-dose fact on the run today, so it joins
        // PRACTICE/DEBUG as always excluded from this contract (fail-closed → TRAINING_ONLY), independent of the
        // shared, forgeable `SessionPurpose.mayContributeToProfile()` (which returns true for TRAINING_FREE).
        listOf(SessionPurpose.PRACTICE, SessionPurpose.DEBUG, SessionPurpose.TRAINING_FREE).forEach { purpose ->
            val run = CognitiveSessionRunFixtures.validRun(sessionPurpose = purpose)
            assertEquals(1, extract(ScoreModuleId.SPEED, listOf(run)).countOf(ExclusionReason.PURPOSE_EXCLUDED))
        }
    }

    // ───────────────────────── P1-06: forgeable eligibility flags are NOT trusted ─────────────────────────

    @Test
    fun a_profile_ineligible_flag_is_ignored_when_the_raw_facts_are_valid() {
        val run = CognitiveSessionRunFixtures.validRun(
            sessionPurpose = SessionPurpose.CALIBRATION,
            eligibility = validEligibility().copy(
                profileEligible = false, calibrationEligible = false, rankingEligible = false,
            ),
        )
        val g = extract(ScoreModuleId.SPEED, listOf(run)).scaleGroups.single()
        assertEquals(1, g.observationCount, "the forgeable eligibility booleans are ignored; raw facts govern")
    }

    @Test
    fun a_preliminary_run_is_still_excluded_by_the_raw_fact_even_with_all_flags_set() {
        // The E8-today shape: flags say false anyway, but the RAW preliminary fact is what excludes it — proving the
        // gate does not depend on the flags in either direction.
        val e8 = CognitiveSessionRunFixtures.validRun(score = validScore().copy(preliminary = true))
        val r = extract(ScoreModuleId.SPEED, listOf(e8))
        assertTrue(r.scaleGroups.isEmpty())
        assertEquals(1, r.countOf(ExclusionReason.PRELIMINARY))
    }

    // ─────────── §18 C4: RT-criticality re-derived per-exercise, fail-closed on UNKNOWN (stricter than P1-07) ──────

    @Test
    fun a_declared_rt_critical_run_on_a_non_frame_clock_is_excluded_as_timebase_non_authoritative() {
        val declaredCritical = RtCriticalityRegistry(
            declared = listOf(RtCriticalityDeclaration("critical-ex", "1.0", RtCriticality.RT_CRITICAL)),
        )
        val run = CognitiveSessionRunFixtures.validRun(
            blockId = ScoreModuleId.SPEED,
            exerciseId = "critical-ex",
            exerciseVersion = "1.0",
            eligibility = validEligibility().copy(
                timingIntegrity = cleanTiming().copy(clockSource = ClockSource.WALL_CLOCK_DERIVED),
            ),
        )
        val r = extract(ScoreModuleId.SPEED, listOf(run), registry = declaredCritical)
        assertTrue(r.scaleGroups.isEmpty())
        assertEquals(1, r.countOf(ExclusionReason.TIMEBASE_NON_AUTHORITATIVE))
    }

    @Test
    fun a_block_with_no_declared_rt_criticality_on_a_non_frame_clock_is_excluded_as_rt_criticality_unknown() {
        // §18 C4: an undeclared (exerciseId, exerciseVersion) is UNKNOWN — excluded via its OWN dedicated reason,
        // never silently reclassified as TIMEBASE_NON_AUTHORITATIVE (fixes the Enfoque flanker/go-no-go/stop-signal
        // fail-open gap: an unregistered exercise is never mistaken for "not timing-critical").
        val run = CognitiveSessionRunFixtures.validRun(
            blockId = ScoreModuleId.MEMORY,
            eligibility = validEligibility().copy(
                timingIntegrity = cleanTiming().copy(clockSource = ClockSource.WALL_CLOCK_DERIVED),
            ),
        )
        val r = extract(ScoreModuleId.MEMORY, listOf(run), registry = RtCriticalityRegistry())
        assertTrue(r.scaleGroups.isEmpty())
        assertEquals(1, r.countOf(ExclusionReason.RT_CRITICALITY_UNKNOWN))
    }

    @Test
    fun an_undeclared_exercise_is_excluded_even_with_an_authoritative_clock_a_good_clock_cannot_rescue_unknown() {
        // The exact regression §18 C4 closes: previously, an undeclared exercise fell back to being TREATED like
        // RT_CRITICAL, so a good clock would let it silently qualify despite never being declared. Now UNKNOWN
        // excludes UNCONDITIONALLY, regardless of clock quality.
        val run = CognitiveSessionRunFixtures.validRun(
            blockId = ScoreModuleId.MEMORY,
            eligibility = validEligibility().copy(
                timingIntegrity = cleanTiming().copy(clockSource = ClockSource.FRAME_MONOTONIC),
            ),
        )
        val r = extract(ScoreModuleId.MEMORY, listOf(run), registry = RtCriticalityRegistry())
        assertTrue(r.scaleGroups.isEmpty(), "an authoritative clock must not rescue an UNKNOWN-criticality exercise")
        assertEquals(1, r.countOf(ExclusionReason.RT_CRITICALITY_UNKNOWN))
    }

    @Test
    fun an_rt_non_critical_exercise_is_never_excluded_by_clock_quality() {
        val nonCritical = RtCriticalityRegistry(
            declared = listOf(RtCriticalityDeclaration("non-critical-ex", "1.0", RtCriticality.RT_NON_CRITICAL)),
        )
        val run = CognitiveSessionRunFixtures.validRun(
            blockId = ScoreModuleId.SPEED,
            exerciseId = "non-critical-ex",
            exerciseVersion = "1.0",
            sessionPurpose = SessionPurpose.CALIBRATION,
            eligibility = validEligibility().copy(
                timingIntegrity = cleanTiming().copy(clockSource = ClockSource.WALL_CLOCK_DERIVED),
            ),
        )
        val r = extract(ScoreModuleId.SPEED, listOf(run), registry = nonCritical)
        assertEquals(1, r.scaleGroups.single().observationCount, "RT_NON_CRITICAL never requires a good clock")
    }

    // ───────────────────────── NOT_FOR_BLOCK ─────────────────────────

    @Test
    fun another_blocks_runs_do_not_contaminate() {
        val speed = CognitiveSessionRunFixtures.validRun(
            sessionId = "s", blockId = ScoreModuleId.SPEED, sessionPurpose = SessionPurpose.CALIBRATION,
        )
        val memory = CognitiveSessionRunFixtures.validRun(sessionId = "m", blockId = ScoreModuleId.MEMORY)
        val r = extract(ScoreModuleId.SPEED, listOf(speed, memory))
        assertEquals(ScoreModuleId.SPEED, r.blockId)
        assertEquals(1, r.scaleGroups.single().observationCount)
        assertEquals(1, r.countOf(ExclusionReason.NOT_FOR_BLOCK))
    }

    // ───────────────────────── P1-04/05: all scales preserved, per-exercise, no recency displacement ─────────────

    @Test
    fun two_different_scale_keys_form_two_groups_none_discarded() {
        val a = CognitiveSessionRunFixtures.validRun(
            sessionId = "a", exerciseId = "ex1", sessionPurpose = SessionPurpose.CALIBRATION,
        )
        val b = CognitiveSessionRunFixtures.validRun(
            sessionId = "b", exerciseId = "ex2", sessionPurpose = SessionPurpose.CALIBRATION,
        )
        val r = extract(ScoreModuleId.SPEED, listOf(a, b))
        assertEquals(2, r.scaleGroups.size, "every distinct scale is its own group; none discarded")
        assertEquals(2, r.audit.distinctScaleCount)
        assertEquals(listOf("ex1", "ex2"), r.scaleGroups.map { it.lineage.scaleKey.exerciseId })
    }

    @Test
    fun an_older_version_run_with_a_fresh_timestamp_does_not_displace_the_other_series() {
        val currentVersion = CognitiveSessionRunFixtures.validRun(
            sessionId = "cur", scoringVersion = "1.0.0", sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY, completedAtEpochMs = DAY + 1, createdAtEpochMs = DAY + 1, updatedAtEpochMs = DAY + 1,
        )
        val staleVersionFresherStamp = CognitiveSessionRunFixtures.validRun(
            sessionId = "old", scoringVersion = "0.9.0", sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = 5 * DAY, completedAtEpochMs = 5 * DAY + 1,
            createdAtEpochMs = 5 * DAY + 1, updatedAtEpochMs = 5 * DAY + 1,
        )
        val r = extract(ScoreModuleId.SPEED, listOf(currentVersion, staleVersionFresherStamp))
        assertEquals(2, r.scaleGroups.size, "recency never displaces a valid series (P1-04)")
        r.scaleGroups.forEach { assertEquals(1, it.observationCount) }
    }

    @Test
    fun a_group_asserts_only_within_scale_comparability_never_established() {
        val g = extract(ScoreModuleId.SPEED, validRunsAcrossDays(3)).scaleGroups.single()
        assertEquals(ComparabilityVerdict.ComparabilityNotEstablished, g.lineage.comparability)
    }

    // ───────────────────────── Raw distinct-day fact (6R3 §7: no maturity/progress band survives) ─────────────────

    @Test
    fun three_utc_days_spanning_under_48h_are_recorded_as_three_distinct_days() {
        fun run(id: String, at: Long) = CognitiveSessionRunFixtures.validRun(
            sessionId = id, sessionPurpose = SessionPurpose.CALIBRATION, startedAtEpochMs = 1L,
            completedAtEpochMs = at, createdAtEpochMs = at, updatedAtEpochMs = at,
        )
        val runs = listOf(
            run("a", DAY - 1_000L), run("b", DAY - 500L),          // day 0
            run("c", DAY + 1_000L), run("d", DAY + 1_500L),        // day 1
            run("e", 2 * DAY + 1_000L),                            // day 2 — span ≈ 24h < 48h
        )
        val g = extract(ScoreModuleId.SPEED, runs).scaleGroups.single()
        assertEquals(5, g.observationCount)
        assertEquals(3, g.distinctDayCount)
    }

    // ───────────────────────── INITIAL_ASSESSMENT / TRAINING_FREE ─────────────────────────

    @Test
    fun initial_assessment_is_exercise_evidence() {
        val run = CognitiveSessionRunFixtures.validRun(sessionPurpose = SessionPurpose.INITIAL_ASSESSMENT)
        val r = extract(ScoreModuleId.SPEED, listOf(run))
        val g = r.scaleGroups.single()
        assertEquals(1, g.observationCount, "an initial assessment is quality-valid exercise evidence")
    }

    // ───────────────────────── Dedup / identity / determinism (P1-10) ─────────────────────────

    @Test
    fun an_identical_duplicate_is_idempotent() {
        val run = CognitiveSessionRunFixtures.validRun(sessionId = "dup", sessionPurpose = SessionPurpose.CALIBRATION)
        val once = extract(ScoreModuleId.SPEED, listOf(run))
        val twice = extract(ScoreModuleId.SPEED, listOf(run, run.copy()))
        assertEquals(once.scaleGroups, twice.scaleGroups)
        assertEquals(1, twice.scaleGroups.single().observationCount)
        assertEquals(1, twice.audit.duplicateCollapsedCount)
        assertEquals(0, twice.countOf(ExclusionReason.IDENTITY_CONFLICT))
    }

    @Test
    fun a_conflicting_duplicate_fails_closed_dropping_both_runs() {
        val a = CognitiveSessionRunFixtures.validRun(sessionId = "x", score = validScore().copy(normalized = 0.4))
        val b = CognitiveSessionRunFixtures.validRun(sessionId = "x", score = validScore().copy(normalized = 0.9))
        val good = CognitiveSessionRunFixtures.validRun(sessionId = "y", sessionPurpose = SessionPurpose.CALIBRATION)
        val r = extract(ScoreModuleId.SPEED, listOf(a, b, good))
        assertEquals(2, r.countOf(ExclusionReason.IDENTITY_CONFLICT), "BOTH conflicting runs dropped (runs, not ids)")
        assertEquals(1, r.scaleGroups.single().observationCount, "only the unambiguous run survives")
    }

    @Test
    fun a_permutation_of_the_input_yields_an_identical_result() {
        val runs = validRunsAcrossDays(5)
        val shuffled = listOf(runs[3], runs[0], runs[4], runs[1], runs[2])
        assertEquals(extract(ScoreModuleId.SPEED, runs), extract(ScoreModuleId.SPEED, shuffled))
    }

    @Test
    fun the_input_list_is_not_mutated() {
        val runs = validRunsAcrossDays(3)
        val snapshot = runs.toList()
        extract(ScoreModuleId.SPEED, runs)
        assertEquals(snapshot, runs)
    }

    @Test
    fun extraction_is_deterministic() {
        val runs = validRunsAcrossDays(3)
        assertEquals(extract(ScoreModuleId.SPEED, runs), extract(ScoreModuleId.SPEED, runs))
    }

    // ───────────────────────── Temporal (P1-09) ─────────────────────────

    @Test
    fun a_future_run_relative_to_now_is_excluded() {
        val runs = listOf(
            validRunsAcrossDays(1).first(),
            CognitiveSessionRunFixtures.validRun(
                sessionId = "future", startedAtEpochMs = 5 * DAY, completedAtEpochMs = 5 * DAY + 1,
                createdAtEpochMs = 5 * DAY + 1, updatedAtEpochMs = 5 * DAY + 1,
            ),
        )
        val r = extract(ScoreModuleId.SPEED, runs, asOf = EvaluationAsOf.authoritative(2 * DAY))
        assertEquals(1, r.scaleGroups.single().observationCount)
        assertEquals(1, r.countOf(ExclusionReason.FUTURE_TIMESTAMP))
    }

    @Test
    fun an_intrinsically_inconsistent_time_is_quarantined_regardless_of_the_clock() {
        // completedAt AFTER the last write is impossible → quarantined permanently, even with a far-future "now".
        val bad = CognitiveSessionRunFixtures.validRun(
            sessionId = "bad", startedAtEpochMs = 1L,
            completedAtEpochMs = 200_000L, createdAtEpochMs = 150_000L, updatedAtEpochMs = 150_000L,
        )
        val r = extract(ScoreModuleId.SPEED, listOf(bad))
        assertTrue(r.scaleGroups.isEmpty())
        assertEquals(1, r.countOf(ExclusionReason.QUARANTINED_INCONSISTENT_TIME))
        assertEquals(0, r.countOf(ExclusionReason.FUTURE_TIMESTAMP), "intrinsic, not clock-relative")
    }

    // ───────────────────────── Conservation + totality + provenance ─────────────────────────

    @Test
    fun the_audit_conserves_a_mixed_input() {
        val valid = CognitiveSessionRunFixtures.validRun(sessionId = "v", sessionPurpose = SessionPurpose.CALIBRATION)
        val dup = valid.copy()
        val memory = CognitiveSessionRunFixtures.validRun(sessionId = "m", blockId = ScoreModuleId.MEMORY)
        val abandoned = CognitiveSessionRunFixtures.validRun(
            sessionId = "a", completionStatus = SessionCompletionStatus.ABANDONED,
        )
        val r = extract(ScoreModuleId.SPEED, listOf(valid, dup, memory, abandoned))
        // 4 raw; 1 not-for-block (memory); 3 for-block = 1 unique(v) + 1 collapsed(dup) + ... 'a' is unique too.
        assertEquals(4, r.audit.rawInputCount)
        assertEquals(3, r.audit.forBlockCount)
        assertEquals(2, r.audit.uniqueIdentityCount, "v and a are unique; dup collapses into v")
        assertEquals(1, r.audit.duplicateCollapsedCount)
        assertEquals(1, r.audit.includedUniqueCount, "only v qualifies")
        assertEquals(1, r.audit.excludedUniqueCount, "a is abandoned")
        assertEquals(1, r.countOf(ExclusionReason.NOT_FOR_BLOCK))
        assertEquals(1, r.countOf(ExclusionReason.NOT_COMPLETED))
    }

    @Test
    fun all_nine_blocks_are_crash_free_on_empty_history() {
        ScoreModuleId.entries.forEach { block ->
            val r = extract(block, emptyList())
            assertEquals(block, r.blockId)
            assertTrue(r.scaleGroups.isEmpty())
        }
    }

    @Test
    fun the_clock_provenance_is_surfaced() {
        val best = extract(ScoreModuleId.SPEED, emptyList(), EvaluationAsOf.bestEffort(1_000L))
        assertEquals(ClockProvenance.BEST_EFFORT_MONOTONIC, best.clockProvenance)
        assertNotEquals(ClockProvenance.AUTHORITATIVE_INJECTED, best.clockProvenance)
    }

    // ───── §18 C2.3: no block-readiness-for-ranking vocabulary at all (not a count, size, or distance-to-threshold)

    @Test
    fun the_evidence_output_carries_no_rank_field_even_with_five_sessions_across_distinct_days() {
        val r = extract(ScoreModuleId.SPEED, validRunsAcrossDays(5))
        val topFields = CognitiveSessionEvidence::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .filterNot { it.contains("$") }
            .toSet()
        assertEquals(
            setOf("blockId", "scaleGroups", "crossScaleComparability", "audit", "clockProvenance"),
            topFields,
            "no rank field of any shape may exist on the emitted type, however much evidence accumulates",
        )
        assertEquals(5, r.scaleGroups.single().observationCount)
    }
}
