package com.mindos.app.features.mindboost.memory.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryRecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.domain.adaptive.DefaultMemoryAdaptiveEngine
import com.mindos.app.features.mindboost.memory.domain.antiplateau.InterventionPlanner
import com.mindos.app.features.mindboost.memory.domain.antiplateau.PlateauDetector
import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
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
import com.mindos.app.features.mindboost.memory.presentation.state.MemorySessionUiState
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemorySessionStateHolderTest {

    private val profiles = InMemoryMemoryProfileRepository()
    private val history = InMemoryMemorySessionHistoryRepository()
    private val deferred = InMemoryDeferredRecallRepository()
    private val recent = InMemoryRecentItemRegistryRepository()

    private fun graph() = MemoryGraph(
        content = defaultMemoryContent(),
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
        profileRepository = profiles,
        sessionHistoryRepository = history,
        deferredRecallRepository = deferred,
        recentItemRegistryRepository = recent,
        epochClock = EpochClock { 1_000L },
    )

    @Test
    fun fullList_sessionRunsStudyRecallFinishAndPersists() = runTest {
        val holder = MemorySessionStateHolder(graph(), userId = "u1", scope = this, seed = 42)

        holder.start(MemoryExerciseId.EX3_LISTA_ESTRATEGIA)
        advanceUntilIdle()
        val study = assertIs<MemorySessionUiState.Study>(holder.uiState)
        val studiedTexts = study.items.toSet()
        assertTrue(studiedTexts.isNotEmpty())

        holder.continueFromStudy()
        advanceUntilIdle()
        val recall = assertIs<MemorySessionUiState.Recall>(holder.uiState)

        // Perfect recall: select exactly the studied items among the candidates.
        val selected = recall.candidates.filter { it.text in studiedTexts }.map { it.id }.toSet()
        holder.submitRecall(selected)
        advanceUntilIdle()

        val finished = assertIs<MemorySessionUiState.Finished>(holder.uiState)
        assertEquals(finished.result.studied, finished.result.hits)
        assertEquals(100, finished.result.immediateAccuracyPercent)

        // Persistence side effects.
        assertTrue(profiles.load("u1") != null)
        assertTrue(history.recent("u1", 10).isNotEmpty())
        assertTrue(deferred.load("u1").isNotEmpty(), "A 24h deferred-recall item must be registered.")

        coroutineContext.cancelChildren()
    }

    @Test
    fun abort_setsAbortedState() = runTest {
        val holder = MemorySessionStateHolder(graph(), userId = "u1", scope = this, seed = 1)
        holder.start(MemoryExerciseId.EX1_SPAN_COMPLEJO)
        advanceUntilIdle()
        holder.abort("test")
        assertIs<MemorySessionUiState.Aborted>(holder.uiState)
        coroutineContext.cancelChildren()
    }

    @Test
    fun spanAndPairs_alsoFinish() = runTest {
        listOf(MemoryExerciseId.EX1_SPAN_COMPLEJO, MemoryExerciseId.EX4_PARES_ASOCIADOS).forEach { ex ->
            val holder = MemorySessionStateHolder(graph(), userId = "u_$ex", scope = this, seed = 7)
            holder.start(ex)
            advanceUntilIdle()
            val study = assertIs<MemorySessionUiState.Study>(holder.uiState)
            holder.continueFromStudy()
            advanceUntilIdle()
            val recall = assertIs<MemorySessionUiState.Recall>(holder.uiState)
            val selected = recall.candidates.filter { it.text in study.items.toSet() }.map { it.id }.toSet()
            holder.submitRecall(selected)
            advanceUntilIdle()
            assertIs<MemorySessionUiState.Finished>(holder.uiState)
        }
        coroutineContext.cancelChildren()
    }
}
