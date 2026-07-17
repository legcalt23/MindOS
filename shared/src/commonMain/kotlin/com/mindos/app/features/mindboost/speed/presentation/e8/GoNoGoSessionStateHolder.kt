package com.mindos.app.features.mindboost.speed.presentation.e8

import com.mindos.app.core.logging.ObservabilityHub
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.scoring.TimingCriticality
import com.mindos.app.features.mindboost.core.session.SecureSessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.session.SessionRecordStore
import com.mindos.app.features.mindboost.core.session.cognitive.ClockSource
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRunStore
import com.mindos.app.features.mindboost.core.session.cognitive.SaveResult
import com.mindos.app.features.mindboost.core.session.cognitive.SessionCompletionStatus
import com.mindos.app.features.mindboost.core.session.cognitive.SessionPurpose
import com.mindos.app.features.mindboost.core.session.cognitive.StoreFailureReason
import com.mindos.app.features.mindboost.core.session.cognitive.TimingIntegritySnapshot
import com.mindos.app.features.mindboost.core.session.cognitive.WriteAheadEvaluator
import com.mindos.app.features.mindboost.core.session.cognitive.resolveWriteAheadEvidence
import com.mindos.app.features.mindboost.core.session.payload.NoOpPayloadJournalStore
import com.mindos.app.features.mindboost.core.session.payload.PayloadJournalStore
import com.mindos.app.features.mindboost.core.session.payload.PayloadKind
import com.mindos.app.features.mindboost.core.sync.DefaultSyncSessionIdGenerator
import com.mindos.app.features.mindboost.core.sync.SyncSessionIdGenerator
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.cognitive.GoNoGoCognitiveSessionAdapter
import com.mindos.app.features.mindboost.speed.domain.cognitive.GoNoGoRunArtifacts
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoConfig
import com.mindos.app.features.mindboost.speed.domain.exercise.GoNoGoVelocityGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.XorShiftSpeedRng
import com.mindos.app.features.mindboost.speed.domain.quality.SpeedAntiGaming
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoFeedback
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoFeedbackKind
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetrics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetricsMapper
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoOfficialRunAdapter
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoOutcome
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoQualityDiagnostics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoResponse
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoRunSummary
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScore
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScorer
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoStimulus
import com.mindos.app.features.mindboost.speed.domain.score.OfficialExerciseRunAdapterResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Fase del bucle de E8. Idle → (Foreperiod → Stimulus → Feedback)×N → Finished. */
enum class GoNoGoPhase { Idle, Foreperiod, Stimulus, Feedback, Finished }

/**
 * SYSTEM-1 slice 4B · Honest, visible outcome of persisting the E8 run to the OFFICIAL cognitive-history store.
 * Replaces a plain `summaryPersisted` boolean so the UI can distinguish a real save from an index-repair-pending
 * save from a failure — never faking saved progress.
 */
sealed interface CognitiveRunSaveState {
    data object NotAttempted : CognitiveRunSaveState
    data object Saved : CognitiveRunSaveState
    data object IdempotentDuplicate : CognitiveRunSaveState
    data object SavedIndexRepairNeeded : CognitiveRunSaveState

    /** A TRANSIENT storage fault (backend threw, or an existing row was unreadable): retryable — the write-ahead
     * journal keeps the entry and a later replay may still succeed. Never shown as saved. */
    data class Failed(val reason: String) : CognitiveRunSaveState
    data class DuplicateConflict(val sessionId: String) : CognitiveRunSaveState

    /** SYSTEM-1 6-R3 P2-01: a TERMINAL non-success (e.g. the subject's local data was already erased,
     * [StoreFailureReason.SUBJECT_TOMBSTONED]) — this run can NEVER be saved on retry. Distinct from [Failed]
     * (retryable) so the UI never implies "try again later" for something permanently gone. */
    data class Unrecoverable(val reason: String) : CognitiveRunSaveState
}

