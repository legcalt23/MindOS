package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.core.scoring.ScoreResult
import com.mindos.app.features.mindboost.memory.di.MemoryGraph
import com.mindos.app.features.mindboost.memory.domain.content.model.WordId
import com.mindos.app.features.mindboost.memory.domain.content.model.WordItem
import com.mindos.app.features.mindboost.memory.domain.content.sampling.SamplingFilters
import com.mindos.app.features.mindboost.memory.domain.deferred.DeferredRecallItem
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallOutcome
import com.mindos.app.features.mindboost.memory.domain.scoring.DeferredRecallResult
import com.mindos.app.features.mindboost.memory.domain.scoring.MemoryDeferredScore
import com.mindos.app.features.mindboost.memory.presentation.state.DeferredRecallUiState
import com.mindos.app.features.mindboost.memory.presentation.state.RecallOptionUi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * EX3 deferred recall (P2) · Drives ONE between-session delayed-recall test: load the user's registered
 * items, pick the first one inside its 24h retention window (DUE), present its studied words mixed with
 * lures (NO re-study), collect a recognition selection, score it via [DeferredRecallScorer]'s outcome, and
 * CONSUME the item (so it is not re-tested). The scored [DeferredRecallResult] is exposed for a later phase
 * to accumulate toward the episodic profile signal.
 *
 * Honest derivation (the EX4 lesson): only studied words that can actually be PRESENTED as candidates
 * count — a word the content pool can no longer resolve is excluded from BOTH the candidate set and
 * [DeferredRecallOutcome.studiedCount], so a pool change can never unfairly deflate the score. False-alarms
 * (selecting a lure) make a "select everything" delayed test non-discriminating, not a free 100%.
 */
class DeferredRecallSessionStateHolder(
    private val graph: MemoryGraph,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
) {
    var uiState: DeferredRecallUiState by mutableStateOf(DeferredRecallUiState.Loading)
        private set

    /** The scored result of the last completed delayed-recall test; null before finishing / when nothing due. */
    var lastResult: DeferredRecallResult? = null
        private set

    /**
     * The durable episodic (DELAYED_RECALL_ACCURACY) verdict after the last completed test — derived from
     * the FULL accumulated log, not just this test. Null when nothing was tested. Reaches VALID only once
     * ≥20 valid outcomes accumulate; the brain-profile reads it in a later phase.
     */
    var lastDeferredScore: ScoreResult? = null
        private set

    private var sessionJob: Job? = null
    private var recallGate: CompletableDeferred<Set<String>>? = null

    fun start() {
        if (sessionJob?.isActive == true) return
        sessionJob = scope.launch { runSession() }
    }

    /** UI calls this with the ids the user recognised. */
    fun submitRecall(selectedIds: Set<String>) {
        recallGate?.takeUnless { it.isCompleted }?.complete(selectedIds)
    }

    fun abort(reason: String = "Sesión interrumpida") {
        sessionJob?.cancel()
        sessionJob = null
        recallGate = null
        uiState = DeferredRecallUiState.Aborted(reason)
    }

    private suspend fun runSession() {
        uiState = DeferredRecallUiState.Loading
        val items = graph.deferredRecallRepository.load(userId)
        val now = graph.epochClock.nowMillis()
        val due = graph.deferredScheduler.due(items, now).firstOrNull()
        val test = due?.let { buildTest(it) }
        if (due == null || test == null) {
            uiState = DeferredRecallUiState.NothingDue
            return
        }
        uiState = DeferredRecallUiState.Recall(RECALL_INSTRUCTION, test.candidates)
        val outcome = score(test, awaitRecall())
        lastResult = DeferredRecallResult(due.exerciseId, outcome)
        graph.deferredRecallRepository.save(userId, items.filterNot { it.id == due.id }) // consume the tested item
        val log = graph.deferredResultStore.record(userId, outcome)
        lastDeferredScore = MemoryDeferredScore.scoreOf(
            log = log,
            exerciseId = due.exerciseId.name,
            sessionId = "$userId:delayed:$now",
            userId = userId,
            timestampMs = now,
        )
        uiState = DeferredRecallUiState.Finished(outcome.hits, outcome.studiedCount, HONEST_NOTE)
    }

    /**
     * Builds the recognition test, or null if it cannot be presented as a real discrimination test —
     * either no studied word resolves (the pool changed) OR not enough lures can be sampled. Both sides
     * fail SYMMETRICALLY and honestly: a lure-less test would make EVERY selection "discriminating"
     * (false-alarm rate structurally 0 → a free 100%), so such an item is skipped (→ NothingDue), never
     * scored.
     */
    private fun buildTest(item: DeferredRecallItem): RecallTest? {
        val studied = resolveStudied(item)
        val studiedIds = studied.map { it.id }.toSet()
        val distractors = sampleDistractors(item, studiedIds)
        if (studied.isEmpty() || distractors.size < DISTRACTOR_COUNT) return null
        val options = (studied + distractors).shuffled(Random(seed)).map { RecallOptionUi(it.id.value, it.text) }
        return RecallTest(options, studiedIds.map { it.value }.toSet(), distractors.map { it.id.value }.toSet())
    }

    /** Samples lures excluding the studied words; empty if the sampler cannot produce them (→ item skipped). */
    private fun sampleDistractors(item: DeferredRecallItem, studiedIds: Set<WordId>): List<WordItem> =
        runCatching {
            graph.content.samplerFor(item.exerciseId, item.language).sampleWords(
                count = DISTRACTOR_COUNT,
                seed = seed,
                filters = SamplingFilters.None,
                excludeRecentIds = studiedIds,
            )
        }.getOrDefault(emptyList())

    private fun resolveStudied(item: DeferredRecallItem): List<WordItem> {
        val pool = runCatching { graph.content.trainingWordPool(item.language) }.getOrNull() ?: return emptyList()
        val byId = pool.items.associateBy { it.id }
        return item.studiedWordIds.mapNotNull { byId[it] }
    }

    private fun score(test: RecallTest, selected: Set<String>): DeferredRecallOutcome = DeferredRecallOutcome(
        studiedCount = test.studiedIds.size,
        hits = selected.count { it in test.studiedIds },
        distractorCount = test.distractorIds.size,
        falseAlarms = selected.count { it in test.distractorIds },
    )

    private suspend fun awaitRecall(): Set<String> {
        val gate = CompletableDeferred<Set<String>>().also { recallGate = it }
        val result = gate.await()
        recallGate = null
        return result
    }

    /** The presented options plus which ids are studied vs lures (for honest scoring). */
    private data class RecallTest(
        val candidates: List<RecallOptionUi>,
        val studiedIds: Set<String>,
        val distractorIds: Set<String>,
    )

    private companion object {
        const val DISTRACTOR_COUNT = 6
        const val RECALL_INSTRUCTION = "De ayer: selecciona las que recuerdes, sin volver a estudiarlas."
        const val HONEST_NOTE =
            "Prueba de retención a 24h: mide cuánto conservaste de la sesión anterior, no lo de hoy."
    }
}
