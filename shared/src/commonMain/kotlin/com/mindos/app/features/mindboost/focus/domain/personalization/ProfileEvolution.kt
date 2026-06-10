package com.mindos.app.features.mindboost.focus.domain.personalization

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.focus.domain.model.BaselineSnapshot
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserArchetype
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import kotlin.math.abs

/** Updates the living cognitive profile from new training data and retests. */
interface ProfileEvolution {
    fun evolve(
        current: UserCognitiveProfile,
        newData: List<SessionResult>,
        retest: BaselineSnapshot?,
    ): UserCognitiveProfile
}

/**
 * Default evolution. Re-infers the archetype but applies hysteresis (a confident,
 * different inference is needed to change at all) and a smooth transition (skill
 * rank never jumps more than one step — no BEGINNER→ELITE overnight). Confidence
 * consolidates while the archetype is stable. See 08_personalization_plan.md §3.
 */
class DefaultProfileEvolution(
    private val inference: ArchetypeInference,
    private val clock: EpochClock,
) : ProfileEvolution {

    override fun evolve(
        current: UserCognitiveProfile,
        newData: List<SessionResult>,
        retest: BaselineSnapshot?,
    ): UserCognitiveProfile {
        val signals = ArchetypeSignalsFactory.from(
            baseline = retest ?: current.baseline,
            recent = newData,
            totalSessions = current.totalSessions,
        )
        val inferred = inference.infer(signals)
        val (archetype, confidence) = resolve(current, inferred)
        return current.copy(
            archetype = archetype,
            archetypeConfidence = confidence,
            baseline = retest ?: current.baseline,
            updatedAtMs = clock.nowMillis(),
        )
    }

    private fun resolve(
        current: UserCognitiveProfile,
        inferred: ArchetypeInferenceResult,
    ): Pair<UserArchetype, Double> = when {
        // Same archetype → consolidate confidence (sustained evidence strengthens it).
        inferred.archetype == current.archetype ->
            current.archetype to maxOf(
                inferred.confidence,
                (current.archetypeConfidence + CONSOLIDATE_STEP).coerceAtMost(1.0),
            )

        // Different but not confident enough → keep current (hysteresis), decay confidence.
        inferred.confidence < CHANGE_THRESHOLD ->
            current.archetype to (current.archetypeConfidence * CONFIDENCE_DECAY)

        // Confident change → step at most one skill rank toward the target.
        else -> oneStepToward(current.archetype, inferred.archetype) to inferred.confidence
    }

    /** Skill rank: novice (0), competent/style (1), expert (2). */
    private fun rank(archetype: UserArchetype): Int = when (archetype) {
        UserArchetype.BEGINNER -> 0
        UserArchetype.ELITE -> 2
        else -> 1
    }

    /**
     * Moves at most one rank toward [target]. The only two-rank gaps are
     * BEGINNER↔ELITE; the stepping stone for those is ADVANCED.
     */
    private fun oneStepToward(current: UserArchetype, target: UserArchetype): UserArchetype =
        if (abs(rank(target) - rank(current)) <= 1) target else UserArchetype.ADVANCED

    private companion object {
        const val CHANGE_THRESHOLD = 0.30
        const val CONSOLIDATE_STEP = 0.10
        const val CONFIDENCE_DECAY = 0.90
    }
}
