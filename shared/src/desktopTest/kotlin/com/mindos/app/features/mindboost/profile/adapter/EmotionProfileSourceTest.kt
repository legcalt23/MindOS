package com.mindos.app.features.mindboost.profile.adapter

import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileReadOnly
import com.mindos.app.features.mindboost.emotions.profile.EmotionProfileSnapshot
import com.mindos.app.features.mindboost.profile.data.adapter.EmotionProfileSource
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend as EmotionTrend

private class FakeEmotionReadOnly(private val snap: EmotionProfileSnapshot?) : EmotionProfileReadOnly {
    override suspend fun snapshot(userId: String): EmotionProfileSnapshot? = snap
}

class EmotionProfileSourceTest {

    @Test
    fun noSnapshot_returnsNullContribution() = runTest {
        val source = EmotionProfileSource(FakeEmotionReadOnly(null))
        assertNull(source.contribution("u1"))
    }

    @Test
    fun snapshot_mapsToNullScoreContribution_passingThroughSufficiencyConfidenceTrend() = runTest {
        val source = EmotionProfileSource(
            FakeEmotionReadOnly(
                EmotionProfileSnapshot(
                    tier = ProfileTier.INITIAL_TREND,
                    dataSufficiency = 0.5,
                    confidence = 0.5,
                    trend = EmotionTrend.STEADY,
                ),
            ),
        )
        val contribution = source.contribution("u1")!!
        assertEquals(CognitiveModuleId.EMOCIONES, contribution.moduleId)
        assertNull(contribution.normalizedScore, "tier is data-maturity, never a quality score")
        assertEquals(0.5, contribution.dataSufficiency, 1e-9)
        assertEquals(0.5, contribution.confidence, 1e-9)
        assertEquals(ProfileTrend.STABLE, contribution.trend) // STEADY maps to STABLE
        assertTrue(contribution.preliminary) // tier != STABLE
        assertNull(contribution.updatedAtMs, "read-only surface exposes no event time → null, never a fabricated 0L")
    }

    @Test
    fun stableTier_isNotPreliminary() = runTest {
        val source = EmotionProfileSource(
            FakeEmotionReadOnly(
                EmotionProfileSnapshot(ProfileTier.STABLE, dataSufficiency = 1.0, confidence = 1.0, trend = EmotionTrend.IMPROVING),
            ),
        )
        val contribution = source.contribution("u1")!!
        assertFalse(contribution.preliminary)
        assertEquals(ProfileTrend.IMPROVING, contribution.trend)
    }

    @Test
    fun stableTierWithFullData_stillHasNullScore_neverFabricatesCompetence() = runTest {
        // The key anti-fabrication guard: even at the TOP tier with full sufficiency/confidence (a
        // "complete/sufficient" history), the regulation score stays null — maturity is never competence.
        val source = EmotionProfileSource(
            FakeEmotionReadOnly(
                EmotionProfileSnapshot(ProfileTier.STABLE, dataSufficiency = 1.0, confidence = 1.0, trend = EmotionTrend.IMPROVING),
            ),
        )
        assertNull(source.contribution("u1")!!.normalizedScore, "STABLE/full data must NOT become a 0..1 score")
    }

    @Test
    fun lowData_abandonedOrInsufficient_isNonPunitive_nullScoreNeverNegative() = runTest {
        // Regla #8 at the boundary: an abandoned/incomplete/insufficient history (low sufficiency) yields a
        // null score — NEVER a zero or a low/negative regulation value. Abandonment cannot penalize.
        val source = EmotionProfileSource(
            FakeEmotionReadOnly(
                EmotionProfileSnapshot(ProfileTier.EXPLORATORY, dataSufficiency = 0.1, confidence = 0.1, trend = EmotionTrend.UNKNOWN),
            ),
        )
        val contribution = source.contribution("u1")!!
        assertNull(contribution.normalizedScore, "insufficient/abandoned data is null, never a penalty score")
        assertTrue(contribution.preliminary)
        assertEquals(0.1, contribution.dataSufficiency, 1e-9) // sufficiency passes through; it is not a score
    }
}
