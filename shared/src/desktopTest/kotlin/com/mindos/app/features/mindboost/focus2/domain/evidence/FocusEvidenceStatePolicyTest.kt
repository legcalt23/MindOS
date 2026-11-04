package com.mindos.app.features.mindboost.focus2.domain.evidence

import com.mindos.app.features.mindboost.core.session.cognitive.evidence.SessionEvidenceState
import com.mindos.app.features.mindboost.core.timing.InputTimestampPrecision
import com.mindos.app.features.mindboost.core.timing.TemporalClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §8 [R2-HIST] — the composition's pinned properties: OFFICIAL_ELIGIBLE is
 * inconstructible (exhaustive, not happy-path), NotCaptured never demotes accuracy evidence, the RT
 * shadow lane requires modality AND native precision AND an Eligible temporal verdict — and the
 * focus2-local vocabulary stays name-identical to the cognitive lane's (the barrier forbids sharing
 * the TYPE in commonMain; parity is pinned HERE, where the barrier does not apply, so the two
 * vocabularies cannot drift silently).
 */
class FocusEvidenceStatePolicyTest {

    private fun evaluation(
        rtVerdict: FocusTemporalEligibility.Verdict,
        excludedFraction: Double?,
        sessionClass: TemporalClass = TemporalClass.T1_FRAME_OBSERVED,
        windowsClassified: Int = 10,
        windowsUnobserved: Int = 0,
    ) = FocusTemporalEligibility.TemporalEvaluation(
        sessionClass = sessionClass,
        windowsClassified = windowsClassified,
        windowsUnobserved = windowsUnobserved,
        coverageComplete = true,
        excludedFraction = excludedFraction,
        rtShadowVerdict = rtVerdict,
    )

    private val temporalVariants: List<FocusTemporalEligibility.TemporalEvaluation?> = listOf(
        null,
        evaluation(FocusTemporalEligibility.Verdict.NotCaptured, excludedFraction = null),
        evaluation(FocusTemporalEligibility.Verdict.Eligible, excludedFraction = 0.0),
        evaluation(
            FocusTemporalEligibility.Verdict.Excluded(
                setOf(FocusTemporalEligibility.ExclusionCause.SESSION_BACKSTOP_EXCEEDED),
            ),
            excludedFraction = 0.9,
        ),
    )

    @Test
    fun vocabulary_isNameIdentical_toTheCanonicalLane() {
        assertEquals(
            SessionEvidenceState.entries.map { it.name },
            FocusEvidenceState.entries.map { it.name },
            "the two session-evidence vocabularies must not drift",
        )
    }

    @Test
    fun officialEligible_isInconstructible_forEveryInputCombination() {
        for (writeAhead in listOf(true, false)) {
            for (dose in listOf(true, false)) {
                for (invalidated in listOf(true, false)) {
                    for (temporal in temporalVariants) {
                        val state = FocusEvidenceStatePolicy.evidenceOf(writeAhead, dose, invalidated, temporal)
                        assertNotEquals(
                            FocusEvidenceState.OFFICIAL_ELIGIBLE,
                            state,
                            "OFFICIAL_ELIGIBLE must have no producer in v1 " +
                                "(writeAhead=$writeAhead dose=$dose invalidated=$invalidated temporal=$temporal)",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun invalidation_winsOverEverything() {
        val state = FocusEvidenceStatePolicy.evidenceOf(
            writeAheadAdmissible = true,
            doseComplete = true,
            scorerInvalidated = true,
            temporal = evaluation(FocusTemporalEligibility.Verdict.Eligible, 0.0),
        )
        assertEquals(FocusEvidenceState.INVALIDATED, state)
    }

    @Test
    fun notCaptured_doesNotDemote_accuracyEvidence() {
        // A probe-less platform still yields provisional accuracy evidence (parity honesty).
        val state = FocusEvidenceStatePolicy.evidenceOf(
            writeAheadAdmissible = true,
            doseComplete = true,
            scorerInvalidated = false,
            temporal = null,
        )
        assertEquals(FocusEvidenceState.PROVISIONAL, state)
    }

    @Test
    fun sessionBackstop_demotesToTrainingOnly() {
        val state = FocusEvidenceStatePolicy.evidenceOf(
            writeAheadAdmissible = true,
            doseComplete = true,
            scorerInvalidated = false,
            temporal = evaluation(
                FocusTemporalEligibility.Verdict.Excluded(
                    setOf(FocusTemporalEligibility.ExclusionCause.SESSION_BACKSTOP_EXCEEDED),
                ),
                excludedFraction = 0.9,
            ),
        )
        assertEquals(FocusEvidenceState.TRAINING_ONLY, state)
    }

    @Test
    fun anEvaluationThatDerivedNoFraction_doesNotDemote_butDoesLoseTheRtLane() {
        // C4-04. Two DIFFERENT facts used to arrive here as the same `?: 0.0`: "no evaluation reached us"
        // (the test above) and "an evaluation reached us and could derive no fraction". This is the second
        // one — reachable through the C4-01 path, where every window was unobservable — and the split has
        // to be asserted, not inherited from a sentinel, because the two axes disagree ON PURPOSE:
        // accuracy is modality-fair and stays PROVISIONAL, while the RT lane, which cannot mean anything
        // without temporal capture, is withdrawn. A future edit that collapses them will break here.
        val unobservable = evaluation(
            FocusTemporalEligibility.Verdict.Excluded(
                setOf(
                    FocusTemporalEligibility.ExclusionCause.COVERAGE_INCOMPLETE,
                    FocusTemporalEligibility.ExclusionCause.SESSION_BACKSTOP_EXCEEDED,
                ),
            ),
            excludedFraction = null,
            windowsClassified = 0,
            windowsUnobserved = 10,
        )
        assertEquals(
            FocusEvidenceState.PROVISIONAL,
            FocusEvidenceStatePolicy.evidenceOf(true, true, false, unobservable),
            "an underivable fraction is not evidence of a bad session, so it must not demote accuracy",
        )
        assertFalse(
            FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.NANOS_NATIVE, unobservable),
            "the RT lane needs an Eligible temporal verdict, and nothing was classifiable",
        )
    }

    @Test
    fun failedGates_demoteToTrainingOnly() {
        assertEquals(
            FocusEvidenceState.TRAINING_ONLY,
            FocusEvidenceStatePolicy.evidenceOf(false, true, false, null),
        )
        assertEquals(
            FocusEvidenceState.TRAINING_ONLY,
            FocusEvidenceStatePolicy.evidenceOf(true, false, false, null),
        )
    }

    @Test
    fun rtShadow_requiresModality_nativePrecision_andEligibleTemporal() {
        val eligible = evaluation(FocusTemporalEligibility.Verdict.Eligible, 0.0)
        assertTrue(
            FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.NANOS_NATIVE, eligible),
        )
        assertFalse(
            FocusEvidenceStatePolicy.rtShadowAdmissible(false, InputTimestampPrecision.NANOS_NATIVE, eligible),
            "sequential/screen-reader modality never feeds the RT lane",
        )
        assertFalse(
            FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.MILLIS_DERIVED, eligible),
            "derived precision never feeds the RT lane",
        )
        assertFalse(
            FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.NANOS_NATIVE, null),
            "no temporal capture never feeds the RT lane",
        )
    }
}
