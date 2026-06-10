package com.mindos.app.features.mindboost.memory.domain.session

import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyAxis
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId

/**
 * One runnable activity inside a session: which exercise to run, at which
 * difficulty, in which [phase]. [deferred] marks a delayed-recall block that
 * re-tests earlier material WITHOUT re-study.
 */
data class MemorySessionBlock(
    val phase: MemorySessionPhase,
    val exerciseId: MemoryExerciseId,
    val difficulty: MemoryDifficultyVector,
    val deferred: Boolean = false,
)

/** The plan for one session: its time blueprint plus the ordered active blocks. */
data class MemorySessionPlan(
    val template: MemorySessionTemplate,
    val blueprint: MemorySessionBlueprint,
    val blocks: List<MemorySessionBlock>,
) {
    init {
        require(blocks.isNotEmpty()) { "A session plan needs at least one block." }
    }
}

/**
 * Builds a [MemorySessionPlan] from a template, a focus exercise and the current
 * difficulty (doc §7). Pure and deterministic — it plans WHAT to run; the runtime
 * (Phase 12) calls the Phase-4 generators per block.
 *
 * Mapping of active phases to blocks:
 * - WARMUP → focus at relieved difficulty.
 * - DEEP_ENCODING → focus at immediate difficulty (encode + immediate recall).
 * - INTERFERENCE → the [interference] exercise (only if the blueprint has the phase
 *   and a distinct interference exercise is supplied).
 * - DELAYED_RECALL → focus, delay raised, marked [deferred].
 *
 * TRANSFER blocks are intentionally NOT produced here — functional transfer
 * protocols are Phase 8/11; the blueprint keeps the phase only for timing.
 */
class MemorySessionAssembler {

    fun assemble(
        template: MemorySessionTemplate,
        focus: MemoryExerciseId,
        difficulty: MemoryDifficultyVector,
        interference: MemoryExerciseId? = null,
    ): MemorySessionPlan {
        require(MemoryExerciseCatalog.all.containsKey(focus)) { "Unknown focus exercise $focus." }
        require(interference == null || interference != focus) {
            "Interference exercise must differ from the focus exercise."
        }
        val blueprint = MemorySessionBlueprints.forTemplate(template)

        val blocks = buildList {
            if (blueprint.hasPhase(MemorySessionPhase.WARMUP)) {
                add(MemorySessionBlock(MemorySessionPhase.WARMUP, focus, warmupDifficulty(difficulty)))
            }
            add(MemorySessionBlock(MemorySessionPhase.DEEP_ENCODING, focus, immediateDifficulty(difficulty)))
            if (blueprint.hasPhase(MemorySessionPhase.INTERFERENCE) && interference != null) {
                add(MemorySessionBlock(MemorySessionPhase.INTERFERENCE, interference, difficulty))
            }
            if (blueprint.hasPhase(MemorySessionPhase.DELAYED_RECALL)) {
                add(
                    MemorySessionBlock(
                        phase = MemorySessionPhase.DELAYED_RECALL,
                        exerciseId = focus,
                        difficulty = delayedDifficulty(difficulty),
                        deferred = true,
                    ),
                )
            }
        }
        return MemorySessionPlan(template, blueprint, blocks)
    }

    private fun warmupDifficulty(d: MemoryDifficultyVector): MemoryDifficultyVector =
        d.stepDown(MemoryDifficultyAxis.LOAD, WARMUP_LOAD_RELIEF)

    private fun immediateDifficulty(d: MemoryDifficultyVector): MemoryDifficultyVector =
        d.adjust(MemoryDifficultyAxis.DELAY, AxisLevel.MIN)

    private fun delayedDifficulty(d: MemoryDifficultyVector): MemoryDifficultyVector =
        d.adjust(MemoryDifficultyAxis.DELAY, DELAYED_RECALL_LEVEL)

    private companion object {
        const val WARMUP_LOAD_RELIEF = 0.3
        val DELAYED_RECALL_LEVEL = AxisLevel(0.6)
    }
}
