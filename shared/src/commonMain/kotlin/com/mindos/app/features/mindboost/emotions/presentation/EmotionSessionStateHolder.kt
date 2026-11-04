@file:Suppress("MagicNumber", "MaxLineLength") // Session tuning constants + dense wiring expressions.

package com.mindos.app.features.mindboost.emotions.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.core.calibration.BlockPrediction
import com.mindos.app.features.mindboost.core.calibration.BlockPredictionOutcome
import com.mindos.app.features.mindboost.core.calibration.CalibrationJudgment
import com.mindos.app.features.mindboost.core.calibration.ConfidenceLevel
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.emotions.analytics.EmotionAnalyticsEvent
import com.mindos.app.features.mindboost.emotions.data.dto.DifficultySummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionAggregateDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionPendingTerminalDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionScoringVersions
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSessionSummaryDto
import com.mindos.app.features.mindboost.emotions.data.dto.EmotionSyncStatus
import com.mindos.app.features.mindboost.emotions.data.dto.HoldoutProbeDto
import com.mindos.app.features.mindboost.emotions.data.dto.NoOpEmotionCloudSync
import com.mindos.app.features.mindboost.emotions.data.dto.PressureCellsDto
import com.mindos.app.features.mindboost.emotions.data.local.EmotionAggregateFactory
import com.mindos.app.features.mindboost.emotions.data.local.EmotionCalibrationSummaries
import com.mindos.app.features.mindboost.emotions.data.local.EmotionExposureStore
import com.mindos.app.features.mindboost.emotions.data.local.EmotionTerminalSemantics
import com.mindos.app.features.mindboost.emotions.data.mapper.EmotionMappers
import com.mindos.app.features.mindboost.emotions.di.EmotionGraph
import com.mindos.app.features.mindboost.emotions.domain.adaptive.AdaptiveAction
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAdaptiveInput
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionAxis
import com.mindos.app.features.mindboost.emotions.domain.adaptive.EmotionDifficultyVector
import com.mindos.app.features.mindboost.emotions.domain.adaptive.InductionIntensity
import com.mindos.app.features.mindboost.emotions.domain.calibration.EmotionCalibrationHost
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseId
import com.mindos.app.features.mindboost.emotions.domain.exercise.EM2RecuperacionGenerator
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionAccuracyStaircaseGenerator
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionBlockSpec
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionHoldoutProbe
import com.mindos.app.features.mindboost.emotions.domain.exercise.EmotionStopSignalGenerator
import com.mindos.app.features.mindboost.emotions.domain.exercise.SeparatorDifficulty
import com.mindos.app.features.mindboost.emotions.domain.exercise.SeparatorItemFactory
import com.mindos.app.features.mindboost.emotions.domain.feedback.EmotionBlockScore
import com.mindos.app.features.mindboost.emotions.domain.feedback.directionOf
import com.mindos.app.features.mindboost.emotions.domain.metrics.ClassifiedEmotionTrial
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionHoldoutEvidence
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricKey
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionRtStatistics
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionSessionQuality
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionSessionQualityEvaluator
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionSessionStats
import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionSessionStatsCalculator
import com.mindos.app.features.mindboost.emotions.domain.metrics.PressureCells
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
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

/**
 * Maps a domain stimulus to the render-ready field. Pure + internal so it is unit-testable.
 *
 * [optionOrderRng] draws the per-trial option order for the classification paradigm. It has no default on purpose:
 * a default would be a constant order, which is exactly the fixed grid this parameter exists to remove, and it
 * would come back silently the first time a call site forgot to pass one.
 */
internal fun emotionFieldFor(
    stimulus: EmotionStimulus,
    optionOrderRng: Random,
): EmotionFieldState = when (val payload = stimulus.payload) {
    is EmotionStimulusPayload.AffectiveGoNoGo -> EmotionFieldState.GoNoGo(payload.frameOpen, payload.affectiveWord)
    is EmotionStimulusPayload.TwoAltPerceptual -> {
        val gap = (payload.discriminability * MAGNITUDE_SPAN).toFloat()
        val correctLeft = payload.correct == EmotionResponseKey.LEFT
        EmotionFieldState.TwoAlt(
            leftMagnitude = BASE_MAGNITUDE + if (correctLeft) gap else -gap,
            rightMagnitude = BASE_MAGNITUDE + if (correctLeft) -gap else gap,
        )
    }
    is EmotionStimulusPayload.Classification ->
        EmotionFieldState.FourCategory(payload.phrase, EmotionCategory.entries.shuffled(optionOrderRng))
}

private const val BASE_MAGNITUDE = 0.5f
private const val MAGNITUDE_SPAN = 0.4

/** Keeps the option-order stream off the per-block generator seeds (`seed + blockIndex`). */
private const val OPTION_ORDER_SEED_OFFSET = 1_000_003L

/**
 * Ola 0 · S3 — how many trials a block must present for THIS exercise's governing metric to be able to reach
 * confidence at all. Derived per governing metric rather than set globally, because the binding sample floor is
 * different for each and a single blanket number does harm in both directions.
 *
 * - `COMMISSION_RATE` needs at least 6 NO-GO trials in the block. The go/no-go generators derive the no-go share
 *   from a ratio of the block size, so a 10-trial block produced only 3–4 of them and the metric could never be
 *   confident, at any difficulty level: the COMMISSION zone was structurally unable to adapt. A 20-trial block
 *   clears the floor at every level, and it also lifts the GO trials past the reaction-time floor, which is what
 *   makes a within-session response-time reading possible for these exercises in the first place.
 * - `POST_ERROR_RECOVERY` needs several errors that are each followed by more trials. That depends on the USER,
 *   not the generator, so no block size can guarantee it — but a 10-trial block leaves almost no room for it,
 *   and this is the metric the tolerance exercises are actually adapted on.
 * - Everything else keeps the base size. In particular the classification exercises govern on `CATEGORY_ACCURACY`,
 *   whose floor is already met at the base size, and enlarging THEIR blocks would actively hurt: the authored
 *   content bank holds only 8 ambiguous training items, so a bigger block is silently backfilled with clear ones
 *   and the maximum reachable ambiguity — the very axis those exercises adapt on — goes DOWN as the block grows.
 */
