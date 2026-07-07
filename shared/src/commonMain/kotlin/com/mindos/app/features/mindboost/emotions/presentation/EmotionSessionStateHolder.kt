@file:Suppress("MagicNumber", "MaxLineLength") // Session tuning constants + dense wiring expressions.

package com.mindos.app.features.mindboost.emotions.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionPendingTerminalDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionAggregateFactory
import com.mindos.app.features.mindboost.emotions.data.mapper.EmotionMappers
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.AdaptiveAction
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveInput
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.adaptive.InductionIntensity
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionBlockSpec
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.valueIfConfident
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTier
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionSafetyLimits
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionSafetyState
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadProxy
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadSignals
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernorState
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEvent
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEventType
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionBlockResult
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionTrialClassifier
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionRunningModel
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/** Maps a domain stimulus to the render-ready field. Pure + internal so it is unit-testable. */
internal fun emotionFieldFor(stimulus: EmotionStimulus): EmotionFieldState = when (val payload = stimulus.payload) {
    is EmotionStimulusPayload.AffectiveGoNoGo -> EmotionFieldState.GoNoGo(payload.frameOpen, payload.affectiveWord)
    is EmotionStimulusPayload.TwoAltPerceptual -> {
        val gap = (payload.discriminability * MAGNITUDE_SPAN).toFloat()
        val correctLeft = payload.correct == EmotionResponseKey.LEFT
        EmotionFieldState.TwoAlt(
            leftMagnitude = BASE_MAGNITUDE + if (correctLeft) gap else -gap,
            rightMagnitude = BASE_MAGNITUDE + if (correctLeft) -gap else gap,
        )
    }
    is EmotionStimulusPayload.Classification -> EmotionFieldState.FourCategory(payload.phrase)
}

private const val BASE_MAGNITUDE = 0.5f
private const val MAGNITUDE_SPAN = 0.4

/**
 * Drives one emotion session in real time: self-report → two mini-blocks with a proxy-armed mid-session
 * safety check between them → self-report → sober feedback, persisting an encrypted summary. The holder
 * ORCHESTRATES; classification/metrics/adaptation/feedback are the pure domain engines. RT is monotonic.
 * Safety strictly precedes performance, and the always-visible safety bar (pause / reduce / exit) routes
 * here. Local UI counters are display-only — the assembler is the metrics authority.
 */
