package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3-P2-02 · Structural complexity/volume benchmarks at n=0/1/10/~year/large-N. These assert STRUCTURAL
 * bounds — never wall-clock duration (flaky across CI/dev hardware, and explicitly out of scope here) — proving
 * the emitted [CognitiveSessionEvidence]'s SIZE tracks the number of DISTINCT exercise scales present, never the
 * raw session count, and that construction succeeds at volume: because [SessionEvidenceAudit]'s own `init` block
 * enforces its conservation invariants on every construction, a successful extraction at n=large is itself proof
 * there is no silent per-run accumulator quietly breaking conservation.
 */
class CognitiveSessionEvidencePerformanceTest {

    private companion object {
        const val DAY = 86_400_000L
        val NOW = EvaluationAsOf.authoritative(Long.MAX_VALUE / 2)
        const val YEAR_DAYS = 365
        const val LARGE_N = 5_000 // structural stand-in for "max" — NOT a load/timing test
    }

    private fun extract(block: ScoreModuleId, runs: List<CognitiveSessionRun>): CognitiveSessionEvidence =
        CognitiveSessionEvidenceExtractor.extractEvidence(
            block,
            SubjectScopedSessionBatch.of("subject", runs),
            NOW,
            EvidenceTestRegistries.PERMISSIVE,
        )

    /** [count] valid CALIBRATION runs, ONE per distinct UTC day, all sharing ONE scale key. */
    private fun oneRunPerDay(count: Int): List<CognitiveSessionRun> = (0 until count).map { i ->
        val at = DAY * (i + 1) + 1_000L
        CognitiveSessionRunFixtures.validRun(
            sessionId = "d_$i",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY * (i + 1),
            completedAtEpochMs = at,
            createdAtEpochMs = at,
            updatedAtEpochMs = at,
        )
    }

    /** [count] valid CALIBRATION runs, all sharing ONE scale key, packed into one short window. */
    private fun manyOnOneScale(count: Int): List<CognitiveSessionRun> = (0 until count).map { i ->
        val at = (i + 1) * 10L
        CognitiveSessionRunFixtures.validRun(
            sessionId = "r_$i",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = 1L,
            completedAtEpochMs = at,
            createdAtEpochMs = at,
            updatedAtEpochMs = at,
        )
    }

    @Test
    fun n_equals_0_is_the_zero_cost_floor() {
        val r = extract(ScoreModuleId.SPEED, emptyList())
        assertEquals(0, r.scaleGroups.size)
        assertTrue(r.audit.exclusions.isEmpty())
    }

    @Test
    fun n_equals_1_yields_exactly_one_bounded_group() {
        val r = extract(ScoreModuleId.SPEED, manyOnOneScale(1))
        assertEquals(1, r.scaleGroups.size)
        assertEquals(1, r.scaleGroups.single().observationCount)
    }

    @Test
    fun n_equals_10_output_size_tracks_scale_variety_not_n() {
        val r = extract(ScoreModuleId.SPEED, manyOnOneScale(10))
        assertEquals(1, r.scaleGroups.size, "10 runs on ONE scale still yield exactly ONE group")
        assertEquals(10, r.scaleGroups.single().observationCount)
    }

    @Test
    fun n_equals_a_year_of_daily_sessions_stays_one_bounded_group_with_a_real_span() {
        val r = extract(ScoreModuleId.SPEED, oneRunPerDay(YEAR_DAYS))
        val g = r.scaleGroups.single()
        assertEquals(YEAR_DAYS, g.observationCount)
        assertEquals(YEAR_DAYS, g.distinctDayCount, "one run per UTC day => one distinct day per run")
        assertTrue(g.spanMs != null && g.spanMs!! > 0L, "a year of sessions has a real, computed first->last span")
        assertTrue(
            r.audit.exclusions.size <= ExclusionReason.entries.size,
            "the exclusion tally is bounded by the number of DISTINCT reasons, never by raw n",
        )
    }

    @Test
    fun n_equals_large_n_output_size_stays_bounded_by_scale_variety_never_by_raw_n() {
        val r = extract(ScoreModuleId.SPEED, manyOnOneScale(LARGE_N))
        assertEquals(
            1,
            r.scaleGroups.size,
            "even at $LARGE_N runs, one scale key => one group (not proportional to n)",
        )
        assertEquals(LARGE_N, r.scaleGroups.single().observationCount)
        assertTrue(
            r.audit.exclusions.size <= ExclusionReason.entries.size,
            "the exclusion tally is bounded by the number of DISTINCT reasons, never by raw n",
        )
    }

    @Test
    fun output_size_is_proportional_to_distinct_scales_not_to_raw_n_across_three_scales() {
        val perScale = LARGE_N / 3
        val scales = listOf("exA", "exB", "exC").flatMapIndexed { scaleIdx, exerciseId ->
            (0 until perScale).map { i ->
                val at = (scaleIdx * perScale + i + 1) * 10L
                CognitiveSessionRunFixtures.validRun(
                    sessionId = "${exerciseId}_$i",
                    exerciseId = exerciseId,
                    sessionPurpose = SessionPurpose.CALIBRATION,
                    startedAtEpochMs = 1L,
                    completedAtEpochMs = at,
                    createdAtEpochMs = at,
                    updatedAtEpochMs = at,
                )
            }
        }
        val r = extract(ScoreModuleId.SPEED, scales)
        assertEquals(3, r.scaleGroups.size, "3 distinct exercises => exactly 3 groups, regardless of runs-per-scale")
        assertEquals(perScale * 3, r.audit.includedUniqueCount)
    }
}