internal fun emotionTrialsPerBlock(exerciseId: EmotionExerciseId, governingMetric: EmotionMetricKey): Int = when {
    // Ola 3 · R1 — the fused inhibition exercise is sized by the SMALLEST cell it has to fill, not by its
    // governing metric. Its affective delta needs 12 stop trials per tone cell; at the BOTTOM of the stop-share
    // axis (0.25) that is 24 stop trials, so 96 trials across the session — 4 blocks of 24. Sized off the top of
    // the axis instead, the delta would exist only for users the engine had already pushed to maximum demand,
    // which is how difficulty silently decides whether a capacity claim is even eligible.
    exerciseId == EmotionExerciseId.EM1_PAUSA -> STOP_SIGNAL_TRIALS_PER_BLOCK
    // Ola 3 · R2 — sized off the BOTTOM of its own axis, the same rule R1 uses. Every post-error reading needs 8
    // pairs, and pairs are made of the user's mistakes, so the block has to be long enough that the mistakes
    // arrive even when the exercise is asking for as few as it ever asks for. At the bottom of `POST_ERROR_LOAD`
    // the staircase targets a 0.25 error rate, and 48 trials is the smallest block whose 10th-percentile error
    // count still clears 8 (0.25*48 - 1.28*sqrt(0.25*0.75*48) = 8.2). Sized off the TOP of the axis instead, the
    // exercise's primary reading would exist only for users the engine had already pushed to more errors — which
    // is difficulty quietly deciding whether a claim is eligible, the defect R1 documented.
    exerciseId == EmotionExerciseId.EM2_RECUPERACION -> RECOVERY_TRIALS_PER_BLOCK
    governingMetric == EmotionMetricKey.COMMISSION_RATE ||
        governingMetric == EmotionMetricKey.POST_ERROR_RECOVERY -> EXTENDED_TRIALS_PER_BLOCK
    else -> BASE_TRIALS_PER_BLOCK
}

/**
 * Ola 3 · R1 — how many blocks this exercise runs. Three everywhere else (see `BLOCKS`); four for the fused
 * inhibition exercise, which is the count its cell arithmetic needs and the count its own design specifies.
 * Blocks are also adaptive DECISIONS, so a fourth costs nothing in adaptation and buys one more.
 */
internal fun emotionBlocksFor(exerciseId: EmotionExerciseId): Int =
    if (exerciseId == EmotionExerciseId.EM1_PAUSA) STOP_SIGNAL_BLOCKS else EmotionSessionStateHolder.BLOCKS

private const val BASE_TRIALS_PER_BLOCK = 10
private const val EXTENDED_TRIALS_PER_BLOCK = 20
private const val STOP_SIGNAL_TRIALS_PER_BLOCK = 24
private const val STOP_SIGNAL_BLOCKS = 4
private const val RECOVERY_TRIALS_PER_BLOCK = 48

/**
 * Ola 0 · S1 — the governing-axis difficulty a NEW session of [exerciseId] resumes from, or null when there is
 * nothing comparable to resume and the catalog default must stand.
 *
 * Difficulty used to be rebuilt from scratch on every launch, so adaptation could never accumulate: whatever a
 * session learned about the user died with it, and the final block's decision — the only one that can now raise
 * difficulty — was written and immediately discarded.
 *
 * Four deliberate choices, each of which the obvious implementation gets wrong:
 * - The level comes from `difficultySummary.level`, a REQUIRED field, never from `difficultyStart`, which carries a
 *   `0.0` default and would therefore make every pre-existing row resume at "easiest" as if it had been measured.
 * - Rows are matched on the exercise AND its governing axis, because several exercises share an axis and matching
 *   on either alone would let one exercise inherit another's staircase.
 * - The MOST RECENT matching row wins whatever its terminal was, with one exception: a session that ended in a
 *   safety stop resumes at the default instead. Skipping such a row (rather than honouring it) would walk back to
 *   an OLDER, HIGHER level and hand the user more load right after the session that overloaded them.
 * - The level is capped at what the exercise's content can actually deliver, so a durable starting point can never
 *   be a difficulty the generator has to silently water down.
 *
 * The hysteresis counter is deliberately NOT carried across sessions: it is advanced by check-ins as well as by
 * quiet blocks, so restoring it would let a block that fired an overload proxy bank credit toward a future
 * step-up, and two stable blocks weeks apart are not the evidence the anti-oscillation guard was designed around.
 */
internal fun carriedDifficultyLevel(
    summaries: List<EmotionSessionSummaryDto>,
    exerciseId: EmotionExerciseId,
    governingAxis: EmotionAxis,
    ceiling: Double,
): Double? {
    val latest = summaries
        .filter { it.exerciseId == exerciseId.name && it.difficultySummary.governingAxis == governingAxis.name }
        .maxByOrNull { it.startedAtMillis }
        ?: return null
    return if (latest.endReason == EmotionTerminalSemantics.SAFETY_STOP) {
        null
    } else {
        latest.difficultySummary.level.coerceIn(0.0, ceiling)
    }
}

/**
 * The highest governing-axis level this exercise's CONTENT can actually deliver in a block of [trialsPerBlock].
 *
 * For the classification exercises the governing axis is category ambiguity, and the authored bank holds a fixed
 * number of ambiguous TRAINING items. Above the point where the block would need more of them than exist, the
 * selector quietly backfills with clear items: the number rises but the task does not get harder. Deriving the
 * ceiling from the bank keeps the two in step instead of pinning a second copy of the count here.
 */
internal fun deliverableDifficultyCeiling(exerciseId: EmotionExerciseId, trialsPerBlock: Int): Double =
    if (exerciseId == EmotionExerciseId.EM3_SEPARADOR || exerciseId == EmotionExerciseId.EM10_SEP_PRESION) {
        val ambiguousTrainingItems = SeparatorItemFactory.ALL.count { !it.isEval && it.ambiguity == 1 }
        (ambiguousTrainingItems.toDouble() / trialsPerBlock).coerceIn(0.0, 1.0)
    } else {
        1.0
    }

/**
 * Drives one emotion session in real time: self-report → two mini-blocks with a proxy-armed mid-session
 * safety check between them → self-report → sober feedback, persisting an encrypted summary. The holder
 * ORCHESTRATES; classification/metrics/adaptation/feedback are the pure domain engines. RT is monotonic.
 * Safety strictly precedes performance, and the always-visible safety bar (pause / reduce / exit) routes
 * here. Local UI counters are display-only — the assembler is the metrics authority.
 */
