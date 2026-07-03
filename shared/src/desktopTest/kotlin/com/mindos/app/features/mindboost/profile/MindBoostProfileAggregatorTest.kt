package com.mindos.app.features.mindboost.profile

import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MindBoostProfileAggregatorTest {

    private val aggregator = MindBoostProfileAggregator()

    private fun contrib(
        module: CognitiveModuleId,
        score: Double?,
        sufficiency: Double = 0.9,
        confidence: Double = 0.8,
        trend: ProfileTrend = ProfileTrend.STABLE,
        preliminary: Boolean = false,
    ) = ModuleContribution(module, score, sufficiency, confidence, trend, preliminary, updatedAtMs = null)

    @Test
    fun emptyContributions_allDimensionsCalibrating_noComposite() {
        val profile = aggregator.aggregate("u1", emptyList(), nowMs = 100L)
        assertEquals(CognitiveDimension.entries.size, profile.dimensions.size)
        assertTrue(profile.dimensions.values.all { it.status == DimensionStatus.CALIBRATING && it.value == null })
        assertNull(profile.overall?.compositeIndex)
        assertEquals(0, profile.overall?.establishedDimensionCount)
        assertEquals(100L, profile.updatedAtMs)
    }

    @Test
    fun nullContributionTimestamp_doesNotBlockAggregation_profileStampComesFromClock() {
        // A qualifying contribution whose event time is unknown (null) must still aggregate; the profile's
        // own updatedAtMs comes from the injected clock (nowMs), NEVER fabricated from the null time (Fase 1.7).
        val c = contrib(CognitiveModuleId.VELOCIDAD, 0.7, confidence = 0.8) // updatedAtMs defaults to null now
        val profile = aggregator.aggregate("u1", listOf(c), nowMs = 555L)
        assertEquals(DimensionStatus.ESTABLISHED, profile.dimension(CognitiveDimension.PROCESSING_SPEED).status)
        assertEquals(555L, profile.updatedAtMs, "profile stamp = injected clock, never derived from a null time")
    }

    @Test
    fun highConfidenceContribution_isEstablished_withItsValue() {
        val profile = aggregator.aggregate("u1", listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.7, confidence = 0.8)), 1L)
        val state = profile.dimension(CognitiveDimension.PROCESSING_SPEED)
        assertEquals(DimensionStatus.ESTABLISHED, state.status)
        assertEquals(0.7, state.value!!, 1e-9)
        assertEquals(listOf(CognitiveModuleId.VELOCIDAD), state.contributingModules)
    }

    @Test
    fun lowConfidenceButQualifying_isEmerging() {
        val profile = aggregator.aggregate("u1", listOf(contrib(CognitiveModuleId.LOGICA, 0.5, confidence = 0.5)), 1L)
        assertEquals(DimensionStatus.EMERGING, profile.dimension(CognitiveDimension.REASONING).status)
    }

    @Test
    fun belowGate_contributionIsIgnored_dimensionStaysCalibrating() {
        val weak = contrib(CognitiveModuleId.ENFOQUE, 0.9, sufficiency = 0.3, confidence = 0.2)
        val profile = aggregator.aggregate("u1", listOf(weak), 1L)
        assertEquals(DimensionStatus.CALIBRATING, profile.dimension(CognitiveDimension.ATTENTION).status)
        assertNull(profile.dimension(CognitiveDimension.ATTENTION).value)
    }

    @Test
    fun nullScore_isIgnored_evenWithHighConfidence() {
        val profile = aggregator.aggregate("u1", listOf(contrib(CognitiveModuleId.MEMORIA, null, confidence = 0.95)), 1L)
        assertEquals(DimensionStatus.CALIBRATING, profile.dimension(CognitiveDimension.WORKING_MEMORY).status)
    }

    @Test
    fun multipleContributions_useConfidenceSufficiencyWeightedMean() {
        val c1 = contrib(CognitiveModuleId.ENFOQUE, 0.4, sufficiency = 1.0, confidence = 0.9)
        val c2 = contrib(CognitiveModuleId.ENFOQUE, 0.8, sufficiency = 0.6, confidence = 0.6)
        val profile = aggregator.aggregate("u1", listOf(c1, c2), 1L)
        val expected = (0.4 * 0.9 * 1.0 + 0.8 * 0.6 * 0.6) / (0.9 * 1.0 + 0.6 * 0.6)
        val state = profile.dimension(CognitiveDimension.ATTENTION)
        assertEquals(expected, state.value!!, 1e-9)
        assertEquals(0.9, state.confidence, 1e-9) // max confidence
    }

    @Test
    fun trend_comesFromHighestConfidenceContributor() {
        val c1 = contrib(CognitiveModuleId.ENFOQUE, 0.5, confidence = 0.9, trend = ProfileTrend.IMPROVING)
        val c2 = contrib(CognitiveModuleId.ENFOQUE, 0.5, confidence = 0.6, trend = ProfileTrend.DECLINING)
        val profile = aggregator.aggregate("u1", listOf(c1, c2), 1L)
        assertEquals(ProfileTrend.IMPROVING, profile.dimension(CognitiveDimension.ATTENTION).trend)
    }

    @Test
    fun preliminaryPropagatesWhenAnyContributorIsPreliminary() {
        val profile = aggregator.aggregate(
            "u1",
            listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.6, confidence = 0.8, preliminary = true)),
            1L,
        )
        assertTrue(profile.dimension(CognitiveDimension.PROCESSING_SPEED).preliminary)
    }

    @Test
    fun composite_isNullUntilEnoughEstablished_thenAveragesThem() {
        val twoEstablished = listOf(
            contrib(CognitiveModuleId.VELOCIDAD, 0.6, confidence = 0.8),
            contrib(CognitiveModuleId.LOGICA, 0.4, confidence = 0.8),
        )
        assertNull(aggregator.aggregate("u1", twoEstablished, 1L).overall?.compositeIndex)

        val threeEstablished = twoEstablished + contrib(CognitiveModuleId.LENGUAJE, 0.8, confidence = 0.8)
        val overall = aggregator.aggregate("u1", threeEstablished, 1L).overall!!
        assertEquals((0.6 + 0.4 + 0.8) / 3.0, overall.compositeIndex!!, 1e-9)
        assertEquals(3, overall.establishedDimensionCount)
        assertEquals(CognitiveDimension.VERBAL, overall.strongest)
        assertEquals(CognitiveDimension.REASONING, overall.developing)
    }
}
