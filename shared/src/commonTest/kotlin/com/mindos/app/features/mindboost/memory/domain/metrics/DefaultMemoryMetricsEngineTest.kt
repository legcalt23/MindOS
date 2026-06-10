package com.mindos.app.features.mindboost.memory.domain.metrics

import com.mindos.app.features.mindboost.memory.domain.content.model.WordCategory
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallType
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryTrialOutcome
import com.mindos.app.features.mindboost.memory.domain.model.ReactionTimeMs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultMemoryMetricsEngineTest {

    private val engine = DefaultMemoryMetricsEngine()

    private fun resp(
        outcome: MemoryTrialOutcome,
        rt: Long? = null,
        category: WordCategory? = null,
    ) = ScoredResponse(
        outcome = outcome,
        rtMs = rt?.let { ReactionTimeMs(it) },
        category = category,
    )

    private fun MemoryMetricSet.require(key: MemoryMetricKey): MemoryMetricValue =
        requireNotNull(this[key]) { "metric $key missing" }

    @Test
    fun immediateRecallAccuracy_whenNoDelay() {
        val trial = ScoredTrial(
            exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            recallType = RecallType.FREE,
            delayMs = 0,
            studiedCount = 4,
            responses = listOf(
                resp(MemoryTrialOutcome.HIT), resp(MemoryTrialOutcome.HIT),
                resp(MemoryTrialOutcome.HIT), resp(MemoryTrialOutcome.INTRUSION),
            ),
            slotAligned = false,
        )
        val m = engine.blockMetrics(trial)
        assertEquals(0.75, m.require(MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY).value, 1e-9)
        assertFalse(m.containsKey(MemoryMetricKey.DELAYED_RECALL_ACCURACY))
        // free recall: omissions = studied - hits = 1 → 0.25
        assertEquals(0.25, m.require(MemoryMetricKey.OMISSION_RATE).value, 1e-9)
        assertEquals(0.25, m.require(MemoryMetricKey.INTRUSION_RATE).value, 1e-9)
    }

    @Test
    fun delayedRecallAccuracy_whenDelayed() {
        val trial = ScoredTrial(
            exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            recallType = RecallType.FREE,
            delayMs = 5_000,
            studiedCount = 2,
            responses = listOf(resp(MemoryTrialOutcome.HIT), resp(MemoryTrialOutcome.HIT)),
            slotAligned = false,
        )
        val m = engine.blockMetrics(trial)
        assertNotNull(m[MemoryMetricKey.DELAYED_RECALL_ACCURACY])
        // n=2 < MIN_N → not confident (honest reporting)
        assertFalse(m.require(MemoryMetricKey.DELAYED_RECALL_ACCURACY).confident)
    }

    @Test
    fun span_emitsPartialCreditWorkingMemorySpan() {
        val trial = ScoredTrial(
            exerciseId = MemoryExerciseId.EX1_SPAN_COMPLEJO,
            recallType = RecallType.SERIAL_FORWARD,
            delayMs = 0,
            studiedCount = 5,
            responses = listOf(
                resp(MemoryTrialOutcome.HIT), resp(MemoryTrialOutcome.HIT), resp(MemoryTrialOutcome.HIT),
                resp(MemoryTrialOutcome.ORDER_ERROR), resp(MemoryTrialOutcome.OMISSION),
            ),
        )
        val m = engine.blockMetrics(trial)
        assertEquals(3.0, m.require(MemoryMetricKey.WORKING_MEMORY_SPAN).value, 1e-9)
        // slot-aligned omission counts the explicit blank
        assertEquals(1.0 / 5.0, m.require(MemoryMetricKey.OMISSION_RATE).value, 1e-9)
    }

    @Test
    fun retrievalSpeed_isMadFilteredMedianOfHitRts() {
        val trial = ScoredTrial(
            exerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS,
            recallType = RecallType.CUED,
            delayMs = 0,
            studiedCount = 5,
            responses = listOf(
                resp(MemoryTrialOutcome.HIT, rt = 100), resp(MemoryTrialOutcome.HIT, rt = 102),
                resp(MemoryTrialOutcome.HIT, rt = 98), resp(MemoryTrialOutcome.HIT, rt = 101),
                resp(MemoryTrialOutcome.HIT, rt = 5_000), // outlier, MAD-rejected
            ),
        )
        val median = engine.blockMetrics(trial).require(MemoryMetricKey.RETRIEVAL_SPEED).value
        assertTrue(median in 99.0..102.0, "Outlier RT should not drag the median (was $median).")
    }

    @Test
    fun ex3FreeRecall_emitsArcClustering() {
        val trial = ScoredTrial(
            exerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
            recallType = RecallType.FREE,
            delayMs = 0,
            studiedCount = 6,
            responses = listOf(
                resp(MemoryTrialOutcome.HIT, category = WordCategory.ANIMALS),
                resp(MemoryTrialOutcome.HIT, category = WordCategory.ANIMALS),
                resp(MemoryTrialOutcome.HIT, category = WordCategory.ANIMALS),
                resp(MemoryTrialOutcome.HIT, category = WordCategory.FOODS),
                resp(MemoryTrialOutcome.HIT, category = WordCategory.FOODS),
                resp(MemoryTrialOutcome.HIT, category = WordCategory.FOODS),
            ),
            slotAligned = false,
        )
        val arc = engine.blockMetrics(trial).require(MemoryMetricKey.ARC_CLUSTERING)
        assertEquals(1.0, arc.value, 1e-9)
        assertTrue(arc.confident, "6 clustered items should yield a confident ARC.")
    }

    @Test
    fun ex4_emitsAssociativeAndConjunction() {
        val trial = ScoredTrial(
            exerciseId = MemoryExerciseId.EX4_PARES_ASOCIADOS,
            recallType = RecallType.CUED,
            delayMs = 0,
            studiedCount = 4,
            responses = listOf(
                resp(MemoryTrialOutcome.HIT), resp(MemoryTrialOutcome.HIT),
                resp(MemoryTrialOutcome.CONJUNCTION_ERROR), resp(MemoryTrialOutcome.INTRUSION),
            ),
        )
        val m = engine.blockMetrics(trial)
        assertEquals(0.5, m.require(MemoryMetricKey.ASSOCIATIVE_INDEX).value, 1e-9)
        assertEquals(0.25, m.require(MemoryMetricKey.CONJUNCTION_ERROR_RATE).value, 1e-9)
    }

    @Test
    fun consolidationIndex_andUnavailableGuard() {
        assertEquals(0.75, engine.consolidationIndex(0.8, 0.6, n = 10).value, 1e-9)
        assertFalse(engine.consolidationIndex(0.0, 0.6, n = 10).confident)
    }

    @Test
    fun dualTaskCost_andUnavailableGuard() {
        assertEquals(1.0 / 3.0, engine.dualTaskCost(0.9, 0.6, n = 10).value, 1e-9)
        assertFalse(engine.dualTaskCost(0.0, 0.6, n = 10).confident)
    }
}
