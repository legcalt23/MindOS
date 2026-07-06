package com.mindos.app.features.mindboost.decisiones.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Write-ahead lifecycle coordinator for ONE full Decisiones flow (check-in → Bayes → result → seal). It keeps
 * the orchestrator [DecisionsFlowStateHolder] pure by owning the recorder side-effects here: an opaque
 * [SessionRecord] id (never the internal "$userId:$now" flow id), the STARTED job, and the started/finalized
 * flags, all serialized under a [Mutex].
 *
 * TOTAL by contract via the injected [SessionRecorder]: a storage failure never breaks the flow. Only lifecycle
 * primitives ever cross to the recorder — never diary text, scenarios, options, choices, rounds, per-option
 * timing, risk/reward, impulsivity, or any decision content.
 */
class DecisionsSessionRecorder(
    private val recorder: SessionRecorder = NoOpSessionRecorder,
    private val sessionIdGenerator: SessionIdGenerator = RandomSessionIdGenerator(),
    private val clock: EpochClock = SystemEpochClock,
) {
    private val mutex = Mutex()
    private var sessionId: String = ""
    private var startJob: Job? = null
    private var startedAtMs: Long = 0L
    private var started: Boolean = false
    private var finalized: Boolean = false

    /**
     * Begins the write-ahead session: generates the opaque id and writes STARTED, launched on [scope] and
     * serialized under the mutex. Idempotent — a second call while already started is a no-op. The job is kept
     * so [finalize]/[abandon] can await STARTED before their own transition.
     */
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        val id = sessionIdGenerator.newSessionId()
        sessionId = id
        val startedAt = clock.nowMillis()
        startedAtMs = startedAt
        startJob = scope.launch {
            mutex.withLock { recorder.onStart(id, ScoreModuleId.DECISIONS, startedAt, plannedTrials = 1) }
        }
    }

    /**
     * Marks FINALIZED exactly once, after awaiting STARTED so the row exists. No-op if the flow never started or
     * is already finalized. `finalized` is set only AFTER onFinalize is attempted; a cancellation before that
     * leaves it false (recovery is the backstop). Counts only — never round/decision counts.
     */
    suspend fun finalize() {
        if (!started || finalized) return
        startJob?.join()
        val activeDurationMs = clock.nowMillis() - startedAtMs
        mutex.withLock { recorder.onFinalize(sessionId, completedTrials = 1, activeDurationMs = activeDurationMs) }
        finalized = true
    }

    /**
     * Marks ABANDONED after awaiting STARTED — only if a real session started and did NOT finalize. A pre-start
     * abandon creates no artificial record; a finalized session is never abandoned.
     */
    suspend fun abandon() {
        if (!started || finalized) return
        startJob?.join()
        mutex.withLock { recorder.onAbandon(sessionId) }
    }
}
