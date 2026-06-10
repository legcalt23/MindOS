@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.FocusSessionStateHolder
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import com.mindos.app.features.mindboost.focus.presentation.state.FocusPhase
import com.mindos.app.features.mindboost.focus.presentation.state.FocusRunningModel
import com.mindos.app.features.mindboost.focus.presentation.state.FocusSessionUiState
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAtmosphere
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusStimulusField

/**
 * Live E1 (Vigía) session surface. During the run the whole field is tappable;
 * a calm HUD shows progress and live counters without distracting from the task.
 */
@Composable
fun FocusSessionScreen(
    holder: FocusSessionStateHolder,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when (val state = holder.uiState) {
            is FocusSessionUiState.Intro -> IntroContent(
                state = state,
                onStart = { holder.start(state.template) },
                onBack = onExit,
            )

            is FocusSessionUiState.Running -> RunningContent(
                model = state.model,
                onTap = { holder.onTap() },
                onAbort = {
                    holder.abort()
                    onExit()
                },
            )

            is FocusSessionUiState.Finished -> FocusSummaryScreen(
                report = state.report,
                template = state.template,
                onDone = onExit,
                onRepeat = onRestart,
            )

            is FocusSessionUiState.Aborted -> AbortedContent(reason = state.reason, onBack = onExit)
        }
    }
}

@Composable
private fun IntroContent(
    state: FocusSessionUiState.Intro,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.7f)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.exerciseName,
            color = AppColors.ProfileTextPrimary,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = state.exerciseDescription,
            color = AppColors.ProfileTextPrimary.copy(alpha = 0.82f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        InstructionCard()
        Spacer(Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan)))
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Estoy listo · ${state.template.totalMinutes} min",
                color = AppColors.OnBright,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Salir",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp),
        )
    }
}

@Composable
private fun InstructionCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(AppColors.FocusSignal))
            Spacer(Modifier.width(10.dp))
            Text("Toca cuando veas el destello verde", color = AppColors.ProfileTextPrimary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(AppColors.FocusLure))
            Spacer(Modifier.width(10.dp))
            Text("Ignora el violeta — no lo toques", color = AppColors.ProfileTextPrimary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun RunningContent(
    model: FocusRunningModel,
    onTap: () -> Unit,
    onAbort: () -> Unit,
) {
    // Dim the ambient during the run so the focal element dominates attention.
    FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.4f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onTap() } },
    ) {
        FocusStimulusField(field = model.field, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            SessionHud(model = model, onAbort = onAbort)
        }

        // Contextual hint, prominent during warmup/idle, gone during a signal.
        val hint = when {
            model.phase == FocusPhase.WARMUP -> "Prepárate…"
            model.field is FocusFieldState.Idle -> "Mantén la mirada en el centro"
            model.field is FocusFieldState.Lure -> "No lo toques"
            else -> ""
        }
        val hintAlpha by animateFloatAsState(
            targetValue = if (hint.isEmpty()) 0f else 1f,
            animationSpec = tween(280),
            label = "hint",
        )
        Text(
            text = hint,
            color = AppColors.FocusReadout.copy(alpha = hintAlpha),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
        )
    }
}

@Composable
private fun SessionHud(model: FocusRunningModel, onAbort: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onAbort),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = AppColors.FocusReadout, fontSize = 16.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HudStat(value = "${model.hits}", label = "aciertos", color = AppColors.FocusHit)
            HudStat(value = "${model.lapses}", label = "lapsos", color = AppColors.FocusMiss)
            HudStat(value = "${model.falseAlarms}", label = "fallos", color = AppColors.FocusError)
        }
    }
    Spacer(Modifier.height(14.dp))
    ProgressBar(progress = model.progress)
}

@Composable
private fun HudStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = AppColors.FocusReadout.copy(alpha = 0.7f), fontSize = 9.sp)
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    val animated by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan))),
        )
    }
}

@Composable
private fun AbortedContent(reason: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = reason, color = AppColors.ProfileTextPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Volver",
            color = AppColors.FocusSignal,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onBack).padding(10.dp),
        )
    }
}
