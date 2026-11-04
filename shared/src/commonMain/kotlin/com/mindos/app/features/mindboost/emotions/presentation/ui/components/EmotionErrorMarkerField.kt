@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.emotions.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings

/**
 * Ola 3 · R2 — the neutral post-error marker: a short muted bar, for 250 ms, and nothing else.
 *
 * Deliberately austere. It has no colour of alarm, no icon, no "incorrecto", and above all it does not reveal
 * which side was right — the exercise holds the error rate at a target, so showing the answer would make the
 * next trial a memory test of the last one. The screen reader gets a plain neutral phrase for the same reason.
 * It is not a punishment; it is the acknowledgement that makes the streak reset legible.
 */
@Composable
fun EmotionErrorMarkerField() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .semantics { contentDescription = EmotionStrings.ERROR_MARKER_A11Y }
                .width(96.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.FocusReadout),
        ) {
            Text("")
        }
    }
}
