@file:Suppress("FunctionNaming", "MagicNumber") // Composables are PascalCase; the layout numbers are the design.

package com.mindos.app.features.mindboost.emotions.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindos.app.features.mindboost.core.calibration.ConfidenceLevel
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings

/**
 * Ola 1 · N1 — the three-button confidence strip.
 *
 * There is no skip button and no timer shown. Both are deliberate: a skip button makes not answering feel like a
 * failure, and a visible countdown turns a low-stakes aside into a second speeded task. Ignoring the strip is a
 * first-class outcome, so it simply lapses.
 */
@Composable
fun EmotionConfidenceStrip(onPick: (ConfidenceLevel) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(EmotionStrings.CONFIDENCE_PROMPT, textAlign = TextAlign.Center)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            ConfidenceLevel.entries.forEach { level ->
                OutlinedButton(onClick = { onPick(level) }) { Text(labelFor(level)) }
            }
        }
    }
}

internal fun labelFor(level: ConfidenceLevel): String = EmotionStrings.confidenceLabel(level)

/**
 * Ola 1 · N1 — the per-block forecast.
 *
 * Asked before the block and never revisited during it. The user is not shown their running score, so the number
 * they pick has to come from their own model of themselves rather than from arithmetic on feedback we handed them.
 */
@Composable
fun EmotionBlockForecastPrompt(blockSize: Int, onSubmit: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(EmotionStrings.blockForecastPrompt(blockSize), textAlign = TextAlign.Center)
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items((0..blockSize).toList()) { n ->
                Button(onClick = { onSubmit(n) }) { Text("$n") }
            }
        }
    }
}
