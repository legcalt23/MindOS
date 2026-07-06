@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.emotions.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState

/**
 * Sober four-part result (decision H). All four parts are ALWAYS rendered together with the transfer
 * label and confidence caveat — never collapsed, never over-promising. On a safety stop, the neutral
 * close copy leads. A disclaimer reminds it is functional training, not a clinical conclusion.
 */
@Composable
fun EmotionResultScreen(state: EmotionSessionUiState.Result, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(state.exerciseName, color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (state.stoppedForSafety) {
            Text(EmotionStrings.DESREGULATION_CLOSE, color = AppColors.FocusReadout, fontSize = 15.sp)
        }
        val feedback = state.feedback
        Part("Qué ocurrió", feedback.whatHappened)
        Part("Qué significa", feedback.whatItMeans)
        Part("Qué practicar", feedback.whatToPractice)
        Part("Qué no se puede concluir aún", feedback.whatNotToConclude)
        val transfer = EmotionStrings.transferLabel(feedback.transferLabel)
        Text("Transferencia: $transfer", color = AppColors.FocusReadout, fontSize = 13.sp)
        Text(feedback.confidenceNote, color = AppColors.FocusReadout, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(EmotionStrings.DISCLAIMERS[0], color = AppColors.FocusReadout, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone) { Text("Volver") }
    }
}

@Composable
private fun Part(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = AppColors.FocusSignal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = AppColors.FocusReadout, fontSize = 14.sp)
    }
}
