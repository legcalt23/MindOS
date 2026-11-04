@file:Suppress("FunctionNaming", "MagicNumber")

package com.mindos.app.features.mindboost.focus.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindos.app.design.AppColors
import com.mindos.app.features.mindboost.focus.presentation.state.FocusFieldState

/**
 * Renders a task-switching (E6) stimulus: a coloured shape plus the ACTIVE rule cue. The user applies
 * the cued rule and answers LEFT/RIGHT (the session screen overlays the two tap halves):
 * - COLOR rule → red = LEFT, blue = RIGHT.
 * - SHAPE rule → circle = LEFT, square = RIGHT.
 *
 * Both dimensions are always present, so the answer is only correct if the active rule is applied —
 * which is what makes a rule switch costly (the SWITCH_COST the metrics engine measures).
 *
 * Under the COLOR rule the fill is the ONLY thing that decides the answer, and the shape is contradictory
 * on half the trials — so if that fill separates on hue alone, a user who cannot resolve the hue fails
 * those trials and the failure is recorded as switch cost. See [ruleColorFill].
 */
@Composable
fun FocusRuleSwitchField(field: FocusFieldState.RuleSwitch, modifier: Modifier = Modifier) {
    val fill = ruleColorFill(field.colorIsRed)
    val shape = if (field.shapeIsCircle) CircleShape else RectangleShape
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                text = if (field.ruleIsColor) "REGLA: COLOR" else "REGLA: FORMA",
                color = AppColors.FocusSignal,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.size(120.dp).clip(shape).background(fill))
        }
    }
}

/**
 * The two values of E6's colour dimension, as one function so the pair has a single owner.
 *
 * The test reads THIS, not the two constants: pinning `FocusRuleColorDark` against `NeuCyan` directly
 * would keep passing on the old pair the day someone swaps what this field draws. The pair must stay
 * separable on luminance, not only on hue — a rule the user never has to learn, because the dark value
 * and the red value are the same stimulus.
 */
internal fun ruleColorFill(colorIsRed: Boolean): Color =
    if (colorIsRed) AppColors.FocusRuleColorDark else AppColors.NeuCyan
