package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.cognitiveRunJson
import com.mindos.app.features.mindboost.core.session.cognitive.decodeRun
import com.mindos.app.features.mindboost.core.session.cognitive.encodeRun
import com.mindos.app.features.mindboost.core.session.payload.JournaledPayload
import com.mindos.app.features.mindboost.core.session.payload.ReplayOutcome

/**
 * S3 write-ahead journal codec + replay handler for a sealed E8 [CognitiveSessionRun]. The journal payload is
 * EXACTLY what `CognitiveSessionRunStore` persists (the run serialized via the cognitive layer's own encoder) —
 * never a richer snapshot.
 */
internal fun encodeCognitiveRun(run: CognitiveSessionRun): String = encodeRun(cognitiveRunJson, run)

/**
 * Replay handler for a journaled E8 run: decode + re-save. IDEMPOTENT by construction — `CognitiveSessionRunStore`
 * dedups by runId, so re-saving an already-persisted run returns `SaveResult.IdempotentDuplicate` (no duplicate). A
 * permanently undecodable payload is dropped ([ReplayOutcome.CONFIRMED] — cannot recover, must not loop).
 *
 * The save result is classified via the SAME [SaveOutcomeClass]/[outcomeClass] the holder's own seal uses (P2-01
 * item 1 — a single source of truth): a SUCCESS or a TERMINAL_FAILURE (a conflicting id replay cannot resolve, or
 * a permanently-refused tombstoned subject) is CONFIRMED — dropped, dead-lettered, never retried forever, with a
 * PII-free breadcrumb on the terminal-failure path; only a TRANSIENT backend fault RETAINs the entry for the next
 * startup.
 */
internal fun e8RunReplayHandler(
    store: CognitiveSessionRunStore,
): suspend (JournaledPayload) -> ReplayOutcome = handler@{ entry ->
    val run = runCatching { decodeRun(cognitiveRunJson, entry.payload) }.getOrNull()
        ?: return@handler ReplayOutcome.CONFIRMED
    val outcome = store.save(run).outcomeClass()
    if (outcome == SaveOutcomeClass.TERMINAL_FAILURE) {
        ObservabilityHub.breadcrumb("e8_replay_terminal_failure")
    }
    if (outcome.shouldConfirmJournal()) ReplayOutcome.CONFIRMED else ReplayOutcome.RETAIN
}
