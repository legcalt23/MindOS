package com.mindos.app.features.mindboost.emotions.domain.metrics

import com.mindos.app.features.mindboost.core.calibration.ConfidenceInterval
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.SampleVerdict
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Ola 3 · R3 · P4 — the two cells the robustness index divides: how a user classified in CALM and how the same
 * user classified UNDER PRESSURE.
 *
 * The totals count every classification trial that was PRESENTED, omissions included. Under a tightened window a
 * timeout is the characteristic way pressure bites, so dropping omissions from the denominator would remove the
 * cost exactly where it is largest and leave an index that flatters the mode it exists to measure.
 *
 * Counts rather than trials, and for the same reason the calibration layer keeps counts: within one cell every
 * trial carries the same mode, so (correct, total) is a genuine summary AND a sufficient statistic — the index,
 * its interval and both accuracies are exactly recoverable. It stops being sufficient the day the index needs to
 * read anything trial-ordered (drift within a block, say), and that is a reason to come back here, not to patch
 * around it.
 */
data class PressureCells(
    val calmCorrect: Int,
    val calmTotal: Int,
    val pressureCorrect: Int,
    val pressureTotal: Int,
) {
    operator fun plus(other: PressureCells): PressureCells = PressureCells(
        calmCorrect = calmCorrect + other.calmCorrect,
        calmTotal = calmTotal + other.calmTotal,
        pressureCorrect = pressureCorrect + other.pressureCorrect,
        pressureTotal = pressureTotal + other.pressureTotal,
    )

    companion object {
        val EMPTY = PressureCells(0, 0, 0, 0)
    }
}

/** What the evidence supports saying about the DIRECTION of the pressure effect. */
enum class RobustnessVerdict {
    /** The interval sits entirely below 1: accuracy really is lower under pressure. */
    PRESSURE_COSTS_ACCURACY,

    /**
     * Named for what it is. "Robust under pressure" would be a claim the interval cannot license: failing to
     * resolve that pressure costs something is not the same as demonstrating that it costs nothing.
     */
    NO_DETECTABLE_COST,
}

/** Why no index exists. Each entry is a different fact about the user, not a shade of the same one. */
enum class RobustnessGap {
    /** Pressure has never been served — the mode is still locked, or every block ran calm. */
    NO_PRESSURE_SERVED,

    /** Pressure has been served, but the smaller cell is still under the ratified floor. Keep training. */
    TOO_FEW_TRIALS,

    /**
     * A cell sits on a boundary where the statistic has no interval. Two sub-cases, neither of which yields a
     * number: nothing correct in calm makes the ratio itself undefined (division by zero), and nothing correct
     * under pressure makes the ratio a legitimate 0 whose precision this estimator cannot express. They are
     * collapsed because the honest output is identical — no index — and split enum entries that no reader
     * distinguishes would be two names for one decision.
     */
    UNDEFINED_AT_CELL_BOUNDARY,
}

/** The index, or the reason there is none. Never a fabricated 1.0 standing in for "we could not tell". */
sealed interface SeparatorRobustness {
    data class Measured(
        val index: Double,
        val interval: ConfidenceInterval,
        val verdict: RobustnessVerdict,
    ) : SeparatorRobustness

    data class Unavailable(val reason: RobustnessGap) : SeparatorRobustness
}

/**
 * Ola 3 · R3 · P4 — accuracy under pressure divided by accuracy in calm, with the interval that says whether the
 * division resolved anything.
 *
 * **The cells are pooled ACROSS sessions, and that is arithmetic rather than preference.** The ratified floor for
 * this shape of reading is [ScoreMetricId.PRESSURE_DELTA]: 20 per cell, both cells required, interval required,
 * and cross-session pooling explicitly allowed. Within one session the calm cell is a single block — 10 trials at
 * today's geometry, 16 at the geometry R3 sketches — so neither clears the floor, and the wider fact is that
 * neither can: with a calm accuracy of .85 dropping to .70 under pressure, a real 15-point cost reads as
 * [0.56, 1.21] at 10 calm trials and [0.61, 1.12] at 16. Both intervals contain 1, which is to say both sessions
 * end unable to tell whether pressure cost the user anything. The smallest single session that could resolve that
 * effect needs 40 calm trials — a 120-trial sitting, four times what the exercise asks of anyone. Pooled instead,
 * today's geometry reaches it after four pressure sessions.
 *
 * So the block was NOT grown to 16. Growing it would have bought a longer session, the same refusal, and a
 * geometry change reaching into item exposure and the ambiguity balance for no reading gained.
 *
 * Two further choices carry weight:
 *  - the floor is read from the central gate rather than restated here, so raising it once raises it everywhere;
 *  - a difference is judged by its SMALLER cell. Forty calm trials against two pressure ones are forty-two
 *    trials and no comparison, and summing them would hide exactly that.
 *
 * Stated limit: pooling assumes a user's accuracy in each mode is the same quantity from session to session. It
 * is the assumption the held-out probe already makes when it sums its own counts, and it is wrong to the extent
 * that a user genuinely improves — improvement inside the pool widens the interval rather than biasing the index,
 * because it enters both cells.
 */
object SeparatorRobustnessIndex {

    fun evaluate(cells: PressureCells): SeparatorRobustness = when {
        cells.pressureTotal == 0 -> SeparatorRobustness.Unavailable(RobustnessGap.NO_PRESSURE_SERVED)
        !cellsAreSufficient(cells) -> SeparatorRobustness.Unavailable(RobustnessGap.TOO_FEW_TRIALS)
        cells.calmCorrect == 0 || cells.pressureCorrect == 0 ->
            SeparatorRobustness.Unavailable(RobustnessGap.UNDEFINED_AT_CELL_BOUNDARY)
        else -> measure(cells)
    }

    /** The metric whose ratified sampling rules this index obeys; exposed so a test can pin that it reads them. */
    val GOVERNING_METRIC: ScoreMetricId = ScoreMetricId.PRESSURE_DELTA

    private fun cellsAreSufficient(cells: PressureCells): Boolean {
        val verdict = MinSampleGate.evaluate(
            metric = GOVERNING_METRIC,
            nEffective = minOf(cells.calmTotal, cells.pressureTotal),
            cellsPresent = cells.calmTotal > 0 && cells.pressureTotal > 0,
        )
        return verdict is SampleVerdict.Sufficient
    }

    /**
     * Delta method on the log of the ratio, which is what keeps the interval multiplicative: a ratio's
     * uncertainty is not symmetric on the raw scale, and a symmetric interval around it would report a lower
     * bound that a proportion cannot reach.
     */
    private fun measure(cells: PressureCells): SeparatorRobustness.Measured {
        val calm = cells.calmCorrect.toDouble() / cells.calmTotal
        val pressure = cells.pressureCorrect.toDouble() / cells.pressureTotal
        val index = pressure / calm
        val standardError = sqrt(
            (1.0 - pressure) / (cells.pressureTotal * pressure) + (1.0 - calm) / (cells.calmTotal * calm),
        )
        val interval = ConfidenceInterval(
            low = index * exp(-Z_95 * standardError),
            high = index * exp(Z_95 * standardError),
        )
        return SeparatorRobustness.Measured(
            index = index,
            interval = interval,
            verdict = if (interval.high < NO_EFFECT) {
                RobustnessVerdict.PRESSURE_COSTS_ACCURACY
            } else {
                RobustnessVerdict.NO_DETECTABLE_COST
            },
        )
    }

    private const val Z_95 = 1.96

    /** The value the index takes when pressure changes nothing; the interval must clear it to claim a direction. */
    private const val NO_EFFECT = 1.0
}
