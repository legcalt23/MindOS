@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.emotions.presentation.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.domain.model.SelfReport
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings

/** Brief, optional-introspection self-report (before/after). Three 1..5 scales; submit continues. */
@Composable
fun EmotionSelfReportScreen(
    exerciseName: String,
    subtitle: String,
    onSubmit: (SelfReport) -> Unit,
    modifier: Modifier = Modifier,
) {
    var valence by remember { mutableStateOf(3) }
    var arousal by remember { mutableStateOf(3) }
    var intensity by remember { mutableStateOf(3) }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(exerciseName, color = AppColors.FocusSignal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(EmotionStrings.SELF_REPORT_TITLE, color = AppColors.FocusReadout, fontSize = 14.sp)
        if (subtitle.isNotBlank()) Text(subtitle, color = AppColors.FocusReadout, fontSize = 13.sp)
        ScaleSelector(EmotionStrings.VALENCE, valence) { valence = it }
        ScaleSelector(EmotionStrings.AROUSAL, arousal) { arousal = it }
        ScaleSelector(EmotionStrings.INTENSITY, intensity) { intensity = it }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onSubmit(SelfReport(valence, arousal, intensity)) }) {
            Text(EmotionStrings.CONTINUE)
        }
    }
}

/** Proxy-armed mid-session check-in: one neutral 1..5 intensity question (never names an emotion). */
@Composable
fun EmotionCheckInScreen(onSubmit: (Int) -> Unit, modifier: Modifier = Modifier) {
    var intensity by remember { mutableStateOf(3) }
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(EmotionStrings.OVERLOAD_CHECK_IN, color = AppColors.FocusSignal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        ScaleSelector(EmotionStrings.CHECK_IN_INTENSITY_PROMPT, intensity) { intensity = it }
        Button(onClick = { onSubmit(intensity) }) { Text(EmotionStrings.CONTINUE) }
    }
}

@Composable
private fun ScaleSelector(label: String, value: Int, onValue: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = AppColors.FocusReadout, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (n in 1..5) NumberChip(n, selected = n == value) { onValue(n) }
        }
    }
}

@Composable
private fun NumberChip(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clickable { onClick() }
            .then(
                if (selected) {
                    Modifier.background(AppColors.FocusSignal, CircleShape)
                } else {
                    Modifier.border(1.dp, AppColors.FocusReadout, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            color = if (selected) AppColors.FocusCanvas else AppColors.FocusReadout,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
