package com.mindos.app.features.mindboost.memory.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryPairsResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryRecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.data.InMemorySpanResultStore
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.domain.adaptive.DefaultMemoryAdaptiveEngine
import com.mindos.app.features.mindboost.memory.domain.antiplateau.InterventionPlanner
import com.mindos.app.features.mindboost.memory.domain.antiplateau.PlateauDetector
import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallScheduler
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex1SpanGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex3ListGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex4PairsGenerator
import com.mindos.app.features.mindboost.memory.domain.metrics.DefaultMemoryMetricsEngine
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.personalization.DefaultArchetypeInference
import com.mindos.app.features.mindboost.memory.domain.reporting.MemoryFitnessCalculator
import com.mindos.app.features.mindboost.memory.domain.session.DefaultMemoryRecoveryPolicy
import com.mindos.app.features.mindboost.memory.domain.session.MemorySessionAssembler
import com.mindos.app.features.mindboost.memory.presentation.state.DeferredRecallUiState
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeferredRecallSessionStateHolderTest {

    private val content = defaultMemoryContent()
    private val deferred = InMemoryDeferredRecallRepository()
    private val deferredResult = InMemoryDeferredResultStore()
    private val now = 100_000_000_000L
    private val dayMillis = 24L * 3_600_000L

    private fun graph() = MemoryGraph(
        content = content,
        generators = mapOf(
            MemoryExerciseId.EX1_SPAN_COMPLEJO to Ex1SpanGenerator(),
            MemoryExerciseId.EX3_LISTA_ESTRATEGIA to Ex3ListGenerator(),
            MemoryExerciseId.EX4_PARES_ASOCIADOS to Ex4PairsGenerator(),
        ),
        metricsEngine = DefaultMemoryMetricsEngine(),
        adaptiveEngine = DefaultMemoryAdaptiveEngine(),
        assembler = MemorySessionAssembler(),
        recoveryPolicy = DefaultMemoryRecoveryPolicy(),
        deferredScheduler = DeferredRecallScheduler(),
        archetypeInference = DefaultArchetypeInference(),
        plateauDetector = PlateauDetector(),
        interventionPlanner = InterventionPlanner(),
        fitnessCalculator = MemoryFitnessCalculator(),
        profileRepository = InMemoryMemoryProfileRepository(),
        sessionHistoryRepository = InMemoryMemorySessionHistoryRepository(),
        deferredRecallRepository = deferred,
        recentItemRegistryRepository = InMemoryRecentItemRegistryRepository(),
        spanResultStore = InMemorySpanResultStore(),
        pairsResultStore = InMemoryPairsResultStore(),
        deferredResultStore = deferredResult,
        epochClock = EpochClock { now },
    )

    private suspend fun seedItem(
        studiedAt: Long,
        count: Int = 5,
        exerciseId: MemoryExerciseId = MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
    ): List<String> {
        val studied = content.trainingWordPool().items.take(count)
        deferred.save(
            "u1",
            listOf(
                DeferredRecallItem(
                    id = "u1:1",
                    exerciseId = exerciseId,
                    language = Language.SPANISH_ES,
                    studiedWordIds = studied.map { it.id },
                    studiedAtEpoch = studiedAt,
                ),
            ),
        )
        return studied.map { it.id.value }
    }

    @Test
    fun noRegisteredItems_yieldsNothingDue() = runTest {
        val holder = DeferredRecallSessionStateHolder(graph(), userId = "u1", scope = this, seed = 7)
        holder.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.NothingDue>(holder.uiState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun pendingItem_outsideTheWindow_yieldsNothingDue_andFeedsNothingToTheProfile() = runTest {
        seedItem(studiedAt = now) // elapsed 0 < 20h → PENDING, not DUE
        val holder = DeferredRecallSessionStateHolder(graph(), userId = "u1", scope = this, seed = 7)
        holder.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.NothingDue>(holder.uiState)
        // Anti-fabrication: an immediate (too-soon) recall is NOT a 24h-retention measure, so it records
        // NO episodic outcome and produces NO score — nothing from it can ever reach EPISODIC_MEMORY.
        assertTrue(deferredResult.load("u1").outcomes.isEmpty(), "a PENDING item records no episodic outcome")
        assertNull(holder.lastResult, "no result for a non-due item")
        assertNull(holder.lastDeferredScore, "no episodic score for a non-due item")
        coroutineContext.cancelChildren()
    }

    @Test
    fun expiredItem_pastTheWindow_yieldsNothingDue_andFeedsNothingToTheProfile() = runTest {
        // Studied ~72h ago → beyond the 48h max window → EXPIRED, not DUE. A stale (>48h) recall is no
        // longer a 24h-retention measure, so it must ALSO feed nothing to the episodic profile.
        seedItem(studiedAt = now - 3L * dayMillis)
        val holder = DeferredRecallSessionStateHolder(graph(), userId = "u1", scope = this, seed = 7)
        holder.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.NothingDue>(holder.uiState)
        assertTrue(deferredResult.load("u1").outcomes.isEmpty(), "an EXPIRED item records no episodic outcome")
        assertNull(holder.lastResult, "no result for an expired item")
        assertNull(holder.lastDeferredScore, "no episodic score for an expired item")
        coroutineContext.cancelChildren()
    }

    @Test
    fun dueItem_presentsRecall_scoresHits_andConsumesTheItem() = runTest {
        val studiedIds = seedItem(studiedAt = now - dayMillis).toSet() // 24h elapsed → DUE
        val holder = DeferredRecallSessionStateHolder(graph(), userId = "u1", scope = this, seed = 7)

        holder.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(holder.uiState)
        // Perfect recall: select exactly the studied words among the presented candidates.
        val selected = recall.candidates.filter { it.id in studiedIds }.map { it.id }.toSet()
        assertEquals(5, selected.size, "all 5 studied words must be presentable candidates")
        holder.submitRecall(selected)
        advanceUntilIdle()

        assertIs<DeferredRecallUiState.Finished>(holder.uiState)
        val result = assertNotNull(holder.lastResult)
        assertEquals(MemoryExerciseId.EX3_LISTA_ESTRATEGIA, result.exerciseId)
        assertEquals(5, result.outcome.studiedCount)
        assertEquals(5, result.outcome.hits)
        assertEquals(0, result.outcome.falseAlarms)
        assertTrue(deferred.load("u1").isEmpty(), "a tested item is consumed (not re-tested)")

        // P3: the scored outcome is persisted, and a durable (one-outcome → non-VALID) score is computed.
        assertEquals(1, deferredResult.load("u1").outcomes.size, "the outcome is persisted")
        val deferredScore = assertNotNull(holder.lastDeferredScore)
        assertFalse(deferredScore.updatesProfile, "one outcome is far below the ≥20 gate — no profile update")
        coroutineContext.cancelChildren()
    }

    @Test
    fun selectEverything_capturesFalseAlarms_notAFree100() = runTest {
        seedItem(studiedAt = now - dayMillis)
        val holder = DeferredRecallSessionStateHolder(graph(), userId = "u1", scope = this, seed = 7)

        holder.start()
        advanceUntilIdle()
        val recall = assertIs<DeferredRecallUiState.Recall>(holder.uiState)
        holder.submitRecall(recall.candidates.map { it.id }.toSet()) // select everything
        advanceUntilIdle()

        val result = assertNotNull(holder.lastResult)
        assertTrue(result.outcome.distractorCount > 0, "the recall must include lures")
        assertEquals(result.outcome.distractorCount, result.outcome.falseAlarms, "select-all selects every lure")
        coroutineContext.cancelChildren()
    }

    @Test
    fun dueItem_thatCannotProduceLures_isSkipped_notPresentedLureless() = runTest {
        // An item whose exercise cannot sample distractors (unsupported in V1 content → sampler throws)
        // must be SKIPPED — never presented as a lure-less test that would bypass the discrimination gate.
        seedItem(studiedAt = now - dayMillis, exerciseId = MemoryExerciseId.EX2_ACTUALIZACION_SET)
        val holder = DeferredRecallSessionStateHolder(graph(), userId = "u1", scope = this, seed = 7)
        holder.start()
        advanceUntilIdle()
        assertIs<DeferredRecallUiState.NothingDue>(holder.uiState)
        coroutineContext.cancelChildren()
    }
}
