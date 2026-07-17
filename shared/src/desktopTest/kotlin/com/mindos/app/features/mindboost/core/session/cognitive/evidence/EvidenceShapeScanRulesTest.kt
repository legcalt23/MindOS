package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 (§10 permanent mutation-fixture) · Proves [EvidenceShapeScanRules] actually DISCRIMINATES a
 * forbidden evaluative shape from a legitimate facts-only one, against synthetic snippets — the reintroduction
 * proof a future edit to the Tier-1/Tier-2 rule list (here, in the Gradle task, or in
 * [CognitiveSessionEvidenceBoundaryTest]) can be checked against without mutating the real evidence-package tree.
 * The OTHER mutation proof (reintroduce a real forbidden field, watch the Gradle task + the boundary test go RED,
 * then revert) is a one-time, Gradle-only landing verification documented as a procedure for the CEO — not
 * attempted here (no Gradle CLI in this pass).
 */
class EvidenceShapeScanRulesTest {

    // ───────────────────────── RED: Tier 1 structural type tokens ─────────────────────────

    @Test
    fun flags_a_bare_double_field() {
        val hits = EvidenceShapeScanRules.scanText("val ratio: Double")
        assertTrue(hits.isNotEmpty(), "a bare Double field must be flagged (an evaluative ratio needs this type)")
    }

    @Test
    fun flags_a_list_of_double() {
        val hits = EvidenceShapeScanRules.scanText("val scores: List<Double>")
        assertTrue(hits.isNotEmpty(), "List<Double> must be flagged — the raw per-session score-list shape")
    }

    @Test
    fun flags_a_double_array() {
        val hits = EvidenceShapeScanRules.scanText("val a: DoubleArray")
        assertTrue(hits.isNotEmpty(), "DoubleArray must be flagged")
    }

    @Test
    fun flags_a_float_array() {
        val hits = EvidenceShapeScanRules.scanText("val b: FloatArray")
        assertTrue(hits.isNotEmpty(), "FloatArray must be flagged")
    }

    @Test
    fun flags_a_duration_field() {
        val hits = EvidenceShapeScanRules.scanText("val span: Duration")
        assertTrue(hits.isNotEmpty(), "Duration must be flagged (6R3 §18 C3)")
    }

    @Test
    fun flags_a_typealias_declaration() {
        val hits = EvidenceShapeScanRules.scanText("typealias ScoreList = List<Int>")
        assertTrue(hits.isNotEmpty(), "typealias must be flagged (6R3 §18 C3)")
    }

    @Test
    fun flags_a_bare_map_field() {
        val hits = EvidenceShapeScanRules.scanText("val declared: Map<String, Int>")
        assertTrue(hits.isNotEmpty(), "a bare Map field must be flagged (6R3 §18 C3)")
    }

    @Test
    fun flags_a_bare_any_field() {
        val hits = EvidenceShapeScanRules.scanText("val payload: Any")
        assertTrue(hits.isNotEmpty(), "a bare Any field must be flagged (6R3 §18 C3)")
    }

    // ───────────────── RED: Tier 2 retired literal names (one test per retired symbol) ─────────────────

    @Test
    fun flags_evidence_quantity_maturity() {
        assertTrue(EvidenceShapeScanRules.scanText("val band: EvidenceQuantityMaturity").isNotEmpty())
    }

    @Test
    fun flags_freshness_status() {
        assertTrue(EvidenceShapeScanRules.scanText("val freshness: FreshnessStatus").isNotEmpty())
    }

    @Test
    fun flags_ordered_normalized_scores() {
        assertTrue(EvidenceShapeScanRules.scanText("val orderedNormalizedScores: List<Int>").isNotEmpty())
    }

    @Test
    fun flags_comparable_within_scale() {
        assertTrue(EvidenceShapeScanRules.scanText("COMPARABLE_WITHIN_SCALE").isNotEmpty())
    }

    @Test
    fun flags_capped_observation_count() {
        assertTrue(EvidenceShapeScanRules.scanText("val cappedObservationCount: Int").isNotEmpty())
    }

