@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.emotions.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import kotlinx.coroutines.delay

/**
 * Neutral, low-motion micro-pause (decision B5). "Salir" is visible the ENTIRE time (t=0); "Continuar"
 * is enabled only after the minimum window. No decorative animation — only a plain countdown.
 */
@Composable
fun MicroPauseOverlay(onContinue: () -> Unit, onExit: () -> Unit, modifier: Modifier = Modifier) {
    var remaining by remember { mutableStateOf(MIN_SECONDS) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            EmotionStrings.MICRO_PAUSE,
            color = AppColors.FocusReadout,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(24.dp))
        if (remaining <= 0) {
            Button(onClick = onContinue) { Text(EmotionStrings.CONTINUE) }
        } else {
            Text("Continuar en ${remaining}s", color = AppColors.FocusReadout, fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            EmotionStrings.SAFETY_EXIT,
            color = AppColors.FocusSignal,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.heightIn(min = 48.dp).clickable { onExit() }.padding(8.dp),
        )
    }
}

private const val MIN_SECONDS = 20
