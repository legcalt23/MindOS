package com.mindos.app.features.mindboost.memory.di

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryPairsResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryRecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.data.InMemorySpanResultStore
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryGraphTest {

    /** Builds a graph with fakes (never rememberMemoryGraph, which is @Composable). */
    private fun fakeGraph(): MemoryGraph = MemoryGraph(
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
        profileRepository = InMemoryMemoryProfileRepository(),
        sessionHistoryRepository = InMemoryMemorySessionHistoryRepository(),
        deferredRecallRepository = InMemoryDeferredRecallRepository(),
        recentItemRegistryRepository = InMemoryRecentItemRegistryRepository(),
        spanResultStore = InMemorySpanResultStore(),
        pairsResultStore = InMemoryPairsResultStore(),
        deferredResultStore = InMemoryDeferredResultStore(),
        epochClock = EpochClock { 0L },
    )

    @Test
    fun graphConstructsWithFakes() {
        val graph = fakeGraph()
        assertNotNull(graph.metricsEngine)
        assertNotNull(graph.adaptiveEngine)
        assertNotNull(graph.profileRepository)
    }

    @Test
    fun generatorsCoverV1Wave() {
        val graph = fakeGraph()
        assertEquals(
            setOf(
                MemoryExerciseId.EX1_SPAN_COMPLEJO,
                MemoryExerciseId.EX3_LISTA_ESTRATEGIA,
                MemoryExerciseId.EX4_PARES_ASOCIADOS,
            ),
            graph.generators.keys,
        )
    }

    @Test
    fun generatorFor_returnsGeneratorOrNull() {
        val graph = fakeGraph()
        val gen = graph.generatorFor(MemoryExerciseId.EX1_SPAN_COMPLEJO)
        assertNotNull(gen)
        assertEquals(MemoryExerciseId.EX1_SPAN_COMPLEJO, gen.exerciseId)
        assertNull(graph.generatorFor(MemoryExerciseId.EX2_ACTUALIZACION_SET))
    }

    @Test
    fun generatorsMatchContentSupportedExercises() {
        val graph = fakeGraph()
        assertEquals(graph.content.supportedExercises(), graph.generators.keys)
    }

    @Test
    fun epochClockIsReadable() {
        assertTrue(fakeGraph().epochClock.nowMillis() >= 0L)
    }
}
