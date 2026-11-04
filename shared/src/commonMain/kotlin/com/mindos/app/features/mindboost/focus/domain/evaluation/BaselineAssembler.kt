package com.mindos.app.features.mindboost.focus.domain.evaluation

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile

/**
 * Pure folding of a finished baseline into calibration outputs (06_ §2–§3). Given the assembled
 * calibration blocks (E1/E2/E3, fixed difficulty, CALIBRATION mode) and the reserved-probe block (a
 * parallel form of a trained paradigm, run in EVALUATION mode — NOT an untrained measure,
 * `MB-FOCUS-0089`), it derives:
 *
 * - **individual thresholds** from the user's own calibration RT distribution (personalized lapse cutoff,
 *   baseline RT-CV) — a fixed 500 ms cutoff is wrong for a much faster/slower person;
 * - **per-exercise starting difficulty** aiming for the productive ~0.80 accuracy zone;
 * - the **[BaselineSnapshot]** (the *pre* side of any later pre/post RCI comparison), built from the
 *   reserved-probe metrics so a retest compares the same instrument against the user's own earlier run.
 *
 * The live runtime stays an orchestrator: it presents stimuli and assembles blocks, then hands the
 * finished blocks here. This class never touches Compose, timing or persistence, so it is fully unit-testable.
 */
class BaselineAssembler(private val calibrationEngine: CalibrationEngine) {

    /** The folded baseline: an updated profile (thresholds + startingDifficulty + baseline) and its snapshot. */
    data class Outcome(val profile: UserCognitiveProfile, val snapshot: BaselineSnapshot)

    @Suppress("LongParameterList") // Each argument is intrinsic data of the baseline being folded.
    fun assemble(
        profile: UserCognitiveProfile,
        calibrationBlocks: List<BlockResult>,
        probeBlock: BlockResult,
        capturedAtMs: Long,
        state: SubjectiveState? = null,
        deviceFingerprint: String? = null,
    ): Outcome {
        require(calibrationBlocks.isNotEmpty()) { "A baseline needs at least one calibration block." }

        val calibrationTrials = calibrationBlocks.flatMap { it.trials }
        val thresholds = calibrationEngine.calibrate(calibrationTrials)
        val startingDifficulty: Map<ExerciseId, DifficultyVector> = calibrationBlocks.associate { block ->
            val definition = ExerciseCatalog.definition(block.exerciseId)
            block.exerciseId to calibrationEngine.initialDifficulty(definition, block.trials)
        }
        val snapshot = calibrationEngine.baselineSnapshot(
            userId = profile.userId,
            metrics = probeBlock.metrics,
            capturedAtMs = capturedAtMs,
            state = state,
            deviceFingerprint = deviceFingerprint,
        )
        val updated = profile.copy(
            thresholds = thresholds,
            // Merge: calibrated values override any prior starting difficulty, keep the rest.
            startingDifficulty = profile.startingDifficulty + startingDifficulty,
            baseline = snapshot,
            updatedAtMs = capturedAtMs,
        )
        return Outcome(profile = updated, snapshot = snapshot)
    }
}
