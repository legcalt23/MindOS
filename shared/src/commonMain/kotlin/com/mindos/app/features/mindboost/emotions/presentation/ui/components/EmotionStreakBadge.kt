@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.emotions.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings

/**
 * Ola 3 · R2 — the visible, recoverable cost of an error: consecutive correct answers, reset to zero by a miss.
 *
 * Rendered in the same readout colour at every value. A streak of zero is not failure state and must not look
 * like one: the exercise holds the error rate at a target, so it will reset this counter for everyone, by design,
 * several times a block. What makes the cost fair is that the next correct answer already rebuilds it.
 */
@Composable
fun EmotionStreakBadge(streak: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.FocusCanvas)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = "${EmotionStrings.STREAK_LABEL}: $streak" },
    ) {
        Text(
            "${EmotionStrings.STREAK_LABEL} $streak",
            color = AppColors.FocusReadout,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