/** Estado de UI de E8 · Inhibición Rápida. Score/feedback null hasta terminar (nunca fabricados). */
data class GoNoGoUiState(
    val phase: GoNoGoPhase = GoNoGoPhase.Idle,
    val trialIndex: Int = 0,
    val totalTrials: Int = 0,
    val currentStimulus: GoNoGoStimulus? = null,
    val lastOutcome: GoNoGoOutcome? = null,
    val score: GoNoGoScore? = null,
    val metrics: GoNoGoMetrics? = null,
    val feedback: GoNoGoFeedbackKind? = null,
    // In-memory result VIEW for the finished panel — NON-persisted; NOT an official run (the only official
    // persistence is the CognitiveSessionRun). Named `resultView` so it never reads as a saved official run.
    val resultView: OfficialExerciseRunAdapterResult? = null,
    val saveState: CognitiveRunSaveState = CognitiveRunSaveState.NotAttempted,
    val sessionId: String? = null,
)

/**
 * Driver de sesión de E8 · Go/No-Go Velocity. State machine PURA guiada por eventos de tiempo desde la pantalla
 * (no toca `Clock.System` ni mide tiempo por su cuenta: la pantalla calcula el RT desde el onset y lo pasa).
 *
 * SYSTEM-1 slice 4B — persistencia OFICIAL: al iniciar escribe el write-ahead `SessionRecord` STARTED (via el
 * [SecureSessionRecorder] reusado); al terminar FINALIZA el SessionRecord, resuelve el [WriteAheadEvaluator]
 * verdict REAL sobre el mismo `sessionId`, construye UN `CognitiveSessionRun` (el ÚNICO historial cognitivo
 * oficial de E8) y lo guarda en el [CognitiveSessionRunStore], exponiendo un [CognitiveRunSaveState] honesto; al
 * abortar marca el SessionRecord ABANDONED. NO escribe telemetry summary (esa doble-verdad se retiró): el score
 * sigue PRELIMINAR (§7) y — sin timebase autoritativo ni cobertura anti-gaming adecuada — el run va a HISTORIAL
 * pero NO cuenta para perfil/calibración/ranking. La recuperación de huérfanos la hace el `SessionRecoveryScanner`
 * EXISTENTE en `SpeedRootScreen` (no se crea uno nuevo).
 */
