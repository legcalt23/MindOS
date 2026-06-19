@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Renders a visual-search display (E7 Rastreo): [FocusFieldState.Search.setSize] items laid out in a
 * grid where the target ("L") may hide among distractors ("T"). Target and distractors share the
 * same colour and size so the target does NOT pop out — the user must scan serially, which is what
 * makes search RT grow with set size (the SET_SIZE_SLOPE the metrics engine measures).
 *
 * Single-tap: the session screen treats the whole field as one response zone (tap = "target present").
 * Layout is derived deterministically from `layoutSeed`, so it is stable during a trial but varies
 * across trials.
 */
@Composable
fun FocusSearchField(field: FocusFieldState.Search, modifier: Modifier = Modifier) {
    val items = remember(field) { searchLayout(field.setSize, field.targetPresent, field.layoutSeed) }
    val columns = remember(field) { ceil(sqrt(field.setSize.coerceAtLeast(1).toDouble())).toInt().coerceAtLeast(1) }
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowItems.forEach { isTarget ->
                        Text(
                            text = if (isTarget) "L" else "T",
                            color = AppColors.ProfileTextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/** A boolean per item (true = target). Exactly one target when [present]; deterministic from [seed]. */
private fun searchLayout(setSize: Int, present: Boolean, seed: Int): List<Boolean> {
    val items = MutableList(setSize.coerceAtLeast(1)) { false }
    if (present) {
        items[Random(seed).nextInt(items.size)] = true
    }
    return items
}
