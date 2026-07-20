package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.personalization.ArchetypePersonalization
import com.mindos.app.features.mindboost.focus.domain.personalization.PersonalizationProfile
import com.mindos.app.features.mindboost.focus.domain.personalization.PersonalizationProfiles

/** One planned core block: which exercise to train, at what difficulty. */
data class PlannedBlock(
    val exerciseId: ExerciseId,
    val mode: Mode,
    val startingDifficulty: DifficultyVector,
    val phase: SessionPhase,
)

/**
 * The plan for one day's session. [volumeMultiplier] and [capDifficulty] encode
 * deload/recovery decisions for the runner to honor. An empty [blocks] list (with
 * [isRestDay]) means recovery recommended a rest day.
 */
data class DailyFocusPlan(
    val cycleWeek: Int,
    val template: SessionTemplate,
    val blocks: List<PlannedBlock>,
    val volumeMultiplier: Double,
    val capDifficulty: Boolean,
    val includeProbe: Boolean,
    val recoveryNote: String? = null,
) {
    val isRestDay: Boolean get() = blocks.isEmpty()
}

/** Generates the day's plan from the profile, cycle, availability and recent history. */
interface DailyPlanGenerator {
    fun generate(
        profile: UserCognitiveProfile,
        cycle: TrainingCycle,
        availableMinutes: Int,
        recent: List<SessionResult>,
    ): DailyFocusPlan
}

/**
 * Default generator. Deterministic given its inputs (no wall clock, no randomness):
 * the week comes from the profile's cycle position, the template from availability,
 * the content from the week's emphasis ∩ available exercises, and volume/cap from
 * the deload schedule and the recovery gate. See 07_ §4.
 */
class DefaultDailyPlanGenerator(
    private val recoveryPolicy: RecoveryPolicy = DefaultRecoveryPolicy(),
    private val availableExercises: Set<ExerciseId> = ExerciseCatalog.implemented.toSet(),
    /**
     * S0 containment (`MB-ENG-0008`). The daily plan is the SIXTH archetype route — one the plan's own
     * five-route inventory (a–e) missed, found by the S0 efficacy gate. It is also the most consequential
     * one, because it does not merely bias a difficulty: `selectExercises` RE-RANKS which exercise the
     * home screen recommends, so an archetype inferred from the contaminated v1 metrics chooses what the
     * user trains next. While this reads `false` the planner runs fully un-personalized.
     */
    private val archetypePersonalizationEnabled: () -> Boolean = {
        FeatureFlagsHub.current.focusScoreSurfacesEnabled
    },
) : DailyPlanGenerator {

    override fun generate(
        profile: UserCognitiveProfile,
        cycle: TrainingCycle,
        availableMinutes: Int,
        recent: List<SessionResult>,
    ): DailyFocusPlan {
        val week = cycle.weekFor(profile.cycleWeek)
        val template = SessionTemplate.forMinutes(availableMinutes)
        val gate = recoveryPolicy.gate(profile, recent)

        if (gate is RecoveryGate.RestRecommended) {
            return DailyFocusPlan(
                cycleWeek = week.index,
                template = template,
                blocks = emptyList(),
                volumeMultiplier = 0.0,
                capDifficulty = true,
                includeProbe = false,
                recoveryNote = gate.reason,
            )
        }

        val deloadVolume = if (week.isDeload) DELOAD_VOLUME else FULL_VOLUME
        val recoveryVolume = (gate as? RecoveryGate.Reduced)?.volumeMultiplier ?: FULL_VOLUME
        // S0: contained ⇒ NEUTRAL personalization, so neither the exercise ranking nor the starting
        // difficulty can be steered by an archetype derived from the contaminated v1 metrics.
        val personalizationOpen = archetypePersonalizationEnabled()
        val personalization = if (personalizationOpen) {
            PersonalizationProfiles.forArchetype(profile.archetype)
        } else {
            PersonalizationProfiles.NEUTRAL
        }
        val blocks = selectExercises(week, template, personalization).map { exerciseId ->
            PlannedBlock(
                exerciseId = exerciseId,
                mode = Mode.TRAINING,
                // Initial difficulty: the user's calibrated/adapted value, or an archetype-biased default.
                // Contained ⇒ a STORED value still wins (route (a), the user's own), but an absent one
                // falls back to EASIEST instead of the archetype's biased default.
                startingDifficulty = if (personalizationOpen) {
                    ArchetypePersonalization.startingDifficulty(
                        profile,
                        ExerciseCatalog.definition(exerciseId),
                    )
                } else {
                    profile.startingDifficulty[exerciseId] ?: DifficultyVector.EASIEST
                },
                phase = SessionPhase.CORE,
            )
        }

        return DailyFocusPlan(
            cycleWeek = week.index,
            template = template,
            blocks = blocks,
            volumeMultiplier = deloadVolume * recoveryVolume,
            capDifficulty = week.isDeload,
            includeProbe = week.evaluation != EvaluationRequirement.NONE,
            recoveryNote = (gate as? RecoveryGate.Reduced)?.reason,
        )
    }

    /**
     * Deterministically picks the day's exercises from the week emphasis ∩ available, re-ranked
     * "weakness first" by the archetype's exercise weights (08_ §2). The sort is STABLE, so equal weights
     * preserve the periodization's emphasis order (a neutral profile schedules exactly the week's order),
     * and round-robin over the ranking keeps the minimum variety the design requires.
     */
    private fun selectExercises(
        week: WeekPlan,
        template: SessionTemplate,
        personalization: PersonalizationProfile,
    ): List<ExerciseId> {
        val pool = week.emphasizedExercises.filter { it in availableExercises }
            .ifEmpty { availableExercises.toList() }
        if (pool.isEmpty()) return emptyList()
        val count = blockCountFor(template)
        val ranked = pool.sortedByDescending { personalization.weightFor(it) }
        return (0 until count).map { ranked[it % ranked.size] }
    }

    // Core blocks scale with session length; these counts ARE the design data.
    @Suppress("MagicNumber")
    private fun blockCountFor(template: SessionTemplate): Int = when (template) {
        SessionTemplate.MAINTENANCE_5 -> 1
        SessionTemplate.STANDARD_10 -> 2
        SessionTemplate.DEVELOPMENT_20 -> 3
        SessionTemplate.ELITE_30 -> 4
    }

    private companion object {
        const val FULL_VOLUME = 1.0
        const val DELOAD_VOLUME = 0.5
    }
}
