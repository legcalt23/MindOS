package com.mindos.app.features.mindboost.profile.presentation

import com.mindos.app.features.mindboost.profile.data.InMemoryMindBoostProfileRepository
import com.mindos.app.features.mindboost.profile.domain.application.MindBoostProfileService
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

private class FixedSource(private var contribution: ModuleContribution?) : CognitiveModuleProfileSource {
    override val moduleId: CognitiveModuleId = CognitiveModuleId.VELOCIDAD
    override suspend fun contribution(userId: String): ModuleContribution? = contribution
    fun set(c: ModuleContribution?) { contribution = c }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MindBoostBrainProfileStateHolderTest {

    private fun service(source: FixedSource) =
        MindBoostProfileService(listOf(source), InMemoryMindBoostProfileRepository(), clock = { 1L })

    private fun contrib(score: Double?, conf: Double = 0.8) =
        ModuleContribution(CognitiveModuleId.VELOCIDAD, score, 0.9, conf, ProfileTrend.STABLE, preliminary = true)

    @Test
    fun start_buildsUiModelFromLiveSignals_andClearsLoading() = runTest {
        val source = FixedSource(contrib(0.7))
        val holder = MindBoostBrainProfileStateHolder(service(source), "u1", this)
        holder.start()
        advanceUntilIdle()

        val model = assertNotNull(holder.uiModel)
        val speed = model.dimensions.first { it.dimension == CognitiveDimension.PROCESSING_SPEED }
        assertEquals("70", speed.displayValue)
        assertFalse(holder.isLoading)
        coroutineContext.cancelChildren()
    }

    @Test
    fun refresh_recomputesAfterNewData() = runTest {
        val source = FixedSource(contrib(null)) // no usable score yet
        val holder = MindBoostBrainProfileStateHolder(service(source), "u1", this)
        holder.start()
        advanceUntilIdle()
        assertEquals("—", holder.uiModel!!.dimensions.first { it.dimension == CognitiveDimension.PROCESSING_SPEED }.displayValue)

        source.set(contrib(0.42))
        holder.refresh()
        advanceUntilIdle()
        assertEquals("42", holder.uiModel!!.dimensions.first { it.dimension == CognitiveDimension.PROCESSING_SPEED }.displayValue)
        coroutineContext.cancelChildren()
    }
}
