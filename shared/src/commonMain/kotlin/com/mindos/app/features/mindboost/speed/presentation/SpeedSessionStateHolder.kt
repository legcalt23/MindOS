// El holder orquesta el bucle de medición: constructor con la configuración completa de la
// corrida (muchos params), varias funciones de fase y números de presentación.
@file:Suppress("LongParameterList", "TooManyFunctions", "MagicNumber")

package com.mindos.app.features.mindboost.speed.presentation

import com.mindos.app.core.time.EpochClock
import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.NoOpSessionRecorder
import com.mindos.app.features.mindboost.core.session.RandomSessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionIdGenerator
import com.mindos.app.features.mindboost.core.session.SessionRecorder
import com.mindos.app.features.mindboost.core.timing.FrameTimingProbe
import com.mindos.app.features.mindboost.core.timing.NoOpFrameTimingProbe
import com.mindos.app.features.mindboost.speed.data.persistence.MeasurementRun
import com.mindos.app.features.mindboost.speed.data.persistence.SpeedMeasurementStore
import com.mindos.app.features.mindboost.speed.domain.FixedTrialEngine
import com.mindos.app.features.mindboost.speed.domain.MetricThresholds
import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.AdaptiveStaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.StaircaseDirective
import com.mindos.app.features.mindboost.speed.domain.StaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.exercise.SpeedPromptGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.SpeedTrialPrompt
import com.mindos.app.features.mindboost.speed.domain.exercise.XorShiftSpeedRng
import com.mindos.app.features.mindboost.speed.domain.metrics.MeasurementContext
import com.mindos.app.features.mindboost.speed.domain.metrics.MetricCalculator
import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.BlockRecord
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot
import com.mindos.app.features.mindboost.speed.domain.quality.DeviceProfileFactory
import com.mindos.app.features.mindboost.speed.domain.quality.QualitySignals
import com.mindos.app.features.mindboost.speed.domain.quality.SessionQualityEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Fases del bucle de medición de un bloque. */
enum class SpeedPhase { Idle, Foreperiod, Stimulus, Feedback, Finished }

/** Estado de UI puramente OPERATIVO/descriptivo (§9). Sin score ni claims (acceptance #16/#17). */
data class SpeedUiState(
    val exerciseId: SpeedExerciseId,
    val totalTrials: Int,
    val phase: SpeedPhase = SpeedPhase.Idle,
    val blockIndex: Int = 0,
    val totalBlocks: Int = 1,
    val trialIndex: Int = 0,
    val foreperiodMs: Long = 0L,
    val deadlineMs: Long? = null,
    val prompt: SpeedTrialPrompt? = null,
    val lastRtMs: Long? = null,
    val lastWasAnticipation: Boolean = false,
    val lastWasLate: Boolean = false,
    val showTooSoon: Boolean = false,
    val blockMetrics: BlockMetrics? = null,
    val qualityLog: QualityDecisionLog? = null,
    val sessionPersisted: Boolean = false,
    val lastSession: MeasurementRun? = null,
)

/**
 * Orquesta una corrida de medición de E1/E6: foreperiodo → estímulo → respuesta →
 * persistencia. Sin score ni claims.
 *
 * MVP-0 (E1): 1 bloque, dificultad fija, `staircase = null`, sin deadline.
 * MVP-1 (E6 adaptativo): `blockCount` bloques; cada bloque toma su `deadline` del
 * [StaircaseEngine] (pc-driven), y entre bloques el staircase actualiza la CCE con las
 * `BlockMetrics`. Una respuesta más lenta que el deadline del bloque cuenta como
 * **incorrecta** (operacionalización del límite temporal): así un deadline más corto baja
 * la `pc` y el staircase afloja — cerrando el lazo pc→deadline. Sin adaptación en E1.
 */
