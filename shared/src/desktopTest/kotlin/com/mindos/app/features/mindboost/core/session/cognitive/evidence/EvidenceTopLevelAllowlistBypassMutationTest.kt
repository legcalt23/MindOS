package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SYSTEM-1 6R3 §18 C3 (B2, kmp-architect W4 review) · Bypass-vector mutation suite for [EvidenceTopLevelScanRules]
 * — the fourth mechanical barrier, a POSITIVE allowlist of the evidence package's TOP-LEVEL declarations. Run
 * against IN-MEMORY synthetic snippets (never mutating the real evidence-package tree) PLUS one real-tree read
 * (never a write) confirming the CURRENT package trips nothing.
 *
 * Closes the two gap vectors [EvidenceShapeBypassMutationTest] already documents as "honest blind spots" of the
 * token/golden barriers:
 *  - `blind_spot_extension_property_computing_a_ratio_is_not_a_stored_field_at_all` — an in-package top-level
 *    extension property computing a scaled ratio, avoiding every banned token.
 *  - `blind_spot_renamed_nested_wrapper_type_carries_no_banned_token` — a brand-new, differently-named top-level
 *    wrapper type.
 *
 * [mutationSuiteActuallyFailsOnEachBypass] is the meta-test proving this suite is not a vacuous no-op: every
 * forbidden snippet trips >=1 offense AND every legitimate snippet (including an indented, non-top-level line)
 * trips zero, asserted together so a rule change that silently defeats either side surfaces as one failure.
 */
class EvidenceTopLevelAllowlistBypassMutationTest {

    // ───────────────────────── RED: the two gap vectors this barrier closes ─────────────────────────

    @Test
    fun catches_top_level_extension_property_computing_a_ratio_that_avoids_every_banned_token() {
        val hits = EvidenceTopLevelScanRules.scanText(
            "val CognitiveSessionEvidence.includedPercent: Int get() = " +
                "(audit.includedUniqueCount * 100) / audit.rawInputCount.coerceAtLeast(1)",
        )
        assertTrue(hits.isNotEmpty(), "an in-package top-level extension property must trip the allowlist: $hits")
    }

    @Test
    fun catches_brand_new_differently_named_top_level_wrapper_type() {
        val hits = EvidenceTopLevelScanRules.scanText("data class Magnitude(val raw: Int)")
        assertTrue(hits.isNotEmpty(), "a brand-new top-level type name must trip the allowlist: $hits")
    }

    // ───────────────────────── false-positive control: every CURRENTLY allowed name, synthetically ────────────

    @Test
    fun does_not_flag_any_of_the_currently_allowed_top_level_declarations() {
        EvidenceTopLevelScanRules.allowedDeclarations.forEach { name ->
            val hits = EvidenceTopLevelScanRules.scanText("class $name")
            assertTrue(hits.isEmpty(), "'$name' is on the allowlist and must not trip: $hits")
        }
    }

    @Test
    fun does_not_flag_an_indented_member_level_declaration_at_all() {
        // Column-0 anchoring: an indented line is a class/object MEMBER, never top-level, so it must not even be
        // extracted as a candidate name (regardless of whether that name happens to be on the allowlist).
        val hits = EvidenceTopLevelScanRules.scanText("    val notTopLevel: Double = 1.0")
        assertTrue(hits.isEmpty(), "an indented line must never be treated as a top-level declaration: $hits")
    }

    // ───────────────────────── false-positive control: the REAL, CURRENT evidence package tree ─────────────────

    @Test
    fun real_evidence_package_sources_trip_zero_top_level_allowlist_violations() {
        val offenders = evidenceSources().flatMap { file ->
            EvidenceTopLevelScanRules.scanText(file.readText()).map { "${file.name} -> $it" }
        }
        assertTrue(offenders.isEmpty(), "the current, real evidence package must trip zero violations: $offenders")
    }

    // ───────────────────────── meta-test: proves the suite is not a vacuous no-op ─────────────────────────

    @Test
    fun mutationSuiteActuallyFailsOnEachBypass() {
        val forbidden = listOf(
            "top-level extension property avoiding every banned token" to
                "val CognitiveSessionEvidence.includedPercent: Int get() = " +
                    "(audit.includedUniqueCount * 100) / audit.rawInputCount.coerceAtLeast(1)",
            "brand-new differently-named top-level wrapper type" to "data class Magnitude(val raw: Int)",
        )
        forbidden.forEach { (label, snippet) ->
            val hits = EvidenceTopLevelScanRules.scanText(snippet)
            assertTrue(hits.isNotEmpty(), "mutation suite must NOT be a no-op -- '$label' must trip >=1 offense")
        }

        val legitimate = listOf(
            "existing pinned data class" to "data class CognitiveSessionEvidence(val blockId: String)",
            "existing pinned sealed interface" to "sealed interface ComparabilityVerdict",
            "existing pinned internal singleton object" to "internal object SessionEvidenceGates",
            "existing pinned enum" to "enum class SessionEvidenceState { TRAINING_ONLY }",
            "indented member is not top-level at all" to "    val notTopLevel: Double = 1.0",
        )
        legitimate.forEach { (label, snippet) ->
            val hits = EvidenceTopLevelScanRules.scanText(snippet)
            assertTrue(hits.isEmpty(), "false-positive control -- '$label' must trip ZERO offenses, got: $hits")
        }
    }

    // Mirrors CognitiveSessionEvidenceBoundaryTest.evidenceSources()/locate() (same climbing strategy, same
    // worktree shape).
    private fun evidenceSources(): List<File> {
        val relative = "src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence"
        val dir = locate(relative)
        val files = dir?.walkTopDown()?.filter { it.isFile && it.extension == "kt" }?.toList() ?: emptyList()
        assertTrue(files.isNotEmpty(), "expected to locate evidence package sources from ${File(".").absolutePath}")
        return files
    }

    private fun locate(relative: String): File? {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: return null
            File(current, relative).takeIf { it.isDirectory }?.let { return it }
            File(current, "shared/$relative").takeIf { it.isDirectory }?.let { return it }
            base = current.parentFile
        }
        return null
    }

    private companion object {
        const val MAX_DEPTH = 8
    }
}
