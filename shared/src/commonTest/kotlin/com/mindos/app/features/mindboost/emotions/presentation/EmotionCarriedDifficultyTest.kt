@file:Suppress("MagicNumber", "MaxLineLength") // Persisted-row fixtures are the data under test.

package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.SeparatorItemFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ola 0 · S1 — the staircase must survive the end of a session.
 *
 * Difficulty was rebuilt from the catalog default on every launch, so no session could ever hand its calibration
 * to the next one and the final block's decision — now the only one that can raise difficulty — was computed and
 * thrown away. These tests pin the resume rule and, just as importantly, the cases where resuming would be wrong.
 */
class EmotionCarriedDifficultyTest {

    private fun row(
        exercise: EmotionExerciseId,
        axis: EmotionAxis,
        level: Double,
        startedAtMillis: Long,
        endReason: String = "COMPLETED",
    ) = EmotionSessionSummaryDto(
        exerciseId = exercise.name,
        startedAtMillis = startedAtMillis,
        durationMs = 60_000,
        difficultySummary = DifficultySummaryDto(axis.name, level, 1),
        metricsSummary = emptyList(),
        completed = endReason == "COMPLETED",
        abandoned = endReason == "ABANDONED",
        endReason = endReason,
    )

    private fun carried(summaries: List<EmotionSessionSummaryDto>, ceiling: Double = 1.0) =
        carriedDifficultyLevel(summaries, EmotionExerciseId.EM1_PAUSA, EmotionAxis.INHIBITORY_DEMAND, ceiling)

    @Test
    fun the_most_recent_session_of_the_same_exercise_sets_the_starting_level() {
        val level = carried(
            listOf(
                row(EmotionExerciseId.EM1_PAUSA, EmotionAxis.INHIBITORY_DEMAND, 0.40, startedAtMillis = 100),
                row(EmotionExerciseId.EM1_PAUSA, EmotionAxis.INHIBITORY_DEMAND, 0.61, startedAtMillis = 300),
                row(EmotionExerciseId.EM1_PAUSA, EmotionAxis.INHIBITORY_DEMAND, 0.55, startedAtMillis = 200),
            ),
        )
        assertEquals(0.61, level)
    }

    @Test
    fun an_exercise_never_inherits_another_exercises_staircase() {
        // EM1 and EM8 share a governing axis, so matching on the axis alone would let one exercise's calibration
        // silently become the other's starting point.
        val level = carried(listOf(row(EmotionExerciseId.EM8_ANTIIMPULSO, EmotionAxis.INHIBITORY_DEMAND, 0.9, startedAtMillis = 300)))
        assertNull(level, "a different exercise's row must not resume this exercise")
    }

    @Test
    fun a_row_recorded_on_a_different_governing_axis_is_not_resumed() {
        val level = carried(listOf(row(EmotionExerciseId.EM1_PAUSA, EmotionAxis.TIME_ON_TASK, 0.9, startedAtMillis = 300)))
        assertNull(level, "a level measured on another axis says nothing about this one")
    }

    @Test
    fun no_history_means_the_catalog_default_stands() {
        assertNull(carried(emptyList()), "with nothing to resume the caller must keep the catalog default")
    }

    @Test
    fun a_session_that_ended_in_a_safety_stop_resets_instead_of_resuming() {
        // The important half. Simply SKIPPING a safety-stopped row would reach back to the older, HIGHER level and
        // greet the user with more load immediately after the session that overloaded them.
        val level = carried(
            listOf(
                row(EmotionExerciseId.EM1_PAUSA, EmotionAxis.INHIBITORY_DEMAND, 0.85, startedAtMillis = 100),
                row(EmotionExerciseId.EM1_PAUSA, EmotionAxis.INHIBITORY_DEMAND, 0.70, startedAtMillis = 300, endReason = EmotionTerminalSemantics.SAFETY_STOP),
            ),
        )
        assertNull(level, "after a safety stop the next session must start from the default, not from an older harder row")
    }

    @Test
    fun the_resumed_level_never_exceeds_what_the_content_can_deliver() {
        val level = carried(
            listOf(row(EmotionExerciseId.EM1_PAUSA, EmotionAxis.INHIBITORY_DEMAND, 0.95, startedAtMillis = 300)),
            ceiling = 0.8,
        )
        assertEquals(0.8, level, "a durable starting point must not be a difficulty the generator has to water down")
    }

    @Test
    fun the_classification_ceiling_is_derived_from_the_bank_and_no_longer_binds_at_real_block_sizes() {
        // Not a pinned number: the bank's ambiguous training items divided by the block size. This cap existed
        // because the authored bank held so few ambiguous items that a normal block already exhausted them and
        // the selector backfilled with clear ones — the difficulty number rose while the task did not. Ola 2 ·
        // R3 · P2 removed the scarcity, so at real block sizes the ceiling is now 1.0. What must NOT be lost is
        // the mechanism: it still has to bite when a block asks for more ambiguity than the bank can supply.
        val ambiguousTraining = SeparatorItemFactory.ALL.count { !it.isEval && it.ambiguity == 1 }
        assertEquals(
            1.0,
            deliverableDifficultyCeiling(EmotionExerciseId.EM3_SEPARADOR, trialsPerBlock = 20),
            "a normal block no longer outruns the ambiguous supply ($ambiguousTraining items)",
        )

        val stretched = deliverableDifficultyCeiling(EmotionExerciseId.EM3_SEPARADOR, ambiguousTraining * 2)
        assertTrue(stretched < 1.0, "the ceiling must still bite when a block asks for more than the bank holds")
        assertTrue(
            deliverableDifficultyCeiling(EmotionExerciseId.EM3_SEPARADOR, ambiguousTraining * 4) < stretched,
            "a larger block dilutes the reachable ambiguity, so its ceiling must be lower",
        )
        assertEquals(
            1.0,
            deliverableDifficultyCeiling(EmotionExerciseId.EM1_PAUSA, trialsPerBlock = 20),
            "only the content-bank exercises are limited by authored items",
        )
    }
}
