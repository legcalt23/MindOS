package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveConfig
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryAdaptiveInput
import com.mindos.app.features.mindboost.memory.domain.adaptive.MemoryPerformance
import com.mindos.app.features.mindboost.memory.domain.catalog.MemoryExerciseCatalog
import com.mindos.app.features.mindboost.memory.domain.content.model.Language
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.content.sampling.RecentItemRegistry
import com.mindos.app.features.mindboost.memory.domain.content.sampling.SamplingFilters
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.exercise.ComplexSpanTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.MemoryTrialPlan
import com.mindos.app.features.mindboost.memory.domain.exercise.PairedAssociatesTrial
import com.mindos.app.features.mindboost.memory.domain.exercise.RecallListTrial
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryMetricKey
import com.mindos.app.features.mindboost.memory.domain.metrics.MemoryScorer
import com.mindos.app.features.mindboost.memory.domain.metrics.RecalledWord
import com.mindos.app.features.mindboost.memory.domain.metrics.ScoredTrial
import com.mindos.app.features.mindboost.memory.domain.metrics.valueOf
import com.mindos.app.features.mindboost.memory.domain.model.MemoryDifficultyVector
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.domain.model.MemoryProfileState
import com.mindos.app.features.mindboost.memory.domain.model.MemorySessionSummary
import com.mindos.app.features.mindboost.memory.presentation.state.MemorySessionResultUi
import com.mindos.app.features.mindboost.memory.presentation.state.MemorySessionUiState
import com.mindos.app.features.mindboost.memory.presentation.state.RecallOptionUi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Drives one memory session, turn-based: generate a trial from the [MemoryGraph]
 * → present study material → collect recall by recognition → score → metrics →
 * adaptive decision → persist (profile difficulty, session summary, recent items,
 * a 24h deferred-recall registration). The holder ORCHESTRATES; all computation
 * lives in the tested domain engines.
 *
 * V1 recall is recognition-based and uniform across the three paradigms (the
 * domain scorers stay faithful per type); timed presentation and paradigm-faithful
 * typed/serial input are a later UI refinement.
 */
