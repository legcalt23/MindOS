@file:Suppress("FunctionNaming", "MagicNumber", "LongMethod", "TooManyFunctions")

package com.mindos.app.features.mindboost.speed.presentation.e8

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.mindos.app.features.mindboost.speed.di.SpeedGraph
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoFeedbackKind
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoMetrics
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoScore
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoSignal
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import kotlinx.coroutines.delay
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val FEEDBACK_MS = 550L

/**
 * E8 · Inhibición Rápida (Go/No-Go Velocity). Pantalla dedicada (el paradigma NO-GO no encaja en la superficie
 * RT-tap compartida): un área táctil única; el usuario TOCA en la señal de avance (GO) e INHIBE en la de freno
 * (NO-GO) antes del deadline. Mide el RT desde el onset con `TimeSource.Monotonic` (stdlib) y alimenta la state
 * machine pura [GoNoGoSessionStateHolder]. El score es PRELIMINAR (§7).
 */
@Composable
fun E8InhibicionScreen(
    graph: SpeedGraph,
    userId: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val holder = remember(graph, userId) {
        GoNoGoSessionStateHolder(
            scope = scope,
            userId = userId,
            telemetryStore = graph.telemetryStore,
            clock = graph.clock,
        )
    }
    val state by holder.state.collectAsState()
    var onset by remember { mutableStateOf<TimeMark?>(null) }

    LaunchedEffect(state.phase, state.trialIndex) {
        when (state.phase) {
            GoNoGoPhase.Foreperiod -> {
                val fp = state.currentStimulus?.foreperiodMs ?: 0L
                delay(fp)
                holder.onStimulusShown()
            }
            GoNoGoPhase.Stimulus -> {
                onset = TimeSource.Monotonic.markNow()
                val stimulus = state.currentStimulus
                if (stimulus != null) {
                    delay(stimulus.deadlineMs + GoNoGoSessionStateHolder.LATE_GRACE_MS)
                    holder.onResponseWindowElapsed()
                }
            }
            GoNoGoPhase.Feedback -> {
                delay(FEEDBACK_MS)
                holder.beginNextTrial()
            }
            else -> Unit
        }
    }

    PlatformBackHandler(enabled = true, onBack = onExit)

    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when (state.phase) {
            GoNoGoPhase.Idle -> IdleContent(onStart = holder::startSession, onBack = onExit)
            GoNoGoPhase.Finished -> FinishedContent(state, onRepeat = holder::startSession, onBack = onExit)
            else -> ActiveContent(
                state = state,
                onTap = {
                    when (state.phase) {
                        GoNoGoPhase.Foreperiod -> holder.onPrematureTap()
                        GoNoGoPhase.Stimulus -> holder.onStimulusTap(onset?.elapsedNow()?.inWholeMilliseconds ?: 0L)
                        else -> Unit
                    }
                },
            )
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(SpeedStrings.E8_TITLE, color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(SpeedStrings.E8_INSTRUCTIONS, color = Color.White, fontSize = 15.sp)
        Text(SpeedStrings.MVP_NOTE, color = AppColors.FocusReadout, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        PillButton(SpeedStrings.START, onStart)
        TextLink(SpeedStrings.BACK, onBack)
    }
}

@Composable
private fun ActiveContent(state: GoNoGoUiState, onTap: () -> Unit) {
    val stimulus = state.currentStimulus
    val isGo = stimulus != null && stimulus.stimulusFamily == "go"
    val (bg, caption) = when (state.phase) {
        GoNoGoPhase.Stimulus -> if (isGo) {
            AppColors.FocusSignal to SpeedStrings.GNG_SIGNAL_GO
        } else {
            AppColors.FocusError to SpeedStrings.GNG_SIGNAL_NO_GO
        }
        GoNoGoPhase.Feedback -> AppColors.FocusCanvas to feedbackCaption(state)
        else -> AppColors.FocusCanvas to SpeedStrings.WAIT
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "${state.trialIndex + 1}/${state.totalTrials}",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp),
        )
        Box(
            Modifier.fillMaxSize().background(bg).clickable { onTap() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                caption,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun feedbackCaption(state: GoNoGoUiState): String {
    val outcome = state.lastOutcome ?: return ""
    return if (outcome.correct) SpeedStrings.GNG_OK else SpeedStrings.GNG_MISS
}

@Composable
private fun FinishedContent(state: GoNoGoUiState, onRepeat: () -> Unit, onBack: () -> Unit) {
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
            if (state.summaryPersisted) SpeedStrings.DATA_SAVED else "…",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
        state.feedback?.let { FeedbackPanel(goNoGoFeedbackText(it)) }
        Spacer(Modifier.height(6.dp))
        state.metrics?.let { ResultsPanel(it, state.score) }
        Spacer(Modifier.height(10.dp))
        PillButton(SpeedStrings.RUN_AGAIN, onRepeat)
        TextLink(SpeedStrings.BACK, onBack)
    }
}

@Composable
private fun ResultsPanel(metrics: GoNoGoMetrics, score: GoNoGoScore?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ResultLine(SpeedStrings.GNG_LABEL_GO_ACCURACY, percentOrDash(metrics.goAccuracy))
        ResultLine(SpeedStrings.GNG_LABEL_NO_GO_ACCURACY, percentOrDash(metrics.noGoAccuracy))
        ResultLine(SpeedStrings.GNG_LABEL_COMMISSION, percentOrDash(metrics.commissionRate))
        ResultLine(SpeedStrings.GNG_LABEL_OMISSION, percentOrDash(metrics.omissionRate))
        ResultLine(SpeedStrings.GNG_LABEL_CONTROL, percentOrDash(metrics.inhibitoryControlIndex))
        ResultLine(SpeedStrings.DASHBOARD_TOTAL, indexOrDash(score))
    }
}

private fun percentOrDash(signal: GoNoGoSignal): String =
    (signal as? GoNoGoSignal.Measured)?.let { "${(it.value * 100.0).toLong()}%" } ?: SpeedStrings.NOT_AVAILABLE

private fun indexOrDash(score: GoNoGoScore?): String =
    score?.index?.let { "${(it * 100.0).toLong()}/100 (preliminar)" } ?: SpeedStrings.NOT_AVAILABLE

@Composable
private fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AppColors.FocusReadout, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeedbackPanel(text: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            SpeedStrings.FEEDBACK_TITLE,
            color = AppColors.FocusSignal,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text, color = Color.White, fontSize = 14.sp)
    }
}