    @Test
    fun flags_observation_count_progress() {
        assertTrue(EvidenceShapeScanRules.scanText("val observationCountProgress: Int").isNotEmpty())
    }

    @Test
    fun flags_distinct_day_progress() {
        assertTrue(EvidenceShapeScanRules.scanText("val distinctDayProgress: Int").isNotEmpty())
    }

    @Test
    fun flags_rank_session_count() {
        assertTrue(EvidenceShapeScanRules.scanText("val rankSessionCount: Int").isNotEmpty())
    }

    @Test
    fun flags_accelerating_session_count() {
        assertTrue(EvidenceShapeScanRules.scanText("val acceleratingSessionCount: Int").isNotEmpty())
    }

    @Test
    fun flags_distinct_days() {
        assertTrue(EvidenceShapeScanRules.scanText("val distinctDays: Int").isNotEmpty())
    }

    @Test
    fun flags_rank_readiness() {
        assertTrue(EvidenceShapeScanRules.scanText("val readiness: RankReadiness").isNotEmpty())
    }

    @Test
    fun flags_rank_policy() {
        assertTrue(EvidenceShapeScanRules.scanText("object RankPolicy").isNotEmpty())
    }

    @Test
    fun flags_rank_block_reason() {
        assertTrue(EvidenceShapeScanRules.scanText("enum class RankBlockReason").isNotEmpty())
    }

    @Test
    fun flags_blocking_reasons() {
        assertTrue(EvidenceShapeScanRules.scanText("val blockingReasons: List<Int>").isNotEmpty())
    }

    // ───────────────── GREEN control: legitimate facts-only shapes are NEVER flagged ─────────────────

    @Test
    fun does_not_flag_raw_int_counts() {
        val hits = EvidenceShapeScanRules.scanText("val observationCount: Int, val distinctDayCount: Int")
        assertTrue(hits.isEmpty(), "raw Int counts are legitimate facts, never flagged: $hits")
    }

    @Test
    fun does_not_flag_a_list_of_exclusion_count() {
        val hits = EvidenceShapeScanRules.scanText("val exclusions: List<ExclusionCount>")
        assertTrue(hits.isEmpty(), "List<ExclusionCount> is a legitimate audit shape, never flagged: $hits")
    }

    @Test
    fun does_not_flag_nullable_span_ms() {
        val hits = EvidenceShapeScanRules.scanText("val spanMs: Long?")
        assertTrue(hits.isEmpty(), "spanMs is deliberately kept on ExerciseScaleEvidence, never flagged: $hits")
    }

    @Test
    fun does_not_flag_an_enum_state_field() {
        val hits = EvidenceShapeScanRules.scanText("val state: SessionEvidenceState")
        assertTrue(hits.isEmpty(), "a states-not-scores enum field is legitimate, never flagged: $hits")
    }

    @Test
    fun does_not_flag_linked_hash_map_or_empty_map_calls() {
        // "Map" is banned as a BARE word (6R3 §18 C3); "LinkedHashMap"/"emptyMap()" must not word-boundary-match it
        // (no boundary immediately before "Map" inside either token) — the extractor legitimately uses both.
        val hits = EvidenceShapeScanRules.scanText("val excluded = LinkedHashMap<ExclusionReason, Int>()")
        assertTrue(hits.isEmpty(), "LinkedHashMap must not trip the bare 'Map' ban: $hits")
        val emptyMapHits = EvidenceShapeScanRules.scanText("private val declared = emptyMap<String, Int>()")
        assertTrue(emptyMapHits.isEmpty(), "emptyMap() must not trip the bare 'Map' ban: $emptyMapHits")
    }

    // ───────────────────────── fileName is a cosmetic label on the offender string ─────────────────────────

    @Test
    fun a_flagged_offender_carries_the_supplied_file_name_label() {
        val hits = EvidenceShapeScanRules.scanText("val ratio: Double", fileName = "Fixture.kt")
        assertTrue(hits.single().startsWith("Fixture.kt"), "the offender label must carry the caller's file name")
    }
}