@Suppress("TooManyFunctions", "LongMethod", "LongParameterList")
class EmotionSessionStateHolder(
    private val graph: EmotionGraph,
    private val exerciseId: EmotionExerciseId,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
    private val recorder: SessionRecorder = NoOpSessionRecorder,
    private val sessionIdGenerator: SessionIdGenerator = RandomSessionIdGenerator(),
) {
    private val definition = graph.definitionFor(exerciseId)
    private val generator = graph.generatorFor(exerciseId)
    private val governingAxis = governingAxisFor(exerciseId)

    var uiState: EmotionSessionUiState by mutableStateOf(
        EmotionSessionUiState.SelfReportBefore(definition.displayName, definition.plainDescription),
    )
        private set

    private var sessionJob: Job? = null
    private var pendingResponse: CompletableDeferred<ResponseEvent>? = null
    private var pendingSelfReport: CompletableDeferred<SelfReport>? = null
    private var pendingIntensity: CompletableDeferred<Int>? = null
    private var resumeFromPause: CompletableDeferred<Unit>? = null

    private var difficulty = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet())
    private var intensity = InductionIntensity.NORMAL
    private var reducePressure = false
    private var consecutiveErrors = 0
    private var blocksSinceChange = 0
    private var pauseState = PauseGovernorState()
    private var pauseUsesThisBlock = 0
    private var previousProxies: Set<OverloadProxy> = emptySet()
    private var baselineRtMedian: Double? = null
    private var baselineRtCv: Double? = null

    private val allTrials = mutableListOf<RecordedEmotionTrial>()
    private val safetyEvents = mutableListOf<SafetyEvent>()
    private var selfReportBefore: SelfReport? = null
    private var selfReportAfter: SelfReport? = null
    private var startedAtMs = 0L

    // Write-ahead recorder coordinator (MC8a): the SessionRecord's own first-wins backstop (join onStart → single
    // FINALIZED/ABANDONED transition).
    private val recorderMutex = Mutex()
    private var recorderSessionId = ""
    private var recorderStartJob: Job? = null
    private var recorderTerminal = false

    // Holder lifecycle (MC8e). A holder represents AT MOST ONE session: NOT_STARTED → RUNNING → TERMINAL.
    // TERMINAL is the first-wins decision for the WHOLE terminal block (own summary + aggregate + SessionRecord +
    // UI); once decided, no later terminal, restart, or late input can change it. All holder coroutines run on the
    // single-threaded composition/UI (or test) dispatcher, so these plain reads/writes are consistent, and
    // `terminalMutex` + NonCancellable make a decided terminal atomic against cooperative cancellation.
    private val terminalMutex = Mutex()
    private var phase = Phase.NOT_STARTED

    /**
     * Starts the one session this holder will ever run. Single-use: only a fresh (NOT_STARTED) holder starts.
     * `beginRecording()` runs synchronously HERE (not inside the launched runSession) so that from the instant the
     * session is RUNNING there is already a recorderSessionId + an onStart in flight. An immediate onExit right
     * after start() can therefore never persist an abandoned summary without a matching SessionRecord (MC8f P0).
     */
    fun start() {
        if (phase != Phase.NOT_STARTED) return
        phase = Phase.RUNNING
        startedAtMs = graph.epochClock.nowMillis()
        beginRecording()
        sessionJob = scope.launch { runSession() }
    }

    fun onTap() = onResponse(EmotionResponseKey.PRIMARY, null)
    fun onLeft() = onResponse(EmotionResponseKey.LEFT, null)
    fun onRight() = onResponse(EmotionResponseKey.RIGHT, null)
    fun onCategory(category: EmotionCategory) = onResponse(keyFor(category), category)

    private fun onResponse(key: EmotionResponseKey, category: EmotionCategory?) {
        if (phase != Phase.RUNNING) return
        val pending = pendingResponse ?: return
        if (!pending.isCompleted) pending.complete(ResponseEvent(graph.monotonicClock.nowNanos(), key, category))
    }

    fun submitSelfReport(report: SelfReport) {
        if (phase != Phase.RUNNING) return
        pendingSelfReport?.takeIf { !it.isCompleted }?.complete(report)
    }

    fun submitCheckInIntensity(intensityValue: Int) {
        if (phase != Phase.RUNNING) return
        pendingIntensity?.takeIf { !it.isCompleted }?.complete(intensityValue)
    }

    /** Safety bar: a strategic pause. Logged, never penalized; over-use trips reduce (anti-gaming). No-op once a
     * terminal has been decided (`phase == TERMINAL`), even if the UI still shows Running mid-terminal (MC8g P1). */
    fun onPause() {
        if (phase != Phase.RUNNING) return
        if (uiState !is EmotionSessionUiState.Running) return
        val decision = graph.pauseGovernor.onPause(pauseState, graph.epochClock.nowMillis())
        pauseState = decision.state
        pauseUsesThisBlock++
        record(SafetyEventType.PAUSE)
        if (decision.reduceIntensity) reduceIntensityInternal()
        resumeFromPause = CompletableDeferred()
        uiState = EmotionSessionUiState.MicroPause
    }

    fun onResumeFromMicroPause() {
        if (phase != Phase.RUNNING) return
        resumeFromPause?.takeIf { !it.isCompleted }?.complete(Unit)
    }

    /** Safety bar: lower induction intensity and widen the response window (reduce temporal pressure). Allowed
     * ONLY while the safety bar is genuinely usable (a live in-session state, before any terminal) — a late tap,
     * or a tap during self-report, must never mutate intensity/pressure/safety events (MC8f/MC8g P1). */
    fun onReduceIntensity() {
        if (!canUseSafetyBar()) return
        reduceIntensityInternal()
        record(SafetyEventType.REDUCE_INTENSITY)
    }

    /** The safety bar mutates training/safety state; it is usable only in a live in-session UI state before any
     * terminal, never during self-report screens or after `phase == TERMINAL`. */
    private fun canUseSafetyBar(): Boolean =
        phase == Phase.RUNNING &&
            (
                uiState is EmotionSessionUiState.Running ||
                    uiState is EmotionSessionUiState.MidCheckIn ||
                    uiState is EmotionSessionUiState.MicroPause
            )

    /** Safety bar: leave now. Persists the abandoned session (only if one really started), then exits. */
    fun onExit() {
        when (phase) {
            // Never started (menu/pre-session): nothing to persist — a created-but-unused holder must not touch
            // history, aggregate, calibration or the SessionRecord. Exit is UI-local only.
            Phase.NOT_STARTED -> uiState = EmotionSessionUiState.Exited
            Phase.RUNNING -> {
                sessionJob?.cancel()
                clearPendingInteractions()
                // Stop stimuli now; expose Exited ONLY if THIS ABANDONED won the terminal (finalize == true).
                // If COMPLETED/SAFETY_STOP already decided, finalize returns false — never overwrite Result.
                scope.launch { if (finalize(SessionEnd.ABANDONED)) uiState = EmotionSessionUiState.Exited }
            }
            // A terminal already won — never re-persist, re-abandon, or overwrite the visible terminal.
            Phase.TERMINAL -> Unit
        }
    }

    private fun reduceIntensityInternal() {
        intensity = intensity.reducedBy()
        reducePressure = true
    }

    /** Cancels AND drops every awaitable interaction so no late input (self-report / check-in / resume / response)
     * can complete a stale deferred or leave an awaitable alive after a terminal has been decided (MC8f/MC8g P1). */
    private fun clearPendingInteractions() {
        pendingResponse?.cancel()
        pendingSelfReport?.cancel()
        pendingIntensity?.cancel()
        resumeFromPause?.cancel()
        pendingResponse = null
        pendingSelfReport = null
        pendingIntensity = null
        resumeFromPause = null
    }

    private suspend fun runSession() {
        selfReportBefore = awaitSelfReport(EmotionSessionUiState.SelfReportBefore(definition.displayName, definition.plainDescription))

        var ended = SessionEnd.COMPLETED
        for (blockIndex in 0 until BLOCKS) {
            pauseUsesThisBlock = 0
            val trials = runBlock(blockIndex)
            allTrials += trials
            val endAfterBlock = applyAdaptationAndSafety(trials, isLastBlock = blockIndex == BLOCKS - 1)
            if (endAfterBlock == SessionEnd.SAFETY_STOP) {
                ended = SessionEnd.SAFETY_STOP
                break
            }
        }

        if (ended == SessionEnd.COMPLETED) {
            selfReportAfter = awaitSelfReport(EmotionSessionUiState.SelfReportAfter(definition.displayName))
        }
        finalize(ended)
    }

    private suspend fun runBlock(blockIndex: Int): List<RecordedEmotionTrial> {
        val spec = EmotionBlockSpec(targetTrialCount = TRIALS_PER_BLOCK, responseWindowMs = responseWindowMs())
        val stimuli = generator.generate(difficulty, spec, seed + blockIndex)
        val recorded = ArrayList<RecordedEmotionTrial>(stimuli.size)
        stimuli.forEachIndexed { i, stimulus ->
            awaitIfPaused()
            delay(stimulus.interStimulusDelayMs)
            val onset = graph.monotonicClock.nowNanos()
            uiState = EmotionSessionUiState.Running(
                EmotionRunningModel(
                    exerciseName = definition.displayName,
                    field = emotionFieldFor(stimulus),
                    trialIndex = blockIndex * TRIALS_PER_BLOCK + i,
                    trialTotal = BLOCKS * TRIALS_PER_BLOCK,
                    intensity = intensity.value,
                ),
            )
            pendingResponse = CompletableDeferred()
            val event = withTimeoutOrNull(stimulus.responseWindowMs) { pendingResponse?.await() }
            pendingResponse = null
            val latency = event?.let { (it.nanos - onset) / NANOS_PER_MS }
            val trial = RecordedEmotionTrial(stimulus, latency, event?.key ?: EmotionResponseKey.NONE, event?.category)
            recorded += trial
            updateErrorStreak(trial)
        }
        return recorded
    }

    /** Returns SAFETY_STOP if the session must end now; otherwise applies the adaptive decision. */
    private suspend fun applyAdaptationAndSafety(trials: List<RecordedEmotionTrial>, isLastBlock: Boolean): SessionEnd {
        val result = graph.assembler.assembleBlock(trials, definition)
        val proxies = graph.overloadDetector.detect(
            OverloadSignals(
                metrics = result.metrics,
                consecutiveErrors = consecutiveErrors,
                exitAttempts = 0,
                pauseUses = pauseUsesThisBlock,
                rtMedianBaseline = baselineRtMedian,
                rtCvBaseline = baselineRtCv,
            ),
        )
        captureBaseline(result.metrics)

        var checkInResolved = false
        if (proxies.isNotEmpty() && !isLastBlock) {
            val reported = awaitMidCheckIn()
            checkInResolved = true
            record(SafetyEventType.MID_SESSION_CHECK_IN, reportedIntensity = reported, proxy = proxies.firstOrNull())
            applyReportedIntensity(reported)
            if (intensity.requiresStop) {
                record(SafetyEventType.SAFETY_STOP)
                return SessionEnd.SAFETY_STOP
            }
        }

        val decision = graph.adaptiveEngine.decide(
            EmotionAdaptiveInput(
                zone = definition.adaptiveZone,
                governingAxis = governingAxis,
                metrics = result.metrics,
                currentDifficulty = difficulty,
                intensity = intensity,
                safety = EmotionSafetyState(proxies, previousProxies, checkInResolved),
                consecutiveErrors = consecutiveErrors,
                blocksSinceChange = blocksSinceChange,
                selfReport = selfReportBefore,
            ),
        )
        previousProxies = proxies
        return applyDecision(decision.action, decision.nextDifficulty, decision.nextIntensity)
    }

    private fun applyDecision(action: AdaptiveAction, nextDifficulty: EmotionDifficultyVector, nextIntensity: InductionIntensity): SessionEnd {
        when (action) {
            AdaptiveAction.SAFETY_STOP -> {
                record(SafetyEventType.SAFETY_STOP)
                return SessionEnd.SAFETY_STOP
            }
            AdaptiveAction.REDUCE_INTENSITY -> {
                intensity = nextIntensity
                difficulty = nextDifficulty
                reducePressure = true
                record(SafetyEventType.REDUCE_INTENSITY)
                blocksSinceChange = 0
            }
            AdaptiveAction.STEP_UP, AdaptiveAction.STEP_DOWN, AdaptiveAction.COOLDOWN -> {
                difficulty = nextDifficulty
                blocksSinceChange = 0
            }
            AdaptiveAction.CHECK_IN, AdaptiveAction.HOLD -> blocksSinceChange++
        }
        return SessionEnd.COMPLETED
    }

    private suspend fun awaitSelfReport(state: EmotionSessionUiState): SelfReport {
        uiState = state
        val deferred = CompletableDeferred<SelfReport>()
        pendingSelfReport = deferred
        val report = deferred.await()
        pendingSelfReport = null
        return report
    }

    private suspend fun awaitMidCheckIn(): Int {
        uiState = EmotionSessionUiState.MidCheckIn
        val deferred = CompletableDeferred<Int>()
        pendingIntensity = deferred
        val value = deferred.await()
        pendingIntensity = null
        return value
    }

    private suspend fun awaitIfPaused() {
        val deferred = resumeFromPause ?: return
        deferred.await()
        resumeFromPause = null
    }

    private fun applyReportedIntensity(reported: Int) {
        when (EmotionSafetyLimits.directiveForCheckIn(reported)) {
            com.mindos.app.features.mindboost.emotions.domain.safety.SafetyDirective.STOP ->
                intensity = InductionIntensity(InductionIntensity.STOP_AT)
            com.mindos.app.features.mindboost.emotions.domain.safety.SafetyDirective.REDUCE -> reduceIntensityInternal()
            else -> Unit
        }
    }

    private fun updateErrorStreak(trial: RecordedEmotionTrial) {
        val outcome = EmotionTrialClassifier.classify(trial)
        val isError = outcome == EmotionTrialOutcome.INCORRECT ||
            outcome == EmotionTrialOutcome.FALSE_ALARM ||
            outcome == EmotionTrialOutcome.MISS
        consecutiveErrors = if (isError) consecutiveErrors + 1 else 0
    }

    private fun captureBaseline(metrics: com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricSet) {
        if (baselineRtMedian == null) baselineRtMedian = metrics.valueIfConfident(EmotionMetricKey.RT_MEDIAN)
        if (baselineRtCv == null) baselineRtCv = metrics.valueIfConfident(EmotionMetricKey.RT_CV)
    }

    /**
     * First-wins terminal for the WHOLE lifecycle (MC8d/e/f/g). A DECIDED terminal (this function entered) applies
     * its CRITICAL durable path atomically, then its BEST-EFFORT lifecycle/aggregate. Returns true iff THIS call
     * won the terminal.
     *
     * I/O classification (MC8g/MC8h):
     *  • CRITICAL & durable (under [terminalMutex] + NonCancellable, cancellation deferred): write a durable PENDING
     *    terminal FIRST, then save the canonical summary. On success the pending is deleted and a success Result is
     *    shown. On summary FAILURE the outcome depends on whether the PENDING persisted: pending-saved → the progress
     *    is recoverable (recovery retries it) and we show the neutral leave state; pending-ALSO-failed → the terminal
     *    is NOT durable and we show the explicit [EmotionSessionUiState.SaveFailed] — never a false "completed",
     *    never a neutral leave sold as recoverable.
     *  • BEST-EFFORT (outside NonCancellable, bounded by [BEST_EFFORT_TIMEOUT_MS]): the SessionRecord transition and
     *    the DERIVED aggregate. A hang/failure there NEVER freezes or falsifies the terminal; recovery reconciles
     *    the SessionRecord against the saved summary (sessionId link) and the aggregate is recomputable.
     */
    private suspend fun finalize(ended: SessionEnd): Boolean {
        val outcome = criticalTerminal(ended) ?: return false
        if (outcome.summarySaved) {
            recordTerminalBestEffort(ended, outcome.activeDurationMs)
            // Aggregate save is best-effort (outside the critical path). It reuses the aggregate ALREADY computed
            // for the Result tier (MC8j), so the calibration advanced is the SAME value the UI showed — one source.
            // MC8q: persist for EVERY non-ABANDONED terminal (COMPLETED and SAFETY_STOP) when an aggregate exists —
            // a SAFETY_STOP can change `safetyLoad` (EmotionAggregateFactory.safetyLoadOf includes recent safety
            // stops), so relying on the reconciler to self-heal it would leave the persisted safety pressure stale
            // until the next Emociones entry. ABANDONED never persists an aggregate. If no valid completed session
            // exists yet, `aggregate` is null (fromSummaries) and nothing is fabricated.
            val aggregate = outcome.aggregate
            if (ended != SessionEnd.ABANDONED && aggregate != null) {
                withTimeoutOrNull(BEST_EFFORT_TIMEOUT_MS) {
                    recoverableTerminal { graph.repository.saveAggregate(userId, aggregate) }
                }
            }
        }
        return true
    }

    /**
     * Runs [block] and isolates ONLY an ordinary recoverable failure (returned as [Result.failure]); a
     * [CancellationException] and a fatal [Error] (OOM/StackOverflow/AssertionError) are ALWAYS rethrown (MC8q),
     * never swallowed — consistent with the reconciler and the repository. The CRITICAL terminal ([criticalTerminal])
     * runs under [NonCancellable], so a cooperative cancellation is deferred there (the block completes); this helper
     * still rethrows any [CancellationException] that surfaces rather than mistaking it for a recoverable fault.
     */
    private suspend fun <T> recoverableTerminal(block: suspend () -> T): Result<T> {
        val result = runCatching { block() }
        result.exceptionOrNull()?.let { if (it is CancellationException || it is Error) throw it }
        return result
    }

    /** The atomic, cancellation-deferred critical path: decide the terminal, persist a durable pending, save the
     * canonical summary, and expose the safe UI. Returns null iff a terminal already won (this call is the loser). */
    private suspend fun criticalTerminal(ended: SessionEnd): TerminalOutcome? = withContext(NonCancellable) {
        terminalMutex.withLock {
            if (phase == Phase.TERMINAL) return@withLock null
            phase = Phase.TERMINAL
            clearPendingInteractions() // no late self-report/intensity/resume/response can revive old logic
            val terminalAtMs = graph.epochClock.nowMillis()
            val activeDurationMs = (terminalAtMs - startedAtMs).coerceAtLeast(0)
            val finalResult = graph.assembler.assembleBlock(allTrials, definition)
            val summary = buildSummary(ended, finalResult, activeDurationMs)
            // Write the durable PENDING retry marker FIRST, then the canonical summary — and capture BOTH outcomes.
            // We may only claim the terminal is "recoverable" when the pending itself actually persisted (MC8h P0/P1).
            // MC8q: every terminal I/O routes through recoverableTerminal — an ordinary storage failure is isolated
            // (captured in the Result), but a CancellationException and a fatal Error are ALWAYS rethrown.
            val pendingSaved = recoverableTerminal {
                graph.repository.savePendingTerminal(userId, EmotionPendingTerminalDto(summary = summary, attemptCount = 1, createdAtMillis = terminalAtMs))
            }.isSuccess
            val summarySaved = recoverableTerminal { graph.repository.saveSessionSummary(userId, summary) }.isSuccess
            var aggregate: EmotionAggregateDto? = null
            if (summarySaved) {
                recoverableTerminal { graph.repository.deletePendingTerminal(userId, recorderSessionId) }
                // MC8j: the Result tier is derived from the SAME endReason-authoritative aggregate the profile uses
                // (via EmotionAggregateFactory) — never the raw `completed` boolean — so UI tier and calibration can
                // never diverge. This one aggregate is also what finalize persists (no second, drifting computation).
                aggregate = recoverableTerminal {
                    EmotionAggregateFactory.fromSummaries(graph.repository.recentSummaries(userId), terminalAtMs)
                }.getOrNull()
                val tier = aggregate?.let { EmotionAggregateFactory.tierFor(it.sessionsCompleted) } ?: ProfileTier.EXPLORATORY
                if (ended != SessionEnd.ABANDONED) {
                    uiState = resultFor(ended, finalResult, tier, aggregate?.sessionsCompleted ?: 0)
                }
            } else if (ended != SessionEnd.ABANDONED) {
                // Summary not durable. If the PENDING persisted, the canonical progress is retained for a retry →
                // neutral leave (recovery finishes it). If NEITHER persisted, the terminal is NOT durable: show an
                // explicit, honest SaveFailed — never a neutral leave sold as recoverable (MC8h P0/P1 invariant 1).
                uiState = if (pendingSaved) EmotionSessionUiState.Exited else EmotionSessionUiState.SaveFailed
            }
            TerminalOutcome(summarySaved, activeDurationMs, terminalAtMs, aggregate)
        }
    }

    private fun buildSummary(ended: SessionEnd, finalResult: EmotionBlockResult, activeDurationMs: Long): EmotionSessionSummaryDto =
        EmotionSessionSummaryDto(
            exerciseId = exerciseId.name,
            startedAtMillis = startedAtMs,
            durationMs = activeDurationMs,
            difficultySummary = DifficultySummaryDto(governingAxis.name, difficulty.level(governingAxis), intensity.value),
            metricsSummary = EmotionMappers.toMetricSummaryDtos(finalResult.metrics),
            selfReportBefore = EmotionMappers.toSelfReportDto(selfReportBefore),
            selfReportAfter = EmotionMappers.toSelfReportDto(selfReportAfter),
            safetyEvents = EmotionMappers.toSafetyEventDtos(safetyEvents),
            completed = ended == SessionEnd.COMPLETED,
            abandoned = ended == SessionEnd.ABANDONED,
            sessionId = recorderSessionId.ifEmpty { null },
            endReason = ended.name, // COMPLETED | SAFETY_STOP | ABANDONED — explicit, no inference needed
        )

    private fun resultFor(
        ended: SessionEnd,
        finalResult: EmotionBlockResult,
        tier: ProfileTier,
        validSessions: Int,
    ): EmotionSessionUiState.Result {
        val stoppedForSafety = ended == SessionEnd.SAFETY_STOP
        return EmotionSessionUiState.Result(
            exerciseName = definition.displayName,
            feedback = graph.feedbackBuilder.build(
                definition,
                finalResult.metrics[definition.governingMetric],
                tier,
                stoppedForSafety = stoppedForSafety,
            ),
            stoppedForSafety = stoppedForSafety,
            validSessions = validSessions,
        )
    }

    /** Write-ahead STARTED at the real session start (opaque id, EMOTIONS, one session unit). Fire-and-forget
     * under the mutex; [recordTerminal] joins this job so the row exists before any terminal transition. */
    private fun beginRecording() {
        val sessionId = sessionIdGenerator.newSessionId()
        recorderSessionId = sessionId
        recorderStartJob = scope.launch {
            recorderMutex.withLock { recorder.onStart(sessionId, ScoreModuleId.EMOTIONS, startedAtMs, plannedTrials = 1) }
        }
    }

    /**
     * BEST-EFFORT SessionRecord transition (MC8g), bounded by [BEST_EFFORT_TIMEOUT_MS] so a hanging/slow recorder
     * (or a stuck onStart join) NEVER freezes the canonical terminal. COMPLETED/SAFETY_STOP → FINALIZED;
     * ABANDONED → ABANDONED. On timeout, or a recorder that never wrote STARTED, the SessionRecord stays
     * pre-terminal and recovery reconciles it against the saved summary (sessionId link). Serialized behind
     * [recorderMutex] with a first-wins [recorderTerminal] flag.
     */
    private suspend fun recordTerminalBestEffort(ended: SessionEnd, activeDurationMs: Long) {
        if (recorderSessionId.isEmpty()) return
        withTimeoutOrNull(BEST_EFFORT_TIMEOUT_MS) {
            recorderStartJob?.join()
            recorderMutex.withLock {
                if (recorderTerminal) return@withLock
                recorderTerminal = true
                recordToRecorder(ended, activeDurationMs)
            }
        }
    }

    /**
     * Invokes the recorder with the duration captured at the terminal decision (never contaminated by persistence
     * latency). MC8q: routes through [recoverableTerminal], so a [CancellationException] AND a fatal [Error] both
     * propagate; only an ordinary contract-violating recorder Exception is isolated — it can neither crash the
     * terminal nor hide the already-saved summary (the SessionRecord simply stays pre-terminal for recovery).
     */
    private suspend fun recordToRecorder(ended: SessionEnd, activeDurationMs: Long) {
        recoverableTerminal {
            when (ended) {
                // A COMPLETED session did one unit of completed work.
                SessionEnd.COMPLETED ->
                    recorder.onFinalize(recorderSessionId, completedTrials = 1, activeDurationMs = activeDurationMs)
                // MC8p: a SAFETY_STOP is a FINALIZED lifecycle terminal but did NO completed work — write
                // completedTrials = 0 at write-time (consistent with EmotionTerminalSemantics.completedTrialsFor(
                // SAFETY_STOP) = 0 and the reconciler's repair), so the write-ahead record never even transiently
                // claims completed work.
                SessionEnd.SAFETY_STOP ->
                    recorder.onFinalize(recorderSessionId, completedTrials = 0, activeDurationMs = activeDurationMs)
                SessionEnd.ABANDONED -> recorder.onAbandon(recorderSessionId)
            }
        }
    }

    private fun responseWindowMs(): Long =
        if (reducePressure) (BASE_WINDOW_MS * REDUCED_PRESSURE_FACTOR).toLong() else BASE_WINDOW_MS

    private fun record(type: SafetyEventType, reportedIntensity: Int? = null, proxy: OverloadProxy? = null) {
        safetyEvents += SafetyEvent(type, (graph.epochClock.nowMillis() - startedAtMs).coerceAtLeast(0), reportedIntensity, proxy)
    }

    private fun keyFor(category: EmotionCategory): EmotionResponseKey = when (category) {
        EmotionCategory.INTERNAL_STATE -> EmotionResponseKey.CATEGORY_A
        EmotionCategory.THOUGHT -> EmotionResponseKey.CATEGORY_B
        EmotionCategory.IMPULSE -> EmotionResponseKey.CATEGORY_C
        EmotionCategory.FACT -> EmotionResponseKey.CATEGORY_D
    }

    private fun governingAxisFor(id: EmotionExerciseId): EmotionAxis = when (id) {
        EmotionExerciseId.EM1_PAUSA -> EmotionAxis.INHIBITORY_DEMAND
        EmotionExerciseId.EM2_RECUPERACION -> EmotionAxis.POST_ERROR_LOAD
        EmotionExerciseId.EM3_SEPARADOR -> EmotionAxis.CATEGORY_AMBIGUITY
        EmotionExerciseId.EM8_ANTIIMPULSO -> EmotionAxis.INHIBITORY_DEMAND
        EmotionExerciseId.EM10_SEP_PRESION -> EmotionAxis.TEMPORAL_PRESSURE
        EmotionExerciseId.EM11_FEEDBACK_NEG -> EmotionAxis.POST_ERROR_LOAD
        else -> EmotionAxis.TIME_ON_TASK
    }

    private data class ResponseEvent(val nanos: Long, val key: EmotionResponseKey, val category: EmotionCategory?)

    private enum class SessionEnd { COMPLETED, SAFETY_STOP, ABANDONED }

    /** Holder lifecycle: a holder runs AT MOST one session. NOT_STARTED → RUNNING → TERMINAL (first-wins). */
    private enum class Phase { NOT_STARTED, RUNNING, TERMINAL }

    /** What the atomic critical terminal produced, so the best-effort phase can proceed without re-reading state.
     * [aggregate] is the derived calibration computed once for the Result tier and reused for the (best-effort)
     * aggregate save — no decorative fields, no second computation. Null when there is no completed session yet. */
    private data class TerminalOutcome(
        val summarySaved: Boolean,
        val activeDurationMs: Long,
        val terminalAtMs: Long,
        val aggregate: EmotionAggregateDto?,
    )

    private companion object {
        const val BLOCKS = 2
        const val TRIALS_PER_BLOCK = 10
        const val BASE_WINDOW_MS = 2500L
        const val REDUCED_PRESSURE_FACTOR = 1.6
        const val NANOS_PER_MS = 1_000_000L
        const val BEST_EFFORT_TIMEOUT_MS = 3_000L // bounds the best-effort SessionRecord/aggregate I/O
    }
}
