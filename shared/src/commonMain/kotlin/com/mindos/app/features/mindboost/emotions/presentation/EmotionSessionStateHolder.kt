@file:Suppress("MagicNumber", "MaxLineLength") // Session tuning constants + dense wiring expressions.

package com.mindos.app.features.mindboost.emotions.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
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
import com.mindos.app.features.mindboost.emotions.domain.model.ProfileTrend
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.domain.model.TransferConfidence
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionSafetyLimits
import com.mindos.app.features.mindboost.emotions.domain.safety.EmotionSafetyState
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadProxy
import com.mindos.app.features.mindboost.emotions.domain.safety.OverloadSignals
import com.mindos.app.features.mindboost.emotions.domain.safety.PauseGovernorState
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEvent
import com.mindos.app.features.mindboost.emotions.domain.safety.SafetyEventType
import com.mindos.app.features.mindboost.emotions.domain.session.EmotionTrialClassifier
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionRunningModel
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
@Suppress("TooManyFunctions", "LongMethod")
class EmotionSessionStateHolder(
    private val graph: EmotionGraph,
    private val exerciseId: EmotionExerciseId,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
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

    fun start() {
        if (sessionJob?.isActive == true) return
        sessionJob = scope.launch { runSession() }
    }

    fun onTap() = onResponse(EmotionResponseKey.PRIMARY, null)
    fun onLeft() = onResponse(EmotionResponseKey.LEFT, null)
    fun onRight() = onResponse(EmotionResponseKey.RIGHT, null)
    fun onCategory(category: EmotionCategory) = onResponse(keyFor(category), category)

    private fun onResponse(key: EmotionResponseKey, category: EmotionCategory?) {
        val pending = pendingResponse ?: return
        if (!pending.isCompleted) pending.complete(ResponseEvent(graph.monotonicClock.nowNanos(), key, category))
    }

    fun submitSelfReport(report: SelfReport) {
        pendingSelfReport?.takeIf { !it.isCompleted }?.complete(report)
    }

    fun submitCheckInIntensity(intensityValue: Int) {
        pendingIntensity?.takeIf { !it.isCompleted }?.complete(intensityValue)
    }

    /** Safety bar: a strategic pause. Logged, never penalized; over-use trips reduce (anti-gaming). */
    fun onPause() {
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
        resumeFromPause?.takeIf { !it.isCompleted }?.complete(Unit)
    }

    /** Safety bar: lower induction intensity and widen the response window (reduce temporal pressure). */
    fun onReduceIntensity() {
        reduceIntensityInternal()
        record(SafetyEventType.REDUCE_INTENSITY)
    }

    /** Safety bar: leave now. Best-effort persist of the abandoned session, then exit. */
    fun onExit() {
        sessionJob?.cancel()
        pendingResponse = null
        scope.launch { finalize(SessionEnd.ABANDONED) }
        uiState = EmotionSessionUiState.Exited
    }

    private fun reduceIntensityInternal() {
        intensity = intensity.reducedBy()
        reducePressure = true
    }

    private suspend fun runSession() {
        startedAtMs = graph.epochClock.nowMillis()
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

    private suspend fun finalize(ended: SessionEnd) {
        val finalResult = graph.assembler.assembleBlock(allTrials, definition)
        val sessions = (runCatching { graph.repository.recentSummaries(userId).size }.getOrDefault(0)) + 1
        val tier = tierFor(sessions)
        val now = graph.epochClock.nowMillis()
        val summary = EmotionSessionSummaryDto(
            exerciseId = exerciseId.name,
            startedAtMillis = startedAtMs,
            durationMs = (now - startedAtMs).coerceAtLeast(0),
            difficultySummary = DifficultySummaryDto(governingAxis.name, difficulty.level(governingAxis), intensity.value),
            metricsSummary = EmotionMappers.toMetricSummaryDtos(finalResult.metrics),
            selfReportBefore = EmotionMappers.toSelfReportDto(selfReportBefore),
            selfReportAfter = EmotionMappers.toSelfReportDto(selfReportAfter),
            safetyEvents = EmotionMappers.toSafetyEventDtos(safetyEvents),
            completed = ended == SessionEnd.COMPLETED,
            abandoned = ended == SessionEnd.ABANDONED,
        )
        runCatching { graph.repository.saveSessionSummary(userId, summary) }
        runCatching { graph.repository.saveAggregate(userId, aggregateOf(sessions, tier, now)) }
        if (ended != SessionEnd.ABANDONED) {
            val primary = finalResult.metrics[definition.governingMetric]
            uiState = EmotionSessionUiState.Result(
                exerciseName = definition.displayName,
                feedback = graph.feedbackBuilder.build(definition, primary, tier),
                stoppedForSafety = ended == SessionEnd.SAFETY_STOP,
            )
        }
    }

    private fun aggregateOf(sessions: Int, tier: ProfileTier, now: Long) = EmotionAggregateDto(
        sessionsCompleted = sessions,
        tier = tier.name,
        dataSufficiency = (sessions.toDouble() / STABLE_SESSIONS).coerceAtMost(1.0),
        safetyLoad = (safetyEvents.size.toDouble() / SAFETY_LOAD_NORM).coerceAtMost(1.0),
        transferConfidence = TransferConfidence.NOT_DEMONSTRATED.name,
        confidence = (sessions.toDouble() / STABLE_SESSIONS).coerceAtMost(1.0),
        trend = ProfileTrend.UNKNOWN.name,
        updatedAtMillis = now,
    )

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

    private fun tierFor(sessions: Int): ProfileTier = when {
        sessions <= 1 -> ProfileTier.EXPLORATORY
        sessions <= 3 -> ProfileTier.PRELIMINARY
        sessions <= 7 -> ProfileTier.INITIAL_TREND
        else -> ProfileTier.STABLE
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

    private companion object {
        const val BLOCKS = 2
        const val TRIALS_PER_BLOCK = 10
        const val BASE_WINDOW_MS = 2500L
        const val REDUCED_PRESSURE_FACTOR = 1.6
        const val NANOS_PER_MS = 1_000_000L
        const val STABLE_SESSIONS = 8.0
        const val SAFETY_LOAD_NORM = 6.0
    }
}
