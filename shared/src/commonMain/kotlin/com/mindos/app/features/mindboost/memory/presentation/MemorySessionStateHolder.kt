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
import com.mindos.app.features.mindboost.memory.domain.exercise.ProcessingTask
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
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.memory.domain.scoring.ComplexSpanLiveScorer
import com.mindos.app.features.mindboost.memory.domain.scoring.MemoryPairsScore
import com.mindos.app.features.mindboost.memory.domain.scoring.MemorySpanScore
import com.mindos.app.features.mindboost.memory.domain.scoring.PairsTrial
import com.mindos.app.features.mindboost.memory.domain.scoring.SpanTrial
import com.mindos.app.features.mindboost.memory.presentation.state.MemorySessionResultUi
import com.mindos.app.features.mindboost.memory.presentation.state.MemorySessionUiState
import com.mindos.app.features.mindboost.memory.presentation.state.RecallOptionUi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
@Suppress("TooManyFunctions", "LongParameterList")
class MemorySessionStateHolder(
    private val graph: MemoryGraph,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
    private val recorder: SessionRecorder = NoOpSessionRecorder,
    private val sessionIdGenerator: SessionIdGenerator = RandomSessionIdGenerator(),
) {
    var uiState: MemorySessionUiState by mutableStateOf(
        MemorySessionUiState.Intro("", ""),
    )
        private set

    private var sessionJob: Job? = null
    private var studyGate: CompletableDeferred<Unit>? = null
    private var processingGate: CompletableDeferred<List<Boolean>>? = null
    private var recallGate: CompletableDeferred<Set<String>>? = null

    // Serializes every recorder lifecycle op for this holder so STARTED / FINALIZED / ABANDONED never overlap.
    private val recorderMutex = Mutex()
    // Opaque write-ahead id + its STARTED job for the current session — separate from the internal
    // "$userId:$endedAt" ids. Set only once a REAL session starts (a valid plan, at Study); the null-plan /
    // null-generator early-outs write no SessionRecord. No memory content ever reaches the recorder.
    private var recorderSessionId: String = ""
    private var recorderStartJob: Job? = null
    private var recorderStartedAtMs: Long = 0L
    private var recorderFinalized: Boolean = false

    /**
     * P1 EX1 · the complex-span trial captured this session — concurrent-processing validity folded
     * with scored recall, [ComplexSpanLiveScorer]. Null for non-span paradigms and before [finish].
     * A later phase accumulates these toward the Memoria profile.
     */
    var lastSpanTrial: SpanTrial? = null
        private set

    /**
     * P1 EX1 (P4) · the durable span verdict after the last complex-span session — derived from the
     * accumulated, persisted log (not just this session). Null for non-span paradigms / before finish.
     * Reaches VALID only once ≥20 valid trials have accumulated; the brain-profile reads it in a later phase.
     */
    var lastSpanScore: ScoreResult? = null
        private set

    /**
     * P1 EX4 · the paired-associates recognition trial captured this session (hits + false-alarms vs the
     * candidate set). Null for non-pairs paradigms / before [finish].
     */
    var lastPairsTrial: PairsTrial? = null
        private set

    /**
     * P1 EX4 (P2b) · the durable associative verdict after the last paired-associates session — derived
     * from the accumulated, persisted log (not just this session). Null for non-pairs paradigms / before
     * finish. Reaches VALID only once ≥20 discriminating trials accumulate (the brain-profile reads it later).
     */
    var lastPairsScore: ScoreResult? = null
        private set

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

    /** UI calls this with one yes/no per processing prompt, in [MemorySessionUiState.Processing.tasks] order. */
    fun submitProcessing(answers: List<Boolean>) {
        processingGate?.takeUnless { it.isCompleted }?.complete(answers)
    }

    /** UI calls this with the ids the user recognised/recalled. */
    fun submitRecall(selectedIds: Set<String>) {
        recallGate?.takeUnless { it.isCompleted }?.complete(selectedIds)
    }

    /**
     * Explicit, completion-aware abandonment. Stops the active session, then — if a real session STARTED and did
     * NOT finalize — awaits its STARTED and marks the SessionRecord ABANDONED under the mutex BEFORE exposing the
     * terminal Aborted state. A pre-session abort has no sessionId → no artificial ABANDONED; a finalized session
     * is never abandoned. If this coroutine is cancelled during onAbandon, Aborted is never falsely exposed and
     * recovery remains the backstop. Only the opaque sessionId reaches the recorder — no memory content.
     */
    suspend fun abortAndAwait(reason: String = "Sesión interrumpida") {
        sessionJob?.cancel()
        sessionJob = null
        studyGate = null
        processingGate = null
        recallGate = null
        val sessionId = recorderSessionId
        val startJob = recorderStartJob
        if (sessionId.isNotEmpty() && !recorderFinalized) {
            startJob?.join()
            recorderMutex.withLock { recorder.onAbandon(sessionId) }
        }
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
        // Real session begins — write-ahead STARTED (lifecycle only: opaque id, block, one planned round).
        // Serialized under the mutex; the job is kept so finalize/abandon can join it. No study words, pairs,
        // sequences, or answers ever reach the recorder. The epoch start is the basis for activeDurationMs.
        val sessionId = sessionIdGenerator.newSessionId()
        recorderSessionId = sessionId
        val startedAtMs = graph.epochClock.nowMillis()
        recorderStartedAtMs = startedAtMs
        recorderStartJob = scope.launch {
            recorderMutex.withLock {
                recorder.onStart(sessionId, ScoreModuleId.MEMORY, startedAtMs, plannedTrials = 1)
            }
        }
        uiState = MemorySessionUiState.Study(def.displayName, study.instruction, study.studyTexts)
        awaitGate(CompletableDeferred<Unit>().also { studyGate = it })

        val processingAnswers = presentProcessing(def.displayName, plan)

        val candidates = candidatesFor(exerciseId, study.answerWords)
        uiState = MemorySessionUiState.Recall(def.displayName, RECALL_INSTRUCTION, candidates.options)
        val selected = awaitRecall()

        finish(exerciseId, def.displayName, profile, difficulty, plan, study, candidates, selected, processingAnswers)
    }

    /**
     * Complex span only: present the concurrent-processing prompts and collect one yes/no per prompt.
     * Returns the answers in prompt order; an empty list for paradigms without a processing demand
     * (recall list, paired associates), which skip straight to recall unchanged.
     */
    private suspend fun presentProcessing(exerciseName: String, plan: MemoryTrialPlan): List<Boolean> {
        val tasks = processingTasksOf(plan)
        if (tasks.isEmpty()) return emptyList()
        uiState = MemorySessionUiState.Processing(exerciseName, PROCESSING_INSTRUCTION, tasks.map { it.prompt })
        return awaitProcessing()
    }

    private fun processingTasksOf(plan: MemoryTrialPlan): List<ProcessingTask> = when (plan) {
        is ComplexSpanTrial -> plan.steps.filterIsInstance<ProcessingTask>()
        is RecallListTrial -> emptyList()
        is PairedAssociatesTrial -> emptyList()
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
        processingAnswers: List<Boolean>,
    ) {
        val scored = score(plan, candidates, selected)
        lastSpanTrial = (plan as? ComplexSpanTrial)?.let {
            ComplexSpanLiveScorer.toSpanTrial(it, processingAnswers, scored)
        }
        lastPairsTrial = (plan as? PairedAssociatesTrial)?.let { pairsTrialOf(it, candidates, selected) }
        // Reset symmetrically with the captured trials; record* repopulates each score only for its own
        // paradigm, so a later session of another paradigm can never leave a stale verdict (KDoc contract).
        lastSpanScore = null
        lastPairsScore = null
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
        lastSpanTrial?.let { recordSpan(exerciseId, endedAt, it, processingAnswers) }
        lastPairsTrial?.let { recordPairs(exerciseId, endedAt, it) }

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

        // Persist FINALIZED after the block's own persistence and BEFORE exposing Finished (canonical-lifecycle
        // rule). Wait for STARTED, finalize under the mutex with the captured id. Counts only: completedTrials
        // is a fixed round count (1), never hits/studiedCount (those are the cognitive signal, not lifecycle).
        recorderStartJob?.join()
        val sessionId = recorderSessionId
        recorderMutex.withLock {
            recorder.onFinalize(sessionId, completedTrials = 1, activeDurationMs = endedAt - recorderStartedAtMs)
        }
        recorderFinalized = true

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

    /**
     * P4 · Persists the captured span trial (+ the session's processing responses, 0/1) and recomputes
     * the durable [lastSpanScore] from the FULL accumulated log — so the verdict reflects every session,
     * not just this one. The span store shares the encrypted `mindboost_memory` namespace.
     */
    private suspend fun recordSpan(
        exerciseId: MemoryExerciseId,
        endedAt: Long,
        trial: SpanTrial,
        processingAnswers: List<Boolean>,
    ) {
        val responses = processingAnswers.map { if (it) 1 else 0 }
        val log = graph.spanResultStore.record(userId, trial, responses)
        lastSpanScore = MemorySpanScore.scoreOf(
            log = log,
            exerciseId = exerciseId.name,
            sessionId = "$userId:$endedAt",
            userId = userId,
            timestampMs = endedAt,
        )
    }

    /**
     * P1 EX4 · Builds the honest recognition record from the live flat-multi-select. Only ANSWERABLE
     * probes count — those whose expected answer is actually present in the candidate set. The V1 recall
     * offers targets + distractors, so a BACKWARD probe (expects the pair's cue, which is never a
     * candidate) is unanswerable and must NOT deflate the score; hits and targetCount stay on one scale,
     * making the cross-session index comparable across difficulty levels. False-alarms are the SELECTED
     * candidates that are NOT a correct answer (so "select everything" yields a high false-alarm rate, not
     * a free 100%). Returns null if no probe is answerable (nothing measurable to record).
     */
    private fun pairsTrialOf(
        plan: PairedAssociatesTrial,
        candidates: Candidates,
        selected: Set<String>,
    ): PairsTrial? {
        val candidateIds = candidates.options.map { it.id }.toSet()
        val answerable = plan.probes.filter { it.expectedTarget.id.value in candidateIds }
        if (answerable.isEmpty()) return null
        val correctIds = answerable.map { it.expectedTarget.id.value }.toSet()
        val distractorIds = candidateIds - correctIds
        return PairsTrial(
            targetCount = answerable.size,
            hits = answerable.count { it.expectedTarget.id.value in selected },
            distractorCount = distractorIds.size,
            falseAlarms = selected.count { it in distractorIds },
        )
    }

    /**
     * P2b · Persists the captured pairs trial and recomputes the durable [lastPairsScore] from the FULL
     * accumulated log. The pairs store shares the encrypted `mindboost_memory` namespace.
     */
    private suspend fun recordPairs(exerciseId: MemoryExerciseId, endedAt: Long, trial: PairsTrial) {
        val log = graph.pairsResultStore.record(userId, trial)
        lastPairsScore = MemoryPairsScore.scoreOf(
            log = log,
            exerciseId = exerciseId.name,
            sessionId = "$userId:$endedAt",
            userId = userId,
            timestampMs = endedAt,
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

    private suspend fun awaitProcessing(): List<Boolean> {
        val gate = CompletableDeferred<List<Boolean>>().also { processingGate = it }
        val result = gate.await()
        processingGate = null
        return result
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
        const val PROCESSING_INSTRUCTION = "Antes de recordar: ¿es correcta cada afirmación?"
        const val RECALL_INSTRUCTION = "Selecciona lo que recuerdes."
        const val HONEST_NOTE =
            "Foto de hoy en este ejercicio. La mejora real se confirma con recuerdo diferido y " +
                "pruebas no entrenadas."
    }
}
