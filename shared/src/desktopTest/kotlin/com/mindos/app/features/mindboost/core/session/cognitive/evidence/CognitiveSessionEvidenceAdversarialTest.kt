package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunFixtures
import com.mindos.app.features.mindboost.core.session.cognitive.PlatformSnapshot
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 (§9 item 4 / psychometrician item 8) · The 6 adversarial contract cases for the FROZEN facts-only
 * evidence contract: "determinism is not validity" — absence must stay absence at every one of these boundaries,
 * never a fabricated or interpreted value. Complements (does not duplicate) the reworked
 * [CognitiveSessionEvidenceExtractorTest], which already pins re-derived quality gating, dedup/quarantine/future
 * handling, order-independence, and conservation in isolation; this file assembles the SIX specific adversarial
 * scenarios end to end and additionally proves — via JVM reflection, not just behavior — that the emitted types
 * carry NO evaluative field for a consumer to exploit.
 */
class CognitiveSessionEvidenceAdversarialTest {

    private companion object {
        const val DAY = 86_400_000L
        val NOW = EvaluationAsOf.authoritative(Long.MAX_VALUE / 2)
    }

    private fun extract(
        block: ScoreModuleId,
        runs: List<CognitiveSessionRun>,
    ): CognitiveSessionEvidence = CognitiveSessionEvidenceExtractor.extractEvidence(
        block,
        SubjectScopedSessionBatch.of("subject", runs),
        NOW,
        EvidenceTestRegistries.PERMISSIVE,
    )

    private fun CognitiveSessionEvidence.countOf(reason: ExclusionReason): Int =
        audit.exclusions.firstOrNull { it.reason == reason }?.count ?: 0

    /**
     * Reflection-based structural proof (not merely behavioral): the emitted types carry EXACTLY their documented
     * facts-only fields — no confidence/progress/maturity/freshness/pooled-statistic/rank field could be read off
     * them even by a consumer bypassing every other test, because the field simply does not exist. As of 6R3 §18
     * C2.3, [CognitiveSessionEvidence] has NO rank field of any shape (not even a count/size/distance).
     */
    private fun assertFactsOnlyShape() {
        val scaleFields = ExerciseScaleEvidence::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .filterNot { it.contains("$") }
            .toSet()
        assertEquals(
            setOf("lineage", "observationCount", "distinctDayCount", "spanMs"),
            scaleFields,
            "ExerciseScaleEvidence must carry ONLY these 4 facts — no confidence/progress/maturity/freshness field",
        )
        val topFields = CognitiveSessionEvidence::class.java.declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .filterNot { it.contains("$") }
            .toSet()
        assertEquals(
            setOf("blockId", "scaleGroups", "crossScaleComparability", "audit", "clockProvenance"),
            topFields,
            "CognitiveSessionEvidence must carry only these 5 members — no block estimate/confidence/rank field",
        )
    }

    // ───────────────────────── Case 1 · n=0 ─────────────────────────

    @Test
    fun case1_n_equals_0_absence_stays_absence_never_a_fabricated_value() {
        val r = extract(ScoreModuleId.SPEED, emptyList())
        assertTrue(r.scaleGroups.isEmpty(), "zero input runs must yield zero scale groups, never a fabricated one")
        assertEquals(0, r.audit.rawInputCount)
        assertEquals(0, r.audit.forBlockCount)
        assertEquals(0, r.audit.uniqueIdentityCount)
        assertEquals(0, r.audit.duplicateCollapsedCount)
        assertEquals(0, r.audit.identityConflictRunCount)
        assertEquals(0, r.audit.includedUniqueCount)
        assertEquals(0, r.audit.excludedUniqueCount)
        assertTrue(r.audit.exclusions.isEmpty(), "no runs means no exclusion tally at all")
        assertEquals(0, r.audit.distinctScaleCount)
        assertTrue(r.audit.stateCounts.isEmpty(), "no runs means no state tally at all")
        assertTrue(r.audit.routeEligibilityCounts.isEmpty(), "no runs means no eligibility tally at all")
        assertNull(r.crossScaleComparability, "fewer than 2 groups => no cross-scale relationship exists at all")
        assertFactsOnlyShape()
    }

    // ───────────────────────── Case 2 · n=1 ─────────────────────────

