package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.ui.graphics.Color
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.presentation.state.ChoiceParadigm
import com.mindos.app.features.mindboost.focus.presentation.state.FeedbackKind
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins that no distinction a Focus exercise asks the user to make rests on colour alone — [FocusStimulusField]
 * by signature, and E6's colour dimension by luminance, kept here so the invariant has one owner.
 *
 * Three real defects motivated this, all measured on the tree of 2026-09-03:
 *  - `Stop` (E4's stop-signal, the discriminandum the SSRT is derived from) differed from the GO `Signal`
 *    only by hue — green vs red, the canonical colour-vision confusion pair — plus 10 % of radius, and the
 *    two are never on screen together, so the radius was not comparable to anything.
 *  - `TOO_EARLY` and `FALSE_ALARM` rendered BYTE-IDENTICALLY: same colour, scale, alpha, duration and core.
 *    That one was invisible to every user, not only to a colour-blind one.
 *  - E6's COLOR rule decided the answer by hue with 1.91:1 of luminance behind it, on a task whose shape
 *    dimension is contradictory by design, so the trials a colour-blind user could not answer were scored
 *    as switch cost.
 */
class FocusFieldAchromaticTest {

    /** Every state the user must tell apart from another WHILE it is drawn by this field. */
    private val discriminated: Map<String, FocusFieldState> = mapOf(
        "Idle" to FocusFieldState.Idle,
        "Signal" to FocusFieldState.Signal(contrast = 1f),
        "Lure" to FocusFieldState.Lure(contrast = 1f),
        "Stop" to FocusFieldState.Stop,
    ) + FeedbackKind.entries.associate { "Feedback.$it" to FocusFieldState.Feedback(it) }

    /** The states whose real rendering lives in their own composable; this field draws nothing for them. */
    private val renderedElsewhere: List<FocusFieldState> = listOf(
        FocusFieldState.Choice(
            ResponseKey.LEFT,
            congruent = true,
            distractorCount = 4,
            paradigm = ChoiceParadigm.FLANKER,
        ),
        FocusFieldState.Search(setSize = 8, targetPresent = true, layoutSeed = 1),
        FocusFieldState.RuleSwitch(colorIsRed = true, shapeIsCircle = true, ruleIsColor = true),
        FocusFieldState.Anchor(targetDirection = ResponseKey.LEFT, intruderPresent = false),
        FocusFieldState.Tracking(
            objectCount = 6,
            targetIndices = setOf(0),
            speed = 1.0,
            probedIsTarget = true,
            layoutSeed = 1,
        ),
        FocusFieldState.Stream(length = 12, t1Index = 3, t2Index = null, layoutSeed = 1),
        FocusFieldState.Dual(go = true, dual = false),
    )

    @Test
    fun everyDrawnStateIsTellableApartWithoutColour() {
        val signatures = discriminated.mapValues { (_, state) -> state.toVisual().achromatic }
        // Anti-vacuity: if the set ever shrinks to one, "all distinct" becomes trivially true.
        assertTrue(signatures.size >= 9, "expected at least 9 discriminated states, got ${signatures.size}")

        val byName = signatures.entries.toList()
        for (i in byName.indices) {
            for (j in i + 1 until byName.size) {
                val a = byName[i]
                val b = byName[j]
                assertNotEquals(
                    a.value,
                    b.value,
                    "${a.key} and ${b.key} render identically with no colour vision: ${a.value}",
                )
            }
        }
    }

    /**
     * The discriminating half. Without the two achromatic marks the historical collisions come straight
     * back, so this fails the moment [FieldVisual.bar] or [FieldVisual.innerRing] stops carrying the
     * distinction — which is what makes the test above non-decorative.
     */
    @Test
    fun theTwoMarksAreTheOnlyThingSeparatingThePairsThatUsedToCollide() {
        fun withoutMarks(state: FocusFieldState) = state.toVisual().achromatic.copy(bar = false, innerRing = false)

        assertEquals(
            withoutMarks(FocusFieldState.Signal(contrast = 1f)),
            withoutMarks(FocusFieldState.Stop),
            "GO and STOP must still be separated by the mark alone; if they now differ elsewhere, re-decide this",
        )
        assertEquals(
            withoutMarks(FocusFieldState.Feedback(FeedbackKind.FALSE_ALARM)),
            withoutMarks(FocusFieldState.Feedback(FeedbackKind.TOO_EARLY)),
            "FALSE_ALARM and TOO_EARLY share every other channel; the inner ring is what separates them",
        )
    }

    /**
     * Why the signature carries a size BAND and not the raw radius: with the raw float, `Stop` at 1.1 and
     * `Signal` at 1.0 read as "distinct", so the pairwise check above passed on a 10 % radius step that no
     * user racing a stop-signal can perceive — the guard was green while the defect it exists for was live.
     * The band is what makes the mark load-bearing, so it gets pinned too.
     */
    @Test
    fun theGoAndStopDiscsShareASizeBandSoRadiusCannotStandInForTheMark() {
        assertEquals(SizeBand.MEDIUM, sizeBandOf(1.0f), "GO disc band")
        assertEquals(SizeBand.MEDIUM, sizeBandOf(1.1f), "STOP disc band")
        assertEquals(SizeBand.SMALL, sizeBandOf(0.55f), "Idle is a band apart, and that IS perceptible")
        assertEquals(SizeBand.LARGE, sizeBandOf(1.28f), "the feedback flash is a band apart too")
    }

    /**
     * The GO signal is pinned exactly. Its detection latency is the baseline the stop-signal reaction time is
     * measured against, so the fix for STOP had to leave it untouched — a change here moves a metric.
     */
    @Test
    fun theGoSignalCarriesNoMarkAndKeepsItsGeometry() {
        val go = FocusFieldState.Signal(contrast = 1f).toVisual()
        assertEquals(1.0f, go.scale, "GO radius")
        assertTrue(go.solidCore, "GO keeps its solid core")
        assertTrue(!go.bar && !go.innerRing, "GO gains no mark: only STOP does")
        assertEquals(200, go.durationMs, "GO onset timing")
    }

    /**
     * E6's colour dimension, which [FocusRuleSwitchField] draws and this field does not.
     *
     * It is the worst instance of the same invariant, so it is pinned in the same place rather than in a
     * second file: under the COLOR rule the fill is the ONLY thing that decides the answer and the shape
     * shown alongside it is contradictory half the time, so a user who cannot resolve the hue fails those
     * trials and the miss is recorded as SWITCH_COST — the instrument's barrier written down as a property
     * of the person. The historical pair (`FocusError` against `NeuCyan`) had 1.91:1 of luminance contrast:
     * a difference, but not one anybody designed or could rely on.
     */
    @Test
    fun theTwoValuesOfE6sColourDimensionAreSeparableWithoutHue() {
        val ratio = contrastRatio(ruleColorFill(colorIsRed = true), ruleColorFill(colorIsRed = false))
        assertTrue(
            ratio >= MIN_CONTRAST,
            "E6's two rule colours are $ratio:1 apart in luminance; under the COLOR rule that is the only " +
                "channel a colour-blind user has, so it must be at least $MIN_CONTRAST:1",
        )
    }

    /**
     * The discriminating half. Without it, the assertion above is satisfied by pushing the dark value to
     * black — separable from cyan and invisible against the canvas, which trades one barrier for another.
     */
    @Test
    fun bothValuesStayVisibleAgainstTheFocusCanvas() {
        listOf(true, false).forEach { isRed ->
            val ratio = contrastRatio(ruleColorFill(isRed), AppColors.FocusCanvas)
            assertTrue(
                ratio >= MIN_CONTRAST,
                "the ${if (isRed) "dark" else "bright"} rule colour is $ratio:1 against the canvas: " +
                    "separating the pair must not make one of them unreadable",
            )
        }
    }

    /** What licenses excluding these from the check above: they draw nothing here. */
    @Test
    fun statesRenderedElsewhereDrawNothingInThisField() {
        assertTrue(renderedElsewhere.size >= 7, "anti-vacuity: expected the 7 delegating states")
        renderedElsewhere.forEach { state ->
            val visual = state.toVisual()
            assertTrue(!visual.drawnHere, "$state must be transparent here; it renders in its own composable")
            assertEquals(0f, visual.alpha, "$state alpha")
        }
    }

    private companion object {
        /** WCAG's floor for a non-text discriminandum. Below it the pair is a hue difference and little else. */
        const val MIN_CONTRAST = 3.0f

        /** WCAG relative luminance: the channel that survives every form of colour vision deficiency. */
        fun Color.relativeLuminance(): Float {
            fun channel(c: Float) = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
            return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
        }

        fun contrastRatio(a: Color, b: Color): Float {
            val first = a.relativeLuminance()
            val second = b.relativeLuminance()
            return (max(first, second) + 0.05f) / (min(first, second) + 0.05f)
        }
    }
}
