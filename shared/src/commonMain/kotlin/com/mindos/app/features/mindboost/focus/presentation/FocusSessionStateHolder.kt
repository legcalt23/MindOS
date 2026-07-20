package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.core.config.FeatureFlagsHub
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecordUpsertOutcome
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveConfig
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveDecision
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveInput
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveResult
import com.mindos.app.features.mindboost.focus.domain.adaptive.FatigueSignal
import com.mindos.app.features.mindboost.focus.domain.adaptive.HoldReason
import com.mindos.app.features.mindboost.focus.domain.engine.FocusAdaptationPolicy
import com.mindos.app.features.mindboost.focus.domain.engine.FocusEngine
import com.mindos.app.features.mindboost.focus.domain.engine.CreateFocusSessionRequest
import com.mindos.app.features.mindboost.focus.domain.engine.FocusSessionMode
import com.mindos.app.features.mindboost.focus.domain.engine.FocusSessionPlan
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E4FrenoGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.StopSignalTracker
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialClassifier
import com.mindos.app.features.mindboost.focus.domain.exercise.VersionedTrialGenerator
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
import com.mindos.app.features.mindboost.focus.domain.model.BlockResult
import com.mindos.app.features.mindboost.focus.domain.model.DifficultyVector
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseDefinition
import com.mindos.app.features.mindboost.focus.domain.model.ExerciseId
import com.mindos.app.features.mindboost.focus.domain.model.IndividualThresholds
import com.mindos.app.features.mindboost.focus.domain.model.Mode
import com.mindos.app.features.mindboost.focus.domain.model.Paradigm
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.domain.model.SessionId
import com.mindos.app.features.mindboost.focus.domain.model.Stimulus
import com.mindos.app.features.mindboost.focus.domain.model.StimulusPayload
import com.mindos.app.features.mindboost.focus.domain.model.SubjectiveState
import com.mindos.app.features.mindboost.focus.domain.model.TaskRule
import com.mindos.app.features.mindboost.focus.domain.model.TrialOutcome
import com.mindos.app.features.mindboost.focus.domain.model.UserCognitiveProfile
import com.mindos.app.features.mindboost.focus.domain.personalization.ArchetypePersonalization
import com.mindos.app.features.mindboost.focus.domain.personalization.PersonalizationProfiles
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.exercise.MicroPauseGate
import com.mindos.app.features.mindboost.focus.presentation.state.ChoiceParadigm
import com.mindos.app.features.mindboost.focus.presentation.state.FeedbackKind
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import com.mindos.app.features.mindboost.focus.presentation.state.FocusPhase
import com.mindos.app.features.mindboost.focus.presentation.state.FocusRunningModel
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Drives one focus session in real time for the exercise selected by [exerciseId]
 * (the generator and definition are resolved from the [graph] registry): presents
 * stimuli with monotonic timing, captures taps, and hands the raw timing to the pure
 * [FocusSessionAssembler] for classification, metrics and adaptation. The holder
 * ORCHESTRATES; it never computes metrics itself (05_/09_ separation).
 *
 * Supports single-tap (E1 Vigilancia, E3 go/no-go) and two-choice LEFT/RIGHT (E2 Filtro,
 * E5 Conflicto) paradigms; the catalogue's `implemented` set gates which ids ever reach here.
 *
 * Reaction time is always `tapNanos - onsetNanos` from the graph's monotonic
 * clock; wall-clock is used only for persistence ordering.
 *
 * TooManyFunctions is suppressed deliberately: this is a single cohesive
 * real-time orchestrator whose private helpers (timing, classification, counters,
 * emission) read more clearly split than inlined into one long method.
 */
