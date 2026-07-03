@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.emotions.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.EmotionSessionStateHolder
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionRunningModel
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionFourCategoryField
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionGoNoGoField
import com.mindos.app.features.mindboost.emotions.presentation.ui.components.EmotionSafetyBar
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

            EmotionSessionUiState.MidCheckIn -> EmotionCheckInScreen(onSubmit = holder::submitCheckInIntensity)

            EmotionSessionUiState.MicroPause ->
                MicroPauseOverlay(onContinue = holder::onResumeFromMicroPause, onExit = holder::onExit)

            is EmotionSessionUiState.SelfReportAfter ->
                EmotionSelfReportScreen(state.exerciseName, subtitle = "", onSubmit = holder::submitSelfReport)

            is EmotionSessionUiState.Result -> EmotionResultScreen(state, onDone = onFinished)

            EmotionSessionUiState.Exited -> LaunchedEffect(Unit) { onFinished() }
        }
    }
}

@Composable
private fun RunningContent(model: EmotionRunningModel, holder: EmotionSessionStateHolder) {
    Column(Modifier.fillMaxSize()) {
        EmotionSafetyBar(
            onPause = holder::onPause,
            onReduce = holder::onReduceIntensity,
            onExit = holder::onExit,
        )
        Box(Modifier.fillMaxSize()) {
            when (val field = model.field) {
                is EmotionFieldState.GoNoGo -> EmotionGoNoGoField(field, onTap = holder::onTap)
                is EmotionFieldState.TwoAlt -> EmotionTwoAltField(field, onLeft = holder::onLeft, onRight = holder::onRight)
                is EmotionFieldState.FourCategory -> EmotionFourCategoryField(field, onCategory = holder::onCategory)
            }
        }
    }
}
