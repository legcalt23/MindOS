@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "TooManyFunctions")

package com.mindos.app.features.mindboost.language.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.language.domain.model.ResponseMode
import com.mindos.app.features.mindboost.language.domain.model.ResponsePayload
import com.mindos.app.features.mindboost.language.presentation.DemoItem
import com.mindos.app.features.mindboost.language.presentation.LanguageSessionStateHolder
import com.mindos.app.features.mindboost.language.presentation.LanguageStage
import com.mindos.app.features.mindboost.language.presentation.LanguageUiState
import com.mindos.app.features.mindboost.language.presentation.exercises.LanguageExercise

/**
 * Provisional, minimal screen for the MindBoost · Lenguaje track. Renders the [LanguageUiState] and
 * forwards built [ResponsePayload]s to the state holder, which runs the real cognitive loop. Supports
 * the three closed exercise models: single choice, evidence selection, and ordering.
 */
@Composable
fun LanguageRootScreen(
    userId: String,
    @Suppress("UNUSED_PARAMETER") displayName: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val holder = remember(userId) { LanguageSessionStateHolder(userId, menuMode = true) }
    val state by holder.state.collectAsState()

    PlatformBackHandler(
        enabled = true,
        onBack = { if (state.stage == LanguageStage.MENU) onExit() else holder.backToMenu() },
    )

    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(titleFor(state), color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            when (state.stage) {
                LanguageStage.MENU -> MenuView(holder.menu, onSelect = holder::start, onExit = onExit)
                LanguageStage.QUESTION -> QuestionView(state, onSubmit = holder::submit)
                LanguageStage.FEEDBACK -> FeedbackView(state, onNext = holder::next)
                LanguageStage.DONE -> DoneView(
                    state,
                    onRestart = holder::restart,
                    onBackToMenu = holder::backToMenu,
                    onExit = onExit,
                )
            }
            Spacer(Modifier.height(6.dp))
            if (state.stage == LanguageStage.QUESTION || state.stage == LanguageStage.FEEDBACK) {
                Text(
                    "Volver al menú",
                    color = AppColors.FocusReadout,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { holder.backToMenu() }.padding(6.dp),
                )
            }
        }
    }
}

private fun titleFor(state: LanguageUiState): String = when (state.stage) {
    LanguageStage.MENU -> "Lenguaje"
    else -> when {
        state.exerciseTitle.isNotBlank() -> "Lenguaje · ${state.exerciseTitle}"
        state.item != null -> "Lenguaje · ${state.item.primaryTrait.display}"
        else -> "Lenguaje"
    }
}

@Composable
private fun ColumnScope.MenuView(
    exercises: List<LanguageExercise>,
    onSelect: (LanguageExercise) -> Unit,
    onExit: () -> Unit,
) {
    Text(
        "Elige un ejercicio: cada uno entrena una habilidad lingüística distinta.",
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
    )
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        exercises.forEach { exercise ->
            ExerciseCard(exercise.title, exercise.subtitle) { onSelect(exercise) }
        }
    }
    Text(
        "Salir",
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
        modifier = Modifier.clickable { onExit() }.padding(6.dp),
    )
}

@Composable
private fun ExerciseCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = AppColors.FocusSignal, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = AppColors.FocusReadout, fontSize = 13.sp)
    }
}

@Composable
private fun QuestionView(state: LanguageUiState, onSubmit: (ResponsePayload) -> Unit) {
    val item = state.item ?: return
    Text("Ítem ${state.itemNumber} de ${state.totalItems}", color = AppColors.FocusReadout, fontSize = 13.sp)
    Text(item.prompt, color = Color.White, fontSize = 17.sp)
    Spacer(Modifier.height(2.dp))
    when (item.responseMode) {
        ResponseMode.MULTIPLE_CHOICE -> item.options.forEach { o ->
            SelectableRow(o.text, selected = false) { onSubmit(ResponsePayload.MultipleChoice(o.id)) }
        }
        ResponseMode.MULTIPLE_CHOICE_WITH_EVIDENCE_SELECTION -> EvidenceSelectionView(item, onSubmit)
        ResponseMode.ORDERING -> OrderingView(item, onSubmit)
        else -> Text("Formato no disponible en esta demo.", color = AppColors.FocusReadout, fontSize = 14.sp)
    }
}

