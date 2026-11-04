package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.scoring.MinSampleGate
import com.mindos.app.features.mindboost.core.scoring.ScoreMetricId
import com.mindos.app.features.mindboost.focus.domain.evaluation.BaselineProtocols
import com.mindos.app.features.mindboost.focus.domain.metrics.MeasurementReachability
import com.mindos.app.features.mindboost.focus.domain.metrics.MeasurementSufficiency
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyAxis
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * R9 · `MB-CATALOG-0014`. The cell of a difference score must be a QUOTA, not a per-trial coin flip.
 *
 * Drawing condition membership with an independent Bernoulli per trial couples two properties that are
 * independent: the expected PROPORTION (exercise design, governed by a difficulty axis, must be preserved) and
 * the realised CELL SIZE (the sample of the comparison, which must not depend on chance). The measured
 * consequence is that whether a session can support a capacity claim comes down to the seed — and, worse, that the
 * adaptive engine silently shifts how often it does (E8: 88 % eligible at minimum load, 9 % at maximum).
 *
 * See `docs/audits/ADR_CONDITION_SCHEDULE_2026-08-19.md`.
 */
class ConditionScheduleTest {

    /**
     * How to read the drawn condition off a stimulus, and the proportion the difficulty mapping DECLARES for it.
     *
     * The proportion is read from the mapping itself rather than restated here: the mapping is the authority, and
     * a second copy of the curve in a test is exactly the "two sources of truth" defect this plan keeps closing.
     */
    private class Governed(
        val proportion: (DifficultyVector) -> Double,
        val flag: (Stimulus) -> Boolean,
    )

    private val governedByGenerator: Map<ExerciseId, Governed> = mapOf(
        ExerciseId.E2_FILTRO to Governed(
            proportion = { E2DifficultyMapping.map(it).incongruentProportion },
            flag = { (it.payload as StimulusPayload.Flanker).congruent.not() },
        ),
        ExerciseId.E4_FRENO to Governed(
            proportion = { E4DifficultyMapping.map(it).stopProportion },
            flag = { (it.payload as StimulusPayload.StopSignal).isStop },
        ),
        ExerciseId.E5_CONFLICTO to Governed(
            proportion = { E5DifficultyMapping.map(it).incongruentProportion },
            flag = { (it.payload as StimulusPayload.Conflict).congruent.not() },
        ),
        ExerciseId.E6_CAMBIO to Governed(
            proportion = { E6DifficultyMapping.map(it).switchFrequency },
            flag = { (it.payload as StimulusPayload.TaskSwitch).isSwitch },
        ),
        ExerciseId.E8_DUO to Governed(
            proportion = { E8DifficultyMapping.map(it).dualProportion },
            flag = { (it.payload as StimulusPayload.DualTask).dual },
        ),
        ExerciseId.E13_PRESION to Governed(
            proportion = { E13DifficultyMapping.map(it).pressureProportion },
            flag = { (it.payload as StimulusPayload.Pressure).underPressure },
        ),
    )

    /** The axis that resizes this exercise's comparison, read from the production declaration, never restated. */
    private fun axisOf(exerciseId: ExerciseId): DifficultyAxis =
        requireNotNull(MeasurementReachability.axisFor(exerciseId)) { "$exerciseId has no declared cell" }

    /** How many slots the generator has to fill, read from the production declaration (E6 has n-1, not n). */
    private fun slotsOf(exerciseId: ExerciseId, trialCount: Int): Int =
        requireNotNull(
            MeasurementReachability.verdictFor(exerciseId, trialCount, DifficultyVector.EASIEST),
        ) { "$exerciseId has no declared cell" }.slots

    /**
     * R9 left the membership set — which exercises owe a quota — living in this test. R10 promoted it to
     * `MeasurementReachability`, where it is a production declaration rather than a test's private opinion, and
     * the derivation from `MeasurementSufficiency.central` now lives with it in `MeasurementReachabilityTest`.
     * All that remains here is that this file's instruments cover exactly what production declares.
     */
    @Test
    fun theQuotaInstrumentsCoverExactlyWhatProductionDeclares() {
        assertTrue(
            MeasurementReachability.declaredExercises().isNotEmpty(),
            "anti-vacuity: production declares no comparative cell at all",
        )
        assertEquals(
            MeasurementReachability.declaredExercises(),
            governedByGenerator.keys,
            "a generator whose cell production declares must be exercised here; the sets diverged",
        )
    }

    @Test
    fun everyGeneratorDrawnCellIsAQuotaAndNotACoinFlip() {
        for ((exerciseId, governed) in governedByGenerator) {
            for (level in listOf(0.0, 1.0)) {
                val difficulty = DifficultyVector.EASIEST.adjust(axisOf(exerciseId), AxisLevel(level))
                val counts = (0 until SEEDS).map { seed ->
                    generate(exerciseId, difficulty, seed).count(governed.flag)
                }.toSet()
                assertEquals(
                    1,
                    counts.size,
                    "$exerciseId at ${axisOf(exerciseId)}=$level drew ${counts.size} different cell sizes over " +
                        "$SEEDS seeds ($counts). The sample of a claim must not depend on the seed",
                )
            }
        }
    }