// detekt LongParameterList (threshold 7): this is a DI boundary (scope + the 2 SYSTEM-1 stores + config + clock +
// id-gen + trialCount + the S3 payload journal); bundling the stores would reshape the CEO-approved 4B wiring just to
// satisfy a lint rule, so a localized suppress is the trade. The 7th param is the CEO-mandated injected
// SessionIdGenerator (e8-$seed audit); the 8th is the optional write-ahead journal (no-op default = pre-S3 behavior).
@Suppress("LongParameterList")
class GoNoGoSessionStateHolder constructor(
    private val scope: CoroutineScope,
    // MANDATORY (no default, P2-01): the only production caller (E8InhibicionScreen) injects the durable,
    // subject-scoped SecureSessionRecordStore. A default InMemory store here would let a careless call-site
    // silently write the write-ahead lifecycle to a throwaway in-process store (lost on exit) instead of the
    // durable one the recovery scanner and cognitive-run verdict both depend on.
    private val sessionRecordStore: SessionRecordStore,
    // MANDATORY (no default): the only production caller (E8InhibicionScreen) injects the durable, subject-scoped
    // SecureCognitiveSessionRunStore(subject = authenticated userId). A default InMemory store here would let a
    // careless call-site silently persist E8 history to a throwaway in-process store (lost on exit) AND drop the
    // per-account isolation the subject scoping guarantees — so the store is a required dependency (P2-01).
    private val cognitiveRunStore: CognitiveSessionRunStore,
    private val config: GoNoGoConfig = GoNoGoConfig.mixedDifficulty(),
    private val clock: EpochClock = SystemEpochClock,
    // Injected so the sessionId is opaque + collision-resistant (see startSession). Reuses the existing sync-layer
    // generator (Firestore-safe, random-suffix disambiguation) rather than a new abstraction (no double-truth).
    private val sessionIdGenerator: SyncSessionIdGenerator = DefaultSyncSessionIdGenerator(),
    private val trialCount: Int = DEFAULT_TRIALS,
    // S3 write-ahead payload journal: the sealed run is journaled BEFORE `cognitiveRunStore.save` and confirmed after,
    // so an OS kill of the backgrounded app mid-seal is recovered by the Velocidad-entry replay (idempotent — the run
    // store dedups by runId). Default no-op = the pre-S3 fire-and-forget-durability behavior (existing tests intact).
    private val payloadJournal: PayloadJournalStore = NoOpPayloadJournalStore,
) {
    // E8 is launched from the Velocidad menu = user-chosen OFFICIAL training (not practice); it stays preliminary
    // and history-only regardless, so purpose does not (yet) gate anything. A plan-launched variant would override.
    private val sessionPurpose: SessionPurpose = SessionPurpose.TRAINING_FREE

    // The reused write-ahead recorder over the SAME store the verdict resolver reads (guarantees consistency).
    private val recorder: SessionRecorder = SecureSessionRecorder(sessionRecordStore, clock)
    private val generator = GoNoGoVelocityGenerator(config)
    private val stimuli = mutableListOf<GoNoGoStimulus>()
    private val responses = mutableListOf<GoNoGoResponse>()
    private var runSeq = 0

    /** MC9-D-R3 — taps recibidos sin onset capturado (input medido pero no temporizable): se cuentan, no se fingen. */
    private var missingOnsetTaps = 0

    // Write-ahead lifecycle coordination (reuses the existing SessionRecorder). Each run captures its OWN immutable
    // RunLifecycle (sessionId + startedAtEpochMs + startJob + finalized) so a "Repetir bloque" that starts a NEW run
    // before the previous run's save coroutine settles can never clobber the sealing run's start time (which would
    // break CognitiveSessionRun's completedAt>=startedAt invariant) nor silence a legitimate abort of the new run.
    private val lifecycleMutex = Mutex()
    private var currentRun: RunLifecycle? = null

    private val _state = MutableStateFlow(GoNoGoUiState(totalTrials = trialCount))
    val state: StateFlow<GoNoGoUiState> = _state.asStateFlow()

    /**
     * Inicia (o reinicia) la corrida: baraja los estímulos, escribe el write-ahead STARTED y arranca.
     *
     * P2-01 (item 4): si YA hay una corrida activa que nunca alcanzó `finishSession()` (p. ej. este método se
     * invoca directamente, sin pasar por los botones gateados de la pantalla), esa corrida previa se abandona
     * EXACTAMENTE UNA VEZ antes de arrancar la nueva — nunca se deja huérfana (un `SessionRecord` STARTED que
     * jamás transiciona). Si la previa YA es terminal (p. ej. "Repetir bloque" tras `finishSession()`, con el
     * sellado aún pendiente), [RunLifecycle.claimTerminal] devuelve `false` y no se hace nada: su propio sellado
     * es dueño de esa transición (comportamiento ya cubierto por `repeating_a_block_before_the_previous_save_settles`).
     */
    // `CoroutineStart.ATOMIC` is deliberate (durable seal / abandon must run even if the screen scope is torn down
    // in the same tick) — see the `sealOfficialRun` KDoc; opt-in acknowledges the delicate API per P2-01 item 5.
    @OptIn(DelicateCoroutinesApi::class)
    fun startSession() {
        val previous = currentRun
        if (previous != null && previous.claimTerminal()) {
            scope.launch(start = CoroutineStart.ATOMIC) {
                withContext(NonCancellable) {
                    previous.startJob.join()
                    lifecycleMutex.withLock { recorder.onAbandon(previous.sessionId) }
                }
            }
        }
        generator.reset()
        stimuli.clear()
        responses.clear()
        missingOnsetTaps = 0
        val startedAt = clock.nowMillis()
        // The RNG seed drives ONLY stimulus randomization: it need not be globally unique, so time+counter is fine —
        // and it stays DECOUPLED from the sessionId (the sessionId must not be derivable from the stimulus stream).
        val rngSeed = startedAt + runSeq
        runSeq += 1
        val rng = XorShiftSpeedRng(if (rngSeed == 0L) 1L else rngSeed)
        repeat(trialCount) { stimuli.add(generator.next(rng)) }
        // Opaque, collision-resistant sessionId via the injected generator: its random suffix disambiguates two runs
        // that start in the SAME millisecond. The old `"e8-$seed"` could collide ACROSS holder instances (runSeq
        // resets to 0, so two E8 entries in one ms produced the same id → silent conflict / false idempotency in the
        // run store). SessionRecord.sessionId == CognitiveSessionRun.sessionId stays true (both use this value).
        val sessionId = sessionIdGenerator.newSessionId(E8_SESSION_MODULE, startedAt).value
        // ATOMIC + NonCancellable (P2-01, kmp-architect follow-up): the write-ahead STARTED seed must land durably
        // even if the screen scope is torn down in the SAME tick startSession() returns. Otherwise a later
        // abort()/sealOfficialRun — which only `startJob.join()`s this job, and `join()` returns normally whether the
        // job COMPLETED or was cancelled mid-write — could persist ABANDONED/FINALIZED over a STARTED row that never
        // landed, inverting the write-ahead ordering the terminal-write sites were hardened to protect.
        val job = scope.launch(start = CoroutineStart.ATOMIC) {
            withContext(NonCancellable) {
                lifecycleMutex.withLock { recorder.onStart(sessionId, ScoreModuleId.SPEED, startedAt, trialCount) }
            }
        }
        currentRun = RunLifecycle(sessionId, startedAt, job)
        _state.value = GoNoGoUiState(
            phase = GoNoGoPhase.Foreperiod,
            trialIndex = 0,
            totalTrials = trialCount,
            currentStimulus = stimuli.firstOrNull(),
            sessionId = sessionId,
        )
    }

    /** El foreperiodo terminó: se muestra el estímulo (onset). La pantalla empieza a medir el RT desde aquí. */
    fun onStimulusShown() {
        if (_state.value.phase == GoNoGoPhase.Foreperiod) {
            _state.value = _state.value.copy(phase = GoNoGoPhase.Stimulus)
        }
    }

    /** Tap durante el foreperiodo (antes del onset): respuesta prematura → resuelve el trial como anticipación. */
    fun onPrematureTap() {
        if (_state.value.phase == GoNoGoPhase.Foreperiod) {
            resolveCurrent(GoNoGoResponse(respondedDuringForeperiod = true, responseTimeMs = null))
        }
    }

    /**
     * Tap tras el onset. [rtMs] = RT (ms) desde el onset ⇒ resuelve el trial (GO en plazo/tarde, o NO-GO comisión);
     * `null` = tap SIN onset capturado (carrera sub-frame) ⇒ NO se fabrica un RT=0 ni se resuelve el trial: se cuenta
     * como input PERDIDO (impacta integridad/confianza/elegibilidad) y el trial se resolverá por ventana agotada.
     */
    fun onStimulusTap(rtMs: Long?) {
        if (_state.value.phase != GoNoGoPhase.Stimulus) return
        if (rtMs != null) {
            resolveCurrent(GoNoGoResponse(respondedDuringForeperiod = false, responseTimeMs = rtMs))
        } else {
            missingOnsetTaps += 1
        }
    }

    /** Se agotó la ventana de respuesta sin tap: GO ⇒ omisión, NO-GO ⇒ inhibición correcta. */
    fun onResponseWindowElapsed() {
        if (_state.value.phase == GoNoGoPhase.Stimulus) {
            resolveCurrent(GoNoGoResponse(respondedDuringForeperiod = false, responseTimeMs = null))
        }
    }

    /** Avanza al siguiente trial, o termina la corrida si no quedan. */
    fun beginNextTrial() {
        if (_state.value.phase != GoNoGoPhase.Feedback) return
        val next = _state.value.trialIndex + 1
        if (next >= trialCount) {
            finishSession()
        } else {
            _state.value = _state.value.copy(
                phase = GoNoGoPhase.Foreperiod,
                trialIndex = next,
                currentStimulus = stimuli.getOrNull(next),
                lastOutcome = null,
            )
        }
    }

    private fun resolveCurrent(response: GoNoGoResponse) {
        val index = _state.value.trialIndex
        val stimulus = stimuli.getOrNull(index) ?: return
        val outcome = GoNoGoOutcome.resolve(stimulus, response)
        responses.add(response)
        _state.value = _state.value.copy(phase = GoNoGoPhase.Feedback, lastOutcome = outcome)
    }

    /**
     * Aborta la corrida (nav-away/reinicio/interrupción): vuelve a Idle SIN guardar un `CognitiveSessionRun` (una
     * corrida incompleta NO es dato) y marca el write-ahead `SessionRecord` como ABANDONED.
     *
     * P2-01 (item 3): la corrida solo se abandona si [RunLifecycle.claimTerminal] la reclama, es decir, si NADIE
     * (ni un `finishSession()` ya disparado) la reclamó antes. Si `finishSession()` YA la reclamó de forma
     * síncrona (p. ej. el usuario toca "Volver" justo tras terminar, antes de que el sellado async se resuelva),
     * este método es un NO-OP TOTAL — nunca borra el resultado recién calculado (`score`/`metrics`/`resultView`)
     * ni escribe ABANDONED sobre una corrida que ya se está sellando como FINALIZED. Exactamente una transición
     * terminal por corrida, nunca ambas.
     */
    // ATOMIC: the ABANDONED write must survive screen-scope teardown — rationale in `sealOfficialRun`.
    @OptIn(DelicateCoroutinesApi::class)
    fun abort() {
        val run = currentRun
        if (run != null && !run.claimTerminal()) return
        stimuli.clear()
        responses.clear()
        missingOnsetTaps = 0
        currentRun = null
        _state.value = GoNoGoUiState(totalTrials = trialCount)
        if (run != null) {
            scope.launch(start = CoroutineStart.ATOMIC) {
                withContext(NonCancellable) {
                    run.startJob.join()
                    lifecycleMutex.withLock { recorder.onAbandon(run.sessionId) }
                }
            }
        }
    }

    private fun finishSession() {
        val outcomes = stimuli.indices.mapNotNull { i ->
            responses.getOrNull(i)?.let { GoNoGoOutcome.resolve(stimuli[i], it) }
        }
        val summary = GoNoGoRunSummary.from(outcomes)
        val metrics = GoNoGoMetricsMapper.metrics(summary)
        // MC9-D-R3: lost-input taps (no onset) feed the data-quality check ⇒ they impact integrity/eligibility.
        val diagnostics = GoNoGoQualityDiagnostics.evaluate(outcomes, missingOnsetTaps)
        // MC9-D-R2/R3: the scorer REQUIRES diagnostics (compile-time gate) ⇒ impossible/lost RTs cannot score positive.
        val score = GoNoGoScorer.score(metrics, diagnostics)
        val feedback = GoNoGoFeedback.forRun(metrics, score)
        val run = currentRun
        val now = clock.nowMillis()
        val startedAt = run?.startedAtEpochMs ?: now
        val durationActualMs = (now - startedAt).coerceAtLeast(0L)
        val completedTrials = outcomes.size
        // The in-memory result view is a NON-persisted UI/diagnostic DTO (kept for the finished panel); the ONLY
        // official persistence is the CognitiveSessionRun sealed below.
        val resultView = run?.let {
            GoNoGoOfficialRunAdapter.toOfficialRun(
                sessionId = it.sessionId,
                exerciseId = SpeedExerciseId.E8_INHIBICION.code,
                exerciseVersion = GoNoGoVelocityGenerator.CONFIG_VERSION,
                summary = summary,
                metrics = metrics,
                score = score,
                diagnostics = diagnostics,
                plannedTrials = trialCount,
                completedTrials = completedTrials,
                durationActualMs = durationActualMs,
            )
        }
        _state.value = _state.value.copy(
            phase = GoNoGoPhase.Finished,
            currentStimulus = null,
            score = score,
            metrics = metrics,
            feedback = feedback,
            resultView = resultView,
        )
        // P2-01 (item 3): claim the run's SINGLE terminal transition synchronously, BEFORE any coroutine is
        // launched. This makes a later `abort()` call for the SAME run (e.g. an immediate "Volver" tap before the
        // async seal below settles) deterministically see the claim already taken and no-op — never abandoning a
        // run that is already being finalized, and never racing two async coroutines for the same decision.
        if (run != null && run.claimTerminal()) {
            val artifacts = GoNoGoRunArtifacts(outcomes, summary, metrics, score, diagnostics)
            sealOfficialRun(run, artifacts, now, durationActualMs, completedTrials)
        }
    }

    /**
     * Seals the run in the CEO-mandated order: FINALIZE the write-ahead `SessionRecord`, resolve the REAL
     * write-ahead verdict for the SAME sessionId, build ONE `CognitiveSessionRun`, journal it (S3 write-ahead),
     * save it, confirm/drop the journal per [SaveOutcomeClass], and surface the honest save-result — but ONLY onto
     * the state slot if it is STILL showing THIS run (P2-01 item 2: a later `startSession()`/`abort()` may have
     * moved `_state` on to a different sessionId while this save was in flight; that run's own UI must never be
     * overwritten by a stale update from an earlier one).
     *
     * `CoroutineStart.ATOMIC` + `NonCancellable` (P2-01 item 5): a plain `launch` can be cancelled BEFORE its body
     * ever runs if the caller's scope (the screen's `rememberCoroutineScope`) is torn down in the same tick
     * `finishSession()` returns — `NonCancellable` alone does not protect against that, since it only takes effect
     * once the body starts executing. `ATOMIC` guarantees this coroutine's body begins regardless, and
     * `NonCancellable` then keeps it running to completion, so a completed run is never lost to an immediate
     * "Volver"/screen-dispose race. S3: the run is journaled BEFORE the save, so an OS kill of the backgrounded app
     * between the enqueue and the save is recovered by the Velocidad-entry replay (idempotent — the run store
     * dedups by runId). Residual ceiling: a process death BEFORE the enqueue still loses the run (the run is only
     * buildable after the async finalize/verdict, so the journal write cannot precede it).
     */
    // ATOMIC + NonCancellable durable seal (P2-01 item 5); full rationale in the KDoc above.
    @OptIn(DelicateCoroutinesApi::class)
    private fun sealOfficialRun(
        run: RunLifecycle,
        artifacts: GoNoGoRunArtifacts,
        completedAtEpochMs: Long,
        durationActualMs: Long,
        completedTrials: Int,
    ) {
        scope.launch(start = CoroutineStart.ATOMIC) {
            withContext(NonCancellable) {
                run.startJob.join()
                // The run's terminal transition was already claimed synchronously in `finishSession()`; only the
                // actual write is serialized here.
                lifecycleMutex.withLock { recorder.onFinalize(run.sessionId, completedTrials, durationActualMs) }
                val verdict = WriteAheadEvaluator.evaluate(
                    resolveWriteAheadEvidence(sessionRecordStore, run.sessionId),
                    SessionCompletionStatus.COMPLETED,
                    run.startedAtEpochMs,
                    completedAtEpochMs,
                )
                val cognitiveRun = GoNoGoCognitiveSessionAdapter.toCognitiveSessionRun(
                    artifacts = artifacts,
                    sessionId = run.sessionId,
                    exerciseId = SpeedExerciseId.E8_INHIBICION.code,
                    exerciseVersion = GoNoGoVelocityGenerator.CONFIG_VERSION,
                    scoringVersion = artifacts.score.version,
                    sessionPurpose = sessionPurpose,
                    writeAhead = verdict,
                    timing = e8TimingSnapshot(artifacts.score),
                    plannedTrials = trialCount,
                    startedAtEpochMs = run.startedAtEpochMs,
                    completedAtEpochMs = completedAtEpochMs,
                    durationActualMs = durationActualMs,
                    createdAtEpochMs = completedAtEpochMs,
                    // FAIL-CLOSED, DERIVED (never a literal): E8's timebase is non-authoritative (system-monotonic,
                    // not Choreographer) and SpeedAntiGaming.COVERAGE is PARTIAL — so the run is history-visible but
                    // denied every official lane.
                    antiGamingCoverageSatisfied = SpeedAntiGaming.COVERAGE.adequateFor(TimingCriticality.RT_CRITICAL),
                )
                // S3 write-ahead: journal the run BEFORE saving. The UI must never claim "saved" before this call
                // returns (P2-01 item 6): `_state.saveState` stays at its prior value (NotAttempted on a fresh run)
                // until `cognitiveRunStore.save` below actually resolves.
                payloadJournal.enqueue(run.sessionId, PayloadKind.E8_COGNITIVE_RUN, encodeCognitiveRun(cognitiveRun))
                val saveResult = cognitiveRunStore.save(cognitiveRun)
                val outcome = saveResult.outcomeClass()
                if (outcome == SaveOutcomeClass.TERMINAL_FAILURE) {
                    ObservabilityHub.breadcrumb("e8_save_terminal_failure")
                }
                if (outcome.shouldConfirmJournal()) payloadJournal.confirm(run.sessionId)
                if (_state.value.sessionId == run.sessionId) {
                    _state.value = _state.value.copy(saveState = saveResult.toE8SaveState())
                }
            }
        }
    }

    /**
     * Immutable per-run lifecycle capture. Threading this through [finishSession]/[sealOfficialRun]/[abort]/
     * [startSession] instead of reading the holder's mutable fields is what makes overlapping runs (a fast
     * "Repetir bloque") safe: a new run can never overwrite a still-sealing run's `startedAtEpochMs` (invariant
     * break). [claimTerminal] is the SINGLE authority for this run's one-and-only terminal transition (finalize XOR
     * abandon): whichever caller claims it first proceeds; every other caller sees `false` and must skip its own
     * write, so a run can never be both finalized and abandoned (P2-01 item 3).
     */
    private class RunLifecycle(
        val sessionId: String,
        val startedAtEpochMs: Long,
        val startJob: Job,
    ) {
        private var terminalClaimed = false

        /**
         * PRECONDITION (kmp-architect P2-01 review): this check-then-set is intentionally NON-atomic — its
         * one-writer guarantee holds because every lifecycle transition ([startSession]/[finishSession]/[abort]) runs
         * on the holder's SINGLE confined dispatcher (the injected `scope`, a Compose-UI `rememberCoroutineScope` at
         * every production call site → single-threaded). Callers MUST NOT drive lifecycle transitions from a second
         * thread / background dispatcher; doing so would let two callers both observe `false` and both claim the
         * terminal transition. If a multi-threaded caller is ever introduced, back this with an atomic (expect/actual
         * `AtomicBoolean` or atomicfu) before that change lands.
         */
        fun claimTerminal(): Boolean {
            if (terminalClaimed) return false
            terminalClaimed = true
            return true
        }
    }

    companion object {
        const val DEFAULT_TRIALS = 40

        /** Module prefix for the generated sessionId (`e8-{startedAtMs}-{suffix}`); kept readable and stable. */
        const val E8_SESSION_MODULE = "e8"

        /** Ventana de gracia (ms) tras el deadline para capturar una respuesta GO tardía antes de cerrar el trial. */
        const val LATE_GRACE_MS = 400L
    }
}

