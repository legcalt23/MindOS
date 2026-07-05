@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "TooManyFunctions", "LongParameterList")

package com.mindos.app.features.mindboost.speed.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.speed.debug.DebugExportMetadata
import com.mindos.app.features.mindboost.speed.debug.DebugExportResult
import com.mindos.app.features.mindboost.speed.debug.DebugExporter
import com.mindos.app.features.mindboost.speed.debug.rememberDebugExporter
import com.mindos.app.features.mindboost.focus.data.local.rememberSecureKeyValueStore
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.di.speedSessionRecorder
import com.mindos.app.features.mindboost.storage.MindBoostStorageNamespaces
import com.mindos.app.features.mindboost.speed.domain.MultiSignalStaircaseEngine
import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.SpeedVersions
import com.mindos.app.features.mindboost.speed.domain.exercise.CifradoGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.CompuertaGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.MultiviaGenerator
import com.mindos.app.features.mindboost.speed.domain.exercise.SpeedPromptGenerator
import com.mindos.app.features.mindboost.speed.domain.score.SpeedScoreEngine
import com.mindos.app.features.mindboost.speed.presentation.SpeedPhase
import com.mindos.app.features.mindboost.speed.presentation.SpeedSessionStateHolder
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import com.mindos.app.features.mindboost.speed.presentation.SpeedTapArea
import com.mindos.app.features.mindboost.speed.presentation.SpeedUiState
import com.mindos.app.features.mindboost.core.timing.rememberFrameTimingProbe
import com.mindos.app.features.mindboost.speed.presentation.rememberSpeedTimebaseHandle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect

private const val FEEDBACK_MS = 650L
private const val APP_VERSION = "1.0-mvp0"

// E6 es adaptativo (MVP-1): varios bloques con deadline gobernado por el StaircaseEngine.
private const val ADAPTIVE_BLOCKS = 4

/**
 * Pantalla compartida de ejercicio de medición (E1/E6). SOLO feedback operativo (§9);
 * sin score ni claims (acceptance #16/#17). Captura el frame de estímulo (Choreographer)
 * y la respuesta (MotionEvent) vía [SpeedTapArea] y persiste la corrida completa.
 */
@Composable
fun SpeedExerciseScreen(
    exerciseId: SpeedExerciseId,
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val handle = rememberSpeedTimebaseHandle()
    val frameProbe = rememberFrameTimingProbe()
    val scope = rememberCoroutineScope()
    val exporter = rememberDebugExporter()
    val generator: SpeedPromptGenerator? = remember(exerciseId) { promptGeneratorFor(exerciseId) }
    val cipherKey = remember(generator) { (generator as? CifradoGenerator)?.key }
    // MC1b: activate the real write-ahead recorder in production. Writes run on `scope` (the SAME composable
    // scope that already persists the Speed run via saveRun), so they inherit its durability; abandonment on
    // nav-away is left to the recovery scanner (a later MC). The sessions store is the account-bound
    // `mindboost_sessions` namespace already covered by the account-deletion eraser.
    val sessionsStore = rememberSecureKeyValueStore(MindBoostStorageNamespaces.Sessions)
    val recorder = remember(sessionsStore, graph.clock) { speedSessionRecorder(sessionsStore, graph.clock) }
    val holder = remember(handle, frameProbe, graph, exerciseId, userId, recorder) {
        val adaptive = exerciseId != SpeedExerciseId.E1_DESTELLO // E6/E3/E7 son adaptativos
        SpeedSessionStateHolder(
            scope = scope,
            store = graph.store,
            clock = graph.clock,
            userId = userId,
            exerciseId = exerciseId,
            context = handle.measurementContext(),
            apiLevel = handle.apiLevel,
            deviceModel = handle.deviceModel,
            osVersion = handle.osVersion,
            thresholds = graph.thresholds,
            blockCount = if (adaptive) ADAPTIVE_BLOCKS else 1,
            staircase = if (adaptive) MultiSignalStaircaseEngine() else null,
            promptGenerator = generator,
            frameProbe = frameProbe,
            recorder = recorder,
        )
    }
    val state by holder.state.collectAsState()
    val scoreEngine = remember { SpeedScoreEngine() }
    var showDashboard by remember { mutableStateOf(false) }

    // Timing del bucle de trials. Foreperiodo y feedback usan delays; el estímulo solicita
    // el frame del Choreographer para fijar stimulusUptimeNanos.
    LaunchedEffect(state.phase, state.trialIndex, state.foreperiodMs) {
        when (state.phase) {
            SpeedPhase.Foreperiod -> {
                delay(state.foreperiodMs)
                holder.onForeperiodElapsed()
            }
            SpeedPhase.Feedback -> {
                delay(FEEDBACK_MS)
                holder.beginNextTrial()
            }
            else -> Unit
        }
    }
    LaunchedEffect(state.phase, state.trialIndex) {
        if (state.phase == SpeedPhase.Stimulus) {
            handle.requestStimulusFrame { holder.onStimulusShown(it) }
        }
    }

    PlatformBackHandler(enabled = true, onBack = onExit)

    val sessionMetrics = state.lastSession?.sessionMetrics
    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when {
            showDashboard && sessionMetrics != null ->
                SpeedDashboardScreen(scoreEngine.score(sessionMetrics), onBack = { showDashboard = false })
            state.phase == SpeedPhase.Idle ->
                IdleContent(exerciseId, onStart = holder::startSession, onBack = onExit)
            state.phase == SpeedPhase.Finished -> FinishedContent(
                state = state,
                exporter = exporter,
                userId = userId,
                graph = graph,
                dashboardAvailable = sessionMetrics != null,
                onOpenDashboard = { showDashboard = true },
                onRepeat = holder::startSession,
                onBack = onExit,
            )
            else -> ActiveContent(state, exerciseId, handle, holder, cipherKey)
        }
    }
}