@Suppress("TooManyFunctions")
class MemorySessionStateHolder(
    private val graph: MemoryGraph,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
) {
    var uiState: MemorySessionUiState by mutableStateOf(
        MemorySessionUiState.Intro("", ""),
    )
        private set

    private var sessionJob: Job? = null
    private var studyGate: CompletableDeferred<Unit>? = null
    private var recallGate: CompletableDeferred<Set<String>>? = null

    fun prepare(exerciseId: MemoryExerciseId) {
        val def = MemoryExerciseCatalog.definition(exerciseId)
        uiState = MemorySessionUiState.Intro(def.displayName, def.plainDescription)
    }

    fun start(exerciseId: MemoryExerciseId) {
        if (sessionJob?.isActive == true) return
        sessionJob = scope.launch { runSession(exerciseId) }
    }

    /** UI calls this when the user finishes studying. */
    fun continueFromStudy() {
        studyGate?.takeUnless { it.isCompleted }?.complete(Unit)
    }

    /** UI calls this with the ids the user recognised/recalled. */
    fun submitRecall(selectedIds: Set<String>) {
        recallGate?.takeUnless { it.isCompleted }?.complete(selectedIds)
    }

    fun abort(reason: String = "Sesión interrumpida") {
        sessionJob?.cancel()
        sessionJob = null
        studyGate = null
        recallGate = null
        uiState = MemorySessionUiState.Aborted(reason)
    }

    private suspend fun runSession(exerciseId: MemoryExerciseId) {
        val def = MemoryExerciseCatalog.definition(exerciseId)
        val generator = graph.generatorFor(exerciseId)
        if (generator == null) {
            uiState = MemorySessionUiState.Aborted("Ejercicio no disponible todavía.")
            return
        }
        val profile = graph.profileRepository.load(userId) ?: MemoryProfileState.initial(userId)
        val difficulty = profile.difficultyFor(exerciseId)
        val registry = graph.recentItemRegistryRepository.load(userId, exerciseId)
        val now = graph.epochClock.nowMillis()

        val plan = runCatching {
            generator.generate(
                difficulty = difficulty,
                content = graph.content,
                seed = seed,
                excludeRecentWordIds = registry?.recentWordIds(now) ?: emptySet(),
                excludeRecentPairIds = registry?.recentPairIds(now) ?: emptySet(),
            )
        }.getOrNull()
        if (plan == null) {
            uiState = MemorySessionUiState.Aborted("No se pudo preparar el ejercicio.")
            return
        }

        val study = studyOf(plan)
        uiState = MemorySessionUiState.Study(def.displayName, study.instruction, study.studyTexts)
        awaitGate(CompletableDeferred<Unit>().also { studyGate = it })

        val candidates = candidatesFor(exerciseId, study.answerWords)
        uiState = MemorySessionUiState.Recall(def.displayName, RECALL_INSTRUCTION, candidates.options)
        val selected = awaitRecall()

        finish(exerciseId, def.displayName, profile, difficulty, plan, study, candidates, selected)
    }

    @Suppress("LongParameterList") // Each value is an intrinsic part of finishing this session.
    private suspend fun finish(
        exerciseId: MemoryExerciseId,
        exerciseName: String,
        profile: MemoryProfileState,
        difficulty: MemoryDifficultyVector,
        plan: MemoryTrialPlan,
        study: StudyPresentation,
        candidates: Candidates,
        selected: Set<String>,
    ) {
        val scored = score(plan, candidates, selected)
        val metrics = graph.metricsEngine.blockMetrics(scored)
        val performance = MemoryPerformance.of(metrics, confidenceBias = 0.0, config = MemoryAdaptiveConfig())
        val decision = graph.adaptiveEngine.decide(
            MemoryAdaptiveInput(
                exercise = MemoryExerciseCatalog.definition(exerciseId),
                current = difficulty,
                recentBlock = metrics,
            ),
        )
        val endedAt = graph.epochClock.nowMillis()

        graph.profileRepository.save(profile.withDifficulty(exerciseId, decision.nextDifficulty))
        graph.sessionHistoryRepository.append(
            MemorySessionSummary(
                sessionId = "$userId:$endedAt",
                userId = userId,
                focusExerciseId = exerciseId,
                endedAtEpoch = endedAt,
                immediateAccuracy = metrics.valueOf(MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY).takeIf { it > 0.0 },
                delayedAccuracy = null,
                performance = performance,
            ),
        )
        persistRecentAndDeferred(exerciseId, study.answerWords.map { it.id }, endedAt)

        val accuracy = metrics.valueOf(MemoryMetricKey.IMMEDIATE_RECALL_ACCURACY)
            .takeIf { it > 0.0 } ?: metrics.valueOf(MemoryMetricKey.DELAYED_RECALL_ACCURACY)
        uiState = MemorySessionUiState.Finished(
            MemorySessionResultUi(
                exerciseName = exerciseName,
                immediateAccuracyPercent = (accuracy * PERCENT).roundToInt(),
                hits = scored.hits,
                studied = scored.studiedCount,
                honestNote = HONEST_NOTE,
            ),
        )
    }

    private suspend fun persistRecentAndDeferred(
        exerciseId: MemoryExerciseId,
        studiedIds: List<WordId>,
        endedAt: Long,
    ) {
        val registry = graph.recentItemRegistryRepository.load(userId, exerciseId)
            ?: RecentItemRegistry(exerciseId)
        graph.recentItemRegistryRepository.save(userId, registry.withWordsUsed(studiedIds, endedAt))

        if (studiedIds.isNotEmpty()) {
            val existing = graph.deferredRecallRepository.load(userId)
            val item = graph.deferredScheduler.register(
                id = "$userId:$endedAt",
                exerciseId = exerciseId,
                language = Language.SPANISH_ES,
                studiedWordIds = studiedIds,
                studiedAtEpoch = endedAt,
            )
            graph.deferredRecallRepository.save(userId, existing + item)
        }
    }

    private fun studyOf(plan: MemoryTrialPlan): StudyPresentation = when (plan) {
        is ComplexSpanTrial -> StudyPresentation(
            studyTexts = plan.expectedRecall.map { it.text },
            answerWords = plan.expectedRecall,
            instruction = "Memoriza estas palabras en orden.",
        )
        is RecallListTrial -> StudyPresentation(
            studyTexts = plan.studyList.map { it.text },
            answerWords = plan.studyList,
            instruction = "Memoriza esta lista.",
        )
        is PairedAssociatesTrial -> StudyPresentation(
            studyTexts = plan.studyPairs.map { "${it.cue.text} → ${it.target.text}" },
            answerWords = plan.studyPairs.map { it.target }.distinctBy { it.id },
            instruction = "Memoriza estos pares.",
        )
    }

    private fun candidatesFor(exerciseId: MemoryExerciseId, answerWords: List<WordItem>): Candidates {
        val answerIds = answerWords.map { it.id.value }.toSet()
        val distractors = runCatching {
            graph.content.samplerFor(exerciseId).sampleWords(
                count = DISTRACTOR_COUNT,
                seed = seed + 1,
                filters = SamplingFilters.None,
                excludeRecentIds = answerWords.map { it.id }.toSet(),
            )
        }.getOrDefault(emptyList())
        val all = (answerWords + distractors).shuffled(Random(seed))
        return Candidates(
            options = all.map { RecallOptionUi(it.id.value, it.text) },
            answerIds = answerIds,
        )
    }

    private fun score(plan: MemoryTrialPlan, candidates: Candidates, selected: Set<String>): ScoredTrial =
        when (plan) {
            is ComplexSpanTrial -> MemoryScorer.scoreComplexSpan(
                plan,
                recalled = plan.expectedRecall.map { w -> if (w.id.value in selected) RecalledWord(w) else null },
            )
            is RecallListTrial -> MemoryScorer.scoreList(
                plan,
                recalled = candidates.selectedWords(plan.studyList, selected),
            )
            is PairedAssociatesTrial -> MemoryScorer.scorePairs(
                plan,
                answers = plan.probes.map { p ->
                    if (p.expectedTarget.id.value in selected) RecalledWord(p.expectedTarget) else null
                },
            )
        }

    private suspend fun awaitGate(gate: CompletableDeferred<Unit>) {
        gate.await()
        studyGate = null
    }

    private suspend fun awaitRecall(): Set<String> {
        val gate = CompletableDeferred<Set<String>>().also { recallGate = it }
        val result = gate.await()
        recallGate = null
        return result
    }

    /** Per-paradigm study material plus the items to recall. */
    private data class StudyPresentation(
        val studyTexts: List<String>,
        val answerWords: List<WordItem>,
        val instruction: String,
    )

    /** Recall candidates (studied + distractors) and which ids count as studied. */
    private data class Candidates(
        val options: List<RecallOptionUi>,
        val answerIds: Set<String>,
    ) {
        /** Selected studied words for free-recall scoring (distractors map to null/ignored upstream). */
        fun selectedWords(studyList: List<WordItem>, selected: Set<String>): List<RecalledWord> =
            studyList.filter { it.id.value in selected }.map { RecalledWord(it) }
    }

    private companion object {
        const val DISTRACTOR_COUNT = 6
        const val PERCENT = 100.0
        const val RECALL_INSTRUCTION = "Selecciona lo que recuerdes."
        const val HONEST_NOTE =
            "Foto de hoy en este ejercicio. La mejora real se confirma con recuerdo diferido y " +
                "pruebas no entrenadas."
    }
}
