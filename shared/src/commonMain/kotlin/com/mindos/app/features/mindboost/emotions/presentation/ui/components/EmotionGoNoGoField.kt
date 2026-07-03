@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.emotions.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState

/**
 * EM1 Pausa renderer. The rule is the FRAME (open = respond, closed = withhold) — never the word.
 * Open/closed is conveyed by an explicit "ABIERTO"/"CERRADO" label and border weight (not color alone).
 * The whole field is a single tap target.
 */
@Composable
fun EmotionGoNoGoField(field: EmotionFieldState.GoNoGo, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().clickable { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (field.frameOpen) "ABIERTO" else "CERRADO",
            color = if (field.frameOpen) AppColors.FocusSignal else AppColors.FocusReadout,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(190.dp)
                .border(
                    width = if (field.frameOpen) 6.dp else 2.dp,
                    color = if (field.frameOpen) AppColors.FocusSignal else AppColors.FocusReadout,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(field.word, color = AppColors.FocusReadout, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(20.dp))
        Text("Toca solo si está ABIERTO", color = AppColors.FocusReadout, fontSize = 14.sp)
    }
}
