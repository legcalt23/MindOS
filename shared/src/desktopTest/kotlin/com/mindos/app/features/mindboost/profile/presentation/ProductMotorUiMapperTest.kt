package com.mindos.app.features.mindboost.profile.presentation

import com.mindos.app.features.mindboost.profile.domain.aggregation.MindBoostProfileAggregator
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.CognitiveModuleId
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.domain.orchestration.RecommendationReason
import com.mindos.app.features.mindboost.profile.domain.orchestration.TrainingRecommendation
import com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi
import com.mindos.app.features.mindboost.profile.presentation.state.ProductMotor
import com.mindos.app.features.mindboost.profile.presentation.state.displayLabel
import com.mindos.app.features.mindboost.profile.presentation.state.displaySymbol
import com.mindos.app.features.mindboost.profile.presentation.state.toProductMotorLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the 9-internal-dimensions -> 8-product-motors presentation fold. Protects the CEO
 * invariants: 8 visible motors in PRD order, Memoria folded (min maturity, weighted mean), null != 0,
 * and safe degradation when a dimension is missing.
 */
class ProductMotorUiMapperTest {

    private val WM = CognitiveDimension.WORKING_MEMORY
    private val EM = CognitiveDimension.EPISODIC_MEMORY
    private val PS = CognitiveDimension.PROCESSING_SPEED
    private val CAL = DimensionStatus.CALIBRATING
    private val EMERG = DimensionStatus.EMERGING
    private val EST = DimensionStatus.ESTABLISHED

    private fun dim(
        dimension: CognitiveDimension,
        status: DimensionStatus = CAL,
        score: Int? = null,
        weight: Double = 0.72,
    ) = DimensionUi(
        dimension = dimension,
        label = dimension.displayLabel(),
        status = status,
        statusLabel = status.displayLabel(),
        displayValue = score?.toString() ?: "—",
        score0to100 = score,
        trend = ProfileTrend.STABLE,
        trendLabel = ProfileTrend.STABLE.displayLabel(),
        trendSymbol = ProfileTrend.STABLE.displaySymbol(),
        preliminary = status != EST,
        calibrationProgress = if (status == CAL) 0 else null,
        calibrationLabel = "",
        combineWeight = weight,
    )

    /** A full 9-dimension input (all CALIBRATING by default), with the given overrides applied. */
    private fun nine(vararg overrides: Pair<CognitiveDimension, DimensionUi>): List<DimensionUi> {
        val byDimension = overrides.toMap()
        return CognitiveDimension.entries.map { byDimension[it] ?: dim(it) }
    }

    /** Fold a two-memory setup and return the combined Memoria motor. */
    private fun memoria(
        wStatus: DimensionStatus,
        wScore: Int?,
        eStatus: DimensionStatus,
        eScore: Int?,
        wWeight: Double = 0.72,
        eWeight: Double = 0.72,
    ) = ProductMotorUiMapper
        .toProductMotors(nine(WM to dim(WM, wStatus, wScore, wWeight), EM to dim(EM, eStatus, eScore, eWeight)))
        .first { it.motor == ProductMotor.MEMORIA }

    // 1 — count: 9 internal -> 8 visible, exactly one Memoria, no separate working/episodic motors
    @Test
    fun foldsNineDimensionsIntoEightMotors_withExactlyOneMemoriaBlock() {
        val motors = ProductMotorUiMapper.toProductMotors(nine())
        assertEquals(8, motors.size)
        assertEquals(1, motors.count { it.motor == ProductMotor.MEMORIA })
        assertEquals(1, motors.count { it.label == "Memoria" })
        assertTrue(motors.none { it.label == "Memoria de trabajo" || it.label == "Memoria episódica" })
    }

    // 2 — both memories calibrating -> CALIBRATING, null, "—", never "0"
    @Test
    fun bothMemoriesCalibrating_memoriaCalibrating_dashNeverZero() {
        val m = memoria(CAL, null, CAL, null)
        assertEquals(CAL, m.status)
        assertNull(m.score0to100)
        assertEquals("—", m.displayValue)
        assertNotEquals("0", m.displayValue)
    }

    // 3 — one value, one calibrating -> minimum status, available signal, absent sub is "—"
    @Test
    fun oneMemoryWithValue_statusIsMinimum_showsOnlyAvailableSignal_absentSubIsDash() {
        val m = memoria(EMERG, 70, CAL, null)
        assertEquals(CAL, m.status)
        assertNotEquals(0, m.score0to100)
        assertEquals(70, m.score0to100)
        assertEquals("70", m.submetrics.first { it.label == "Trabajo" }.displayValue)
        assertEquals("—", m.submetrics.first { it.label == "Episódica" }.displayValue)
    }

    // 4 — both established -> combined score within the sub-metric range, no sub-metric lost
    @Test
    fun bothMemoriesEstablished_memoriaEstablished_scoreWithinRange_noSubmetricLost() {
        val m = memoria(EST, 80, EST, 60, wWeight = 0.9, eWeight = 0.9)
        assertEquals(EST, m.status)
        assertTrue(m.score0to100 in 60..80)
        assertEquals(2, m.submetrics.size)
    }

    // 5 — a MEMORIA_EPISODICA recommendation reads "Memoria", never "Memoria episódica"
    @Test
    fun recommendation_forEpisodicMemory_showsMemoria_notEpisodicAsAMotor() {
        val model = BrainProfileUiModelBuilder.build(
            MindBoostProfileAggregator().aggregate("u1", emptyList(), 1L),
            TrainingRecommendation(CognitiveModuleId.MEMORIA_EPISODICA, EM, RecommendationReason.GATHER_DATA),
        )
        assertTrue(model.recommendation.text.contains("Entrena Memoria"))
        assertFalse(model.recommendation.text.contains("Memoria episódica"))
        assertEquals("Memoria", model.recommendation.moduleLabel)
    }

