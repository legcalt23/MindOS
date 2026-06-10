@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.session.SessionTemplate
import com.mindos.app.features.mindboost.focus.presentation.state.FocusHomeUiState
import com.mindos.app.features.mindboost.focus.presentation.ui.components.FocusAtmosphere
import kotlin.math.roundToInt

/**
 * Premium MindBoost · Enfoque home. Honest by design: the focus index shows its
 * uncertainty, and nothing promises far transfer. The user picks a session
 * length and starts the recommended exercise (today: Vigía).
 */
@Composable
fun FocusHomeScreen(
    state: FocusHomeUiState,
    onStart: (SessionTemplate) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.8f)

        when (state) {
            is FocusHomeUiState.Loading -> LoadingPulse(modifier = Modifier.align(Alignment.Center))
            is FocusHomeUiState.Ready -> ReadyContent(state = state, onStart = onStart, onBack = onBack)
        }
    }
}

@Composable
private fun ReadyContent(
    state: FocusHomeUiState.Ready,
    onStart: (SessionTemplate) -> Unit,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf(SessionTemplate.STANDARD_10) }
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scroll)
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        TopBar(onBack = onBack)

        Spacer(Modifier.height(34.dp))
        Text(
            text = "MindBoost",
            color = AppColors.ProfileTextPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ENFOQUE",
                color = AppColors.FocusSignal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "· Semana ${state.cycleWeek} de 8",
                color = AppColors.FocusReadout,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
        StatsRow(state = state)

        Spacer(Modifier.height(24.dp))
        RecommendedCard(state = state)

        Spacer(Modifier.height(22.dp))
        Text(
            text = "DURACIÓN",
            color = AppColors.FocusReadout,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        DurationSelector(selected = selected, onSelect = { selected = it })

        Spacer(Modifier.height(28.dp))
        StartButton(template = selected, onClick = { onStart(selected) })

        Spacer(Modifier.height(20.dp))
        Text(
            text = "El progreso dentro del ejercicio es solo una señal. La mejora real se " +
                "confirma con pruebas no entrenadas. Aquí no se promete inteligencia ni " +
                "concentración general — se entrena y se mide con honestidad.",
            color = AppColors.FocusReadout.copy(alpha = 0.7f),
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", color = AppColors.ProfileTextPrimary, fontSize = 24.sp)
        }
        Text(
            text = "MINDVERSE",
            color = AppColors.FocusReadout.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatsRow(state: FocusHomeUiState.Ready) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatChip(value = "${state.streakDays}", label = "RACHA", modifier = Modifier.weight(1f))
        StatChip(value = "${state.totalSessions}", label = "SESIONES", modifier = Modifier.weight(1f))
        StatChip(
            value = state.lastSustainedFocusIndex?.let { "${it.roundToInt()}" } ?: "—",
            label = "FOCO",
            modifier = Modifier.weight(1f),
            highlighted = state.lastSustainedFocusIndex != null,
        )
    }
}

@Composable
private fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(18.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = if (highlighted) AppColors.FocusSignal else AppColors.ProfileTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = AppColors.FocusReadout, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RecommendedCard(state: FocusHomeUiState.Ready) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        AppColors.FocusAuroraA.copy(alpha = 0.45f),
                        AppColors.FocusAuroraB.copy(alpha = 0.30f),
                    ),
                ),
            )
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
            .padding(22.dp),
    ) {
        Text(
            text = if (state.needsBaseline) "EMPIEZA POR TU BASE" else "HOY ENTRENAS",
            color = AppColors.FocusSignal,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = state.recommendedExerciseName,
            color = AppColors.ProfileTextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.recommendedDescription,
            color = AppColors.ProfileTextPrimary.copy(alpha = 0.82f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun DurationSelector(selected: SessionTemplate, onSelect: (SessionTemplate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SessionTemplate.entries.forEach { template ->
            val isSelected = template == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) AppColors.FocusSignal.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
                    )
                    .border(
                        1.dp,
                        if (isSelected) AppColors.FocusSignal.copy(alpha = 0.7f) else AppColors.Stroke,
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(template) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${template.totalMinutes}",
                    color = if (isSelected) AppColors.FocusSignal else AppColors.ProfileTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = "min", color = AppColors.FocusReadout, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun StartButton(template: SessionTemplate, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Empezar · ${template.displayName}",
            color = AppColors.OnBright,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LoadingPulse(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "loading")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    Text(
        text = "Cargando…",
        color = AppColors.FocusReadout,
        fontSize = 15.sp,
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
}
