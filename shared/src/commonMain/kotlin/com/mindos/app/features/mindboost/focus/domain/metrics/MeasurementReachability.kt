package com.mindos.app.features.mindboost.focus.domain.metrics

import com.mindos.app.features.mindboost.core.runtime.conditionQuota
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.focus.domain.exercise.E13DifficultyMapping
import com.mindos.app.features.mindboost.focus.domain.exercise.E2DifficultyMapping
import com.mindos.app.features.mindboost.focus.domain.exercise.E4DifficultyMapping
import com.mindos.app.features.mindboost.focus.domain.exercise.E5DifficultyMapping
import com.mindos.app.features.mindboost.focus.domain.exercise.E6DifficultyMapping
import com.mindos.app.features.mindboost.focus.domain.exercise.E8DifficultyMapping
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId

/**
 * What a planned block CAN support, before anybody plays it.
 *
 * [smallestCell] is the smaller of the two cells the comparison needs (or, for a metric that does not compare
 * cells, the single cell), as the quota will lay it out. [floor] is the ratified minimum from [MinSampleGate].
 *
 * This is an UPPER BOUND, and the asymmetry is the whole point. A realised sample can only be smaller — only
 * HITs feed the reaction-time cells — so:
 *
 * - `reachable == false` means the claim is IMPOSSIBLE for this block, knowable before a single trial is shown.
 *   Whatever the user then does, `measurementSufficient` was never going to be true, and reporting that as if
 *   it were a fact about the person would be attributing OUR design's limit to them.
 * - `reachable == true` means the claim is possible, never that it is earned.
 */
data class ReachabilityVerdict(
    val metric: MetricKey,
    val axis: DifficultyAxis,
    val slots: Int,
    val smallestCell: Int,
    val floor: Int,
    val comparesTwoCells: Boolean,
    val reachable: Boolean,
)

/**
 * Whether a planned block can sustain the MEASUREMENT claim its exercise exists to make.
 *
 * Three facts decide that — the ratified floor, the condition proportion the difficulty dictates, and the block
 * size the session template dictates — and until R10 no symbol related the three. The consequence was the
 * `null != 0` family in its attributive form: when a difference score came back under-sampled, "this user's
 * session fell short" and "this planned form could never have cleared the floor" were indistinguishable.
 *
 * ## Why the difficulty axis is in here at all
 *
 * For every governed exercise the axis the adaptive engine steers (`ExerciseCatalog.governingAxis`) is exactly
 * the axis driving its condition proportion. E2/E5/E6 are steered on `INTERFERENCE`, which sets the incongruent
 * or switch share; E4 on `INHIBITORY_DEMAND`, which sets the stop share; E8 on `DUAL_TASK_LOAD`; E13 on
 * `PRESSURE`. So the engine, doing the only thing it knows how to do, resizes the cells of the comparison. That
 * is structural, not a coincidence, and it is why eligibility has to be a declared function of difficulty
 * rather than something discovered afterwards.
 *
 * ## What this deliberately does not do
 *
 * It changes no proportion, no axis range, no block size and no floor. Those are product decisions with owners
 * (`MB-CATALOG-0017`, `MB-CATALOG-0018`); lowering the central table is the anti-pattern `MB-CATALOG-0009`
 * closed. See `docs/audits/ADR_MEASUREMENT_REACHABILITY_2026-08-20.md`.
 */
object MeasurementReachability {

    /**
     * How one exercise's comparative cell is drawn.
     *
     * [proportionAt] reads the real difficulty mapping rather than restating its curve, and [slotsIn] the real
     * number of slots the generator has to fill. Both are references to the authority, not copies of it — a
     * second copy of a curve is how a declaration drifts away from the thing it declares.
     */
    private class ComparativeCell(
        val metric: MetricKey,
        val axis: DifficultyAxis,
        val slotsIn: (Int) -> Int,
        val proportionAt: (DifficultyVector) -> Double,
        val atLeast: Int = 0,
    )