private fun goNoGoFeedbackText(kind: GoNoGoFeedbackKind): String = when (kind) {
    GoNoGoFeedbackKind.CALIBRATING -> SpeedStrings.GNG_FEEDBACK_CALIBRATING
    GoNoGoFeedbackKind.HIGH_COMMISSION -> SpeedStrings.GNG_FEEDBACK_HIGH_COMMISSION
    GoNoGoFeedbackKind.HIGH_OMISSION -> SpeedStrings.GNG_FEEDBACK_HIGH_OMISSION
    GoNoGoFeedbackKind.HIGH_ANTICIPATION -> SpeedStrings.GNG_FEEDBACK_HIGH_ANTICIPATION
    GoNoGoFeedbackKind.UNSTABLE -> SpeedStrings.GNG_FEEDBACK_UNSTABLE
    GoNoGoFeedbackKind.STRONG_CONTROL_GOOD_SPEED -> SpeedStrings.GNG_FEEDBACK_STRONG_CONTROL_GOOD_SPEED
    GoNoGoFeedbackKind.STRONG_CONTROL_REDUCE_LATENCY -> SpeedStrings.GNG_FEEDBACK_STRONG_CONTROL_REDUCE_LATENCY
    GoNoGoFeedbackKind.BALANCED -> SpeedStrings.GNG_FEEDBACK_BALANCED
}

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
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
        modifier = Modifier.clickable { onClick() }.padding(8.dp),
    )
}
