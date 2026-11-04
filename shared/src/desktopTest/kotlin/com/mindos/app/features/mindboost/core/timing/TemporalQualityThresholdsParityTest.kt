package com.mindos.app.features.mindboost.core.timing

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S5 · Paso 6 — the physical protocol measures against GO/NO-GO numbers, and those numbers must be the
 * ones production actually classifies with. If `S5_PASO6_PHYSICAL_PROTOCOL.md` says "drops <= 5%" while
 * [TemporalQualityThresholds] says something else, a field campaign burns real devices measuring against
 * an instrument the product does not use — and NOTHING fails visibly. This pins the two together.
 *
 * The version is pinned too: these numbers belong to `tqt-1`, so a version bump forces a protocol review,
 * which is exactly what changing a classifying threshold should force.
 */
class TemporalQualityThresholdsParityTest {

    @Test
    fun theProtocolPinsTheSameNumbersTheCodeClassifiesWith() {
        val documented = parseParityBlock()

        // Anti-vacuity: a regex that matched nothing, or a block that quietly lost a row, would make the
        // comparison below pass over an empty map. The key SET is asserted, not just the values.
        assertEquals(EXPECTED_KEYS, documented.keys, "the parity block must pin exactly the classifying config")

        val mismatches = mismatchesAgainstCode(documented)
        val message = "protocol and code disagree on: $mismatches"
        assertTrue(mismatches.isEmpty(), message)
    }

    @Test
    fun theComparisonActuallyCompares_discriminantHalf() {
        // Without this, "no mismatches" could mean the comparison is a no-op. Perturb one documented value
        // and the same function must report it.
        val mutated = parseParityBlock().toMutableMap()
        mutated[MUTATED_KEY] = "0.07"

        val mismatches = mismatchesAgainstCode(mutated)
        assertTrue(mismatches.any { it.startsWith(MUTATED_KEY) }, "a wrong documented value must be caught: $mismatches")
    }

    @Test
    fun aMissingVersionIsAMismatch_notASilentPass() {
        val withoutVersion = parseParityBlock() - "version"
        assertTrue(mismatchesAgainstCode(withoutVersion).any { it.startsWith("version") }, "version must be pinned")
    }

    private fun mismatchesAgainstCode(documented: Map<String, String>): List<String> {
        val code = TemporalQualityThresholds.DEFAULT
        val expectedNumbers = mapOf(
            "minFramesWindow" to code.minFramesWindow.toDouble(),
            "minFramesSession" to code.minFramesSession.toDouble(),
            "windowDropCleanMax" to code.windowDropCleanMax,
            "windowDropExcludeMin" to code.windowDropExcludeMin,
            "sessionExcludedMax" to code.sessionExcludedMax,
            "cadenceMatchTolerance" to code.cadenceMatchTolerance,
        )
        val mismatches = mutableListOf<String>()
        if (documented["version"] != code.version) {
            mismatches += "version: doc=${documented["version"]} code=${code.version}"
        }
        for ((key, expected) in expectedNumbers) {
            val actual = documented[key]?.toDoubleOrNull()
            if (actual == null || abs(actual - expected) > TOLERANCE) {
                mismatches += "$key: doc=${documented[key]} code=$expected"
            }
        }
        return mismatches
    }

    private fun parseParityBlock(): Map<String, String> {
        val text = locate(PROTOCOL_DOC).readText()
        val start = text.indexOf(BLOCK_START)
        val end = text.indexOf(BLOCK_END)
        assertTrue(start >= 0 && end > start, "the parity block sentinels are missing from $PROTOCOL_DOC")
        return text.substring(start + BLOCK_START.length, end)
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("|") && !it.contains("---") }
            .map { row -> row.trim('|').split("|").map { it.trim() } }
            .filter { it.size == 2 && it[0] != "key" }
            .associate { it[0] to it[1] }
    }

    private fun locate(relative: String): File {
        var base: File? = File(".").absoluteFile
        repeat(MAX_LOOKUP_DEPTH) {
            val current = base ?: error("no parent")
            File(current, relative).takeIf { it.exists() }?.let { return it }
            base = current.parentFile
        }
        error("expected to locate $relative from ${File(".").absolutePath}")
    }

    private companion object {
        const val PROTOCOL_DOC = "docs/engines/focus_v5/S5_PASO6_PHYSICAL_PROTOCOL.md"
        const val BLOCK_START = "<!-- TQT_PARITY_BLOCK_START -->"
        const val BLOCK_END = "<!-- TQT_PARITY_BLOCK_END -->"
        const val MUTATED_KEY = "windowDropCleanMax"
        const val TOLERANCE = 1e-9
        const val MAX_LOOKUP_DEPTH = 8

        val EXPECTED_KEYS = setOf(
            "version",
            "minFramesWindow",
            "minFramesSession",
            "windowDropCleanMax",
            "windowDropExcludeMin",
            "sessionExcludedMax",
            "cadenceMatchTolerance",
        )
    }
}