@Composable
private fun EvidenceSelectionView(item: DemoItem, onSubmit: (ResponsePayload) -> Unit) {
    var selectedOption by remember(item.taskId) { mutableStateOf<String?>(null) }
    val selectedEvidence = remember(item.taskId) { mutableStateListOf<String>() }

    Text("1) Elige la conclusión:", color = AppColors.FocusReadout, fontSize = 13.sp)
    item.options.forEach { o ->
        SelectableRow(o.text, selected = selectedOption == o.id) { selectedOption = o.id }
    }
    Spacer(Modifier.height(4.dp))
    Text("2) Marca su evidencia:", color = AppColors.FocusReadout, fontSize = 13.sp)
    item.evidence.forEach { e ->
        SelectableRow(e.text, selected = selectedEvidence.contains(e.id)) {
            if (!selectedEvidence.remove(e.id)) selectedEvidence.add(e.id)
        }
    }
    Spacer(Modifier.height(6.dp))
    PrimaryButton("Confirmar", enabled = selectedOption != null) {
        selectedOption?.let { onSubmit(ResponsePayload.EvidenceSelection(it, selectedEvidence.toList())) }
    }
}

@Composable
private fun OrderingView(item: DemoItem, onSubmit: (ResponsePayload) -> Unit) {
    val order = remember(item.taskId) { mutableStateListOf<String>() }

    Text("Toca en orden, de menos a más justificada:", color = AppColors.FocusReadout, fontSize = 13.sp)
    item.options.forEach { o ->
        val pos = order.indexOf(o.id)
        val label = if (pos >= 0) "${pos + 1}.  ${o.text}" else o.text
        SelectableRow(label, selected = pos >= 0) { if (pos < 0) order.add(o.id) }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Reiniciar orden",
        color = AppColors.FocusReadout,
        fontSize = 13.sp,
        modifier = Modifier.clickable { order.clear() }.padding(6.dp),
    )
    PrimaryButton("Confirmar", enabled = order.size == item.options.size) {
        onSubmit(ResponsePayload.Ordering(order.toList()))
    }
}

@Composable
private fun FeedbackView(state: LanguageUiState, onNext: () -> Unit) {
    val correct = state.lastAnswerCorrect == true
    Text(
        if (correct) "✓ Correcto" else "✗ Incorrecto",
        color = if (correct) AppColors.FocusSignal else Color(0xFFE0A23B),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Text(state.feedback, color = Color.White, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    PrimaryButton(if (state.itemNumber >= state.totalItems) "Ver resultado" else "Siguiente", onClick = onNext)
}

@Composable
private fun DoneView(
    state: LanguageUiState,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit,
    onExit: () -> Unit,
) {
    Text("Sesión completada", color = AppColors.FocusSignal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Text("Aciertos: ${state.accuracyPercent ?: 0}%", color = Color.White, fontSize = 15.sp)
    Spacer(Modifier.height(4.dp))
    Text("Por habilidad:", color = AppColors.FocusReadout, fontSize = 13.sp)
    state.skillLines.forEach { line ->
        Text("· $line", color = Color.White, fontSize = 14.sp)
    }
    Spacer(Modifier.height(6.dp))
    Text(state.summaryNote, color = AppColors.FocusReadout, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Repetir ejercicio", onClick = onRestart)
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Elegir otro ejercicio", onClick = onBackToMenu)
    Spacer(Modifier.height(8.dp))
    Text(
        "Salir",
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
        modifier = Modifier.clickable { onExit() }.padding(6.dp),
    )
}

@Composable
private fun SelectableRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) AppColors.FocusSignal else AppColors.FocusSignal.copy(alpha = 0.5f)
    val fill = if (selected) AppColors.FocusSignal.copy(alpha = 0.18f) else Color.Transparent
    Text(
        text,
        color = Color.White,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(13.dp),
    )
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val fill = if (enabled) AppColors.FocusSignal else AppColors.FocusSignal.copy(alpha = 0.35f)
    Text(
        text,
        color = AppColors.FocusCanvas,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(14.dp),
    )
}
