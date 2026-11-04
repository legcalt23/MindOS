package com.mindos.app.features.mindboost.focus.domain.adaptive

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * FOC-P0-02 — the adaptive contract carries no flattering absence.
 *
 * The defect (`MB-EFFICACY-0005`): the only production call-site of [AdaptiveInput] fed the engine three
 * constants that READ as measurements — a fatigue sentinel meaning "no fatigue", an empty mastery map and an
 * `Int.MAX_VALUE` hysteresis counter — while none of the three was ever measured. The engine could therefore
 * `StepUp` on a user it knew nothing about, and its fatigue safety rules could never fire. The type-level
 * fix makes the three fields nullable and the engine `Hold(METRIC_UNAVAILABLE)` on `null`; THIS test is the
 * structural half: no production source under `focus/` may reintroduce the sentinel, the empty map or the
 * saturated counter at the call-site. Behaviour is pinned in `AdaptiveAbsenceHoldTest` (commonTest).
 *
 * Source scan lives in desktopTest (file IO; not Detekt-scanned), mirroring `AdequateCoverageInvariantTest`.
 * Comments are stripped first so a KDoc that DESCRIBES the retired sentinel does not count as a use.
 */
class AdaptiveContractAbsenceGuardTest {

    private val fatigueSentinel = Regex("\\bFatigueSignal\\.NONE\\b")
    private val saturatedHysteresis = Regex("blocksSinceChange\\s*=\\s*Int\\.MAX_VALUE")
    private val emptyMastery = Regex("perAxisMastery\\s*=\\s*emptyMap\\(\\)")

    /**
     * A `FatigueSignal` CONSTRUCTED at the argument position is the sentinel with its name removed: only a
     * producer may build one, and a call-site passes the producer's variable (or `null`). Declared ceiling: a
     * fabricated signal bound to a local first, or a constant defined elsewhere, is NOT caught by text; the
     * compile-time guarantee is only that `null` is expressible and held.
     */
    private val inlineFatigue = Regex("fatigue\\s*=\\s*FatigueSignal\\(")

    @Test
    fun noFocusProductionSourceReintroducesAFlatteringAbsence() {
        val files = focusProductionFiles()
        assertTrue(files.size >= MIN_FILES, "anti-vacuity: expected >= $MIN_FILES focus files, saw ${files.size}")

        val violations = files.flatMap { file ->
            val code = codeWithoutComments(file)
            listOf(fatigueSentinel, saturatedHysteresis, emptyMastery, inlineFatigue)
                .filter { it.containsMatchIn(code) }
                .map { "${file.name}: ${it.pattern}" }
        }
        assertTrue(violations.isEmpty(), "flattering absence back in the adaptive contract:\n" + violations.joinToString("\n"))
    }

    /**
     * The OTHER half of the same clause, and the one the patterns above cannot see: the three session signals
     * must carry no DEFAULT. A `= null` is fail-closed in its own right (the engine still holds), so this is
     * not the sentinel defect returning — but it is the vector `MB-FOCUS-0071` names: a default lets a future
     * call-site that DOES produce a signal omit it silently, and no per-file text scan can tell the difference
     * between "not passed" and "not measured". The declaration is the only place that fact lives.
     */
    @Test
    fun theThreeSessionSignalsCarryNoDefault() {
        val block = adaptiveInputBlock()
        val absent = SESSION_SIGNALS.filter { declarationOf(block, it) == null }
        assertTrue(absent.isEmpty(), "anti-vacuity: AdaptiveInput no longer declares $absent — move this guard")

        val optional = SESSION_SIGNALS.filter { declarationOf(block, it)?.contains('=') == true }
        assertTrue(
            optional.isEmpty(),
            "a default re-fabricates the absence at every call-site that forgets the signal, and no per-file " +
                "text guard can see it (MB-FOCUS-0071): $optional carry one",
        )
        assertTrue(
            declarationOf(block, "config")?.contains('=') == true,
            "discriminating half: config's default is legitimate, so the guard must not ban every '='",
        )
    }

