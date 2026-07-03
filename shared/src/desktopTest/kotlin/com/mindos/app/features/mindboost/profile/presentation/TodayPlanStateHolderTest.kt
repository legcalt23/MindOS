package com.mindos.app.features.mindboost.profile.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.profile.data.InMemoryMindBoostProfileRepository
import com.mindos.app.features.mindboost.profile.domain.application.MindBoostProfileService
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TodayPlanStateHolderTest {

    private fun calibrating(dimension: CognitiveDimension, sufficiency: Double) = DimensionState(
        dimension = dimension, value = null, confidence = 0.0, dataSufficiency = sufficiency,
        trend = ProfileTrend.UNKNOWN, status = DimensionStatus.CALIBRATING, preliminary = true,
        contributingModules = emptyList(),
    )

    @Test
    fun load_usesCachedProfile_andRanksPlanByMostDataFirst() = runTest {
        val repo = InMemoryMindBoostProfileRepository()
        repo.save(
            MindBoostProfile(
                userId = "u1",
                dimensions = mapOf(
                    CognitiveDimension.ATTENTION to calibrating(CognitiveDimension.ATTENTION, 0.2),
                    CognitiveDimension.WORKING_MEMORY to calibrating(CognitiveDimension.WORKING_MEMORY, 0.8),
                ),
                overall = null,
                updatedAtMs = 1L,
            ),
        )
        val service = MindBoostProfileService(
            sources = emptyList(),
            repository = repo,
            clock = EpochClock { 0L },
        )
        val holder = TodayPlanStateHolder(
            service = service,
            userId = "u1",
            scope = this,
            plannable = setOf(CognitiveModuleId.ENFOQUE, CognitiveModuleId.MEMORIA),
        )

        holder.load()
        advanceUntilIdle()

        assertEquals(
            listOf(CognitiveModuleId.MEMORIA, CognitiveModuleId.ENFOQUE),
            holder.plan.map { it.moduleId },
        )
        coroutineContext.cancelChildren()
    }
}
