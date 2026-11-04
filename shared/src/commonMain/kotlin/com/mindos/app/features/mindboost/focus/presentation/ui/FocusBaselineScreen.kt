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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mindos.app.features.mindboost.focus.presentation.FocusBaselineStateHolder
import com.mindos.app.features.mindboost.focus.presentation.state.FocusBaselineSummary
import com.mindos.app.features.mindboost.focus.presentation.state.FocusBaselineUiState
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAtmosphere
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusMeasureRunningContent
import kotlin.math.roundToInt

/**
 * The initial baseline surface: fixed-difficulty calibration blocks + one reserved probe. It establishes
 * the user's individual thresholds, starting difficulty and the pre-snapshot. Honest by design — it never
 * frames the result as a score, only as today's starting point.
 */
@Composable
fun FocusBaselineScreen(
    holder: FocusBaselineStateHolder,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when (val state = holder.uiState) {
            is FocusBaselineUiState.Intro -> IntroContent(state, onStart = { holder.start() }, onBack = onExit)
            is FocusBaselineUiState.Running -> FocusMeasureRunningContent(
                model = state.model,
                onResponse = { holder.onResponse(it) },
                onAbort = { holder.abort(); onExit() },
            )
            is FocusBaselineUiState.Finished -> FinishedContent(state.summary, onDone = onExit)
            // Same message shape as an abort: nothing was published either way, and the user's next step
            // ("run it again") is the same. What differs is WHY, which the state carries.
            is FocusBaselineUiState.NotMeasurable -> AbortedContent(state.reason, onBack = onExit)
            is FocusBaselineUiState.Aborted -> AbortedContent(state.reason, onBack = onExit)
        }
    }
}

@Composable
private fun IntroContent(state: FocusBaselineUiState.Intro, onStart: () -> Unit, onBack: () -> Unit) {
    FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.7f)
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Tu base", color = AppColors.ProfileTextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "${state.blockCount} bloques · ~${state.estimatedMinutes} min",
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
        PrimaryButton(text = "Empezar mi base", onClick = onStart)
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
private fun FinishedContent(summary: FocusBaselineSummary, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Base registrada", color = AppColors.FocusSignal, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryRow("Umbral de lapso", "${summary.lapseRtMs} ms")
            SummaryRow(
                "Estabilidad basal (RT-CV)",
                summary.baselineRtCv?.let { "${(it * 100).roundToInt()}%" } ?: "—",
            )
            SummaryRow("Ejercicios calibrados", "${summary.calibratedExercises}")
            SummaryRow("Métricas de la prueba", "${summary.probeMetrics}")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            summary.note,
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryButton(text = "Continuar", onClick = onDone)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = AppColors.ProfileTextPrimary, fontSize = 14.sp)
        Text(value, color = AppColors.FocusSignal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AbortedContent(reason: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(reason, color = AppColors.ProfileTextPrimary, fontSize = 18.sp)
        Spacer(Modifier.height(18.dp))
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
