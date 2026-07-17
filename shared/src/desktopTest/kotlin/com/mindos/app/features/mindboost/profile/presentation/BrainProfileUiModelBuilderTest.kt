package com.mindos.app.features.mindboost.profile.presentation

import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionState
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.model.ModuleContribution
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrainProfileUiModelBuilderTest {

    private val aggregator = MindBoostProfileAggregator()

    private fun contrib(module: CognitiveModuleId, score: Double?, conf: Double = 0.8, preliminary: Boolean = true) =
        ModuleContribution(module, score, dataSufficiency = 0.9, confidence = conf, ProfileTrend.IMPROVING, preliminary)

    private fun recommendation(
        module: CognitiveModuleId? = CognitiveModuleId.ENFOQUE,
        reason: RecommendationReason = RecommendationReason.GATHER_DATA,
    ) = TrainingRecommendation(module, module?.primaryDimension, reason)

    private fun ui(contributions: List<ModuleContribution>, rec: TrainingRecommendation = recommendation()) =
        BrainProfileUiModelBuilder.build(aggregator.aggregate("u1", contributions, 1L), rec)

    @Test
    fun calibratingDimension_showsEmDash_neverZero() = run {
        val model = ui(emptyList())
        val attention = model.dimensions.first { it.dimension == CognitiveDimension.ATTENTION }
        assertEquals(DimensionStatus.CALIBRATING, attention.status)
        assertEquals("Calibrando", attention.statusLabel)
        assertEquals("—", attention.displayValue)
        assertNull(attention.score0to100)
        // No calibrating dimension may ever render "0".
        assertTrue(model.dimensions.none { it.status == DimensionStatus.CALIBRATING && it.displayValue == "0" })
    }

    @Test
    fun establishedDimension_showsRoundedScore_andLabel() = run {
        val model = ui(listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.63, conf = 0.8)))
        val speed = model.dimensions.first { it.dimension == CognitiveDimension.PROCESSING_SPEED }
        assertEquals(DimensionStatus.ESTABLISHED, speed.status)
        // "Progresando", never "Establecido"/"Official": no ADR-approved equating exists yet (C2.4 item 5).
        assertEquals("Progresando", speed.statusLabel)
        assertEquals("63", speed.displayValue)
        assertEquals(63, speed.score0to100)
        assertEquals("Velocidad de procesamiento", speed.label)
        assertEquals("Mejorando", speed.trendLabel)
    }

    private fun profileWith(state: DimensionState) = BrainProfileUiModelBuilder.build(
        MindBoostProfile("u1", mapOf(state.dimension to state), overall = null, updatedAtMs = 1L),
        recommendation(),
    )

    private fun reasoning(value: Double?, confidence: Double, status: DimensionStatus) = DimensionState(
        dimension = CognitiveDimension.REASONING,
        value = value,
        confidence = confidence,
        dataSufficiency = 1.0,
        trend = ProfileTrend.STABLE,
        status = status,
        preliminary = status != DimensionStatus.ESTABLISHED,
        contributingModules = emptyList(),
    )

    @Test
    fun emergingBelowDisplayGate_showsEmDash_notNumber() = run {
        // DC-2 / B6: an EMERGING dimension whose confidence is below the display gate must NOT leak a number.
        val model = profileWith(reasoning(value = 0.5, confidence = 0.40, status = DimensionStatus.EMERGING))
        val r = model.dimensions.first { it.dimension == CognitiveDimension.REASONING }
        assertEquals(DimensionStatus.EMERGING, r.status)
        assertEquals("Emergente", r.statusLabel)
        assertEquals("—", r.displayValue)
        assertNull(r.score0to100)
    }

    @Test
    fun emergingAtDisplayGate_showsProvisionalNumber() = run {
        val model = profileWith(reasoning(value = 0.60, confidence = 0.60, status = DimensionStatus.EMERGING))
        val r = model.dimensions.first { it.dimension == CognitiveDimension.REASONING }
        assertEquals(DimensionStatus.EMERGING, r.status)
        assertEquals("60", r.displayValue)
        assertEquals(60, r.score0to100)
    }

    @Test
    fun allEightDimensionsPresent_inEnumOrder() = run {
        val model = ui(emptyList())
        assertEquals(CognitiveDimension.entries, model.dimensions.map { it.dimension })
    }

    @Test
    fun overall_staysWithoutIndex_evenWithThreeEstablished_whenTheGlobalScoreFlagIsOff() = run {
        // C2.5 (SYSTEM-1 Slice 6-R3): the default (OFF) flag means the global NEVER surfaces, no matter how
        // many dimensions reach ESTABLISHED — the exact "an accidental 3rd dimension" gap this closes.
        assertFalse(ui(emptyList()).overall.hasIndex)
        assertEquals("—", ui(emptyList()).overall.indexLabel)

        val model = ui(
            listOf(
                contrib(CognitiveModuleId.VELOCIDAD, 0.6, conf = 0.8),
                contrib(CognitiveModuleId.LOGICA, 0.4, conf = 0.8),
                contrib(CognitiveModuleId.LENGUAJE, 0.8, conf = 0.8),
            ),
        )
        assertFalse(model.overall.hasIndex, "the flag is OFF by default: no composite, official or provisional")
        assertEquals("—", model.overall.indexLabel)
    }

    @Test
    fun overall_showsProvisionalIndexAndCoverage_whenTheGlobalScoreFlagIsOn() = run {
        val flaggedOnAggregator = MindBoostProfileAggregator(globalScoreEnabled = { true })
        val profile = flaggedOnAggregator.aggregate(
            "u1",
            listOf(
                contrib(CognitiveModuleId.VELOCIDAD, 0.6, conf = 0.8),
                contrib(CognitiveModuleId.LOGICA, 0.4, conf = 0.8),
                contrib(CognitiveModuleId.LENGUAJE, 0.8, conf = 0.8),
            ),
            1L,
        )
        val model = BrainProfileUiModelBuilder.build(profile, recommendation())
        assertFalse(model.overall.hasIndex, "§33/§34 D4: coverage-only — no global number even with the flag ON")
        assertEquals("—", model.overall.indexLabel)
        assertContains(model.overall.caption, "Lenguaje") // strongest (0.8) -> product label
        assertContains(model.overall.caption, "Lógica") // developing (0.4, REASONING) -> product label "Lógica"
        // Coverage is exposed explicitly (SCO-014) — the caption can never be mistaken for an official,
        // fully-covered score.
        assertContains(model.overall.caption, "Cobertura provisional")
        assertContains(model.overall.caption, "3 de ${CognitiveDimension.entries.size}")
    }

    @Test
    fun recommendationText_reflectsReason_andModule() = run {
        val gatherDataText = ui(
            emptyList(),
            recommendation(CognitiveModuleId.VELOCIDAD, RecommendationReason.GATHER_DATA),
        ).recommendation.text
        assertContains(gatherDataText, "Velocidad")

        val developAreaText = ui(
            emptyList(),
            recommendation(CognitiveModuleId.LOGICA, RecommendationReason.DEVELOP_AREA),
        ).recommendation.text
        assertContains(developAreaText, "desarrollar")
        assertEquals(
            "Aún no hay módulos disponibles para entrenar.",
            ui(emptyList(), recommendation(null, RecommendationReason.NONE_AVAILABLE)).recommendation.text,
        )
    }

    @Test
    fun carriesNonClinicalDisclaimer() = run {
        val disclaimer = ui(emptyList()).disclaimer
        assertContains(disclaimer, "No es un diagnóstico")
        assertFalse(disclaimer.contains("trastorno"), "must not imply a clinical condition")
    }

    @Test
    fun calibratingDimension_withPartialData_showsProgressTowardFirstReading() = run {
        val profile = MindBoostProfile(
            userId = "u1",
            dimensions = mapOf(
                CognitiveDimension.ATTENTION to DimensionState(
                    dimension = CognitiveDimension.ATTENTION,
                    value = null,
                    confidence = 0.2,
                    dataSufficiency = 0.4,
                    trend = ProfileTrend.UNKNOWN,
                    status = DimensionStatus.CALIBRATING,
                    preliminary = true,
                    contributingModules = emptyList(),
                ),
            ),
            overall = null,
            updatedAtMs = 1L,
        )
        val attention = BrainProfileUiModelBuilder.build(profile, recommendation())
            .dimensions.first { it.dimension == CognitiveDimension.ATTENTION }
        // Honesty invariant preserved: still "—", no polygon vertex.
        assertEquals("—", attention.displayValue)
        assertNull(attention.score0to100)
        // NEW: progress toward the first reading is surfaced instead of a bare dash.
        assertEquals(40, attention.calibrationProgress)
        assertContains(attention.calibrationLabel, "40%")
    }

    @Test
    fun brandNewCalibrating_promptsToStart_measuredDimensionHasNoProgress() = run {
        val calibrating = ui(emptyList()).dimensions.first { it.dimension == CognitiveDimension.ATTENTION }
        assertEquals(0, calibrating.calibrationProgress)
        assertTrue(calibrating.calibrationLabel.isNotEmpty())

        val speed = ui(listOf(contrib(CognitiveModuleId.VELOCIDAD, 0.63)))
            .dimensions.first { it.dimension == CognitiveDimension.PROCESSING_SPEED }
        assertNull(speed.calibrationProgress)
        assertEquals("", speed.calibrationLabel)
    }
}
