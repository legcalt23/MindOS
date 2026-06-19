@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState

/**
 * Renders a dual-task trial (E8 Dúo): the primary go/no-go target (green = go, dim = no-go) the user
 * taps on go. On dual trials a concurrent counter ticks at the top — a real second demand on
 * attention the user is told to monitor. The accuracy drop from single to dual trials is the
 * dual-task cost the metrics engine measures (the secondary load is not separately scored).
 */
@Composable
fun FocusDualTaskField(field: FocusFieldState.Dual, modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val color = if (field.go) AppColors.FocusSignal else AppColors.FocusReadout.copy(alpha = 0.4f)
            Box(Modifier.size(90.dp).clip(CircleShape).background(color))
        }
        if (field.dual) {
            var counter by remember(field) { mutableStateOf(0) }
            LaunchedEffect(field) {
                while (true) {
                    kotlinx.coroutines.delay(450)
                    counter = (counter + 1) % 10
                }
            }
            Box(Modifier.align(Alignment.TopCenter).padding(18.dp)) {
                Text(
                    "Contador: $counter",
                    color = AppColors.FocusLure,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
