package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
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
import com.mindos.app.features.mindboost.memory.presentation.ui.MemoryRootScreen
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MemoryRootScreenSmokeTest {

    private fun fakeGraph() = MemoryGraph(
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
        epochClock = EpochClock { 1_000L },
    )

    @Test
    fun memoryHome_rendersTitleAndExercises() = runComposeUiTest {
        setContent {
            MemoryRootScreen(
                userId = "u1",
                displayName = "Curro",
                onExit = {},
                graph = fakeGraph(),
            )
        }
        onNodeWithText("MindBoost · Memoria").assertIsDisplayed()
        onNodeWithText("Span Complejo").assertIsDisplayed()
    }
}
