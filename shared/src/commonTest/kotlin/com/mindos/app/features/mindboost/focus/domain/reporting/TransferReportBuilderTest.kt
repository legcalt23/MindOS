package com.mindos.app.features.mindboost.focus.domain.reporting

import com.mindos.app.features.mindboost.core.copy.ForbiddenLexicon
import com.mindos.app.features.mindboost.core.copy.MindBoostCopySafetyScanner
import com.mindos.app.features.mindboost.focus.domain.evaluation.EvaluationResult
import com.mindos.app.features.mindboost.focus.domain.evaluation.MetricChange
import com.mindos.app.features.mindboost.focus.domain.evaluation.RciResult
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TransferReportBuilderTest {

    private fun evaluation(vararg changes: MetricChange) =
        EvaluationResult(userId = "user", capturedAtMs = 0, changes = changes.toList())

    @Test
    fun reliableProbeChange_isStrongEvidence() {
        val change = MetricChange(
            metric = MetricKey.RT_MEDIAN, pre = 320.0, post = 270.0,
            rci = RciResult.Value(rci = -2.1, reliable = true),
        )
        val report = TransferReportBuilder.build(evaluation(change))
        assertTrue(report.hasStrongEvidence)
        assertEquals(MetricKey.RT_MEDIAN, report.strongEvidence.single().metric)
    }

    @Test
    fun gatedChange_isPreliminaryOnly() {
        val change = MetricChange(
            metric = MetricKey.ACCURACY, pre = 0.80, post = 0.84, rci = RciResult.NotEnoughData,
        )
        val report = TransferReportBuilder.build(evaluation(change))
        assertFalse(report.hasStrongEvidence)
        assertEquals(1, report.preliminary.size)
    }

    /**
     * Re-anchored by FOC-P0-00. Both notes used to DENY a far-transfer reading in one clause while
     * ASSERTING a near-transfer one in the next — and the near claim was false, because the reserved
     * probes run paradigms the catalogue also trains (see `FocusClaimLevelGateTest`). The freeze
     * removed the assertion, so there is nothing left to deny, and the copy now states its bound
     * positively: the form this codebase already prefers, since it satisfies the invariant
     * structurally rather than by vigilance. The retired wording is described, never quoted — a
     * comment that keeps prohibited copy is one more site for the scanner and the licence that
     * revives the defect.
     *
     * What is pinned is therefore the BOUND, plus a half this test never had: that no term from the
     * claim ladder reappears. `FocusClaimLevelGateTest` cannot supply that half here, because it reads
     * source literals and this note is composed at runtime from two of them.
     */
    @Test
    fun honestNote_neverClaimsFarTransfer() {
        val strong = TransferReportBuilder.build(
            evaluation(
                MetricChange(MetricKey.RT_CV, 0.30, 0.20, RciResult.Value(-2.5, reliable = true)),
            ),
        ).honestNote.lowercase()
        assertTrue("no una conclusión sobre ti" in strong, "the strong note must bound itself: $strong")
        assertNoClaimAboveC0(strong)
    }

    @Test
    fun preliminaryNote_framesIntraExerciseAsSecondary() {
        val note = TransferReportBuilder.build(
            evaluation(MetricChange(MetricKey.ACCURACY, 0.8, 0.82, RciResult.NotEnoughData)),
        ).honestNote.lowercase()
        assertTrue("describe tu práctica" in note, "in-exercise progress must be framed as practice: $note")
        assertTrue("no una conclusión sobre ti" in note, "and explicitly not as a conclusion: $note")
        assertNoClaimAboveC0(note)
    }

    private fun assertNoClaimAboveC0(composed: String) {
        val hits = MindBoostCopySafetyScanner.scan(composed, ForbiddenLexicon.EFFICACY_CLAIMS_C0)
        assertTrue(hits.isEmpty(), "composed copy claims above C0 ${hits.map { it.term }}: $composed")
    }

    // --- R12 · `MB-CATALOG-0019` — a measure withheld for sample must be DISCLOSED, not omitted. ---

    private fun withheld(change: MetricChange, vararg keys: MetricKey) = EvaluationResult(
        userId = "user",
        capturedAtMs = 0,
        changes = listOf(change),
        withheldForSample = keys.toList(),
    )

    @Test
    fun withheldMeasures_areDisclosedInTheNote() {
        val note = TransferReportBuilder.build(
            withheld(
                MetricChange(MetricKey.ACCURACY, 0.8, 0.82, RciResult.NotEnoughData),
                MetricKey.CONGRUENCY_EFFECT,
            ),
        ).honestNote.lowercase()

        // The plain note's "aun sin cambio fiable" promises reachability ("yet"). For a cell that cannot
        // reach its ratified floor at this probe length, more sessions will never produce it, so the
        // omission has to be stated rather than left as silence.
        assertTrue("muestra suficiente" in note, "the withheld fact must reach the note: $note")
    }

    @Test
    fun theWithheldClauseIsAppended_leavingTheExistingNotesIntact_discriminantHalf() {
        // Abort criterion A3, mechanised WITHOUT transcribing the literals: pinning by `startsWith` fails
        // the moment either existing string is reworded, and does not rot when the copy is re-accented.
        val preliminary = MetricChange(MetricKey.ACCURACY, 0.8, 0.82, RciResult.NotEnoughData)
        val strong = MetricChange(MetricKey.RT_CV, 0.30, 0.20, RciResult.Value(-2.5, reliable = true))

        listOf(preliminary, strong).forEach { change ->
            val plain = TransferReportBuilder.build(evaluation(change)).honestNote
            val disclosed = TransferReportBuilder.build(
                withheld(change, MetricKey.CONGRUENCY_EFFECT),
            ).honestNote

            assertTrue(disclosed.startsWith(plain), "the existing note must survive byte-identically")
            assertTrue(disclosed.length > plain.length, "and the withheld fact must actually be added")
        }
    }

    @Test
    fun withoutWithheldMeasures_theNoteGainsNothing_discriminantHalf() {
        // Without this half, appending the clause unconditionally would pass everything above.
        val note = TransferReportBuilder.build(
            evaluation(MetricChange(MetricKey.ACCURACY, 0.8, 0.82, RciResult.NotEnoughData)),
        ).honestNote.lowercase()
        assertFalse("muestra suficiente" in note)
    }

    // --- R13 · `MB-CATALOG-0021` — a row's label must be a NAME, never the enum identifier. ---

    /** The label the builder actually hands the three screens for [metric]. */
    private fun labelOf(metric: MetricKey): String =
        TransferReportBuilder.build(
            evaluation(MetricChange(metric, 1.0, 1.0, RciResult.NotEnoughData)),
        ).preliminary.single().label

    @Test
    fun noLabelLeaksTheInternalIdentifier() {
        // Abort criterion A2. Driven off `MetricKey.entries`, so a metric added later is covered without
        // editing this test — and the `when` it feeds is exhaustive, so that metric cannot compile unnamed.
        // Every metric is checked, not only the ones a probe can reach today: `perMetric` carries whatever
        // the engine marked `confident`, and which of those clear the central floor is not this file's call.
        assertTrue(MetricKey.entries.size > 20, "anti-vacuity: the catalogue shrank, this sweep proves little")

        MetricKey.entries.forEach { metric ->
            val label = labelOf(metric)
            assertNotEquals(metric.name, label, "the raw identifier reached the screen for $metric")
            assertFalse('_' in label, "an identifier-shaped label reached the screen for $metric: $label")
            assertTrue(label.isNotBlank(), "$metric has a blank label")
        }
    }

    @Test
    fun theFourPreexistingLabelsAreByteIdentical_discriminantHalf() {
        // Abort criterion A1. Without this half, "rename everything" would satisfy the sweep above while
        // silently rewording copy that already shipped — which is a product decision, not a defect fix.
        assertEquals("Precisión (prueba)", labelOf(MetricKey.ACCURACY))
        assertEquals("Reacción (prueba)", labelOf(MetricKey.RT_MEDIAN))
        assertEquals("Estabilidad (prueba)", labelOf(MetricKey.RT_CV))
        assertEquals("Lapsos (prueba)", labelOf(MetricKey.LAPSES))
    }

    @Test
    fun everyLabelMarksTheRowAsProbeMeasured() {
        // The "(prueba)" suffix is the ONLY probe marker anywhere on the row. No screen carries a
        // section header saying so — the one this comment used to cite was never shipped.
        MetricKey.entries.forEach { metric ->
            assertTrue("(prueba)" in labelOf(metric), "$metric loses the probe marker: ${labelOf(metric)}")
        }
    }

    // --- R14 · `MB-CATALOG-0024` — a measured change must never be written as a bare zero. ---

    /** The delta string the builder actually hands the two screens for this pre/post pair. */
    private fun deltaTextOf(pre: Double, post: Double): String =
        TransferReportBuilder.build(
            evaluation(MetricChange(MetricKey.ACCURACY, pre, post, RciResult.NotEnoughData)),
        ).preliminary.single().deltaText

    @Test
    fun subUnitChangeKeepsItsMagnitude_inBothDirections() {
        // Abort criterion A2, on the repo's own canonical fixture: 0.80 -> 0.82 is two accuracy points,
        // and integer rounding wrote it as "+0" — a measured value rendered as no change.
        assertEquals("+0,02", deltaTextOf(0.80, 0.82))
        assertEquals("-0,02", deltaTextOf(0.82, 0.80))
        // The asymmetry that made a real decline indistinguishable from stillness: `sign` only ever added
        // "+", and Int has no negative zero, so both a decline and a true zero printed "0".
        assertNotEquals(deltaTextOf(0.82, 0.80), deltaTextOf(0.80, 0.80))
    }

    @Test
    fun everyReachableSubUnitMetricStopsPrintingABareZero() {
        // The four metrics that clear the central floor at probe length AND live below 1.0 (R13 §1.2 crossed
        // with the engine's real ranges): proportion, ratio and the 0..1 indices. Realistic deltas.
        val cases = listOf(
            MetricChange(MetricKey.ACCURACY, 0.80, 0.82, RciResult.NotEnoughData),
            MetricChange(MetricKey.RT_CV, 0.30, 0.20, RciResult.NotEnoughData),
            MetricChange(MetricKey.BLOCK_STABILITY, 0.70, 0.75, RciResult.NotEnoughData),
            MetricChange(MetricKey.RATE_CORRECT_SCORE, 2.10, 2.40, RciResult.NotEnoughData),
        )
        TransferReportBuilder.build(evaluation(*cases.toTypedArray())).preliminary.forEach { line ->
            assertNotEquals("+0", line.deltaText, "${line.metric} still prints a fabricated zero")
            assertNotEquals("0", line.deltaText, "${line.metric} still prints a fabricated zero")
            assertTrue(',' in line.deltaText, "${line.metric} lost its decimals: ${line.deltaText}")
        }
    }

    @Test
    fun deltasAtOrAboveOneRenderExactlyAsBefore_discriminantHalf() {
        // Abort criterion A1. Without this half, "always two decimals" would satisfy the tests above while
        // rewording every shipped millisecond, count and focus-index row. Integer rounding is CORRECT there.
        assertEquals("-50", deltaTextOf(320.0, 270.0)) // RT-shaped, milliseconds
        assertEquals("+3", deltaTextOf(40.0, 43.2)) // focus-index-shaped, 0..100 under the same `INDEX` unit
        assertEquals("+2", deltaTextOf(1.0, 3.0)) // count-shaped
        assertEquals("0", deltaTextOf(0.80, 0.80)) // an exact zero stays the bare "0" it already was
        assertEquals("+1", deltaTextOf(0.0, 0.996)) // rounds up to the unit: the integer branch, not "+0,100"
    }
}
