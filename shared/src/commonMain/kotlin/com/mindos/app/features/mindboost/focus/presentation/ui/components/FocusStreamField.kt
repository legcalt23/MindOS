@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Renders an RSVP stream (E10 Flujo): items flash one at a time, fast. Distractor letters are dim;
 * the target letters (T1 at `t1Index`, optional T2 at `t2Index`) flash in the signal colour. The
 * session screen captures a single tap — responding means "I caught a target". The speed (≈80 ms per
 * item) is what makes detection hard, which is the point of the paradigm.
 */
@Composable
fun FocusStreamField(field: FocusFieldState.Stream, modifier: Modifier = Modifier) {
    val items = remember(field) { buildStream(field) }
    var index by remember(field) { mutableStateOf(0) }
    LaunchedEffect(field) {
        for (k in items.indices) {
            index = k
            delay(FRAME_MS)
        }
    }
    val item = items.getOrElse(index) { items.lastOrNull() ?: StreamItem(" ", isTarget = false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = item.glyph,
            color = if (item.isTarget) AppColors.FocusSignal else AppColors.FocusReadout,
            fontSize = 84.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val FRAME_MS = 80L

private data class StreamItem(val glyph: String, val isTarget: Boolean)

/** Distractors are dim consonants; the two target slots use distinct vivid letters. */
private fun buildStream(field: FocusFieldState.Stream): List<StreamItem> {
    val random = Random(field.layoutSeed)
    val distractors = ('A'..'Z').filter { it != 'X' && it != 'O' }
    return (0 until field.length).map { i ->
        when (i) {
            field.t1Index -> StreamItem("X", isTarget = true)
            field.t2Index -> StreamItem("O", isTarget = true)
            else -> StreamItem(distractors[random.nextInt(distractors.size)].toString(), isTarget = false)
        }
    }
}
