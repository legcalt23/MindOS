package com.mindos.app.features.mindboost.focus.presentation.ui

import com.mindos.app.features.mindboost.core.a11y.A11yTargets
import com.mindos.app.features.mindboost.core.a11y.platformMinimumTarget
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins that no control in the Focus V1 lane — the one the MindBoost hub actually mounts — sizes its own hit
 * area with a literal.
 *
 * Three did, and each was a way OUT of something: the two abort buttons that end a running block (36dp) and
 * the home back button that leaves the Focus track (40dp). All three sat below every platform minimum, on the
 * controls a user reaches for when a timed task is going badly. They now go through `FocusGlyphButton`, which
 * takes its hit area from [platformMinimumTarget] — 48dp on Android, 44pt on iOS, rather than one compromise
 * number — while keeping the visible disc at whatever diameter it already had.
 *
 * The guard is textual, and a text guard is normally fail-open against Gradle's up-to-date checks (a comment
 * edit changes no bytecode, so the test does not re-run). It is sound HERE because its subject is production
 * Kotlin: reintroducing a hand-sized clickable is a source change, which changes `compileKotlinDesktop`'s
 * output and so invalidates this test's classpath. It would NOT be sound over a `.md` or over a KDoc.
 */
class FocusControlTargetTest {

    @Test
    fun noFocusControlSizesItsOwnHitAreaWithALiteral() {
        val files = focusUiSources()
        // Anti-vacuity: an extractor that reads nothing reports "all clean" about everything.
        assertTrue(files.size >= MIN_FILES, "expected at least $MIN_FILES Focus UI sources, found ${files.size}")

        val chainsWithClick = files.flatMap { file -> modifierChains(file).map { file to it } }
            .filter { (_, chain) -> chain.contains(CLICKABLE) }
        assertTrue(
            chainsWithClick.isNotEmpty(),
            "no clickable modifier chain found at all: the chain splitter is broken, not the lane clean",
        )

        val offenders = chainsWithClick.filter { (_, chain) -> LITERAL_SIZE.containsMatchIn(chain) }
            .map { (file, chain) -> "${file.name}: ${chain.replace(WHITESPACE, " ").trim().take(120)}" }

        assertEquals(
            emptyList(),
            offenders,
            "a Focus control sizes its own hit area; route it through FocusGlyphButton, which reads " +
                "platformMinimumTarget",
        )
    }

    /**
     * The discriminating half. Without it, "no literal size near a clickable" is also satisfied by a lane with
     * no sized controls at all, so this asserts the component that IS allowed to size itself still does — and
     * does it from the authority rather than from a number of its own.
     */
    @Test
    fun theOneControlThatSizesItselfReadsThePlatformAuthority() {
        val component = focusUiSources().single { it.name == COMPONENT }
        val chain = modifierChains(component).single { it.contains(CLICKABLE) }

        assertTrue(chain.contains(".size($AUTHORITY)"), "the shared control must size itself from $AUTHORITY")
        assertTrue(!LITERAL_SIZE.containsMatchIn(chain), "and never from a literal")
    }

    /**
     * The extractor, checked against a case that is known to be clean and that the naive version got wrong.
     *
     * `FocusBaselineScreen.IntroContent` draws a decorative 10dp bullet and, ten lines later, a clickable
     * "Ahora no" link. They are two chains and neither is an offender; a splitter that merges them reports a
     * violation that does not exist. Without this, the guard above passes for the wrong reason the day someone
     * loosens the splitter to make a false positive go away.
     */
    @Test
    fun theExtractorKeepsTwoChainsInOneFunctionApart() {
        val screen = focusUiSources().single { it.name == "FocusBaselineScreen.kt" }
        val chains = modifierChains(screen)

        val bullet = chains.filter { it.startsWith(".size(10.dp)") }
        assertEquals(1, bullet.size, "expected exactly one decorative 10dp bullet chain")
        assertTrue(!bullet.single().contains(CLICKABLE), "the bullet is decorative: it must not absorb a click")

        val link = chains.filter { it.startsWith(CLICKABLE) }
        assertTrue(link.isNotEmpty(), "expected the back links to be extracted as their own chains")
        assertTrue(link.none { LITERAL_SIZE.containsMatchIn(it) }, "no back link declares a size of its own")
    }

