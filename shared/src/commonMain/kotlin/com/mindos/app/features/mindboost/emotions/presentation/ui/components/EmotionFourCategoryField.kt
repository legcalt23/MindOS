@file:Suppress("FunctionNaming", "MagicNumber", "MaxLineLength")

package com.mindos.app.features.mindboost.emotions.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.presentation.EmotionStrings
import com.mindos.app.features.mindboost.emotions.presentation.state.EmotionFieldState

/**
 * EM3 Separador renderer: the neutral phrase + the four categories in a 2×2 grid, laid out in the order the field
 * carries. Each button is ≥56dp with an ES label; selection confirms on release (Button default). Distinct buttons →
 * distinct category responses (no PRIMARY+index fallback).
 *
 * Ola 3 · R3 · P1 — the order comes from `field.options`, drawn per trial, and this file no longer names a single
 * category. Hard-coding the quadrants here is what made "always tap the same place" worth up to 0.45 of trials
 * against a chance of 0.25; reading the order from the field is what keeps a future edit from restoring it.
 */
@Composable
fun EmotionFourCategoryField(
    field: EmotionFieldState.FourCategory,
    onCategory: (EmotionCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = field.phrase,
            color = AppColors.FocusSignal,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text("¿Qué es esto?", color = AppColors.FocusReadout, fontSize = 14.sp)
        field.options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { category -> CategoryButton(category, onCategory, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryButton(category: EmotionCategory, onCategory: (EmotionCategory) -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = { onCategory(category) }, modifier = modifier.heightIn(min = 56.dp)) {
        Text(EmotionStrings.categoryLabel(category), fontSize = 14.sp)
    }
}
