@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.emotions.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings

/**
 * Always-visible safety controls during a running session (decision B): Pausar · Reducir intensidad ·
 * Salir. Each is a ≥48dp tap target. The user can always stop; the system never forces continuity.
 */
@Composable
fun EmotionSafetyBar(
    onPause: () -> Unit,
    onReduce: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SafetyAction(EmotionStrings.SAFETY_PAUSE, onPause)
        SafetyAction(EmotionStrings.SAFETY_REDUCE, onReduce)
        SafetyAction(EmotionStrings.SAFETY_EXIT, onExit)
    }
}

@Composable
private fun SafetyAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = AppColors.FocusReadout,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.heightIn(min = 48.dp).clickable { onClick() }.padding(8.dp),
    )
}