    /**
     * The number itself. A hit area smaller than the disc it contains would make the guard above true and the
     * control still unreachable, so the visible diameters the lane uses are pinned under the minimum.
     */
    @Test
    fun everyVisibleDiscFitsInsideThePlatformMinimum() {
        assertEquals(A11yTargets.DESKTOP_MINIMUM, platformMinimumTarget, "this test runs on the desktop actual")
        VISIBLE_DIAMETERS_DP.forEach { diameter ->
            assertTrue(
                diameter <= platformMinimumTarget.value,
                "a ${diameter}dp disc does not fit inside a ${platformMinimumTarget.value}dp hit area",
            )
        }
    }

    /** Every `.kt` under the Focus V1 presentation UI package, screens and components alike. */
    private fun focusUiSources(): List<File> =
        locate(FOCUS_UI_DIR).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /**
     * Extracts one string per modifier chain: from a `Modifier`/`modifier` token forward through the
     * `.call(...)` segments that hang off it, with parentheses balanced so a nested lambda cannot end it.
     *
     * Splitting on blank lines instead — the first thing tried here — is not good enough and reports the
     * OPPOSITE of the truth: a function that draws a decorative `Modifier.size(10.dp)` bullet and, ten lines
     * later, a `Modifier.clickable(...)` text link becomes one blob containing both, and the guard "fails" on
     * two chains that are each individually fine. An extractor that over-reports is as useless as one that
     * reports nothing; it just fails in the direction that looks diligent.
     */
    private fun modifierChains(file: File): List<String> {
        val src = file.readText()
        return MODIFIER_TOKEN.findAll(src).map { match -> chainFrom(src, match.range.last + 1) }.toList()
    }

    /** Consumes `.name(...)` segments from [start], balancing parentheses; stops at anything else. */
    private fun chainFrom(src: String, start: Int): String {
        var i = start
        while (i < src.length) {
            val dot = src.indexOfFirstNonWhitespace(i)
            if (dot == null || src[dot] != '.') break
            // The '(' must belong to THIS call. A trailing-lambda segment (`.semantics { ... }`) has none,
            // and searching on would swallow the rest of the file into one chain.
            val call = CALL_SEGMENT.matchAt(src, dot) ?: break
            val open = dot + call.value.length - 1
            var depth = 0
            var j = open
            while (j < src.length) {
                if (src[j] == '(') depth++
                if (src[j] == ')') {
                    depth--
                    if (depth == 0) break
                }
                j++
            }
            if (depth != 0) break
            i = j + 1
        }
        return src.substring(start, i)
    }

    private fun String.indexOfFirstNonWhitespace(from: Int): Int? {
        var i = from
        while (i < length && this[i].isWhitespace()) i++
        return i.takeIf { it < length }
    }

    /** Walks up from the test working dir until the repo-relative path resolves (module vs root runner). */
    private fun locate(relative: String): File {
        var current: File? = File(".").absoluteFile
        while (current != null) {
            File(current, relative).takeIf { it.exists() }?.let { return it }
            current = current.parentFile
        }
        error("expected to locate $relative from ${File(".").absolutePath}")
    }

    private companion object {
        const val FOCUS_UI_DIR =
            "shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/presentation/ui"
        const val COMPONENT = "FocusGlyphButton.kt"
        const val CLICKABLE = ".clickable("
        const val AUTHORITY = "platformMinimumTarget"
        const val MIN_FILES = 15

        /** `.size(36.dp)` and friends. A `.size(platformMinimumTarget)` deliberately does not match. */
        val LITERAL_SIZE = Regex("""\.size\(\s*[0-9]""")
        val MODIFIER_TOKEN = Regex("""\b[Mm]odifier\b""")
        val CALL_SEGMENT = Regex("""\.\w+\s*\(""")
        val WHITESPACE = Regex("""\s+""")

        /** The visible discs the lane draws: the abort default and the home back button's override. */
        val VISIBLE_DIAMETERS_DP = listOf(36f, 40f)
    }
}
