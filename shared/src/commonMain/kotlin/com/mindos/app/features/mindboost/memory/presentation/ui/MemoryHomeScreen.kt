@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.memory.presentation.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryExerciseCardUi
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryHomeUiState

/**
 * MindBoost · Memoria home. Honest by design: no promises of "photographic memory",
 * delayed-recall reminders surfaced, sub-capacities trained per exercise. The user
 * picks an exercise to start. Functional V1 (no animations).
 */
@Composable
fun MemoryHomeScreen(
    state: MemoryHomeUiState,
    onStart: (MemoryExerciseId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        when (state) {
            is MemoryHomeUiState.Loading ->
                Text("Cargando…", color = AppColors.FocusReadout, modifier = Modifier.align(Alignment.Center))

            is MemoryHomeUiState.Ready -> ReadyContent(state, onStart, onBack)
        }
    }
}

@Composable
private fun ReadyContent(
    state: MemoryHomeUiState.Ready,
    onStart: (MemoryExerciseId) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("MindBoost · Memoria", color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Hola, ${state.displayName}", color = Color.White, fontSize = 16.sp)
        Text(
            "Semana ${state.cycleWeek} · Perfil: ${state.archetypeName}",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
        if (state.dueDeferredCount > 0) {
            Text(
                "Tienes ${state.dueDeferredCount} repaso(s) pendiente(s) de sesiones anteriores.",
                color = AppColors.FocusMiss,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("Elige un ejercicio", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        state.exercises.forEach { card ->
            ExerciseCard(card, onClick = { onStart(card.exerciseId) })
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Volver",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onBack() }.padding(8.dp),
        )
    }
}

@Composable
private fun ExerciseCard(card: MemoryExerciseCardUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.FocusReadout.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(card.name, color = AppColors.FocusSignal, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(card.description, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}
