@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.logic.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppShapes
import com.mindos.app.design.AppSpacing
import com.mindos.app.features.mindboost.logic.domain.model.LogicSkill
import com.mindos.app.features.mindboost.logic.presentation.state.LogicProgressTrend
import com.mindos.app.features.mindboost.logic.presentation.state.LogicProgressUiState
import com.mindos.app.features.mindboost.logic.presentation.state.LogicSkillProgress
import kotlin.math.roundToInt

/**
 * Read-only Lógica progress screen (mirrors FocusProgressScreen): total sessions, latest average ICR + its
 * trend, and per-subcapacity recent ICR. Honest copy: nothing is rendered as 0 while calibrating, and the
 * closing note reminds that stable improvement needs isomorphs + retests, not one session.
 */
@Composable
fun LogicProgressScreen(state: LogicProgressUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.FocusCanvas)
            .statusBarsPadding()
            .padding(AppSpacing.ScreenHorizontal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.M),
    ) {
        Text("Progreso · Lógica", color = AppColors.FocusSignal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        when (state) {
            LogicProgressUiState.Loading -> Text("Cargando progreso…", color = AppColors.TextPrimary)
            is LogicProgressUiState.Ready -> ReadyContent(state)
        }
        Text(
            "Este resumen describe tu práctica reciente. La mejora estable se confirma con isomorfos y retests.",
            color = AppColors.TextMuted,
            fontSize = 13.sp,
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        Spacer(Modifier.height(AppSpacing.L))
    }
}

@Composable
private fun ReadyContent(state: LogicProgressUiState.Ready) {
    ProgressCard {
        Text(
            "${state.totalSessions} sesiones guardadas",
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            state.latestAverageIcr?.let { "ICR medio reciente: ${it.roundToInt()}" } ?: "ICR: en calibración",
            color = AppColors.FocusSignal,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(trendLabel(state.icrTrend), color = AppColors.FocusReadout, fontSize = 13.sp)
    }
    Text("Por subcapacidad", color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
    if (state.skills.isEmpty()) {
        Text(
            "Aún sin datos por subcapacidad — entrena para empezar.",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
    }
    state.skills.forEach { skill -> SkillProgressCard(skill) }
}

@Composable
private fun SkillProgressCard(skill: LogicSkillProgress) {
    ProgressCard {
        Text(skillName(skill.skillId), color = AppColors.TextPrimary)
        Text(
            skill.recentIcr?.let { "ICR reciente: ${it.roundToInt()} · ${skill.attempts} intentos" }
                ?: "En calibración · ${skill.attempts} intentos",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ProgressCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.S),
            content = { content() },
        )
    }
}

private fun trendLabel(trend: LogicProgressTrend): String = when (trend) {
    LogicProgressTrend.IMPROVING -> "Tendencia: mejorando ↗"
    LogicProgressTrend.STABLE -> "Tendencia: estable →"
    LogicProgressTrend.DECLINING -> "Tendencia: a la baja ↘"
    LogicProgressTrend.UNKNOWN -> "Tendencia: en calibración"
}

private fun skillName(skill: LogicSkill): String = skill.name.lowercase().replace('_', ' ')
