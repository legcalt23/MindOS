@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState

/**
 * Renders a digital-distraction (E11 Ancla) trial: a simple LEFT/RIGHT arrow as the ongoing main
 * task, plus — on intruder trials — a salient notification-style banner that must be ignored. The
 * session screen overlays the LEFT/RIGHT tap halves; responding to the arrow's direction is correct
 * regardless of the banner, so accuracy under intrusion vs baseline is the distraction-resistance
 * index the metrics engine measures.
 */
@Composable
fun FocusAnchorField(field: FocusFieldState.Anchor, modifier: Modifier = Modifier) {
    val arrow = if (field.targetDirection == ResponseKey.LEFT) "←" else "→"
    Box(modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(arrow, color = AppColors.FocusSignal, fontSize = 76.sp, fontWeight = FontWeight.Bold)
        }
        if (field.intruderPresent) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.FocusLure)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    "Notificación nueva — ignórala",
                    color = AppColors.ProfileTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
