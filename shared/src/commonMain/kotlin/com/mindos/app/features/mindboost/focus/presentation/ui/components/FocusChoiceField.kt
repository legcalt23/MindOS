@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.domain.model.ResponseKey
import com.mindos.app.features.mindboost.focus.presentation.state.ChoiceParadigm
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState

/**
 * Renders a two-choice stimulus the user answers LEFT or RIGHT:
 * - FLANKER (E2): a central target arrow flanked by arrows that agree (congruent) or oppose
 *   (incongruent). The user responds to the CENTER arrow.
 * - CONFLICT (E5): a single arrow placed on its own side (congruent) or the opposing side
 *   (incongruent, Simon-style spatial conflict). The user responds to the arrow's DIRECTION,
 *   ignoring where it appears.
 *
 * The session screen overlays invisible LEFT/RIGHT tap halves; this composable is purely visual.
 */
@Composable
fun FocusChoiceField(field: FocusFieldState.Choice, modifier: Modifier = Modifier) {
    val target = field.targetDirection.glyph()
    Box(modifier, contentAlignment = Alignment.Center) {
        when (field.paradigm) {
            ChoiceParadigm.FLANKER -> FlankerRow(field, target)
            ChoiceParadigm.CONFLICT -> ConflictArrow(field, target)
            ChoiceParadigm.DEADLINE -> DeadlineArrow(target, field.underPressure)
        }
    }
}

@Composable
private fun DeadlineArrow(target: String, underPressure: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (underPressure) {
            Text("¡RÁPIDO!", color = AppColors.FocusError, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            target,
            color = if (underPressure) AppColors.FocusError else AppColors.FocusSignal,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FlankerRow(field: FocusFieldState.Choice, target: String) {
    val flanker = if (field.congruent) target else field.targetDirection.opposite().glyph()
    val perSide = (field.distractorCount / 2).coerceAtLeast(1)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(perSide) { Flanker(flanker) }
        Text(target, color = AppColors.FocusSignal, fontSize = 60.sp, fontWeight = FontWeight.Bold)
        repeat(perSide) { Flanker(flanker) }
    }
}

@Composable
private fun Flanker(glyph: String) {
    Text(glyph, color = AppColors.FocusReadout, fontSize = 44.sp)
}

@Composable
private fun ConflictArrow(field: FocusFieldState.Choice, target: String) {
    // Congruent → the arrow sits on its own side; incongruent → the opposite side (spatial conflict).
    val onLeft = if (field.congruent) {
        field.targetDirection == ResponseKey.LEFT
    } else {
        field.targetDirection == ResponseKey.RIGHT
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (onLeft) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Text(
            target,
            color = AppColors.FocusSignal,
            fontSize = 68.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
    }
}

private fun ResponseKey.glyph(): String = if (this == ResponseKey.LEFT) "←" else "→"

private fun ResponseKey.opposite(): ResponseKey =
    if (this == ResponseKey.LEFT) ResponseKey.RIGHT else ResponseKey.LEFT
