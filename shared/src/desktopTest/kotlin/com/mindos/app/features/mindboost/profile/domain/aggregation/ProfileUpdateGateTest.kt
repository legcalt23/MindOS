package com.mindos.app.features.mindboost.profile.domain.aggregation

import com.mindos.app.features.mindboost.core.scoring.AdapterOutput
import com.mindos.app.features.mindboost.core.scoring.AntiGaming
import com.mindos.app.features.mindboost.core.scoring.BankIntegrity
import com.mindos.app.features.mindboost.core.scoring.DataQuality
import com.mindos.app.features.mindboost.core.scoring.DetectorCoverage
import com.mindos.app.features.mindboost.core.scoring.MetricStability
import com.mindos.app.features.mindboost.core.scoring.Sampling
import com.mindos.app.features.mindboost.core.scoring.ScoreInputs
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreProvenance
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.scoring.ScoreResultFactory
import com.mindos.app.features.mindboost.core.scoring.ScoreState
import com.mindos.app.features.mindboost.core.scoring.SecondaryGate
import com.mindos.app.features.mindboost.core.scoring.Signal
import com.mindos.app.features.mindboost.core.scoring.SignalKey
import com.mindos.app.features.mindboost.core.scoring.SignalUnit
import com.mindos.app.features.mindboost.core.scoring.TimebaseQuality
import com.mindos.app.features.mindboost.core.scoring.TimingCriticality
import com.mindos.app.features.mindboost.core.scoring.UncertaintyLabel
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** INFRA-06 · the only authorized ScoreResult→profile bridge: only VALID contributes; never a low value. */
class ProfileUpdateGateTest {

    private fun scoreResult(
        module: ScoreModuleId = ScoreModuleId.FOCUS,
        valid: Boolean = true,
        signalValue: Double? = 0.8,
    ): ScoreResult {
        val inputs = ScoreInputs(
            criticality = TimingCriticality.RT_IRRELEVANT,
            isEmotionalOrFrustration = false,
            completed = true,
            quality = DataQuality(
                TimebaseQuality.NOT_TIME_CRITICAL, false, 0.0,
                BankIntegrity.OK, MetricStability.STABLE, null,
            ),
            antiGaming = AntiGaming(isClean = true, detectorCoverage = DetectorCoverage.ADEQUATE),
            sampling = Sampling(
                nEffective = if (valid) 30 else 1,
                minNRequired = 20,
                confidence = 0.8,
                crossSessionAggregated = true,
                confidenceInterval = 0.0..1.0,
            ),
            primaryComputable = true,
            secondaryAccuracyGate = SecondaryGate(passes = true),
        )
        val signal = signalValue?.let {
            Signal(SignalKey("s"), it, null, null, SignalUnit.RATIO, UncertaintyLabel.LOW)
        }
        val provenance = ScoreProvenance("EX", module, "s1", "hash", timestampMs = 123L, scoringVersion = "v1")
        return ScoreResultFactory.from(AdapterOutput(inputs, provenance, signal))
    }

    @Test
    fun validResult_contributes_withMappedModuleAndScore() {
        val c = ProfileUpdateGate.contributionFor(scoreResult(ScoreModuleId.FOCUS))
        assertTrue(c != null)
        assertEquals(CognitiveModuleId.ENFOQUE, c.moduleId)
        assertEquals(0.8, c.normalizedScore)
        assertEquals(123L, c.updatedAtMs)
    }

    @Test
    fun nonValidResult_contributesNull_absenceNotLowValue() {
        val result = scoreResult(valid = false)
        assertEquals(ScoreState.INSUFFICIENT_N, result.state)
        assertNull(ProfileUpdateGate.contributionFor(result))
    }

    @Test
    fun moduleId_mapping_isTotal() {
        val expected = mapOf(
            ScoreModuleId.FOCUS to CognitiveModuleId.ENFOQUE,
            ScoreModuleId.SPEED to CognitiveModuleId.VELOCIDAD,
            ScoreModuleId.MEMORY to CognitiveModuleId.MEMORIA,
            ScoreModuleId.EPISODIC_MEMORY to CognitiveModuleId.MEMORIA_EPISODICA,
            ScoreModuleId.LOGIC to CognitiveModuleId.LOGICA,
            ScoreModuleId.LANGUAGE to CognitiveModuleId.LENGUAJE,
            ScoreModuleId.DECISIONS to CognitiveModuleId.DECISIONES,
            ScoreModuleId.EMOTIONS to CognitiveModuleId.EMOCIONES,
            ScoreModuleId.CREATIVITY to CognitiveModuleId.CREATIVIDAD,
        )
        for ((scoreId, cognitiveId) in expected) {
            assertEquals(cognitiveId, scoreId.toCognitiveModuleId())
        }
        assertEquals(ScoreModuleId.entries.size, expected.size, "mapping must cover every ScoreModuleId")
    }

    @Test
    fun unnormalizedSignal_yieldsNullScore_notAThrow() {
        val c = ProfileUpdateGate.contributionFor(scoreResult(signalValue = 5.0))
        assertTrue(c != null)
        assertNull(c.normalizedScore, "out-of-range signal must map to null, not a fabricated/clamped score")
    }

    @Test
    fun feedsRealPureAggregator_validRaisesDimension_nonValidLeavesItCalibrating() {
        val aggregator = MindBoostProfileAggregator()

        val withValid = aggregator.aggregate(
            userId = "u1",
            contributions = ProfileUpdateGate.contributions(listOf(scoreResult(ScoreModuleId.FOCUS))),
            nowMs = 0L,
        )
        val attention = withValid.dimensions.getValue(CognitiveDimension.ATTENTION)
        // A single contribution's weighted value equals its own score (float-rounding tolerance).
        assertEquals(0.8, assertNotNull(attention.value), absoluteTolerance = 1e-9)
        assertEquals(DimensionStatus.ESTABLISHED, attention.status)

        val withNonValid = aggregator.aggregate(
            userId = "u1",
            contributions = ProfileUpdateGate.contributions(listOf(scoreResult(valid = false))),
            nowMs = 0L,
        )
        // Absence of update, never a reduced value: the dimension stays calibrating with a null value.
        assertNull(withNonValid.dimensions.getValue(CognitiveDimension.ATTENTION).value)
    }
}