    // 6 — label mapping folds both memory modules into "Memoria"
    @Test
    fun toProductMotorLabel_foldsBothMemoryModulesIntoMemoria() {
        assertEquals("Memoria", CognitiveModuleId.MEMORIA.toProductMotorLabel())
        assertEquals("Memoria", CognitiveModuleId.MEMORIA_EPISODICA.toProductMotorLabel())
        assertEquals("Velocidad mental", CognitiveModuleId.VELOCIDAD.toProductMotorLabel())
    }

    // 7 — minimum-maturity table from the CEO spec
    @Test
    fun minimumMaturityRule_matchesTheCeoTable() {
        assertEquals(CAL, memoria(EST, 70, CAL, null).status)
        assertEquals(EMERG, memoria(EST, 70, EMERG, 70).status)
        assertEquals(EST, memoria(EST, 70, EST, 70).status)
    }

    // 8 — combined score is weighted by confidence*dataSufficiency, not a simple mean
    @Test
    fun combinedScore_isWeightedByConfidenceTimesSufficiency_notSimpleMean() {
        // Working dominates (0.9 vs 0.1): (80*0.9 + 40*0.1)/1.0 = 76, not the simple mean 60.
        assertEquals(76, memoria(EST, 80, EST, 40, wWeight = 0.9, eWeight = 0.1).score0to100)
    }

    // 9 — falls back to a simple mean when there is no valid weight
    @Test
    fun combinedScore_fallsBackToSimpleMean_whenNoValidWeight() {
        assertEquals(70, memoria(EST, 80, EST, 60, wWeight = 0.0, eWeight = 0.0).score0to100)
    }

    // 10 — null != 0: no motor without data ever shows "0"
    @Test
    fun everyMotor_withNoData_showsDash_neverZero() {
        val motors = ProductMotorUiMapper.toProductMotors(nine())
        assertTrue(motors.all { it.score0to100 == null && it.displayValue == "—" })
        assertTrue(motors.none { it.displayValue == "0" })
    }

    // 11 — a 1:1 motor keeps its dimension value and its product label, with no sub-metrics
    @Test
    fun oneToOneMotor_carriesItsValueAndProductLabel_noSubmetrics() {
        val velocidad = ProductMotorUiMapper.toProductMotors(nine(PS to dim(PS, EST, 63)))
            .first { it.motor == ProductMotor.VELOCIDAD }
        assertEquals("Velocidad mental", velocidad.label)
        assertEquals(63, velocidad.score0to100)
        assertTrue(velocidad.submetrics.isEmpty())
    }

    // 12 — the visible order is exactly the PRD product order
    @Test
    fun visibleMotors_followProductOrderFromPrd() {
        val labels = ProductMotorUiMapper.toProductMotors(nine()).map { it.label }
        assertEquals(
            listOf(
                "Enfoque", "Decisiones", "Lógica", "Creatividad",
                "Lenguaje", "Emociones", "Velocidad mental", "Memoria",
            ),
            labels,
        )
    }

    // 13 — a missing 1:1 dimension does not crash and degrades to "—"
    @Test
    fun missingOneToOneDimension_doesNotCrash_andShowsDash() {
        val input = nine().filterNot { it.dimension == CognitiveDimension.ATTENTION }
        val motors = ProductMotorUiMapper.toProductMotors(input)
        assertEquals(8, motors.size)
        val enfoque = motors.first { it.motor == ProductMotor.ENFOQUE }
        assertEquals(CAL, enfoque.status)
        assertNull(enfoque.score0to100)
        assertEquals("—", enfoque.displayValue)
        assertNotEquals("0", enfoque.displayValue)
    }

    // 14 — a missing episodic memory does not crash; Memoria still exists with the absent sub as "—"
    @Test
    fun missingEpisodicMemory_doesNotCrash_memoriaStillExists() {
        val input = nine(WM to dim(WM, EMERG, 70)).filterNot { it.dimension == EM }
        val motors = ProductMotorUiMapper.toProductMotors(input)
        assertEquals(8, motors.size)
        val m = motors.first { it.motor == ProductMotor.MEMORIA }
        assertEquals(CAL, m.status) // a missing sub forces the minimum maturity, never EMERGING/ESTABLISHED
        assertEquals("70", m.submetrics.first { it.label == "Trabajo" }.displayValue)
        assertEquals("—", m.submetrics.first { it.label == "Episódica" }.displayValue)
        assertNotEquals("0", m.displayValue)
    }

    // 15 — a missing working memory does not crash; Memoria still exists with the absent sub as "—"
    @Test
    fun missingWorkingMemory_doesNotCrash_memoriaStillExists() {
        val input = nine(EM to dim(EM, EST, 55)).filterNot { it.dimension == WM }
        val motors = ProductMotorUiMapper.toProductMotors(input)
        assertEquals(8, motors.size)
        val m = motors.first { it.motor == ProductMotor.MEMORIA }
        assertEquals(CAL, m.status) // a missing sub forces the minimum maturity, never EMERGING/ESTABLISHED
        assertEquals("—", m.submetrics.first { it.label == "Trabajo" }.displayValue)
        assertEquals("55", m.submetrics.first { it.label == "Episódica" }.displayValue)
        assertNotEquals("0", m.displayValue)
    }
}
