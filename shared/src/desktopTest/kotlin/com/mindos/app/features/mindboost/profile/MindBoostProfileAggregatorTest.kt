package com.mindos.app.features.mindboost.profile

import com.mindos.app.core.config.DefaultFeatureFlags
import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.core.config.StaticFeatureFlags
import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MindBoostProfileAggregatorTest {

    private val aggregator = MindBoostProfileAggregator()

    // FeatureFlagsHub is process-global; reset it so cross-test ordering can't leak state (mirrors
    // FeatureFlagsTest's convention).
    @AfterTest
    fun resetHub() {
        FeatureFlagsHub.current = DefaultFeatureFlags
    }

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
    fun composite_isNullUntilEnoughEstablished_andStaysNullWithTheFlagOff() {
        // C2.5 (SYSTEM-1 Slice 6-R3): with the default (OFF) flag, compositeIndex stays null EVEN with 3
        // established dimensions — the pre-C2.5 behaviour (auto-surfacing a composite once 3 dims cleared
        // ESTABLISHED) is exactly the barrier this closes.
        val twoEstablished = listOf(
            contrib(CognitiveModuleId.VELOCIDAD, 0.6, confidence = 0.8),
            contrib(CognitiveModuleId.LOGICA, 0.4, confidence = 0.8),
        )
        assertNull(aggregator.aggregate("u1", twoEstablished, 1L).overall?.compositeIndex)

        val threeEstablished = twoEstablished + contrib(CognitiveModuleId.LENGUAJE, 0.8, confidence = 0.8)
        val overall = aggregator.aggregate("u1", threeEstablished, 1L).overall!!
        assertNull(overall.compositeIndex, "official composite requires ADR_SCI_006_APPROVED, hardcoded false")
        assertNull(overall.provisionalIndex, "provisional composite requires the flag ON")
        assertEquals(3, overall.establishedDimensionCount)
        assertEquals(CognitiveDimension.VERBAL, overall.strongest)
        assertEquals(CognitiveDimension.REASONING, overall.developing)
    }

    @Test
    fun provisionalComposite_appearsOnlyWithTheFlagOn_andExposesCoverage() {
        val flaggedOn = MindBoostProfileAggregator(globalScoreEnabled = { true })
        val threeEstablished = listOf(
            contrib(CognitiveModuleId.VELOCIDAD, 0.6, confidence = 0.8),
            contrib(CognitiveModuleId.LOGICA, 0.4, confidence = 0.8),
            contrib(CognitiveModuleId.LENGUAJE, 0.8, confidence = 0.8),
        )
        val overall = flaggedOn.aggregate("u1", threeEstablished, 1L).overall!!
        assertNull(overall.compositeIndex, "official composite stays unreachable regardless of the flag")
        val provisional = requireNotNull(overall.provisionalIndex)
        // §33/§34 D4: coverage-only — the provisional view carries NO composite number (no `.value`).
        assertEquals(3, provisional.coveredDimensions.size)
        assertTrue(CognitiveDimension.PROCESSING_SPEED in provisional.coveredDimensions)
        assertTrue(CognitiveDimension.REASONING in provisional.coveredDimensions)
        assertTrue(CognitiveDimension.VERBAL in provisional.coveredDimensions)
        // Coverage/missing are exposed EXPLICITLY (SCO-014) — never silently partial.
        assertEquals(CognitiveDimension.entries.size - 3, provisional.missingDimensions.size)
        assertTrue(CognitiveDimension.ATTENTION in provisional.missingDimensions)
    }

    @Test
    fun defaultAggregator_readsTheLiveFeatureFlagsHub() {
        // Verifies the DEFAULT constructor param actually reads FeatureFlagsHub (not just the injectable
        // seam) — the literal instruction: "Lee el flag vía FeatureFlagsHub.current".
        val threeEstablished = listOf(
            contrib(CognitiveModuleId.VELOCIDAD, 0.6, confidence = 0.8),
            contrib(CognitiveModuleId.LOGICA, 0.4, confidence = 0.8),
            contrib(CognitiveModuleId.LENGUAJE, 0.8, confidence = 0.8),
        )
        assertNull(aggregator.aggregate("u1", threeEstablished, 1L).overall?.provisionalIndex)

        FeatureFlagsHub.current = StaticFeatureFlags(mindboostGlobalScoreEnabled = true)
        val hubAggregator = MindBoostProfileAggregator()
        assertTrue(hubAggregator.aggregate("u1", threeEstablished, 1L).overall!!.provisionalIndex != null)
    }

    @Test
    fun registeringMoreEstablishedDimensions_neverActivatesTheGlobalAlone_flagOffStaysNull() {
        // Simulates a future world where every module's item bank is registered/validated and ALL 9
        // dimensions reach ESTABLISHED (the exact "an accidental 3rd/Nth dimension" scenario efficacy-guardian
        // flagged, item 6 of §6.2) — with the flag at its default (OFF), the global stays fully unreachable.
        val allEstablished = listOf(
            contrib(CognitiveModuleId.ENFOQUE, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.VELOCIDAD, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.MEMORIA, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.MEMORIA_EPISODICA, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.LOGICA, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.LENGUAJE, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.DECISIONES, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.EMOCIONES, 0.5, confidence = 0.9),
            contrib(CognitiveModuleId.CREATIVIDAD, 0.5, confidence = 0.9),
        )
        val overall = aggregator.aggregate("u1", allEstablished, 1L).overall!!
        assertEquals(CognitiveDimension.entries.size, overall.establishedDimensionCount)
        assertNull(overall.compositeIndex, "official stays blocked by ADR_SCI_006_APPROVED even with 9/9")
        assertNull(overall.provisionalIndex, "provisional stays blocked by the flag even with 9/9")
    }

    @Test
    fun officialComposite_stillUnreachable_evenWithAllNineEstablished_andTheFlagOn() {
        // ADR_SCI_006_APPROVED is a code constant, never a flag: turning the flag ON only ever unlocks the
        // PROVISIONAL tier, never the official one, no matter how many dimensions are ESTABLISHED.
        val flaggedOn = MindBoostProfileAggregator(globalScoreEnabled = { true })
        val allEstablished = CognitiveModuleId.entries.map { contrib(it, 0.5, confidence = 0.9) }
        val overall = flaggedOn.aggregate("u1", allEstablished, 1L).overall!!
        assertEquals(CognitiveDimension.entries.size, overall.establishedDimensionCount)
        assertNull(overall.compositeIndex)
        assertTrue(overall.provisionalIndex != null)
        assertTrue(overall.provisionalIndex!!.missingDimensions.isEmpty())
    }

    @Test
    fun composite_averagesOnlyEstablished_excludesEmergingWithAValue() {
        // Locks the honest Global-Score invariant: an EMERGING dimension holds a real value but must NOT
        // enter the composite. OverallSummary is computed over ESTABLISHED dimensions only; if summarize()
        // ever started including EMERGING, this catches it (the 2-vs-3 test above would not, as it adds
        // no EMERGING dimension with a value). Uses the flag-ON injectable seam so the provisional value is
        // actually computed (the flag-OFF default would just leave both indices null).
        val flaggedOn = MindBoostProfileAggregator(globalScoreEnabled = { true })
        val contributions = listOf(
            contrib(CognitiveModuleId.VELOCIDAD, 0.6, confidence = 0.8), // ESTABLISHED
            contrib(CognitiveModuleId.LOGICA, 0.4, confidence = 0.8), // ESTABLISHED
            contrib(CognitiveModuleId.LENGUAJE, 0.8, confidence = 0.8), // ESTABLISHED
            contrib(CognitiveModuleId.ENFOQUE, 0.2, confidence = 0.5), // EMERGING (value present, excluded)
        )
        val profile = flaggedOn.aggregate("u1", contributions, 1L)
        val attention = profile.dimension(CognitiveDimension.ATTENTION)
        assertEquals(DimensionStatus.EMERGING, attention.status)
        assertEquals(0.2, attention.value!!, 1e-9)
        val overall = profile.overall!!
        assertEquals(3, overall.establishedDimensionCount)
        assertNull(overall.compositeIndex, "official stays blocked by ADR_SCI_006_APPROVED")
        // §33/§34 D4: coverage-only. The EMERGING dimension (ATTENTION) must NOT enter the covered set — only the
        // three ESTABLISHED dimensions are covered (never a 4-way anything).
        val provisional = overall.provisionalIndex!!
        assertEquals(3, provisional.coveredDimensions.size)
        assertTrue(CognitiveDimension.ATTENTION !in provisional.coveredDimensions)
    }
}
