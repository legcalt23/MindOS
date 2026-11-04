@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import com.mindos.app.features.mindboost.focus.presentation.state.FocusMeasureRunningModel

/**
 * Shared live surface for the fixed-difficulty measurement flows (baseline + evaluation). Renders the
 * tap/choice field with the right response zones and a calm HUD (which block, progress, abort). It is
 * deliberately separate from the training session screen: those paradigms (tap/choice only) are a
 * subset, and reusing this keeps both measurement screens consistent without touching the richer
 * training surface.
 */
@Composable
fun FocusMeasureRunningContent(
    model: FocusMeasureRunningModel,
    onResponse: (ResponseKey) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.FocusCanvas)) {
        // Frozen: this is the live measurement surface (RT-critical). The ambient
        // must not invalidate the Canvas per frame and contend with timing (C5 honesty).
        FocusAtmosphere(modifier = Modifier.fillMaxSize(), intensity = 0.4f, animated = false)

        when (val field = model.field) {
            is FocusFieldState.Choice -> ChoiceResponseLayer(onResponse) {
                FocusChoiceField(field = field, modifier = Modifier.fillMaxSize())
            }
            else -> SingleTapField(onTap = { onResponse(ResponseKey.PRIMARY) }) {
                FocusStimulusField(field = field, modifier = Modifier.fillMaxSize())
            }
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            MeasureHud(model = model, onAbort = onAbort)
        }
    }
}

@Composable
private fun MeasureHud(model: FocusMeasureRunningModel, onAbort: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FocusAbortButton(onAbort = onAbort)
        Column(horizontalAlignment = Alignment.End) {
            Text(model.title, color = AppColors.FocusSignal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "${model.blockLabel} · bloque ${model.blockIndex + 1} de ${model.blockCount}",
                color = AppColors.FocusReadout,
                fontSize = 11.sp,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    ProgressBar(progress = model.progress)
}

@Composable
private fun ProgressBar(progress: Float) {
    val animated by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(AppColors.FocusSignal, AppColors.NeuCyan))),
        )
    }
}

@Composable
private fun SingleTapField(onTap: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onTap() } }) { content() }
}

@Composable
private fun ChoiceResponseLayer(onResponse: (ResponseKey) -> Unit, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        ChoiceTapZone(modifier = Modifier.weight(1f)) { onResponse(ResponseKey.LEFT) }
        ChoiceTapZone(modifier = Modifier.weight(1f)) { onResponse(ResponseKey.RIGHT) }
    }
    content()
}

@Composable
private fun ChoiceTapZone(modifier: Modifier, onTap: () -> Unit) {
    Box(modifier = modifier.fillMaxHeight().pointerInput(Unit) { detectTapGestures { onTap() } })
}