/** Generador de estímulos por ejercicio (null = E1, RT simple sin elección). */
private fun promptGeneratorFor(exerciseId: SpeedExerciseId): SpeedPromptGenerator? = when (exerciseId) {
    SpeedExerciseId.E1_DESTELLO -> null
    SpeedExerciseId.E6_MULTIVIA -> MultiviaGenerator()
    SpeedExerciseId.E3_CIFRADO -> CifradoGenerator()
    SpeedExerciseId.E7_COMPUERTA -> CompuertaGenerator()
}

@Composable
private fun IdleContent(exerciseId: SpeedExerciseId, onStart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(titleFor(exerciseId), color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(instructionsFor(exerciseId), color = Color.White, fontSize = 15.sp)
        Text(SpeedStrings.MVP_NOTE, color = AppColors.FocusReadout, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        PillButton(SpeedStrings.START, onStart)
        TextLink(SpeedStrings.BACK, onClick = onBack)
    }
}

@Composable
private fun ActiveContent(
    state: SpeedUiState,
    exerciseId: SpeedExerciseId,
    handle: com.mindos.app.features.mindboost.speed.presentation.SpeedTimebaseHandle,
    holder: SpeedSessionStateHolder,
    cipherKey: Map<String, String>?,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            headerText(state),
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp),
        )
        Box(Modifier.fillMaxSize()) {
            when (exerciseId) {
                SpeedExerciseId.E1_DESTELLO -> E1Surface(state, handle, holder)
                SpeedExerciseId.E6_MULTIVIA,
                SpeedExerciseId.E3_CIFRADO,
                SpeedExerciseId.E7_COMPUERTA -> TwoChoiceSurface(state, handle, holder, cipherKey)
            }
        }
    }
}

@Composable
private fun E1Surface(
    state: SpeedUiState,
    handle: com.mindos.app.features.mindboost.speed.presentation.SpeedTimebaseHandle,
    holder: SpeedSessionStateHolder,
) {
    val tapEnabled = state.phase == SpeedPhase.Foreperiod || state.phase == SpeedPhase.Stimulus
    SpeedTapArea(
        handle = handle,
        enabled = tapEnabled,
        onResponse = { holder.onResponse(it, null) },
        modifier = Modifier.fillMaxSize().background(stimulusBackground(state.phase)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            StimulusCaption(state)
        }
    }
}

/**
 * Superficie genérica de 2 vías para E6/E3/E7. La señal ([SpeedUiState.prompt]) solo se
 * muestra en la fase Stimulus (no durante el foreperiodo); el lado correcto NO se resalta
 * (el usuario debe interpretar la señal). Para E3 se muestra la clave de cifrado arriba.
 */