    /** Anti-vacuity: the scan really covers the one production call-site, or a green here would prove nothing. */
    @Test
    fun theScanReachesTheHolderCallSite() {
        val holder = focusProductionFiles().single { it.name == "FocusSessionStateHolder.kt" }
        assertTrue(
            codeWithoutComments(holder).contains("AdaptiveInput("),
            "FocusSessionStateHolder.kt no longer constructs AdaptiveInput — move this guard to the new call-site",
        )
    }

    /** Discriminant halves: the patterns see the sentinel they exist for and stay blind to Memory's own type. */
    @Test
    fun thePatternsAreDiscriminant() {
        assertTrue(fatigueSentinel.containsMatchIn("fatigue = FatigueSignal.NONE,"))
        assertFalse(
            fatigueSentinel.containsMatchIn("fatigue = MemoryFatigueSignal.NONE,"),
            "Memory's adaptive contract is out of this PR's scope and must not trip the focus guard",
        )
        assertTrue(saturatedHysteresis.containsMatchIn("blocksSinceChange = Int.MAX_VALUE,"))
        assertTrue(emptyMastery.containsMatchIn("perAxisMastery = emptyMap(),"))
        assertTrue(inlineFatigue.containsMatchIn("fatigue = FatigueSignal(FatigueLevel.LOW, emerging = false, sustained = false),"))
        assertFalse(inlineFatigue.containsMatchIn("fatigue = fatigueProducer.signal,"), "a producer's variable is legitimate")
        assertFalse(inlineFatigue.containsMatchIn("val signal = FatigueSignal(level, emerging, sustained)"), "a producer may construct")
        assertEquals(0, fatigueSentinel.findAll("/* FatigueSignal.NONE */".let { codeWithoutComments(it) }).count())

        // Both directions for the declaration reader: a nullable type with no initialiser is mandatory, the
        // same name with `= null` is not, and a legitimate default is still seen as one.
        assertFalse(
            declarationOf("    val fatigue: FatigueSignal?,", "fatigue")!!.contains('='),
            "a nullable type without an initialiser is a MANDATORY parameter",
        )
        assertTrue(declarationOf("    val fatigue: FatigueSignal? = null,", "fatigue")!!.contains('='))
        assertTrue(declarationOf("    val config: AdaptiveConfig = AdaptiveConfig(),", "config")!!.contains('='))
        assertEquals(null, declarationOf("    val other: Int,", "fatigue"), "an absent parameter reads as null")
    }

    // --- source scan helpers (mirror AdequateCoverageInvariantTest) ---

    /** The parameter list of `AdaptiveInput`, comments stripped, without its closing parenthesis. */
    private fun adaptiveInputBlock(): String {
        val source = codeWithoutComments(focusProductionFiles().single { it.name == "AdaptiveModels.kt" })
        val start = source.indexOf("data class AdaptiveInput(")
        assertTrue(start >= 0, "AdaptiveInput left AdaptiveModels.kt — move this guard to its new home")
        val end = source.indexOf("\n)", start)
        assertTrue(end > start, "the AdaptiveInput parameter list is not closed: the extractor read nothing")
        return source.substring(start, end)
    }

    /** Whatever follows `val <name>:` on its declaration line, or `null` when the parameter is absent. */
    private fun declarationOf(block: String, name: String): String? =
        Regex("^\\s*val\\s+$name\\s*:(.*)$", RegexOption.MULTILINE).find(block)?.groupValues?.get(1)

    private fun focusProductionFiles(): List<File> =
        findDir("src/commonMain/kotlin/com/mindos/app/features/mindboost/focus")
            .walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun findDir(relative: String): File {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: error("focus source dir not found")
            File(current, relative).let { if (it.exists()) return it }
            File(current, "shared/$relative").let { if (it.exists()) return it }
            base = current.parentFile
        }
        error("focus source dir not found from ${File(".").absolutePath}")
    }

    private fun codeWithoutComments(file: File): String = codeWithoutComments(file.readText())

    private fun codeWithoutComments(source: String): String {
        val noBlock = source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        return noBlock.lineSequence().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.take(idx) else line
        }
    }

    private companion object {
        const val MAX_DEPTH = 8
        const val MIN_FILES = 60
        val SESSION_SIGNALS = listOf("perAxisMastery", "fatigue", "blocksSinceChange")
    }
}