// Honest timing: E8 times RT with `TimeSource.Monotonic` → SYSTEM_MONOTONIC, not frame-authoritative.
private fun e8TimingSnapshot(score: GoNoGoScore): TimingIntegritySnapshot =
    GoNoGoCognitiveSessionAdapter.timingSnapshotFor(score.timingIntegrity)
        .copy(clockSource = ClockSource.SYSTEM_MONOTONIC, monotonicAvailable = true)

/**
 * Three-way settlement classification of a [SaveResult] — the SINGLE source of truth both [sealOfficialRun]'s
 * journal-confirm decision and [e8RunReplayHandler] use (P2-01 item 1), so a run is never silently treated as
 * saved when it merely reached a terminal non-success, and a permanently-undoable result is never retried
 * forever.
 */
internal enum class SaveOutcomeClass { SUCCESS, TERMINAL_FAILURE, TRANSIENT_FAILURE }

internal fun SaveResult.outcomeClass(): SaveOutcomeClass = when (this) {
    SaveResult.Saved, SaveResult.IdempotentDuplicate, SaveResult.SavedIndexRepairNeeded -> SaveOutcomeClass.SUCCESS
    is SaveResult.DuplicateRunConflict -> SaveOutcomeClass.TERMINAL_FAILURE
    is SaveResult.Failed -> if (reason == StoreFailureReason.SUBJECT_TOMBSTONED) {
        SaveOutcomeClass.TERMINAL_FAILURE
    } else {
        SaveOutcomeClass.TRANSIENT_FAILURE
    }
}

