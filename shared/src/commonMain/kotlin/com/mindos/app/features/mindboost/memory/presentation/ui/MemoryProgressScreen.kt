@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.memory.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryExerciseProgress
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryProgressUiState

/**
 * Read-only Memoria progress screen (mirrors FocusProgressScreen): sessions recorded per implemented
 * exercise. Honest copy — counts are "registradas" (recorded), and the note makes clear that a first
 * reading needs many valid sessions; nothing is presented as a validated score here.
 */
@Composable
fun MemoryProgressScreen(state: MemoryProgressUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Progreso · Memoria", color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            when (state) {
                MemoryProgressUiState.Loading ->
                    Text("Cargando…", color = AppColors.FocusReadout, fontSize = 14.sp)

                is MemoryProgressUiState.Ready -> ReadyContent(state)
            }
            Text(
                "Volver",
                color = AppColors.FocusReadout,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onBack() }.padding(8.dp),
            )
        }
    }
}

@Composable
private fun ReadyContent(state: MemoryProgressUiState.Ready) {
    Text(
        "Sesiones registradas por ejercicio. La memoria necesita muchas sesiones (~20 válidas) " +
            "para una primera lectura.",
        color = AppColors.FocusReadout,
        fontSize = 13.sp,
    )
    state.exercises.forEach { ExerciseProgressCard(it) }
}

@Composable
private fun ExerciseProgressCard(item: MemoryExerciseProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.FocusReadout.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(item.name, color = AppColors.FocusSignal, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text("${item.recordedCount} sesiones registradas", color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}
