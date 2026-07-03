package com.mindos.app.features.mindboost.profile

import com.mindos.app.features.mindboost.profile.data.InMemoryMindBoostProfileRepository
import com.mindos.app.features.mindboost.profile.domain.application.MindBoostProfileService
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import com.mindos.app.features.mindboost.profile.domain.source.CognitiveModuleProfileSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSource(
    override val moduleId: CognitiveModuleId,
    private val contribution: ModuleContribution?,
    private val fail: Boolean = false,
) : CognitiveModuleProfileSource {
    override suspend fun contribution(userId: String): ModuleContribution? {
        if (fail) error("source boom")
        return contribution
    }
}

class MindBoostProfileServiceTest {

    private fun contrib(module: CognitiveModuleId, score: Double?, conf: Double = 0.8) =
        ModuleContribution(module, score, dataSufficiency = 0.9, confidence = conf, ProfileTrend.STABLE, preliminary = false)

    @Test
    fun refresh_aggregatesLiveSources_andPersists() = runTest {
        val repo = InMemoryMindBoostProfileRepository()
        val service = MindBoostProfileService(
            sources = listOf(
                FakeSource(CognitiveModuleId.VELOCIDAD, contrib(CognitiveModuleId.VELOCIDAD, 0.7)),
                FakeSource(CognitiveModuleId.LOGICA, contrib(CognitiveModuleId.LOGICA, 0.4)),
            ),
            repository = repo,
            clock = { 999L },
        )

        val profile = service.refresh("u1")
        assertEquals(0.7, profile.dimension(CognitiveDimension.PROCESSING_SPEED).value!!, 1e-9)
        assertEquals(999L, profile.updatedAtMs)
        // persisted: cached returns the same snapshot
        assertEquals(profile, service.cached("u1"))
    }

    @Test
    fun refresh_isolatesAFailingSource_othersStillContribute() = runTest {
        val repo = InMemoryMindBoostProfileRepository()
        val service = MindBoostProfileService(
            sources = listOf(
                FakeSource(CognitiveModuleId.VELOCIDAD, contrib(CognitiveModuleId.VELOCIDAD, 0.7)),
                FakeSource(CognitiveModuleId.LOGICA, contribution = null, fail = true),
            ),
            repository = repo,
            clock = { 1L },
        )

        val profile = service.refresh("u1")
        assertEquals(DimensionStatus.ESTABLISHED, profile.dimension(CognitiveDimension.PROCESSING_SPEED).status)
        assertEquals(DimensionStatus.CALIBRATING, profile.dimension(CognitiveDimension.REASONING).status)
    }

    @Test
    fun refresh_withNoData_persistsAllCalibrating() = runTest {
        val repo = InMemoryMindBoostProfileRepository()
        val service = MindBoostProfileService(
            sources = listOf(FakeSource(CognitiveModuleId.ENFOQUE, contribution = null)),
            repository = repo,
            clock = { 1L },
        )
        val profile = service.refresh("u1")
        assertTrue(profile.dimensions.values.all { it.status == DimensionStatus.CALIBRATING })
        assertNull(profile.overall?.compositeIndex)
    }

    @Test
    fun recommend_isLimitedToWiredModules_andGathersDataFirst() = runTest {
        val repo = InMemoryMindBoostProfileRepository()
        val service = MindBoostProfileService(
            sources = listOf(
                FakeSource(CognitiveModuleId.VELOCIDAD, contrib(CognitiveModuleId.VELOCIDAD, 0.7)),
                FakeSource(CognitiveModuleId.ENFOQUE, contribution = null), // calibrating
            ),
            repository = repo,
            clock = { 1L },
        )
        val profile = service.refresh("u1")
        val rec = service.recommend(profile)
        assertEquals(CognitiveModuleId.ENFOQUE, rec.moduleId)
        assertEquals(RecommendationReason.GATHER_DATA, rec.reason)
    }
}
