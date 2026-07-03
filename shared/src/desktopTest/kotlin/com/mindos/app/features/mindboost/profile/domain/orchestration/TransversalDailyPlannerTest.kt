package com.mindos.app.features.mindboost.profile.domain.orchestration

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransversalDailyPlannerTest {

    private val planner = TransversalDailyPlanner()

    private val plannable = setOf(
        CognitiveModuleId.ENFOQUE,
        CognitiveModuleId.MEMORIA,
        CognitiveModuleId.LOGICA,
        CognitiveModuleId.DECISIONES,
        CognitiveModuleId.EMOCIONES,
    )

    private fun profileOf(vararg states: DimensionState): MindBoostProfile =
        MindBoostProfile("u1", states.associateBy { it.dimension }, overall = null, updatedAtMs = 1L)

    private fun calibrating(dimension: CognitiveDimension, sufficiency: Double) = DimensionState(
        dimension = dimension, value = null, confidence = 0.0, dataSufficiency = sufficiency,
        trend = ProfileTrend.UNKNOWN, status = DimensionStatus.CALIBRATING, preliminary = true,
        contributingModules = emptyList(),
    )

    private fun emerging(dimension: CognitiveDimension, confidence: Double) = DimensionState(
        dimension = dimension, value = 0.5, confidence = confidence, dataSufficiency = 0.9,
        trend = ProfileTrend.UNKNOWN, status = DimensionStatus.EMERGING, preliminary = true,
        contributingModules = emptyList(),
    )

    private fun established(dimension: CognitiveDimension, value: Double) = DimensionState(
        dimension = dimension, value = value, confidence = 0.9, dataSufficiency = 1.0,
        trend = ProfileTrend.STABLE, status = DimensionStatus.ESTABLISHED, preliminary = false,
        contributingModules = emptyList(),
    )

    @Test
    fun emptyProfile_plansFirstModulesByOrdinal_asGatherData() {
        val plan = planner.plan(profileOf(), plannable, maxItems = 3)
        assertEquals(
            listOf(CognitiveModuleId.ENFOQUE, CognitiveModuleId.MEMORIA, CognitiveModuleId.LOGICA),
            plan.map { it.moduleId },
        )
        assertTrue(plan.all { it.reason == RecommendationReason.GATHER_DATA })
    }

    @Test
    fun calibratingDims_orderedByMostDataFirst() {
        val profile = profileOf(
            calibrating(CognitiveDimension.ATTENTION, sufficiency = 0.2), // ENFOQUE
            calibrating(CognitiveDimension.WORKING_MEMORY, sufficiency = 0.8), // MEMORIA
            calibrating(CognitiveDimension.REASONING, sufficiency = 0.5), // LOGICA
        )
        val plan = planner.plan(profile, plannable, maxItems = 3)
        assertEquals(
            listOf(CognitiveModuleId.MEMORIA, CognitiveModuleId.LOGICA, CognitiveModuleId.ENFOQUE),
            plan.map { it.moduleId },
        )
    }

    @Test
    fun tiers_calibratingBeforeEmergingBeforeEstablished() {
        val profile = profileOf(
            established(CognitiveDimension.ATTENTION, value = 0.9), // ENFOQUE — tier 2
            calibrating(CognitiveDimension.WORKING_MEMORY, sufficiency = 0.3), // MEMORIA — tier 0
            emerging(CognitiveDimension.REASONING, confidence = 0.4), // LOGICA — tier 1
        )
        // Restrict to the three set dims so unset modules (which default to CALIBRATING) don't intrude.
        val available = setOf(CognitiveModuleId.ENFOQUE, CognitiveModuleId.MEMORIA, CognitiveModuleId.LOGICA)
        val plan = planner.plan(profile, available, maxItems = 3)
        assertEquals(
            listOf(CognitiveModuleId.MEMORIA, CognitiveModuleId.LOGICA, CognitiveModuleId.ENFOQUE),
            plan.map { it.moduleId },
        )
        assertEquals(RecommendationReason.GATHER_DATA, plan[0].reason)
        assertEquals(RecommendationReason.GATHER_DATA, plan[1].reason)
        assertEquals(RecommendationReason.DEVELOP_AREA, plan[2].reason)
    }

    @Test
    fun respectsMaxItems_andExcludesModulesNotInAvailable() {
        val plan = planner.plan(profileOf(), plannable, maxItems = 2)
        assertEquals(2, plan.size)
        assertTrue(plan.none { it.moduleId == CognitiveModuleId.VELOCIDAD })
    }

    @Test
    fun emptyAvailable_yieldsEmptyPlan() {
        assertTrue(planner.plan(profileOf(), emptySet(), maxItems = 3).isEmpty())
    }
}
