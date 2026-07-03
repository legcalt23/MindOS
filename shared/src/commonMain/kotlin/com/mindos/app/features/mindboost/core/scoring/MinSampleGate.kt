@file:Suppress("MagicNumber") // The integers ARE the golden nMin thresholds (audit §Thresholds).

package com.mindos.app.features.mindboost.core.scoring

/** Metrics the central sample-gate governs (neutral ids; modules map their native metrics onto these). */
enum class ScoreMetricId {
    CONGRUENCY_EFFECT,
    SWITCH_COST,
    PRESSURE_DELTA,
    DUAL_TASK_COST,
    POST_ERROR,
    SSRT,
    DPRIME,
    BRIER,
    RCI,
    BUR,
    COWANS_K,
    ACCURACY_RT_BASE,
}

/** Per-metric sampling requirements (the golden table; raising any value is a deliberate change). */
data class MinSampleSpec(
    val metric: ScoreMetricId,
    val nMinPerCell: Int,
    val requiresAllCells: Boolean,
    val crossSessionAllowed: Boolean,
    val requiresConfidenceInterval: Boolean,
)

/** Sampling verdict. [InsufficientData] is NEVER a number (invariant `null != 0`). */
sealed interface SampleVerdict {
    data class Sufficient(val nEffective: Int, val requiresIc: Boolean) : SampleVerdict
    data class InsufficientData(val nNeeded: Int, val nHave: Int) : SampleVerdict
}

/**
 * INFRA-06 · Single source of truth for per-metric minimum samples. This is the canonical table that
 * replaces the dispersed `MIN_N=3..5` of `DefaultMetricsEngine` (migrating the live engine to consume
 * this is a deliberate calibration change tracked separately — it would reclassify currently-"confident"
 * small samples). Comparatives need ALL cells + ≥20/cell; SSRT ≥30 stops; Brier ≥40 aggregated; etc.
 */
object MinSampleGate {

    private fun spec(metric: ScoreMetricId, nMin: Int, allCells: Boolean = false) =
        MinSampleSpec(metric, nMin, allCells, crossSessionAllowed = true, requiresConfidenceInterval = true)

    private val specs: Map<ScoreMetricId, MinSampleSpec> = listOf(
        spec(ScoreMetricId.CONGRUENCY_EFFECT, 20, allCells = true),
        spec(ScoreMetricId.SWITCH_COST, 20, allCells = true),
        spec(ScoreMetricId.PRESSURE_DELTA, 20, allCells = true),
        spec(ScoreMetricId.DUAL_TASK_COST, 20, allCells = true),
        spec(ScoreMetricId.POST_ERROR, 20, allCells = true),
        spec(ScoreMetricId.SSRT, 30),
        spec(ScoreMetricId.DPRIME, 20),
        spec(ScoreMetricId.BRIER, 40),
        spec(ScoreMetricId.RCI, 2),
        spec(ScoreMetricId.BUR, 12),
        spec(ScoreMetricId.COWANS_K, 20),
        spec(ScoreMetricId.ACCURACY_RT_BASE, 20),
    ).associateBy { it.metric }

    fun specFor(metric: ScoreMetricId): MinSampleSpec = specs.getValue(metric)

    /** Sampling verdict. NEVER returns a number when nMin/cell coverage isn't met (`null != 0`). */
    fun evaluate(metric: ScoreMetricId, nEffective: Int, cellsPresent: Boolean): SampleVerdict {
        val spec = specs.getValue(metric)
        return when {
            spec.requiresAllCells && !cellsPresent ->
                SampleVerdict.InsufficientData(spec.nMinPerCell, nEffective)
            nEffective < spec.nMinPerCell ->
                SampleVerdict.InsufficientData(spec.nMinPerCell, nEffective)
            else ->
                SampleVerdict.Sufficient(nEffective, requiresIc = spec.requiresConfidenceInterval)
        }
    }
}
