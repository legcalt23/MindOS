package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SYSTEM-1 6R3 §18 C3 (task<->rules parity) · [EvidenceShapeScanRules] is a DELIBERATE, independent copy of the
 * Tier-1/Tier-2 literal lists the Gradle `evidenceBoundaryScanner` task (`shared/build.gradle.kts`) applies over
 * the REAL `core.session.cognitive.evidence` tree — a Gradle Kotlin-DSL script and this JVM desktopTest
 * compilation unit are different compilation units and cannot share one source file (see
 * [EvidenceShapeScanRules]'s own KDoc). A deliberate copy can still DRIFT silently (someone edits one list and
 * forgets the other, or edits both but in a different order) — this test reads `shared/build.gradle.kts` as TEXT
 * (permitted file IO in desktopTest, same technique [CognitiveSessionEvidenceBoundaryTest] already uses over the
 * evidence package's own sources) and regex-extracts its literal Tier-1/Tier-2 lists, then asserts they are
 * IDENTICAL — same entries, SAME ORDER — to [EvidenceShapeScanRules]'s.
 *
 * Fragility stance: the extraction is a plain-text regex over a Kotlin DSL script, not a real parser — if the
 * Gradle task's literal-list shape ever changes enough to defeat it (a multi-line string, a `+` concatenation, a
 * helper function building the list, etc.), this test FAILS LOUDLY with an explicit "update this test's regex"
 * message. It must never silently degrade to comparing against an empty list and reporting a false "parity ok".
 */
class EvidenceScannerParityTest {

    @Test
    fun tier1_banned_type_tokens_match_the_gradle_task_verbatim() {
        val extracted = extractListOfStringLiterals(buildGradleKtsText(), "val tier1BannedTypeTokens = listOf")
        assertEquals(
            EvidenceShapeScanRules.tier1BannedTypeTokens,
            extracted,
            "Tier-1 banned type tokens drifted between shared/build.gradle.kts and EvidenceShapeScanRules " +
                "(gradle=$extracted, test-side=${EvidenceShapeScanRules.tier1BannedTypeTokens})",
        )
    }

    @Test
    fun tier2_retired_evaluative_literals_match_the_gradle_task_verbatim() {
        val extracted = extractListOfStringLiterals(
            buildGradleKtsText(),
            "val tier2RetiredEvaluativeLiterals = listOf",
        )
        assertEquals(
            EvidenceShapeScanRules.tier2RetiredEvaluativeLiterals,
            extracted,
            "Tier-2 retired literals drifted between shared/build.gradle.kts and EvidenceShapeScanRules " +
                "(gradle=$extracted, test-side=${EvidenceShapeScanRules.tier2RetiredEvaluativeLiterals})",
        )
    }

    /**
     * Extract the string literals inside the FIRST `<marker>(...)` call in [source]. Fails loudly (never returns
     * an empty list silently) if [marker] does not appear EXACTLY once, or its parenthesized body contains no
     * string literal at all — both are signs the Gradle DSL's shape changed and this regex must be updated before
     * the parity result can be trusted.
     */
    private fun extractListOfStringLiterals(source: String, marker: String): List<String> {
        val occurrences = Regex(Regex.escape(marker)).findAll(source).count()
        val occurrenceMsg = "EvidenceScannerParityTest expected exactly ONE occurrence of `$marker` in " +
            "shared/build.gradle.kts, found $occurrences -- the Gradle task's shape changed; update this test's " +
            "extraction before trusting the parity result."
        check(occurrences == 1) { occurrenceMsg }
        val body = Regex(Regex.escape(marker) + "\\s*\\(([\\s\\S]*?)\\)").find(source)?.groupValues?.get(1)
        val locateMsg = "EvidenceScannerParityTest could not locate a `$marker(...)` call in " +
            "shared/build.gradle.kts -- the Gradle task's Kotlin DSL literal-list shape changed; update this " +
            "test's regex before trusting the parity result."
        checkNotNull(body) { locateMsg }
        val literals = Regex("\"([^\"]*)\"").findAll(body).map { it.groupValues[1] }.toList()
        val emptyMsg = "EvidenceScannerParityTest parsed `$marker(...)` but found ZERO string literals inside " +
            "it -- the Gradle task's list shape changed; update this test's regex before trusting the parity result."
        check(literals.isNotEmpty()) { emptyMsg }
        return literals
    }

    private fun buildGradleKtsText(): String {
        val file = locate("build.gradle.kts")
        val notFoundMsg = "EvidenceScannerParityTest could not locate shared/build.gradle.kts from " +
            File(".").absoluteFile.path
        checkNotNull(file) { notFoundMsg }
        return file.readText()
    }

    // Mirrors CognitiveSessionEvidenceBoundaryTest.locate() (same climbing strategy over the same worktree shape),
    // adapted for a single file instead of a directory.
    private fun locate(relative: String): File? {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: return null
            File(current, relative).takeIf { it.isFile }?.let { return it }
            File(current, "shared/$relative").takeIf { it.isFile }?.let { return it }
            base = current.parentFile
        }
        return null
    }

    private companion object {
        const val MAX_DEPTH = 8
    }
}