class SpeedSessionStateHolder(
    private val scope: CoroutineScope,
    private val store: SpeedMeasurementStore,
    private val clock: EpochClock,
    private val userId: String,
    private val exerciseId: SpeedExerciseId,
    private val context: MeasurementContext,
    private val apiLevel: Int,
    private val deviceModel: String,
    private val osVersion: String,
    private val thresholds: MetricThresholds = MetricThresholds.DEFAULT,
    private val trialsPerBlock: Int = DEFAULT_TRIALS_PER_BLOCK,
    private val fixedDifficulty: Double = DEFAULT_DIFFICULTY,
    private val blockCount: Int = 1,
    private val staircase: StaircaseEngine? = null,
    private val promptGenerator: SpeedPromptGenerator? = null,
    // INFRA-02: real frame-drop/jitter source. Default no-op keeps desktop/iOS/tests at "unavailable"
    // (non-authoritative, no jank), identical to the old hardcoded 0.0/false; Android supplies the real
    // Choreographer probe via rememberFrameTimingProbe().
    private val frameProbe: FrameTimingProbe = NoOpFrameTimingProbe,
    // MC1 (Fase 2.2): additive write-ahead session lifecycle. Defaults keep production/tests unchanged until a
    // real SecureSessionRecorder is injected. NEVER touches scoring/persistence (only the hooks below).
    private val recorder: SessionRecorder = NoOpSessionRecorder,
    private val sessionIdGenerator: SessionIdGenerator = RandomSessionIdGenerator(),
) {
    private val _state = MutableStateFlow(
        SpeedUiState(exerciseId = exerciseId, totalTrials = trialsPerBlock, totalBlocks = blockCount),
    )
    val state: StateFlow<SpeedUiState> = _state.asStateFlow()

    // MC9-B-R §1: a fresh measurement session id PER RUN. A single holder instance is reused across "Run Again"
    // (onRepeat = startSession), so a run-scoped id — regenerated in startSession() — is what stops a repeat from
    // overwriting/contaminating the previous run's history. The run counter guarantees distinctness even under a
    // fixed clock (same-millisecond repeats). Stays stable for the duration of one run; blocks/trials/metrics/
    // quality logs/version snapshot all read it.
    private var measurementRunSeq = 0
    private var activeMeasurementSessionId: String = newMeasurementSessionId()
    private val foreperiodRng = ForeperiodGenerator(clock.nowMillis())
    private val promptRng = XorShiftSpeedRng(clock.nowMillis())
    private val metricCalculator = MetricCalculator(thresholds)
    private val qualityEvaluator = SessionQualityEvaluator(thresholds)

    // Acumuladores de la corrida (multi-bloque).
    private val allTrials = mutableListOf<TrialRecord>()
    private val allBlocks = mutableListOf<BlockRecord>()
    private val allBlockMetrics = mutableListOf<BlockMetrics>()

    private var blockIndex = 0
    private var currentDeadlineNanos: Long? = null
    private var currentStimulusNanos: Long = 0L
    private var blockStartedAtEpochMs: Long = clock.nowMillis()
    private var engine: FixedTrialEngine = newEngine(blockId(0))

    // Write-ahead recorder state (MC1). Opaque, generated at startSession; NOT the module's own
    // [activeMeasurementSessionId] (regenerated in lock-step for the same run).
    private val recorderMutex = Mutex()
    private var recorderSessionId: String = ""
    private var recorderStartedAtMs: Long = 0L

    /** Inicia (o reinicia) la corrida completa y programa el primer bloque. */
    fun startSession() {
        frameProbe.start()
        allTrials.clear()
        allBlocks.clear()
        allBlockMetrics.clear()
        blockIndex = 0
        // MC9-B-R §1: mint a NEW measurement session id for THIS run (read by blocks/trials/metrics/quality/
        // version snapshot below). The write-ahead recorderSessionId stays separate but is regenerated in lock-step.
        activeMeasurementSessionId = newMeasurementSessionId()
        staircase?.reset()
        // MC9-B: reset any generator sequence state (E4 · Cambio de Regla starts each run on the first rule).
        promptGenerator?.reset()
        recorderStartedAtMs = clock.nowMillis()
        recorderSessionId = sessionIdGenerator.newSessionId()
        val planned = trialsPerBlock * blockCount
        recordLifecycle { recorder.onStart(recorderSessionId, ScoreModuleId.SPEED, recorderStartedAtMs, planned) }
        _state.value = SpeedUiState(exerciseId = exerciseId, totalTrials = trialsPerBlock, totalBlocks = blockCount)
        beginBlock()
    }

    /** Llamado por la pantalla cuando expira el foreperiodo: muestra el estímulo. */
    fun onForeperiodElapsed() {
        if (_state.value.phase != SpeedPhase.Foreperiod) return
        _state.value = _state.value.copy(phase = SpeedPhase.Stimulus, showTooSoon = false)
    }

    /** Llamado con el `frameTimeNanos` del Choreographer cuando el estímulo se pinta. */
    fun onStimulusShown(stimulusUptimeNanos: Long) {
        if (_state.value.phase != SpeedPhase.Stimulus) return
        currentStimulusNanos = stimulusUptimeNanos
        engine.startTrial(fixedDifficulty, currentDeadlineNanos, stimulusUptimeNanos)
    }

    /**
     * Respuesta táctil capturada por [SpeedTapArea]. [choice] es null en E1; en E6 indica
     * el lado tocado. Un toque durante el foreperiodo es salida en falso.
     */
    fun onResponse(responseUptimeNanos: Long, choice: SpeedChoice?) {
        when (_state.value.phase) {
            SpeedPhase.Foreperiod -> registerPrematureTap()
            SpeedPhase.Stimulus -> registerValidResponse(responseUptimeNanos, choice)
            else -> Unit
        }
    }

    /** Avanza al siguiente trial o, si el bloque terminó, lo finaliza. */
    fun beginNextTrial() {
        val current = _state.value
        if (current.trialIndex >= trialsPerBlock) {
            finishBlock()
            return
        }
        _state.value = current.copy(
            phase = SpeedPhase.Foreperiod,
            foreperiodMs = foreperiodRng.nextForeperiodMs(),
            prompt = promptGenerator?.next(promptRng),
            showTooSoon = false,
            lastWasLate = false,
        )
    }

    private fun beginBlock() {
        currentDeadlineNanos = staircase?.let { it.getNextDeadline(it.currentCce()) }
        engine = newEngine(blockId(blockIndex))
        blockStartedAtEpochMs = clock.nowMillis()
        _state.value = _state.value.copy(
            phase = SpeedPhase.Idle,
            blockIndex = blockIndex,
            trialIndex = 0,
            deadlineMs = currentDeadlineNanos?.let { it / NANOS_PER_MS },
        )
        beginNextTrial()
    }

    private fun registerPrematureTap() {
        _state.value = _state.value.copy(
            phase = SpeedPhase.Foreperiod,
            foreperiodMs = foreperiodRng.nextForeperiodMs(),
            showTooSoon = true,
        )
    }

    private fun registerValidResponse(responseUptimeNanos: Long, choice: SpeedChoice?) {
        // E1 (RT simple) no tiene prompt ⇒ cualquier respuesta es correcta. En 2 vías
        // (E6/E3/E7) la corrección es contra el lado correcto del prompt.
        val baseCorrect = _state.value.prompt?.let { choice != null && choice == it.correctChoice } ?: true
        val rtNanos = responseUptimeNanos - currentStimulusNanos
        val late = currentDeadlineNanos?.let { rtNanos > it } ?: false
        // Respuesta tardía ⇒ incorrecta (operacionalización del límite temporal del §6).
        val isCorrect = baseCorrect && !late
        val record = runCatching { engine.onResponse(responseUptimeNanos, isCorrect) }.getOrNull() ?: return
        _state.value = _state.value.copy(
            phase = SpeedPhase.Feedback,
            trialIndex = _state.value.trialIndex + 1,
            lastRtMs = record.rtNanos / NANOS_PER_MS,
            lastWasAnticipation = record.isAnticipation,
            lastWasLate = late,
            showTooSoon = false,
        )
    }

    private fun finishBlock() {
        val trials = engine.getTrialHistory()
        allTrials += trials
        val blockMetrics = metricCalculator.blockMetrics(blockId(blockIndex), trials, context)
        allBlocks += buildBlockRecord(blockIndex)
        blockMetrics?.let {
            allBlockMetrics += it
            // El staircase gobierna la dificultad del SIGUIENTE bloque a partir de este (§6).
            staircase?.updateDifficulty(it)
        }
        _state.value = _state.value.copy(blockMetrics = blockMetrics)

        // R5 COOLDOWN (§6): si el staircase multi-señal pide cooldown por fatiga, la corrida
        // termina de forma controlada en lugar de seguir añadiendo bloques.
        val cooldown = (staircase as? AdaptiveStaircaseEngine)?.lastDirective() == StaircaseDirective.COOLDOWN
        if (!cooldown && blockIndex < blockCount - 1) {
            // Heartbeat between blocks: a long multi-block run stays "fresh" for the recovery scanner. The
            // terminal block is NOT checkpointed (finishSession finalizes it), avoiding a redundant write.
            val completed = allTrials.size
            val duration = activeRecorderDurationMs()
            recordLifecycle { recorder.onCheckpoint(recorderSessionId, completed, duration) }
            blockIndex += 1
            beginBlock()
        } else {
            finishSession()
        }
    }

    private fun finishSession() {
        val run = assembleRun()
        _state.value = _state.value.copy(
            phase = SpeedPhase.Finished,
            blockMetrics = allBlockMetrics.lastOrNull(),
            qualityLog = run.qualityLogs.firstOrNull(),
            lastSession = run,
        )
        scope.launch {
            store.saveRun(run)
            _state.value = _state.value.copy(sessionPersisted = true)
        }
        // Additive write-ahead finalize; separate from the scoring/persistence launch above (unchanged).
        val completed = allTrials.size
        val duration = activeRecorderDurationMs()
        recordLifecycle { recorder.onFinalize(recorderSessionId, completed, duration) }
    }

    private fun assembleRun(): MeasurementRun {
        val nowMs = clock.nowMillis()
        val sessionStartedAtMs = allBlocks.firstOrNull()?.startedAtEpochMs ?: nowMs
        val deviceProfile = buildDeviceProfile()
        val sessionMetrics = metricCalculator.sessionMetrics(
            sessionId = activeMeasurementSessionId,
            allTrials = allTrials,
            orderedBlockMetrics = allBlockMetrics,
            context = context,
        )
        val frameStats = frameProbe.stop()
        val decision = qualityEvaluator.evaluate(
            QualitySignals(
                totalTrials = allTrials.size,
                cv = sessionMetrics.cv,
                anticipationRate = sessionMetrics.anticipationRate,
                omissionRate = sessionMetrics.omissionRate,
                inputPrecision = context.inputTimestampPrecision,
                frameDropFraction = frameStats.frameDropFraction,
                criticalFrameJitter = frameStats.criticalFrameJitter,
                source = context.timestampSource,
            ),
        )
        return MeasurementRun(
            session = SessionRecord(
                id = activeMeasurementSessionId,
                userId = userId,
                timestampEpochMs = nowMs,
                deviceProfileId = deviceProfile.id,
                durationMs = nowMs - sessionStartedAtMs,
            ),
            deviceProfile = deviceProfile,
            versionSnapshot = VersionSnapshot(
                sessionId = activeMeasurementSessionId,
                metricsVersion = SpeedVersions.METRICS_VERSION,
                staircaseVersion = SpeedVersions.STAIRCASE_VERSION,
                difficultyModelVersion = SpeedVersions.DIFFICULTY_MODEL_VERSION,
                lapseMultiplier = thresholds.lapseMultiplier,
            ),
            blocks = allBlocks.toList(),
            trials = allTrials.toList(),
            blockMetrics = allBlockMetrics.toList(),
            sessionMetrics = sessionMetrics,
            qualityLogs = listOf(
                QualityDecisionLog(
                    id = "$activeMeasurementSessionId:q0",
                    sessionId = activeMeasurementSessionId,
                    deviceTimingRisk = decision.deviceTimingRisk,
                    sessionQuality = decision.sessionQuality,
                    reasons = decision.reasons,
                    createdAtEpochMs = nowMs,
                ),
            ),
        )
    }

    private fun buildBlockRecord(index: Int): BlockRecord = BlockRecord(
        id = blockId(index),
        sessionId = activeMeasurementSessionId,
        blockIndex = index,
        exerciseId = exerciseId.code,
        startedAtEpochMs = blockStartedAtEpochMs,
        durationMs = clock.nowMillis() - blockStartedAtEpochMs,
        // En E6 adaptativo la "dificultad fija" del bloque ES la CCE a la que corrió.
        fixedDifficulty = staircase?.currentCce() ?: fixedDifficulty,
        deadlineNanos = currentDeadlineNanos,
        timestampSource = context.timestampSource,
        inputTimestampPrecision = context.inputTimestampPrecision,
        timebase = context.timebase,
    )

    private fun buildDeviceProfile(): DeviceProfile {
        // Proxy de SRT del dispositivo: la mediana de TR del primer bloque (solo significativa en E1).
        val srtMs = if (exerciseId == SpeedExerciseId.E1_DESTELLO) {
            allBlockMetrics.firstOrNull()?.rtMedianNanos?.let { it / NANOS_PER_MS_DOUBLE } ?: 0.0
        } else {
            0.0
        }
        val id = "device:${deviceModel}:$apiLevel".filter { it.isLetterOrDigit() || it == ':' || it == '-' }
        return DeviceProfileFactory.create(
            id = id,
            deviceModel = deviceModel,
            osVersion = osVersion,
            apiLevel = apiLevel,
            simpleReactionMedianMs = srtMs,
        )
    }

    private fun blockId(index: Int): String = "$activeMeasurementSessionId:b$index"

    /** A distinct measurement session id per run: readable prefix + a run counter so repeats never collide. */
    private fun newMeasurementSessionId(): String = "$userId:speed:${clock.nowMillis()}:r${measurementRunSeq++}"

    private fun newEngine(blockId: String): FixedTrialEngine =
        FixedTrialEngine(blockId, exerciseId, context, thresholds)

    /** Serializes write-ahead upserts so STARTED -> CHECKPOINT -> FINALIZED never race; off the frame path. */
    private fun recordLifecycle(action: suspend () -> Unit) {
        scope.launch { recorderMutex.withLock { action() } }
    }

    private fun activeRecorderDurationMs(): Long = (clock.nowMillis() - recorderStartedAtMs).coerceAtLeast(0)

    private companion object {
        const val DEFAULT_TRIALS_PER_BLOCK = 12
        const val DEFAULT_DIFFICULTY = 0.5
        const val NANOS_PER_MS = 1_000_000L
        const val NANOS_PER_MS_DOUBLE = 1_000_000.0
    }
}

/**
 * Generador determinista de foreperiodos y elecciones (no usa `Math.random()`). El
 * foreperiodo es presentación pura (no se mide), así que la semilla del reloj es adecuada.
 */
private class ForeperiodGenerator(seed: Long) {
    private var state: Long = if (seed == 0L) 1L else seed

    private fun next(): Long {
        var x = state
        x = x xor (x shl SHIFT_A)
        x = x xor (x ushr SHIFT_B)
        x = x xor (x shl SHIFT_C)
        state = x
        return x
    }

    fun nextForeperiodMs(): Long {
        val span = MAX_FOREPERIOD_MS - MIN_FOREPERIOD_MS
        val unit = (next() ushr 1) % span
        return MIN_FOREPERIOD_MS + unit
    }

    private companion object {
        const val MIN_FOREPERIOD_MS = 800L
        const val MAX_FOREPERIOD_MS = 2200L
        const val SHIFT_A = 13
        const val SHIFT_B = 7
        const val SHIFT_C = 17
    }
}
