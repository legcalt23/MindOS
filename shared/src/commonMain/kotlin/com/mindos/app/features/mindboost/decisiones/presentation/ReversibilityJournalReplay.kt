package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.features.mindboost.core.session.payload.JournaledPayload
import com.mindos.app.features.mindboost.core.session.payload.ReplayOutcome
import com.mindos.app.features.mindboost.decisiones.data.ReversibilityTrialDto
import com.mindos.app.features.mindboost.decisiones.data.toDomain
import com.mindos.app.features.mindboost.decisiones.data.toDto
import com.mindos.app.features.mindboost.decisiones.domain.repository.ReversibilityResultStore
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * S3 write-ahead journal codec + replay handler for the reversibility log. The journal payload is EXACTLY the
 * per-session unit the reversibility store merges — a `List<ReversibilityTrialDto>` (the two honest facts per card),
 * never a richer snapshot than the sink already persists (charter minimization).
 */
private val reversibilityJournalJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val reversibilityTrialsSerializer = ListSerializer(ReversibilityTrialDto.serializer())

/** Serializes a finished session's trials into the journal payload (the exact per-session merge unit). */
internal fun encodeReversibilityTrials(trials: List<ReversibilityTrial>): String =
    reversibilityJournalJson.encodeToString(reversibilityTrialsSerializer, trials.map { it.toDto() })

private fun decodeReversibilityTrials(raw: String): List<ReversibilityTrial> =
    reversibilityJournalJson.decodeFromString(reversibilityTrialsSerializer, raw).map { it.toDomain() }

/**
 * Replay handler for a journaled reversibility log: decodes the trials and merges them IDEMPOTENTLY by session id
 * ([ReversibilityResultStore.recordOnce]) so a re-applied entry never double-counts. A permanently undecodable
 * payload is dropped ([ReplayOutcome.CONFIRMED] — it cannot be recovered, so replay must not loop on it); a transient
 * store failure RETAINs the entry for the next startup. Cancellation is always rethrown (never swallowed).
 */
internal fun reversibilityReplayHandler(
    store: ReversibilityResultStore,
    userId: String,
): suspend (JournaledPayload) -> ReplayOutcome = handler@{ entry ->
    val trials = runCatching { decodeReversibilityTrials(entry.payload) }.getOrNull()
        ?: return@handler ReplayOutcome.CONFIRMED
    applyReversibility(store, userId, entry.entryId, trials)
}

// A transient store failure maps to RETAIN (kept for the next startup replay); the exception itself carries no
// recovery signal beyond "not now", so it is intentionally not rethrown/logged here. Cancellation is always rethrown.
@Suppress("SwallowedException")
private suspend fun applyReversibility(
    store: ReversibilityResultStore,
    userId: String,
    sessionId: String,
    trials: List<ReversibilityTrial>,
): ReplayOutcome = try {
    store.recordOnce(userId, sessionId, trials)
    ReplayOutcome.CONFIRMED
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
    ReplayOutcome.RETAIN
}
