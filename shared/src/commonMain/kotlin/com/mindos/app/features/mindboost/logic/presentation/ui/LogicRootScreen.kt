@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "TooManyFunctions")

package com.mindos.app.features.mindboost.logic.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.mindos.app.core.navigation.PlatformBackHandler
import com.mindos.app.design.AppColors
import com.mindos.app.design.AppShapes
import com.mindos.app.design.AppSpacing
import com.mindos.app.features.mindboost.logic.di.LogicGraph
import com.mindos.app.features.mindboost.logic.di.rememberLogicGraph
import com.mindos.app.features.mindboost.logic.domain.model.JustificationClaim
import com.mindos.app.features.mindboost.logic.domain.model.LogicConfidence
import com.mindos.app.features.mindboost.logic.domain.model.LogicErrorTag
import com.mindos.app.features.mindboost.logic.domain.model.LogicExerciseType
import com.mindos.app.features.mindboost.logic.domain.model.ReasoningRule
import com.mindos.app.features.mindboost.logic.domain.model.SkillState
import com.mindos.app.features.mindboost.logic.presentation.LogicStateHolder
import com.mindos.app.features.mindboost.logic.presentation.rememberLogicProgressStateHolder
import com.mindos.app.features.mindboost.logic.presentation.rememberLogicStateHolder
import com.mindos.app.features.mindboost.logic.presentation.state.LogicUiState
import kotlin.math.roundToInt

@Composable
fun LogicRootScreen(
    userId: String,
    displayName: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    graph: LogicGraph = rememberLogicGraph(),
) {
    val holder = rememberLogicStateHolder(graph, userId, displayName)
    LaunchedEffect(holder) { holder.loadHome() }
    var showProgress by remember { mutableStateOf(false) }
    val progressHolder = rememberLogicProgressStateHolder(graph)
    LaunchedEffect(showProgress) { if (showProgress) progressHolder.load(userId) }

    val state = holder.uiState
    PlatformBackHandler(enabled = true) {
        when {
            showProgress -> showProgress = false
            state is LogicUiState.Home || state is LogicUiState.Loading -> onExit()
            else -> holder.returnHome()
        }
    }

    if (showProgress) {
        LogicProgressScreen(progressHolder.uiState, onBack = { showProgress = false }, modifier = modifier)
    } else {
        LogicSurface(modifier) {
            when (state) {
                LogicUiState.Loading -> CenteredMessage("Preparando tu mapa de razonamiento…")
                is LogicUiState.Home -> LogicHomeScreen(state, holder::startSession, { showProgress = true }, onExit)
                is LogicUiState.Question -> LogicQuestionScreen(state, holder)
                is LogicUiState.Feedback -> LogicFeedbackScreen(state, holder::continueSession)
                is LogicUiState.Summary -> LogicSummaryScreen(state, holder::returnHome)
                is LogicUiState.Error -> ErrorScreen(state.message, holder::returnHome)
            }
        }
    }
}

@Composable
private fun LogicSurface(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.FocusCanvas)
            .statusBarsPadding()
            .padding(AppSpacing.ScreenHorizontal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.M),
    ) {
        content()
        Spacer(Modifier.height(AppSpacing.L))
    }
}