    private val cells: Map<ExerciseId, ComparativeCell> = mapOf(
        ExerciseId.E2_FILTRO to ComparativeCell(
            metric = MetricKey.CONGRUENCY_EFFECT,
            axis = DifficultyAxis.INTERFERENCE,
            slotsIn = { it },
            proportionAt = { E2DifficultyMapping.map(it).incongruentProportion },
        ),
        // E4 guarantees at least one stop, mirroring its generator: the quota may round up, never down to zero.
        ExerciseId.E4_FRENO to ComparativeCell(
            metric = MetricKey.SSRT,
            axis = DifficultyAxis.INHIBITORY_DEMAND,
            slotsIn = { it },
            proportionAt = { E4DifficultyMapping.map(it).stopProportion },
            atLeast = 1,
        ),
        ExerciseId.E5_CONFLICTO to ComparativeCell(
            metric = MetricKey.CONGRUENCY_EFFECT,
            axis = DifficultyAxis.INTERFERENCE,
            slotsIn = { it },
            proportionAt = { E5DifficultyMapping.map(it).incongruentProportion },
        ),
        // E6 can only switch from trial 1 onwards: trial 0 has no predecessor, so it has n-1 slots, not n.
        ExerciseId.E6_CAMBIO to ComparativeCell(
            metric = MetricKey.SWITCH_COST,
            axis = DifficultyAxis.INTERFERENCE,
            slotsIn = { it - 1 },
            proportionAt = { E6DifficultyMapping.map(it).switchFrequency },
        ),
        ExerciseId.E8_DUO to ComparativeCell(
            metric = MetricKey.DUAL_TASK_COST,
            axis = DifficultyAxis.DUAL_TASK_LOAD,
            slotsIn = { it },
            proportionAt = { E8DifficultyMapping.map(it).dualProportion },
        ),
        ExerciseId.E13_PRESION to ComparativeCell(
            metric = MetricKey.PRESSURE_DELTA,
            axis = DifficultyAxis.PRESSURE,
            slotsIn = { it },
            proportionAt = { E13DifficultyMapping.map(it).pressureProportion },
        ),
    )

    /**
     * Governed exercises whose cell the generator CANNOT hand out, because the user splits it.
     *
     * `POST_ERROR_*` divides trials by whether the person erred on the previous one, so no schedule can size it
     * in advance and no quota could fix it. Declared rather than silently dropped: the derivation test treats
     * this set as the only legitimate reason for a governed exercise to be absent from [cells].
     *
     * E3 is here for that reason alone. Its go/no-go split IS generator-drawn, but the metrics that split
     * carries — `FALSE_ALARMS`/`OMISSIONS` — have no ratified central floor, so no claim rests on it.
     */
    val behaviouralCells: Set<ExerciseId> = setOf(ExerciseId.E3_GATILLO, ExerciseId.E12_REENGANCHE)

    /** The exercises whose comparative cell is declared here. */
    fun declaredExercises(): Set<ExerciseId> = cells.keys

    /** The difficulty axis that resizes [exerciseId]'s comparison, or `null` when it has no declared cell. */
    fun axisFor(exerciseId: ExerciseId): DifficultyAxis? = cells[exerciseId]?.axis

    /**
     * What a block of [trialCount] trials of [exerciseId] at [difficulty] can support.
     *
     * `null` means this exercise has no comparative metric with a ratified floor, so the question does not
     * apply. It is never reused as "no": a sentinel carrying two meanings is exactly how a gate ends up with a
     * ceiling below its own threshold.
     */
    fun verdictFor(exerciseId: ExerciseId, trialCount: Int, difficulty: DifficultyVector): ReachabilityVerdict? {
        val cell = cells[exerciseId] ?: return null
        // A metric outside the central table has no ratified floor, so the question does not apply: `null`
        // again, never `false`. Expressed as a `let` rather than a second early return to stay inside the
        // module's return-count budget without turning the two distinct absences into one.
        return MeasurementSufficiency.centralMetricFor(cell.metric)?.let { central ->
            val spec = MinSampleGate.specFor(central)
            val slots = cell.slotsIn(trialCount)
            val flagged = conditionQuota(slots, cell.proportionAt(difficulty), cell.atLeast)
            val smallestCell = if (spec.requiresAllCells) minOf(flagged, slots - flagged) else flagged
            ReachabilityVerdict(
                metric = cell.metric,
                axis = cell.axis,
                slots = slots,
                smallestCell = smallestCell,
                floor = spec.nMinPerCell,
                comparesTwoCells = spec.requiresAllCells,
                // `== true` and not `!= false`: a metric outside the table must never pass by default.
                reachable = MeasurementSufficiency.evaluate(
                    key = cell.metric,
                    nEffective = smallestCell,
                    cellsPresent = smallestCell > 0,
                ) == true,
            )
        }
    }
}
