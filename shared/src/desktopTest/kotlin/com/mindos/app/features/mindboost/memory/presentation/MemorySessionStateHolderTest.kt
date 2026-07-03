package com.mindos.app.features.mindboost.memory.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredRecallRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemoryProfileRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryMemorySessionHistoryRepository
import com.mindos.app.features.mindboost.memory.data.InMemoryDeferredResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryPairsResultStore
import com.mindos.app.features.mindboost.memory.data.InMemoryRecentItemRegistryRepository
import com.mindos.app.features.mindboost.memory.data.InMemorySpanResultStore
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.domain.adaptive.DefaultMemoryAdaptiveEngine
import com.mindos.app.features.mindboost.memory.domain.antiplateau.InterventionPlanner
import com.mindos.app.features.mindboost.memory.domain.antiplateau.PlateauDetector
import com.mindos.app.features.mindboost.memory.domain.content.defaultMemoryContent
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallScheduler
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex1SpanGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex3ListGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.Ex4PairsGenerator
import com.mindos.app.features.mindboost.memory.domain.exercise.PairedAssociatesTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallDirection
import com.mindos.app.features.mindboost.memory.domain.metrics.DefaultMemoryMetricsEngine
import com.mindos.app.features.mindboost.memory.domain.model.AxisLevel
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemorySessionStateHolderTest {

    private val profiles = InMemoryMemoryProfileRepository()
    private val history = InMemoryMemorySessionHistoryRepository()
    private val deferred = InMemoryDeferredRecallRepository()
    private val recent = InMemoryRecentItemRegistryRepository()
    private val spanStore = InMemorySpanResultStore()
    private val pairsStore = InMemoryPairsResultStore()

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
        spanResultStore = spanStore,
        pairsResultStore = pairsStore,
        deferredResultStore = InMemoryDeferredResultStore(),
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
            // Complex span (EX1) inserts a concurrent-processing phase before recall; pairs (EX4) does not.
            (holder.uiState as? MemorySessionUiState.Processing)?.let { processing ->
                holder.submitProcessing(List(processing.tasks.size) { true })
                advanceUntilIdle()
            }
            val recall = assertIs<MemorySessionUiState.Recall>(holder.uiState)
            val selected = recall.candidates.filter { it.text in study.items.toSet() }.map { it.id }.toSet()
            holder.submitRecall(selected)
            advanceUntilIdle()
            assertIs<MemorySessionUiState.Finished>(holder.uiState)
        }
        coroutineContext.cancelChildren()
    }

    @Test
    fun complexSpan_presentsProcessingPhase_andCapturesAnswersIntoSpanTrial() = runTest {
        val holder = MemorySessionStateHolder(graph(), userId = "u_span", scope = this, seed = 7)

        holder.start(MemoryExerciseId.EX1_SPAN_COMPLEJO)
        advanceUntilIdle()
        val study = assertIs<MemorySessionUiState.Study>(holder.uiState)

        holder.continueFromStudy()
        advanceUntilIdle()
        // A complex span MUST gate recall behind concurrent processing — that is what makes it complex.
        val processing = assertIs<MemorySessionUiState.Processing>(holder.uiState)
        assertTrue(processing.tasks.isNotEmpty(), "Complex span must present concurrent-processing prompts.")

        // The UiState never reveals which answer is correct, so submit none: the processing demand is unmet.
        holder.submitProcessing(emptyList())
        advanceUntilIdle()

        val recall = assertIs<MemorySessionUiState.Recall>(holder.uiState)
        holder.submitRecall(emptySet())
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Finished>(holder.uiState)

        // The processing answers flowed into a SpanTrial: an unmet demand → this trial's processing is invalid.
        val trial = assertNotNull(holder.lastSpanTrial, "A complex-span session must capture a SpanTrial.")
        assertEquals(study.items.size, trial.sequenceLength)
        assertFalse(trial.processingCorrect, "Empty processing answers cannot satisfy the dual-task gate.")

        // P4: the trial is durably recorded in the encrypted span store, and a live score is computed.
        assertEquals(1, spanStore.load("u_span").trials.size, "The session's span trial must be persisted.")
        val score = assertNotNull(holder.lastSpanScore, "A complex-span session must compute a durable verdict.")
        assertFalse(score.updatesProfile, "One trial is far below the ≥20 gate — it must not update the profile.")

        coroutineContext.cancelChildren()
    }

    @Test
    fun lastSpanScore_isClearedWhenAComplexSpanIsFollowedByANonSpanSession() = runTest {
        val holder = MemorySessionStateHolder(graph(), userId = "u_seq", scope = this, seed = 7)

        // A complex-span session produces a span verdict.
        holder.start(MemoryExerciseId.EX1_SPAN_COMPLEJO)
        advanceUntilIdle()
        holder.continueFromStudy()
        advanceUntilIdle()
        (holder.uiState as? MemorySessionUiState.Processing)?.let {
            holder.submitProcessing(List(it.tasks.size) { true })
            advanceUntilIdle()
        }
        holder.submitRecall(emptySet())
        advanceUntilIdle()
        assertNotNull(holder.lastSpanScore)

        // A subsequent NON-span session on the SAME holder must not leave a stale span verdict.
        holder.start(MemoryExerciseId.EX3_LISTA_ESTRATEGIA)
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Study>(holder.uiState)
        holder.continueFromStudy()
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Recall>(holder.uiState)
        holder.submitRecall(emptySet())
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Finished>(holder.uiState)
        assertNull(holder.lastSpanScore, "A non-span session must clear the previous span verdict.")

        coroutineContext.cancelChildren()
    }

    @Test
    fun pairs_recordsTrialWithFalseAlarms_andComputesDurablePairsScore() = runTest {
        val holder = MemorySessionStateHolder(graph(), userId = "u_pairs", scope = this, seed = 7)

        holder.start(MemoryExerciseId.EX4_PARES_ASOCIADOS)
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Study>(holder.uiState)
        holder.continueFromStudy()
        advanceUntilIdle()
        // Pairs has NO processing phase → straight to recall.
        val recall = assertIs<MemorySessionUiState.Recall>(holder.uiState)

        // "Select everything" — every lure becomes a false alarm: the derivation must capture them so the
        // recognition score can't be gamed by over-selection.
        holder.submitRecall(recall.candidates.map { it.id }.toSet())
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Finished>(holder.uiState)

        val trial = assertNotNull(holder.lastPairsTrial, "A pairs session must capture a PairsTrial.")
        assertTrue(trial.distractorCount > 0, "the recall must include lures")
        assertEquals(trial.distractorCount, trial.falseAlarms, "select-all selects every lure as a false alarm")

        // P2b: the trial is durably recorded and a (non-VALID, one-trial) live score is computed.
        assertEquals(1, pairsStore.load("u_pairs").trials.size, "The session's pairs trial must be persisted.")
        val score = assertNotNull(holder.lastPairsScore, "A pairs session must compute a durable verdict.")
        assertFalse(score.updatesProfile, "One trial is far below the ≥20 gate — it must not update the profile.")
        assertNull(holder.lastSpanScore, "A pairs session leaves no span verdict.")

        coroutineContext.cancelChildren()
    }

    @Test
    fun pairs_bidirectional_countsOnlyAnswerableProbes_notUnhittableBackwardProbes() = runTest {
        // retrieval=MAX → bidirectional: the generator adds a BACKWARD probe per pair whose expected
        // answer is the CUE, which the flat-select recall never offers as a candidate.
        val difficulty = MemoryDifficultyVector(retrieval = AxisLevel.MAX)
        val plan = Ex4PairsGenerator()
            .generate(difficulty, defaultMemoryContent(), seed = 7, emptySet(), emptySet()) as PairedAssociatesTrial
        val forwardProbes = plan.probes.count { it.direction == RecallDirection.FORWARD }
        assertEquals(forwardProbes * 2, plan.probes.size, "retrieval=MAX must produce bidirectional probes")

        // Seed the profile so the holder generates the SAME bidirectional plan (same seed + difficulty).
        profiles.save(
            MemoryProfileState.initial("u_bidir").withDifficulty(MemoryExerciseId.EX4_PARES_ASOCIADOS, difficulty),
        )
        val holder = MemorySessionStateHolder(graph(), userId = "u_bidir", scope = this, seed = 7)
        holder.start(MemoryExerciseId.EX4_PARES_ASOCIADOS)
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Study>(holder.uiState)
        holder.continueFromStudy()
        advanceUntilIdle()
        val recall = assertIs<MemorySessionUiState.Recall>(holder.uiState)
        holder.submitRecall(recall.candidates.map { it.id }.toSet()) // select all → every answerable probe hit
        advanceUntilIdle()
        assertIs<MemorySessionUiState.Finished>(holder.uiState)

        // Unhittable BACKWARD probes must NOT inflate targetCount (the silent-halving bug); only the
        // answerable FORWARD probes count, so a complete selection yields hitRate 1.0, not 0.5.
        val trial = assertNotNull(holder.lastPairsTrial)
        assertEquals(forwardProbes, trial.targetCount, "backward probes must not inflate targetCount")
        assertEquals(trial.targetCount, trial.hits, "selecting all targets hits every answerable probe")

        coroutineContext.cancelChildren()
    }
}