/** A SUCCESS is durably done and a TERMINAL_FAILURE can never succeed on retry — both are confirmed (dropped/
 * dead-lettered) so the journal never loops. Only a TRANSIENT failure is kept for the next replay attempt. */
internal fun SaveOutcomeClass.shouldConfirmJournal(): Boolean = this != SaveOutcomeClass.TRANSIENT_FAILURE

/** Maps a storage [SaveResult] to the honest UI [CognitiveRunSaveState] (a failure/conflict never reads as saved;
 * a TERMINAL non-success gets its own distinct, non-retryable state — P2-01 item 1). */
private fun SaveResult.toE8SaveState(): CognitiveRunSaveState = when (this) {
    SaveResult.Saved -> CognitiveRunSaveState.Saved
    SaveResult.IdempotentDuplicate -> CognitiveRunSaveState.IdempotentDuplicate
    SaveResult.SavedIndexRepairNeeded -> CognitiveRunSaveState.SavedIndexRepairNeeded
    is SaveResult.DuplicateRunConflict -> CognitiveRunSaveState.DuplicateConflict(runId)
    is SaveResult.Failed -> if (reason == StoreFailureReason.SUBJECT_TOMBSTONED) {
        CognitiveRunSaveState.Unrecoverable(reason.name)
    } else {
        CognitiveRunSaveState.Failed(reason.name)
    }
}
