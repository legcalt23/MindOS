package com.mindos.app.features.mindboost.focus.presentation.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins a LIMITATION, not a capability: the stimulus renderers of the Focus V1 lane declare no semantic node,
 * and adding one would be worse than the absence.
 *
 * `MB-FOCUS-0110` measured why. In a visual discrimination task a description either REVEALS the answer (a
 * go/no-go disc announced by its colour is the response) or it is announced asynchronously with respect to
 * stimulus onset, and the announcement lands inside the window the reaction time is derived from — the input
 * the only capacity claim this product makes is computed from. So "the renderers carry no label" is not an
 * oversight waiting for a one-line fix; what is missing is a declared eligibility decision, or the sequential
 * modality the V5 lane already models. Both are product decisions, and neither is a test.
 *
 * A RED here is therefore NOT a regression. It means somebody took that decision and gave a renderer a
 * semantic node. When that happens the assertion is to be INVERTED with the evidence of the decision, never
 * deleted — and the change must land in BOTH copies of the capture layer (`FocusSessionScreen` and
 * `FocusMeasureRunningContent`) or the pre and post lanes of the comparison diverge (`MB-FOCUS-0109`).
 *
 * The CONTROLS are a separate matter and are already labelled: `FocusGlyphButton` carries its own description
 * and a button role (`MB-FOCUS-0108`), which is what the discriminating half below asserts. Without it this
 * guard would also be satisfied by a lane with no semantics anywhere, which is the vacuity it exists to avoid.
 */
class FocusStimulusSemanticsTest {

    @Test
    fun noStimulusRendererDeclaresASemanticNode() {
        val fields = stimulusRenderers()
        // Anti-vacuity: an extractor that reads nothing reports "all clean" about everything.
        assertTrue(fields.size >= MIN_FIELDS, "expected at least $MIN_FIELDS stimulus renderers, found ${fields.size}")

        val labelled = fields.filter { file ->
            SEMANTIC_NODE.containsMatchIn(codeWithoutComments(file.readText()))
        }.map { it.name }

        assertEquals(
            emptyList(),
            labelled,
            "a stimulus renderer gained a semantic node: $labelled. If this is the eligibility decision of " +
                "MB-FOCUS-0110, invert this assertion with that decision as evidence — do not delete it",
        )
    }

    /**
     * The discriminating half. The one component in the package that IS a control, not a stimulus, still
     * declares both its description and its role — so a green above means "the renderers carry nothing",
     * not "the reader sees nothing".
     */
    @Test
    fun theControlThatIsNotAStimulusStillCarriesItsLabel() {
        val control = componentSources().single { it.name == CONTROL }
        val source = codeWithoutComments(control.readText())

        assertTrue(SEMANTIC_NODE.containsMatchIn(source), "$CONTROL must keep the label it was given")
        assertTrue(source.contains(BUTTON_ROLE), "$CONTROL must keep announcing itself as a button")
    }

    /**
     * The other half every text guard needs and that is the one usually missing: proof that the reader does
     * not read too MUCH. A KDoc describing the label this lane deliberately does not ship is prose, not a
     * semantic node, and counting it would report a violation that does not exist.
     */
    @Test
    fun aDescriptionThatLivesOnlyInACommentIsNotASemanticNode() {
        val prose = "/** Deliberately no contentDescription here. */\nfun render() {}\n// .semantics { } either"
        assertTrue(!SEMANTIC_NODE.containsMatchIn(codeWithoutComments(prose)), "comments are not nodes")
        assertTrue(
            SEMANTIC_NODE.containsMatchIn(codeWithoutComments("    .semantics { contentDescription = it }")),
            "and a real node is still seen",
        )
    }

    /** Every stimulus renderer: the package's `*Field.kt`, so a new one is covered without editing a list. */
    private fun stimulusRenderers(): List<File> = componentSources().filter { it.name.endsWith(FIELD_SUFFIX) }

    private fun componentSources(): List<File> =
        locate(COMPONENTS_DIR).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Strips block and line comments, so prose ABOUT a label never counts as one. */
    private fun codeWithoutComments(source: String): String {
        val noBlock = source.replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        return noBlock.lineSequence().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.take(idx) else line
        }
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
        const val COMPONENTS_DIR =
            "shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/presentation/ui/components"
        const val CONTROL = "FocusGlyphButton.kt"
        const val FIELD_SUFFIX = "Field.kt"
        const val BUTTON_ROLE = "Role.Button"
        const val MIN_FIELDS = 8

        /** A description or a semantics block — either one puts the renderer on a screen reader's path. */
        val SEMANTIC_NODE = Regex("""contentDescription|\.semantics\s*[({]|clearAndSetSemantics""")
    }
}
