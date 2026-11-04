@file:Suppress("FunctionNaming", "LongMethod", "MagicNumber", "TooManyFunctions")

package com.mindos.app.features.mindboost.decisiones.presentation.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.decisiones.presentation.BayesArenaStateHolder
import com.mindos.app.features.mindboost.decisiones.presentation.state.BayesArenaUiState
import com.mindos.app.features.mindboost.decisiones.presentation.state.BayesEstimateFeedback
import com.mindverse.mindboost.core.session.DecisionRound
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/** Composition-scoped factory for [BayesArenaStateHolder] with a default scenario set. */
@Composable
fun rememberBayesArenaStateHolder(
    seed: Long = BayesArenaStateHolder.DEFAULT_SEED,
): BayesArenaStateHolder = remember(seed) {
    BayesArenaStateHolder(scenarios = BayesArenaStateHolder.generateScenarios(seed = seed))
}

/**
 * Bayes Arena (EX-07/08) screen. The holder owns all scoring; this screen renders the scenario
 * console, captures the posterior estimate and confidence via sliders, measures reaction time
 * monotonically, and shows honest Bayesian-update feedback at the end.
 */
@Composable
fun BayesArenaScreen(
    holder: BayesArenaStateHolder,
    onFinished: (List<DecisionRound>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.FocusCanvas)
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        when (val state = holder.uiState) {
            is BayesArenaUiState.Idle -> IntroContent(
                onStart = holder::start,
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center),
            )

            is BayesArenaUiState.Running -> RunningContent(
                state = state,
                onSubmit = holder::submit,
                modifier = Modifier.align(Alignment.Center),
            )

            is BayesArenaUiState.Finished -> FinishedContent(
                state = state,
                onContinue = { onFinished(state.rounds) },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun IntroContent(onStart: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Arena Bayes", color = AppColors.ProfileTextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Te damos los datos de una prueba. Estima la probabilidad real y di cuánta certeza tienes. " +
                "Medimos cómo actualizas con la evidencia, no si aciertas un resultado.",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(28.dp))
        CtaButton(text = "Empezar", onClick = onStart)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Volver",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp),
        )
    }
}

@Composable
private fun RunningContent(
    state: BayesArenaUiState.Running,
    onSubmit: (Double, Double, Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mark = remember(state.scenarioIndex) { TimeSource.Monotonic.markNow() }
    var posterior by remember(state.scenarioIndex) { mutableStateOf(0.5f) }
    var confidence by remember(state.scenarioIndex) { mutableStateOf(0.75f) }
    // R3 · AGRS dial term: the interaction fact, reset per scenario. Submitting without ever touching the
    // posterior slider is the "dial default" pattern; believing the default IS honest, so we record the
    // touch, not the value.
    var posteriorMoved by remember(state.scenarioIndex) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Escenario ${state.scenarioIndex + 1} / ${state.scenarioCount}",
            color = AppColors.FocusReadout,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        ConsoleCard(state)

        Spacer(Modifier.height(22.dp))
        SliderRow(
            label = "Probabilidad real",
            valueText = "${(posterior * 100).roundToInt()}%",
            value = posterior,
            onValueChange = {
                posterior = it
                posteriorMoved = true
            },
            valueRange = 0f..1f,
        )
        Spacer(Modifier.height(16.dp))
        SliderRow(
            label = "Tu certeza",
            valueText = "${(confidence * 100).roundToInt()}%",
            value = confidence,
            onValueChange = { confidence = it },
            valueRange = 0.5f..1f,
        )

        Spacer(Modifier.height(26.dp))
        CtaButton(text = "Enviar estimación") {
            onSubmit(
                posterior.toDouble(),
                confidence.toDouble(),
                mark.elapsedNow().inWholeMilliseconds,
                posteriorMoved,
            )
        }
    }
}

@Composable
private fun ConsoleCard(state: BayesArenaUiState.Running) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.NeuCyan.copy(alpha = 0.05f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ConsoleLine(label = "Prevalencia", value = "${state.baseRatePercent.roundToInt()}%")
        ConsoleLine(label = "Acierta positivos", value = "${state.sensitivityPercent.roundToInt()}%")
        ConsoleLine(label = "Falsas alarmas", value = "${state.falsePositivePercent.roundToInt()}%")
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Da positivo. ¿Qué probabilidad real hay de que sea cierto?",
            color = AppColors.ProfileTextPrimary,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConsoleLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = AppColors.FocusReadout, fontSize = 13.sp)
        Text(text = value, color = AppColors.FocusSignal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = AppColors.FocusReadout, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = valueText, color = AppColors.ProfileTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@Composable
private fun FinishedContent(
    state: BayesArenaUiState.Finished,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Cómo actualizaste",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatRatio(state.summary.meanBayesianUpdateRatio),
            color = AppColors.FocusSignal,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "× el ajuste de Bayes (1.0 = ideal)",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(20.dp))
        state.perScenario.forEachIndexed { index, item ->
            ScenarioFeedbackRow(index = index, feedback = item)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(18.dp))
        CtaButton(text = "Continuar", onClick = onContinue)
    }
}

@Composable
private fun ScenarioFeedbackRow(index: Int, feedback: BayesEstimateFeedback) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.NeuCyan.copy(alpha = 0.04f))
            .border(1.dp, AppColors.Stroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Escenario ${index + 1}",
            color = AppColors.FocusReadout,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Tú ${(feedback.userPosterior * 100).roundToInt()}%",
                color = AppColors.ProfileTextPrimary,
                fontSize = 14.sp,
            )
            Text(
                text = "Bayes ${(feedback.mathPosterior * 100).roundToInt()}%",
                color = AppColors.FocusSignal,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatRatio(value: Double): String {
    val rounded = (value * 100).roundToInt() / 100.0
    return rounded.toString()
}

@Composable
private fun CtaButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppColors.FocusSignal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = AppColors.OnBright, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
