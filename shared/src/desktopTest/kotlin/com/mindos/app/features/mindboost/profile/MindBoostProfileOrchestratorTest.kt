package com.mindos.app.features.mindboost.profile

import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.orchestration.MindBoostProfileOrchestrator
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MindBoostProfileOrchestratorTest {

    private val aggregator = MindBoostProfileAggregator()
    private val orchestrator = MindBoostProfileOrchestrator()

    private fun contrib(module: CognitiveModuleId, score: Double?, confidence: Double) =
        ModuleContribution(module, score, dataSufficiency = 0.9, confidence = confidence, ProfileTrend.STABLE, preliminary = false)

    @Test
    fun noAvailableModules_recommendsNothing() {
        val profile = aggregator.aggregate("u1", emptyList(), 1L)
        val rec = orchestrator.recommend(profile, emptySet())
        assertEquals(RecommendationReason.NONE_AVAILABLE, rec.reason)
        assertNull(rec.moduleId)
    }

    @Test
    fun calibratingDimension_recommendedToGatherData() {
        // VELOCIDAD established; ENFOQUE has no data (calibrating) → ENFOQUE wins to gather data.
        val profile = aggregator.aggregate("u1", listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.6, 0.8)), 1L)
        val rec = orchestrator.recommend(profile, setOf(CognitiveModuleId.VELOCIDAD, CognitiveModuleId.ENFOQUE))
        assertEquals(CognitiveModuleId.ENFOQUE, rec.moduleId)
        assertEquals(CognitiveDimension.ATTENTION, rec.dimension)
        assertEquals(RecommendationReason.GATHER_DATA, rec.reason)
    }

    @Test
    fun emergingPreferredOverEstablished_toBuildConfidence() {
        // LOGICA emerging (low confidence), VELOCIDAD established → train LOGICA to gather data.
        val profile = aggregator.aggregate(
            "u1",
            listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.6, 0.9), contrib(CognitiveModuleId.LOGICA, 0.5, 0.5)),
            1L,
        )
        val rec = orchestrator.recommend(profile, setOf(CognitiveModuleId.VELOCIDAD, CognitiveModuleId.LOGICA))
        assertEquals(CognitiveModuleId.LOGICA, rec.moduleId)
        assertEquals(RecommendationReason.GATHER_DATA, rec.reason)
    }

    @Test
    fun allEstablished_developsTheLowestValuedArea() {
        val profile = aggregator.aggregate(
            "u1",
            listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.8, 0.9), contrib(CognitiveModuleId.LOGICA, 0.3, 0.9)),
            1L,
        )
        val rec = orchestrator.recommend(profile, setOf(CognitiveModuleId.VELOCIDAD, CognitiveModuleId.LOGICA))
        assertEquals(CognitiveModuleId.LOGICA, rec.moduleId) // 0.3 is the lowest established
        assertEquals(RecommendationReason.DEVELOP_AREA, rec.reason)
    }
}