@Composable
private fun TwoChoiceSurface(
    state: SpeedUiState,
    handle: com.mindos.app.features.mindboost.speed.presentation.SpeedTimebaseHandle,
    holder: SpeedSessionStateHolder,
    cipherKey: Map<String, String>?,
) {
    val tapEnabled = state.phase == SpeedPhase.Foreperiod || state.phase == SpeedPhase.Stimulus
    val prompt = state.prompt
    Column(Modifier.fillMaxSize()) {
        if (cipherKey != null) KeyRow(cipherKey)
        Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
            TwoChoiceCaption(state)
        }
        Row(Modifier.fillMaxSize()) {
            ChoicePanel(
                label = prompt?.leftLabel ?: SpeedStrings.LEFT,
                handle = handle,
                enabled = tapEnabled,
                onResponse = { holder.onResponse(it, SpeedChoice.LEFT) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
            ChoicePanel(
                label = prompt?.rightLabel ?: SpeedStrings.RIGHT,
                handle = handle,
                enabled = tapEnabled,
                onResponse = { holder.onResponse(it, SpeedChoice.RIGHT) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TwoChoiceCaption(state: SpeedUiState) {
    val (text, color) = when (state.phase) {
        SpeedPhase.Stimulus -> (state.prompt?.cue ?: SpeedStrings.TAP_NOW) to AppColors.FocusSignal
        SpeedPhase.Feedback -> feedbackText(state) to AppColors.FocusSignal
        else -> (if (state.showTooSoon) SpeedStrings.TOO_SOON else SpeedStrings.WAIT) to AppColors.FocusReadout
    }
    Text(text, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun KeyRow(cipherKey: Map<String, String>) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cipherKey.forEach { (symbol, value) ->
            Text("$symbol=$value", color = AppColors.FocusReadout, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ChoicePanel(
    label: String,
    handle: com.mindos.app.features.mindboost.speed.presentation.SpeedTimebaseHandle,
    enabled: Boolean,
    onResponse: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SpeedTapArea(
        handle = handle,
        enabled = enabled,
        onResponse = onResponse,
        modifier = modifier.background(AppColors.FocusCanvas),
    ) {
        Box(Modifier.fillMaxSize().border(1.dp, AppColors.FocusReadout.copy(alpha = 0.3f)), Alignment.Center) {
            Text(label, color = AppColors.FocusReadout, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StimulusCaption(state: SpeedUiState) {
    val (text, color) = when (state.phase) {
        SpeedPhase.Stimulus -> SpeedStrings.TAP_NOW to AppColors.FocusCanvas
        SpeedPhase.Feedback -> feedbackText(state) to AppColors.FocusSignal
        else -> (if (state.showTooSoon) SpeedStrings.TOO_SOON else SpeedStrings.WAIT) to AppColors.FocusReadout
    }
    Text(text, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

private fun feedbackText(state: SpeedUiState): String = when {
    state.lastWasAnticipation -> SpeedStrings.TOO_SOON
    state.lastWasLate -> SpeedStrings.LATE
    state.lastRtMs != null -> "${state.lastRtMs}${SpeedStrings.MILLIS_SUFFIX}"
    else -> ""
}

/** Cabecera operativa: bloque (si hay varios), progreso de trials y límite temporal. */
private fun headerText(state: SpeedUiState): String {
    val block = if (state.totalBlocks > 1) {
        "${SpeedStrings.BLOCK} ${state.blockIndex + 1}/${state.totalBlocks} · "
    } else {
        ""
    }
    val limit = state.deadlineMs?.let { " · ${SpeedStrings.LIMIT} $it${SpeedStrings.MILLIS_SUFFIX}" } ?: ""
    return "$block${state.trialIndex}/${state.totalTrials}$limit"
}

@Composable
private fun FinishedContent(
    state: SpeedUiState,
    exporter: DebugExporter,
    userId: String,
    graph: SpeedGraph,
    dashboardAvailable: Boolean,
    onOpenDashboard: () -> Unit,
    onRepeat: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            SpeedStrings.BLOCK_COMPLETED,
            color = AppColors.FocusSignal,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (state.sessionPersisted) SpeedStrings.DATA_SAVED else "…",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(6.dp))
        ResultsPanel(state)
        Spacer(Modifier.height(10.dp))
        if (dashboardAvailable) PillButton(SpeedStrings.DASHBOARD_OPEN, onOpenDashboard)
        DebugPanel(state, exporter, userId, graph)
        Spacer(Modifier.height(10.dp))
        PillButton(SpeedStrings.RUN_AGAIN, onRepeat)
        TextLink(SpeedStrings.BACK, onClick = onBack)
    }
}

@Composable
private fun ResultsPanel(state: SpeedUiState) {
    val metrics = state.blockMetrics
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ResultLine(SpeedStrings.LABEL_TRIALS, metrics?.totalTrials?.toString() ?: "0")
        ResultLine(SpeedStrings.LABEL_ACCURACY, metrics?.let { percent(it.pc) } ?: SpeedStrings.NOT_AVAILABLE)
        ResultLine(
            SpeedStrings.LABEL_MEDIAN_RT,
            metrics?.rtMedianNanos?.let { "${(it / 1_000_000.0).toLong()}${SpeedStrings.MILLIS_SUFFIX}" }
                ?: SpeedStrings.NOT_AVAILABLE,
        )
        ResultLine(
            SpeedStrings.LABEL_ANTICIPATIONS,
            metrics?.let { percent(it.anticipationRate) } ?: SpeedStrings.NOT_AVAILABLE,
        )
        ResultLine(SpeedStrings.LABEL_QUALITY, state.qualityLog?.sessionQuality?.name ?: SpeedStrings.NOT_AVAILABLE)
        ResultLine(SpeedStrings.LABEL_PRECISION, state.qualityLog?.deviceTimingRisk?.name ?: SpeedStrings.NOT_AVAILABLE)
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AppColors.FocusReadout, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DebugPanel(state: SpeedUiState, exporter: DebugExporter, userId: String, graph: SpeedGraph) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.FocusLure.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            SpeedStrings.DEBUG_PANEL_TITLE,
            color = AppColors.FocusLure,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        if (exporter.isEnabled) {
            TextLink(SpeedStrings.DEBUG_EXPORT, color = AppColors.FocusReadout) {
                val run = state.lastSession ?: return@TextLink
                scope.launch {
                    val result = exporter.export(run, metadataFor(state))
                    message = when (result) {
                        is DebugExportResult.Written -> result.jsonPath
                        is DebugExportResult.DisabledInRelease -> SpeedStrings.DEBUG_EXPORT_DISABLED
                        is DebugExportResult.Failed -> result.reason
                    }
                }
            }
        } else {
            Text(SpeedStrings.DEBUG_EXPORT_DISABLED, color = AppColors.FocusReadout, fontSize = 12.sp)
        }
        TextLink(SpeedStrings.DEBUG_DELETE, color = AppColors.FocusError) {
            scope.launch {
                val report = graph.store.deleteUserData(userId)
                message = "${SpeedStrings.DEBUG_DELETED} (${report.totalRows})"
            }
        }
        if (message.isNotBlank()) Text(message, color = AppColors.FocusReadout, fontSize = 11.sp)
    }
}

private fun metadataFor(state: SpeedUiState): DebugExportMetadata {
    val run = state.lastSession
    val metrics = run?.sessionMetrics
    return DebugExportMetadata(
        appVersion = APP_VERSION,
        metricsVersion = SpeedVersions.METRICS_VERSION,
        debugExportVersion = SpeedVersions.DEBUG_EXPORT_VERSION,
        timebase = run?.blocks?.firstOrNull()?.timebase
            ?: com.mindos.app.features.mindboost.speed.domain.Timebase.ANDROID_UI_UPTIME_NANOS,
        inputTimestampPrecision = metrics?.inputTimestampPrecision
            ?: com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision.MILLIS_DERIVED,
        deviceTimingRisk = state.qualityLog?.deviceTimingRisk
            ?: com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk.MODERATE,
        sessionQuality = state.qualityLog?.sessionQuality
            ?: com.mindos.app.features.mindboost.speed.domain.SessionQuality.LOW,
        thresholds = com.mindos.app.features.mindboost.speed.domain.MetricThresholds.DEFAULT,
    )
}

// ── UI primitives (estilo del MindBoostHubScreen) ────────────────────────────

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(AppColors.FocusSignal.copy(alpha = 0.16f))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = AppColors.FocusSignal, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TextLink(text: String, color: Color = AppColors.FocusReadout, onClick: () -> Unit) {
    Text(text, color = color, fontSize = 14.sp, modifier = Modifier.clickable { onClick() }.padding(8.dp))
}

private fun titleFor(exerciseId: SpeedExerciseId): String = when (exerciseId) {
    SpeedExerciseId.E1_DESTELLO -> SpeedStrings.E1_TITLE
    SpeedExerciseId.E3_CIFRADO -> SpeedStrings.E3_TITLE
    SpeedExerciseId.E6_MULTIVIA -> SpeedStrings.E6_TITLE
    SpeedExerciseId.E7_COMPUERTA -> SpeedStrings.E7_TITLE
}

private fun instructionsFor(exerciseId: SpeedExerciseId): String = when (exerciseId) {
    SpeedExerciseId.E1_DESTELLO -> SpeedStrings.E1_INSTRUCTIONS
    SpeedExerciseId.E3_CIFRADO -> SpeedStrings.E3_INSTRUCTIONS
    SpeedExerciseId.E6_MULTIVIA -> SpeedStrings.E6_INSTRUCTIONS
    SpeedExerciseId.E7_COMPUERTA -> SpeedStrings.E7_INSTRUCTIONS
}

private fun stimulusBackground(phase: SpeedPhase): Color =
    if (phase == SpeedPhase.Stimulus) AppColors.FocusSignal else AppColors.FocusCanvas

private fun percent(fraction: Double): String = "${(fraction * 100.0).toLong()}%"
