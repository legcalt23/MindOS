package com.mindos.app.features.mindboost.profile.presentation

import com.mindos.app.features.mindboost.profile.domain.model.CognitiveDimension
import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.profile.presentation.state.DimensionUi
import com.mindos.app.features.mindboost.profile.presentation.state.ProductMotor
import com.mindos.app.features.mindboost.profile.presentation.state.ProductMotorUi
import com.mindos.app.features.mindboost.profile.presentation.state.ProductSubMetricUi
import com.mindos.app.features.mindboost.profile.presentation.state.displayLabel
import com.mindos.app.features.mindboost.profile.presentation.state.displaySymbol
import com.mindos.app.features.mindboost.profile.presentation.state.primaryDimension
import kotlin.math.roundToInt

/**
 * Presentation-border fold: the 9 internal [DimensionUi] -> the 8 product-facing [ProductMotorUi] the user
 * sees. The internal WORKING_MEMORY + EPISODIC_MEMORY split folds under a single "Memoria" motor; every
 * other motor is 1:1.
 *
 * Pure and side-effect-free. It only RE-PRESENTS already-built, already-gated display values — it does NOT
 * touch scoring, calibration, the aggregator, or CognitiveTaxonomy. Honesty preserved: a missing value stays
 * "—" (never "0"); the combined Memoria status is the MINIMUM maturity of its two sub-dimensions, so the
 * product never claims "established" while a part of memory is still calibrating.
 *
 * See docs/architecture/MINDBOOST_TAXONOMY_MAPPING.md §7.
 */
object ProductMotorUiMapper {

    private const val EM_DASH = "—"
    private const val MAX_PERCENT = 100

    /**
     * Fold the 9 internal dimensions into the 8 visible product motors, in the PRD product order.
     * Defensive: a missing dimension (legacy / corrupt / partial state) degrades to a safe CALIBRATING
     * motor ("—", never 0) instead of crashing the UI — see [missingDimensionUi].
     */
    fun toProductMotors(dimensions: List<DimensionUi>): List<ProductMotorUi> {
        val byDimension = dimensions.associateBy { it.dimension }
        return ProductMotor.entries.map { motor ->
            when (motor) {
                ProductMotor.MEMORIA -> combinedMemoria(
                    resolve(byDimension, CognitiveDimension.WORKING_MEMORY),
                    resolve(byDimension, CognitiveDimension.EPISODIC_MEMORY),
                )
                else -> passthrough(motor, resolve(byDimension, motor.primaryDimension()))
            }
        }
    }

    /** The dimension's UI row, or a safe CALIBRATING placeholder when it is absent (never crashes). */
    private fun resolve(
        byDimension: Map<CognitiveDimension, DimensionUi>,
        dimension: CognitiveDimension,
    ): DimensionUi = byDimension[dimension] ?: missingDimensionUi(dimension)

    /** A safe "unknown yet" row for an absent dimension: CALIBRATING, null score, "—" (never 0). */
    private fun missingDimensionUi(dimension: CognitiveDimension): DimensionUi = DimensionUi(
        dimension = dimension,
        label = dimension.displayLabel(),
        status = DimensionStatus.CALIBRATING,
        statusLabel = DimensionStatus.CALIBRATING.displayLabel(),
        displayValue = EM_DASH,
        score0to100 = null,
        trend = ProfileTrend.UNKNOWN,
        trendLabel = ProfileTrend.UNKNOWN.displayLabel(),
        trendSymbol = ProfileTrend.UNKNOWN.displaySymbol(),
        preliminary = true,
        calibrationProgress = 0,
        calibrationLabel = "",
        combineWeight = 0.0,
    )

    private fun passthrough(motor: ProductMotor, dim: DimensionUi): ProductMotorUi = ProductMotorUi(
        motor = motor,
        label = motor.displayLabel(),
        status = dim.status,
        statusLabel = dim.statusLabel,
        displayValue = dim.displayValue,
        score0to100 = dim.score0to100,
        trend = dim.trend,
        trendLabel = dim.trendLabel,
        trendSymbol = dim.trendSymbol,
        preliminary = dim.preliminary,
        calibrationProgress = dim.calibrationProgress,
        calibrationLabel = dim.calibrationLabel,
        submetrics = emptyList(),
    )

    private fun combinedMemoria(working: DimensionUi, episodic: DimensionUi): ProductMotorUi {
        val status = minMaturity(working.status, episodic.status)
        // The more mature sub provides the trend (tie -> working).
        val dominant = if (maturityRank(episodic.status) > maturityRank(working.status)) episodic else working
        val score = combinedScore(working, episodic)
        // Calibration progress is surfaced ONLY when there is nothing to show yet (both sub-values absent).
        val showCalibration = score == null && status == DimensionStatus.CALIBRATING
        val calibratingSub = listOf(working, episodic)
            .filter { it.status == DimensionStatus.CALIBRATING }
            .minByOrNull { it.calibrationProgress ?: 0 }

        return ProductMotorUi(
            motor = ProductMotor.MEMORIA,
            label = ProductMotor.MEMORIA.displayLabel(),
            status = status,
            statusLabel = status.displayLabel(),
            displayValue = score?.toString() ?: EM_DASH,
            score0to100 = score,
            trend = dominant.trend,
            trendLabel = dominant.trendLabel,
            trendSymbol = dominant.trendSymbol,
            preliminary = working.preliminary || episodic.preliminary || status != DimensionStatus.ESTABLISHED,
            calibrationProgress = if (showCalibration) calibratingSub?.calibrationProgress else null,
            calibrationLabel = if (showCalibration) (calibratingSub?.calibrationLabel ?: "") else "",
            submetrics = listOf(
                ProductSubMetricUi("Trabajo", working.displayValue),
                ProductSubMetricUi("Episódica", episodic.displayValue),
            ),
        )
    }

    /**
     * Combined Memoria value: weighted mean by confidence*dataSufficiency when BOTH sub-values are shown; the
     * single available sub-value (provisional) when only one is; null when neither is (never 0). Falls back to
     * a simple mean when no valid weight exists.
     */
    private fun combinedScore(working: DimensionUi, episodic: DimensionUi): Int? {
        val work = working.score0to100
        val epi = episodic.score0to100
        return when {
            work != null && epi != null -> {
                val totalWeight = working.combineWeight + episodic.combineWeight
                val mean = if (totalWeight > 0.0) {
                    (work * working.combineWeight + epi * episodic.combineWeight) / totalWeight
                } else {
                    listOf(work, epi).average()
                }
                mean.roundToInt().coerceIn(0, MAX_PERCENT)
            }
            work != null -> work
            epi != null -> epi
            else -> null
        }
    }

    private fun minMaturity(a: DimensionStatus, b: DimensionStatus): DimensionStatus =
        if (maturityRank(a) <= maturityRank(b)) a else b

    private fun maturityRank(status: DimensionStatus): Int = when (status) {
        DimensionStatus.CALIBRATING -> 0
        DimensionStatus.EMERGING -> 1
        DimensionStatus.ESTABLISHED -> 2
    }
}
