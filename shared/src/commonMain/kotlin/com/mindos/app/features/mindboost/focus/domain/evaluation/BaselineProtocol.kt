package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.AxisLevel
import com.mindos.app.features.mindboost.focus.domain.model.Capacity
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode

/**
 * Which context questions the baseline asks before measuring. These are covariates
 * (sleep, caffeine, mood, hour) that contextualize variance — never a diagnosis.
 * The answers are captured as a
 * [com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState].
 */
data class StateQuestionnaire(
    val askSleepHours: Boolean = true,
    val askCaffeine: Boolean = true,
    val askMood: Boolean = true,
    val captureLocalHour: Boolean = true,
)

/**
 * One fixed-difficulty block of the baseline. A block is either a calibration block
 * (drawn from a training-family exercise, used to set individual thresholds) or a
 * reserved-probe block (a reserved SEED of a paradigm the catalogue also trains — a parallel form,
 * not an untrained measure, `MB-FOCUS-0089`). Either way it runs at a fixed difficulty and NEVER adapts.
 */
data class BaselineBlock(
    val capacity: Capacity,
    val exerciseId: ExerciseId?,
    val probe: ProbeDefinition?,
    val fixedDifficulty: DifficultyVector,
    val targetTrialCount: Int,
    val mode: Mode,
) {
    init {
        require(mode != Mode.TRAINING) { "Baseline blocks never run in TRAINING mode." }
        require((exerciseId == null) != (probe == null)) {
            "A baseline block is exactly one of: an exercise (calibration) or a reserved probe."
        }
        require(targetTrialCount > 0) { "targetTrialCount must be > 0." }
    }

    val isProbe: Boolean get() = probe != null
    val estimatedDurationMs: Long get() = targetTrialCount * AVG_TRIAL_MS

    companion object {
        const val AVG_TRIAL_MS = 3_800L
    }
}

/**
 * The fixed 15–20 minute initial baseline: vigilance + selective/interference +
 * inhibition (calibration), then one reserved probe (transfer), plus the state
 * questionnaire. See 06_evaluation_and_baseline_plan.md §2.
 */
data class BaselineProtocol(
    val blocks: List<BaselineBlock>,
    val stateQuestionnaire: StateQuestionnaire,
) {
    init {
        require(blocks.isNotEmpty()) { "A baseline needs at least one block." }
        require(blocks.count { it.isProbe } >= 1) { "A baseline must include at least one reserved probe." }
        require(blocks.none { it.mode == Mode.TRAINING }) { "Baseline must not adapt." }
    }

    val estimatedDurationMs: Long
        get() = blocks.sumOf { it.estimatedDurationMs } + QUESTIONNAIRE_OVERHEAD_MS

    private companion object {
        const val QUESTIONNAIRE_OVERHEAD_MS = 60_000L
    }
}

/** The canonical initial baseline protocol. */
@Suppress("MagicNumber") // Per-block trial counts and fixed levels are the protocol's data.
object BaselineProtocols {

    private val calibrationDifficulty = DifficultyVector(
        perceptualLoad = AxisLevel(0.35),
        temporalUncertainty = AxisLevel(0.35),
    )

    val INITIAL: BaselineProtocol = BaselineProtocol(
        blocks = listOf(
            BaselineBlock(
                capacity = Capacity.SUSTAINED_VIGILANCE,
                exerciseId = ExerciseId.E1_VIGIA,
                probe = null,
                fixedDifficulty = calibrationDifficulty,
                targetTrialCount = 75,
                mode = Mode.CALIBRATION,
            ),
            BaselineBlock(
                capacity = Capacity.SELECTIVE_FILTERING,
                exerciseId = ExerciseId.E2_FILTRO,
                probe = null,
                fixedDifficulty = calibrationDifficulty,
                targetTrialCount = 60,
                mode = Mode.CALIBRATION,
            ),
            BaselineBlock(
                capacity = Capacity.RESPONSE_INHIBITION,
                exerciseId = ExerciseId.E3_GATILLO,
                probe = null,
                fixedDifficulty = calibrationDifficulty,
                targetTrialCount = 60,
                mode = Mode.CALIBRATION,
            ),
            BaselineBlock(
                capacity = Capacity.SUSTAINED_VIGILANCE,
                exerciseId = null,
                probe = ProbeBank.VIGILANCE_PROBE,
                fixedDifficulty = ProbeBank.VIGILANCE_PROBE.fixedDifficulty,
                targetTrialCount = 55,
                mode = Mode.EVALUATION,
            ),
        ),
        stateQuestionnaire = StateQuestionnaire(),
    )

    /** The calibration exercises must all exist in the training catalogue. */
    val calibrationExercises: List<ExerciseId>
        get() = INITIAL.blocks.mapNotNull { it.exerciseId }.filter { it in ExerciseCatalog.all.keys }
}