    @Test
    fun case2_n_equals_1_one_group_provisional_ceiling_no_evaluative_field_can_exist() {
        val run = CognitiveSessionRunFixtures.validRun(
            sessionId = "solo",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY,
            completedAtEpochMs = DAY + 1_000L,
            createdAtEpochMs = DAY + 1_000L,
            updatedAtEpochMs = DAY + 1_000L,
        )
        val r = extract(ScoreModuleId.SPEED, listOf(run))
        val g = r.scaleGroups.single()
        assertEquals(1, g.observationCount)
        assertEquals(1, g.distinctDayCount)
        // Honest limitation (report): the frozen contract carries no separate first/last-observed field — only
        // `spanMs`, which is null below 2 observations (never a fabricated 0). "first == last instant" for n=1
        // manifests here as span-absence, not a computed zero.
        assertNull(g.spanMs, "fewer than two observations => no span at all, not a fabricated zero")
        assertEquals(ComparabilityVerdict.ComparabilityNotEstablished, g.lineage.comparability)
        assertNull(r.crossScaleComparability, "only 1 group => no cross-scale relationship exists at all")
        // The session qualified (included) => its per-session canonical state is the PROVISIONAL ceiling, never
        // OFFICIAL_ELIGIBLE (structurally unreachable from this contract).
        assertEquals(SessionEvidenceState.PROVISIONAL, SessionEvidenceStatePolicy.stateOf(null))
        assertFactsOnlyShape()
    }

    // ───────────────────────── Case 3 · n=many, same UTC day ─────────────────────────

    @Test
    fun case3_n_equals_8_same_utc_day_grinding_never_inflates_distinct_days() {
        val runs = (0 until 8).map { i ->
            val at = (i + 1) * 1_000L
            CognitiveSessionRunFixtures.validRun(
                sessionId = "sameday_$i",
                sessionPurpose = SessionPurpose.CALIBRATION,
                startedAtEpochMs = 1L,
                completedAtEpochMs = at,
                createdAtEpochMs = at,
                updatedAtEpochMs = at,
            )
        }
        val r = extract(ScoreModuleId.SPEED, runs)
        val g = r.scaleGroups.single()
        assertEquals(8, g.observationCount, "8 same-day sessions are 8 observations, never collapsed by day")
        assertEquals(1, g.distinctDayCount, "all 8 fall on the same UTC day — grinding cannot manufacture new days")
        assertFactsOnlyShape()
    }

    // ───────────────────────── Case 4a · equal timestamps, distinct sessionIds ─────────────────────────

    @Test
    fun case4a_equal_timestamps_distinct_session_ids_deterministic_span_zero_no_trend_or_mean() {
        val at = DAY + 500L
        val runA = CognitiveSessionRunFixtures.validRun(
            sessionId = "tie_b",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY,
            completedAtEpochMs = at,
            createdAtEpochMs = at,
            updatedAtEpochMs = at,
        )
        val runB = CognitiveSessionRunFixtures.validRun(
            sessionId = "tie_a",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY,
            completedAtEpochMs = at,
            createdAtEpochMs = at,
            updatedAtEpochMs = at,
        )
        val forward = extract(ScoreModuleId.SPEED, listOf(runA, runB))
        val reversed = extract(ScoreModuleId.SPEED, listOf(runB, runA))
        assertEquals(forward, reversed, "input order must not affect a tied-timestamp result (order-independence)")
        val g = forward.scaleGroups.single()
        assertEquals(2, g.observationCount)
        assertEquals(0L, g.spanMs, "identical timestamps => a REAL, non-null zero span, not absence")
        assertEquals(1, g.distinctDayCount)
        // No trend/mean field exists anywhere on the contract to assert "not computed" against; its absence IS
        // the mutation-fixture's job (see assertFactsOnlyShape below).
        assertFactsOnlyShape()
    }

    // ───────────────────────── Case 4b · true identity conflict ─────────────────────────

    @Test
    fun case4b_true_identity_conflict_both_dropped_canonical_state_invalidated() {
        val conflictA = CognitiveSessionRunFixtures.validRun(
            sessionId = "conflict",
            sessionPurpose = SessionPurpose.CALIBRATION,
            score = CognitiveSessionRunFixtures.validScore().copy(normalized = 0.3),
        )
        val conflictB = CognitiveSessionRunFixtures.validRun(
            sessionId = "conflict",
            sessionPurpose = SessionPurpose.CALIBRATION,
            score = CognitiveSessionRunFixtures.validScore().copy(normalized = 0.9),
        )
        val r = extract(ScoreModuleId.SPEED, listOf(conflictA, conflictB))
        assertTrue(r.scaleGroups.isEmpty(), "both conflicting runs are dropped; nothing survives to a group")
        assertEquals(2, r.audit.identityConflictRunCount)
        assertEquals(2, r.countOf(ExclusionReason.IDENTITY_CONFLICT))
        assertEquals(0, r.audit.uniqueIdentityCount, "a pure conflict never reaches the unique-survivor bucket")
        assertEquals(0, r.audit.includedUniqueCount)
        assertEquals(
            SessionEvidenceState.INVALIDATED,
            SessionEvidenceStatePolicy.stateOf(ExclusionReason.IDENTITY_CONFLICT),
        )
        assertFactsOnlyShape()
    }

