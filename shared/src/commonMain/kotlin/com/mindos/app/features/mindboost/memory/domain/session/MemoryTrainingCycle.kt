package com.mindos.app.features.mindboost.memory.domain.session

import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.session.MemoryEvaluationRequirement.FULL
import com.mindos.app.features.mindboost.memory.domain.session.MemoryEvaluationRequirement.LIGHT
import com.mindos.app.features.mindboost.memory.domain.session.MemoryVolume.HIGH
import com.mindos.app.features.mindboost.memory.domain.session.MemoryVolume.LOW
import com.mindos.app.features.mindboost.memory.domain.session.MemoryVolume.MODERATE

/** Training volume for a week. */
enum class MemoryVolume { LOW, MODERATE, HIGH }

/** How much evaluation a week requires. */
enum class MemoryEvaluationRequirement { NONE, LIGHT, FULL }

/** The plan for one week of the periodized cycle (doc §8). */
data class MemoryWeekPlan(
    val index: Int,
    val focus: String,
    val emphasizedExercises: List<MemoryExerciseId>,
    val volume: MemoryVolume,
    val isDeload: Boolean,
    val evaluation: MemoryEvaluationRequirement,
) {
    init {
        require(index in 1..MemoryTrainingCycle.WEEKS) { "Week index must be in 1..${MemoryTrainingCycle.WEEKS}." }
        require(emphasizedExercises.isNotEmpty()) { "A week must emphasize at least one exercise." }
    }
}

/** An 8-week periodized cycle (doc §8). Deload weeks (4 and 8) carry the full retests. */
data class MemoryTrainingCycle(val weeks: List<MemoryWeekPlan>) {
    init {
        require(weeks.size == WEEKS) { "A cycle needs exactly $WEEKS weeks." }
        require(weeks.map { it.index } == (1..WEEKS).toList()) { "Weeks must be indexed 1..$WEEKS in order." }
        require(weeks.filter { it.isDeload }.map { it.index } == DELOAD_WEEKS) {
            "Deload weeks must be exactly $DELOAD_WEEKS (doc §8)."
        }
    }

    /** Week plan for [cycleWeek], clamped into 1..[WEEKS]. */
    fun weekFor(cycleWeek: Int): MemoryWeekPlan = weeks[(cycleWeek - 1).coerceIn(0, weeks.size - 1)]

    companion object {
        const val WEEKS = 8
        val DELOAD_WEEKS = listOf(4, 8)
    }
}

/**
 * The canonical 8-week cycle (doc §8). Emphasis is restricted to the V1 implemented
 * exercises (EX1/EX3/EX4); the weekly focus text preserves the doc's themes even
 * where the driving exercises (interference, faces, narrative…) arrive in later waves.
 */
@Suppress("MagicNumber") // Week indices 1..8 are the canonical cycle data (doc §8).
object MemoryTrainingCycles {

    private val WM = MemoryExerciseId.EX1_SPAN_COMPLEJO
    private val EPISODIC = MemoryExerciseId.EX3_LISTA_ESTRATEGIA
    private val ASSOC = MemoryExerciseId.EX4_PARES_ASOCIADOS

    val STANDARD_8_WEEK = MemoryTrainingCycle(
        listOf(
            MemoryWeekPlan(1, "Calibración y fundamentos", listOf(WM, EPISODIC, ASSOC), LOW, false, FULL),
            MemoryWeekPlan(2, "Codificación básica", listOf(EPISODIC, ASSOC), MODERATE, false, LIGHT),
            MemoryWeekPlan(3, "Recuperación activa", listOf(WM, EPISODIC, ASSOC), MODERATE, false, LIGHT),
            MemoryWeekPlan(4, "Interferencia básica + descarga", listOf(EPISODIC, ASSOC), LOW, true, FULL),
            MemoryWeekPlan(5, "Profundidad y asociación", listOf(EPISODIC, ASSOC), HIGH, false, LIGHT),
            MemoryWeekPlan(6, "Resistencia y presión", listOf(WM, ASSOC), HIGH, false, LIGHT),
            MemoryWeekPlan(7, "Integración y transferencia", listOf(WM, EPISODIC, ASSOC), MODERATE, false, LIGHT),
            MemoryWeekPlan(8, "Evaluación final + descarga", listOf(WM, EPISODIC, ASSOC), LOW, true, FULL),
        ),
    )
}