// `LargeClass` joined the list in Ola 3: R1's stop-signal staircase and R2's post-error settle both live in the
// real-time loop, because both need the outcome of the trial that just ended before the next one is presented.
// Extracting them would move the loop's own control flow behind a seam and buy nothing but a smaller file.
@Suppress("TooManyFunctions", "LongMethod", "LongParameterList", "LargeClass")
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
    private val trialsPerBlock = emotionTrialsPerBlock(exerciseId, definition.governingMetric)
    private val blocks = emotionBlocksFor(exerciseId)

    /**
     * Ola 3 · R3 · P1 — one stream for the whole session, advanced by every trial `present` shows, so the holdout
     * probe and the training blocks draw from the same sequence and a replay of the session reproduces it exactly.
     *
     * Offset off the block seeds because `generate` is handed `seed + blockIndex`: without it, block 0 would shuffle
     * its items from an identical stream, which is harmless but reads as a bug every time someone finds it.
     */
    private val optionOrderRng = Random(seed + OPTION_ORDER_SEED_OFFSET)

    /** Ola 3 · R3 · P3 — set once per session from the pooled held-out probes; see [pressureAppliesTo]. */
    private var pressureUnlocked = false

    var uiState: EmotionSessionUiState by mutableStateOf(
        EmotionSessionUiState.SelfReportBefore(definition.displayName, definition.plainDescription),
    )
        private set

    private var sessionJob: Job? = null
    private var pendingResponse: CompletableDeferred<ResponseEvent>? = null
    private var pendingSelfReport: CompletableDeferred<SelfReport>? = null
    private var pendingIntensity: CompletableDeferred<Int>? = null
    private var resumeFromPause: CompletableDeferred<Unit>? = null

    // Ola 1 · N1 — the transversal calibration layer, which owns its own state and records nothing for hosts it
    // cannot serve. It receives a way to SHOW a prompt and nothing else, so it can never reach back into the session.
    private val calibration = EmotionCalibrationCapture { prompt -> uiState = prompt }

    private var difficulty = EmotionDifficultyVector.initial(EmotionAxis.entries.toSet())

    /**
     * Ola 3 · R1 — the online stop-signal staircase, or null for a paradigm that has none. It lives for the whole
     * SESSION rather than per block: it converges on p(respond|signal) ~ 0.5 over dozens of stop trials, and
     * restarting it every block would keep re-walking the same approach and spend almost no time near the only
     * point at which the SSRT estimate is valid.
     */
    private val stopTracker = (generator as? EmotionStopSignalGenerator)?.tracker(difficulty)

    /**
     * Ola 3 · R2 — the online accuracy staircase, or null for a paradigm that has none. Session-lived for the same
     * reason as [stopTracker]: it is holding an ERROR RATE at a target, and a per-block restart would spend every
     * block walking back to the point it had already found.
     */
    private val accuracyStaircase = (generator as? EmotionAccuracyStaircaseGenerator)?.staircase(difficulty)

    /** Ola 3 · R2 — consecutive correct answers; the visible, recoverable cost an error resets. Null = no streak. */
    private var streak: Int? = if (accuracyStaircase != null) 0 else null

    /** Ola 3 · R2 — true once the induction ceiling has fired this session; the relief target is not re-applied. */
    private var reliefApplied = false

    /** Ola 3 · R2 — response times of the first trials of the session, for the in-block RT baseline. */
    private val baselineRts = mutableListOf<Double>()

    /** Consecutive failed inhibitions — counted apart from [consecutiveErrors]; see [updateErrorStreak]. */
    private var consecutiveFailedStops = 0
    private var difficultyStartLevel = 0.0 // MC8u-C §3: the governing-axis difficulty at session start (persisted)
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

    // MC8u-D §5: cross-session exposure guard (EM3/EM10 content bank). Loaded once at session start; block N is drawn
    // avoiding both the persisted recent history AND the phrases already used in earlier blocks of THIS session.
    private var recentExposedPhrases: Set<String> = emptySet()
    private val sessionUsedPhrases = linkedSetOf<String>()

    // Ola 4: the held-out probe. Kept in its OWN list, never merged into allTrials — that separation is the whole
    // point: probe trials must not reach the assembler's metrics, the block score, the adaptive decision, the
    // difficulty, the error streak or the calibration count. They are evidence ABOUT training, not training.
    private val probeTrials = mutableListOf<RecordedEmotionTrial>()
    private var probeServedIds: List<String> = emptyList()

    // Ola 3 · R3 · P4 — this session's share of the robustness index, accumulated block by block because the
    // block is the only place that knows which mode it ran in. Probe trials are excluded for the same reason they
    // are excluded everywhere else: they are held-out evidence, always calm, and never training.
    private var pressureCells = PressureCells.EMPTY

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
        difficultyStartLevel = difficulty.level(governingAxis) // captured before any adaptation
        beginRecording()
        emitAnalytics(EmotionAnalyticsEvent.ExerciseStarted(exerciseId.name))
        sessionJob = scope.launch { runSession() }
    }

    /**
     * MC8u-B+ §I: PII-free analytics emission, best-effort and FAIL-SAFE — a throwing/slow sink can never break, freeze
     * or alter a session. Only cooperative cancellation and a fatal Error propagate; an ordinary sink Exception is
     * isolated. Events carry no content, self-report values, user id, or wall-clock time (see [EmotionAnalyticsEvent]).
     */
    private fun emitAnalytics(event: EmotionAnalyticsEvent) {
        runCatching { graph.analytics.log(event) }
            .exceptionOrNull()?.let { if (it is CancellationException || it is Error) throw it }
    }

    /** Terminal analytics for a result-bearing terminal (MC8u-B+ §I). A COMPLETED session reports completion +
     * calibration progress (and low-confidence when the reading was not trustworthy); a SAFETY_STOP relies on the
     * already-logged safety events. Both surface the shown recommendation. All PII-free. */
    private fun emitTerminalAnalytics(ended: SessionEnd, quality: EmotionSessionQuality, counted: Boolean, validSessions: Int, recommendedId: EmotionExerciseId?) {
        val stoppedForSafety = ended == SessionEnd.SAFETY_STOP
        // MC8u-D §3: a non-abandoned terminal always reports exercise completion; anomalies are surfaced coarsely.
        emitAnalytics(EmotionAnalyticsEvent.ExerciseCompleted(exerciseId.name, stoppedForSafety))
        if (quality.anomalyFlags.isNotEmpty()) {
            emitAnalytics(EmotionAnalyticsEvent.AnomalyFlagDetected(exerciseId.name, quality.anomalyFlags))
        }
        if (ended == SessionEnd.COMPLETED) {
            emitAnalytics(EmotionAnalyticsEvent.SessionCompleted(exerciseId.name, counted, validSessions))
            if (!counted) emitAnalytics(EmotionAnalyticsEvent.SessionNotCounted(exerciseId.name, notCountedReason(quality)))
            emitAnalytics(EmotionAnalyticsEvent.CalibrationProgressUpdated(exerciseId.name, validSessions, EmotionStrings.VALID_SESSION_TARGET))
            if (validSessions >= EmotionStrings.VALID_SESSION_TARGET) {
                emitAnalytics(EmotionAnalyticsEvent.CalibrationCompleted(exerciseId.name, validSessions))
            }
            if (!quality.governingConfident) {
                emitAnalytics(EmotionAnalyticsEvent.LowConfidenceResultShown(exerciseId.name, quality.anomalyFlags))
            }
        } else {
            // SAFETY_STOP is a non-abandoned terminal that never counts — report it coarsely as not-counted too.
            emitAnalytics(EmotionAnalyticsEvent.SessionNotCounted(exerciseId.name, "safety_stop"))
        }
        recommendedId?.let { emitAnalytics(EmotionAnalyticsEvent.RecommendationShown(exerciseId.name, it.name)) }
    }

    /** Coarse, PII-free reason a completed session did not count (never content, never a per-session score). */
    private fun notCountedReason(quality: EmotionSessionQuality): String = when {
        !quality.governingConfident || quality.anomalyFlags.isNotEmpty() -> "low_confidence"
        else -> "insufficient"
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

    /** Ola 1 · N1 — the judgments captured this session, in order. Empty for hosts the layer cannot serve. */
    internal val sessionCalibration: List<CalibrationJudgment> get() = calibration.sessionJudgments

    /** Ola 1 · N1 — this session's block forecasts, scored. Empty when the user let every prompt lapse. */
    internal val sessionBlockForecasts: List<BlockPredictionOutcome> get() = calibration.sessionForecasts

    /** Ola 1 · N1 — the confidence strip. Ignored after the window closes: silence is a valid, recorded outcome. */
    fun submitConfidence(level: ConfidenceLevel) {
        if (phase != Phase.RUNNING) return
        calibration.submitConfidence(level)
    }

    /** Ola 1 · N1 — the per-block forecast: how many of the coming block the user expects to get right. */
    fun submitBlockForecast(predictedCorrect: Int) {
        if (phase != Phase.RUNNING) return
        calibration.submitForecast(predictedCorrect)
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
        calibration.cancelPending()
        pendingResponse = null
        pendingSelfReport = null
        pendingIntensity = null
        resumeFromPause = null
        calibration.clearPending()
    }

    private suspend fun runSession() {
        restoreCarriedDifficulty()
        // MC8u-D §5: load this user's recent exposures for content-bank exercises so the generator draws fresher items.
        if (usesContentBank()) {
            recentExposedPhrases = (graph.repository as? EmotionExposureStore)
                ?.recentExposedPhrases(userId, exerciseId.name)?.toSet() ?: emptySet()
        }
        selfReportBefore = awaitSelfReport(EmotionSessionUiState.SelfReportBefore(definition.displayName, definition.plainDescription))
        runHoldoutProbe()

        var ended = SessionEnd.COMPLETED
        for (blockIndex in 0 until blocks) {
            pauseUsesThisBlock = 0
            val trials = runBlock(blockIndex)
            allTrials += trials
            val endAfterBlock = applyAdaptationAndSafety(trials, isLastBlock = blockIndex == blocks - 1)
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

    /**
     * Ola 0 · S1 — resumes the staircase where this exercise left it, then stamps the level this session actually
     * STARTS from. Best-effort on purpose: a history that cannot be read leaves the catalog default in place and
     * training continues, because losing the carried difficulty is a worse-calibrated session while refusing to
     * start is no session at all.
     */
    private suspend fun restoreCarriedDifficulty() {
        val carried = runCatching {
            carriedDifficultyLevel(
                summaries = graph.repository.recentSummaries(userId),
                exerciseId = exerciseId,
                governingAxis = governingAxis,
                ceiling = deliverableDifficultyCeiling(exerciseId, trialsPerBlock),
            )
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            null
        }
        if (carried != null) difficulty = difficulty.with(governingAxis, carried)
        // Stamped AFTER the restore: a record claiming the session began at the catalog default while the user
        // actually trained at a carried level would misreport every resumed session's starting point.
        difficultyStartLevel = difficulty.level(governingAxis)
    }

    /**
     * Ola 3 · R3 · P3 — whether block [blockIndex] is served under time pressure. Three conditions, and each one
     * says something the others do not.
     *
     * - [pressureUnlocked]: held-out accuracy in calm has cleared the bar. Without it the mode never starts.
     * - `blockIndex > 0`: the first block of every session stays calm. R3 reads robustness as accuracy under
     *   pressure against accuracy in calm, and a session with no calm block has no denominator — leaving the
     *   reference inside the same session is also what keeps between-session drift out of the comparison.
     * - `!reducePressure`: the safety path widens the window when overload is detected, and a pressure window
     *   stamped on top of that would silently discard the reduction. Safety wins, so the block reverts to calm.
     */
    private fun pressureAppliesTo(blockIndex: Int): Boolean =
        pressureUnlocked && blockIndex > 0 && !reducePressure

    private suspend fun runBlock(blockIndex: Int): List<RecordedEmotionTrial> {
        // MC8u-D §5: exclude the persisted recent history AND phrases already used earlier this session (no in-session
        // repeats). Empty for non-content-bank exercises → identical to pre-guard behavior.
        val recent = if (usesContentBank()) recentExposedPhrases + sessionUsedPhrases else emptySet()
        val spec = EmotionBlockSpec(targetTrialCount = trialsPerBlock, responseWindowMs = responseWindowMs(), recentPhrases = recent)
        val stimuli = generator.generate(difficulty, spec, seed + blockIndex)
        if (usesContentBank()) {
            sessionUsedPhrases += stimuli.mapNotNull { (it.payload as? EmotionStimulusPayload.Classification)?.phrase }
        }
        val forecast = calibration.askForecast(servesCalibration(), blockIndex, stimuli.size)
        val underPressure = pressureAppliesTo(blockIndex)
        val trials = present(
            stimuli = stimuli,
            indexOffset = blockIndex * trialsPerBlock,
            total = blocks * trialsPerBlock,
            feedsAdaptation = true,
            underPressure = underPressure,
        )
        accumulatePressureCells(trials, underPressure)
        scoreBlockForecast(forecast, trials)
        return trials
    }

    /**
     * Ola 3 · R3 · P4 — folds a finished block into this session's two robustness cells.
     *
     * The mode is taken from what the block was SERVED under, not re-derived from the windows the trials carry.
     * Re-deriving would work today and would be the same defect the module already refused once: a fact that was
     * decided at presentation belongs recorded, not reconstructed from a side effect of itself.
     */
    private fun accumulatePressureCells(trials: List<RecordedEmotionTrial>, underPressure: Boolean) {
        val classifications = trials.filter { it.stimulus.payload is EmotionStimulusPayload.Classification }
        if (classifications.isEmpty()) return
        val correct = classifications.count { EmotionTrialClassifier.classify(it) == EmotionTrialOutcome.CORRECT }
        pressureCells += if (underPressure) {
            PressureCells(0, 0, pressureCorrect = correct, pressureTotal = classifications.size)
        } else {
            PressureCells(calmCorrect = correct, calmTotal = classifications.size, 0, 0)
        }
    }

    /**
     * The cells to persist, or null when this session has nothing to contribute — never an all-zero row, which
     * would read at the aggregate as a session where the user got nothing right.
     */
    private fun pressureCellsSummary(): PressureCellsDto? =
        pressureCells.takeIf { it.calmTotal > 0 || it.pressureTotal > 0 }?.let {
            PressureCellsDto(
                calmCorrect = it.calmCorrect,
                calmTotal = it.calmTotal,
                pressureCorrect = it.pressureCorrect,
                pressureTotal = it.pressureTotal,
            )
        }

    /**
     * Ola 1 · N1 — scores a block against the forecast made before it started.
     *
     * "Correct" here is the same flat bit calibration uses everywhere ([EmotionCalibrationHost.wasCorrect]), not
     * the session's richer outcome classification: the user was asked how many they would get RIGHT, so scoring
     * them against anything else would answer a question they were never asked.
     */
    private fun scoreBlockForecast(forecast: BlockPrediction?, trials: List<RecordedEmotionTrial>) {
        val correct = trials.count {
            EmotionCalibrationHost.wasCorrect(it.stimulus.payload, it.responseKey, it.chosenCategory) == true
        }
        calibration.score(forecast, correct)
    }

    /**
     * Presents [stimuli] one at a time and records the responses. Shared by training blocks and the Ola-4 holdout
     * probe so the probe is the SAME task from the user's side (same renderer, same window, same pause/exit).
     *
     * [feedsAdaptation] is the one behavioural difference: probe trials must not move the error streak, because an
     * evaluation must never steer the training it is there to evaluate. Pause and exit stay live either way —
     * safety is not something a measurement gets to switch off.
     */
    private suspend fun present(
        stimuli: List<EmotionStimulus>,
        indexOffset: Int,
        total: Int,
        feedsAdaptation: Boolean,
        underPressure: Boolean,
    ): List<RecordedEmotionTrial> {
        val recorded = ArrayList<RecordedEmotionTrial>(stimuli.size)
        stimuli.forEachIndexed { i, raw ->
            awaitIfPaused()
            // Ola 3: the staircase's CURRENT setting is stamped onto the stimulus before it is shown, so the
            // recorded trial carries the difficulty the user actually faced. Nothing downstream may re-derive it.
            val stimulus = withOnlineDifficulty(raw, underPressure)
            delay(stimulus.interStimulusDelayMs)
            val onset = graph.monotonicClock.nowNanos()
            val model = EmotionRunningModel(
                exerciseName = definition.displayName,
                field = emotionFieldFor(stimulus, optionOrderRng),
                trialIndex = indexOffset + i,
                trialTotal = total,
                intensity = intensity.value,
                streak = streak,
            )
            uiState = EmotionSessionUiState.Running(model)
            pendingResponse = CompletableDeferred()
            val event = awaitResponse(stimulus)
            pendingResponse = null
            val latency = event?.let { (it.nanos - onset) / NANOS_PER_MS }
            val trial = RecordedEmotionTrial(stimulus, latency, event?.key ?: EmotionResponseKey.NONE, event?.category)
            recorded += trial
            advanceStopStaircase(stimulus, responded = event != null)
            calibration.capture(trial.stimulus.payload, trial.responseKey, trial.chosenCategory)
            captureEarlyRt(latency)
            if (feedsAdaptation) updateErrorStreak(trial)
            settleRecoveryTrial(trial, model)
        }
        return recorded
    }

    /**
     * Stamps the live staircase setting onto the trial about to be shown; a paradigm without one passes through.
     *
     * A stop trial is identified by the answer the GENERATOR declared (`correctResponseKey == NONE`), not by the
     * payload's own `requiresWithhold`: before the stamp, a stop trial's frame is open and indistinguishable
     * from a go trial — which is exactly the point of the paradigm.
     */
    private fun withOnlineDifficulty(
        stimulus: EmotionStimulus,
        underPressure: Boolean,
    ): EmotionStimulus = when (val payload = stimulus.payload) {
        is EmotionStimulusPayload.AffectiveGoNoGo ->
            if (stopTracker != null && stimulus.correctResponseKey == EmotionResponseKey.NONE) {
                stimulus.copy(payload = payload.copy(stopAfterMs = stopTracker.currentSsdMs))
            } else {
                stimulus
            }

        is EmotionStimulusPayload.TwoAltPerceptual ->
            if (accuracyStaircase != null) {
                stimulus.copy(payload = payload.copy(discriminability = accuracyStaircase.currentLevel))
            } else {
                stimulus
            }

        // Ola 3 · R3 · P3 — the pressure window is an ONLINE parameter, like the stop-signal delay above: it
        // depends on history the generator was never given. The generator emits the calm window and the runtime
        // stamps what it actually presented, so the recorded trial carries the window the user really faced.
        is EmotionStimulusPayload.Classification ->
            if (underPressure) {
                stimulus.copy(responseWindowMs = SeparatorDifficulty.pressureWindowMs(payload.phrase))
            } else {
                stimulus
            }
    }

    /**
     * Ola 3 · R2 — everything a two-alternative recovery trial owes the user once it is answered: the staircase
     * step, the visible cost, the neutral marker, and the induction ceiling. Every other paradigm no-ops.
     *
     * Order matters. The staircase is advanced FIRST because its one-trial lag means this outcome will not reach
     * the difficulty until the trial after next — which is the whole reason the post-error trial is not quietly
     * made easier by the error that defines it. The ceiling is checked LAST because it reads the streak of errors
     * this trial just extended.
     */
    private suspend fun settleRecoveryTrial(trial: RecordedEmotionTrial, model: EmotionRunningModel) {
        if (accuracyStaircase == null || trial.stimulus.payload !is EmotionStimulusPayload.TwoAltPerceptual) return
        val correct = EmotionTrialClassifier.classify(trial) == EmotionTrialOutcome.CORRECT
        accuracyStaircase.onOutcome(correct)
        streak = if (correct) (streak ?: 0) + 1 else 0
        if (!correct) showErrorMarker(model)
        applyInductionCeiling()
    }

    /**
     * The brief neutral marker after a wrong answer, with the reset streak still on screen beside it. It replaces
     * the field for [EM2RecuperacionGenerator.ERROR_MARKER_MS] and never names the answer the user missed.
     *
     * The model is PASSED IN rather than read back off `uiState`, and that is the whole correctness of this
     * function. Between the response and this call the trial hands the screen to the Ola-1 confidence strip, which
     * has to come first — the strip asks how sure the user is, and a marker that arrived before it would tell them
     * the answer and destroy the judgment it is measuring. Reading `uiState` here therefore found a
     * `ConfidencePrompt`, not a `Running`, and the original elvis-return swallowed the marker on every
     * strip-bearing trial: the one piece of post-error feedback this exercise is built around, silently absent,
     * with a green suite. The guard that remains is about the states that mean the trial flow is OVER (pause,
     * exit, terminal), where forcing a Running frame back on screen would be a real defect rather than a missing
     * one.
     */
    private suspend fun showErrorMarker(model: EmotionRunningModel) {
        val inTrialFlow = uiState is EmotionSessionUiState.Running || uiState is EmotionSessionUiState.ConfidencePrompt
        if (!inTrialFlow) return
        uiState = EmotionSessionUiState.Running(model.copy(field = EmotionFieldState.ErrorMarker, streak = streak))
        delay(EM2RecuperacionGenerator.ERROR_MARKER_MS)
    }

    /**
     * Ola 3 · R2 — the induction ceiling the audit requires for this exercise, and the only one of the module's
     * paradigms that needs one: its error rate is held at a target ON PURPOSE, so frustration is a designed input
     * rather than an accident, and it must have a stated maximum.
     *
     * The response is the audit's own: after [EM2RecuperacionGenerator.MAX_CONSECUTIVE_ERRORS] errors in a row the
     * staircase moves to a SMALLER target error rate for the rest of the session. It is applied to the process,
     * never to a hand-picked trial — an exercise that reached in and made one measured trial easy would be
     * manufacturing the recovery it exists to measure. Applied once: a ceiling that ratcheted downward on every
     * subsequent run would walk the induction to nothing and take the measurement with it. The exit is one tap
     * away throughout, and `consecutiveErrors` is already what arms the overload check-in.
     */
    private fun applyInductionCeiling() {
        val staircase = accuracyStaircase ?: return
        if (!reliefApplied && consecutiveErrors >= EM2RecuperacionGenerator.MAX_CONSECUTIVE_ERRORS) {
            staircase.retargetTo(1.0 - EM2RecuperacionGenerator.RELIEF_ERROR_RATE)
            reliefApplied = true
            record(SafetyEventType.REDUCE_INTENSITY)
        }
    }

    /**
     * Ola 3 · R2 — the response-time baseline, captured from the FIRST trials of the session rather than from the
     * metrics of the first completed block.
     *
     * The slowing proxy compares a block's response time against a baseline, so a baseline that only exists after
     * a block has finished leaves the first block — the one where a user meets the induction cold — with no proxy
     * at all. Taking it from the opening trials arms it for every block, including that one.
     */
    private fun captureEarlyRt(latencyMs: Long?) {
        if (baselineRtMedian != null || latencyMs == null || baselineRts.size >= BASELINE_TRIALS) return
        baselineRts += latencyMs.toDouble()
        if (baselineRts.size == BASELINE_TRIALS) {
            baselineRtMedian = EmotionRtStatistics.median(baselineRts)
            baselineRtCv = EmotionRtStatistics.coefficientOfVariation(baselineRts)
        }
    }

    /**
     * Awaits the response. A stop trial is presented in TWO phases: the frame is open — so the go response is
     * genuinely under way — and closes when the staircase's delay elapses, leaving the rest of the window to
     * abort in. A response landing in either phase ends the trial, which is why the same deferred is awaited
     * twice rather than replaced.
     */
    private suspend fun awaitResponse(stimulus: EmotionStimulus): ResponseEvent? {
        val stopAfter = (stimulus.payload as? EmotionStimulusPayload.AffectiveGoNoGo)?.stopAfterMs
        val early = withTimeoutOrNull(stopAfter ?: stimulus.responseWindowMs) { pendingResponse?.await() }
        if (early != null || stopAfter == null) return early
        showClosedFrame(stimulus)
        val remaining = stimulus.responseWindowMs - stopAfter
        return if (remaining > 0) withTimeoutOrNull(remaining) { pendingResponse?.await() } else null
    }

    /** Phase two of a stop trial: the same field with the frame shut. Only the frame changes — never the word. */
    private fun showClosedFrame(stimulus: EmotionStimulus) {
        val payload = stimulus.payload as? EmotionStimulusPayload.AffectiveGoNoGo
        val running = uiState as? EmotionSessionUiState.Running
        if (payload != null && running != null) {
            uiState = EmotionSessionUiState.Running(
                running.model.copy(field = EmotionFieldState.GoNoGo(frameOpen = false, word = payload.affectiveWord)),
            )
        }
    }

    /**
     * Feeds the outcome back into the staircase: a successful stop lengthens the next delay, a failed one
     * shortens it. Only stop trials move it — a go trial carries no information about where the delay should be.
     */
    private fun advanceStopStaircase(stimulus: EmotionStimulus, responded: Boolean) {
        val payload = stimulus.payload as? EmotionStimulusPayload.AffectiveGoNoGo
        if (payload?.stopAfterMs != null) stopTracker?.onStopOutcome(successfulStop = !responded)
    }

    /** True when this exercise's paradigm can carry a confidence judgment at all (see [EmotionCalibrationHost]). */
    private fun servesCalibration(): Boolean =
        EmotionCalibrationHost.chanceLevelOf(probePayloadForParadigm()) != null

    /**
     * A zero-cost probe of the exercise's paradigm: generates one stimulus at the current difficulty purely to ask
     * which payload shape this exercise emits. Far less brittle than a hand-maintained list of exercise ids, which
     * would silently stop covering a new exercise the day one is added.
     */
    private fun probePayloadForParadigm(): EmotionStimulusPayload =
        generator.generate(
            difficulty,
            EmotionBlockSpec(targetTrialCount = 1, responseWindowMs = responseWindowMs(), recentPhrases = emptySet()),
            seed,
        ).first().payload

    /**
     * Ola 4 — administers ONE held-out probe, before any training this session, on items this user has never been
     * trained on. Skipped silently (no probe recorded) when the exercise has no content bank, when the served-set
     * cannot be read, or when the holdout is exhausted: a probe drawn on unknown or reused items would be a number
     * without a meaning, which is worse than no number.
     */
    private suspend fun runHoldoutProbe() {
        if (!usesContentBank()) return
        val history = readProbeHistory() ?: return
        val draw = EmotionHoldoutProbe.draw(history.servedItemIds, seed)
        if (draw is EmotionHoldoutProbe.Draw.Served) {
            val stimuli = EmotionHoldoutProbe.stimuli(draw.items, responseWindowMs(), PROBE_INTER_STIMULUS_MS)
            probeTrials += present(
                stimuli = stimuli,
                indexOffset = 0,
                total = EmotionHoldoutProbe.PROBE_SIZE,
                feedsAdaptation = false,
                // Ola 3 · R3 · P3 — the probe is ALWAYS calm, including in a session whose training runs under
                // pressure. It is this module's only cross-session comparable measurement and a user gets at most
                // `MAX_PROBES` of them, so letting its conditions follow the session's mode would split that one
                // series into two half-series that cannot be pooled — and it is the series the unlock reads.
                underPressure = false,
            )
            probeServedIds = draw.items.map { it.phrase }
        }
        // Decided AFTER this session's probe so the freshest evidence counts, and re-decided from the whole pooled
        // history every session, so it is reversible when accuracy falls back.
        pressureUnlocked = EmotionHoldoutEvidence.pressureUnlocked(
            correct = history.correct + probeCorrect(),
            items = history.items + probeTrials.size,
        )
    }

    /** Every held-out probe this user has already answered: which items were burned, and how they went. */
    private data class ProbeHistory(val servedItemIds: Set<String>, val correct: Int, val items: Int)

    /**
     * This user's probe history, or null when it cannot be read.
     *
     * Null means "do not probe AND do not unlock pressure": serving a probe without knowing what was already
     * burned risks re-testing an item the user has seen, which measures memory of the probe instead of the ability
     * it claims to measure — and unlocking a harder mode on evidence we could not read is the same mistake with
     * the user on the receiving end.
     */
    private suspend fun readProbeHistory(): ProbeHistory? = runCatching {
        val probes = graph.repository.recentSummaries(userId).mapNotNull { it.holdoutProbe }
        ProbeHistory(
            servedItemIds = probes.flatMap { it.servedItemIds }.toSet(),
            correct = probes.sumOf { it.correct },
            items = probes.sumOf { it.total },
        )
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        null
    }

    /** Held-out items answered correctly in THIS session's probe. */
    private fun probeCorrect(): Int =
        probeTrials.count { EmotionTrialClassifier.classify(it) == EmotionTrialOutcome.CORRECT }

    /** The probe result to persist, or null when no probe ran this session (never a fabricated empty probe). */
    private fun probeSummary(): HoldoutProbeDto? {
        if (probeServedIds.isEmpty() || probeTrials.isEmpty()) return null
        return HoldoutProbeDto(servedItemIds = probeServedIds, correct = probeCorrect(), total = probeTrials.size)
    }

    /** Returns SAFETY_STOP if the session must end now; otherwise applies the adaptive decision. */
    private suspend fun applyAdaptationAndSafety(trials: List<RecordedEmotionTrial>, isLastBlock: Boolean): SessionEnd {
        val result = graph.assembler.assembleBlock(trials, definition)
        val proxies = graph.overloadDetector.detect(
            OverloadSignals(
                metrics = result.metrics,
                consecutiveErrors = consecutiveErrors,
                // Ola 0 · S5 — this signal HAS NO PRODUCER, and saying so out loud is the point.
                //
                // `OverloadThresholds.exitAttemptsPerBlock` advertises an armed safety net that fires after
                // repeated attempts to leave, but the first `onExit` in a running session is already terminal:
                // there is no second attempt to count, so the proxy can never fire no matter how the user behaves.
                // The number below is therefore not a measurement, it is the honest absence of one.
                //
                // It is deliberately NOT fixed here. The only way to produce a second attempt is to make leaving
                // harder — a confirmation step between the user and the exit — and putting a speed bump in front
                // of the exit of an emotion-regulation exercise, in the name of detecting distress, would trade a
                // real safety property for a detector. That is a product decision, not a wiring fix, so the gap
                // is recorded and pinned by a test rather than papered over with a counter that cannot reach its
                // own threshold.
                exitAttempts = NO_EXIT_ATTEMPT_SIGNAL,
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
                governingMetric = definition.governingMetric,
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

    /**
     * Ola 3 · R1 — a failed inhibition on a STAIRCASE trial is not an error in the sense this streak means.
     *
     * The staircase converges on failing to stop about half the time, on purpose: that is the regime the SSRT
     * estimate is only valid in. Feeding those failures into the generic streak would arm the overload check-in
     * and the step-down rule against the measurement working as designed, and the exercise would walk its own
     * difficulty down until nothing was measurable. So a failed stop is NEUTRAL for the generic streak and is
     * counted on its own; only a RUN of them — the audit's overload signal for this paradigm — becomes the
     * streak, which is what produces the designed response (lower the stop share, never lengthen the session).
     *
     * Go-trial omissions and wrong answers are untouched: those still mean what they always meant.
     */
    private fun updateErrorStreak(trial: RecordedEmotionTrial) {
        val outcome = EmotionTrialClassifier.classify(trial)
        val payload = trial.stimulus.payload as? EmotionStimulusPayload.AffectiveGoNoGo
        if (payload?.stopAfterMs != null) {
            consecutiveFailedStops =
                if (outcome == EmotionTrialOutcome.FALSE_ALARM) consecutiveFailedStops + 1 else 0
            if (consecutiveFailedStops >= FAILED_STOP_RUN_SIGNAL) consecutiveErrors = consecutiveFailedStops
        } else {
            val isError = outcome == EmotionTrialOutcome.INCORRECT ||
                outcome == EmotionTrialOutcome.FALSE_ALARM ||
                outcome == EmotionTrialOutcome.MISS
            consecutiveErrors = if (isError) consecutiveErrors + 1 else 0
        }
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
            recordExposureBestEffort()
            recordSyncBestEffort(ended, outcome.summary)
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
            // MC8u-B+ §B/§E: derive the per-session quality (governing-confident + anomaly flags) and stamp it + the
            // scoring/exercise versions onto the durable summary, so the calibration gate can withhold a low-confidence
            // or anomalous session from the 5-valid count. Uses the SAME classification authority as the assembler.
            val classified = allTrials.map { ClassifiedEmotionTrial(it, EmotionTrialClassifier.classify(it)) }
            val quality = EmotionSessionQualityEvaluator.evaluate(classified, finalResult.metrics, definition.governingMetric)
            // MC8u-C §2/§3: explicit counts/RT stats + the 0–100 training-performance block score (null when the
            // governing metric is not confident → CALIBRATING, never a fabricated number).
            val stats = EmotionSessionStatsCalculator.from(classified)
            val blockScore = EmotionBlockScore.of(finalResult.metrics[definition.governingMetric], directionOf(definition.governingMetric))
            val summary = buildSummary(ended, finalResult, activeDurationMs, quality, stats, blockScore)
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
                val validSessions = aggregate?.sessionsCompleted ?: 0
                if (ended != SessionEnd.ABANDONED) {
                    // MC8u-B §3: `counted` is the single PRD authority (valid identity + real completion + ≥120s + quality
                    // gate), so the result never claims a short/safety-stopped/low-confidence session calibrated.
                    // `reducePressure` is the honest "intensity/pressure was lowered" flag (intensity floors at NORMAL).
                    val counted = EmotionTerminalSemantics.isValidForAggregate(summary)
                    val result = resultFor(ended, finalResult, tier, validSessions, counted, reducePressure)
                    uiState = result
                    emitTerminalAnalytics(ended, quality, counted, validSessions, result.feedback.nextRecommendedExerciseId)
                } else {
                    emitAnalytics(EmotionAnalyticsEvent.SessionAbandoned(exerciseId.name))
                }
            } else if (ended != SessionEnd.ABANDONED) {
                // Summary not durable. If the PENDING persisted, the canonical progress is retained for a retry →
                // neutral leave (recovery finishes it). If NEITHER persisted, the terminal is NOT durable: show an
                // explicit, honest SaveFailed — never a neutral leave sold as recoverable (MC8h P0/P1 invariant 1).
                // MC8u-D §3/§7: a total save failure is observable telemetry (never silent progress loss).
                if (!pendingSaved) emitAnalytics(EmotionAnalyticsEvent.SessionSaveFailed(exerciseId.name))
                uiState = if (pendingSaved) EmotionSessionUiState.Exited else EmotionSessionUiState.SaveFailed
            }
            TerminalOutcome(summarySaved, activeDurationMs, terminalAtMs, aggregate, summary)
        }
    }

    private fun buildSummary(
        ended: SessionEnd,
        finalResult: EmotionBlockResult,
        activeDurationMs: Long,
        quality: EmotionSessionQuality,
        stats: EmotionSessionStats,
        normalizedScore: Int?,
    ): EmotionSessionSummaryDto =
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
            governingConfident = quality.governingConfident,
            anomalyFlags = quality.anomalyFlags,
            scoringVersion = EmotionScoringVersions.SCORING,
            exerciseVersion = EmotionScoringVersions.exerciseVersion(exerciseId.name),
            difficultyStart = difficultyStartLevel,
            totalAnswers = stats.totalAnswers,
            correctAnswers = stats.correctAnswers,
            incorrectAnswers = stats.incorrectAnswers,
            omissions = stats.omissions,
            accuracy = stats.accuracy,
            averageReactionTimeMs = stats.averageReactionTimeMs,
            bestReactionTimeMs = stats.bestReactionTimeMs,
            worstReactionTimeMs = stats.worstReactionTimeMs,
            reactionTimeVariability = stats.reactionTimeVariability,
            normalizedScore = normalizedScore,
            // MC8u-D §2/§6: explicit sync posture. LOCAL_ONLY = durable on-device now, no cloud copy (charter default);
            // a future sync layer flips it. `platform` is injected from the graph (UNKNOWN until the app sets it).
            platform = graph.platform,
            syncStatus = EmotionSyncStatus.LOCAL_ONLY,
            holdoutProbe = probeSummary(),
            pressureCells = pressureCellsSummary(),
            // Ola 1 · N1: what the transversal calibration layer captured, as per-bucket counts. Null when the
            // session carried nothing (a go/no-go host, or a user who never used the strip) — absent, not zero.
            calibration = EmotionCalibrationSummaries.summarize(sessionCalibration, sessionBlockForecasts),
        )

    private fun resultFor(
        ended: SessionEnd,
        finalResult: EmotionBlockResult,
        tier: ProfileTier,
        validSessions: Int,
        counted: Boolean,
        intensityReduced: Boolean,
    ): EmotionSessionUiState.Result {
        val stoppedForSafety = ended == SessionEnd.SAFETY_STOP
        return EmotionSessionUiState.Result(
            exerciseName = definition.displayName,
            feedback = graph.feedbackBuilder.build(
                definition,
                finalResult.metrics[definition.governingMetric],
                tier,
                stoppedForSafety = stoppedForSafety,
                // Ola 2 · R3 · P4: the matrix was assembled every session and read by nobody. It is the whole
                // session's (assembleBlock runs over `allTrials`), so the pattern rests on the full sample.
                confusion = finalResult.confusion,
            ),
            stoppedForSafety = stoppedForSafety,
            validSessions = validSessions,
            counted = counted && !stoppedForSafety,
            intensityReduced = intensityReduced,
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
        emitAnalytics(EmotionAnalyticsEvent.SafetyEventTriggered(exerciseId.name, type.name))
    }

    private fun keyFor(category: EmotionCategory): EmotionResponseKey = when (category) {
        EmotionCategory.INTERNAL_STATE -> EmotionResponseKey.CATEGORY_A
        EmotionCategory.THOUGHT -> EmotionResponseKey.CATEGORY_B
        EmotionCategory.IMPULSE -> EmotionResponseKey.CATEGORY_C
        EmotionCategory.FACT -> EmotionResponseKey.CATEGORY_D
    }

    /** EM3/EM10 draw from the shared content bank, so they carry the cross-session exposure guard (MC8u-D §5). */
    private fun usesContentBank(): Boolean =
        exerciseId == EmotionExerciseId.EM3_SEPARADOR || exerciseId == EmotionExerciseId.EM10_SEP_PRESION

    /**
     * BEST-EFFORT: record the phrases shown this session so the NEXT session deprioritizes them (MC8u-D §5). Bounded
     * and isolated like the other terminal side-effects — a storage failure never affects the canonical terminal.
     * Only for content-bank exercises with a store that supports exposure tracking.
     */
    private suspend fun recordExposureBestEffort() {
        if (!usesContentBank() || sessionUsedPhrases.isEmpty()) return
        val exposureStore = graph.repository as? EmotionExposureStore ?: return
        withTimeoutOrNull(BEST_EFFORT_TIMEOUT_MS) {
            recoverableTerminal { exposureStore.recordExposedPhrases(userId, exerciseId.name, sessionUsedPhrases.toList()) }
        }
    }

    /**
     * BEST-EFFORT cloud sync (MC8u-E §3). With the charter default no-op sink this is a no-op (production stays
     * local-first). With a real [com.mindos.app.features.mindboost.emotions.data.dto.EmotionCloudSyncSink], a durable
     * summary is pushed; the persisted syncStatus flips from the push result (idempotent re-save by sessionId), and a
     * failed/absent push emits `emotion_sync_failed` and NEVER deletes local data. Bounded + isolated like the other
     * terminal side-effects. ABANDONED terminals are not pushed.
     */
    private suspend fun recordSyncBestEffort(ended: SessionEnd, summary: EmotionSessionSummaryDto) {
        if (ended == SessionEnd.ABANDONED || graph.cloudSync === NoOpEmotionCloudSync) return
        withTimeoutOrNull(BEST_EFFORT_TIMEOUT_MS) {
            val status = recoverableTerminal { graph.cloudSync.push(userId, summary) }.getOrNull()
            if (status == null || status == EmotionSyncStatus.SYNC_FAILED) {
                emitAnalytics(EmotionAnalyticsEvent.SyncFailed(exerciseId.name))
            } else if (status != summary.syncStatus) {
                recoverableTerminal { graph.repository.saveSessionSummary(userId, summary.copy(syncStatus = status)) }
            }
        }
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
        val summary: EmotionSessionSummaryDto,
    )

    internal companion object {
        /**
         * Ola 0 · S1 — THREE blocks, not two. The engine holds difficulty until `blocksSinceChange` reaches its
         * anti-oscillation threshold (`DefaultEmotionAdaptiveEngine.COOLDOWN_AFTER_DECISIONS`), and that counter can
         * only reach 0 then 1 across two blocks: `holdForHysteresis` precedes `stepUp` in the rule order, so with two
         * blocks the step-up rule was never once evaluated in a real session and difficulty could only ever fall.
         * A third block lets the counter reach the threshold before the final decision, so a session can raise
         * difficulty AT MOST once, on evidence gathered in that same session. The threshold itself is deliberately
         * untouched: it is the anti-oscillation guard, not the defect.
         */
        const val BLOCKS = 3

        /**
         * Ola 0 · S5 — the value passed for a safety signal the session cannot produce. Named so that the absence
         * is visible at the call site instead of reading as a measured zero. See the call site for why it is not
         * simply implemented, and [EmotionExitAttemptProxyTest] for the test that stops it drifting unnoticed.
         */
        /**
         * Ola 3 · R1 — consecutive failed inhibitions before a staircase run stops being the measurement working
         * and starts being an overload signal. Four, from the audit's R1 design: at the convergence point each
         * stop is a coin flip, so three in a row happens to roughly one stop-trial run in eight and would fire
         * on ordinary sessions.
         */
        const val FAILED_STOP_RUN_SIGNAL = 4

        /** Ola 3 · R2 — opening trials the response-time baseline is taken from (audit: the first 8 of block 0). */
        const val BASELINE_TRIALS = 8

        const val NO_EXIT_ATTEMPT_SIGNAL = 0
        const val BASE_WINDOW_MS = 2500L
        const val REDUCED_PRESSURE_FACTOR = 1.6
        const val NANOS_PER_MS = 1_000_000L
        const val BEST_EFFORT_TIMEOUT_MS = 3_000L // bounds the best-effort SessionRecord/aggregate I/O
        const val PROBE_INTER_STIMULUS_MS = 500L // matches the EM3/EM10 training gap: the probe must not feel different

    }
}
