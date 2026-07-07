@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength") // Composable naming + layout constants + copy.

package com.mindos.app.features.mindboost.emotions.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.EmotionHistoryUiModel
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings

/**
 * Minimal history/progress card (MC8u-B §4). Renders the PII-free [EmotionHistoryUiModel]: last session (valid vs
 * practice), trained capacity, calibration state, progress toward 5 valid sessions, next suggestion, an optional
 * protective note, and the "not a clinical evaluation" line. Never an official rank before 5 valid sessions.
 */
@Composable
fun EmotionHistoryCard(model: EmotionHistoryUiModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(EmotionStrings.HISTORY_TITLE, color = AppColors.FocusSignal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (model.isEmpty) {
            Text(EmotionStrings.HISTORY_EMPTY, color = AppColors.FocusReadout, fontSize = 13.sp)
            return@Column
        }
        model.lastExerciseName?.let {
            Text("${EmotionStrings.HISTORY_LAST_SESSION}: $it", color = AppColors.FocusReadout, fontSize = 13.sp)
        }
        Text(model.lastSessionLabel, color = AppColors.FocusReadout, fontSize = 13.sp)
        Text("${EmotionStrings.HISTORY_STATE_PREFIX}${model.stateLabel}", color = AppColors.FocusReadout, fontSize = 13.sp)
        Text(model.validSessionsProgress, color = AppColors.FocusReadout, fontSize = 13.sp)
        model.nextSuggestion?.let {
            Text("${EmotionStrings.HISTORY_NEXT_PREFIX}$it", color = AppColors.FocusReadout, fontSize = 13.sp)
        }
        model.safetyNote?.let { Text(it, color = AppColors.FocusReadout, fontSize = 13.sp) }
        Text(EmotionStrings.HISTORY_NOT_CLINICAL, color = AppColors.FocusReadout, fontSize = 11.sp)
    }
}
