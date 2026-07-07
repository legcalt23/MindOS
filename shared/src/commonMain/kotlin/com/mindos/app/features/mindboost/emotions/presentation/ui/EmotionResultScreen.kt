@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength")

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
import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.presentation.EmotionResultNotes
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionSessionUiState

/**
 * Post-session result (MC8u P0-1/P0-4). Renders the capacity-specific, confidence-gated feedback: what you
 * trained, what your performance showed (only when confident), a specific next practice, the next recommended
 * exercise + reason, the transfer label, the calibration progress and the health-scope disclaimer. On a safety
 * stop the performance/practice slots are suppressed and the protective note leads. Every visible title lives in
 * [EmotionStrings] so the copy/terminology scanners cover it.
 */
@Composable
fun EmotionResultScreen(state: EmotionSessionUiState.Result, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val feedback = state.feedback
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(state.exerciseName, color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        if (state.stoppedForSafety) {
            Text(EmotionStrings.RESULT_SAFETY_TITLE, color = AppColors.FocusSignal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(EmotionStrings.DESREGULATION_CLOSE, color = AppColors.FocusReadout, fontSize = 15.sp)
            feedback.safetyNote?.let { Text(it, color = AppColors.FocusReadout, fontSize = 14.sp) }
        } else {
            Part(EmotionStrings.RESULT_WHAT_TRAINED, feedback.whatTrained)
            Part(EmotionStrings.RESULT_PERFORMANCE, feedback.whatPerformanceShowed)
            feedback.strongSignal?.let { Text(it, color = AppColors.FocusSignal, fontSize = 14.sp) }
            feedback.weakSignal?.let { Text(it, color = AppColors.FocusReadout, fontSize = 14.sp) }
            Part(EmotionStrings.RESULT_PRACTICE, feedback.whatToPractice)
            val transfer = EmotionStrings.transferLabel(feedback.transferLabel)
            Text("${EmotionStrings.RESULT_TRANSFER_PREFIX}$transfer", color = AppColors.FocusReadout, fontSize = 13.sp)
            Text(feedback.confidenceNote, color = AppColors.FocusReadout, fontSize = 13.sp)
            Text(EmotionStrings.validSessionsProgress(state.validSessions), color = AppColors.FocusReadout, fontSize = 13.sp)
        }

        Part(EmotionStrings.RESULT_LIMITS, feedback.whatNotToConclude)

        // MC8u-B §3/§5: visible, non-punitive status notes (not counted / limited sample / calibrating / intensity
        // reduced), composed by the pure EmotionResultNotes so the wording and order are testable.
        EmotionResultNotes.notesFor(state).forEach { note ->
            Text(note, color = AppColors.FocusReadout, fontSize = 13.sp)
        }

        feedback.nextRecommendedExerciseId?.let { id ->
            val name = EmotionExerciseCatalog.all[id]?.displayName.orEmpty()
            val body = if (name.isBlank()) feedback.nextRecommendationReason else "$name. ${feedback.nextRecommendationReason}"
            Part(EmotionStrings.RESULT_NEXT, body)
        }

        Spacer(Modifier.height(4.dp))
        Text(EmotionStrings.DISCLAIMERS[3], color = AppColors.FocusReadout, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDone) { Text(EmotionStrings.RESULT_BACK) }
    }
}

@Composable
private fun Part(title: String, body: String) {
    if (body.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = AppColors.FocusSignal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = AppColors.FocusReadout, fontSize = 14.sp)
    }
}