@Composable
private fun LogicHomeScreen(
    state: LogicUiState.Home,
    onStart: () -> Unit,
    onProgress: () -> Unit,
    onBack: () -> Unit,
) {
    Text("MindBoost · Lógica", color = AppColors.FocusSignal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Text(
        "Entrena razonamiento, justificación, contraejemplos y confianza. No promete subir tu CI.",
        color = AppColors.TextPrimary,
        fontSize = 15.sp,
    )
    LogicCard {
        Text("Hola, ${state.displayName}", color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(
            "${state.completedSessions} sesiones guardadas · Progreso basado en tu propio historial.",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
        Text(
            "La transferencia se mostrará como “en calibración” hasta resolver estructuras isomorfas.",
            color = AppColors.Warning,
            fontSize = 13.sp,
        )
    }
    if (state.skillStates.isNotEmpty()) {
        Text("Subcapacidades recientes", color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        state.skillStates.take(4).forEach { SkillRow(it) }
    }
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text("Empezar sesión")
    }
    TextButton(onClick = onProgress, modifier = Modifier.fillMaxWidth()) { Text("Ver progreso") }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
}

@Composable
private fun SkillRow(state: SkillState) {
    LogicCard {
        Text(skillLabel(state), color = AppColors.TextPrimary)
        Text(
            state.recentIcr?.let { "ICR reciente: ${it.roundToInt()}" } ?: "En calibración",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
    }
}

private const val LOGIC_STEPS = 3

/**
 * Step wizard: the reasoning task is split into 3 focused screens (task → reasoning → confidence) instead
 * of one dense wall of inputs. Selections still accumulate in the holder, so scoring is unchanged; the
 * wizard only controls WHICH inputs are visible and gates advancement so a validation failure can only
 * surface on the step that owns the missing input.
 */
@Composable
private fun LogicQuestionScreen(state: LogicUiState.Question, holder: LogicStateHolder) {
    var step by remember(state.itemIndex) { mutableStateOf(0) }

    Text(
        "Inferencia ${state.itemIndex}/${state.itemCount}",
        color = AppColors.FocusSignal,
        fontWeight = FontWeight.Bold,
    )
    LinearProgressIndicator(
        progress = { state.itemIndex.toFloat() / state.itemCount },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        "Paso ${step + 1} de $LOGIC_STEPS · ${stepTitle(step)}",
        color = AppColors.FocusReadout,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )

    when (step) {
        0 -> LogicTaskStep(state, holder)
        1 -> LogicReasoningStep(state, holder)
        else -> LogicConfidenceStep(state, holder)
    }

    state.visibleHint?.let {
        LogicCard {
            Text("Pista", color = AppColors.Warning, fontWeight = FontWeight.SemiBold)
            Text(it, color = AppColors.TextPrimary)
        }
    }
    state.validationMessage?.let { Text(it, color = AppColors.Error, fontSize = 13.sp) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.S)) {
        if (step > 0) {
            OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) { Text("Atrás") }
        }
        OutlinedButton(onClick = holder::useHint, modifier = Modifier.weight(1f)) { Text("Pista") }
        if (step < LOGIC_STEPS - 1) {
            Button(
                onClick = { step++ },
                enabled = canAdvanceLogicStep(step, state),
                modifier = Modifier.weight(1f),
            ) { Text("Siguiente") }
        } else {
            Button(
                onClick = holder::submitAnswer,
                enabled = state.confidence != null,
                modifier = Modifier.weight(1f),
            ) { Text("Responder") }
        }
    }
}

private fun stepTitle(step: Int): String = when (step) {
    0 -> "La tarea"
    1 -> "Tu razonamiento"
    else -> "Tu confianza"
}

private fun canAdvanceLogicStep(step: Int, state: LogicUiState.Question): Boolean = when (step) {
    0 -> state.selectedAnswerId != null
    else -> state.selectedRule != null && state.selectedClaim != null
}

@Composable
private fun LogicTaskStep(state: LogicUiState.Question, holder: LogicStateHolder) {
    Text(
        state.item.prompt,
        color = AppColors.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Text("Premisas: selecciona las que sostienen tu razonamiento", color = AppColors.FocusReadout)
    state.item.premises.forEach { premise ->
        SelectableRow(
            text = premise.text,
            selected = premise.id in state.selectedPremiseIds,
            onClick = { holder.togglePremise(premise.id) },
            checkbox = true,
        )
    }
    Text("Respuesta", color = AppColors.FocusReadout)
    state.item.answerOptions.forEach { option ->
        SelectableRow(
            text = option.text,
            selected = option.id == state.selectedAnswerId,
            onClick = { holder.selectAnswer(option.id) },
        )
    }
}

@Composable
private fun LogicReasoningStep(state: LogicUiState.Question, holder: LogicStateHolder) {
    ChoiceSection(
        title = "Regla aplicada",
        choices = state.item.validReasoningRules,
        selected = state.selectedRule,
        label = ::ruleLabel,
        onSelect = holder::selectRule,
    )
    ChoiceSection(
        title = "Justificación estructurada",
        choices = claimChoices(state.item.exerciseType),
        selected = state.selectedClaim,
        label = ::claimLabel,
        onSelect = holder::selectClaim,
    )
}

@Composable
private fun LogicConfidenceStep(state: LogicUiState.Question, holder: LogicStateHolder) {
    ChoiceSection(
        title = "Confianza",
        choices = LogicConfidence.entries,
        selected = state.confidence,
        label = ::confidenceLabel,
        onSelect = holder::selectConfidence,
    )
}

@Composable
private fun LogicFeedbackScreen(state: LogicUiState.Feedback, onContinue: () -> Unit) {
    val score = state.attempt.scoreBreakdown
    Text(
        state.feedback.title,
        color = if (state.attempt.isCorrect) AppColors.Success else AppColors.Warning,
        fontSize = 22.sp,
    )
    LogicCard {
        Text(
            "ICR ${score.totalIcr.roundToInt()}/100",
            color = AppColors.FocusSignal,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        MetricLine("Respuesta final", score.finalAnswerScore)
        MetricLine("Premisas", score.premiseIdentificationScore)
        MetricLine("Regla", score.ruleApplicationScore)
        MetricLine("Justificación", score.justificationScore)
        MetricLine("Calibración", score.confidenceCalibrationScore)
        MetricLine("Independencia de pistas", score.independenceFromHintsScore)
        Text(
            score.transferConsistencyScore?.let { "Transferencia: ${(it * 100).roundToInt()}%" }
                ?: "Transferencia: en calibración",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
    }
    LogicCard {
        Text(state.feedback.diagnostic, color = AppColors.TextPrimary)
        Text(state.feedback.nextStep, color = AppColors.FocusReadout)
    }
    if (state.attempt.errorTags.isNotEmpty()) {
        Text(
            "Patrones: ${state.attempt.errorTags.joinToString { errorLabel(it) }}",
            color = AppColors.Warning,
            fontSize = 13.sp,
        )
    }
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text(if (state.itemIndex == state.itemCount) "Ver resumen" else "Continuar")
    }
}

@Composable
private fun LogicSummaryScreen(state: LogicUiState.Summary, onDone: () -> Unit) {
    val result = state.result
    Text("Sesión completada", color = AppColors.FocusSignal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    LogicCard {
        Text("ICR medio ${result.averageIcr.roundToInt()}", color = AppColors.TextPrimary, fontSize = 26.sp)
        MetricLine("Precisión inferencial", result.inferentialAccuracy)
        MetricLine("Justificación", result.averageJustification)
        MetricLine("Calibración de confianza", result.averageConfidenceCalibration)
        Text(
            result.transferConsistency?.let { "Transferencia isomorfa: ${(it * 100).roundToInt()}%" }
                ?: "Transferencia isomorfa: en calibración",
            color = AppColors.FocusReadout,
        )
    }
    if (result.topErrorTags.isNotEmpty()) {
        Text("Errores principales: ${result.topErrorTags.joinToString { errorLabel(it) }}", color = AppColors.Warning)
    }
    LogicCard {
        Text("Siguiente recomendación", color = AppColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(result.recommendation, color = AppColors.FocusReadout)
    }
    Text(
        "Este resultado describe esta práctica. La mejora estable requiere isomorfos y retests.",
        color = AppColors.TextMuted,
        fontSize = 13.sp,
    )
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Volver a Lógica") }
}

@Composable
private fun ErrorScreen(message: String, onBack: () -> Unit) {
    CenteredMessage(message)
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
}

@Composable
private fun CenteredMessage(message: String) {
    Text(
        message,
        color = AppColors.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(AppSpacing.XXL),
    )
}

@Composable
private fun SelectableRow(text: String, selected: Boolean, onClick: () -> Unit, checkbox: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = AppShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppColors.SurfaceVariant else AppColors.Surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.S),
        ) {
            if (checkbox) Checkbox(selected, onCheckedChange = { onClick() })
            else RadioButton(selected, onClick = onClick)
            Text(text, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun <T> ChoiceSection(
    title: String,
    choices: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(title, color = AppColors.FocusReadout)
    choices.forEach { choice ->
        OutlinedButton(
            onClick = { onSelect(choice) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (choice == selected) "✓ ${label(choice)}" else label(choice))
        }
    }
}

@Composable
private fun LogicCard(content: @Composable () -> Unit) {
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

@Composable
private fun MetricLine(label: String, value: Double) {
    Text("$label: ${(value * 100).roundToInt()}%", color = AppColors.FocusReadout, fontSize = 13.sp)
}

private fun claimChoices(type: LogicExerciseType): List<JustificationClaim> = when (type) {
    LogicExerciseType.DEDUCTION_FROM_PREMISES -> listOf(
        JustificationClaim.CONCLUSION_FOLLOWS,
        JustificationClaim.CONCLUSION_DOES_NOT_FOLLOW,
    )
    LogicExerciseType.CONDITIONAL_TEST -> listOf(
        JustificationClaim.RULE_IS_REFUTED,
        JustificationClaim.CONCLUSION_DOES_NOT_FOLLOW,
    )
    LogicExerciseType.NECESSARY_SUFFICIENT -> listOf(
        JustificationClaim.CONDITIONS_CLASSIFIED,
        JustificationClaim.CONCLUSION_DOES_NOT_FOLLOW,
    )
    LogicExerciseType.CONTRADICTION_DETECTION -> listOf(
        JustificationClaim.SET_IS_CONSISTENT,
        JustificationClaim.SET_IS_INCONSISTENT,
    )
    LogicExerciseType.COUNTEREXAMPLE -> listOf(
        JustificationClaim.COUNTEREXAMPLE_INVALIDATES,
        JustificationClaim.CONCLUSION_FOLLOWS,
    )
}

private fun ruleLabel(rule: ReasoningRule): String = when (rule) {
    ReasoningRule.MODUS_PONENS -> "Aplicar regla al caso"
    ReasoningRule.MODUS_TOLLENS -> "Contraposición"
    ReasoningRule.CONDITIONAL_FALSIFICATION -> "Buscar P verdadero y Q falso"
    ReasoningRule.NECESSARY_SUFFICIENT_CLASSIFICATION -> "Clasificar necesario/suficiente"
    ReasoningRule.CONSISTENCY_CHECK -> "Comprobar compatibilidad"
    ReasoningRule.COUNTEREXAMPLE_CONSTRUCTION -> "Conservar premisas y negar conclusión"
}

private fun claimLabel(claim: JustificationClaim): String = when (claim) {
    JustificationClaim.NOT_PROVIDED -> "Sin justificación"
    JustificationClaim.CONCLUSION_FOLLOWS -> "La conclusión se sigue"
    JustificationClaim.CONCLUSION_DOES_NOT_FOLLOW -> "La conclusión no se sigue"
    JustificationClaim.RULE_IS_REFUTED -> "El caso refuta la regla"
    JustificationClaim.CONDITIONS_CLASSIFIED -> "La relación necesaria/suficiente está clasificada"
    JustificationClaim.SET_IS_CONSISTENT -> "El conjunto es consistente"
    JustificationClaim.SET_IS_INCONSISTENT -> "El conjunto es inconsistente"
    JustificationClaim.COUNTEREXAMPLE_INVALIDATES -> "El contraejemplo invalida la conclusión"
}

private fun confidenceLabel(confidence: LogicConfidence): String = when (confidence) {
    LogicConfidence.LOW -> "Baja"
    LogicConfidence.MEDIUM -> "Media"
    LogicConfidence.HIGH -> "Alta"
}

private fun skillLabel(state: SkillState): String = state.skillId.name.lowercase().replace('_', ' ')

private fun errorLabel(error: LogicErrorTag): String = error.name.lowercase().replace('_', ' ')
