package com.mindos.app.features.mindboost.focus.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.mindboost.focus.di.FocusGraph
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveConfig
import com.mindos.app.features.mindboost.focus.domain.adaptive.AdaptiveInput
import com.mindos.app.features.mindboost.focus.domain.adaptive.FatigueSignal
import com.mindos.app.features.mindboost.focus.domain.exercise.BlockSpec
import com.mindos.app.features.mindboost.focus.domain.exercise.E4FrenoGenerator
import com.mindos.app.features.mindboost.focus.domain.exercise.StopSignalTracker
import com.mindos.app.features.mindboost.focus.domain.exercise.TrialClassifier
import com.mindos.app.features.mindboost.focus.domain.model.BlockId
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
import com.mindos.app.features.mindboost.focus.domain.session.FocusSessionAssembler
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
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
@Suppress("TooManyFunctions")
class FocusSessionStateHolder(
    private val graph: FocusGraph,
    private val exerciseId: ExerciseId,
    private val userId: String,
    private val scope: CoroutineScope,
    private val seed: Long,
) {
    private val definition = graph.definitionFor(exerciseId)
    private val generator = graph.generatorFor(exerciseId)
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
        if (!pending.isCompleted) pending.complete(ResponseEvent(graph.monotonicClock.nowNanos(), key))
    }

    fun abort(reason: String = "Sesión interrumpida") {
        sessionJob?.cancel()
        sessionJob = null
        pendingResponse = null
        uiState = FocusSessionUiState.Aborted(reason)
    }

    private suspend fun runSession(template: SessionTemplate, subjectiveState: SubjectiveState?) {
        val profile = graph.profileRepository.load(userId) ?: UserCognitiveProfile(userId = userId)
        val setup = RunSetup(
            template = template,
            profile = profile,
            thresholds = profile.thresholds,
            difficulty = profile.startingDifficultyFor(definition.id),
            startedAtMs = graph.epochClock.nowMillis(),
            subjectiveState = subjectiveState,
        )
        resetCounters()

        val trialCount = template.estimatedTrialCount()
        emitRunning(FocusPhase.WARMUP, FocusFieldState.Idle, 0, trialCount)
        delay(WARMUP_COUNTDOWN_MS)

        val spec = BlockSpec(targetTrialCount = trialCount, mode = Mode.TRAINING)
        val stimuli = generator.generate(setup.difficulty, spec, seed)
        // E4 needs an ONLINE stop-signal-delay staircase (it depends on the user's responses, so it
        // is never pre-generated). Non-stop exercises leave this null.
        val stopTracker = (generator as? E4FrenoGenerator)?.tracker(setup.difficulty)
        val recorded = ArrayList<FocusSessionAssembler.RecordedTrial>(stimuli.size)

        stimuli.forEachIndexed { index, stimulus ->
            emitRunning(FocusPhase.CORE, FocusFieldState.Idle, index, trialCount)
            delay(stimulus.interStimulusDelayMs)

            val onsetNanos = graph.monotonicClock.nowNanos()
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

        finishSession(setup, recorded)
    }

    private suspend fun finishSession(setup: RunSetup, recorded: List<FocusSessionAssembler.RecordedTrial>) {
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

        val decision = graph.adaptiveEngine.decide(
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

        graph.historyRepository.append(session)
        val advancedProfile = setup.profile.copy(
            startingDifficulty = setup.profile.startingDifficulty +
                (definition.id to decision.nextDifficulty),
            totalSessions = setup.profile.totalSessions + 1,
            updatedAtMs = endedAtMs,
        )
        // Re-infer the (revisable) archetype from this session — hysteresis + one-step transition.
        val evolvedProfile = graph.profileEvolution.evolve(
            current = advancedProfile,
            newData = listOf(session),
            retest = null,
        )
        graph.profileRepository.save(evolvedProfile)

        uiState = FocusSessionUiState.Finished(graph.assembler.buildReport(session), setup.template)
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
