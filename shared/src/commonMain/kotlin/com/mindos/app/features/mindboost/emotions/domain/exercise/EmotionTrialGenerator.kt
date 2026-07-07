package com.mindos.app.features.mindboost.emotions.domain.exercise

import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus

/**
 * Produces a deterministic stimulus sequence for one block. Determinism (same seed → same sequence) is
 * what makes the generator unit-testable and replayable. Generators use a seeded PRNG only — never a
 * non-deterministic source (no clock, no global Random).
 */
interface EmotionTrialGenerator {
    val exerciseId: EmotionExerciseId

    fun generate(difficulty: EmotionDifficultyVector, spec: EmotionBlockSpec, seed: Long): List<EmotionStimulus>
}

/**
 * What a block should look like. [recentPhrases] (MC8u-D §5) is the set of content phrases recently shown to THIS
 * user for content-bank exercises (EM3/EM10); the generator deprioritizes them so repeated sessions draw fresher
 * items. Empty by default — a generator that ignores it (EM1/EM2/EM8/EM11) is unaffected, and an empty set
 * reproduces the pre-guard behavior exactly.
 */
data class EmotionBlockSpec(
    val targetTrialCount: Int,
    val responseWindowMs: Long,
    val recentPhrases: Set<String> = emptySet(),
) {
    init {
        require(targetTrialCount > 0) { "targetTrialCount must be > 0." }
        require(responseWindowMs > 0) { "responseWindowMs must be > 0." }
    }
}
