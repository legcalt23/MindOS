@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.reporting.MetricTrend
import com.mindos.app.features.mindboost.focus.domain.reporting.TrendDirection
import com.mindos.app.features.mindboost.focus.presentation.state.FocusProgressUiState
import kotlin.math.roundToInt

/**
 * Honest progress view (read-only): the composite Focus Score with its uncertainty band, and the
 * direction of each tracked metric. Never promises far transfer; near-transfer appears once
 * EVALUATION sessions (probes) exist.
 */
@Composable
fun FocusProgressScreen(state: FocusProgressUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when (state) {
            is FocusProgressUiState.Loading ->
                Text("Cargando…", color = AppColors.FocusReadout, modifier = Modifier.align(Alignment.Center))

            is FocusProgressUiState.Ready -> ReadyContent(state, onBack)
        }
    }
}

@Composable
private fun ReadyContent(state: FocusProgressUiState.Ready, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Progreso", color = AppColors.FocusSignal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${state.totalSessions} sesiones guardadas", color = AppColors.FocusReadout, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        FocusScoreCard(state)
        Text("Tendencias", color = AppColors.ProfileTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        state.trends.forEach { TrendRow(it) }
        Spacer(Modifier.height(8.dp))
        Text(
            "La transferencia (mejora en pruebas no entrenadas) aparecerá cuando completes evaluaciones.",
            color = AppColors.FocusReadout.copy(alpha = 0.7f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Text(
            "Volver",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onBack() }.padding(8.dp),
        )
    }
}

@Composable
private fun FocusScoreCard(state: FocusProgressUiState.Ready) {
    val score = state.focusScore
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("FOCUS SCORE", color = AppColors.FocusSignal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            text = score.value?.let { "${it.roundToInt()}" } ?: "—",
            color = AppColors.ProfileTextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        if (score.value != null && score.lowerBound != null && score.upperBound != null) {
            Text(
                "Banda: ${score.lowerBound.roundToInt()}–${score.upperBound.roundToInt()}",
                color = AppColors.FocusReadout,
                fontSize = 13.sp,
            )
        }
        Text(
            text = score.caption,
            color = if (score.confident) AppColors.FocusReadout else AppColors.Warning,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun TrendRow(trend: MetricTrend) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(metricLabel(trend.metric), color = AppColors.ProfileTextPrimary, fontSize = 14.sp)
        Text(trend.caption, color = directionColor(trend.direction), fontSize = 13.sp)
    }
}

private fun metricLabel(metric: MetricKey): String = when (metric) {
    MetricKey.ACCURACY -> "Precisión"
    MetricKey.RT_MEDIAN -> "Reacción (mediana)"
    MetricKey.RT_CV -> "Estabilidad (RT-CV)"
    MetricKey.SUSTAINED_FOCUS_INDEX -> "Índice de foco sostenido"
    else -> metric.name.lowercase().replace('_', ' ')
}

private fun directionColor(direction: TrendDirection) = when (direction) {
    TrendDirection.IMPROVING -> AppColors.FocusSignal
    TrendDirection.DECLINING -> AppColors.Warning
    TrendDirection.STABLE, TrendDirection.INSUFFICIENT -> AppColors.FocusReadout
}
