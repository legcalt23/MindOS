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
import com.mindos.app.features.mindboost.emotions.presentation.EmotionCalibrationProgressUiModel

/**
 * MC8u-D §6 calibration-progress card for the block screen / dashboard. Renders the PII-free reusable model: the
 * status (provisional vs official — never an official rank before 5 valid sessions), the 0/5→5/5 progress, the
 * 0–100 task-performance block score ("—" while calibrating), an optional not-counted / low-confidence note, the
 * next suggestion, and a non-clinical disclaimer. Every state is a labelled Text (never color-only).
 */
@Composable
fun EmotionCalibrationProgressCard(model: EmotionCalibrationProgressUiModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.FocusSignal.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(model.title, color = AppColors.FocusSignal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(model.statusLabel, color = AppColors.FocusReadout, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(model.progressText, color = AppColors.FocusReadout, fontSize = 13.sp)
        Text(model.blockScoreText, color = AppColors.FocusReadout, fontSize = 13.sp)
        model.lastSessionText?.let { Text(it, color = AppColors.FocusReadout, fontSize = 13.sp) }
        model.notCountedNote?.let { Text(it, color = AppColors.FocusReadout, fontSize = 12.sp) }
        model.lowConfidenceNote?.let { Text(it, color = AppColors.FocusReadout, fontSize = 12.sp) }
        model.nextRecommendationText?.let { Text(it, color = AppColors.FocusReadout, fontSize = 13.sp) }
        Text(model.disclaimer, color = AppColors.FocusReadout, fontSize = 11.sp)
    }
}
