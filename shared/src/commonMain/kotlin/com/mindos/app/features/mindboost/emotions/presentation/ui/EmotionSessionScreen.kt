@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.emotions.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.EmotionSessionStateHolder
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionRunningModel
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionErrorMarkerField
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionFourCategoryField
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionGoNoGoField
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionSafetyBar
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionStreakBadge
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionTwoAltField

/**
 * Renders one running session, dispatching by [EmotionSessionStateHolder.uiState]. The always-visible
 * safety bar overlays every running state. Self-report/check-in/micro-pause/result are full screens;
 * safety stop and exit route through the holder so abandonment is persisted.
 */
@Composable
fun EmotionSessionScreen(
    holder: EmotionSessionStateHolder,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(holder) { holder.start() }
    Box(modifier.fillMaxSize().background(AppColors.FocusCanvas).statusBarsPadding()) {
        when (val state = holder.uiState) {
            is EmotionSessionUiState.SelfReportBefore ->
                EmotionSelfReportScreen(state.exerciseName, state.instructions, onSubmit = holder::submitSelfReport)

            is EmotionSessionUiState.Running -> RunningContent(state.model, holder)

            // Ola 1 · N1 — the transversal calibration layer. Both prompts lapse on their own; neither blocks.
            // Both keep the safety bar: a measurement layer never gets to be the reason pause and exit disappear,
            // and the forecast prompt in particular holds the screen for far longer than a trial does.
            is EmotionSessionUiState.ConfidencePrompt -> UnderSafetyBar(holder) {
                EmotionConfidenceStrip(onPick = holder::submitConfidence)
            }

            is EmotionSessionUiState.BlockForecastPrompt -> UnderSafetyBar(holder) {
                EmotionBlockForecastPrompt(state.blockSize, onSubmit = holder::submitBlockForecast)
            }

            EmotionSessionUiState.MidCheckIn -> EmotionCheckInScreen(onSubmit = holder::submitCheckInIntensity)

            EmotionSessionUiState.MicroPause ->
                MicroPauseOverlay(onContinue = holder::onResumeFromMicroPause, onExit = holder::onExit)

            is EmotionSessionUiState.SelfReportAfter ->
                EmotionSelfReportScreen(state.exerciseName, subtitle = "", onSubmit = holder::submitSelfReport)

            is EmotionSessionUiState.Result -> EmotionResultScreen(state, onDone = onFinished)

            EmotionSessionUiState.Exited -> LaunchedEffect(Unit) { onFinished() }

            // MC8h: a durable-save failure is shown explicitly (never auto-dismissed as if it had saved).
            EmotionSessionUiState.SaveFailed -> EmotionSaveFailedScreen(onDone = onFinished)
        }
    }
}

@Composable
private fun EmotionSaveFailedScreen(onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(EmotionStrings.SAVE_FAILED_TITLE, color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(EmotionStrings.SAVE_FAILED_BODY, color = AppColors.FocusReadout, fontSize = 15.sp)
        Button(onClick = onDone) { Text(EmotionStrings.SAVE_FAILED_ACTION) }
    }
}

/** Renders [content] beneath the always-visible safety bar. Pause/reduce/exit stay reachable throughout. */
@Composable
private fun UnderSafetyBar(holder: EmotionSessionStateHolder, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        EmotionSafetyBar(
            onPause = holder::onPause,
            onReduce = holder::onReduceIntensity,
            onExit = holder::onExit,
        )
        content()
    }
}

@Composable
private fun RunningContent(model: EmotionRunningModel, holder: EmotionSessionStateHolder) {
    UnderSafetyBar(holder) {
        Box(Modifier.fillMaxSize()) {
            when (val field = model.field) {
                is EmotionFieldState.GoNoGo -> EmotionGoNoGoField(field, onTap = holder::onTap)
                is EmotionFieldState.TwoAlt -> EmotionTwoAltField(field, onLeft = holder::onLeft, onRight = holder::onRight)
                is EmotionFieldState.FourCategory -> EmotionFourCategoryField(field, onCategory = holder::onCategory)
                is EmotionFieldState.ErrorMarker -> EmotionErrorMarkerField()
            }
            // Ola 3 · R2 — the streak sits ABOVE the field rather than inside it, so it survives the error marker
            // and the user sees the same counter drop to zero on the very screen that says they missed.
            model.streak?.let { EmotionStreakBadge(it, Modifier.align(Alignment.TopEnd)) }
        }
    }
}
