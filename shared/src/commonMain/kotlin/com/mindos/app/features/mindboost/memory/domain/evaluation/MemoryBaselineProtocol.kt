package com.mindos.app.features.mindboost.memory.domain.evaluation

import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemorySubcapacity
import com.mindos.app.features.mindboost.memory.domain.model.Mode

/**
 * Context questions asked before the baseline. Covariates (sleep, mood, hour) that
 * contextualize variance — never a diagnosis (doc §6 honesty).
 */
data class MemoryStateQuestionnaire(
    val askSleepHours: Boolean = true,
    val askMood: Boolean = true,
    val captureLocalHour: Boolean = true,
)

/**
 * One fixed-difficulty block of the baseline. Either a calibration block (a
 * training-family exercise used to set thresholds) or a reserved-probe block (the
 * un-trained transfer measure). Never adapts (doc §9).
 */
data class MemoryBaselineBlock(
    val subcapacity: MemorySubcapacity,
    val exerciseId: MemoryExerciseId?,
    val probe: MemoryProbe?,
    val fixedDifficulty: MemoryDifficultyVector,
    val mode: Mode,
) {
    init {
        require(mode != Mode.TRAINING) { "Baseline blocks never run in TRAINING mode." }
        require((exerciseId == null) != (probe == null)) {
            "A baseline block is exactly one of: an exercise (calibration) or a reserved probe."
        }
    }

    val isProbe: Boolean get() = probe != null
}

/**
 * The initial baseline (doc §9): calibration blocks per V1 subcapacity plus at
 * least one reserved probe (the untrained transfer measure), all at fixed
 * difficulty, none adapting.
 */
data class MemoryBaselineProtocol(
    val blocks: List<MemoryBaselineBlock>,
    val stateQuestionnaire: MemoryStateQuestionnaire,
) {
    init {
        require(blocks.isNotEmpty()) { "A baseline needs at least one block." }
        require(blocks.count { it.isProbe } >= 1) { "A baseline must include at least one reserved probe." }
        require(blocks.none { it.mode == Mode.TRAINING }) { "Baseline must not adapt." }
    }
}

/** The canonical initial baseline protocol for the V1 wave. */
object MemoryBaselineProtocols {

    private const val CALIBRATION_LOAD = 0.35

    private val calibrationDifficulty = MemoryDifficultyVector.EASIEST
        .adjust(MemoryDifficultyAxis.LOAD, AxisLevel(CALIBRATION_LOAD))

    val INITIAL: MemoryBaselineProtocol = MemoryBaselineProtocol(
        blocks = listOf(
            MemoryBaselineBlock(
                subcapacity = MemorySubcapacity.WORKING_MEMORY,
                exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
                probe = null,
                fixedDifficulty = calibrationDifficulty,
                mode = Mode.CALIBRATION,
            ),
            MemoryBaselineBlock(
                subcapacity = MemorySubcapacity.EPISODIC,
                exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                probe = null,
                fixedDifficulty = calibrationDifficulty,
                mode = Mode.CALIBRATION,
            ),
            MemoryBaselineBlock(
                subcapacity = MemorySubcapacity.ASSOCIATIVE,
                exerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS,
                probe = null,
                fixedDifficulty = calibrationDifficulty,
                mode = Mode.CALIBRATION,
            ),
            MemoryBaselineBlock(
                subcapacity = MemorySubcapacity.EPISODIC,
                exerciseId = null,
                probe = MemoryProbeBank.EPISODIC,
                fixedDifficulty = MemoryProbeBank.EPISODIC.fixedDifficulty,
                mode = Mode.EVALUATION,
            ),
        ),
        stateQuestionnaire = MemoryStateQuestionnaire(),
    )

    /** Calibration exercises must all exist in the training catalogue. */
    val calibrationExercises: List<MemoryExerciseId>
        get() = INITIAL.blocks.mapNotNull { it.exerciseId }.filter { it in MemoryExerciseCatalog.all.keys }
}
