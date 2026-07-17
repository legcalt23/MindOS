package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 §18 C3 · Exhaustive, AUTOMATED bypass-vector mutation suite for [EvidenceShapeScanRules], run
 * against an IN-MEMORY synthetic snippet (never mutating the real evidence-package tree). This suite is the
 * permanent SUBSTITUTE for the CEO's one-time manual §13 procedure ("temporarily add `val staleness: Double` to
 * ExerciseScaleEvidence.kt, watch `:shared:evidenceBoundaryScanner` + `:shared:desktopTest` go RED, then revert,
 * never commit the temporary edit"). That manual step is RETIRED as a gate requirement — see
 * `docs/mindboost_velocidad_implementation_plan/EVIDENCE_PUBLIC_SHAPE_ALLOWLIST.md` ("Manual procedure retired").
 *
 * TWO complementary sections:
 *  - RED: one test per bypass vector [EvidenceShapeScanRules.scanText] MUST catch (nullable Double/Float, both
 *    inside a collection, `DoubleArray`/`FloatArray`, `Duration`, a `typealias` aliasing a banned type, a bare
 *    `Map`/`Any`).
 *  - HONEST BLIND SPOTS: one test per vector the TEXT/TOKEN scanner structurally CANNOT catch — a scaled Int/Long
 *    magnitude, a String/enum encoding a magnitude/band, a `.toDouble()`/`.toInt()` call embedded inside a longer
 *    identifier (no word-boundary match), an extension property computing a ratio instead of a stored field, and
 *    a brand-new, differently-named wrapper type. Each blind-spot test asserts the EMPTY scan result so the gap is
 *    literal and falsifiable, not merely asserted in prose. For a bypass that adds a NEW property to one of the
 *    20 EXISTING pinned types, [EvidencePublicShapeGoldenTest] is the barrier that actually catches it — the two
 *    suites are complementary layers, never a substitute for one another. A genuinely NEW top-level type, or an
 *    extension property/top-level function declared INSIDE the evidence package that never touches a banned token
 *    and is never itself a stored field on a pinned type, is NOT caught by either barrier today — see the
 *    allowlist doc's "Honest residual gaps" section; this is reported to the lead, not silently patched here
 *    (production is frozen for this pass).
 *
 * [mutationSuiteActuallyFailsOnEachBypass] is the meta-test proving this suite is not a vacuous no-op: every
 * "forbidden" snippet trips >=1 offense AND every "legitimate" (KEEP-AS-FACT) snippet trips zero offenses (the
 * false-positive control), asserted together so a rule change that silently defeats either side surfaces as one
 * failure.
 */
class EvidenceShapeBypassMutationTest {

    // ───────────────────────── RED: bypass vectors the scanner DOES catch ─────────────────────────

    @Test
    fun catches_nullable_double() {
        assertTrue(EvidenceShapeScanRules.scanText("val ratio: Double?").isNotEmpty())
    }

    @Test
    fun catches_nullable_float() {
        assertTrue(EvidenceShapeScanRules.scanText("val ratio: Float?").isNotEmpty())
    }

    @Test
    fun catches_double_inside_a_list() {
        assertTrue(EvidenceShapeScanRules.scanText("val scores: List<Double>").isNotEmpty())
    }

    @Test
    fun catches_float_inside_a_map_value() {
        assertTrue(EvidenceShapeScanRules.scanText("val weights: Map<String, Float>").isNotEmpty())
    }

    @Test
    fun catches_double_array() {
        assertTrue(EvidenceShapeScanRules.scanText("val a: DoubleArray").isNotEmpty())
    }

    @Test
    fun catches_float_array() {
        assertTrue(EvidenceShapeScanRules.scanText("val b: FloatArray").isNotEmpty())
    }

    @Test
    fun catches_duration_field() {
        assertTrue(EvidenceShapeScanRules.scanText("val span: Duration").isNotEmpty())
    }

    @Test
    fun catches_typealias_aliasing_a_banned_type() {
        assertTrue(EvidenceShapeScanRules.scanText("typealias ScoreProxy = Double").isNotEmpty())
    }

    @Test
    fun catches_bare_map_field() {
        assertTrue(EvidenceShapeScanRules.scanText("val declared: Map<String, Int>").isNotEmpty())
    }

    @Test
    fun catches_bare_any_field() {
        assertTrue(EvidenceShapeScanRules.scanText("val payload: Any").isNotEmpty())
    }

    // ───────────────────────── HONEST BLIND SPOTS: the scanner CANNOT catch these ─────────────────────────

    @Test
    fun blind_spot_scaled_long_magnitude_is_not_caught_by_the_token_scanner() {
        // progressMillis is a scaled/evaluative-shaped magnitude wearing a Long costume -- no banned token
        // appears. Barrier: none of the 3 mechanical checks catch a NEW Long-typed field on a NEW type; on an
        // EXISTING pinned type, adding this field DOES break EvidencePublicShapeGoldenTest's exact field-set.
        val hits = EvidenceShapeScanRules.scanText("val progressMillis: Long")
        assertTrue(hits.isEmpty(), "documents the gap: a scaled Long magnitude carries no banned token: $hits")
    }

