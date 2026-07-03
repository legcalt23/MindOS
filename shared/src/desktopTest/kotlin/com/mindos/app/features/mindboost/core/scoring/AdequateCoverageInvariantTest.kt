package com.mindos.app.features.mindboost.core.scoring

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * INFRA-03 honesty invariant (Fase 0): **`DetectorCoverage.ADEQUATE`/`PARTIAL` ⇒ a real, tested
 * anti-gaming control is executed.** A "designed but not tested" — or worse, absent — detector must
 * never be claimed as coverage (a fabricated anti-gaming claim would let gameable data update the
 * profile while the UI/state machine treats it as gaming-protected).
 *
 * This is a source-scanning boundary test (Detekt does not scan desktopTest, so file IO is fine), in the
 * spirit of `MetricIntegrityTest`. It walks the production MindBoost tree, strips comments so KDoc
 * mentions don't count, and asserts that every CODE occurrence of `DetectorCoverage.ADEQUATE`/`PARTIAL`
 * lives in a registered scorer that actually INVOKES its tested control. A new scorer that hardcodes
 * `ADEQUATE` without a backing control (or stops invoking it) breaks the build.
 *
 * The control's "it actually fires" proof lives in each control's own synthetic-adversary test
 * (`MemorySpanAntiGamingTest`, `AssociativePairsScorerTest` select-all→INVALIDATED,
 * `DeferredRecallScorerTest` select-all→INVALIDATED, `ReversibilityScorerTest` constant→INVALIDATED) and
 * the per-scorer `*ScoreTest`s; THIS test guarantees the structural link (no ADEQUATE without a control).
 * Adding an entry to [DETECTOR_BACKED] therefore REQUIRES adding that control's adversary test.
 */
class AdequateCoverageInvariantTest {

    /**
     * Production files allowed to claim `ADEQUATE`/`PARTIAL` coverage → the tested anti-gaming control
     * each MUST invoke. For the discrimination-gate exercises the control IS the scorer's gate
     * (select-all/constant → INVALIDATED); for EX1 it is the dedicated [MemorySpanAntiGaming] detector.
     */
    private val detectorBacked = mapOf(
        "MemorySpanScore.kt" to "MemorySpanAntiGaming",
        "MemoryPairsScore.kt" to "AssociativePairsScorer",
        "MemoryDeferredScore.kt" to "DeferredRecallScorer",
        "ReversibilityScore.kt" to "ReversibilityScorer",
        // SpeedAntiGaming DECLARES its own coverage (PARTIAL, never ADEQUATE) and invokes its tested
        // detectors; its red-team is SpeedAntiGamingTest. SpeedProfileSource consults SpeedAntiGaming.COVERAGE
        // (it holds no DetectorCoverage literal itself, so it is not a claiming site).
        "SpeedAntiGaming.kt" to "RhythmTappingDetector",
    )

    @Test
    fun adequateCoverageOnlyInScorersThatInvokeATestedControl() {
        val violations = mutableListOf<String>()
        for (file in claimingFiles()) {
            val control = detectorBacked[file.name]
            when {
                control == null ->
                    violations += "${file.name} claims DetectorCoverage.ADEQUATE/PARTIAL but is not a " +
                        "registered detector-backed scorer (anti-gaming only counts if a tested control runs)"
                !codeWithoutComments(file).contains(control) ->
                    violations += "${file.name} claims ADEQUATE/PARTIAL coverage without invoking its " +
                        "tested control $control"
            }
        }
        val message = "ADEQUATE-coverage invariant violated (ADEQUATE => real detector executed):\n" +
            violations.joinToString("\n")
        assertTrue(violations.isEmpty(), message)
    }

    @Test
    fun detectorBackedRegistryHasNoStaleEntries() {
        val claimingNow = claimingFiles().map { it.name }.toSet()
        for (name in detectorBacked.keys) {
            assertTrue(
                name in claimingNow,
                "Stale registry entry: $name no longer claims DetectorCoverage.ADEQUATE/PARTIAL in code",
            )
        }
    }

    @Test
    fun adequateForCriticalityRequiresFullCoverageWhenRtCritical() {
        assertTrue(DetectorCoverage.ADEQUATE.adequateFor(TimingCriticality.RT_CRITICAL))
        assertFalse(DetectorCoverage.PARTIAL.adequateFor(TimingCriticality.RT_CRITICAL))
        assertTrue(DetectorCoverage.PARTIAL.adequateFor(TimingCriticality.RT_SECONDARY))
        assertFalse(DetectorCoverage.NONE.adequateFor(TimingCriticality.RT_IRRELEVANT))
    }

    // --- source scan helpers (mirror ArenaSourceLocator; desktopTest is not Detekt-scanned) ---

    private fun claimingFiles(): List<File> = mindBoostKotlinFiles().filter { file ->
        val code = codeWithoutComments(file)
        code.contains("DetectorCoverage.ADEQUATE") || code.contains("DetectorCoverage.PARTIAL")
    }

    private fun mindBoostKotlinFiles(): List<File> =
        findDir("src/commonMain/kotlin/com/mindos/app/features/mindboost")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun findDir(relative: String): File {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: error("MindBoost source dir not found")
            File(current, relative).let { if (it.exists()) return it }
            File(current, "shared/$relative").let { if (it.exists()) return it }
            base = current.parentFile
        }
        error("MindBoost source dir not found from ${File(".").absolutePath}")
    }

    /** Source with block + line comments removed, so doc-comment mentions don't fool the token scan. */
    private fun codeWithoutComments(file: File): String {
        val noBlock = file.readText().replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        return noBlock.lineSequence().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.take(idx) else line
        }
    }

    private companion object {
        const val MAX_DEPTH = 8
    }
}
