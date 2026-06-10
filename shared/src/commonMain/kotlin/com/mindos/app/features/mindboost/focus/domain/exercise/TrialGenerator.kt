package com.mindos.app.features.mindboost.focus.domain.exercise

import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus

/**
 * Produces a deterministic stimulus sequence for one block. Determinism (same
 * seed → same sequence) is what makes the generator unit-testable and replayable.
 * Generators use a seeded PRNG only — never a non-deterministic source.
 */
interface TrialGenerator {
    val exerciseId: ExerciseId

    fun generate(difficulty: DifficultyVector, spec: BlockSpec, seed: Long): List<Stimulus>
}

/** What a block should look like. Some exercises (E1) are time-bounded, not count-bounded. */
data class BlockSpec(
    val targetTrialCount: Int,
    val mode: Mode,
    val durationBudgetMs: Long? = null,
) {
    init {
        require(targetTrialCount > 0) { "targetTrialCount must be > 0." }
    }
}

/** Linear interpolation helper shared by difficulty mappings. */
internal fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t.coerceIn(0.0, 1.0)
