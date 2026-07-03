@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.memory.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.memory.presentation.DeferredRecallSessionStateHolder
import com.mindos.app.features.mindboost.memory.presentation.state.DeferredRecallUiState

/**
 * Renders the between-session 24h delayed-recall review driven by [DeferredRecallSessionStateHolder]:
 * load due items → recognise the studied words among lures (NO re-study) → honest retention summary.
 * Functional V1 (no animations). This screen is what makes EX3's episodic loop reachable in-app.
 */
@Composable
fun DeferredRecallScreen(
    holder: DeferredRecallSessionStateHolder,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DeferredRecallContent(
        state = holder.uiState,
        onSubmit = { holder.submitRecall(it) },
        onExit = onExit,
        modifier = modifier,
    )
}

/** Stateless content (testable without the holder): one screen per [DeferredRecallUiState]. */
@Composable
internal fun DeferredRecallContent(
    state: DeferredRecallUiState,
    onSubmit: (Set<String>) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("MindBoost · Repaso", color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            when (state) {
                is DeferredRecallUiState.Loading ->
                    Text("Cargando…", color = AppColors.FocusReadout, fontSize = 15.sp)

                is DeferredRecallUiState.NothingDue -> NothingDueContent(onExit)
                is DeferredRecallUiState.Recall -> RecallContent(state, onSubmit)
                is DeferredRecallUiState.Finished -> FinishedContent(state, onExit)
                is DeferredRecallUiState.Aborted -> MessageWithExit(state.reason, onExit)
            }
        }
    }
}

@Composable
private fun NothingDueContent(onExit: () -> Unit) {
    Text(
        "No tienes repasos pendientes ahora mismo. Los repasos aparecen un día después de estudiar.",
        color = Color.White,
        fontSize = 15.sp,
    )
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Volver", onExit)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecallContent(state: DeferredRecallUiState.Recall, onSubmit: (Set<String>) -> Unit) {
    var selected by remember(state.candidates) { mutableStateOf(emptySet<String>()) }
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
private fun FinishedContent(state: DeferredRecallUiState.Finished, onExit: () -> Unit) {
    Text("Recordados: ${state.recalled}/${state.studied}", color = Color.White, fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    Text(state.honestNote, color = AppColors.FocusReadout, fontSize = 13.sp)
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
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) AppColors.FocusSignal else Color.White, fontSize = 14.sp)
    }
}
