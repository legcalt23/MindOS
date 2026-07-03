@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.emotions.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState

/**
 * EM2 Recuperación renderer: two persistently-visible, labeled perceptual bars (text + height, never
 * color alone). Tap the side with the LARGER bar. Each side is an individually-addressable ≥48dp target
 * with a contentDescription (decision H accessibility; resolves the ux-P0 about invisible tap halves).
 */
@Composable
fun EmotionTwoAltField(
    field: EmotionFieldState.TwoAlt,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("¿Qué barra es más alta?", color = AppColors.FocusReadout, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Bar("Izquierda", field.leftMagnitude, onLeft, Modifier.weight(1f))
            Bar("Derecha", field.rightMagnitude, onRight, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Bar(label: String, magnitude: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 48.dp)
            .clickable { onClick() }
            .semantics { contentDescription = "Barra $label" }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .width(70.dp)
                .height((magnitude * 240).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.FocusSignal),
        )
        Spacer(Modifier.height(10.dp))
        Text(label, color = AppColors.FocusReadout, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
