@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.decisiones.presentation.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.decisiones.presentation.DecisionCheckInStateHolder
import com.mindos.app.features.mindboost.decisiones.presentation.state.DecisionCheckInUiState
import com.mindos.app.features.mindboost.decisiones.presentation.state.GoNoGoStimulus
import com.mindverse.mindboost.core.session.CheckInInput
import kotlin.math.roundToInt
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/** Composition-scoped factory for [DecisionCheckInStateHolder] with a default Go/No-go sequence. */
@Composable
fun rememberDecisionCheckInStateHolder(
    seed: Long = DecisionCheckInStateHolder.DEFAULT_SEED,
): DecisionCheckInStateHolder = remember(seed) {
    DecisionCheckInStateHolder(stimuli = DecisionCheckInStateHolder.generateStimuli(seed = seed))
}

/**
 * Check-in (CRI / Go-No-go) screen. The holder owns all scoring; this screen only renders the
 * current stimulus, measures reaction time monotonically, and drives the per-trial timeout.
 */
@Composable
fun DecisionCheckInScreen(
    holder: DecisionCheckInStateHolder,
    onFinished: (CheckInInput) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.FocusCanvas)
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        when (val state = holder.uiState) {
            is DecisionCheckInUiState.Idle -> IntroContent(
                onStart = holder::start,
                onSelectFatigue = holder::setSubjectiveFatigue,
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center),
            )

            is DecisionCheckInUiState.Running -> RunningContent(
                state = state,
                onTap = holder::onTap,
                onTimeout = holder::onTimeout,
                modifier = Modifier.align(Alignment.Center),
            )

            is DecisionCheckInUiState.Finished -> FinishedContent(
                state = state,
                onContinue = { onFinished(state.checkInInput) },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun IntroContent(
    onStart: () -> Unit,
    onSelectFatigue: (Double) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Check-in", color = AppColors.ProfileTextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Toca cuando el círculo esté verde. No toques si está rojo. " +
                "Mide tu estado antes de decidir.",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "¿CÓMO LLEGAS HOY?",
            color = AppColors.FocusReadout,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        FatigueSelector(onSelect = onSelectFatigue)
        Spacer(Modifier.height(28.dp))
        CtaButton(text = "Empezar check-in", onClick = onStart)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Volver",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp),
        )
    }
}

@Composable
private fun FatigueSelector(onSelect: (Double) -> Unit) {
    val options = listOf("Cansado" to 0.2, "Normal" to 0.6, "Descansado" to 0.9)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (label, value) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.NeuCyan.copy(alpha = 0.06f))
                    .border(1.dp, AppColors.Stroke, RoundedCornerShape(14.dp))
                    .clickable { onSelect(value) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, color = AppColors.ProfileTextPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun RunningContent(
    state: DecisionCheckInUiState.Running,
    onTap: (Long) -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Re-marked each trial; the LaunchedEffect's stale timeout is cancelled when trialIndex changes.
    val mark = remember(state.trialIndex) { TimeSource.Monotonic.markNow() }
    LaunchedEffect(state.trialIndex) {
        delay(state.responseWindowMs)
        onTimeout()
    }

    val isGo = state.stimulus == GoNoGoStimulus.GO
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Prueba ${state.trialIndex + 1} / ${state.trialCount}",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(if (isGo) AppColors.Success else AppColors.Error)
                .clickable { onTap(mark.elapsedNow().inWholeMilliseconds) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isGo) "TOCA" else "ESPERA",
                color = AppColors.OnBright,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isGo) "Verde: toca lo más rápido que puedas." else "Rojo: no toques.",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FinishedContent(
    state: DecisionCheckInUiState.Finished,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "CRI", color = AppColors.FocusReadout, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${state.readiness.criValue.roundToInt()}",
            color = AppColors.FocusSignal,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Aciertos ${state.summary.correctResponses}/${state.summary.totalTrials}",
                color = AppColors.FocusReadout,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "RT medio ${state.summary.meanReactionMs} ms",
                color = AppColors.FocusReadout,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        CtaButton(text = "Continuar", onClick = onContinue)
    }
}

@Composable
private fun CtaButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.FocusSignal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.OnBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
