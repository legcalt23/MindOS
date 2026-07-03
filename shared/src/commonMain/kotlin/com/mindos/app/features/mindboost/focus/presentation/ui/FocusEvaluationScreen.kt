@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.reporting.TransferLine
import com.mindos.app.features.mindboost.focus.domain.reporting.TransferReport
import com.mindos.app.features.mindboost.focus.presentation.FocusEvaluationStateHolder
import com.mindos.app.features.mindboost.focus.presentation.state.FocusEvaluationUiState
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAtmosphere
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusMeasureRunningContent
import kotlin.math.roundToInt

/**
 * Evaluation surface: runs ONE reserved probe (fixed difficulty, EVALUATION mode) and shows the honest
 * near-transfer verdict against the baseline. Reliable changes (RCI) are the strongest evidence;
 * otherwise the result is preliminary. Never claims far transfer.
 */
@Composable
fun FocusEvaluationScreen(
    holder: FocusEvaluationStateHolder,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when (val state = holder.uiState) {
            is FocusEvaluationUiState.NeedsBaseline -> MessageContent("Necesitas tu base", state.reason, onExit)
            is FocusEvaluationUiState.Intro -> IntroContent(state, onStart = { holder.start() }, onBack = onExit)
            is FocusEvaluationUiState.Running -> FocusMeasureRunningContent(
                model = state.model,
                onResponse = { holder.onResponse(it) },
                onAbort = { holder.abort(); onExit() },
            )
            is FocusEvaluationUiState.Finished -> FinishedContent(state.transfer, onDone = onExit)
            is FocusEvaluationUiState.Aborted -> MessageContent("Evaluación detenida", state.reason, onExit)
        }
    }
}

@Composable
private fun IntroContent(state: FocusEvaluationUiState.Intro, onStart: () -> Unit, onBack: () -> Unit) {
    FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.7f)
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Evaluación", color = AppColors.ProfileTextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "${state.probeName} · ~${state.estimatedMinutes} min",
            color = AppColors.FocusSignal,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(26.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.instructions.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(AppColors.FocusSignal))
                    Spacer(Modifier.width(10.dp))
                    Text(line, color = AppColors.ProfileTextPrimary, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(34.dp))
        PrimaryButton(text = "Empezar evaluación", onClick = onStart)
        Spacer(Modifier.height(14.dp))
        Text(
            "Ahora no",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp),
        )
    }
}

@Composable
private fun FinishedContent(transfer: TransferReport, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Resultado", color = AppColors.FocusSignal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Pruebas NO entrenadas — la evidencia más honesta de cambio real.",
            color = AppColors.FocusReadout,
            fontSize = 12.sp,
        )
        if (transfer.strongEvidence.isNotEmpty()) {
            Text("Cambio fiable", color = AppColors.FocusSignal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            transfer.strongEvidence.forEach { TransferRow(it, strong = true) }
        }
        if (transfer.preliminary.isNotEmpty()) {
            Text(
                "Preliminar (aún no concluyente)",
                color = AppColors.Warning,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            transfer.preliminary.forEach { TransferRow(it, strong = false) }
        }
        Text(
            transfer.honestNote,
            color = AppColors.FocusReadout.copy(alpha = 0.75f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(text = "Continuar", onClick = onDone)
    }
}

@Composable
private fun TransferRow(line: TransferLine, strong: Boolean) {
    val accent = if (strong) AppColors.FocusSignal else AppColors.FocusReadout
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(line.label, color = AppColors.ProfileTextPrimary, fontSize = 13.sp)
        val delta = line.change.rawDelta
        val sign = if (delta > 0) "+" else ""
        Text("$sign${delta.roundToInt()}", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MessageContent(title: String, message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = AppColors.ProfileTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        Text(
            "Volver",
            color = AppColors.FocusSignal,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onBack).padding(10.dp),
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = AppColors.OnBright, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