@Suppress("TooManyFunctions", "LongParameterList")
class FocusSessionStateHolder(
    private val graph: FocusGraph,
    private val exerciseId: ExerciseId,
    private val userId: String,
    private val scope: CoroutineScope,
    private val recorder: SessionRecorder = NoOpSessionRecorder,
    private val sessionIdGenerator: SessionIdGenerator = RandomSessionIdGenerator(),
    /**
     * S0 containment gate for the v1 score AND for the archetype route it feeds (`MB-ENG-0007`/`MB-ENG-0008`).
     *
     * The archetype is inferred from the very metrics the v1 score mis-weights (`ArchetypeInference` consumes
     * `RT_CV` and `LAPSES`, with `faScore` double-weighted), so a key-masher is classified IMPULSIVE and that
     * classification then steers the NEXT session through three further doors. While this reads `false` all
     * three are shut: (b) the archetype-biased starting difficulty, (c) the micro-pause input gate, and
     * (d) the archetype's scoring modifiers — which are moot anyway because no score is computed.
     *
     * Route (a) — a starting difficulty ALREADY persisted from a real prior session — is deliberately left
     * intact: it is the user's own empirical value, not an inference, and discarding it would destroy
     * legitimate history. Route (e), the legacy `AdaptiveEngine`, is out of S0 scope (TRAINING_ONLY).
     */
    private val scoreSurfacesEnabled: () -> Boolean = { FeatureFlagsHub.current.focusScoreSurfacesEnabled },
) {
    private val definition = graph.definitionFor(exerciseId)
    /**
     * The UNMIGRATED generator, or null for a migrated exercise. Null for E2 by construction, not by accident:
     * a versioned generator is not a [TrialGenerator], so it cannot be in the legacy registry at all. Resolving
     * it eagerly with `generatorFor` used to throw for E2 the moment the two registries were separated — which
     * is the type system reporting, correctly, that E2 has no legacy route to resolve.
     */
    private val legacyGenerator = graph.generators[exerciseId]

    /**
     * Non-null exactly for the exercises migrated to [FocusEngine] (ENG-01: E2 only). The branch below keys on
     * THIS, not on an exercise id: a migrated exercise cannot fall back into the unversioned path by mistake,
     * because the type says it has a versioned one.
     */
    private val versionedGenerator = graph.versionedGenerators[exerciseId]
    private val instructions = instructionsFor(definition)

    var uiState: FocusSessionUiState by mutableStateOf(
        FocusSessionUiState.Intro(
            exerciseName = definition.displayName,
            exerciseDescription = definition.plainDescription,
            template = SessionTemplate.MAINTENANCE_5,
            instructions = instructions,
        ),
    )
        private set

    private var sessionJob: Job? = null
    private var pendingResponse: CompletableDeferred<ResponseEvent>? = null

    // Live counters (main-thread only, mutated inside the session coroutine).
    private var hits = 0
    private var lapses = 0
    private var falseAlarms = 0
    private var lastReactionMs: Long? = null

    // Impulsive micro-pause (TRAINING only): the current trial's onset and the gate window for this run.
    private var currentOnsetNanos = 0L
    private var microPauseWindowMs = 0L

    // Write-ahead session record (MC7): serialized lifecycle over mindboost_sessions. Opaque id (separate from
    // the internal "$userId:$endedAtMs" id) and finalized flag — no trial/stimulus/score content. The STARTED
    // write is now AWAITED inline rather than launched (ENG-01): the identity must be durable before trial 1,
    // so there is no background job left to join.
    private val recorderMutex = Mutex()
    private var recorderSessionId: String = ""
    private var recorderStartedAtMs: Long = 0L
    private var recorderFinalized: Boolean = false

    fun prepare(template: SessionTemplate) {
        uiState = FocusSessionUiState.Intro(
            exerciseName = definition.displayName,
            exerciseDescription = definition.plainDescription,
            template = template,
            instructions = instructions,
        )
    }

    fun start(template: SessionTemplate, subjectiveState: SubjectiveState? = null) {
        if (sessionJob?.isActive == true) return
        sessionJob = scope.launch { runSession(template, subjectiveState) }
    }

    /** UI calls this for a single-tap response (E1/E3); equivalent to the PRIMARY key. */
    fun onTap() = onResponse(ResponseKey.PRIMARY)

    /** UI calls this with the chosen key: PRIMARY for tap, LEFT/RIGHT for two-choice. */
    fun onResponse(key: ResponseKey) {
        val pending = pendingResponse ?: return
        val now = graph.monotonicClock.nowNanos()
        // Complete only if not already done AND the impulsive micro-pause window has elapsed (no-op when 0):
        // taps that arrive before the gate are swallowed so a precipitous response cannot register.
        if (!pending.isCompleted && MicroPauseGate.accepts(currentOnsetNanos, now, microPauseWindowMs)) {
            pending.complete(ResponseEvent(now, key))
        }
    }

    /**
     * Explicit, completion-aware abandonment. Stops the active session, then — if a real session STARTED and did
     * NOT finalize — awaits its STARTED and marks the SessionRecord ABANDONED under the mutex BEFORE exposing the
     * terminal Aborted state. A pre-session abort has no sessionId → no artificial ABANDONED; a finalized session
     * is never abandoned. Cancellation during onAbandon never falsely exposes Aborted; recovery is the backstop.
     * Only the opaque sessionId reaches the recorder — no focus content.
     */
    suspend fun abortAndAwait(reason: String = "Sesión interrumpida") {
        sessionJob?.cancel()
        sessionJob = null
        pendingResponse = null
        // recorderSessionId is only published AFTER STARTED is durably written, so a non-empty id here means the
        // row provably exists and can be honestly marked ABANDONED. An abort racing the STARTED write sees an
        // empty id and marks nothing — correct, since there is no session yet; any orphan row that write leaves
        // behind is reconciled by SessionRecoveryScanner.
        val sessionId = recorderSessionId
        if (sessionId.isNotEmpty() && !recorderFinalized) {
            recorderMutex.withLock { recorder.onAbandon(sessionId) }
        }
        uiState = FocusSessionUiState.Aborted(reason)
    }

    private suspend fun runSession(template: SessionTemplate, subjectiveState: SubjectiveState?) {
        val profile = graph.profileRepository.load(userId) ?: UserCognitiveProfile(userId = userId)
        val archetypeRoutesOpen = scoreSurfacesEnabled()
        val setup = RunSetup(
            template = template,
            profile = profile,
            thresholds = profile.thresholds,
            difficulty = initialDifficultyFor(profile, archetypeRoutesOpen),
            startedAtMs = graph.epochClock.nowMillis(),
            subjectiveState = subjectiveState,
        )
        microPauseWindowMs = microPauseWindowFor(profile, archetypeRoutesOpen)
        resetCounters()

        // Real session begins — write-ahead STARTED (opaque id, FOCUS, one planned round). Serialized under the
        // mutex; the job is kept so finalize/abandon can join it. No stimuli, trials, RTs, or scores reach the
        // recorder. The opaque id is separate from the internal "$userId:$endedAtMs" id built at finish.
        val startedAtMs = setup.startedAtMs
        val sessionId = sessionIdGenerator.newSessionId()
        recorderStartedAtMs = startedAtMs

        val trialCount = template.estimatedTrialCount()
        val spec = BlockSpec(targetTrialCount = trialCount, mode = Mode.TRAINING)
        val plan = planFor(setup.difficulty, spec)

        // The identity goes into the SAME write as STARTED, and that write is AWAITED here — not launched into
        // the background as before. A trial presented under an identity that was never written is unreproducible
        // forever, and no later write can recover it, so the engine route fails closed: no durable identity, no
        // session. The legacy route (plan == null) keeps its best-effort behaviour and records no identity.
        if (!openSession(sessionId, startedAtMs, plan)) {
            uiState = FocusSessionUiState.Aborted(IDENTITY_NOT_DURABLE_MESSAGE)
            return
        }

        emitRunning(FocusPhase.WARMUP, FocusFieldState.Idle, 0, trialCount)
        delay(WARMUP_COUNTDOWN_MS)

        val stimuli = plan?.stimuli ?: legacyForm(setup.difficulty, spec)
        // E4 needs an ONLINE stop-signal-delay staircase (it depends on the user's responses, so it
        // is never pre-generated). Non-stop exercises leave this null.
        val stopTracker = (legacyGenerator as? E4FrenoGenerator)?.tracker(setup.difficulty)
        val recorded = ArrayList<FocusSessionAssembler.RecordedTrial>(stimuli.size)

        stimuli.forEachIndexed { index, stimulus ->
            emitRunning(FocusPhase.CORE, FocusFieldState.Idle, index, trialCount)
            delay(stimulus.interStimulusDelayMs)

            val onsetNanos = graph.monotonicClock.nowNanos()
            currentOnsetNanos = onsetNanos
            val presented = presentAndAwait(stimulus, index, trialCount, stopTracker)
            val latencyMs = presented.event
                ?.let { ((it.monotonicNs - onsetNanos) / NANOS_PER_MS).coerceAtLeast(0) }
            recorded += FocusSessionAssembler.RecordedTrial(
                stimulus = stimulus,
                responseLatencyMs = latencyMs,
                responseKey = presented.event?.key,
                stopSignalDelayMs = presented.ssdUsed,
            )

            // Drive the staircase: a withheld stop (no response) is a success → SSD goes up next time.
            if (stimulus.requiresStop) stopTracker?.onStopOutcome(successfulStop = latencyMs == null)

            val classification = classifyTrial(stimulus, latencyMs, presented.event?.key, setup.thresholds)
            applyCounters(classification.outcome, latencyMs, setup.thresholds)
            emitRunning(
                FocusPhase.CORE,
                FocusFieldState.Feedback(feedbackFor(classification.outcome)),
                index + 1, trialCount,
            )
            delay(FEEDBACK_MS)
        }

        emitRunning(FocusPhase.COOLDOWN, FocusFieldState.Idle, trialCount, trialCount)
        delay(COOLDOWN_MS)

        finishSession(setup, plan, recorded)
    }

    /**
     * Writes STARTED and the session's identity as ONE durable row, and only then publishes the session id.
     *
     * Returns false when the engine route could not persist — the caller must then abort. The legacy route
     * (null [plan]) keeps its best-effort contract: it records no identity, so a failed write degrades it no
     * further than it already is, and it returns true.
     */
    /**
     * The session's starting difficulty. Normally the user's calibrated/adapted value, or an
     * archetype-biased default when none is stored yet (e.g. impulsive starts an inhibition exercise with
     * inhibitoryDemand+, 08_ §2).
     *
     * S0 route (b): while the score surfaces are contained, an ABSENT stored value falls back to EASIEST
     * instead of the biased default, so an archetype inferred from the contaminated v1 metrics cannot set
     * the difficulty of the next session (`MB-ENG-0008`). A STORED value still wins in both modes — that
     * is route (a), the user's own empirical value, which containment deliberately preserves.
     */
    private fun initialDifficultyFor(
        profile: UserCognitiveProfile,
        archetypeRoutesOpen: Boolean,
    ): DifficultyVector = if (archetypeRoutesOpen) {
        ArchetypePersonalization.startingDifficulty(profile, definition)
    } else {
        profile.startingDifficulty[definition.id] ?: DifficultyVector.EASIEST
    }

    /**
     * The pre-response input gate. Impulsive archetypes get a brief anti-precipitation window; others 0.
     *
     * S0 route (c): while contained there is no gate at all, so input handling never depends on an
     * archetype derived from the v1 metrics.
     */
    private fun microPauseWindowFor(profile: UserCognitiveProfile, archetypeRoutesOpen: Boolean): Long =
        if (archetypeRoutesOpen) {
            MicroPauseGate.windowMs(
                PersonalizationProfiles.forArchetype(profile.archetype).scoringModifiers.prePressureMicroPause,
            )
        } else {
            0L
        }

    private suspend fun openSession(sessionId: String, startedAtMs: Long, plan: FocusSessionPlan?): Boolean {
        val started = recorderMutex.withLock {
            recorder.onStart(sessionId, ScoreModuleId.FOCUS, startedAtMs, plannedTrials = 1, plan?.identity)
        }
        if (plan != null && started !is SessionRecordUpsertOutcome.Stored) return false
        // Only NOW does the session exist for abort/finalize purposes: publishing the id before the row was
        // durable would let abortAndAwait try to ABANDON a session that was never STARTED. An abort racing this
        // window leaves an orphan STARTED row, which SessionRecoveryScanner reconciles — the documented backstop.
        recorderSessionId = sessionId
        return true
    }

    /**
     * The versioned route (ENG-01): E2 goes through [FocusEngine], which is the only thing allowed to build its
     * form. Null for the twelve exercises not yet migrated — they are not silently degraded here, they simply
     * have no engine route yet. Their form is drawn from a minted-but-unpersisted seed, so it is honestly
     * NON-REPRODUCIBLE — which is the pre-existing state, not a regression this slice introduces.
     */
    private fun planFor(difficulty: DifficultyVector, spec: BlockSpec): FocusSessionPlan? {
        versionedGenerator ?: return null
        return graph.focusEngine.createSession(
            CreateFocusSessionRequest(
                exerciseId = exerciseId,
                difficulty = difficulty,
                spec = spec,
                sessionTemplateVersion = SessionTemplate.TEMPLATE_VERSION,
                // Provisional safe mode: real training, no measurement, no official scoring, fixed difficulty.
                mode = FocusSessionMode.TRAINING_ONLY,
            ),
        )
    }

    /**
     * The unversioned path the twelve unmigrated exercises still use.
     *
     * Unreachable for a migrated exercise, and now for a stronger reason than "the caller checks first": a
     * migrated generator is not a [com.mindos.app.features.mindboost.focus.domain.exercise.TrialGenerator], so
     * it is not in [FocusGraph.generators] and this function has nothing it could even call for E2.
     */
    @Suppress("DEPRECATION")
    private fun legacyForm(difficulty: DifficultyVector, spec: BlockSpec): List<Stimulus> {
        val generator = requireNotNull(legacyGenerator) {
            "$exerciseId has neither a versioned generator nor a legacy one, so it is exposed by the catalogue " +
                "with no runnable path. ExerciseCatalogConsistencyTest exists to make that unreachable."
        }
        return generator.generate(difficulty, spec, graph.seedSource.mint().value)
    }

    private suspend fun finishSession(
        setup: RunSetup,
        plan: FocusSessionPlan?,
        recorded: List<FocusSessionAssembler.RecordedTrial>,
    ) {
        val endedAtMs = graph.epochClock.nowMillis()
        val sessionIdValue = "$userId:$endedAtMs"
        val block = graph.assembler.assembleBlock(
            blockId = BlockId("$sessionIdValue:b0"),
            definition = definition,
            mode = Mode.TRAINING,
            difficultyStart = setup.difficulty,
            difficultyEnd = setup.difficulty,
            thresholds = setup.thresholds,
            recorded = recorded,
            sessionIdPrefix = userId,
            epochMillis = endedAtMs,
        )
        val session = graph.assembler.assembleSession(
            sessionId = SessionId(sessionIdValue),
            mode = Mode.TRAINING,
            definition = definition,
            blocks = listOf(block),
            startedAtMs = setup.startedAtMs,
            endedAtMs = endedAtMs,
            subjectiveState = setup.subjectiveState,
        )

        val decision = decideAdaptation(plan, setup, block)

        graph.historyRepository.append(session)
        val advancedProfile = setup.profile.copy(
            startingDifficulty = setup.profile.startingDifficulty +
                (definition.id to decision.nextDifficulty),
            totalSessions = setup.profile.totalSessions + 1,
            updatedAtMs = endedAtMs,
        )
        // Re-infer the (revisable) archetype from this session — hysteresis + one-step transition.
        // S0 (producer side of MB-ENG-0008): `ArchetypeInference` consumes RT_CV and LAPSES — the two
        // contaminated inputs — so while contained the re-inference does NOT run and no new archetype is
        // persisted. `advancedProfile` is still saved, because its other updates (the adapted starting
        // difficulty, the session count) are legitimate empirical data that must not be lost. Gating the
        // producer alone would be insufficient — an archetype persisted by an earlier build survives it —
        // which is why every CONSUMER is gated too (session setup here, and the daily planner).
        val profileToSave = if (scoreSurfacesEnabled()) {
            graph.profileEvolution.evolve(
                current = advancedProfile,
                newData = listOf(session),
                retest = null,
            )
        } else {
            advancedProfile
        }
        graph.profileRepository.save(profileToSave)

        // Score this session through the archetype's modifiers active during it (pre-session profile):
        // impulsive/fast-inaccurate are not rewarded for haste; lapses/errors weigh per the profile.
        // S0 route (d) + surface 3: while contained the score is NOT COMPUTED AT ALL, so no v1 number
        // exists to leak into the report, and the archetype's scoring modifiers are never applied. The
        // score card simply has nothing to render, because `personalizedScore` stays null.
        //
        // The summary's per-session metric cards are OUT of S0's scope by the plan ("el resto de
        // FocusSummaryScreen NO se toca") and are deliberately NOT described here as honest descriptive
        // counts — that would be an overclaim. At least one of them is not: "Lapsos" counts responses past
        // a PVT-derived 500 ms threshold applied to a 900-1500 ms window, so an honest 400-650 ms
        // responder is still shown a non-zero count under a label that asserts an attention failure. A
        // count is descriptively honest only when its counting rule fits the task; this one does not.
        // Tracked as `MB-FOCUS-0008` (declared debt, not a deviation).
        val personalized = if (scoreSurfacesEnabled()) {
            val modifiers = PersonalizationProfiles.forArchetype(setup.profile.archetype).scoringModifiers
            graph.personalizedScorer.score(session.sessionMetrics, modifiers)
        } else {
            null
        }
        val report = graph.assembler.buildReport(session).copy(personalizedScore = personalized)
        // Persist FINALIZED after the block's own persistence (history + profile) and BEFORE exposing Finished.
        // STARTED was already awaited before trial 1, so there is nothing to join. Counts only — never trial
        // content, and never the identity again: FINALIZED preserves the row's existing one.
        val activeDurationMs = endedAtMs - recorderStartedAtMs
        recorderMutex.withLock {
            recorder.onFinalize(recorderSessionId, completedTrials = 1, activeDurationMs = activeDurationMs)
        }
        recorderFinalized = true
        uiState = FocusSessionUiState.Finished(report, setup.template)
    }

    /**
     * Honours the plan's adaptation policy. A plan that says FIXED means FIXED: the adaptive engine is not
     * consulted at all, so no rule — including the ones still open (T3's unguarded members, `Cooldown`,
     * `ContainImpulsivity`) — can move difficulty on the engine route while that work is unfinished.
     *
     * Exhaustive on the nullable policy: a new [FocusAdaptationPolicy] cannot be added without deciding here,
     * and null is the honest "this exercise has no engine route yet" (the twelve unmigrated ones), which keeps
     * their existing behaviour untouched.
     */
    private fun decideAdaptation(
        plan: FocusSessionPlan?,
        setup: RunSetup,
        block: BlockResult,
    ): AdaptiveResult = when (plan?.adaptation) {
        FocusAdaptationPolicy.FIXED -> AdaptiveResult(
            decision = AdaptiveDecision.Hold(HoldReason.FIXED_DIFFICULTY_MODE),
            nextDifficulty = setup.difficulty,
            rationale = "Dificultad fija: esta ruta no adapta todavía.",
        )
        null -> graph.adaptiveEngine.decide(
            AdaptiveInput(
                exercise = definition,
                current = setup.difficulty,
                recentBlock = block.metrics,
                recentTrials = block.trials,
                perAxisMastery = emptyMap(),
                fatigue = FatigueSignal.NONE,
                blocksSinceChange = Int.MAX_VALUE,
                config = AdaptiveConfig(),
            ),
        )
    }

    private suspend fun awaitResponse(windowMs: Long): ResponseEvent? {
        val deferred = CompletableDeferred<ResponseEvent>()
        pendingResponse = deferred
        val result = withTimeoutOrNull(windowMs) { deferred.await() }
        pendingResponse = null
        return result
    }

    /**
     * Presents [stimulus] and awaits the response. Stop-signal (E4) trials show the GO target, then
     * — if not yet answered — flip to a STOP cue after the online SSD; the returned [PresentResult]
     * carries the SSD actually used so the assembler can attribute it. Other paradigms show their
     * field for the whole response window.
     */
    private suspend fun presentAndAwait(
        stimulus: Stimulus,
        index: Int,
        trialCount: Int,
        stopTracker: StopSignalTracker?,
    ): PresentResult {
        if (stimulus.requiresStop && stopTracker != null) {
            return presentStopTrial(stimulus, index, trialCount, stopTracker)
        }
        val choiceKey = stimulus.correctResponseKey?.takeIf { it.isDirectional }
        val field = if (choiceKey != null) choiceFieldFor(stimulus) else fieldFor(stimulus)
        emitRunning(FocusPhase.CORE, field, index, trialCount)
        return PresentResult(awaitResponse(effectiveWindowMs(stimulus)), ssdUsed = null)
    }

    /**
     * E13 enforces the deadline as the response window on pressure trials (any captured response is
     * then within deadline ≤ window, so the assembler's window-based classification stays consistent).
     * All other trials use the full response window.
     */
    private fun effectiveWindowMs(stimulus: Stimulus): Long {
        val payload = stimulus.payload
        return if (payload is StimulusPayload.Pressure && payload.underPressure && payload.deadlineMs > 0L) {
            payload.deadlineMs.coerceAtMost(stimulus.responseWindowMs)
        } else {
            stimulus.responseWindowMs
        }
    }

    /**
     * Stop-signal (E4) presentation: show the GO target, then — if not yet answered — flip to a STOP
     * cue after the online SSD. Returns the response (or null) and the SSD actually used.
     */
    private suspend fun presentStopTrial(
        stimulus: Stimulus,
        index: Int,
        trialCount: Int,
        stopTracker: StopSignalTracker,
    ): PresentResult {
        val ssd = stopTracker.currentSsdMs
        emitRunning(FocusPhase.CORE, fieldFor(stimulus), index, trialCount) // GO target
        val goPhaseMs = ssd.coerceIn(0L, stimulus.responseWindowMs)
        val early = awaitResponse(goPhaseMs)
        if (early != null) return PresentResult(early, ssd) // responded before the stop cue
        emitRunning(FocusPhase.CORE, FocusFieldState.Stop, index, trialCount) // STOP cue
        val remainingMs = (stimulus.responseWindowMs - goPhaseMs).coerceAtLeast(0L)
        return PresentResult(awaitResponse(remainingMs), ssd)
    }

    /** Classifies a trial by paradigm: stop-signal (E4), two-choice (E2/E5), or tap (E1/E3/E12). */
    private fun classifyTrial(
        stimulus: Stimulus,
        latencyMs: Long?,
        responseKey: ResponseKey?,
        thresholds: IndividualThresholds,
    ): TrialClassifier.Classification {
        val choiceKey = stimulus.correctResponseKey?.takeIf { it.isDirectional }
        return when {
            stimulus.requiresStop ->
                TrialClassifier.classifyStop(latencyMs, stimulus.responseWindowMs)
            choiceKey != null ->
                TrialClassifier.classifyChoice(
                    correctKey = choiceKey,
                    responseKey = responseKey,
                    responseLatencyMs = latencyMs,
                    anticipationFloorMs = thresholds.anticipationFloorMs,
                    responseWindowMs = stimulus.responseWindowMs,
                )
            else ->
                TrialClassifier.classify(
                    isTarget = stimulus.isTarget,
                    responseLatencyMs = latencyMs,
                    anticipationFloorMs = thresholds.anticipationFloorMs,
                    responseWindowMs = stimulus.responseWindowMs,
                )
        }
    }

    private fun fieldFor(stimulus: Stimulus): FocusFieldState = when (val payload = stimulus.payload) {
        is StimulusPayload.Search ->
            FocusFieldState.Search(payload.setSize, payload.targetPresent, stimulus.index)
        is StimulusPayload.MultiObjectTracking ->
            FocusFieldState.Tracking(
                objectCount = payload.objectCount,
                targetIndices = payload.targetIndices,
                speed = payload.speed,
                probedIsTarget = stimulus.isTarget,
                layoutSeed = stimulus.index,
            )
        is StimulusPayload.RsvpStream ->
            FocusFieldState.Stream(payload.length, payload.t1Index, payload.t2Index, stimulus.index)
        is StimulusPayload.DualTask ->
            FocusFieldState.Dual(go = stimulus.isTarget, dual = payload.dual)
        is StimulusPayload.Vigilance ->
            if (stimulus.isTarget) {
                FocusFieldState.Signal(payload.contrast.toFloat())
            } else {
                FocusFieldState.Lure(payload.contrast.toFloat())
            }
        else ->
            if (stimulus.isTarget) FocusFieldState.Signal(1f) else FocusFieldState.Lure(1f)
    }

    /** Builds the two-choice field from the stimulus payload (flanker E2 / Simon-style E5). */
    private fun choiceFieldFor(stimulus: Stimulus): FocusFieldState = when (val p = stimulus.payload) {
        is StimulusPayload.Flanker ->
            FocusFieldState.Choice(p.centerDirection, p.congruent, p.flankerCount, ChoiceParadigm.FLANKER)
        is StimulusPayload.Conflict ->
            FocusFieldState.Choice(p.relevantResponse, p.congruent, 0, ChoiceParadigm.CONFLICT)
        is StimulusPayload.Pressure ->
            FocusFieldState.Choice(
                targetDirection = p.correctResponse,
                congruent = true,
                distractorCount = 0,
                paradigm = ChoiceParadigm.DEADLINE,
                underPressure = p.underPressure,
            )
        is StimulusPayload.TaskSwitch ->
            FocusFieldState.RuleSwitch(
                colorIsRed = p.colorIsRed,
                shapeIsCircle = p.shapeIsCircle,
                ruleIsColor = p.rule == TaskRule.RULE_A,
            )
        is StimulusPayload.AnchorTask ->
            FocusFieldState.Anchor(targetDirection = p.mainResponse, intruderPresent = p.intruderPresent)
        else ->
            FocusFieldState.Choice(
                targetDirection = stimulus.correctResponseKey ?: ResponseKey.LEFT,
                congruent = true,
                distractorCount = 0,
                paradigm = ChoiceParadigm.CONFLICT,
            )
    }

    private fun feedbackFor(outcome: TrialOutcome): FeedbackKind = when (outcome) {
        TrialOutcome.HIT -> FeedbackKind.HIT
        TrialOutcome.MISS, TrialOutcome.TIMEOUT -> FeedbackKind.MISS
        TrialOutcome.FALSE_ALARM -> FeedbackKind.FALSE_ALARM
        TrialOutcome.CORRECT_REJECTION -> FeedbackKind.CORRECT_REJECTION
        TrialOutcome.ANTICIPATION -> FeedbackKind.TOO_EARLY
    }

    private fun applyCounters(outcome: TrialOutcome, latencyMs: Long?, thresholds: IndividualThresholds) {
        when (outcome) {
            TrialOutcome.HIT -> {
                hits++
                lastReactionMs = latencyMs
                if (latencyMs != null && latencyMs > thresholds.lapseRtMs) lapses++
            }
            TrialOutcome.FALSE_ALARM -> falseAlarms++
            else -> Unit
        }
    }

    private fun resetCounters() {
        hits = 0
        lapses = 0
        falseAlarms = 0
        lastReactionMs = null
    }

    private fun emitRunning(
        phase: FocusPhase,
        field: FocusFieldState,
        trialsDone: Int,
        trialsTotal: Int,
    ) {
        uiState = FocusSessionUiState.Running(
            FocusRunningModel(
                exerciseName = definition.displayName,
                phase = phase,
                field = field,
                progress = if (trialsTotal == 0) 0f else (trialsDone.toFloat() / trialsTotal),
                trialsDone = trialsDone,
                trialsTotal = trialsTotal,
                hits = hits,
                lapses = lapses,
                falseAlarms = falseAlarms,
                lastReactionMs = lastReactionMs,
            ),
        )
    }

    /** A captured response: its monotonic timestamp and which key the user pressed. */
    private data class ResponseEvent(val monotonicNs: Long, val key: ResponseKey)

    /** The outcome of presenting one stimulus: the response (or null) and the SSD used (E4 only). */
    private data class PresentResult(val event: ResponseEvent?, val ssdUsed: Long?)

    /** Immutable per-run setup, captured once so [finishSession] stays small. */
    private data class RunSetup(
        val template: SessionTemplate,
        val profile: UserCognitiveProfile,
        val thresholds: IndividualThresholds,
        val difficulty: DifficultyVector,
        val startedAtMs: Long,
        val subjectiveState: SubjectiveState?,
    )

    private companion object {
        /**
         * Shown when the engine route could not durably record the session's identity. It is an abort, not a
         * degraded run: a session whose form can never be reconstructed is worse than one that did not happen.
         */
        const val IDENTITY_NOT_DURABLE_MESSAGE = "No se pudo guardar la sesión. Inténtalo de nuevo."
        const val NANOS_PER_MS = 1_000_000L
        const val WARMUP_COUNTDOWN_MS = 4_000L
        const val COOLDOWN_MS = 2_500L
        const val FEEDBACK_MS = 320L
    }
}