    // ───────────────────────── Case 5 · incompatible scoring versions ─────────────────────────

    @Test
    fun case5_incompatible_scoring_versions_are_never_pooled_two_groups_signal_incompatibility() {
        val v1 = CognitiveSessionRunFixtures.validRun(
            sessionId = "v1",
            exerciseId = "ex_shared",
            scoringVersion = "1.0.0",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY,
            completedAtEpochMs = DAY + 1_000L,
            createdAtEpochMs = DAY + 1_000L,
            updatedAtEpochMs = DAY + 1_000L,
        )
        val v2 = CognitiveSessionRunFixtures.validRun(
            sessionId = "v2",
            exerciseId = "ex_shared",
            scoringVersion = "2.0.0",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = 2 * DAY,
            completedAtEpochMs = 2 * DAY + 1_000L,
            createdAtEpochMs = 2 * DAY + 1_000L,
            updatedAtEpochMs = 2 * DAY + 1_000L,
        )
        val r = extract(ScoreModuleId.SPEED, listOf(v1, v2))
        assertEquals(
            2,
            r.scaleGroups.size,
            "differing scoringVersion => two DISTINCT scale groups; a differing scale key is never pooled",
        )
        assertEquals(2, r.audit.distinctScaleCount)
        r.scaleGroups.forEach { g ->
            assertEquals(1, g.observationCount, "each version's own group carries only its own run")
            assertEquals(
                ComparabilityVerdict.ComparabilityNotEstablished,
                g.lineage.comparability,
                "within-group ceiling stays honest even though the block as a whole is cross-scale incompatible",
            )
        }
        // §18 C2.2: 2+ scale groups without an approved bridge is POSITIVE, known incompatibility — the extractor
        // CONSTRUCTS ComparabilityVerdict.Incompatible for the block; it is NOT silently substituted by the group
        // count. This asserts the verdict is really emitted (the CEO's explicit requirement).
        assertEquals(
            ComparabilityVerdict.Incompatible,
            r.crossScaleComparability,
            "differing scale keys => Incompatible is emitted as a real verdict (§18 C2.2), never implied by count alone",
        )
        assertFactsOnlyShape()
    }

    // ───────────────────────── Case 6 · device/platform change, same scale key ─────────────────────────

    @Test
    fun case6_device_change_uncaptured_in_the_scale_key_still_one_group_never_bridged() {
        val android = CognitiveSessionRunFixtures.validRun(
            sessionId = "android_run",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = DAY,
            completedAtEpochMs = DAY + 1_000L,
            createdAtEpochMs = DAY + 1_000L,
            updatedAtEpochMs = DAY + 1_000L,
            platform = PlatformSnapshot(osName = "Android", osVersion = "14", appVersion = "1.0.0", deviceTier = null),
        )
        val ios = CognitiveSessionRunFixtures.validRun(
            sessionId = "ios_run",
            sessionPurpose = SessionPurpose.CALIBRATION,
            startedAtEpochMs = 2 * DAY,
            completedAtEpochMs = 2 * DAY + 1_000L,
            createdAtEpochMs = 2 * DAY + 1_000L,
            updatedAtEpochMs = 2 * DAY + 1_000L,
            platform = PlatformSnapshot(osName = "iOS", osVersion = "17", appVersion = "1.0.0", deviceTier = null),
        )
        val r = extract(ScoreModuleId.SPEED, listOf(android, ios))
        assertEquals(
            1,
            r.scaleGroups.size,
            "platform/device is not part of the scale key => both pool into ONE group (device uncaptured)",
        )
        val g = r.scaleGroups.single()
        assertEquals(2, g.observationCount)
        assertEquals(
            ComparabilityVerdict.ComparabilityNotEstablished,
            g.lineage.comparability,
            "device/modality lineage is uncaptured; comparability is never claimed, never Bridged",
        )
        assertNull(
            r.crossScaleComparability,
            "a single pooled group => no cross-scale relationship exists to judge (null, never a fabricated verdict)",
        )
        // ExerciseScaleEvidence has no per-device breakdown field at all (assertFactsOnlyShape) — the contract
        // structurally cannot expose a value pooling (or splitting) the two device runs beyond the raw count.
        assertFactsOnlyShape()
    }
}
