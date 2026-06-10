package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.catalog.ExerciseCatalog
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.SessionResult
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile

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
        val blocks = selectExercises(week, template).map { exerciseId ->
            PlannedBlock(
                exerciseId = exerciseId,
                mode = Mode.TRAINING,
                startingDifficulty = profile.startingDifficultyFor(exerciseId),
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

    /** Deterministically picks the day's exercises from the week emphasis ∩ available. */
    private fun selectExercises(week: WeekPlan, template: SessionTemplate): List<ExerciseId> {
        val pool = week.emphasizedExercises.filter { it in availableExercises }
            .ifEmpty { availableExercises.toList() }
        if (pool.isEmpty()) return emptyList()
        val count = blockCountFor(template)
        return (0 until count).map { pool[it % pool.size] }
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