/** Paradigm-appropriate how-to lines shown on the intro screen before a run. */
private fun instructionsFor(definition: ExerciseDefinition): List<String> = when (definition.paradigm) {
    Paradigm.FLANKER, Paradigm.STROOP -> listOf(
        "Aparecerá una flecha central: responde a su dirección.",
        "Pulsa la mitad izquierda o derecha de la pantalla.",
        "Ignora las flechas o la posición de alrededor.",
    )
    Paradigm.STOP_SIGNAL -> listOf(
        "Toca rápido en cada destello verde.",
        "Si el círculo se pone rojo, FRENA: no toques.",
    )
    Paradigm.DEADLINE_PRESSURE -> listOf(
        "Responde a la dirección de la flecha (izquierda/derecha).",
        "En los trials en rojo (¡RÁPIDO!) tienes menos tiempo: decide ya.",
    )
    Paradigm.VISUAL_SEARCH -> listOf(
        "Busca la L escondida entre las T.",
        "Toca si la L está presente; si no aparece, no toques.",
    )
    Paradigm.TASK_SWITCHING -> listOf(
        "Fíjate en la regla activa: COLOR o FORMA.",
        "COLOR → rojo: izquierda · azul: derecha.",
        "FORMA → círculo: izquierda · cuadrado: derecha.",
    )
    Paradigm.DIGITAL_DISTRACTION -> listOf(
        "Responde a la flecha (izquierda/derecha) sin parar.",
        "Si aparece una notificación, ignórala y sigue con la flecha.",
    )
    Paradigm.MOT -> listOf(
        "Memoriza los puntos verdes y NO los pierdas mientras se mueven.",
        "Al final, toca solo si el punto señalado era uno de ellos.",
    )
    Paradigm.RSVP -> listOf(
        "Verás una ráfaga rápida de letras.",
        "Toca en cuanto veas una letra objetivo (en color).",
    )
    Paradigm.DUAL_TASK -> listOf(
        "Toca con cada destello verde; ignora el círculo apagado.",
        "En los trials dobles, vigila además el contador de arriba.",
    )
    else -> listOf(
        "Toca cuando veas el destello verde.",
        "Ignora el violeta — no lo toques.",
    )
}
