package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.session.payload.PayloadKind
import com.mindos.app.features.mindboost.core.session.payload.PayloadReplayScanner
import com.mindos.app.features.mindboost.core.session.payload.SecurePayloadJournalStore
import com.mindos.app.features.mindboost.decisiones.data.LocalReversibilityResultStore
import com.mindos.app.features.mindboost.decisiones.domain.scoring.ReversibilityTrial
import com.mindos.app.features.mindboost.focus.data.local.InMemorySecureKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * S3 end-to-end: the reversibility write-ahead journal + idempotent replay. Proves a session KILLED between the
 * journal write and the merge is recovered on the next process, and that replaying the same session never
 * double-counts its trials (recordOnce dedups by session id).
 */
class ReversibilityWriteAheadTest {

    private val clock = EpochClock { 7_000L }

    private fun trials(vararg pairs: Pair<Boolean, Boolean>) =
        pairs.map { ReversibilityTrial(actuallyReversible = it.first, selectedReversible = it.second) }

    @Test
    fun recordOnce_isIdempotentBySessionId() = runTest {
        val store = LocalReversibilityResultStore(InMemorySecureKeyValueStore())
        val t = trials(true to true, false to false)

        store.recordOnce("u", "s1", t)
        store.recordOnce("u", "s1", t) // a replay of the same session

        val log = store.load("u")
        assertEquals(2, log.trials.size, "the same session's trials are merged exactly once")
        assertEquals(listOf("s1"), log.mergedSessionIds)
    }

    @Test
    fun journaledTrials_areRecovered_afterProcessDeath_withoutDuplication() = runTest {
        // Two durable "device files" (distinct namespaces): the journal + the reversibility (decisiones) store.
        val journalBacking = InMemorySecureKeyValueStore()
        val decisionsBacking = InMemorySecureKeyValueStore()
        val userId = "u"
        val sessionId = "u:1000"
        val t = trials(true to true, false to true, true to false)

        // ---- Process 1: the session finished; trials were JOURNALED but the app was KILLED before the merge. ----
        SecurePayloadJournalStore(journalBacking, subject = userId, clock = clock)
            .enqueue(sessionId, PayloadKind.REVERSIBILITY_LOG, encodeReversibilityTrials(t))
        assertTrue(
            LocalReversibilityResultStore(decisionsBacking).load(userId).trials.isEmpty(),
            "nothing was merged before the kill",
        )

        // ---- Process 2: fresh objects over the SAME durable backings; replay recovers the merge. ----
        val store2 = LocalReversibilityResultStore(decisionsBacking)
        val journal2 = SecurePayloadJournalStore(journalBacking, subject = userId, clock = clock)
        val scanner = PayloadReplayScanner(
            journal2,
            mapOf(PayloadKind.REVERSIBILITY_LOG to reversibilityReplayHandler(store2, userId)),
        )

        assertEquals(1, scanner.replay())
        assertEquals(t, store2.load(userId).trials, "the killed session's trials are recovered on replay")
        assertTrue(journal2.pending().isEmpty(), "the journal is drained after a successful merge")

        // ---- A redundant replay (a crash re-applied the SAME entry) must not double-count. ----
        journal2.enqueue(sessionId, PayloadKind.REVERSIBILITY_LOG, encodeReversibilityTrials(t))
        scanner.replay()
        assertEquals(t, store2.load(userId).trials, "an idempotent replay never duplicates the session")
    }
}
