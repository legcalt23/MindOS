@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber")

package com.mindos.app.features.mindboost.memory.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.presentation.MemorySessionStateHolder
import com.mindos.app.features.mindboost.memory.presentation.state.MemorySessionUiState

/**
 * Renders one memory session driven by [MemorySessionStateHolder]: intro → study →
 * recall (by selection) → finished. Functional V1 (no timed presentation/animations).
 */
@Composable
fun MemorySessionScreen(
    holder: MemorySessionStateHolder,
    exerciseId: MemoryExerciseId,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val state = holder.uiState) {
                is MemorySessionUiState.Intro -> IntroContent(state, onStart = { holder.start(exerciseId) })
                is MemorySessionUiState.Study -> StudyContent(state, onContinue = { holder.continueFromStudy() })
                is MemorySessionUiState.Processing ->
                    ProcessingContent(state, onSubmit = { holder.submitProcessing(it) })
                is MemorySessionUiState.Recall -> RecallContent(state, onSubmit = { holder.submitRecall(it) })
                is MemorySessionUiState.Finished -> FinishedContent(state, onExit = onExit)
                is MemorySessionUiState.Aborted -> MessageWithExit(state.reason, onExit)
            }
        }
    }
}

@Composable
private fun IntroContent(state: MemorySessionUiState.Intro, onStart: () -> Unit) {
    Text(state.exerciseName, color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(state.exerciseDescription, color = AppColors.FocusReadout, fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Empezar", onStart)
}

@Composable
private fun StudyContent(state: MemorySessionUiState.Study, onContinue: () -> Unit) {
    Text(state.exerciseName, color = AppColors.FocusSignal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Text(state.instruction, color = Color.White, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    state.items.forEach { item ->
        Text(item, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(vertical = 2.dp))
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Listo, recordar", onContinue)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProcessingContent(state: MemorySessionUiState.Processing, onSubmit: (List<Boolean>) -> Unit) {
    var answers by remember(state.tasks) { mutableStateOf(List<Boolean?>(state.tasks.size) { null }) }
    Text(state.exerciseName, color = AppColors.FocusSignal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Text(state.instruction, color = Color.White, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    state.tasks.forEachIndexed { index, prompt ->
        Text(prompt, color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Sí", answers[index] == true) { answers = answers.toMutableList().also { it[index] = true } }
            Chip("No", answers[index] == false) { answers = answers.toMutableList().also { it[index] = false } }
        }
    }
    Spacer(Modifier.height(12.dp))
    val allAnswered = answers.none { it == null }
    PrimaryButton(if (allAnswered) "Continuar" else "Responde cada una") {
        if (allAnswered) onSubmit(answers.map { it == true })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecallContent(state: MemorySessionUiState.Recall, onSubmit: (Set<String>) -> Unit) {
    var selected by remember(state.candidates) { mutableStateOf(emptySet<String>()) }
    Text(state.exerciseName, color = AppColors.FocusSignal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Text(state.instruction, color = Color.White, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.candidates.forEach { option ->
            val isOn = option.id in selected
            Chip(option.text, isOn) {
                selected = if (isOn) selected - option.id else selected + option.id
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Enviar", onClick = { onSubmit(selected) })
}

@Composable
private fun FinishedContent(state: MemorySessionUiState.Finished, onExit: () -> Unit) {
    val r = state.result
    Text(r.exerciseName, color = AppColors.FocusSignal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val pct = r.immediateAccuracyPercent
    Text(
        if (pct != null) "Acierto: $pct%  (${r.hits}/${r.studied})" else "Recordados: ${r.hits}/${r.studied}",
        color = Color.White,
        fontSize = 16.sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(r.honestNote, color = AppColors.FocusReadout, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Volver", onExit)
}

@Composable
private fun MessageWithExit(message: String, onExit: () -> Unit) {
    Text(message, color = AppColors.FocusReadout, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Volver", onExit)
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.FocusSignal.copy(alpha = 0.18f))
            .border(1.dp, AppColors.FocusSignal, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
    ) {
        Text(
            label,
            color = AppColors.FocusSignal,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) AppColors.FocusSignal else AppColors.FocusReadout.copy(alpha = 0.4f)
    val bg = if (selected) AppColors.FocusSignal.copy(alpha = 0.2f) else Color.Transparent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) AppColors.FocusSignal else Color.White, fontSize = 14.sp)
    }
}
