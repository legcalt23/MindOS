package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId

/** Training volume for a week. */
enum class Volume { LOW, MODERATE, HIGH }

/** How much evaluation a week requires. */
enum class EvaluationRequirement { NONE, LIGHT, FULL }

/** The plan for one week of the periodized cycle. */
data class WeekPlan(
    val index: Int,
    val focus: String,
    val emphasizedExercises: List<ExerciseId>,
    val volume: Volume,
    val isDeload: Boolean,
    val evaluation: EvaluationRequirement,
) {
    init {
        require(index in 1..TrainingCycle.WEEKS) { "Week index must be in 1..${TrainingCycle.WEEKS}." }
        require(emphasizedExercises.isNotEmpty()) { "A week must emphasize at least one exercise." }
    }
}

/** An 8-week periodized cycle. Deload weeks (4 and 8) carry the full retests. */
data class TrainingCycle(val weeks: List<WeekPlan>) {
    init {
        require(weeks.size == WEEKS) { "A cycle needs exactly $WEEKS weeks." }
        require(weeks.map { it.index } == (1..WEEKS).toList()) { "Weeks must be indexed 1..$WEEKS in order." }
    }

    /** Week plan for [cycleWeek], clamped into 1..[WEEKS]. */
    fun weekFor(cycleWeek: Int): WeekPlan = weeks[(cycleWeek - 1).coerceIn(0, weeks.size - 1)]

    companion object {
        const val WEEKS = 8
    }
}

/** The canonical 8-week cycle (07_session_periodization_plan.md §1). */
object TrainingCycles {

    val STANDARD_8_WEEK = TrainingCycle(
        listOf(
            WeekPlan(
                index = 1,
                focus = "Base + diagnóstico",
                emphasizedExercises = listOf(ExerciseId.E1_VIGIA, ExerciseId.E2_FILTRO, ExerciseId.E3_GATILLO),
                volume = Volume.LOW,
                isDeload = false,
                evaluation = EvaluationRequirement.FULL,
            ),
            WeekPlan(
                index = 2,
                focus = "Sostenida + selectiva",
                emphasizedExercises = listOf(ExerciseId.E1_VIGIA, ExerciseId.E2_FILTRO, ExerciseId.E7_RASTREO),
                volume = Volume.MODERATE,
                isDeload = false,
                evaluation = EvaluationRequirement.LIGHT,
            ),
            WeekPlan(
                index = 3,
                focus = "Inhibición + interferencia",
                emphasizedExercises = listOf(ExerciseId.E3_GATILLO, ExerciseId.E4_FRENO, ExerciseId.E5_CONFLICTO),
                volume = Volume.MODERATE,
                isDeload = false,
                evaluation = EvaluationRequirement.LIGHT,
            ),
            WeekPlan(
                index = 4,
                focus = "Descarga + evaluación",
                emphasizedExercises = listOf(ExerciseId.E1_VIGIA, ExerciseId.E2_FILTRO),
                volume = Volume.LOW,
                isDeload = true,
                evaluation = EvaluationRequirement.FULL,
            ),
            WeekPlan(
                index = 5,
                focus = "Flexibilidad + recuperación",
                emphasizedExercises = listOf(ExerciseId.E6_CAMBIO, ExerciseId.E12_REENGANCHE, ExerciseId.E10_FLUJO),
                volume = Volume.MODERATE,
                isDeload = false,
                evaluation = EvaluationRequirement.LIGHT,
            ),
            WeekPlan(
                index = 6,
                focus = "Integración / doble tarea",
                emphasizedExercises = listOf(ExerciseId.E8_DUO, ExerciseId.E9_ENJAMBRE, ExerciseId.E11_ANCLA),
                volume = Volume.HIGH,
                isDeload = false,
                evaluation = EvaluationRequirement.LIGHT,
            ),
            WeekPlan(
                index = 7,
                focus = "Pico bajo presión",
                emphasizedExercises = listOf(ExerciseId.E13_PRESION, ExerciseId.E11_ANCLA, ExerciseId.E8_DUO),
                volume = Volume.HIGH,
                isDeload = false,
                evaluation = EvaluationRequirement.LIGHT,
            ),
            WeekPlan(
                index = 8,
                focus = "Descarga + evaluación final",
                emphasizedExercises = listOf(ExerciseId.E1_VIGIA, ExerciseId.E2_FILTRO),
                volume = Volume.LOW,
                isDeload = true,
                evaluation = EvaluationRequirement.FULL,
            ),
        ),
    )
}
