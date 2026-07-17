package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SYSTEM-1 6R3 §18 C3 · The CANONICAL, EXHAUSTIVE golden of the evidence package's public shape — every DTO,
 * enum, sealed-interface variant, and thin carrier class declared in `core.session.cognitive.evidence` (20 types,
 * cross-checked against [CognitiveSessionEvidenceBoundaryTest]'s own `protectedTypeNames` consumer-sweep list),
 * PLUS the version constants of its 3 policy/function singleton objects. See
 * `docs/mindboost_velocidad_implementation_plan/EVIDENCE_PUBLIC_SHAPE_ALLOWLIST.md` for the field-by-field
 * source/purpose/inference rationale this golden's shape corresponds to.
 *
 * Complements — does NOT replace — [CognitiveSessionEvidenceAdversarialTest.assertFactsOnlyShape] (which only
 * pins [ExerciseScaleEvidence] and [CognitiveSessionEvidence], inline with its own 6-case adversarial suite; that
 * file is owned by a different wave and is left untouched here). THIS file is the canonical, exhaustive one: it
 * must FAIL if any public field/property is added, renamed, or retyped on ANY of the 20 types, or if a NEW enum
 * value or sealed-interface variant is introduced anywhere in the package.
 *
 * Enum entry lists are asserted as an ORDERED list (not just a set): production code sorts by `.ordinal`
 * ([CognitiveSessionEvidenceExtractor]'s `stateCountsOf`/`routeEligibilityCountsOf`/exclusion tallies), so
 * reordering existing entries — not only adding one — is itself a shape change this golden must catch.
 *
 * [sourcePropertyNames] filters two JVM-codegen artifacts that are NOT Kotlin-source properties before comparing:
 * `INSTANCE` (the singleton field Kotlin emits for every `object`/`data object`) and `Companion` (the static
 * reference every class with a `companion object` gets — [EvaluationAsOf], [SubjectScopedSessionBatch], and
 * [RtCriticalityRegistry] all declare one, purely for factory functions/constants, no state of their own). It
 * also strips any `$`-prefixed synthetic field a compiler plugin might inject (e.g. a Compose-compiler stability
 * marker) — an implementation artifact, never a source-level property this contract's shape is about.
 */
class EvidencePublicShapeGoldenTest {

    private fun Class<*>.sourcePropertyNames(): Set<String> =
        declaredFields
            .filterNot { java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .filterNot { it == "INSTANCE" || it == "Companion" || it.startsWith("$") }
            .toSet()

    // ───────────────────────── data class field-shape goldens ─────────────────────────

    @Test
    fun cognitiveSessionEvidence_shape_is_exactly_five_facts() {
        val fields = CognitiveSessionEvidence::class.java.sourcePropertyNames()
        val expected = setOf("blockId", "scaleGroups", "crossScaleComparability", "audit", "clockProvenance")
        assertEquals(expected, fields, "CognitiveSessionEvidence public shape changed: $fields")
    }

    @Test
    fun exerciseScaleEvidence_shape_is_exactly_four_facts() {
        val fields = ExerciseScaleEvidence::class.java.sourcePropertyNames()
        val expected = setOf("lineage", "observationCount", "distinctDayCount", "spanMs")
        assertEquals(expected, fields, "ExerciseScaleEvidence public shape changed: $fields")
    }

    @Test
    fun scoreScaleKey_shape_is_exactly_four_identity_fields() {
        val fields = ScoreScaleKey::class.java.sourcePropertyNames()
        val expected = setOf("blockId", "exerciseId", "exerciseVersion", "scoringVersion")
        assertEquals(expected, fields, "ScoreScaleKey public shape changed: $fields")
    }

    @Test
    fun measurementLineage_shape_is_exactly_three_fields() {
        val fields = MeasurementLineage::class.java.sourcePropertyNames()
        val expected = setOf("scaleKey", "comparability", "lineagePolicyVersion")
        assertEquals(expected, fields, "MeasurementLineage public shape changed: $fields")
    }

    @Test
    fun exclusionCount_shape_is_exactly_reason_and_count() {
        val fields = ExclusionCount::class.java.sourcePropertyNames()
        assertEquals(setOf("reason", "count"), fields, "ExclusionCount public shape changed: $fields")
    }

    @Test
    fun stateCount_shape_is_exactly_state_and_count() {
        val fields = StateCount::class.java.sourcePropertyNames()
        assertEquals(setOf("state", "count"), fields, "StateCount public shape changed: $fields")
    }

    @Test
    fun routeEligibilityCount_shape_is_exactly_eligibility_and_count() {
        val fields = RouteEligibilityCount::class.java.sourcePropertyNames()
        assertEquals(setOf("eligibility", "count"), fields, "RouteEligibilityCount public shape changed: $fields")
    }

    @Test
    fun policyVersionBundle_shape_is_exactly_five_version_strings() {
        val fields = PolicyVersionBundle::class.java.sourcePropertyNames()
        val expected = setOf(
            "evidenceRuleVersion", "lineagePolicyVersion", "evidenceQuantityPolicyVersion",
            "rankPolicyVersion", "freshnessPolicyVersion",
        )
        assertEquals(expected, fields, "PolicyVersionBundle public shape changed: $fields")
    }

    @Test
    fun sessionEvidenceAudit_shape_is_exactly_twelve_facts() {
        val fields = SessionEvidenceAudit::class.java.sourcePropertyNames()
        val expected = setOf(
            "rawInputCount", "forBlockCount", "uniqueIdentityCount", "duplicateCollapsedCount",
            "identityConflictRunCount", "includedUniqueCount", "excludedUniqueCount", "exclusions",
            "distinctScaleCount", "policyVersions", "stateCounts", "routeEligibilityCounts",
        )
        assertEquals(expected, fields, "SessionEvidenceAudit public shape changed: $fields")
    }

    @Test
    fun sessionEvidenceDisposition_shape_is_exactly_three_fields() {
        val fields = SessionEvidenceDisposition::class.java.sourcePropertyNames()
        val expected = setOf("state", "routeEligibility", "reasons")
        assertEquals(expected, fields, "SessionEvidenceDisposition public shape changed: $fields")
    }

    @Test
    fun rtCriticalityDeclaration_shape_is_exactly_three_fields() {
        val fields = RtCriticalityDeclaration::class.java.sourcePropertyNames()
        val expected = setOf("exerciseId", "exerciseVersion", "criticality")
        assertEquals(expected, fields, "RtCriticalityDeclaration public shape changed: $fields")
    }

    @Test
    fun evaluationAsOf_shape_is_exactly_two_fields() {
        val fields = EvaluationAsOf::class.java.sourcePropertyNames()
        assertEquals(setOf("epochMs", "provenance"), fields, "EvaluationAsOf public shape changed: $fields")
    }

    // ───────────────────────── thin carrier classes (not data class, still shape-pinned) ─────────────────────────

    @Test
    fun subjectScopedSessionBatch_shape_is_exactly_token_and_runs() {
        val fields = SubjectScopedSessionBatch::class.java.sourcePropertyNames()
        assertEquals(setOf("subjectToken", "runs"), fields, "SubjectScopedSessionBatch public shape changed: $fields")
    }

    @Test
    fun rtCriticalityRegistry_shape_is_exactly_declared_and_fallback() {
        val fields = RtCriticalityRegistry::class.java.sourcePropertyNames()
        assertEquals(setOf("declared", "fallback"), fields, "RtCriticalityRegistry public shape changed: $fields")
    }

    // ───────────────────────── sealed-interface variant inventory + per-variant shape ─────────────────────────

    @Test
    fun comparabilityVerdict_has_exactly_three_variants() {
        val variants = ComparabilityVerdict::class.java.declaredClasses.map { it.simpleName }.toSet()
        val expected = setOf("ComparabilityNotEstablished", "Incompatible", "Bridged")
        assertEquals(expected, variants, "ComparabilityVerdict variant inventory changed: $variants")
    }

    @Test
    fun comparabilityNotEstablished_carries_no_business_field() {
        val fields = ComparabilityVerdict.ComparabilityNotEstablished::class.java.sourcePropertyNames()
        assertEquals(emptySet(), fields, "ComparabilityNotEstablished must stay a bare marker: $fields")
    }

    @Test
    fun incompatible_carries_no_business_field() {
        val fields = ComparabilityVerdict.Incompatible::class.java.sourcePropertyNames()
        assertEquals(emptySet(), fields, "Incompatible must stay a bare marker: $fields")
    }

    @Test
    fun bridged_shape_is_exactly_one_bridge_version_field() {
        val fields = ComparabilityVerdict.Bridged::class.java.sourcePropertyNames()
        assertEquals(setOf("bridgeVersion"), fields, "Bridged public shape changed: $fields")
    }

    // ───────────────────────── enums: exact ORDERED entry lists (ordinal drives production sort order) ────────

    @Test
    fun exclusionReason_entries_are_exactly_this_ordered_list() {
        val expected = listOf(
            "NOT_FOR_BLOCK", "IDENTITY_CONFLICT", "QUARANTINED_INCONSISTENT_TIME", "FUTURE_TIMESTAMP",
            "NOT_COMPLETED", "DATA_QUALITY", "TIMING_COMPROMISED", "RT_CRITICALITY_UNKNOWN",
            "TIMEBASE_NON_AUTHORITATIVE", "NOT_VALID", "PRELIMINARY", "LOW_CONFIDENCE", "PURPOSE_EXCLUDED",
        )
        val actual = ExclusionReason.entries.map { it.name }
        assertEquals(expected, actual, "ExclusionReason entries/order changed: $actual")
    }

    @Test
    fun sessionEvidenceState_entries_are_exactly_this_ordered_list() {
        val expected = listOf("TRAINING_ONLY", "PROVISIONAL", "OFFICIAL_ELIGIBLE", "INVALIDATED")
        val actual = SessionEvidenceState.entries.map { it.name }
        assertEquals(expected, actual, "SessionEvidenceState entries/order changed: $actual")
    }

    @Test
    fun evidenceRouteEligibility_entries_are_exactly_this_ordered_list() {
        val expected = listOf("TRAINING_ONLY", "PROVISIONAL_PROFILE_ELIGIBLE", "PROFILE_ELIGIBLE", "RANK_ELIGIBLE")
        val actual = EvidenceRouteEligibility.entries.map { it.name }
        assertEquals(expected, actual, "EvidenceRouteEligibility entries/order changed: $actual")
    }

    @Test
    fun rtCriticality_entries_are_exactly_this_ordered_list() {
        val expected = listOf("RT_CRITICAL", "RT_NON_CRITICAL", "UNKNOWN")
        val actual = RtCriticality.entries.map { it.name }
        assertEquals(expected, actual, "RtCriticality entries/order changed: $actual")
    }

    @Test
    fun clockProvenance_entries_are_exactly_this_ordered_list() {
        val expected = listOf("AUTHORITATIVE_INJECTED", "BEST_EFFORT_MONOTONIC")
        val actual = ClockProvenance.entries.map { it.name }
        assertEquals(expected, actual, "ClockProvenance entries/order changed: $actual")
    }

    // ───────────────────────── version-constant singleton objects (pinned by VALUE, not reflection) ────────────
    // These 3 are pure policy/function holders, not DTOs — out of the "campo público de DTO/enum" mandate — but a
    // silent version bump is still worth catching, so they are pinned by direct constant VALUE instead.

    @Test
    fun lineagePolicy_version_is_pinned() {
        assertEquals("measurement-lineage-v1", LineagePolicy.VERSION, "LineagePolicy.VERSION drifted silently")
    }

    @Test
    fun sessionEvidenceStatePolicy_version_is_pinned() {
        assertEquals(
            "session-evidence-state-v2",
            SessionEvidenceStatePolicy.VERSION,
            "SessionEvidenceStatePolicy.VERSION drifted silently",
        )
    }

    @Test
    fun cognitiveSessionEvidenceExtractor_rule_version_is_pinned() {
        assertEquals(
            "cognitive-session-evidence-v2",
            CognitiveSessionEvidenceExtractor.RULE_VERSION,
            "CognitiveSessionEvidenceExtractor.RULE_VERSION drifted silently",
        )
    }

    @Test
    fun rtCriticalityRegistry_version_is_pinned() {
        assertEquals(
            "rt-criticality-registry-v2",
            RtCriticalityRegistry.VERSION,
            "RtCriticalityRegistry.VERSION drifted silently",
        )
    }
}
