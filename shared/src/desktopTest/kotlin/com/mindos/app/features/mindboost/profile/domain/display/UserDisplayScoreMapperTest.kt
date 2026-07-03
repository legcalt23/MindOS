package com.mindos.app.features.mindboost.profile.domain.display

import com.mindos.app.features.mindboost.core.scoring.ScoringVersionRegistry
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fase 1.1 — the display frontier (B6). The mapper is the ONLY path from the internal profile to a visible
 * number, and it must never fabricate one: `Withheld` = "—", never 0. It gates on DimensionStatus +
 * confidence ONLY (DC-2), never on session count.
 */
class UserDisplayScoreMapperTest {

    private fun dim(status: DimensionStatus, value: Double?, confidence: Double) = DimensionState(
        dimension = CognitiveDimension.REASONING,
        value = value,
        confidence = confidence,
        dataSufficiency = 1.0,
        trend = ProfileTrend.STABLE,
        status = status,
        preliminary = status != DimensionStatus.ESTABLISHED,
        contributingModules = emptyList(),
    )

    private fun signal(status: DimensionStatus, value: Double?, confidence: Double) =
        ProfileDimensionSignal.fromDimensionState(dim(status, value, confidence))

    // --- The sealed boundary: raw/procedural/invalid signals cannot become a displayable signal ---

    @Test
    fun fromDimensionState_isNull_whenValueIsNull() {
        assertNull(
            signal(DimensionStatus.CALIBRATING, value = null, confidence = 0.0),
            "a CALIBRATING / value-null dimension (procedural feedback, invalid internal score) is not displayable",
        )
    }

    @Test
    fun fromDimensionState_carriesOnlyStatusAndConfidence_whenValuePresent() {
        val s = requireNotNull(signal(DimensionStatus.EMERGING, value = 0.7, confidence = 0.6))
        assertEquals(0.7, s.normalized)
        assertEquals(DimensionStatus.EMERGING, s.status)
        assertEquals(0.6, s.confidence)
    }

    // --- DC-2 gate ---

    @Test
    fun withheld_whenNoSignal() {
        assertEquals(DisplayScore.Withheld, UserDisplayScoreMapper.displayScore(null))
    }

    @Test
    fun established_isOfficial_roundedTo0to100() {
        assertEquals(DisplayScore.Official(63), UserDisplayScoreMapper.displayScore(signal(DimensionStatus.ESTABLISHED, 0.63, 0.9)))
    }

    @Test
    fun emergingAtGate_isProvisional() {
        assertEquals(DisplayScore.Provisional(60), UserDisplayScoreMapper.displayScore(signal(DimensionStatus.EMERGING, 0.60, 0.55)))
    }

    @Test
    fun emergingBelowGate_isWithheld_neverANumber() {
        assertEquals(DisplayScore.Withheld, UserDisplayScoreMapper.displayScore(signal(DimensionStatus.EMERGING, 0.70, 0.54)))
    }

    // --- Property: NEVER an integer unless (ESTABLISHED) OR (EMERGING && confidence >= 0.55) ---

    @Test
    fun property_noIntegerUnlessGatePasses() {
        val confidences = listOf(0.0, 0.3, 0.54, 0.55, 0.7, 1.0)
        val values = listOf(0.0, 0.25, 0.5, 0.9, 1.0)
        for (status in listOf(DimensionStatus.EMERGING, DimensionStatus.ESTABLISHED)) {
            for (c in confidences) {
                for (v in values) {
                    val display = UserDisplayScoreMapper.displayScore(signal(status, v, c))
                    val showsInteger = display is DisplayScore.Official || display is DisplayScore.Provisional
                    val gatePasses = status == DimensionStatus.ESTABLISHED || c >= UserDisplayScoreMapper.DISPLAY_CONFIDENCE_GATE
                    assertEquals(gatePasses, showsInteger, "status=$status confidence=$c: integer shown must equal gatePasses")
                }
            }
        }
    }

    @Test
    fun property_percentIsMonotonicNonDecreasing_andBounded() {
        var prev = -1
        for (v in listOf(0.0, 0.1, 0.5, 0.999, 1.0)) {
            val official = UserDisplayScoreMapper.displayScore(signal(DimensionStatus.ESTABLISHED, v, 0.9)) as DisplayScore.Official
            assertTrue(official.value0to100 in 0..100, "percent must be in [0,100]")
            assertTrue(official.value0to100 >= prev, "percent must be monotonic non-decreasing in normalized")
            prev = official.value0to100
        }
    }

    // --- Boundary: maturity is NOT decided by session count; only DimensionStatus + confidence ---

    @Test
    fun gateBoundary_isConfidenceOnly_forEmerging() {
        // The signal type carries no session field at all; crossing 0.55 confidence is the ONLY thing that
        // flips an EMERGING dimension from Withheld to Provisional. Pins that the display gate does not
        // infer maturity from how many sessions were played (that is upstream calibration, DC-3).
        assertEquals(DisplayScore.Withheld, UserDisplayScoreMapper.displayScore(signal(DimensionStatus.EMERGING, 0.8, 0.5499)))
        assertEquals(DisplayScore.Provisional(80), UserDisplayScoreMapper.displayScore(signal(DimensionStatus.EMERGING, 0.8, 0.5500)))
    }

    @Test
    fun version_isPinnedFromTheRegistry_notAScatteredString() {
        assertEquals(ScoringVersionRegistry.USER_DISPLAY_SCORE_MAPPER, UserDisplayScoreMapper.version)
    }
}
