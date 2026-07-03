package com.mindos.app.features.mindboost.profile.domain.aggregation

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.model.SessionEvidence
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import com.mindos.app.features.mindboost.profile.presentation.BrainProfileUiModelBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Fase 1.8 (DC-3) — the aggregator caps a dimension's maturity by the session-based module's cross-session
 * evidence when present, and is unchanged for trial-accumulation modules that expose no evidence.
 */
class MindBoostProfileAggregatorCalibrationTest {

    private val aggregator = MindBoostProfileAggregator()

    private fun contrib(module: CognitiveModuleId, conf: Double, evidence: SessionEvidence?, score: Double? = 0.7) =
        ModuleContribution(
            moduleId = module,
            normalizedScore = score,
            dataSufficiency = 0.9,
            confidence = conf,
            trend = ProfileTrend.STABLE,
            preliminary = true,
            updatedAtMs = 1L,
            sessionEvidence = evidence,
        )

    private fun stateFor(module: CognitiveModuleId, conf: Double, evidence: SessionEvidence?, score: Double? = 0.7) =
        aggregator.aggregate("u1", listOf(contrib(module, conf, evidence, score)), 1L)
            .dimension(module.primaryDimension)

    @Test
    fun oneSessionEvidence_downgradesToCalibrating_notDisplayable() {
        val state = stateFor(CognitiveModuleId.ENFOQUE, conf = 0.8, evidence = SessionEvidence(1, 1))
        assertEquals(DimensionStatus.CALIBRATING, state.status)
        assertNull(state.value, "one session is not enough cross-session evidence → CALIBRATING (never a number)")
    }

    @Test
    fun twoCrossSessionEvidence_allowsEmergingPerConfidence() {
        val state = stateFor(CognitiveModuleId.ENFOQUE, conf = 0.5, evidence = SessionEvidence(2, 2))
        assertEquals(DimensionStatus.EMERGING, state.status)
        assertNotNull(state.value)
    }

    @Test
    fun fiveSessionsAcrossThreeDays_allowsEstablishedPerConfidence() {
        val state = stateFor(CognitiveModuleId.ENFOQUE, conf = 0.8, evidence = SessionEvidence(5, 3))
        assertEquals(DimensionStatus.ESTABLISHED, state.status)
    }

    @Test
    fun highConfidenceButTooFewDistinctDays_cappedAtEmerging() {
        val state = stateFor(CognitiveModuleId.ENFOQUE, conf = 0.9, evidence = SessionEvidence(5, 2))
        assertEquals(DimensionStatus.EMERGING, state.status)
    }

    @Test
    fun noSessionEvidence_preservesCurrentConfidenceBehavior() {
        // Trial-accumulation modules (e.g. Memoria) expose null evidence → NOT gated by the cross-session clause.
        assertEquals(
            DimensionStatus.ESTABLISHED,
            stateFor(CognitiveModuleId.MEMORIA, conf = 0.8, evidence = null).status,
        )
        assertEquals(
            DimensionStatus.EMERGING,
            stateFor(CognitiveModuleId.MEMORIA, conf = 0.5, evidence = null).status,
        )
    }

    @Test
    fun endToEnd_insufficientEvidenceRendersEmDash() {
        val profile = aggregator.aggregate(
            "u1",
            listOf(contrib(CognitiveModuleId.ENFOQUE, conf = 0.8, evidence = SessionEvidence(1, 1))),
            1L,
        )
        val ui = BrainProfileUiModelBuilder.build(
            profile,
            TrainingRecommendation(null, null, RecommendationReason.NONE_AVAILABLE),
        )
        val attention = ui.dimensions.first { it.dimension == CognitiveDimension.ATTENTION }
        assertEquals("—", attention.displayValue)
        assertNull(attention.score0to100)
    }
}