    /**
     * The abort criterion of the ADR, made executable: the quota may only round, never re-parameterise. If a
     * proportion had to move, that is a product decision and does not belong in this change.
     */
    @Test
    fun theQuotaPreservesTheProportionTheDifficultyMappingDeclares() {
        for ((exerciseId, governed) in governedByGenerator) {
            for (level in listOf(0.0, 0.5, 1.0)) {
                val difficulty = DifficultyVector.EASIEST.adjust(axisOf(exerciseId), AxisLevel(level))
                val slots = slotsOf(exerciseId, BLOCK_SIZE)
                val declared = governed.proportion(difficulty)
                val realised = generate(exerciseId, difficulty, seed = 1).count(governed.flag).toDouble() / slots
                assertTrue(
                    abs(realised - declared) <= 1.0 / slots,
                    "$exerciseId at ${axisOf(exerciseId)}=$level realised $realised against a declared $declared; " +
                        "a quota may differ by rounding only (<= ${1.0 / slots})",
                )
            }
        }
    }

    /**
     * A quota that laid its trials out in a block would trade a sampling defect for a worse one: the condition
     * would become perfectly predictable and confounded with time-on-task. It must be shuffled.
     */
    @Test
    fun theQuotaIsShuffledAndNotLaidOutInABlock() {
        for ((exerciseId, governed) in governedByGenerator) {
            val difficulty = DifficultyVector.EASIEST.adjust(axisOf(exerciseId), AxisLevel(0.5))
            val positions = (0 until SEEDS).map { seed ->
                generate(exerciseId, difficulty, seed).withIndex().filter { governed.flag(it.value) }.map { it.index }
            }
            assertTrue(
                positions.all { it.isNotEmpty() },
                "anti-vacuity: $exerciseId produced an empty condition cell, so this test would prove nothing",
            )
            assertTrue(
                positions.none { indices -> indices == indices.indices.toList() },
                "$exerciseId laid its condition out as a contiguous prefix: the order is now predictable and " +
                    "confounded with time-on-task",
            )
            assertTrue(
                positions.distinct().size > 1,
                "$exerciseId produced the same layout for every seed: the schedule is not being shuffled",
            )
        }
    }

    /**
     * The live consequence, pinned as an exact number instead of a distribution. Supersedes the distributional
     * pin `RciMeasurementSufficiencyTest.theBaselineE2BlockRarelySamplesACongruencyComparison`, which after the
     * quota would pass without discriminating anything.
     */
    @Test
    fun theBaselineCongruencyCellIsNowADeterministicDeficitInsteadOfALottery() {
        val block = BaselineProtocols.INITIAL.blocks.single { it.exerciseId == ExerciseId.E2_FILTRO }
        val floor = MeasurementSufficiency.centralMetricFor(
            MetricKey.CONGRUENCY_EFFECT,
        )
        assertEquals(ScoreMetricId.CONGRUENCY_EFFECT, floor, "the metric lost its dedicated central floor")

        val cells = (0 until SEEDS).map { seed ->
            E2FiltroGenerator().generate(
                difficulty = block.fixedDifficulty,
                spec = BlockSpec(targetTrialCount = block.targetTrialCount, mode = block.mode),
                seed = SessionSeed(seed.toLong(), RngVersion.V1),
            ).count { (it.payload as StimulusPayload.Flanker).congruent.not() }
        }.toSet()

        assertEquals(
            setOf((block.targetTrialCount * E2_BASELINE_INCONGRUENT_SHARE).roundToInt()),
            cells,
            "the baseline incongruent cell must be the same number for every user; it was $cells",
        )
        assertEquals(
            false,
            MeasurementSufficiency.evaluate(
                key = MetricKey.CONGRUENCY_EFFECT,
                nEffective = cells.single(),
                cellsPresent = true,
            ),
            "12 incongruent trials against a floor of 20 is insufficient — for everyone, now, deterministically",
        )
    }

    /**
     * `SSRT` needs 30 stop trials and a 60-trial block tops out around 20 at the hardest setting: a HARD ceiling,
     * not a tail. The quota makes it visible; removing it is a block-sizing decision (declared, not taken here).
     */
    @Test
    fun ssrtCannotBeSampledInASixtyTrialBlockAtAnyDifficulty() {
        val floorSpec = MinSampleGate.specFor(ScoreMetricId.SSRT)
        val best = listOf(0.0, 0.5, 1.0).maxOf { level ->
            val difficulty = DifficultyVector.EASIEST.adjust(DifficultyAxis.INHIBITORY_DEMAND, AxisLevel(level))
            generate(ExerciseId.E4_FRENO, difficulty, seed = 1).count { it.requiresStop }
        }
        assertTrue(best > 0, "anti-vacuity: no stop trials at all would make this assertion meaningless")
        assertTrue(
            best < floorSpec.nMinPerCell,
            "E4 now reaches $best stops in a $BLOCK_SIZE-trial block against a floor of ${floorSpec.nMinPerCell}; " +
                "if the block or the proportion changed, revisit the declared ceiling instead of deleting this",
        )
    }

    private fun generate(id: ExerciseId, difficulty: DifficultyVector, seed: Int): List<Stimulus> {
        val spec = BlockSpec(targetTrialCount = BLOCK_SIZE, mode = Mode.TRAINING)
        FocusExerciseGenerators.versioned[id]?.let {
            return it.generate(difficulty, spec, SessionSeed(seed.toLong(), RngVersion.V1))
        }
        val legacy = requireNotNull(FocusExerciseGenerators.legacy[id]) { "no generator registered for $id" }
        return legacy.generate(difficulty, spec, seed.toLong())
    }

    private companion object {
        const val SEEDS = 40
        const val BLOCK_SIZE = 60

        /** `E2DifficultyMapping` at `interference = MIN`, which is where `calibrationDifficulty` leaves it. */
        const val E2_BASELINE_INCONGRUENT_SHARE = 0.20
    }
}