    @Test
    fun blind_spot_scaled_int_percent_is_not_caught_by_the_token_scanner() {
        val hits = EvidenceShapeScanRules.scanText("val progressPercent: Int")
        assertTrue(hits.isEmpty(), "documents the gap: a scaled Int percent carries no banned token: $hits")
    }

    @Test
    fun blind_spot_string_encoded_magnitude_is_not_caught_by_the_token_scanner() {
        // A String is not itself an evaluative type token -- "FRESH"/"50%"/"HIGH" could smuggle a scaled/banded
        // verdict through a type the scanner has no reason to flag.
        val hits = EvidenceShapeScanRules.scanText("val staleBand: String")
        assertTrue(hits.isEmpty(), "documents the gap: a String-encoded magnitude carries no banned token: $hits")
    }

    @Test
    fun blind_spot_toDouble_call_embedded_in_a_longer_identifier_is_not_caught() {
        // Regex("\\bDouble\\b") requires a word boundary BEFORE "Double"; inside "toDouble" the preceding
        // character "o" is a word character, so no boundary exists there and it never matches -- a getter body
        // computing `(x.toDouble() / y).toInt()` is invisible to the token scanner even though it performs a
        // real floating-point ratio internally before narrowing back to Int.
        val hits = EvidenceShapeScanRules.scanText(
            "val progressPercent: Int get() = (observationCount.toDouble() / threshold * 100).toInt()",
        )
        assertTrue(hits.isEmpty(), "documents the gap: .toDouble()/.toInt() embedded calls are invisible: $hits")
    }

    @Test
    fun blind_spot_extension_property_computing_a_ratio_is_not_a_stored_field_at_all() {
        // An extension property is not a constructor parameter of the receiver type, so it never appears in
        // EvidencePublicShapeGoldenTest's `declaredFields` reflection either -- and if it also avoids every
        // banned token (as above), the text scanner misses it too. Genuinely uncaught by BOTH mechanical
        // barriers when declared INSIDE the evidence package (a cross-package extension referencing
        // CognitiveSessionEvidence by name is separately blocked by the already-landed consumer-sweep scanner,
        // which bans referencing any protected type name at all outside the package) -- flagged as a residual gap
        // in the allowlist doc, not fixed here (production is frozen for this pass).
        val hits = EvidenceShapeScanRules.scanText(
            "val CognitiveSessionEvidence.includedPercent: Int get() = " +
                "(audit.includedUniqueCount * 100) / audit.rawInputCount.coerceAtLeast(1)",
        )
        assertTrue(hits.isEmpty(), "documents the gap: an Int-returning extension property is invisible: $hits")
    }

    @Test
    fun blind_spot_renamed_nested_wrapper_type_carries_no_banned_token() {
        // A brand-new type name (never on Tier-2's retired-literal list, never matching a Tier-1 type token)
        // wrapping a scaled Int is invisible to the text scanner; it is ALSO invisible to the golden test unless
        // a reviewer notices the new type and adds it to the golden's inventory -- no mechanical force does that
        // automatically. Documented as a residual gap of the "enumerate & pin" golden strategy, not a false claim
        // that the golden is exhaustive against unknown future types.
        val hits = EvidenceShapeScanRules.scanText("data class Magnitude(val raw: Int)")
        assertTrue(hits.isEmpty(), "documents the gap: a wholly new wrapper type name is invisible: $hits")
    }

    // ───────────────────────── meta-test: proves the suite is not a vacuous no-op ─────────────────────────

    @Test
    fun mutationSuiteActuallyFailsOnEachBypass() {
        val forbidden = listOf(
            "nullable Double" to "val ratio: Double?",
            "nullable Float" to "val ratio: Float?",
            "List<Double>" to "val scores: List<Double>",
            "Map<String, Float>" to "val weights: Map<String, Float>",
            "DoubleArray" to "val a: DoubleArray",
            "FloatArray" to "val b: FloatArray",
            "Duration" to "val span: Duration",
            "typealias to Double" to "typealias ScoreProxy = Double",
            "bare Map" to "val declared: Map<String, Int>",
            "bare Any" to "val payload: Any",
        )
        forbidden.forEach { (label, snippet) ->
            val hits = EvidenceShapeScanRules.scanText(snippet)
            assertTrue(hits.isNotEmpty(), "mutation suite must NOT be a no-op -- '$label' must trip >=1 offense")
        }

        val legitimate = listOf(
            "raw Int count" to "val observationCount: Int",
            "List<ExclusionCount> audit shape" to "val exclusions: List<ExclusionCount>",
            "nullable spanMs" to "val spanMs: Long?",
            "enum-state field" to "val state: SessionEvidenceState",
            "List<RouteEligibilityCount> audit shape" to "val routeEligibilityCounts: List<RouteEligibilityCount>",
        )
        legitimate.forEach { (label, snippet) ->
            val hits = EvidenceShapeScanRules.scanText(snippet)
            assertTrue(hits.isEmpty(), "false-positive control -- '$label' must trip ZERO offenses, got: $hits")
        }
    }
}
